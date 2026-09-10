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
 */
package org.exist.xquery.functions.fn.transform;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.logging.log4j.Logger;
import org.easymock.Capture;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.security.PermissionDeniedException;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.ManagedCollectionLock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import xyz.elemental.mediatype.MediaType;

import javax.xml.transform.Source;
import java.io.IOException;
import java.util.Optional;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class FunTransformITTest {

    private static final XmldbURI TEST_IMPORT_XSLT_COLLECTION = XmldbURI.create("/db/fn-transform-import-test");
    private static final XmldbURI IMPORT_A_XSLT_NAME = XmldbURI.create("a.xsl");
    private static final XmldbURI IMPORT_B_XSLT_NAME = XmldbURI.create("b.xsl");

    private static final String IMPORT_A_XSLT =
        "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" +
        "  <xsl:import href=\"b.xsl\"/>\n" +
        "  <xsl:template match=\"node()\">\n" +
        "    <doc><p>From A</p><xsl:call-template name=\"from-b\"/></doc>\n" +
        "  </xsl:template>\n" +
        "</xsl:stylesheet>";

    private static final String IMPORT_B_XSLT =
        "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" +
        "  <xsl:template name=\"from-b\"><p>From B</p></xsl:template>\n" +
        "</xsl:stylesheet>";

    private static final String DOCUMENT_SAME_DIR_IMPORT_VIA_DB_LOCATION_QUERY =
        "fn:transform(map {\n" +
        "  \"stylesheet-location\": \"/db/fn-transform-import-test/a.xsl\",\n" +
        "  \"source-node\": document { <empty/> }\n" +
        "})?output";

    private static final String ELEMENT_SAME_DIR_IMPORT_VIA_DB_LOCATION_QUERY =
        "fn:transform(map {\n" +
        "  \"stylesheet-location\": \"/db/fn-transform-import-test/a.xsl\",\n" +
        "  \"source-node\": <empty/>\n" +
        "})?output";

    private static final String DOCUMENT_SAME_DIR_IMPORT_VIA_XMLDB_LOCATION_QUERY =
        "fn:transform(map {\n" +
        "  \"stylesheet-location\": \"xmldb:exist:///db/fn-transform-import-test/a.xsl\",\n" +
        "  \"source-node\": document { <empty/> }\n" +
        "})?output";

    private static final String ELEMENT_SAME_DIR_IMPORT_VIA_XMLDB_LOCATION_QUERY =
        "fn:transform(map {\n" +
        "  \"stylesheet-location\": \"xmldb:exist:///db/fn-transform-import-test/a.xsl\",\n" +
        "  \"source-node\": <empty/>\n" +
        "})?output";

    private static final String DOCUMENT_SAME_DIR_IMPORT_VIA_DB_NODE_QUERY =
        "fn:transform(map {\n" +
        "  \"stylesheet-node\": doc(\"/db/fn-transform-import-test/a.xsl\"),\n" +
        "  \"source-node\": document { <empty/> }\n" +
        "})?output";

    private static final String ELEMENT_SAME_DIR_IMPORT_VIA_DB_NODE_QUERY =
        "fn:transform(map {\n" +
        "  \"stylesheet-node\": doc(\"/db/fn-transform-import-test/a.xsl\"),\n" +
        "  \"source-node\": <empty/>\n" +
        "})?output";

    private static final String DOCUMENT_SAME_DIR_IMPORT_VIA_XMLDB_NODE_QUERY =
        "fn:transform(map {\n" +
        "  \"stylesheet-node\": doc(\"xmldb:exist:///db/fn-transform-import-test/a.xsl\"),\n" +
        "  \"source-node\": document { <empty/> }\n" +
        "})?output";

    private static final String ELEMENT_SAME_DIR_IMPORT_VIA_XMLDB_NODE_QUERY =
        "fn:transform(map {\n" +
        "  \"stylesheet-node\": doc(\"xmldb:exist:///db/fn-transform-import-test/a.xsl\"),\n" +
        "  \"source-node\": <empty/>\n" +
        "})?output";

    private static final XmldbURI TEST_IDENTITY_XSLT_COLLECTION = XmldbURI.create("/db/transform-identity-test");
    private static final XmldbURI IDENTITY_XSLT_NAME = XmldbURI.create("xsl-identity.xslt");

    private static final String IDENTITY_XSLT =
        "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">\n" +
            "  <xsl:template match=\"node()|@*\">\n" +
            "    <xsl:copy>\n" +
            "      <xsl:apply-templates select=\"node()|@*\"/>\n" +
            "    </xsl:copy>\n" +
            "  </xsl:template> \n" +
            "</xsl:stylesheet>";

    private static final XmldbURI IDENTITY_XML_NAME = XmldbURI.create("example.xml");

    private static final String IDENTITY_XML =
        "<a><!-- comment1 --> hello <b x=\"y\"> world</b></a>";

    private static final String IDENTITY_PERSISTENT_XSLT_QUERY =
            "let $xslt := doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XSLT_NAME).getRawCollectionPath() + "')\n" +
            "let $xml := doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XML_NAME).getRawCollectionPath() + "')\n" +
            "return\n" +
            "\tfn:transform(map {\n" +
            "    \"stylesheet-node\": $xslt,\n" +
            "    \"source-node\": $xml\n" +
            "  })?output";

    private static final String IDENTITY_MEMORY_XSLT_QUERY =
            "let $xslt := doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XSLT_NAME).getRawCollectionPath() + "')\n" +
            "let $xml := document {" + IDENTITY_XML + "}\n" +
            "return\n" +
            "\tfn:transform(map {\n" +
            "    \"stylesheet-node\": $xslt,\n" +
            "    \"source-node\": $xml\n" +
            "  })?output";

    private static final String IDENTITY_MIXED_XSLT_QUERY_1 =
        "let $xslt := doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XSLT_NAME).getRawCollectionPath() + "')\n" +
            "let $xml := <mixed i=\"j\">{doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XML_NAME).getRawCollectionPath() + "')}</mixed>\n" +
            "return\n" +
            "\tfn:transform(map {\n" +
            "    \"stylesheet-node\": $xslt,\n" +
            "    \"source-node\": $xml\n" +
            "  })?output";

    private static final String IDENTITY_MIXED_XSLT_QUERY_2 =
        "let $xslt := doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XSLT_NAME).getRawCollectionPath() + "')\n" +
            "let $xml := <mixed i=\"j\">{doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XML_NAME).getRawCollectionPath() + "')/a/b}</mixed>\n" +
            "return\n" +
            "\tfn:transform(map {\n" +
            "    \"stylesheet-node\": $xslt,\n" +
            "    \"source-node\": $xml\n" +
            "  })?output";

    private static final String IDENTITY_MIXED_XSLT_QUERY_3 =
        "let $xslt := doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XSLT_NAME).getRawCollectionPath() + "')\n" +
            "let $xml := <mixed i=\"j\">{doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XML_NAME).getRawCollectionPath() + "')/a/comment()}</mixed>\n" +
            "return\n" +
            "\tfn:transform(map {\n" +
            "    \"stylesheet-node\": $xslt,\n" +
            "    \"source-node\": $xml\n" +
            "  })?output";

    private static final String IDENTITY_MIXED_XSLT_QUERY_4 =
        "let $xslt := doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XSLT_NAME).getRawCollectionPath() + "')\n" +
            "let $xml := <mixed i=\"j\">{doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XML_NAME).getRawCollectionPath() + "')/a/b/text()}</mixed>\n" +
            "return\n" +
            "\tfn:transform(map {\n" +
            "    \"stylesheet-node\": $xslt,\n" +
            "    \"source-node\": $xml\n" +
            "  })?output";

    private static final String IDENTITY_MIXED_XSLT_QUERY_5 =
        "let $xslt := doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XSLT_NAME).getRawCollectionPath() + "')\n" +
            "let $xml := <mixed i=\"j\">{doc('" + TEST_IDENTITY_XSLT_COLLECTION.append(IDENTITY_XML_NAME).getRawCollectionPath() + "')/a/b/@x}</mixed>\n" +
            "return\n" +
            "\tfn:transform(map {\n" +
            "    \"stylesheet-node\": $xslt,\n" +
            "    \"source-node\": $xml\n" +
            "  })?output";



    @ClassRule
    public static ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @Test
    public void documentSameDirectoryImportViaDbLocation() throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Source expected = Input.fromString("<doc><p>From A</p><p>From B</p></doc>").build();
        expectQuery(DOCUMENT_SAME_DIR_IMPORT_VIA_DB_LOCATION_QUERY, expected);
    }

    @Test
    public void elementSameDirectoryImportViaDbLocation() throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Source expected = Input.fromString("<doc><p>From A</p><p>From B</p></doc>").build();
        expectQuery(ELEMENT_SAME_DIR_IMPORT_VIA_DB_LOCATION_QUERY, expected);
    }

    @Test
    public void documentSameDirectoryImportViaXmldbLocation() throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Source expected = Input.fromString("<doc><p>From A</p><p>From B</p></doc>").build();
        expectQuery(DOCUMENT_SAME_DIR_IMPORT_VIA_XMLDB_LOCATION_QUERY, expected);
    }

    @Test
    public void elementSameDirectoryImportViaXmldbLocation() throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Source expected = Input.fromString("<doc><p>From A</p><p>From B</p></doc>").build();
        expectQuery(ELEMENT_SAME_DIR_IMPORT_VIA_XMLDB_LOCATION_QUERY, expected);
    }

    @Test
    public void documentSameDirectoryImportViaDbNode() throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Source expected = Input.fromString("<doc><p>From A</p><p>From B</p></doc>").build();
        expectQuery(DOCUMENT_SAME_DIR_IMPORT_VIA_DB_NODE_QUERY, expected);
    }

    @Test
    public void elementSameDirectoryImportViaDbNode() throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Source expected = Input.fromString("<doc><p>From A</p><p>From B</p></doc>").build();
        expectQuery(ELEMENT_SAME_DIR_IMPORT_VIA_DB_NODE_QUERY, expected);
    }

    @Test
    public void documentSameDirectoryImportViaXmldbNode() throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Source expected = Input.fromString("<doc><p>From A</p><p>From B</p></doc>").build();
        expectQuery(DOCUMENT_SAME_DIR_IMPORT_VIA_XMLDB_NODE_QUERY, expected);
    }

    @Test
    public void elementSameDirectoryImportViaXmldbNode() throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Source expected = Input.fromString("<doc><p>From A</p><p>From B</p></doc>").build();
        expectQuery(ELEMENT_SAME_DIR_IMPORT_VIA_XMLDB_NODE_QUERY, expected);
    }

    @Test
    public void identityPersistentDom() throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Source expected = Input.fromString(IDENTITY_XML).build();
        expectQuery(IDENTITY_PERSISTENT_XSLT_QUERY, expected);
    }

    @Test
    public void identityMemoryDom() throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Source expected = Input.fromString(IDENTITY_XML).build();
        expectQuery(IDENTITY_MEMORY_XSLT_QUERY, expected);
    }

    /**
     * {@see https://github.com/eXist-db/exist/issues/5682}
     */
    @Test
    public void identityMixedMemoryAndPersistentDom() throws XPathException, PermissionDeniedException, EXistException, IOException {
        // Document reference
        Source expected = Input.fromString("<mixed i=\"j\">" + IDENTITY_XML + "</mixed>").build();
        expectQuery(IDENTITY_MIXED_XSLT_QUERY_1, expected);

        // Element reference
        expected = Input.fromString("<mixed i=\"j\"><b x=\"y\"> world</b></mixed>").build();
        expectQuery(IDENTITY_MIXED_XSLT_QUERY_2, expected);

        // Comment reference
        expected = Input.fromString("<mixed i=\"j\"><!-- comment1 --></mixed>").build();
        expectQuery(IDENTITY_MIXED_XSLT_QUERY_3, expected);

        // Text reference
        expected = Input.fromString("<mixed i=\"j\"> world</mixed>").build();
        expectQuery(IDENTITY_MIXED_XSLT_QUERY_4, expected);

        // Attribute reference
        expected = Input.fromString("<mixed i=\"j\" x=\"y\"/>").build();
        expectQuery(IDENTITY_MIXED_XSLT_QUERY_5, expected);
    }

    @Test
    public void xslMessageIsLogged() throws EXistException, PermissionDeniedException, IOException, XPathException {

        // set a mock logger so we can capture the log output for our test
        final Logger mockLogger = createMock(Logger.class);
        Transform.setLogger(mockLogger);

        // expectations
        final Capture<String> formatPattern = newCapture();
        final Capture<String> startTagCapture = newCapture();
        final Capture<String> logMessageCapture = newCapture();
        mockLogger.info(capture(formatPattern), capture(startTagCapture), capture(logMessageCapture));

        // reset mock state before test
        replay(mockLogger);

        // execute test
        final String query =
            "fn:transform(map {\n" +
            "  \"stylesheet-text\": '<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">\n" +
            "    <xsl:template match=\"/\">\n" +
            "      <xsl:message>Hello from XSLT</xsl:message>\n" +
            "    </xsl:template>\n" +
            "  </xsl:stylesheet>',\n" +
            "  \"source-node\": document { <in/> }\n" +
            "})?output";

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
            assertNotNull(queryResult.result);
        }

        // verify our expectations
        verify(mockLogger);

        // check our assertions about the log message
        final String startTag = startTagCapture.getValue();
        final String message = logMessageCapture.getValue();

        assertEquals("<xsl:message terminate=\"false\" sourceLine=\"3\" sourceColumn=\"20\">", startTag);
        assertEquals("Hello from XSLT", message);
    }

    private static void expectQuery(final String query, final Source expected) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
            final Sequence sequence = queryResult.result;

            assertNotNull(sequence);
            assertTrue(sequence.hasOne());

            final Source actual = Input.fromDocument((Document) sequence.itemAt(0).toJavaObject(Node.class)).build();

            final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();

            assertFalse(diff.toString(), diff.hasDifferences());
        }
    }

    @BeforeClass
    public static void storeResources() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            createCollection(broker, transaction, TEST_IDENTITY_XSLT_COLLECTION,
                Tuple(IDENTITY_XSLT_NAME, IDENTITY_XSLT),
                Tuple(IDENTITY_XML_NAME, IDENTITY_XML)
            );

            createCollection(broker, transaction, TEST_IMPORT_XSLT_COLLECTION,
                Tuple(IMPORT_A_XSLT_NAME, IMPORT_A_XSLT),
                Tuple(IMPORT_B_XSLT_NAME, IMPORT_B_XSLT)
            );

            transaction.commit();
        }
    }

    @SafeVarargs
    private static void createCollection(final DBBroker broker, final Txn transaction, final XmldbURI collectionUri, final Tuple2<XmldbURI, String>... docs) throws PermissionDeniedException, IOException, SAXException, LockException, EXistException {
        try (final ManagedCollectionLock collectionLock = broker.getBrokerPool().getLockManager().acquireCollectionWriteLock(collectionUri)) {
            final Collection collection = broker.getOrCreateCollection(transaction, collectionUri);
            broker.saveCollection(transaction, collection);
            final MediaType xmlMediaType = broker.getBrokerPool().getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
            for (final Tuple2<XmldbURI, String> doc : docs) {
                broker.storeDocument(transaction, doc._1, new StringInputSource(doc._2), xmlMediaType, collection);
            }
        }
    }
}
