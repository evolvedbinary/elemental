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
import org.exist.collections.Collection;
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
import org.exist.xmldb.DatabaseImpl;
import org.exist.xmldb.EXistCollectionManagementService;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.exist.samples.Samples.SAMPLES;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.xml.sax.SAXException;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;
import xyz.elemental.mediatype.MediaType;

public class CopyCollectionRecoveryTest {

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
    void copyToSubCollection(final BrokerPool pool) {
        final TransactionManager transact = pool.getTransactionManager();
        assertThrows(PermissionDeniedException.class, () -> {
            try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                 final Txn transaction = transact.beginTransaction()) {

            try (final Collection src = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI)) {
                broker.saveCollection(transaction, src);


                try (final Collection dst = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI2)) {
                    broker.saveCollection(transaction, dst);

                fail("expect PermissionDeniedException: Cannot copy collection '/db/test' to it child collection '/db/test/test2'");

                transaction.commit();
            }
        });
    }

    private void store(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = transact.beginTransaction()) {

            try (final Collection root = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI)) {
                broker.saveCollection(transaction, root);
            }

            try (final Collection test = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI.append("test2"))) {
                broker.saveCollection(transaction, test);

                final String sample = getSampleData();
                final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
                broker.storeDocument(transaction, XmldbURI.create("test.xml"), new StringInputSource(sample), xmlMediaType, test);

                try (final Collection dest = broker.getOrCreateCollection(transaction, XmldbURI.ROOT_COLLECTION_URI.append("destination"))) {
                    broker.saveCollection(transaction, dest);

                    broker.copyCollection(transaction, test, dest, XmldbURI.create("test3"));
                }
            }

            transact.commit(transaction);
        }
    }

    private void read(final BrokerPool pool) throws EXistException, PermissionDeniedException, SAXException {
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            final Serializer serializer = broker.borrowSerializer();
            try(final LockedDocument lockedDoc = broker.getXMLResource(XmldbURI.ROOT_COLLECTION_URI.append("destination/test3/test.xml"), LockMode.READ_LOCK)) {
                assertNotNull(lockedDoc, "Document should not be null");
                serializer.serialize(lockedDoc.getDocument());
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

                test2 = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI.append("test2"));
                assertNotNull(test2);
                broker.saveCollection(transaction, test2);

                final String sample = getSampleData();

                final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
                broker.storeDocument(transaction, XmldbURI.create("test.xml"), new StringInputSource(sample), xmlMediaType, test2);

                transact.commit(transaction);
            }

            final Txn transaction = transact.beginTransaction();

            final Collection dest = broker.getOrCreateCollection(transaction, XmldbURI.ROOT_COLLECTION_URI.append("destination"));
            assertNotNull(dest);
            broker.saveCollection(transaction, dest);
            broker.copyCollection(transaction, test2, dest, XmldbURI.create("test3"));

//DO NOT COMMIT TRANSACTION
            pool.getJournalManager().get().flush(true, false);
        }
    }

    private void readAborted(final BrokerPool pool) throws EXistException, PermissionDeniedException {
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            final Serializer serializer = broker.borrowSerializer();
            try(final LockedDocument lockedDoc = broker.getXMLResource(XmldbURI.ROOT_COLLECTION_URI.append("destination/test3/test.xml"), LockMode.READ_LOCK)) {
                assertNull(lockedDoc, "Document should not exist as copy was not committed");
            } finally {
                broker.returnSerializer(serializer);
            }
        }
    }

    private void xmldbStore() throws XMLDBException, IOException {
        try (final org.xmldb.api.base.Collection root = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, "admin", "")) {
            assertNotNull(root);
            EXistCollectionManagementService mgr = root.getService(EXistCollectionManagementService.class);
            assertNotNull(mgr);

            try (final org.xmldb.api.base.Collection test = mgr.createCollection(TestConstants.TEST_COLLECTION_URI.toString())) {
                assertNotNull(test);

                try (final org.xmldb.api.base.Collection test2 = mgr.createCollection(TestConstants.TEST_COLLECTION_URI.append("test2").toString())) {
                    assertNotNull(test2);

                    final String sample = getSampleData();
                    try (final Resource res = test2.createResource("test_xmldb.xml", XMLResource.class)) {
                        assertNotNull(res);
                        res.setContent(sample);
                        test2.storeResource(res);
                    }

                    try (final org.xmldb.api.base.Collection dest = mgr.createCollection("destination")) {
                        assertNotNull(dest);
                        mgr.copy(TestConstants.TEST_COLLECTION_URI2, XmldbURI.ROOT_COLLECTION_URI.append("destination"), XmldbURI.create("test3"));
                    }
                }
            }
        }
    }

    private void xmldbRead() throws XMLDBException {
        try (final org.xmldb.api.base.Collection test = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/destination/test3", "admin", "")) {
            assertNotNull(test);

            try (final Resource res = test.getResource("test_xmldb.xml")) {
                assertNotNull("Document should not be null", res);
            }

            try (final org.xmldb.api.base.Collection root = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, "admin", "")) {
                assertNotNull(root);

                final EXistCollectionManagementService mgr = root.getService(EXistCollectionManagementService.class);
                assertNotNull(mgr);
                mgr.removeCollection("destination");
            }
        }
    }

    private String getSampleData() throws IOException {
        try (final InputStream is = SAMPLES.getBiblioSample()) {
            return InputStreamUtil.readString(is, UTF_8);
        }
    }

    @AfterEach
    void cleanup() {
        BrokerPool.FORCE_CORRUPTION = false;
    }
}
