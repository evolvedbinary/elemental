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
 */
package org.exist.dom.memtree;

import com.googlecode.junittoolbox.ParallelRunner;
import org.apache.xerces.dom.AttrNSImpl;
import org.exist.Namespaces;
import org.exist.util.ExistSAXParserFactory;
import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Adam Retter <adam@evolvedbinary.com>
 */
@RunWith(ParallelRunner.class)
public class DocumentImplTest {

    private static final String DOC_WITH_NAMESPACES =
            "<repo:meta xmlns=\"http://exist-db.org/xquery/repo\" xmlns:repo=\"http://exist-db.org/xquery/repo\">\n" +
            "    <repo:description>some description or other</repo:description>\n" +
            "</repo:meta>";

    @Test
    public void checkNamespaces_xerces() throws IOException, ParserConfigurationException, SAXException {
        final Document doc;
        try(final InputStream is = new UnsynchronizedByteArrayInputStream(DOC_WITH_NAMESPACES.getBytes(UTF_8))) {
            doc = parseXerces(is);
        }

        final Element elem = doc.getDocumentElement();
        final NamedNodeMap attrs = elem.getAttributes();
        assertEquals(2, attrs.getLength());

        int index = 0;

        final Attr attr1 = (Attr)attrs.item(index++);
        assertEquals(Node.ATTRIBUTE_NODE, attr1.getNodeType());
        assertTrue(attr1 instanceof AttrNSImpl);
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attr1.getNamespaceURI());
        assertEquals(null, attr1.getPrefix());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, attr1.getLocalName());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, attr1.getNodeName());
        assertEquals("http://exist-db.org/xquery/repo", attr1.getValue());

        final Attr attr2 = (Attr)attrs.item(index++);
        assertEquals(Node.ATTRIBUTE_NODE, attr2.getNodeType());
        assertTrue(attr2 instanceof AttrNSImpl);
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attr2.getNamespaceURI());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, attr2.getPrefix());
        assertEquals("repo", attr2.getLocalName());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE + ":repo", attr2.getNodeName());
        assertEquals("http://exist-db.org/xquery/repo", attr2.getValue());

    }

    @Test
    public void checkNamespaces_saxon() throws IOException, ParserConfigurationException, SAXException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        final Document doc;
        try(final InputStream is = new UnsynchronizedByteArrayInputStream(DOC_WITH_NAMESPACES.getBytes(UTF_8))) {
            doc = parseSaxon(is);
        }

        final Element elem = doc.getDocumentElement();
        final NamedNodeMap attrs = elem.getAttributes();
        assertEquals(3, attrs.getLength());

        int index = 0;

        final Attr attr1 = (Attr)attrs.item(index++);
        assertEquals(Node.ATTRIBUTE_NODE, attr1.getNodeType());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attr1.getNamespaceURI());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, attr1.getPrefix());
        assertEquals(XMLConstants.XML_NS_PREFIX, attr1.getLocalName());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE + ":" + XMLConstants.XML_NS_PREFIX, attr1.getNodeName());
        assertEquals(XMLConstants.XML_NS_URI, attr1.getValue());

        final Attr attr2 = (Attr)attrs.item(index++);
        assertEquals(Node.ATTRIBUTE_NODE, attr2.getNodeType());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attr2.getNamespaceURI());
        assertEquals(null, attr2.getPrefix());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, attr2.getLocalName());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, attr2.getNodeName());
        assertEquals("http://exist-db.org/xquery/repo", attr2.getValue());

        final Attr attr3 = (Attr)attrs.item(index++);
        assertEquals(Node.ATTRIBUTE_NODE, attr3.getNodeType());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attr3.getNamespaceURI());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, attr3.getPrefix());
        assertEquals("repo", attr3.getLocalName());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE + ":repo", attr3.getNodeName());
        assertEquals("http://exist-db.org/xquery/repo", attr3.getValue());
    }

    @Test
    public void checkNamespaces_exist() throws IOException, SAXException, ParserConfigurationException {
        final DocumentImpl doc;
        try(final InputStream is = new UnsynchronizedByteArrayInputStream(DOC_WITH_NAMESPACES.getBytes(UTF_8))) {
            doc = parseExist(is);
        }

        final ElementImpl elem = (ElementImpl)doc.getDocumentElement();
        final NamedNodeMap attrs = elem.getAttributes();
        assertEquals(1, attrs.getLength());
//        assertEquals(2, attrs.getLength());

        int index = 0;

        final Attr attr1 = (Attr)attrs.item(index++);
        assertEquals(NodeImpl.NAMESPACE_NODE, attr1.getNodeType());
        assertTrue(attr1 instanceof NamespaceNode);
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attr1.getNamespaceURI());
        assertEquals(null, attr1.getPrefix());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, attr1.getLocalName());
        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, attr1.getNodeName());
        assertEquals("http://exist-db.org/xquery/repo", attr1.getValue());

//        final Attr attr2 = (Attr)attrs.item(index++);
//        assertEquals(NodeImpl.NAMESPACE_NODE, attr2.getNodeType());
//        assertTrue(attr2 instanceof NamespaceNode);
//        assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, attr2.getNamespaceURI());
//        assertEquals(XMLConstants.XMLNS_ATTRIBUTE, attr2.getPrefix());
//        assertEquals("repo", attr2.getLocalName());
//        assertEquals(XMLConstants.XMLNS_ATTRIBUTE + ":repo", attr2.getNodeName());
//        assertEquals("http://exist-db.org/xquery/repo", attr2.getValue());
    }

    private Document parseXerces(final InputStream is) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        assertTrue(builder.isNamespaceAware());
        return builder.parse(is);
    }

    private Document parseSaxon(final InputStream is) throws IOException, SAXException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        final Class clazz = Class.forName("net.sf.saxon.dom.DocumentBuilderImpl");
        final DocumentBuilder builder = (DocumentBuilder)clazz.newInstance();
        assertTrue(builder.isNamespaceAware());
        return builder.parse(is);
    }

    private DocumentImpl parseExist(final InputStream is) throws ParserConfigurationException, SAXException, IOException {
        final SAXParserFactory saxParserFactory = ExistSAXParserFactory.getSAXParserFactory();
        final SAXParser saxParser  = saxParserFactory.newSAXParser();
        final XMLReader reader = saxParser.getXMLReader();
        reader.setFeature("http://xml.org/sax/features/namespaces", true);
        assertTrue(saxParser.isNamespaceAware());
        final InputSource src = new InputSource(is);
        final SAXAdapter adapter = new SAXAdapter();
        reader.setContentHandler(adapter);

        reader.setProperty(Namespaces.SAX_LEXICAL_HANDLER, adapter);
        reader.parse(src);
        return adapter.getDocument();
    }
}
