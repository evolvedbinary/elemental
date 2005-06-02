/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-03 Wolfgang M. Meier
 *  meier@ifs.tu-darmstadt.de
 *  http://exist.sourceforge.net
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 *  $Id$
 * 
 */
package org.exist;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.exist.dom.AttrImpl;
import org.exist.dom.CommentImpl;
import org.exist.dom.DocumentImpl;
import org.exist.dom.DocumentTypeImpl;
import org.exist.dom.ElementImpl;
import org.exist.dom.NodeObjectPool;
import org.exist.dom.ProcessingInstructionImpl;
import org.exist.dom.QName;
import org.exist.dom.TextImpl;
import org.exist.storage.DBBroker;
import org.exist.storage.GeneralRangeIndexSpec;
import org.exist.storage.NodePath;
import org.exist.util.Configuration;
import org.exist.util.ProgressIndicator;
import org.exist.util.XMLChar;
import org.exist.util.XMLString;
import org.exist.xquery.value.StringValue;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;

/**
 * Parses a given input document via SAX, stores it to
 * the database and handles index-creation.
 * 
 * @author wolf
 *
 */
public class Indexer extends Observable implements ContentHandler, LexicalHandler, ErrorHandler {

	private static final String ATTR_ID_TYPE = "ID";

    private final static Logger LOG =
		Logger.getLogger(Indexer.class);

	protected DBBroker broker = null;
	protected XMLString charBuf = new XMLString();
	protected int currentLine = 0;
	protected NodePath currentPath = new NodePath();
	
	protected DocumentImpl document = null;
	
	protected boolean insideDTD = false;
	protected boolean validate = false;
	protected int level = 0;
	protected Locator locator = null;
	protected int normalize = XMLString.SUPPRESS_BOTH;
	protected Map nsMappings = new HashMap();
	protected Element rootNode;
	
	protected Stack stack = new Stack();
	protected Stack nodeContentStack = new Stack();
	
	protected String ignorePrefix = null;
	protected ProgressIndicator progress;
	protected boolean suppressWSmixed =false;

    /* used to record the number of children of an element during 
     * validation phase. later, when storing the nodes, we already
     * know the child count and don't need to update the element
     * a second time.
     */
    private int childCnt[] = new int[0x1000];
//    private int childCnt[] = null;
    
    // the current position in childCnt
    private int elementCnt = 0;
    
	// reusable fields
	private TextImpl text = new TextImpl();
	private Stack usedElements = new Stack();

	/**
	 *  Create a new parser using the given database broker and
	 * user to store the document.
	 *
	 *@param  broker
	 *@exception  EXistException  
	 */
	public Indexer(DBBroker broker) throws EXistException {
		this.broker = broker;
		Configuration config = broker.getConfiguration();
		String suppressWS =
			(String) config.getProperty("indexer.suppress-whitespace");
		if (suppressWS != null) {
			if (suppressWS.equals("leading"))
				normalize = XMLString.SUPPRESS_LEADING_WS;
			else if (suppressWS.equals("trailing"))
				normalize = XMLString.SUPPRESS_TRAILING_WS;
			else if (suppressWS.equals("none"))
				normalize = 0;
		}
		
		Boolean temp;
		if ((temp = (Boolean) config.getProperty("indexer.preserve-whitespace-mixed-content"))
			!= null)
			suppressWSmixed = temp.booleanValue();
	}

	public void setValidating(boolean validate) {
		this.validate = validate;
	}

    /**
     * Prepare the indexer for parsing a new document. This will
     * reset the internal state of the Indexer object.
     *
     * @param doc
     */
    public void setDocument(DocumentImpl doc) {
        document = doc;
        // reset internal fields
        level = 0;
        currentPath.reset();
        stack = new Stack();
        nsMappings.clear();
        rootNode = null;
    }
    
	/**
	 * Set the document object to be used by this Indexer. This
	 * method doesn't reset the internal state.
	 * 
	 * @param doc
	 */
	public void setDocumentObject(DocumentImpl doc) {
	    document = doc;
	}

	public DocumentImpl getDocument() {
		return document;
	}
	
	public void characters(char[] ch, int start, int length) {
		if (length <= 0)
			return;
		if (charBuf != null) {
			charBuf.append(ch, start, length);
		} else {
			charBuf = new XMLString(ch, start, length);
		}
	}

	public void comment(char[] ch, int start, int length) {
		if (insideDTD)
			return;
		CommentImpl comment = new CommentImpl(ch, start, length);
		comment.setOwnerDocument(document);
		if (stack.empty()) {
			if (!validate)
				broker.store(comment, currentPath);
			document.appendChild(comment);
		} else {
			ElementImpl last = (ElementImpl) stack.peek();
			if (charBuf != null && charBuf.length() > 0) {
				final XMLString normalized = charBuf.normalize(normalize);
				if (normalized.length() > 0) {
					text.setData(normalized);
					text.setOwnerDocument(document);
					last.appendChildInternal(text);
					if (!validate)
						storeText();
				}
				charBuf.reset();
			}
			last.appendChildInternal(comment);
			if (!validate)
				broker.store(comment, currentPath);
		}
	}

	public void endCDATA() {
	}

	public void endDTD() {
		insideDTD = false;
	}

	public void endDocument() {
		if (!validate) {
			progress.finish();
			setChanged();
			notifyObservers(progress);
		}
//        LOG.debug("elementCnt = " + childCnt.length);
	}

	public void endElement(String namespace, String name, String qname) {
		final ElementImpl last = (ElementImpl) stack.peek();
		if (last.getNodeName().equals(qname)) {
			if (charBuf != null && charBuf.length() > 0) {
				// remove whitespace if the node has just a single text child,
				// keep whitespace for mixed content.
				final XMLString normalized =
					last.getChildCount() == 0 ? charBuf.normalize(normalize) : 
						(charBuf.isWhitespaceOnly() ? null : charBuf);
				if (normalized != null && normalized.length() > 0) {
					text.setData(normalized);
					text.setOwnerDocument(document);
					last.appendChildInternal(text);
					if (!validate)
						storeText();
					text.clear();
				}
				charBuf.reset();
			}
			stack.pop();
			
			XMLString elemContent = null;
			if (GeneralRangeIndexSpec.hasQNameOrValueIndex(last.getIndexType())) {
				elemContent = (XMLString) nodeContentStack.pop();
			}
			
			if (!validate)
			    broker.endElement(last, currentPath, elemContent == null ? null : elemContent.toString());
			
			currentPath.removeLastComponent();
			if (validate) {
				if (document.getTreeLevelOrder(level) < last.getChildCount()) {
					document.setTreeLevelOrder(level, last.getChildCount());
				}
                if (childCnt != null)
                    setChildCount(last);
			} else {
				document.setOwnerDocument(document);
				if (childCnt == null && last.getChildCount() > 0) {
					broker.update(last);
				}
			}
			level--;
			if (last != rootNode) {
				last.clear();
				usedElements.push(last);
			}
		}
	}

    /**
     * @param last
     */
    private void setChildCount(final ElementImpl last) {
        if (last.getPosition() >= childCnt.length) {
            int n[] = new int[childCnt.length * 2];
            System.arraycopy(childCnt, 0, n, 0, childCnt.length);
            childCnt = n;
        }
        childCnt[last.getPosition()] = last.getChildCount();
    }

	public void endEntity(String name) {
	}

	public void endPrefixMapping(String prefix) {
		if (ignorePrefix != null && prefix.equals(ignorePrefix)) {
			ignorePrefix = null;
		} else {
			nsMappings.remove(prefix);
		}
	}

	public void error(SAXParseException e) throws SAXException {
		LOG.debug("error at line " + e.getLineNumber(), e);
		throw new SAXException(
			"error at line " + e.getLineNumber() + ": " + e.getMessage(),
			e);
	}

	public void fatalError(SAXParseException e) throws SAXException {
		LOG.debug("fatal error at line " + e.getLineNumber(), e);
		throw new SAXException(
			"fatal error at line " + e.getLineNumber() + ": " + e.getMessage(),
			e);
	}

	public void ignorableWhitespace(char[] ch, int start, int length) {
	}

	public void processingInstruction(String target, String data) {
		ProcessingInstructionImpl pi =
			new ProcessingInstructionImpl(0, target, data);
		pi.setOwnerDocument(document);
		if (stack.isEmpty()) {
			if (!validate)
				broker.store(pi, currentPath);
			document.appendChild(pi);
		} else {
			ElementImpl last = (ElementImpl) stack.peek();
			if (charBuf != null && charBuf.length() > 0) {
				XMLString normalized = charBuf.normalize(normalize);
				if (normalized.length() > 0) {
					//TextImpl text =
					//    new TextImpl( normalized );
					text.setData(normalized);
					text.setOwnerDocument(document);
					last.appendChildInternal(text);
					if (!validate)
						storeText();
					text.clear();
				}
				charBuf.reset();
			}
			last.appendChildInternal(pi);
			if (!validate)
				broker.store(pi, currentPath);
		}
	}

	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	/**
	 *  set SAX parser feature. This method will catch (and ignore) exceptions
	 *  if the used parser does not support a feature.
	 *
	 *@param  factory  
	 *@param  feature  
	 *@param  value    
	 */
	private void setFeature(
		SAXParserFactory factory,
		String feature,
		boolean value) {
		try {
			factory.setFeature(feature, value);
		} catch (SAXNotRecognizedException e) {
			LOG.warn(e);
		} catch (SAXNotSupportedException snse) {
			LOG.warn(snse);
		} catch (ParserConfigurationException pce) {
			LOG.warn(pce);
		}
	}

	public void skippedEntity(String name) {
	}

	public void startCDATA() {
	}

	// Methods of interface LexicalHandler
	// used to determine Doctype

	public void startDTD(String name, String publicId, String systemId) {
		DocumentTypeImpl docType =
			new DocumentTypeImpl(name, publicId, systemId);
		document.setDocumentType(docType);
		insideDTD = true;
	}

	public void startDocument() {
		if (!validate) {
			progress = new ProgressIndicator(currentLine, 100);
			if (document.getDoctype() == null) {
				// we don't know the doctype
				// set it to the root node's tag name
				final DocumentTypeImpl dt =
					new DocumentTypeImpl(
						rootNode.getTagName(),
						null,
						document.getFileName());
				document.setDocumentType(dt);
			}
			document.setChildCount(0);
            elementCnt = 0;
		}
	}

	public void startElement(
		String namespace,
		String name,
		String qname,
		Attributes attributes) throws SAXException {
		// calculate number of real attributes:
		// don't store namespace declarations
		int attrLength = attributes.getLength();
		String attrQName;
		String attrNS;
		for (int i = 0; i < attributes.getLength(); i++) {
			attrNS = attributes.getURI(i);
			attrQName = attributes.getQName(i);
			if (attrQName.startsWith("xmlns")
				|| attrNS.equals("http://exist.sourceforge.net/NS/exist"))
				--attrLength;
		}

		ElementImpl last = null;
		ElementImpl node = null;
		int p = qname.indexOf(':');
		String prefix = p > -1 ? qname.substring(0, p) : "";
		QName qn = broker.getSymbols().getQName(namespace, name, prefix);
		if (!stack.empty()) {
			last = (ElementImpl) stack.peek();
			if (charBuf != null) {
				if(charBuf.isWhitespaceOnly()) {
					if (suppressWSmixed) {
						if(charBuf.length() > 0 && last.getChildCount() > 0) {
							text.setData(charBuf);
							text.setOwnerDocument(document);
							last.appendChildInternal(text);
							if (!validate)
								storeText();
							text.clear();
					   }
					}
					
				} else if(charBuf.length() > 0) {
					// mixed element content: don't normalize the text node, just check
					// if there is any text at all
					text.setData(charBuf);
					text.setOwnerDocument(document);
					last.appendChildInternal(text);
					if (!validate)
						storeText();
					text.clear();
				}
				charBuf.reset();
			}
			if (!usedElements.isEmpty()) {
				node = (ElementImpl) usedElements.pop();
				node.setNodeName(qn);
			} else
				node = new ElementImpl(qn);
			last.appendChildInternal(node);

			node.setOwnerDocument(document);
			node.setAttributes((short) attrLength);
			if (nsMappings != null && nsMappings.size() > 0) {
				node.setNamespaceMappings(nsMappings);
				nsMappings.clear();
			}

			stack.push(node);
			currentPath.addComponent(qn);

            node.setPosition(elementCnt++);
			if (!validate) {
                if (childCnt != null)
                    node.setChildCount(childCnt[node.getPosition()]);
				storeElement(node);
			}
		} else {
			if (validate)
				node = new ElementImpl(0, qn);
			else
				node = new ElementImpl(1, qn);
			rootNode = node;
			node.setOwnerDocument(document);
			node.setAttributes((short) attrLength);
			if (nsMappings != null && nsMappings.size() > 0) {
				node.setNamespaceMappings(nsMappings);
				nsMappings.clear();
			}

			stack.push(node);
			currentPath.addComponent(qn);

            node.setPosition(elementCnt++);
			if (!validate) {
                if (childCnt != null)
                    node.setChildCount(childCnt[node.getPosition()]);
				storeElement(node);
			}
			document.appendChild(node);
		}

		level++;
		if (document.getMaxDepth() < level)
			document.setMaxDepth(level);

		String attrPrefix;
		String attrLocalName;
		for (int i = 0; i < attributes.getLength(); i++) {
			attrNS = attributes.getURI(i);
			attrLocalName = attributes.getLocalName(i);
			attrQName = attributes.getQName(i);
			// skip xmlns-attributes and attributes in eXist's namespace
			if (attrQName.startsWith("xmlns")
				|| attrNS.equals("http://exist.sourceforge.net/NS/exist"))
				--attrLength;
			else {
				p = attrQName.indexOf(':');
				attrPrefix = (p > -1) ? attrQName.substring(0, p) : null;
				final AttrImpl attr = (AttrImpl)NodeObjectPool.getInstance().borrowNode(AttrImpl.class);
				attr.setNodeName(document.getSymbols().getQName(attrNS, attrLocalName, attrPrefix));
				attr.setValue(attributes.getValue(i));
				attr.setOwnerDocument(document);
				if (attributes.getType(i).equals(ATTR_ID_TYPE)) {
					attr.setType(AttrImpl.ID);
				} else if (attr.getQName().compareTo(AttrImpl.XML_ID_QNAME) == 0) {
					// an xml:id attribute. Normalize the attribute and set its type to ID
					attr.setValue(StringValue.trimWhitespace(StringValue.collapseWhitespace(attr.getValue())));
					if (!XMLChar.isValidNCName(attr.getValue()))
						throw new SAXException("Value of xml:id attribute is not a valid NCName: " + attr.getValue());
					attr.setType(AttrImpl.ID);
				}
				node.appendChildInternal(attr);
				if (!validate)
					broker.store(attr, currentPath);
				attr.release();
			}
		}
		if (attrLength > 0)
			node.setAttributes((short) attrLength);
		// notify observers about progress every 100 lines
		if (locator != null) {
			currentLine = locator.getLineNumber();
			if (!validate) {
				progress.setValue(currentLine);
				if (progress.changed()) {
					setChanged();
					notifyObservers(progress);
				}
			}
		}
	}

	private void storeText() {
		if (!nodeContentStack.isEmpty()) {
			for (int i = 0; i < nodeContentStack.size(); i++) {
				XMLString next = (XMLString) nodeContentStack.get(i);
				next.append(charBuf);
			}
		}
		broker.store(text, currentPath);
	}

	private void storeElement(ElementImpl node) {
		broker.store(node, currentPath);
        node.setChildCount(0);
		if (GeneralRangeIndexSpec.hasQNameOrValueIndex(node.getIndexType())) {
			XMLString contentBuf = new XMLString();
			nodeContentStack.push(contentBuf);
		}
	}

	public void startEntity(String name) {
	}

	public void startPrefixMapping(String prefix, String uri) {
		// skip the eXist namespace
		if (uri.equals("http://exist.sourceforge.net/NS/exist")) {
			ignorePrefix = prefix;
			return;
		}
		nsMappings.put(prefix, uri);
	}

	public void warning(SAXParseException e) throws SAXException {
		LOG.debug("warning at line " + e.getLineNumber(), e);
		throw new SAXException(
			"warning at line " + e.getLineNumber() + ": " + e.getMessage(),
			e);
	}
	
	private static StringBuffer removeLastPathComponent(StringBuffer path) {
		int i = -1;
		for(i = path.length() - 1; i > -1; i--) {
			if(path.charAt(i) == '/')
				break;
		}
		if(i < 0)
			return path;
		return path.delete(i, path.length());
	}

}
