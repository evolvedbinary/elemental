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
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.ManagedCollectionLock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
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

import javax.xml.transform.Source;
import java.io.IOException;
import java.util.Optional;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class FunTransformITTest {

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
    public void identityPersistentDom() throws XPathException, PermissionDeniedException, EXistException {
        final Source expected = Input.fromString(IDENTITY_XML).build();
        expectQuery(IDENTITY_PERSISTENT_XSLT_QUERY, expected);
    }

    @Test
    public void identityMemoryDom() throws XPathException, PermissionDeniedException, EXistException {
        final Source expected = Input.fromString(IDENTITY_XML).build();
        expectQuery(IDENTITY_MEMORY_XSLT_QUERY, expected);
    }

    /**
     * {@see https://github.com/eXist-db/exist/issues/5682}
     */
    @Test
    public void identityMixedMemoryAndPersistentDom() throws XPathException, PermissionDeniedException, EXistException {
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

    private static void expectQuery(final String query, final Source expected) throws EXistException, XPathException, PermissionDeniedException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final XQuery xquery = pool.getXQueryService();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            final Sequence sequence = xquery.execute(broker, query, null);

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

            transaction.commit();
        }
    }

    @SafeVarargs
    private static void createCollection(final DBBroker broker, final Txn transaction, final XmldbURI collectionUri, final Tuple2<XmldbURI, String>... docs) throws PermissionDeniedException, IOException, SAXException, LockException, EXistException {
        try (final ManagedCollectionLock collectionLock = broker.getBrokerPool().getLockManager().acquireCollectionWriteLock(collectionUri)) {
            final Collection collection = broker.getOrCreateCollection(transaction, collectionUri);
            broker.saveCollection(transaction, collection);
            for (final Tuple2<XmldbURI, String> doc : docs) {
                broker.storeDocument(transaction, doc._1, new StringInputSource(doc._2), MimeType.XML_TYPE, collection);
            }
        }
    }
}
