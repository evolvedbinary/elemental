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
package org.exist.xquery.functions.system;

import com.evolvedbinary.j8fu.function.ConsumerE;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.security.PermissionDeniedException;
import org.exist.source.DbUriSource;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Type;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.io.IOException;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.test.Util.storeQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class GetMainModuleLoadPathTest {

    private static XmldbURI TEST_COLLECTION_URI = XmldbURI.create("/db/get-main-module-load-path-test");
    private static XmldbURI TEST_SUB_COLLECTION_URI = TEST_COLLECTION_URI.append("sub1");

    private static XmldbURI STANDALONE_MAIN_MODULE_URI = TEST_COLLECTION_URI.append("standalone-main.xq");
    private static String STANDALONE_MAIN_MODULE_XQ =
            "import module namespace system = \"http://exist-db.org/xquery/system\";\n" +
            "document {\n" +
            "  <path>{system:get-main-module-load-path()}</path>\n" +
            "}";

    private static XmldbURI IMPORTED_LIBRARY_MODULE_URI = TEST_SUB_COLLECTION_URI.append("imported-library.xqm");
    private static String IMPORTED_LIBRARY_MODULE_XQ =
            "module namespace ilm = \"http://ilm\";\n" +
            "import module namespace system = \"http://exist-db.org/xquery/system\";\n" +
            "declare function ilm:main-module-load-path() {\n" +
            "  system:get-main-module-load-path()\n" +
            "};";

    private static XmldbURI IMPORTING_MAIN_MODULE_URI = TEST_COLLECTION_URI.append("importing-main.xq");
    private static String IMPORTING_MAIN_MODULE_XQ =
            "import module namespace system = \"http://exist-db.org/xquery/system\";\n" +
            "import module namespace ilm = \"http://ilm\" at \"xmldb:exist://" + IMPORTED_LIBRARY_MODULE_URI.getXmldbURI().toString() + "\";\n" +
            "document {\n" +
            "  <paths>\n" +
            "    <path>{system:get-main-module-load-path()}</path>\n" +
            "    <library-path>{ilm:main-module-load-path()}</library-path>\n" +
            "  </paths>\n" +
            "}";

    @ClassRule
    public static ExistEmbeddedServer EXIST_EMBEDDED_SERVER = new ExistEmbeddedServer(true, true);

    @BeforeClass
    public static void setup() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction =  brokerPool.getTransactionManager().beginTransaction()) {

            // store xquery documents
            try (final Collection testCollection = broker.getOrCreateCollection(transaction, TEST_COLLECTION_URI)) {
                storeQuery(broker, transaction, new StringInputSource(STANDALONE_MAIN_MODULE_XQ.getBytes(UTF_8)), testCollection, STANDALONE_MAIN_MODULE_URI);

                try (final Collection testSubCollection = broker.getOrCreateCollection(transaction, TEST_SUB_COLLECTION_URI)) {
                    storeQuery(broker, transaction, new StringInputSource(IMPORTED_LIBRARY_MODULE_XQ.getBytes(UTF_8)), testSubCollection, IMPORTED_LIBRARY_MODULE_URI);
                }

                storeQuery(broker, transaction, new StringInputSource(IMPORTING_MAIN_MODULE_XQ.getBytes(UTF_8)), testCollection, IMPORTING_MAIN_MODULE_URI);
            }

            transaction.commit();
        }
    }

    @Test
    public void standaloneMainModule() throws EXistException, PermissionDeniedException, XPathException, IOException {
        final String expected = "<path>.</path>";

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction =  brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(STANDALONE_MAIN_MODULE_XQ), false, null, null, null, null, null)) {
                assertNotNull(queryResult.result);
                assertEquals(1, queryResult.result.getItemCount());
                final Item item = queryResult.result.itemAt(0);
                assertEquals(Type.DOCUMENT, item.getType());

                final Source expectedSrc = Input.fromString(expected).build();
                final Source actualSrc = Input.fromNode((Document) item).build();

                final Diff diff = DiffBuilder.compare(expectedSrc)
                        .withTest(actualSrc)
                        .checkForSimilar()
                        .build();

                assertFalse(diff.toString(), diff.hasDifferences());
            }

            transaction.commit();
        }
    }

    @Test
    public void importingMainModule() throws EXistException, PermissionDeniedException, XPathException, IOException {
        final String expected =
                "<paths>" +
                "<path>.</path>" +
                "<library-path>.</library-path>" +
                "</paths>";

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction =  brokerPool.getTransactionManager().beginTransaction()) {

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(IMPORTING_MAIN_MODULE_XQ), false, null, null, null, null, null)) {
                assertNotNull(queryResult.result);
                assertEquals(1, queryResult.result.getItemCount());
                final Item item = queryResult.result.itemAt(0);
                assertEquals(Type.DOCUMENT, item.getType());

                final Source expectedSrc = Input.fromString(expected).build();
                final Source actualSrc = Input.fromNode((Document) item).build();

                final Diff diff = DiffBuilder.compare(expectedSrc)
                        .withTest(actualSrc)
                        .checkForSimilar()
                        .build();

                assertFalse(diff.toString(), diff.hasDifferences());
            }

            transaction.commit();
        }
    }


    @Test
    public void storedStandaloneMainModule() throws EXistException, PermissionDeniedException, XPathException, IOException {
        final String expected = "<path>" + TEST_COLLECTION_URI.getCollectionPath() + "</path>";

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction =  brokerPool.getTransactionManager().beginTransaction()) {

            final ConsumerE<XQueryContext, XPathException> preCompilationContextSetup = xqueryContext -> xqueryContext.setModuleLoadPath(STANDALONE_MAIN_MODULE_URI.removeLastSegment().getCollectionPath());

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, DbUriSource.from(brokerPool, STANDALONE_MAIN_MODULE_URI, false, false), false, null, null, preCompilationContextSetup, null, null)) {
                assertNotNull(queryResult.result);
                assertEquals(1, queryResult.result.getItemCount());
                final Item item = queryResult.result.itemAt(0);
                assertEquals(Type.DOCUMENT, item.getType());

                final Source expectedSrc = Input.fromString(expected).build();
                final Source actualSrc = Input.fromNode((Document) item).build();

                final Diff diff = DiffBuilder.compare(expectedSrc)
                        .withTest(actualSrc)
                        .checkForSimilar()
                        .build();

                assertFalse(diff.toString(), diff.hasDifferences());
            }

            transaction.commit();
        }
    }

    @Test
    public void storedImportingMainModule() throws EXistException, PermissionDeniedException, XPathException, IOException {
        final String expected =
                "<paths>" +
                "<path>" + TEST_COLLECTION_URI.getCollectionPath() + "</path>" +
                "<library-path>" + TEST_COLLECTION_URI.getCollectionPath() + "</library-path>" +
                "</paths>";

        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction =  brokerPool.getTransactionManager().beginTransaction()) {

            final ConsumerE<XQueryContext, XPathException> preCompilationContextSetup = xqueryContext -> xqueryContext.setModuleLoadPath(IMPORTING_MAIN_MODULE_URI.removeLastSegment().getCollectionPath());


            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, DbUriSource.from(brokerPool, IMPORTING_MAIN_MODULE_URI, false, false), false, null, null, preCompilationContextSetup, null, null)) {
                assertNotNull(queryResult.result);
                assertEquals(1, queryResult.result.getItemCount());
                final Item item = queryResult.result.itemAt(0);
                assertEquals(Type.DOCUMENT, item.getType());

                final Source expectedSrc = Input.fromString(expected).build();
                final Source actualSrc = Input.fromNode((Document) item).build();

                final Diff diff = DiffBuilder.compare(expectedSrc)
                        .withTest(actualSrc)
                        .checkForSimilar()
                        .build();

                assertFalse(diff.toString(), diff.hasDifferences());
            }

            transaction.commit();
        }
    }
}
