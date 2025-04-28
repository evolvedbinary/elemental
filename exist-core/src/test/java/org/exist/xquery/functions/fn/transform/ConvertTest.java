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
package org.exist.xquery.functions.fn.transform;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.*;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.dom.persistent.NodeProxy;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Item;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.XMLConstants;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ConvertTest {

    @ClassRule
    public static ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    private static final XmldbURI TEST_COLLECTION_URI = XmldbURI.create("/db/covert-test");

    private static final Configuration SAXON_CONFIGURATION = new Configuration();
    private static final Processor SAXON_PROCESSOR = new Processor(SAXON_CONFIGURATION);

    static final Convert.ToSaxon toSaxon = new Convert.ToSaxon() {
        @Override
        DocumentBuilder newDocumentBuilder() {
            return SAXON_PROCESSOR.newDocumentBuilder();
        }
    };

    @Test
    public void memtreeDocumentToSaxon() throws XPathException {
        // create an Elemental in-memory DOM document
        final org.exist.dom.memtree.DocumentImpl document = getInMemoryDocument(false);

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) document);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertDocument(xdmNode);
    }

    @Test
    public void memtreeDocumentExplicitToSaxon() throws XPathException {
        // create an Elemental in-memory DOM document
        final org.exist.dom.memtree.DocumentImpl document = getInMemoryDocument(true);

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) document);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertDocument(xdmNode);
    }

    @Test
    public void persistentDocumentToSaxon() throws XPathException, LockException, PermissionDeniedException, EXistException, IOException, SAXException {
        // create an Elemental persistent DOM document
        final org.exist.dom.persistent.DocumentImpl document = getPersistentDocument();
        final NodeProxy nodeProxy = NodeProxy.wrap(null, document);

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) nodeProxy);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertDocument(xdmNode);
    }

    @Test
    public void persistentElementFromDocumentToSaxon() throws XPathException, LockException, PermissionDeniedException, EXistException, IOException, SAXException {
        // create an Elemental in-memory DOM document
        final org.exist.dom.persistent.DocumentImpl document = getPersistentDocument();
        final org.exist.dom.persistent.ElementImpl element = (org.exist.dom.persistent.ElementImpl) document.getDocumentElement();
        final NodeProxy nodeProxy = NodeProxy.wrap(null, element);

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) nodeProxy);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertFirstElement(xdmNode);
        assertEquals(document, element.getOwnerDocument());
    }

    @Test
    public void persistentElementFromElementToSaxon() throws XPathException, LockException, PermissionDeniedException, EXistException, IOException, SAXException {
        // create an Elemental in-memory DOM document
        final org.exist.dom.persistent.DocumentImpl document = getPersistentDocument();
        final org.exist.dom.persistent.ElementImpl element = (org.exist.dom.persistent.ElementImpl) document.getDocumentElement();
        final org.exist.dom.persistent.ElementImpl nestedElement = (org.exist.dom.persistent.ElementImpl) element.getFirstChild();
        final NodeProxy nodeProxy = NodeProxy.wrap(null, nestedElement);

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) nodeProxy);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertNestedElement(xdmNode);
        assertEquals(document, element.getOwnerDocument());
    }

    @Test
    public void memtreeElementFromDocumentToSaxon() throws XPathException {
        // create an Elemental in-memory DOM document
        final org.exist.dom.memtree.DocumentImpl document = getInMemoryDocument(false);
        final org.exist.dom.memtree.ElementImpl element = (org.exist.dom.memtree.ElementImpl) document.getDocumentElement();

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) element);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertFirstElement(xdmNode);
        assertEquals(document, element.getOwnerDocument());
    }

    @Test
    public void memtreeElementFromDocumentExplicitToSaxon() throws XPathException {
        // create an Elemental in-memory DOM document
        final org.exist.dom.memtree.DocumentImpl document = getInMemoryDocument(true);
        final org.exist.dom.memtree.ElementImpl element = (org.exist.dom.memtree.ElementImpl) document.getDocumentElement();

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) element);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertFirstElement(xdmNode);
        assertEquals(document, element.getOwnerDocument());
    }

    @Test
    public void memtreeElementToSaxon() throws XPathException {
        final org.exist.dom.memtree.ElementImpl element = getFirstInMemoryElement();

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) element);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertFirstElement(xdmNode);
    }

    @Test
    public void memtreeElementFromElementToSaxon() throws XPathException {
        final org.exist.dom.memtree.ElementImpl element = getNestedInMemoryElement();

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) element);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertNestedElement(xdmNode);
    }

    @Test
    public void memtreeAttributeFromDocumentToSaxon() throws XPathException {
        // create an Elemental in-memory DOM document
        final org.exist.dom.memtree.DocumentImpl document = getInMemoryDocument(false);
        final org.exist.dom.memtree.ElementImpl element = (org.exist.dom.memtree.ElementImpl) document.getDocumentElement();
        final org.exist.dom.memtree.AttrImpl attribute = (org.exist.dom.memtree.AttrImpl) element.getAttributeNode("b");

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) attribute);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertAttribute("2", xdmNode);
        assertEquals(document, element.getOwnerDocument());
    }

    @Test
    public void memtreeAttributeFromDocumentExplicitToSaxon() throws XPathException {
        // create an Elemental in-memory DOM document
        final org.exist.dom.memtree.DocumentImpl document = getInMemoryDocument(true);
        final org.exist.dom.memtree.ElementImpl element = (org.exist.dom.memtree.ElementImpl) document.getDocumentElement();
        final org.exist.dom.memtree.AttrImpl attribute = (org.exist.dom.memtree.AttrImpl) element.getAttributeNode("b");

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) attribute);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertAttribute("2", xdmNode);
        assertEquals(document, element.getOwnerDocument());
    }


    @Test
    public void memtreeAttributeToSaxon() throws XPathException {
        final org.exist.dom.memtree.AttrImpl attribute = getFirstInMemoryAttribute();

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) attribute);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertAttribute("1", xdmNode);
    }

    @Test
    public void memtreeAttributeFromElementToSaxon() throws XPathException {
        final org.exist.dom.memtree.AttrImpl attribute = getNestedInMemoryAttribute();

        // convert to Saxon
        final XdmValue xdmValue = toSaxon.of((Item) attribute);
        final XdmNode xdmNode = assertXdmValueToXdmNode(xdmValue);

        // check the properties of the saxon typed value
        assertAttribute("3", xdmNode);
    }

    private static org.exist.dom.memtree.DocumentImpl getInMemoryDocument(final boolean explicitlyCreateDocument) {
        final MemTreeBuilder memTreeBuilder = new MemTreeBuilder();
        memTreeBuilder.startDocument(explicitlyCreateDocument);

        addInMemoryElements(memTreeBuilder);

        memTreeBuilder.endDocument();
        return memTreeBuilder.getDocument();
    }

    private static org.exist.dom.memtree.ElementImpl getFirstInMemoryElement() {
        final MemTreeBuilder memTreeBuilder = new MemTreeBuilder();
        memTreeBuilder.startDocument(false);

        final Tuple2<Integer, Integer> elementNodeNumbers = addInMemoryElements(memTreeBuilder);

        memTreeBuilder.endDocument();
        return (org.exist.dom.memtree.ElementImpl) memTreeBuilder.getDocument().getNode(elementNodeNumbers._1);
    }

    private static org.exist.dom.memtree.ElementImpl getNestedInMemoryElement() {
        final MemTreeBuilder memTreeBuilder = new MemTreeBuilder();
        memTreeBuilder.startDocument(false);

        final Tuple2<Integer, Integer> elementNodeNumbers = addInMemoryElements(memTreeBuilder);

        memTreeBuilder.endDocument();
        return (org.exist.dom.memtree.ElementImpl) memTreeBuilder.getDocument().getNode(elementNodeNumbers._2);
    }

    private static org.exist.dom.memtree.AttrImpl getFirstInMemoryAttribute() {
        final MemTreeBuilder memTreeBuilder = new MemTreeBuilder();
        memTreeBuilder.startDocument(false);

        final Tuple2<Integer, Integer> elementNodeNumbers = addInMemoryElements(memTreeBuilder);

        memTreeBuilder.endDocument();
        return (org.exist.dom.memtree.AttrImpl) memTreeBuilder.getDocument().getNode(elementNodeNumbers._1).getAttributes().getNamedItem("a");
    }

    private static org.exist.dom.memtree.AttrImpl getNestedInMemoryAttribute() {
        final MemTreeBuilder memTreeBuilder = new MemTreeBuilder();
        memTreeBuilder.startDocument(false);

        final Tuple2<Integer, Integer> elementNodeNumbers = addInMemoryElements(memTreeBuilder);

        memTreeBuilder.endDocument();
        return (org.exist.dom.memtree.AttrImpl) memTreeBuilder.getDocument().getNode(elementNodeNumbers._2).getAttributes().getNamedItem("c");
    }

    private static Tuple2<Integer, Integer> addInMemoryElements(final MemTreeBuilder memTreeBuilder) {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(XMLConstants.NULL_NS_URI, "a", "a", "string", "1");
        attrs.addAttribute(XMLConstants.NULL_NS_URI, "b", "b", "string", "2");
        final int elem1NodeNumber = memTreeBuilder.startElement(XMLConstants.NULL_NS_URI, "elem1", "elem1", attrs);

        attrs = new AttributesImpl();
        attrs.addAttribute(XMLConstants.NULL_NS_URI, "c", "c", "string", "3");
        attrs.addAttribute(XMLConstants.NULL_NS_URI, "d", "d", "string", "4");
        final int elem2NodeNumber = memTreeBuilder.startElement(XMLConstants.NULL_NS_URI, "elem2", "elem2", attrs);
        memTreeBuilder.characters("text1");
        memTreeBuilder.endElement();

        memTreeBuilder.endElement();

        return Tuple(elem1NodeNumber, elem2NodeNumber);
    }

    private static org.exist.dom.persistent.DocumentImpl getPersistentDocument() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final org.exist.dom.memtree.DocumentImpl inMemoryDocument = getInMemoryDocument(true);

        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        try (final Txn transaction = brokerPool.getTransactionManager().beginTransaction();
             final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Collection testCollection = broker.getOrCreateCollection(transaction, TEST_COLLECTION_URI)) {

            final XmldbURI documentName = XmldbURI.create(UUID.randomUUID() + ".xml");
            broker.storeDocument(transaction, documentName, inMemoryDocument, MimeType.XML_TYPE, testCollection);

            final  org.exist.dom.persistent.DocumentImpl persistentDocument = testCollection.getDocument(broker, documentName);

            transaction.commit();

            return persistentDocument;
        }
    }

    private static XdmNode assertXdmValueToXdmNode(final XdmValue xdmValue) {
        assertNotNull(xdmValue);
        assertEquals(1, xdmValue.size());
        final XdmItem xdmItem = xdmValue.itemAt(0);
        assertNotNull(xdmItem);
        assertTrue(xdmItem instanceof XdmNode);
        return (XdmNode) xdmItem;
    }

    private static void assertDocument(final XdmNode xdmNode) {
        assertEquals(XdmNodeKind.DOCUMENT, xdmNode.getNodeKind());

        final Iterator<XdmNode> iterator = xdmNode.children().iterator();
        assertTrue(iterator.hasNext());
        final XdmNode firstChild = iterator.next();
        assertFirstElement(firstChild);
        assertFalse(iterator.hasNext());
    }

    private static void assertFirstElement(final XdmNode firstElement) {
        assertEquals(XdmNodeKind.ELEMENT, firstElement.getNodeKind());
        assertEquals("elem1", firstElement.getNodeName().getClarkName());
        assertAttribute("1", firstElement, "a");
        assertAttribute("2", firstElement, "b");

        final Iterator<XdmNode> iterator = firstElement.children().iterator();
        assertTrue(iterator.hasNext());
        final XdmNode nestedElement = iterator.next();
        assertNestedElement(nestedElement);
        assertFalse(iterator.hasNext());
    }

    private static void assertAttribute(final String expected, final XdmNode attribute) {
        assertEquals(XdmNodeKind.ATTRIBUTE, attribute.getNodeKind());
        assertEquals(expected, attribute.getStringValue());
    }

    private static void assertAttribute(final String expected, final XdmNode element, final String attributeName) {
       assertEquals(expected, element.attribute(attributeName));
    }

    private static void assertNestedElement(final XdmNode nestedElement) {
        assertEquals(XdmNodeKind.ELEMENT, nestedElement.getNodeKind());
        assertEquals("elem2", nestedElement.getNodeName().getClarkName());
        assertAttribute("3", nestedElement, "c");
        assertAttribute("4", nestedElement, "d");

        final Iterator<XdmNode> iterator = nestedElement.children().iterator();
        assertTrue(iterator.hasNext());
        final XdmNode firstChild = iterator.next();
        assertEquals(XdmNodeKind.TEXT, firstChild.getNodeKind());
        assertEquals("text1", firstChild.getStringValue());
        assertFalse(iterator.hasNext());
    }
}
