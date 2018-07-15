/*
 * eXist Open Source Native XML Database
 * Copyright (C) 2001-2018 The eXist Project
 * http://exist-db.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.exist.collections;

import org.exist.EXistException;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.BinaryDocument;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.DBBroker.PreserveType;
import org.exist.storage.lock.Lock.LockMode;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.test.TestConstants;
import org.exist.util.LockException;
import org.exist.util.io.FastByteArrayInputStream;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CollectionStoreTest {

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    private static final XmldbURI TEST_XML_DOC_URI = XmldbURI.create("test.xml");
    private static final String TEST_XML_DOC = "<test>" + System.currentTimeMillis() + "</test>";

    private static final XmldbURI TEST_BIN_DOC_URI = XmldbURI.create("test.bin");
    private static final String TEST_BIN_DOC = "test " + System.currentTimeMillis();

    @Test
    public void store() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final BrokerPool pool =  existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            try (final Collection col = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI)) {
                final IndexInfo indexInfo = col.validateXMLResource(transaction, broker, TEST_XML_DOC_URI, TEST_XML_DOC);
                col.store(transaction, broker, indexInfo, TEST_XML_DOC);
                broker.saveCollection(transaction, col);
            }

            try (final Collection col = broker.openCollection(TestConstants.TEST_COLLECTION_URI, LockMode.READ_LOCK)) {
                try (final LockedDocument lockedDoc = col.getDocumentWithLock(broker, TEST_XML_DOC_URI, LockMode.READ_LOCK)) {

                    // NOTE: early release of collection lock inline with async locking
                    col.close();

                    if (lockedDoc != null) {
                        final Source expected = Input.fromString(TEST_XML_DOC).build();
                        final Source actual = Input.fromDocument(lockedDoc.getDocument()).build();
                        final Diff diff = DiffBuilder.compare(actual)
                                .withTest(expected)
                                .checkForSimilar()
                                .build();

                        assertFalse(diff.toString(), diff.hasDifferences());
                    }
                }
            }
        }
    }

    @Test
    public void storeBinary() throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        storeBinary(PreserveType.NO_PRESERVE);
    }

    @Test
    public void storeBinary_preserveOnCopy() throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        storeBinary(PreserveType.PRESERVE);
    }

    private void storeBinary(final PreserveType preserveOnCopy) throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        final BrokerPool pool =  existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            try (final Collection col = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI)) {
                final byte[] bin = TEST_BIN_DOC.getBytes(UTF_8);
                try (final InputStream is = new FastByteArrayInputStream(bin)) {
                    final BinaryDocument binDoc = col.addBinaryResource(transaction, broker, new BinaryDocument(broker.getBrokerPool(), col, TEST_BIN_DOC_URI), is, "text/plain", bin.length, null, null, preserveOnCopy);
                    assertNotNull(binDoc);
                }
                broker.saveCollection(transaction, col);
            }

            try (final Collection col = broker.openCollection(TestConstants.TEST_COLLECTION_URI, LockMode.READ_LOCK)) {
                try (final LockedDocument lockedDoc = col.getDocumentWithLock(broker, TEST_BIN_DOC_URI, LockMode.READ_LOCK)) {

                    // NOTE: early release of collection lock inline with async locking
                    col.close();

                    if (lockedDoc != null) {
                        assertTrue(lockedDoc.getDocument() instanceof BinaryDocument);

                        final BinaryDocument doc = (BinaryDocument)lockedDoc.getDocument();

                        final Path docData = broker.getBinaryFile(doc);
                        assertEquals(TEST_BIN_DOC, new String(Files.readAllBytes(docData), UTF_8));
                    }
                }
            }
        }
    }
}
