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
package org.exist.xquery.functions.fn;

import com.evolvedbinary.j8fu.Either;
import org.exist.EXistException;
import org.exist.Namespaces;
import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.memtree.SAXAdapter;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.util.ExistSAXParserFactory;
import org.exist.xquery.*;
import org.exist.xquery.value.AnyURIValue;
import org.exist.xquery.value.Sequence;
import org.junit.*;

import static com.evolvedbinary.j8fu.Either.Left;
import static org.junit.Assert.*;

import org.exist.xmldb.EXistResource;
import org.exist.xmldb.LocalXMLResource;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author Joe Wicentowski
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public class DocTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    private static SAXParserFactory saxParserFactory = ExistSAXParserFactory.getSAXParserFactory();
    static {
        saxParserFactory.setNamespaceAware(true);
    }
    private Collection test = null;

    @Before
    public void setUp() throws XMLDBException {
        final CollectionManagementService cms = existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
        //Creates the 'test' collection
        test = cms.createCollection("test");
        assertNotNull(test);

        storeResource(test, "test.xq", BinaryResource.class, "application/xquery", "doc('test.xml')");
        storeResource(test, "test1.xq", BinaryResource.class, "application/xquery", "doc('/test.xml')");
        storeResource(test, "test2.xq", BinaryResource.class, "application/xquery", "doc('/db/test.xml')");

        storeResource(existEmbeddedServer.getRoot(), "test.xml", XMLResource.class, null, "<x/>");
        storeResource(test, "test.xml", XMLResource.class, null, "<y/>");

    }

    @After
    public void tearDown() throws XMLDBException {
        final CollectionManagementService cms = existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
        //Creates the 'test' collection
        cms.removeCollection("test");
        test = null;

        existEmbeddedServer.getRoot().removeResource(existEmbeddedServer.getRoot().getResource("test.xml"));
    }
    
    private void storeResource(final Collection col, final String fileName, final Class<? extends Resource> type, final String mimeType, final String content) throws XMLDBException {
    	Resource res = col.createResource(fileName, type);
    	res.setContent(content);
    	
    	if (mimeType != null) {
            ((EXistResource) res).setMimeType(mimeType);
        }
        
    	col.storeResource(res);
    }

    @Test
    public void testURIResolveWithEval() throws XMLDBException {
        String query = "util:eval(xs:anyURI('/db/test/test.xq'), false(), ())";
        ResourceSet result = existEmbeddedServer.executeQuery(query);

        LocalXMLResource res = (LocalXMLResource)result.getResource(0);
        assertNotNull(res);
        Node n = res.getContentAsDOM();
        assertEquals("y", n.getLocalName());

        query = "util:eval(xs:anyURI('/db/test/test1.xq'), false(), ())";
        result = existEmbeddedServer.executeQuery(query);

        res = (LocalXMLResource)result.getResource(0);
        assertNotNull(res);
        n = res.getContentAsDOM();
        assertEquals("x", n.getLocalName());

        query = "util:eval(xs:anyURI('/db/test/test2.xq'), false(), ())";
        result = existEmbeddedServer.executeQuery(query);

        res = (LocalXMLResource)result.getResource(0);
        assertNotNull(res);
        n = res.getContentAsDOM();
        assertEquals("x", n.getLocalName());
    }

    @Test
    public void doc_dynamicallyAvailableDocument_absoluteUri() throws XPathException, EXistException, PermissionDeniedException {
        final BrokerPool pool = BrokerPool.getInstance();

        final String doc = "<timestamp>" + System.currentTimeMillis() + "</timestamp>";
        final String docUri = "http://from-dynamic-context/doc1";
        final String query = "fn:doc('" + docUri + "')";

        try (final DBBroker broker = pool.getBroker()) {
            final XQueryContext context = new XQueryContext(pool);
            context.addDynamicallyAvailableDocument(docUri, (broker2, transaction, uri) -> asInMemoryDocument(doc));

            final XQuery xqueryService = pool.getXQueryService();
            final CompiledXQuery compiled = xqueryService.compile(context, query);
            final Sequence result = xqueryService.execute(broker, compiled, null);

            assertFalse(result.isEmpty());
            assertEquals(1, result.getItemCount());
            assertTrue(result.itemAt(0) instanceof Node);

            final Source expectedSource = Input.fromString(doc).build();
            final Source actualSource = Input.fromNode((Node)result.itemAt(0)).build();
            final Diff diff = DiffBuilder.compare(expectedSource)
                    .withTest(actualSource)
                    .checkForIdentical()
                    .checkForSimilar()
                    .build();

            assertFalse(diff.toString(), diff.hasDifferences());
        }
    }

    @Test
    public void doc_dynamicallyAvailableDocument_relativeUri() throws XPathException, EXistException, PermissionDeniedException, URISyntaxException {
        final BrokerPool pool = BrokerPool.getInstance();

        final String doc = "<timestamp>" + System.currentTimeMillis() + "</timestamp>";
        final String baseUri = "http://from-dynamic-context/";
        final String docRelativeUri = "doc1";
        final String query = "fn:doc('" + docRelativeUri + "')";

        try (final DBBroker broker = pool.getBroker()) {
            final XQueryContext context = new XQueryContext(pool);
            context.setBaseURI(new AnyURIValue(new URI(baseUri)));
            context.addDynamicallyAvailableDocument(baseUri + docRelativeUri, (broker2, transaction, uri) -> asInMemoryDocument(doc));

            final XQuery xqueryService = pool.getXQueryService();
            final CompiledXQuery compiled = xqueryService.compile(context, query);
            final Sequence result = xqueryService.execute(broker, compiled, null);

            assertFalse(result.isEmpty());
            assertEquals(1, result.getItemCount());
            assertTrue(result.itemAt(0) instanceof Node);

            final Source expectedSource = Input.fromString(doc).build();
            final Source actualSource = Input.fromNode((Node)result.itemAt(0)).build();
            final Diff diff = DiffBuilder.compare(expectedSource)
                    .withTest(actualSource)
                    .checkForIdentical()
                    .checkForSimilar()
                    .build();

            assertFalse(diff.toString(), diff.hasDifferences());
        }
    }

    @Test
    public void docAvailable_dynamicallyAvailableDocument_absoluteUri() throws XPathException, EXistException, PermissionDeniedException {
        final BrokerPool pool = BrokerPool.getInstance();

        final String doc = "<timestamp>" + System.currentTimeMillis() + "</timestamp>";
        final String docUri = "http://from-dynamic-context/doc1";
        final String query = "fn:doc-available('" + docUri + "')";

        try (final DBBroker broker = pool.getBroker()) {
            final XQueryContext context = new XQueryContext(pool);
            context.addDynamicallyAvailableDocument(docUri, (broker2, transaction, uri) -> asInMemoryDocument(doc));

            final XQuery xqueryService = pool.getXQueryService();
            final CompiledXQuery compiled = xqueryService.compile(context, query);
            final Sequence result = xqueryService.execute(broker, compiled, null);

            assertFalse(result.isEmpty());
            assertEquals(1, result.getItemCount());
            assertTrue(result.itemAt(0).toJavaObject(Boolean.class).booleanValue());
        }
    }

    @Test
    public void docAvailable_dynamicallyAvailableDocument_relativeUri() throws XPathException, EXistException, PermissionDeniedException, URISyntaxException {
        final BrokerPool pool = BrokerPool.getInstance();

        final String doc = "<timestamp>" + System.currentTimeMillis() + "</timestamp>";
        final String baseUri = "http://from-dynamic-context/";
        final String docRelativeUri = "doc1";
        final String query = "fn:doc-available('" + docRelativeUri + "')";

        try (final DBBroker broker = pool.getBroker()) {
            final XQueryContext context = new XQueryContext(pool);
            context.setBaseURI(new AnyURIValue(new URI(baseUri)));
            context.addDynamicallyAvailableDocument(baseUri + docRelativeUri, (broker2, transaction, uri) -> asInMemoryDocument(doc));

            final XQuery xqueryService = pool.getXQueryService();
            final CompiledXQuery compiled = xqueryService.compile(context, query);
            final Sequence result = xqueryService.execute(broker, compiled, null);

            assertFalse(result.isEmpty());
            assertEquals(1, result.getItemCount());
            assertTrue(result.itemAt(0).toJavaObject(Boolean.class).booleanValue());
        }
    }

    @Test
    public void docAvailableInPredicate() throws XPathException, EXistException, PermissionDeniedException {
        final BrokerPool pool = BrokerPool.getInstance();
        final String query = "('/db/test.xml', '/db/test/test.xml', '/db/non-existent.xml')[fn:doc-available(.)]";

        try (final DBBroker broker = pool.getBroker()) {
            final XQueryContext context = new XQueryContext(pool);

            final XQuery xqueryService = pool.getXQueryService();
            final CompiledXQuery compiled = xqueryService.compile(context, query);
            final Sequence result = xqueryService.execute(broker, compiled, null);

            assertFalse(result.isEmpty());
            assertEquals(2, result.getItemCount());
            assertEquals("/db/test.xml", result.itemAt(0).getStringValue());
            assertEquals("/db/test/test.xml", result.itemAt(1).getStringValue());
        }
    }

    private Either<DocumentImpl, org.exist.dom.persistent.DocumentImpl> asInMemoryDocument(final String doc) throws XPathException {
        try {
            final SAXAdapter saxAdapter = new SAXAdapter();
            final SAXParser saxParser = saxParserFactory.newSAXParser();
            final XMLReader xmlReader = saxParser.getXMLReader();

            xmlReader.setContentHandler(saxAdapter);
            xmlReader.setProperty(Namespaces.SAX_LEXICAL_HANDLER, saxAdapter);

            try (final Reader reader = new StringReader(doc)) {
                xmlReader.parse(new InputSource(reader));
            }
            return Left(saxAdapter.getDocument());
        } catch (final ParserConfigurationException | SAXException | IOException e) {
            throw new XPathException((Expression) null, "Unable to parse document", e);
        }
    }
}
