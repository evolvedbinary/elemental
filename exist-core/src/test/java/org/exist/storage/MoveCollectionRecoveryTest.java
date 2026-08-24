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
package org.exist.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.exist.EXistException;
import org.exist.TestUtils;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.lock.Lock.LockMode;
import org.exist.storage.serializers.Serializer;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.test.TestConstants;
import org.exist.util.DatabaseConfigurationException;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.util.io.InputStreamUtil;
import org.exist.xmldb.EXistCollectionManagementService;
import org.exist.xmldb.DatabaseImpl;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xml.sax.SAXException;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;
import xyz.elemental.mediatype.MediaType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.exist.samples.Samples.SAMPLES;

public class MoveCollectionRecoveryTest {

    @RegisterExtension
    public EmbeddedDatabaseExtension embeddedDatabase = new EmbeddedDatabaseExtension(true, true);

    @Test
    void storeAndRead(final BrokerPool pool) throws EXistException, DatabaseConfigurationException, LockException, PermissionDeniedException, SAXException, IOException {
        BrokerPool.FORCE_CORRUPTION = true;
        store(pool);

        embeddedDatabase.restart();

        BrokerPool.FORCE_CORRUPTION = false;
        read(pool);
    }

    @Test
    void storeAndReadAborted(final BrokerPool pool) throws EXistException, DatabaseConfigurationException, LockException, PermissionDeniedException, SAXException, IOException {
        BrokerPool.FORCE_CORRUPTION = true;
        storeAborted(pool);

        embeddedDatabase.restart();

        BrokerPool.FORCE_CORRUPTION = false;
        readAborted(pool);
    }

    @Test
    void storeAndReadXmldb() throws DatabaseConfigurationException, XMLDBException, EXistException, IOException {
        // initialize xml:db driver
        final Database database = new DatabaseImpl();
        database.setProperty("create-database", "true");
        DatabaseManager.registerDatabase(database);


        BrokerPool.FORCE_CORRUPTION = false;
        xmldbStore();

        embeddedDatabase.restart();

        BrokerPool.FORCE_CORRUPTION = false;
        xmldbRead();
    }

    @Test
    void moveToSelfSubCollection(final BrokerPool pool) throws EXistException, IOException, PermissionDeniedException, TriggerException, LockException {
        final TransactionManager transact = pool.getTransactionManager();
        assertThrows(PermissionDeniedException.class, () -> {
            try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                 final Txn transaction = transact.beginTransaction()) {

            try (final Collection src = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI)) {
                assertNotNull(src);
                broker.saveCollection(transaction, src);

                try (final Collection dst = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI2)) {
                    assertNotNull(dst);
                    broker.saveCollection(transaction, dst);

                    broker.moveCollection(transaction, src, dst, src.getURI().lastSegment());
                }
            }

                fail("expect PermissionDeniedException: Cannot move collection '/db/test' to it child collection '/db/test/test2'");

                transact.commit(transaction);
            }
        });
    }

    private void store(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = transact.beginTransaction()) {

            try (final Collection root = broker.getOrCreateCollection(transaction,	TestConstants.TEST_COLLECTION_URI)) {
                assertNotNull(root);
                broker.saveCollection(transaction, root);
            }

            try (final Collection test = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI2)) {
                assertNotNull(test);
                broker.saveCollection(transaction, test);


                final String sample;
                try (final InputStream is = SAMPLES.getBiblioSample()) {
                    assertNotNull(is);
                    sample = InputStreamUtil.readString(is, UTF_8);
                }

                final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
                broker.storeDocument(transaction, TestConstants.TEST_XML_URI, new StringInputSource(sample), xmlMediaType, test);

                try (final Collection dest = broker.getOrCreateCollection(transaction, TestConstants.DESTINATION_COLLECTION_URI)) {
                    assertNotNull(dest);
                    broker.saveCollection(transaction, dest);
                    broker.moveCollection(transaction, test, dest, XmldbURI.create("test3"));
                }
            }

            transact.commit(transaction);
        }
    }

    private void read(final BrokerPool pool) throws EXistException, PermissionDeniedException, SAXException {
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            final Serializer serializer = broker.borrowSerializer();

            try(final LockedDocument lockedDoc =  broker.getXMLResource(TestConstants.DESTINATION_COLLECTION_URI.append("test3").append(TestConstants.TEST_XML_URI), LockMode.READ_LOCK)) {
                assertNotNull(lockedDoc, "Document should not be null");
                final String data = serializer.serialize(lockedDoc.getDocument());
                assertNotNull(data);
            } finally {
                broker.returnSerializer(serializer);
            }
        }
    }

    private void storeAborted(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            Collection test2;

            try(final Txn transaction = transact.beginTransaction()) {

                final Collection root = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI);
                assertNotNull(root);
                broker.saveCollection(transaction, root);

                test2 = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI2);
                assertNotNull(test2);
                broker.saveCollection(transaction, test2);

                final String sample;
                try (final InputStream is = SAMPLES.getBiblioSample()) {
                    assertNotNull(is);
                    sample = InputStreamUtil.readString(is, UTF_8);
                }
                final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
                broker.storeDocument(transaction, TestConstants.TEST_XML_URI, new StringInputSource(sample), xmlMediaType, test2);

                transact.commit(transaction);
            }

            final Txn transaction = transact.beginTransaction();
            assertNotNull(transaction);

            final Collection dest = broker.getOrCreateCollection(transaction, TestConstants.DESTINATION_COLLECTION_URI2);
            assertNotNull(dest);
            broker.saveCollection(transaction, dest);
            broker.moveCollection(transaction, test2, dest, XmldbURI.create("test3"));

//          Don't commit...
            pool.getJournalManager().get().flush(true, false);
        }
    }

    private void readAborted(final BrokerPool pool) throws EXistException, PermissionDeniedException {
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            final Serializer serializer = broker.borrowSerializer();
            try(final LockedDocument lockedDoc = broker.getXMLResource(TestConstants.DESTINATION_COLLECTION_URI2.append("test3").append(TestConstants.TEST_XML_URI), LockMode.READ_LOCK)) {
                assertNull(lockedDoc, "Document should be null");
            } finally {
                broker.returnSerializer(serializer);
            }
        }
    }

    private void xmldbStore() throws XMLDBException, IOException {
        try (final org.xmldb.api.base.Collection root = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, "admin", "")) {
            assertNotNull(root);
            final EXistCollectionManagementService rootMgr = root.getService(EXistCollectionManagementService.class);
            assertNotNull(rootMgr);

            try (final org.xmldb.api.base.Collection test = rootMgr.createCollection("test")) {

                final EXistCollectionManagementService testMgr = test.getService(EXistCollectionManagementService.class);
                try (final org.xmldb.api.base.Collection test2 = testMgr.createCollection("test2")) {
                    assertNotNull(test2);

                    final String sample;
                    try (final InputStream is = SAMPLES.getBiblioSample()) {
                        assertNotNull(is);
                        sample = InputStreamUtil.readString(is, UTF_8);
                    }

                    try (final Resource res = test2.createResource("test_xmldb.xml", XMLResource.class)) {
                        assertNotNull(res);
                        res.setContent(sample);
                        test2.storeResource(res);
                    }

                    try (final org.xmldb.api.base.Collection dest = rootMgr.createCollection(TestConstants.DESTINATION_COLLECTION_URI3.lastSegment().toString())) {
                        assertNotNull(dest);
                        rootMgr.move(TestConstants.TEST_COLLECTION_URI2, TestConstants.DESTINATION_COLLECTION_URI3, XmldbURI.create("test3"));
                    }
                }
            }
        }
    }

    private void xmldbRead() throws XMLDBException {
        try (final org.xmldb.api.base.Collection test = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + TestConstants.DESTINATION_COLLECTION_URI3.lastSegment().toString() + "/test3", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
            assertNotNull(test);
            try (final Resource res = test.getResource("test_xmldb.xml")) {
                assertNotNull("Document should not be null", res);
            }
        }
    }

    @AfterEach
    void cleanup() {
        BrokerPool.FORCE_CORRUPTION = false;
    }
}
