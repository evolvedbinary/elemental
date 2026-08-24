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
package org.exist.xmldb;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XPathQueryService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author jmv
 */
public class DOMTest {

	private static final Logger LOG =  LogManager.getLogger(DOMTest.class);

	@RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

	private static String name = "test.xml";

    /** 
     * - Storing XML resource from XML string
     * - simple XQuery
     * - removing resource
     * - shutdownDB with the DatabaseInstanceManager
     */
    @Test
    void test1() throws XMLDBException {
		final CollectionManagementService cms = existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
		try (final Collection created = cms.createCollection("A")) { } // jmv
		cms.removeCollection("A");
		try (final Collection created = cms.createCollection("A")) { }

		try (final Collection coll = XMLDB_EMBEDDED_DATABASE.getRoot().getChildCollection("A")) {

			try (final XMLResource r = coll.createResource(name, XMLResource.class)) {
				r.setContent("<properties><property key=\"type\">Table</property></properties>");
				coll.storeResource(r);
			}

			final XPathQueryService xpqs = coll.getService(XPathQueryService.class);
			try (final EXistResourceSet rs = (EXistResourceSet) xpqs.query("//properties[property[@key='type' and text()='Table']]")) {
				for (final ResourceIterator i = rs.getIterator(); i.hasMoreResources(); ) {
					try (final XMLResource r = (XMLResource) i.nextResource()) {
						final String s = (String) r.getContent();
						assertNotNull(s);
						final Node content = r.getContentAsDOM();
						assertNotNull(content);
						assertTrue(content instanceof Element);

						coll.removeResource(r);
					}
				}
			}
		}

		cms.removeCollection("A");
	}
	/** 
	 * - create and fill a simple document via DOM and JAXP
	 * - store it with setContentAsDOM()
	 * - simple access via getContentAsDOM()
	 * */
	@Test
	void test2() throws XMLDBException, InstantiationException, IllegalAccessException, ClassNotFoundException, ParserConfigurationException, IOException {

		try (final XMLResource resource = XMLDB_EMBEDDED_DATABASE.getRoot().createResource(name, XMLResource.class)) {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.newDocument();
			final Element rootElem = doc.createElement("element");
			doc.appendChild(rootElem);
			resource.setContentAsDOM(doc);
			XMLDB_EMBEDDED_DATABASE.getRoot().storeResource(resource);
		}

		try (final XMLResource resource = (XMLResource) XMLDB_EMBEDDED_DATABASE.getRoot().getResource(name)) {
			final String s = (String) resource.getContent();
			assertNotNull(s);
			final Node content = resource.getContentAsDOM();
			assertNotNull(content);
			assertTrue(content instanceof Document);
		}

		XMLDB_EMBEDDED_DATABASE.restart();

		try (final XMLResource resource = (XMLResource) XMLDB_EMBEDDED_DATABASE.getRoot().getResource(name)) {
			XMLDB_EMBEDDED_DATABASE.getRoot().removeResource(resource);
		}
	}
	
	/** like test 2 but add attribute and text as well */
	@Test
	public void test3() throws XMLDBException, ParserConfigurationException {
		final Collection coll = XMLDB_EMBEDDED_DATABASE.getRoot();
		try (final XMLResource resource = coll.createResource(name, XMLResource.class)) {
			final Document doc =
				DocumentBuilderFactory
					.newInstance()
					.newDocumentBuilder()
					.newDocument();
			final Element rootElem = doc.createElement("element");
			final Element propertyElem = doc.createElement("property");
			propertyElem.setAttribute("key", "value");
			propertyElem.appendChild(doc.createTextNode("text"));
			rootElem.appendChild(propertyElem);
			doc.appendChild(rootElem);
			resource.setContentAsDOM(doc);

			coll.storeResource(resource);
		}

		try (final Collection coll2 = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, "admin", "")) {
			final XMLResource resource = (XMLResource) coll2.getResource(name);
			final String s = (String) resource.getContent();
			assertNotNull(s);
			final Node content = resource.getContentAsDOM();
			assertNotNull(content);
			assertTrue(content instanceof Document);

			coll2.removeResource(resource);
		}
	}

    /** like test 3 but uses the DOM as input to an (identity) XSLT transform */
    @Test
    void test4_getContentAsString() throws XMLDBException, ParserConfigurationException, IOException, SAXException, TransformerException {
		_test4(false);
	}

    @Test
    void test4_getContentAsDOM() throws XMLDBException, ParserConfigurationException, IOException, SAXException, TransformerException {
		_test4(true);
	}

	private void _test4(boolean getContentAsDOM) throws TransformerException, ParserConfigurationException, XMLDBException, IOException, SAXException {
		final Collection coll = XMLDB_EMBEDDED_DATABASE.getRoot();
		try (final XMLResource resource = coll.createResource(name, XMLResource.class)) {

			final Document doc =
				DocumentBuilderFactory
					.newInstance()
					.newDocumentBuilder()
					.newDocument();
			final Element rootElem = doc.createElement("element");
			final Element propertyElem = doc.createElement("property");
			propertyElem.setAttribute("key", "value");
			propertyElem.appendChild(doc.createTextNode("text"));
			rootElem.appendChild(propertyElem);
			doc.appendChild(rootElem);
			resource.setContentAsDOM(doc);

			coll.storeResource(resource);
		}

		try (final Collection coll2 = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, "admin", "")) {
			try (final XMLResource resource = (XMLResource) coll2.getResource(name)) {

				final Node n;
				if (getContentAsDOM) {
					n = resource.getContentAsDOM();
				} else {
					final String s = (String) resource.getContent();
					final byte[] bytes = s.getBytes(UTF_8);
					try (final UnsynchronizedByteArrayInputStream bais = new UnsynchronizedByteArrayInputStream(bytes)) {
						final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						n = db.parse(bais);
					}
				}
				assertNotNull(n);
				assertTrue(n instanceof Document);

				final Transformer t = TransformerFactory.newInstance().newTransformer();
				final DOMSource source = new DOMSource(n);
				final SAXResult result = new SAXResult(new DOMTest.SAXHandler());
				t.transform(source, result);

				coll2.removeResource(resource);
			}
		}
	}

	public static class SAXHandler implements ContentHandler {
		SAXHandler() {
		}

		@Override
		public void characters(char[] ch, int start, int length) {
			LOG.trace("SAXHandler.characters({}, {}, {})", new String(ch), start, length);
		}

		@Override
		public void endDocument() {
			LOG.trace("SAXHandler.endDocument()");
		}

		@Override
		public void endElement(
			String namespaceURI,
			String localName,
			String qName) {
			LOG.trace("SAXHandler.endElement({}, {}, {})", namespaceURI, localName, qName);
		}

		@Override
		public void endPrefixMapping(String prefix) {
			LOG.trace("SAXHandler.endPrefixMapping({})", prefix);
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) {
			LOG.trace("SAXHandler.ignorableWhitespace({}, {}, {})", new String(ch), start, length);
		}

		@Override
		public void processingInstruction(String target, String data) {
			LOG.trace("SAXHandler.processingInstruction({}, {})", target, data);
		}

		@Override
		public void setDocumentLocator(Locator locator) {
			LOG.trace("SAXHandler.setDocumentLocator({})", locator);
		}

		@Override
		public void skippedEntity(String name) {
			LOG.trace("SAXHandler.skippedEntity({})", name);
		}

		@Override
		public void startDocument() {
			LOG.trace("SAXHandler.startDocument()");
		}

		@Override
		public void startElement(
			String namespaceURI,
			String localName,
			String qName,
			Attributes atts) {
			LOG.trace("SAXHandler.startElement({}, {}, {},{})", namespaceURI, localName, qName, atts);
		}

		@Override
		public void startPrefixMapping(String prefix, String xuri) {
			LOG.trace("SAXHandler.startPrefixMapping({}, {})", prefix, xuri);
		}

	}

}
