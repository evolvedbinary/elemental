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
package org.exist.contentextraction.xquery;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.security.PermissionDeniedException;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Sequence;
import org.junit.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.contentextraction.xquery.Util.executeQuery;
import static org.exist.contentextraction.xquery.Util.withCompiledQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ContentFunctionsTest {

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @BeforeClass
    public static void setup() throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            try (final Collection collection = broker.getOrCreateCollection(transaction, XmldbURI.create("/db/content-functions-test"))) {

                try (final InputStream is = ContentFunctionsTest.class.getResourceAsStream("minimal.pdf")) {
                    assertNotNull(is);
                    collection.addBinaryResource(transaction, broker, XmldbURI.create("minimal.pdf"), is, "application/pdf", -1);
                }

                try (final InputStream is = ContentFunctionsTest.class.getResourceAsStream("test.xlsx")) {
                    assertNotNull(is);
                    collection.addBinaryResource(transaction, broker, XmldbURI.create("test.xlsx"), is, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", -1);
                }

            }

            transaction.commit();
        }
    }

    @AfterClass
    public static void teardown() throws EXistException, PermissionDeniedException, IOException, TriggerException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            try (final Collection collection = broker.openCollection(XmldbURI.create("/db/content-functions-test"), Lock.LockMode.WRITE_LOCK)) {
                if (collection != null) {
                    broker.removeCollection(transaction, collection);
                }
            }
        }
    }

    @Test
    public void getMetadataFromPdf() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final String mainQuery =
                "declare namespace html = \"http://www.w3.org/1999/xhtml\";\n" +
                "declare namespace contentextraction = \"http://exist-db.org/xquery/contentextraction\";\n" +
                "declare namespace util = \"http://exist-db.org/xquery/util\";\n" +
                "let $bin := util:binary-doc(\"/db/content-functions-test/minimal.pdf\")\n" +
                "  return\n" +
                "    contentextraction:get-metadata($bin)//html:meta[@name = (\"xmpTPg:NPages\", \"Content-Type\")]/@content";

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final Source mainQuerySource = new StringSource(mainQuery);
        try (final DBBroker broker = pool.getBroker();
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            final Tuple2<Integer, String> metadata = withCompiledQuery(broker, mainQuerySource, mainCompiledQuery -> {
                final Sequence result = executeQuery(broker, mainCompiledQuery);
                assertEquals(2, result.getItemCount());

                return Tuple(result.itemAt(0).toJavaObject(int.class), result.itemAt(1).getStringValue());
            });

            transaction.commit();

            assertEquals(1, metadata._1.intValue());
            assertEquals("application/pdf", metadata._2);
        }
    }

    @Test
    public void getMetadataAndContentFromPdf() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final String mainQuery =
                "declare namespace html = \"http://www.w3.org/1999/xhtml\";\n" +
                "declare namespace contentextraction = \"http://exist-db.org/xquery/contentextraction\";\n" +
                "declare namespace util = \"http://exist-db.org/xquery/util\";\n" +
                "let $bin := util:binary-doc(\"/db/content-functions-test/minimal.pdf\")\n" +
                "  return\n" +
                "    contentextraction:get-metadata-and-content($bin)//html:p[2]/string()";

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final Source mainQuerySource = new StringSource(mainQuery);
        try (final DBBroker broker = pool.getBroker();
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            final String content = withCompiledQuery(broker, mainQuerySource, mainCompiledQuery -> {
                final Sequence result = executeQuery(broker, mainCompiledQuery);
                assertEquals(1, result.getItemCount());

                return result.itemAt(0).getStringValue();
            });

            transaction.commit();

            assertEquals("Hello World", content);
        }
    }

    @Ignore("see https://github.com/eXist-db/exist/issues/3835")
    @Test
    public void getMetadataFromXlsx() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final String mainQuery =
                "declare namespace html = \"http://www.w3.org/1999/xhtml\";\n" +
                        "declare namespace contentextraction = \"http://exist-db.org/xquery/contentextraction\";\n" +
                        "declare namespace util = \"http://exist-db.org/xquery/util\";\n" +
                        "let $bin := util:binary-doc(\"/db/content-functions-test/test.xlsx\")\n" +
                        "  return\n" +
                        "    contentextraction:get-metadata($bin)//html:meta[@name = (\"xmpTPg:NPages\", \"Content-Type\")]/@content";

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final Source mainQuerySource = new StringSource(mainQuery);
        try (final DBBroker broker = pool.getBroker();
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            final Tuple2<Integer, String> metadata = withCompiledQuery(broker, mainQuerySource, mainCompiledQuery -> {
                final Sequence result = executeQuery(broker, mainCompiledQuery);
                assertEquals(2, result.getItemCount());

                return Tuple(result.itemAt(0).toJavaObject(int.class), result.itemAt(1).getStringValue());
            });

            transaction.commit();

            assertEquals(1, metadata._1.intValue());
            assertEquals("application/pdf", metadata._2);
        }
    }
}
