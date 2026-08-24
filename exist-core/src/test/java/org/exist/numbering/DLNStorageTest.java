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
package org.exist.numbering;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.NodeHandle;
import org.exist.dom.persistent.NodeProxy;
import org.exist.security.PermissionDeniedException;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.StorageAddress;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.w3c.dom.Attr;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DLNStorageTest {

    private static final XmldbURI TEST_COLLECTION = XmldbURI.create(XmldbURI.ROOT_COLLECTION + "/test");

    private static final String TEST_XML =
            "<test>" +
            "<para>My first paragraph.</para>" +
            "<!-- A comment -->" +
            "<para>This one contains a <a href=\"#\">link</a>.</para>" +
            "<?echo \"A processing instruction\"?>" +
            "<para>Another <b>paragraph</b>.</para>" +
            "</test>";

    @Test
    void nodeStorage() throws EXistException, XPathException, PermissionDeniedException {
        final BrokerPool pool = BrokerPool.getInstance();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            // test element ids
            String query = "doc('/db/test/test_string.xml')/test/para";
            Sequence seq;
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                seq = queryResult.result;
                assertEquals(3, seq.getItemCount());
                NodeProxy comment = (NodeProxy) seq.itemAt(0);
                assertEquals("1.1", comment.getNodeId().toString());
                comment = (NodeProxy) seq.itemAt(1);
                assertEquals("1.3", comment.getNodeId().toString());
                comment = (NodeProxy) seq.itemAt(2);
                assertEquals("1.5", comment.getNodeId().toString());
            }

            query = "doc('/db/test/test_string.xml')/test//a";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
                NodeProxy a = (NodeProxy) seq.itemAt(0);
                assertEquals("1.3.2", a.getNodeId().toString());
            }

            // test attribute id
            query = "doc('/db/test/test_string.xml')/test//a/@href";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
                NodeProxy href = (NodeProxy) seq.itemAt(0);
                StorageAddress.toString(href);
                assertEquals("1.3.2.1", href.getNodeId().toString());
                // test Attr deserialization
                Attr attr = (Attr) href.getNode();
                StorageAddress.toString(((NodeHandle) attr));
                // test Attr fields
                assertEquals("href", attr.getNodeName());
                assertEquals("href", attr.getName());
                assertEquals("#", attr.getValue());
                // test DOMFile.getNodeValue()
                assertEquals("#", href.getStringValue());
            }

            // test text node
            query = "doc('/db/test/test_string.xml')/test//b/text()";
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
                seq = queryResult.result;
                assertEquals(1, seq.getItemCount());
                NodeProxy text = (NodeProxy) seq.itemAt(0);
                assertEquals("1.5.2.1", text.getNodeId().toString());
                // test DOMFile.getNodeValue()
                assertEquals("paragraph", text.getStringValue());
                // test Text deserialization
                Text node = (Text) text.getNode();
                assertEquals("paragraph", node.getNodeValue());
                assertEquals("paragraph", node.getData());
            }
        }
    }

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, true);

    @BeforeAll
    static void setUp(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = transact.beginTransaction()) {

            Collection test = broker.getOrCreateCollection(transaction, TEST_COLLECTION);
            broker.saveCollection(transaction, test);

            final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
            broker.storeDocument(transaction, XmldbURI.create("test_string.xml"), new StringInputSource(TEST_XML), xmlMediaType, test);
            //TODO : unlock the collection here ?

            transact.commit(transaction);
        }
    }

    @AfterAll
    static void tearDown(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, TriggerException {
        final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = transact.beginTransaction()) {

            final Collection root = broker.getOrCreateCollection(transaction, XmldbURI.create(XmldbURI.ROOT_COLLECTION + TEST_COLLECTION));
            assertNotNull(root);
            broker.removeCollection(transaction, root);

            transact.commit(transaction);
        }
    }
}
