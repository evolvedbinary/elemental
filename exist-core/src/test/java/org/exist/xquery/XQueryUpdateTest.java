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
package org.exist.xquery;

import java.io.IOException;
import java.util.Optional;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.security.PermissionDeniedException;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.serializers.Serializer;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.ClassRule;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import static org.junit.jupiter.api.Assertions.*;

public class XQueryUpdateTest {

    protected static XmldbURI TEST_COLLECTION = XmldbURI.create(XmldbURI.ROOT_COLLECTION + "/test");

    protected static String TEST_XML =
            "<?xml version=\"1.0\"?>" +
                    "<products/>";

    protected static String UPDATE_XML =
            "<progress total=\"100\" done=\"0\" failed=\"0\" passed=\"0\"/>";

    protected final static int ITEMS_TO_APPEND = 500;

    @Test
    void append() throws EXistException, PermissionDeniedException, XPathException, SAXException, IOException {
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            String query =
            	"   declare variable $i external;\n" +
            	"	update insert\n" +
            	"		<product id='id{$i}' num='{$i}'>\n" +
            	"			<description>Description {$i}</description>\n" +
            	"			<price>{$i + 1.0}</price>\n" +
            	"			<stock>{$i * 10}</stock>\n" +
            	"		</product>\n" +
            	"	into /products";
            final XQueryContext context = new XQueryContext(pool);
            final CompiledXQuery compiled = pool.getXQueryService().compile(context, query);
            for (int i = 0; i < ITEMS_TO_APPEND; i++) {
                context.declareVariable("i", true, Integer.valueOf(i));
                pool.getXQueryService().execute(broker, compiled, null);
            }

            query = "/products";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());

                final Serializer serializer = broker.borrowSerializer();
                try {
                    serializer.serialize((NodeValue) seq.itemAt(0));
                } finally {
                    broker.returnSerializer(serializer);
                }
            }

            query = "//product";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }

            query = "//product[price > 0.0]";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }
        }
    }

    @Test
    void appendAttributes() throws EXistException, PermissionDeniedException, XPathException, SAXException, LockException, IOException {

        append();

        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            String query =
            	"   declare variable $i external;\n" +
            	"	update insert\n" +
            	"		attribute name { concat('n', $i) }\n" +
            	"	into //product[@num = $i]";
            final XQueryContext context = new XQueryContext(pool);
            final CompiledXQuery compiled = pool.getXQueryService().compile(context, query);
            for (int i = 0; i < ITEMS_TO_APPEND; i++) {
                context.declareVariable("i", true, Integer.valueOf(i));
                pool.getXQueryService().execute(broker, compiled, null);
            }

            query = "/products";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());

                final Serializer serializer = broker.borrowSerializer();
                try {
                    serializer.serialize((NodeValue) seq.itemAt(0));
                } finally {
                    broker.returnSerializer(serializer);
                }
            }

            query = "//product";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }

            query = "//product[@name = 'n20']";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
            }

            store(broker, "attribs.xml", "<test attr1='aaa' attr2='bbb'>ccc</test>");
            query = "update insert attribute attr1 { 'eee' } into /test";

            //testing duplicate attribute ...
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

            query = "doc('" + TEST_COLLECTION + "/attribs.xml')/test[@attr1 = 'eee']";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
                final Serializer serializer = broker.borrowSerializer();
                try {
                    serializer.serialize((NodeValue) seq.itemAt(0));
                } finally {
                    broker.returnSerializer(serializer);
                }
            }
        }
    }

    @Test
    void insertBefore() throws EXistException, PermissionDeniedException, XPathException, SAXException, IOException {
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            String query =
                    "   update insert\n" +
                            "       <product id='original'>\n" +
                            "           <description>Description</description>\n" +
                            "           <price>0</price>\n" +
                            "           <stock>10</stock>\n" +
                            "       </product>\n" +
                            "   into /products";

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

            query = "//product";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
            }

            query =
                "   declare variable $i external;\n" +
                "   update insert\n" +
                "       <product id='id{$i}'>\n" +
                "           <description>Description {$i}</description>\n" +
                "           <price>{$i + 1.0}</price>\n" +
                "           <stock>{$i * 10}</stock>\n" +
                "       </product>\n" +
                "   preceding /products/product[1]";
            final XQueryContext context = new XQueryContext(pool);
            final CompiledXQuery compiled = pool.getXQueryService().compile(context, query);
            for (int i = 0; i < ITEMS_TO_APPEND; i++) {
                context.declareVariable("i", true, Integer.valueOf(i));
                pool.getXQueryService().execute(broker, compiled, null);
            }

            query = "/products";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());

                final Serializer serializer = broker.borrowSerializer();
                try {
                    serializer.serialize((NodeValue) seq.itemAt(0));
                } finally {
                    broker.returnSerializer(serializer);
                }
            }

            query = "//product";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND + 1, seq.getItemCount());
            }

            query = "//product[price > 0.0]";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }
        }
    }

    @Test
    void insertAfter() throws EXistException, PermissionDeniedException, XPathException, SAXException, IOException {
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            String query =
                    "   update insert\n" +
                            "       <product id='original'>\n" +
                            "           <description>Description</description>\n" +
                            "           <price>0</price>\n" +
                            "           <stock>10</stock>\n" +
                            "       </product>\n" +
                            "   into /products";

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

            query = "//product";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
            }

            query =
                "   declare variable $i external;\n" +
                "   update insert\n" +
                "       <product id='id{$i}'>\n" +
                "           <description>Description {$i}</description>\n" +
                "           <price>{$i + 1.0}</price>\n" +
                "           <stock>{$i * 10}</stock>\n" +
                "       </product>\n" +
                "   following /products/product[1]";
            final XQueryContext context = new XQueryContext(pool);
            final CompiledXQuery compiled = pool.getXQueryService().compile(context, query);
            for (int i = 0; i < ITEMS_TO_APPEND; i++) {
                context.declareVariable("i", true, Integer.valueOf(i));
                pool.getXQueryService().execute(broker, compiled, null);
            }

            query = "/products";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());

                final Serializer serializer = broker.borrowSerializer();
                try {
                    serializer.serialize((NodeValue) seq.itemAt(0));
                } finally {
                    broker.returnSerializer(serializer);
                }
            }

            query = "//product";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND + 1, seq.getItemCount());
            }

            query = "//product[price > 0.0]";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }
        }
    }

    @Test
    void update() throws EXistException, PermissionDeniedException, XPathException, SAXException, IOException {

        append();

        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            String query =
            	"declare option exist:output-size-limit '-1';\n" +
            	"for $prod at $i in //product return\n" +
                "	update value $prod/description\n" +
                "	with 'Updated Description ' || $i";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

            query = "count(//product[starts-with(description, 'Updated')])";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, (int)seq.itemAt(0).toJavaObject(int.class));
            }

            for (int i = 1; i <= ITEMS_TO_APPEND; i++) {
                query = "//product[description eq 'Updated Description " + i + "']";
                try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                    final Sequence seq = queryResult.result;
                    assertEquals(1, seq.getItemCount());
                }
            }

            query = "//product[stock cast as xs:double gt 400]";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(459, seq.getItemCount());
            }

            query = "//product[starts-with(stock, '401')]";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
            }

            query = "/products";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
            }

            query = "//product[@num cast as xs:integer eq 3]";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
            }

            query = "/products";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
            }

            query =
                    "declare option exist:output-size-limit '-1';\n" +
                            "for $prod in //product return\n" +
                            "	update value $prod/stock\n" +
                            "	with (<local>10</local>,<external>1</external>)";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

            query = "//product/stock/external[. cast as xs:integer eq 1]";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }
        }
    }

    @Test
    void remove() throws EXistException, PermissionDeniedException, XPathException, SAXException, IOException {

        append();

        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

        	String query =
        		"for $prod in //product return\n" +
        		"	update delete $prod\n";
        	try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

        	query = "//product";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(0, seq.getItemCount());
            }

        }
    }

    @Test
    void rename() throws EXistException, PermissionDeniedException, XPathException, SAXException, IOException {

        append();

        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            String query =
            	"for $prod in //product return\n" +
            	"	update rename $prod/description as 'desc'\n";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

            query = "//product/desc";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }

            query =
            	"for $prod in //product return\n" +
            	"	update rename $prod/@num as 'count'\n";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

            query = "//product/@count";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }

        }
    }

    @Test
    void replace() throws EXistException, PermissionDeniedException, XPathException, SAXException, IOException {

        append();

        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            String query =
            	"for $prod in //product return\n" +
            	"	update replace $prod/description with <desc>An updated description.</desc>\n";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

            query = "//product/desc";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }

            query =
            	"for $prod in //product return\n" +
            	"	update replace $prod/@num with '1'\n";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

            query = "//product/@num";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }

            query =
            	"for $prod in //product return\n" +
            	"	update replace $prod/desc/text() with 'A new update'\n";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

            query = "//product[starts-with(desc, 'A new')]";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }
        }
    }

    @Test
    void attrUpdate() throws EXistException, LockException, SAXException, PermissionDeniedException, IOException, XPathException {
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            store(broker, "test.xml", UPDATE_XML);

            String query =
                    "let $progress := /progress\n" +
                    "for $i in 1 to 100\n" +
                    "let $done := $progress/@done\n" +
                    "return (\n" +
                    "   update value $done with xs:int($done + 1),\n" +
                    "   xs:int(/progress/@done)\n" +
                    ")";

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }
        }
    }

    @Test
    void appendCDATA() throws EXistException, PermissionDeniedException, XPathException, SAXException, IOException {
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            String query =
            	"	update insert\n" +
            	"		<product>\n" +
            	"			<description><![CDATA[me & you <>]]></description>\n" +
            	"		</product>\n" +
            	"	into /products";
            final XQueryContext context = new XQueryContext(pool);
            final CompiledXQuery compiled = pool.getXQueryService().compile(context, query);
            for (int i = 0; i < ITEMS_TO_APPEND; i++) {
                pool.getXQueryService().execute(broker, compiled, null);
            }

            query = "/products";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(1, seq.getItemCount());

                final Serializer serializer = broker.borrowSerializer();
                try {
                    serializer.serialize((NodeValue) seq.itemAt(0));
                } finally {
                    broker.returnSerializer(serializer);
                }
            }

            query = "//product";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence seq = queryResult.result;
                assertEquals(ITEMS_TO_APPEND, seq.getItemCount());
            }
        }
    }

    @Test
    void insertAttrib() throws EXistException, PermissionDeniedException, XPathException, IOException {
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            String query =
                "declare namespace xmldb = 'http://exist-db.org/xquery/xmldb'; "+
                "let $uri := xmldb:store('/db', 'insertAttribDoc.xml', <C/>) "+
                "let $node := doc($uri)/element() "+
                "let $attrib := <Value f='ATTRIB VALUE'/>/@* "+
                "return update insert $attrib into $node";

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                // no result access needed
            }

			query = "doc('/db/insertAttribDoc.xml')/element()[@f eq 'ATTRIB VALUE']";
			try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                final Sequence result = queryResult.result;
                assertFalse(result.isEmpty());
            }
        }
    }

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, true);

    @BeforeEach
    void loadTestData() throws EXistException, LockException, SAXException, PermissionDeniedException, IOException {
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            store(broker, "test.xml", TEST_XML);
        }
    }

    @AfterEach
    void removeTestData() throws EXistException, PermissionDeniedException, IOException, TriggerException {
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final Txn transaction = transact.beginTransaction()) {

            final Collection root = broker.getOrCreateCollection(transaction, TEST_COLLECTION);
            assertNotNull(root);
            broker.removeCollection(transaction, root);

            transact.commit(transaction);
        }
    }


    private void store(DBBroker broker, String docName, String data) throws PermissionDeniedException, EXistException, SAXException, LockException, IOException {
        Collection root;
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        final TransactionManager mgr = pool.getTransactionManager();
        try (final Txn transaction = mgr.beginTransaction()) {

            root = broker.getOrCreateCollection(transaction, TEST_COLLECTION);
            broker.saveCollection(transaction, root);

            final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
            broker.storeDocument(transaction, XmldbURI.create(docName), new StringInputSource(data), xmlMediaType, root);
            //TODO : unlock the collection here ?

            mgr.commit(transaction);
        }
        final DocumentImpl doc = root.getDocument(broker, XmldbURI.create(docName));
        final Serializer serializer = broker.borrowSerializer();
        try {
            serializer.serialize(doc);
        } finally {
            broker.returnSerializer(serializer);
        }
    }
}
