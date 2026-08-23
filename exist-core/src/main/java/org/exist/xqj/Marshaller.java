/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
package org.exist.xqj;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.dom.QName;
import org.exist.dom.memtree.*;
import org.exist.storage.DBBroker;
import org.exist.storage.ElementValue;
import org.exist.xquery.Expression;
import org.exist.xquery.NameTest;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.array.ArrayType;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.util.StringUtil.nullIfEmpty;


/**
 * A utility class that provides marshalling services for external variables and methods 
 * to create DOM Nodes from streamed representation.
 * 
 * @author Wolfgang Meier
 *
 */
public class Marshaller {

    public final static String NAMESPACE = "http://exist-db.org/xquery/types/serialized";
    public final static String PREFIX = "sx";

    private final static Properties DEFAULT_OUTPUT_PROPERTIES = new Properties();

    private final static String VALUE_ELEMENT = "value";
    private final static String VALUE_ELEMENT_PREFIXED_NAME = PREFIX + ":value";
    
    private final static String SEQ_ELEMENT = "sequence";
    private final static String SEQ_ELEMENT_PREFIXED_NAME = PREFIX + ":sequence";
    
    private final static String ATTR_TYPE = "type";
    private final static String ATTR_ITEM_TYPE = "item-type";

    private final static String ATTR_NAME = "name";

    public final static QName SEQUENCE_ELEMENT_QNAME = new QName(SEQ_ELEMENT, NAMESPACE, PREFIX);
    public final static QName VALUE_ELEMENT_QNAME = new QName(VALUE_ELEMENT,  NAMESPACE, PREFIX);
    public final static QName ENTRY_ELEMENT_QNAME = new QName("entry", NAMESPACE, PREFIX);
    public final static QName KEY_ELEMENT_QNAME = new QName("key", NAMESPACE, PREFIX);
    
    /**
     * Marshall a sequence in an xml based string representation.
     *
     * @param broker the database broker
     * @param seq Sequence to be marshalled
     * @param handler Content handler for building the resulting string
     *
     * @throws XPathException if an XPath error occurs
     * @throws SAXException if a SAX parsing exception occurs
     */
    public static void marshall(final DBBroker broker, final Sequence seq, final ContentHandler handler)
            throws XPathException, SAXException {
        final AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", ATTR_ITEM_TYPE, ATTR_ITEM_TYPE, "CDATA", Type.getTypeName(seq.getItemType()));
        handler.startElement(NAMESPACE, SEQ_ELEMENT, SEQ_ELEMENT_PREFIXED_NAME, attrs);
        for (final SequenceIterator i = seq.iterate(); i.hasNext(); ) {
            marshallItem(broker, i.nextItem(), handler);
        }
        handler.endElement(NAMESPACE, SEQ_ELEMENT, SEQ_ELEMENT_PREFIXED_NAME);
    }
    
    
    /**
     * Marshall the items of a sequence in  an xml based string representation.
     *
     * @param broker the database broker
     * @param seq Sequence which items are to be marshalled
     * @param start index of first item to be marshalled
     * @param howmany number of items following and including the first to be marshalled
     * @param handler Content handler for building the resulting string
     *
     * @throws XPathException if an XPath error occurs
     * @throws SAXException if a SAX parsing exception occurs
     */
    public static void marshall(final DBBroker broker, final Sequence seq, final int start, final int howmany,
            final ContentHandler handler) throws XPathException, SAXException {
        final AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", ATTR_ITEM_TYPE, ATTR_ITEM_TYPE, "CDATA", Type.getTypeName(seq.getItemType()));
        handler.startElement(NAMESPACE, SEQ_ELEMENT, SEQ_ELEMENT_PREFIXED_NAME, attrs);
        for (int i = start; i < howmany && i < seq.getItemCount(); i++ ) {
        	
            marshallItem(broker, seq.itemAt(i), handler);
        }
        handler.endElement(NAMESPACE, SEQ_ELEMENT, SEQ_ELEMENT_PREFIXED_NAME);
    }

    /**
     * Marshall an item in an xml based string representation.
     *
     * @param broker the database broker
     * @param item Sequence(or Item) to me marshalled
     * @param handler Content handler for building the resulting string
     *
     *
     * @throws XPathException if an XPath error occurs
     * @throws SAXException if a SAX parsing exception occurs
     */
    public static void marshallItem(final DBBroker broker, final Item item, final ContentHandler handler)
        throws SAXException, XPathException {
        marshallItem(broker, item, handler, DEFAULT_OUTPUT_PROPERTIES);
    }

    /**
     * Marshall an item in an xml based string representation.
     *
     * @param broker the database broker
     * @param item Sequence(or Item) to me marshalled
     * @param handler Content handler for building the resulting string
     * @param outputProperties any output properties for the Serializer
     *
     * @throws XPathException if an XPath error occurs
     * @throws SAXException if a SAX parsing exception occurs
     */
    public static void marshallItem(final DBBroker broker, final Item item, final ContentHandler handler, final Properties outputProperties)
            throws SAXException, XPathException {
        final AttributesImpl attrs = new AttributesImpl();
        int type = item.getType();
        if (type == Type.NODE) {
            final short nodeType = ((NodeValue)item).getNode().getNodeType();
            type = Type.fromDomNodeType(nodeType);
        }
        attrs.addAttribute("", ATTR_TYPE, ATTR_TYPE, "CDATA", Type.getTypeName(type));
        if (Type.subTypeOf(item.getType(), Type.NODE)) {
            handler.startElement(NAMESPACE, VALUE_ELEMENT, VALUE_ELEMENT_PREFIXED_NAME, attrs);
            final NodeValue nv = (NodeValue) item;
            nv.toSAX(broker, handler, outputProperties);
            handler.endElement(NAMESPACE, VALUE_ELEMENT, VALUE_ELEMENT_PREFIXED_NAME);
        } else {
            handler.startElement(NAMESPACE, VALUE_ELEMENT, VALUE_ELEMENT_PREFIXED_NAME, attrs);
            final String value = item.getStringValue();
            handler.characters(value.toCharArray(), 0, value.length());
            handler.endElement(NAMESPACE, VALUE_ELEMENT, VALUE_ELEMENT_PREFIXED_NAME);
        }
    }

    public static Sequence demarshall(final InputStream is) throws XMLStreamException, XPathException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        final XMLStreamReader parser = factory.createXMLStreamReader(is);
        return demarshall(parser);
    }

    public static Sequence demarshall(final Reader reader) throws XMLStreamException, XPathException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        final XMLStreamReader parser = factory.createXMLStreamReader(reader);
        return demarshall(parser);
    }
    
    public static Sequence demarshall(final Node n) throws XMLStreamException, XPathException {
    	final DOMSource source = new DOMSource(n, null);
    	final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        
        final XMLStreamReader parser = factory.createXMLStreamReader(source);
    	return demarshall(parser);
    	
    }

    public static Sequence demarshall(final XMLStreamReader parser) throws XMLStreamException, XPathException {
        int event = parser.next();
        while (event != XMLStreamConstants.START_ELEMENT) {
            event = parser.next();
        }

        if (!NAMESPACE.equals(parser.getNamespaceURI())) {
            throw new XMLStreamException("Root element is not in the correct namespace. Expected: " + NAMESPACE);
        }

        if (!SEQ_ELEMENT.equals(parser.getLocalName())) {
            throw new XMLStreamException("Root element should be a " + SEQ_ELEMENT_PREFIXED_NAME);
        }

        Sequence result = Sequence.EMPTY_SEQUENCE;
        while ((event = parser.next()) != XMLStreamConstants.END_DOCUMENT) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT :
                    if (NAMESPACE.equals(parser.getNamespaceURI()) && VALUE_ELEMENT.equals(parser.getLocalName())) {
                        int type = Type.ITEM;

                        String typeName = null;
                        // scan through attributes instead of direct lookup to work around issue in xerces
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (ATTR_TYPE.equals(parser.getAttributeLocalName(i))) {
                                typeName = parser.getAttributeValue(i);
                                break;
                            }
                        }
                        if (typeName != null && !typeName.isEmpty()) {
                            type = Type.getType(typeName);
                        }

                        final Item item;
                        if (Type.subTypeOf(type, Type.NODE)) {
                            item = streamToDOM(type, parser, null);
                        } else {
                            item = new StringValue(null, parser.getElementText()).convertTo(type);
                        }

                        if (result == Sequence.EMPTY_SEQUENCE) {
                            result = new ValueSequence();
                        }
                        result.add(item);
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT :
                    if (NAMESPACE.equals(parser.getNamespaceURI()) && SEQ_ELEMENT.equals(parser.getLocalName())) {
                        return result;
                    }
                    break;
            }
        }

        return result;
    }

    public static Sequence demarshall(final XQueryContext context, final NodeImpl node) throws XMLStreamException, XPathException {
        return demarshallSequence(context, node);
    }

    private static Sequence demarshallSequence(final XQueryContext context, final NodeImpl node) throws XMLStreamException, XPathException {
        final String ns = node.getNamespaceURI();
        if (ns == null || !NAMESPACE.equals(ns)) {
            throw new XMLStreamException("Sequence element is not in the correct namespace. Expected: " + NAMESPACE);
        }
        if (!SEQ_ELEMENT.equals(node.getLocalName())) {
            throw new XMLStreamException("Element should be a " + SEQ_ELEMENT_PREFIXED_NAME);
        }

        return demarshallValues(context, node);
    }

    private static Sequence demarshallValues(final XQueryContext context, final NodeImpl node) throws XMLStreamException, XPathException {
        final InMemoryNodeSet sxValues = new InMemoryNodeSet();
        node.selectChildren(new NameTest(Type.ELEMENT, VALUE_ELEMENT_QNAME), sxValues);

        if (sxValues.isEmpty()) {
            return Sequence.EMPTY_SEQUENCE;
        }

        final ValueSequence result = new ValueSequence(sxValues.size());
        for (final SequenceIterator itSxValue = sxValues.iterate(); itSxValue.hasNext();) {
            final ElementImpl sxValue = (ElementImpl) itSxValue.nextItem();
            final Item item = demarshallValue(context, sxValue);
            result.add(item);
        }
        return result;
    }

    public static Item demarshallValue(final XQueryContext context, final ElementImpl sxValue) throws XMLStreamException, XPathException {
        int type = Type.ITEM;
        final String typeName = sxValue.getAttribute(ATTR_TYPE);
        if (!typeName.isEmpty()) {
            type = Type.getType(typeName);
        }

        @Nullable String attrNameString = null;
        if (sxValue instanceof Element) {
            attrNameString = nullIfEmpty(sxValue.getAttribute(ATTR_NAME));
        }

        final InMemoryNodeSet sxSequences = new InMemoryNodeSet();
        sxValue.selectChildren(new NameTest(Type.ELEMENT, SEQUENCE_ELEMENT_QNAME), sxSequences);

        final InMemoryNodeSet sxEntries = new InMemoryNodeSet();
        sxValue.selectChildren(new NameTest(Type.ELEMENT, ENTRY_ELEMENT_QNAME), sxEntries);

        @Nullable Node item = null;
        item = sxValue.getFirstChild();

        if (type == Type.ATTRIBUTE || (type == Type.ITEM && attrNameString != null)) {
            if (attrNameString.isEmpty()) {
                throw new XMLStreamException("sx:value must contain a name attribute if type is " + typeName);
            }

            final String attrPrefix;
            final String attrNamespace;
            final String attrLocalName;
            final int colonSep = attrNameString.indexOf(':');
            if (colonSep > -1) {
                attrPrefix = attrNameString.substring(0, colonSep);
                attrNamespace = item.lookupNamespaceURI(attrPrefix);
                if (attrNamespace == null) {
                    throw new XMLStreamException("sx:value's name attribute contains the QName prefix '" +  attrPrefix + "' for which the namespace has not been declared if type is " + typeName);
                }
                attrLocalName = attrNameString.substring(colonSep + 1);
            } else {
                attrPrefix = XMLConstants.DEFAULT_NS_PREFIX;
                attrNamespace = XMLConstants.NULL_NS_URI;
                attrLocalName = attrNameString;
            }

            final QName attrName = new QName(attrLocalName, attrNamespace, attrPrefix, ElementValue.ATTRIBUTE);
            final MemTreeBuilder builder = new MemTreeBuilder(context);
            builder.startDocument();
            final int attrNodeNumber = builder.addAttribute(attrName, sxValue.getTextContent());
            builder.endDocument();
            final AttrImpl attr = (AttrImpl) builder.getDocument().getAttribute(attrNodeNumber);
            return attr;

        } else if (Type.subTypeOf(type, Type.NODE)) {

            switch (type) {
                case Type.ELEMENT:
                    do {
                        if (item.getNodeType() == Node.DOCUMENT_NODE) {
                            item = ((DocumentImpl) item).getDocumentElement();
                        }

                        if (item.getNodeType() == Node.ELEMENT_NODE) {
                            return (ElementImpl) item;
                        }

                        item = item.getNextSibling();
                    } while (item != null);

                    throw new XMLStreamException("sx:value must contain an Element if type is " + typeName);


                case Type.COMMENT:
                    do {
                        if (item.getNodeType() == Node.COMMENT_NODE) {
                            return (CommentImpl) item;
                        }
                        item = item.getNextSibling();
                    } while (item != null);

                    throw new XMLStreamException("sx:value must contain a Comment node if type is " + typeName);


                case Type.PROCESSING_INSTRUCTION:
                    do {
                        if (item.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                            return (ProcessingInstructionImpl) item;
                        }
                        item = item.getNextSibling();
                    } while (item != null);

                    throw new XMLStreamException("sx:value must contain a Processing Instruction node if type is " + typeName);


                case Type.TEXT:
                    do {
                        if (item.getNodeType() == Node.TEXT_NODE) {
                            return (TextImpl) item;
                        }
                        item = item.getNextSibling();
                    } while (item != null);


                case Type.DOCUMENT:
                default:
                    do {
                        if (item.getNodeType() == Node.DOCUMENT_NODE || item.getNodeType() == Node.ELEMENT_NODE) {
                            final DocumentBuilderReceiver receiver = new DocumentBuilderReceiver(((NodeImpl) item).getExpression());
                            try {
                                receiver.startDocument();
                                ((NodeImpl) item).copyTo(null, receiver);
                                receiver.endDocument();
                            } catch (final SAXException e) {
                                throw new XPathException(item != null ? ((NodeImpl) item).getExpression() : null, "Error while demarshalling node: " + e.getMessage(), e);
                            }
                            return (NodeImpl) receiver.getDocument();
                        }
                        item = item.getNextSibling();
                    } while (item != null);

                    throw new XMLStreamException("sx:value must contain a Document or Element if type is " + typeName);
            }

        } else if (type == Type.ITEM && item.getNodeType() != Node.TEXT_NODE) {
            // item() type requested and we have been given a node which is not a text() node
            return (NodeImpl) item;

        } else if (type == Type.ARRAY_ITEM || (type == Type.ITEM && !sxSequences.isEmpty())) {
            // array(*) type
            final List<Sequence> arrayValues = new ArrayList<>();
            for (final SequenceIterator itSxSequence = sxSequences.iterate(); itSxSequence.hasNext();) {
                final ElementImpl sxSequence = (ElementImpl) itSxSequence.nextItem();
                final Sequence arrayValue = demarshallSequence(context, sxSequence);
                arrayValues.add(arrayValue);
            }
            return new ArrayType(context, arrayValues);

        } else if (type == Type.MAP_ITEM || (type == Type.ITEM && !sxEntries.isEmpty())) {
            // map(*) type
            final List<Tuple2<AtomicValue, Sequence>> mapEntries = new ArrayList<>();

            for (final SequenceIterator itSxEntry = sxEntries.iterate(); itSxEntry.hasNext();) {
                final ElementImpl sxEntry = (ElementImpl) itSxEntry.nextItem();
                final NodeList entryKeys = sxEntry.getElementsByTagNameNS(KEY_ELEMENT_QNAME.getNamespaceURI(), KEY_ELEMENT_QNAME.getLocalPart());
                final Element entryKey = (Element) entryKeys.item(0);
                final int keyType = Type.getType(entryKey.getAttribute(ATTR_TYPE));
                final String keyStr = entryKey.getTextContent();
                final AtomicValue key = new StringValue(keyStr).convertTo(keyType);
                final NodeList entrySequences = sxEntry.getElementsByTagNameNS(SEQUENCE_ELEMENT_QNAME.getNamespaceURI(), SEQUENCE_ELEMENT_QNAME.getLocalPart());
                final ElementImpl entrySequence = (ElementImpl) entrySequences.item(0);
                final Sequence value = demarshallSequence(context, entrySequence);
                mapEntries.add(Tuple(key, value));
            }
            return new MapType(context, null, mapEntries);

        } else {
            // specific non-node type or text()
            final StringBuilder data = new StringBuilder();
            do {
                if (item.getNodeType() == Node.TEXT_NODE || item.getNodeType() == Node.CDATA_SECTION_NODE) {
                    data.append(item.getNodeValue());
                }
                item = item.getNextSibling();
            } while (item != null);

            return new StringValue(data.toString()).convertTo(type);
        }
    }

    /**
     * Creates an Item from a streamed representation.
     *
     * @param rootType the type of the root node
     * @param parser Parser to read xml elements from
     * @param expression the expression from which the item derives
     * @return item the item
     *
     * @throws XMLStreamException if an error occurs during streaming.
     */
    public static Item streamToDOM(int rootType, XMLStreamReader parser, final Expression expression) throws XMLStreamException {
        final MemTreeBuilder builder = new MemTreeBuilder(expression);
        builder.startDocument();
        int event;
        boolean finish = false;
        while ((event = parser.next()) != XMLStreamConstants.END_DOCUMENT) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT :
                    final AttributesImpl attribs = new AttributesImpl();
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        final javax.xml.namespace.QName qn = parser.getAttributeName(i);
                        attribs.addAttribute(qn.getNamespaceURI(), qn.getLocalPart(), qn.getPrefix() + ':' + qn.getLocalPart(),
                                parser.getAttributeType(i), parser.getAttributeValue(i));
                    }
                   builder.startElement(QName.fromJavaQName(parser.getName()), attribs);
//                    for (int i = 0; i < parser.getNamespaceCount(); i++) {
//                        builder.namespaceNode(parser.getNamespacePrefix(i), parser.getNamespaceURI(i));
//                    }
                    break;
                case XMLStreamConstants.END_ELEMENT :
                    if (NAMESPACE.equals(parser.getNamespaceURI()) && VALUE_ELEMENT.equals(parser.getLocalName()))
                        {finish = true;}
                    else
                        {builder.endElement();}
                    break;
                case XMLStreamConstants.CHARACTERS :
                    builder.characters(parser.getText());
                    break;

                case XMLStreamConstants.COMMENT:
                    builder.comment(parser.getText());
                    break;

                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    builder.processingInstruction(parser.getPITarget(), parser.getPIData());
                    break;

                case XMLStreamConstants.CDATA:
                    builder.cdataSection(parser.getText());
                    break;
            }
            if (finish) {break;}
        }
        builder.endDocument();
        if (rootType == Type.DOCUMENT)
            {return builder.getDocument();}
        else if (rootType == Type.ELEMENT)
            {return (NodeImpl) builder.getDocument().getDocumentElement();}
        else
            {return (NodeImpl) builder.getDocument().getFirstChild();}
    }
    
    /**
     * Creates a node from a string representation.
     *
     * @param content the content
     * @return node the result node.
     *
     * @throws XMLStreamException if an error occurs during streaming.
     */
    public static Node streamToNode(String content) throws XMLStreamException {
    	final StringReader reader = new StringReader(content);
    	return streamToNode(reader, null);
    }
    
    
   
    /**
     * Creates a node from a streamed representation.
     *
     * @param reader the reader.
     * @return item the result item.
     *
     * @throws XMLStreamException if an error occurs during streaming.
     */
    public static Node streamToNode(Reader reader) throws XMLStreamException {
        return streamToNode(reader, null);
    }
   
    /**
     * Creates a node from a streamed representation.
     *
     * @param reader the reader.
     * @param expression the expression from which te node derives
     * @return item the result item.
     *
     * @throws XMLStreamException if an error occurs during streaming.
     */
    public static Node streamToNode(Reader reader, final Expression expression) throws XMLStreamException {
    	final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        final XMLStreamReader parser = factory.createXMLStreamReader(reader);
        final MemTreeBuilder builder = new MemTreeBuilder(expression);
        builder.startDocument();
        int event;
        boolean finish = false;
        while ((event = parser.next()) != XMLStreamConstants.END_DOCUMENT) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT :
                    final AttributesImpl attribs = new AttributesImpl();
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        final javax.xml.namespace.QName qn = parser.getAttributeName(i);
                        attribs.addAttribute(qn.getNamespaceURI(), qn.getLocalPart(), qn.getPrefix() + ':' + qn.getLocalPart(),
                                parser.getAttributeType(i), parser.getAttributeValue(i));
                    }
                   builder.startElement(QName.fromJavaQName(parser.getName()), attribs);
//                    for (int i = 0; i < parser.getNamespaceCount(); i++) {
//                        builder.namespaceNode(parser.getNamespacePrefix(i), parser.getNamespaceURI(i));
//                    }
                    break;
                case XMLStreamConstants.END_ELEMENT :
                    if (NAMESPACE.equals(parser.getNamespaceURI()) && VALUE_ELEMENT.equals(parser.getLocalName()))
                        {finish = true;}
                    else
                        {builder.endElement();}
                    break;
                case XMLStreamConstants.CHARACTERS :
                    builder.characters(parser.getText());
                    break;
            }
            if (finish) {break;}
        }
        builder.endDocument();
        return builder.getDocument().getDocumentElement();
    }
}


