/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.dom.memtree;

import org.exist.Indexer;
import org.exist.Namespaces;
import org.exist.dom.QName;
import org.exist.dom.persistent.NodeProxy;
import org.exist.xquery.Constants;
import org.exist.xquery.Expression;
import org.exist.xquery.XQueryContext;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import java.util.Arrays;


/**
 * Use this class to build a new in-memory DOM document.
 *
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang</a>
 */
public class MemTreeBuilder {

    private XQueryContext context;
    private DocumentImpl doc;
    private short level = 1;
    private int[] prevNodeInLevel;
    private String defaultNamespaceURI = XMLConstants.NULL_NS_URI;
    private final Expression expression;

    public MemTreeBuilder() {
        this((Expression) null);
    }

    public MemTreeBuilder(final Expression expression) {
        this(expression, null);
    }

    public MemTreeBuilder(final XQueryContext context) {
        this(null, context);
    }

    public MemTreeBuilder(final Expression expression, final XQueryContext context) {
        this.expression = expression;
        this.context = context;
        prevNodeInLevel = new int[15];
        Arrays.fill(prevNodeInLevel, -1);
        prevNodeInLevel[0] = 0;
    }

    public Expression getExpression() {
        return expression;
    }

    public void reset(@Nullable final XQueryContext context) {
        this.context = context;
        doc = null;
        level = 1;
        prevNodeInLevel = new int[15];
        Arrays.fill(prevNodeInLevel, -1);
        prevNodeInLevel[0] = 0;
        defaultNamespaceURI = XMLConstants.NULL_NS_URI;
    }

    /**
     * Returns the created document object.
     *
     * @return DOCUMENT ME!
     */
    public DocumentImpl getDocument() {
        return doc;
    }


    public XQueryContext getContext() {
        return context;
    }


    public int getSize() {
        return doc.getSize();
    }


    /**
     * Start building the document.
     */
    public void startDocument() {
        this.doc = new DocumentImpl(expression, context, false);
    }


    /**
     * Start building the document.
     *
     * @param explicitCreation DOCUMENT ME!
     */
    public void startDocument(final boolean explicitCreation) {
        this.doc = new DocumentImpl(expression, context, explicitCreation);
    }


    /**
     * End building the document.
     */
    public void endDocument() {
    }


    /**
     * Create a new element.
     *
     * @param namespaceURI DOCUMENT ME!
     * @param localName    DOCUMENT ME!
     * @param qname        DOCUMENT ME!
     * @param attributes   DOCUMENT ME!
     * @return the node number of the created element
     */
    public int startElement(final String namespaceURI, String localName, final String qname, final Attributes attributes) {
        final int prefixIdx = qname.indexOf(':');

        String prefix = null;
        if (context != null && !getDefaultNamespace().equals(namespaceURI == null ? XMLConstants.NULL_NS_URI : namespaceURI)) {
            prefix = context.getPrefixForURI(namespaceURI);
        }

        if (prefix == null) {
            prefix = (prefixIdx != Constants.STRING_NOT_FOUND) ? qname.substring(0, prefixIdx) : null;
        }

        if (localName.isEmpty()) {
            if (prefixIdx > -1) {
                localName = qname.substring(prefixIdx + 1);
            } else {
                localName = qname;
            }
        }

        final QName qn = new QName(localName, namespaceURI, prefix);
        return startElement(qn, attributes);
    }


    /**
     * Create a new element.
     *
     * @param qname      DOCUMENT ME!
     * @param attributes DOCUMENT ME!
     * @return the node number of the created element
     */
    public int startElement(final QName qname, final Attributes attributes) {
        final int nodeNr = doc.addNode(Node.ELEMENT_NODE, level, qname);

        if(attributes != null) {

            // parse attributes
            for(int i = 0; i < attributes.getLength(); i++) {
                final String attrQName = attributes.getQName(i);

                // skip xmlns-attributes
                if(!(attrQName.startsWith(XMLConstants.XMLNS_ATTRIBUTE))) {
                    final int p = attrQName.indexOf(':');
                    final String attrNS = attributes.getURI(i);
                    final String attrPrefix = (p != Constants.STRING_NOT_FOUND) ? attrQName.substring(0, p) : null;

                    String attrLocalName = attributes.getLocalName(i);
                    if (p == Constants.STRING_NOT_FOUND && attrLocalName.isEmpty()) {
                        // NOTE: Attributes#getLocalName(int) can return empty string if namespace processing is not enabled
                        attrLocalName = attrQName;
                    }

                    final QName attrQn = new QName(attrLocalName, attrNS, attrPrefix);
                    final int type = getAttribType(attrQn, attributes.getType(i));
                    doc.addAttribute(nodeNr, attrQn, attributes.getValue(i), type);
                }
            }
        }

        // update links
        if((level + 1) >= prevNodeInLevel.length) {
            final int[] t = new int[level + 2];
            System.arraycopy(prevNodeInLevel, 0, t, 0, prevNodeInLevel.length);
            prevNodeInLevel = t;
        }
        final int prevNr = prevNodeInLevel[level]; // TODO: remove potential ArrayIndexOutOfBoundsException

        if(prevNr > -1) {
            doc.next[prevNr] = nodeNr;
        }
        doc.next[nodeNr] = prevNodeInLevel[level - 1];
        prevNodeInLevel[level] = nodeNr;
        ++level;
        return nodeNr;
    }


    private int getAttribType(final QName qname, final String type) {
        if(qname.equals(Namespaces.XML_ID_QNAME) || type.equals(Indexer.ATTR_ID_TYPE)) {
            // an xml:id attribute.
            return AttrImpl.ATTR_ID_TYPE;
        } else if(type.equals(Indexer.ATTR_IDREF_TYPE)) {
            return AttrImpl.ATTR_IDREF_TYPE;
        } else if(type.equals(Indexer.ATTR_IDREFS_TYPE)) {
            return AttrImpl.ATTR_IDREFS_TYPE;
        } else {
            return AttrImpl.ATTR_CDATA_TYPE;
        }
    }


    /**
     * Close the last element created.
     */
    public void endElement() {
//      System.out.println("end-element: level = " + level);
        prevNodeInLevel[level] = -1;
        --level;
    }


    public int addReferenceNode(final NodeProxy proxy) {
        final int lastNode = doc.getLastNode();

        if (lastNode > 0 && level == doc.getTreeLevel(lastNode)) {

            if (doc.getNodeType(lastNode) == Node.TEXT_NODE && proxy.getNodeType() == Node.TEXT_NODE) {

                // if the last node is a text node, we have to append the
                // characters to this node. XML does not allow adjacent text nodes.
                doc.appendChars(lastNode, proxy.getNodeValue());
                return lastNode;
            }

            if (doc.getNodeType(lastNode) == NodeImpl.REFERENCE_NODE) {

                // check if the previous node is a reference node. if yes, check if it is a text node
                final int p = doc.alpha[lastNode];

                if (doc.references[p].getNodeType() == Node.TEXT_NODE && proxy.getNodeType() == Node.TEXT_NODE) {

                    // found a text node reference. create a new char sequence containing
                    // the concatenated text of both nodes
                    final String s = doc.references[p].getStringValue() + proxy.getStringValue();
                    doc.replaceReferenceNode(lastNode, s);
                    return lastNode;
                }
            }
        }

        // check if the node is a Document Node, if so we don't add the document, but instead we add all of its children
        final NodeProxy[] proxies;
        if (proxy.getNodeType() == Node.DOCUMENT_NODE) {
            final org.exist.dom.persistent.DocumentImpl document = (org.exist.dom.persistent.DocumentImpl) proxy.getNode();
            final NodeList documentChildren = document.getChildNodes();
            proxies = new NodeProxy[documentChildren.getLength()];
            for (int i = 0; i < documentChildren.getLength(); i++) {
                final org.exist.dom.persistent.NodeImpl<?> child = (org.exist.dom.persistent.NodeImpl<?>) documentChildren.item(i);
                proxies[i] = NodeProxy.wrap(proxy.getExpression(), child);
            }

            // if we are at the top of the in-memory doc, copy over any doctype decl
            if (level == 1 && document.getDoctype() != null) {
                final DocumentTypeImpl documentType = new DocumentTypeImpl(document.getExpression(), doc, -1, document.getDoctype().getName(), document.getDoctype().getPublicId(), document.getDoctype().getSystemId());
                doc.setDoctype(documentType);
            }
        } else {
            proxies = new NodeProxy[1];
            proxies[0] = proxy;
        }

        // add the node proxy(s) as reference node(s)
        int firstProxyNodeNr = -1;
        for (int i = 0; i < proxies.length; i++) {
            final int nodeNr = doc.addNode(NodeImpl.REFERENCE_NODE, level, null);
            doc.addReferenceNode(nodeNr, proxies[i]);
            linkNode(nodeNr);

            if (firstProxyNodeNr == -1) {
                firstProxyNodeNr = nodeNr;
            }
        }
        return firstProxyNodeNr;
    }


    public int addAttribute(final QName qname, final String value) {
        final int lastNode = doc.getLastNode();

        //if(0 < lastNode && doc.nodeKind[lastNode] != Node.ELEMENT_NODE) {
        //Definitely wrong !
        //lastNode = characters(value);
        //} else {
        //lastNode = doc.addAttribute(lastNode, qname, value);
        //}
        final int nodeNr = doc.addAttribute(lastNode, qname, value, getAttribType(qname, Indexer.ATTR_CDATA_TYPE));

        //TODO :
        //1) call linkNode(nodeNr); ?
        //2) is there a relationship between lastNode and nodeNr ?
        return nodeNr;
    }


    /**
     * Create a new text node.
     *
     * @param ch    DOCUMENT ME!
     * @param start DOCUMENT ME!
     * @param len   DOCUMENT ME!
     * @return the node number of the created node
     */
    public int characters(final char[] ch, final int start, final int len) {
        final int lastNode = doc.getLastNode();

        if (lastNode > 0 && level == doc.getTreeLevel(lastNode)) {

            if (doc.getNodeType(lastNode) == Node.TEXT_NODE || doc.getNodeType(lastNode) == Node.CDATA_SECTION_NODE) {

                // if the last node is a Text or CDATA node, we have to append the
                // characters to this node. XML does not allow adjacent text nodes.
                doc.appendChars(lastNode, ch, start, len);
                return lastNode;
            }

            if (doc.getNodeType(lastNode) == NodeImpl.REFERENCE_NODE) {

                // check if the previous node is a reference node. if yes, check if it is a Text or CDATA node
                final int p = doc.alpha[lastNode];

                if (doc.references[p].getNodeType() == Node.TEXT_NODE || doc.references[p].getNodeType() == Node.CDATA_SECTION_NODE) {

                    // found a text node reference. create a new char sequence containing
                    // the concatenated text of both nodes
                    final StringBuilder s = new StringBuilder(doc.references[p].getStringValue());
                    s.append(ch, start, len);
                    doc.replaceReferenceNode(lastNode, s);
                    return lastNode;
                }
                // fall through and add the node below
            }
        }
        final int nodeNr = doc.addNode(Node.TEXT_NODE, level, null);
        doc.addChars(nodeNr, ch, start, len);
        linkNode(nodeNr);
        return nodeNr;
    }


    /**
     * Create a new text node.
     *
     * @param s DOCUMENT ME!
     * @return the node number of the created node, -1 if no node was created
     */
    public int characters(final CharSequence s) {
        if (s == null) {
            return -1;
        }

        final int lastNode = doc.getLastNode();

        if (lastNode > 0 && level == doc.getTreeLevel(lastNode)) {

            if (doc.getNodeType(lastNode) == Node.TEXT_NODE || doc.getNodeType(lastNode) == Node.CDATA_SECTION_NODE) {

                // if the last node is a Text or CDATA node, we have to append the
                // characters to this node. XML does not allow adjacent text nodes.
                doc.appendChars(lastNode, s);
                return lastNode;
            }

            if (doc.getNodeType(lastNode) == NodeImpl.REFERENCE_NODE) {

                final int p = doc.alpha[lastNode];
                // check if the previous node is a reference node. if yes, check if it is a Text or CDATA node

                if (doc.references[p].getNodeType() == Node.TEXT_NODE || doc.references[p].getNodeType() == Node.CDATA_SECTION_NODE) {

                    // found a text node reference. create a new char sequence containing
                    // the concatenated text of both nodes
                    doc.replaceReferenceNode(lastNode, doc.references[p].getStringValue() + s);
                    return lastNode;
                }
                // fall through and add the node below
            }
        }
        final int nodeNr = doc.addNode(Node.TEXT_NODE, level, null);
        doc.addChars(nodeNr, s);
        linkNode(nodeNr);
        return nodeNr;
    }


    public int comment(final CharSequence data) {
        final int nodeNr = doc.addNode(Node.COMMENT_NODE, level, null);
        doc.addChars(nodeNr, data);
        linkNode(nodeNr);
        return nodeNr;
    }


    public int comment(final char[] ch, final int start, final int len) {
        final int nodeNr = doc.addNode(Node.COMMENT_NODE, level, null);
        doc.addChars(nodeNr, ch, start, len);
        linkNode(nodeNr);
        return nodeNr;
    }


    public int cdataSection(final CharSequence data) {
        final int lastNode = doc.getLastNode();

        if (lastNode > 0 && level == doc.getTreeLevel(lastNode)) {

            if (doc.getNodeType(lastNode) == Node.TEXT_NODE || doc.getNodeType(lastNode) == Node.CDATA_SECTION_NODE) {

                // if the last node is a text node, we have to append the
                // characters to this node. XML does not allow adjacent text nodes.
                doc.appendChars(lastNode, data);
                return lastNode;
            }

            if (doc.getNodeType(lastNode) == NodeImpl.REFERENCE_NODE) {

                // check if the previous node is a reference node. if yes, check if it is a text node
                final int p = doc.alpha[lastNode];

                if (doc.references[p].getNodeType() == Node.TEXT_NODE || doc.references[p].getNodeType() == Node.CDATA_SECTION_NODE) {

                    // found a text node reference. create a new char sequence containing
                    // the concatenated text of both nodes
                    doc.replaceReferenceNode(lastNode, doc.references[p].getStringValue() + data);
                    return lastNode;
                }
                // fall through and add the node below
            }
        }
        final int nodeNr = doc.addNode(Node.CDATA_SECTION_NODE, level, null);
        doc.addChars(nodeNr, data);
        linkNode(nodeNr);
        return nodeNr;
    }


    public int processingInstruction(final String target, final String data) {
        final QName qname = new QName(target, null, null);
        final int nodeNr = doc.addNode(Node.PROCESSING_INSTRUCTION_NODE, level, qname);
        doc.addChars(nodeNr, (data == null) ? "" : data);
        linkNode(nodeNr);
        return nodeNr;
    }

    public int namespaceNode(final String prefix, final String uri) {
        final QName qname;
        if(prefix == null || prefix.isEmpty()) {
            qname = new QName(XMLConstants.XMLNS_ATTRIBUTE, uri);
        } else {
            qname = new QName(prefix, uri, XMLConstants.XMLNS_ATTRIBUTE);
        }
        return namespaceNode(qname);
    }

    public int namespaceNode(final QName qname) {
        return namespaceNode(qname, false);
    }

    public int namespaceNode(final QName qname, final boolean checkNS) {
        final int lastNode = doc.getLastNode();
        boolean addNode = true;
        if(doc.nodeName != null) {
            final QName elemQN = doc.nodeName[lastNode];
            if(elemQN != null) {
                final String elemPrefix = (elemQN.getPrefix() == null) ? XMLConstants.DEFAULT_NS_PREFIX : elemQN.getPrefix();
                final String elemNs = (elemQN.getNamespaceURI() == null) ? XMLConstants.NULL_NS_URI : elemQN.getNamespaceURI();
                final String qnPrefix = (qname.getPrefix() == null) ? XMLConstants.DEFAULT_NS_PREFIX : qname.getPrefix();
                if (checkNS
                    && XMLConstants.DEFAULT_NS_PREFIX.equals(elemPrefix)
                    && XMLConstants.NULL_NS_URI.equals(elemNs)
                    && XMLConstants.DEFAULT_NS_PREFIX.equals(qnPrefix)
                    && XMLConstants.XMLNS_ATTRIBUTE.equals(qname.getLocalPart())) {

                    throw new DOMException(
                        DOMException.NAMESPACE_ERR,
                        "Cannot output a namespace node for the default namespace when the element is in no namespace."
                    );
                }
                if(elemPrefix.equals(qname.getLocalPart()) && (elemQN.getNamespaceURI() != null)) {
                    addNode = false;
                }
            }
        }
        return (addNode ? doc.addNamespace(lastNode, qname) : -1);
    }


    public int documentType(final String publicId, final String systemId) {
//      int nodeNr = doc.addNode(Node.DOCUMENT_TYPE_NODE, level, null);
//      doc.addChars(nodeNr, data);
//      linkNode(nodeNr);
//      return nodeNr;
        return -1;
    }


    public void documentType(final String name, final String publicId, final String systemId) {
    }


    private void linkNode(final int nodeNr) {
        final int prevNr = prevNodeInLevel[level];

        if(prevNr > -1) {
            doc.next[prevNr] = nodeNr;
        }
        doc.next[nodeNr] = prevNodeInLevel[level - 1];
        prevNodeInLevel[level] = nodeNr;
    }


    public void setReplaceAttributeFlag(final boolean replaceAttribute) {
        doc.replaceAttribute = replaceAttribute;
    }

    public void setDefaultNamespace(final String defaultNamespaceURI) {
        this.defaultNamespaceURI = defaultNamespaceURI;
    }

    private String getDefaultNamespace() {
        // guard against someone setting null as the defaultNamespaceURI
        return defaultNamespaceURI == null ? XMLConstants.NULL_NS_URI : defaultNamespaceURI;
    }
}
