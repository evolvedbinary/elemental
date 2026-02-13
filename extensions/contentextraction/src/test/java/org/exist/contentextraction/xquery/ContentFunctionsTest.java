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
package org.exist.contentextraction.xquery;

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
import org.exist.xquery.XQueryUtil;
import org.junit.*;
import xyz.elemental.mediatype.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

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
                    collection.addBinaryResource(transaction, broker, XmldbURI.create("minimal.pdf"), is, MediaType.APPLICATION_PDF, -1);
                }

                try (final InputStream is = ContentFunctionsTest.class.getResourceAsStream("test.xlsx")) {
                    assertNotNull(is);
                    collection.addBinaryResource(transaction, broker, XmldbURI.create("test.xlsx"), is, MediaType.APPLICATION_OPENXML_SPREADSHEET, -1);
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

            final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, mainQuerySource, false, null, null, null, null, null);

            assertEquals(2, queryResult.result.getItemCount());

            transaction.commit();

            assertEquals(1, (int) queryResult.result.itemAt(0).toJavaObject(int.class));
            assertEquals(MediaType.APPLICATION_PDF, queryResult.result.itemAt(1).getStringValue());
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

            final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, mainQuerySource, false, null, null, null, null, null);

            assertEquals(1, queryResult.result.getItemCount());

            transaction.commit();

            assertEquals("Hello World", queryResult.result.itemAt(0).getStringValue());
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

            final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, mainQuerySource, false, null, null, null, null, null);

            assertEquals(2, queryResult.result.getItemCount());

            transaction.commit();

            assertEquals(1, (int) queryResult.result.itemAt(0).toJavaObject(int.class));
            assertEquals(MediaType.APPLICATION_PDF, queryResult.result.itemAt(1).getStringValue());
        }
    }
}
