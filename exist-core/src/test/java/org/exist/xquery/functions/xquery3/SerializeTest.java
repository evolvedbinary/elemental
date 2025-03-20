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
package org.exist.xquery.functions.xquery3;

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
import java.util.function.Consumer;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class SerializeTest {

    @ClassRule
    public static ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    private static final XmldbURI TEST_SERIALIZE_COLLECTION = XmldbURI.create("/db/serialize-test");

    private static final XmldbURI DOC_WITH_PI_NAME = XmldbURI.create("test-with-pi.xml");

    private static final String DOC_WITH_PI =
        "<?pi?>\n" +
        "<elem a=\"abc\"><!--comment--><b>123</b></elem>";

    private static final String SERIALIZE_WITH_EXIST_ID_ALL_QUERY =
        "let $doc := doc(\"" + TEST_SERIALIZE_COLLECTION.getCollectionPath() + "/" + DOC_WITH_PI_NAME.getCollectionPath() + "\")\n" +
        "return\n" +
        "     fn:serialize($doc, map { xs:QName(\"exist:add-exist-id\"): \"all\" })";

    @Test
    public void serializeReference() throws XPathException, PermissionDeniedException, EXistException {
        final String expected = "<?pi?><elem xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" exist:id=\"2\" exist:source=\"" + DOC_WITH_PI_NAME.getCollectionPath() + "\" a=\"abc\"><!--comment--><b exist:id=\"2.3\">123</b></elem>";
        expectQueryString(SERIALIZE_WITH_EXIST_ID_ALL_QUERY, expected);
    }

    @BeforeClass
    public static void storeResources() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            createCollection(broker, transaction, TEST_SERIALIZE_COLLECTION,
                Tuple(DOC_WITH_PI_NAME, DOC_WITH_PI)
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

    private static void expectQueryNode(final String query, final Source expected) throws EXistException, XPathException, PermissionDeniedException {
        expectQuery(query, sequence -> {
            assertNotNull(sequence);
            assertTrue(sequence.hasOne());

            final Source actual;
            try {
                actual = Input.fromDocument((Document) sequence.itemAt(0).toJavaObject(Node.class)).build();
            } catch (final XPathException e) {
                throw new AssertionError("Expected Node type result from query");
            }

            final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();

            assertFalse(diff.toString(), diff.hasDifferences());
        });
    }

    private static void expectQueryString(final String query, final String expected) throws EXistException, XPathException, PermissionDeniedException {
        expectQuery(query, sequence -> {
            assertNotNull(sequence);
            assertTrue(sequence.hasOne());

            try {
                assertEquals(expected, sequence.itemAt(0).toJavaObject(String.class));
            } catch (final XPathException e) {
                throw new AssertionError("Expected String type result from query");
            }
        });
    }

    private static void expectQuery(final String query, final Consumer<Sequence> resultConsumer) throws EXistException, XPathException, PermissionDeniedException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final XQuery xquery = pool.getXQueryService();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            final Sequence sequence = xquery.execute(broker, query, null);

            resultConsumer.accept(sequence);
        }
    }
}
