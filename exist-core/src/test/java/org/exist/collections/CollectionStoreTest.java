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
package org.exist.collections;

import com.evolvedbinary.j8fu.Try;
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
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.test.TestConstants;
import org.exist.util.LockException;
import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import xyz.elemental.mediatype.MediaType;

import javax.xml.transform.Source;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class CollectionStoreTest {

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, true);

    private static final XmldbURI TEST_XML_DOC_URI = XmldbURI.create("test.xml");
    private static final String TEST_XML_DOC = "<test>" + System.currentTimeMillis() + "</test>";

    private static final XmldbURI TEST_BIN_DOC_URI = XmldbURI.create("test.bin");
    private static final String TEST_BIN_DOC = "test " + System.currentTimeMillis();

    @Test
    void store(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            try (final Collection col = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI)) {
                final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
                broker.storeDocument(transaction, TEST_XML_DOC_URI, new StringInputSource(TEST_XML_DOC), xmlMediaType, col);
                broker.saveCollection(transaction, col);
            }

            try (final Collection col = broker.openCollection(TestConstants.TEST_COLLECTION_URI, LockMode.READ_LOCK)) {
                try (final LockedDocument lockedDoc = col.getDocumentWithLock(broker, TEST_XML_DOC_URI, LockMode.READ_LOCK)) {

                    // NOTE: early release of collection lock inline with async locking
                    col.close();

                    if (lockedDoc != null) {
                        final Source expected = Input.fromString(TEST_XML_DOC).build();
                        final Source actual = Input.fromDocument(lockedDoc.getDocument()).build();
                        final Diff diff = DiffBuilder.compare(expected)
                                .withTest(actual)
                                .checkForSimilar()
                                .build();

                        assertFalse(diff.hasDifferences(), diff.toString());
                    }
                }
            }

            transaction.commit();
        }
    }

    @Test
    void storeBinary(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        storeBinary(pool, PreserveType.NO_PRESERVE);
    }

    @Test
    void storeBinary_preserveOnCopy(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        storeBinary(pool, PreserveType.PRESERVE);
    }

    private void storeBinary(final BrokerPool pool, final PreserveType preserveOnCopy) throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            try (final Collection col = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI)) {
                final byte[] bin = TEST_BIN_DOC.getBytes(UTF_8);
                try (final InputStream is = new UnsynchronizedByteArrayInputStream(bin)) {
                    final int docId = broker.getNextResourceId(transaction);
                    final BinaryDocument binDoc = col.addBinaryResource(transaction, broker, new BinaryDocument(null, broker.getBrokerPool(), col, docId, TEST_BIN_DOC_URI), is, MediaType.TEXT_PLAIN, bin.length, null, null, preserveOnCopy);
                    assertNotNull(binDoc);
                }
                broker.saveCollection(transaction, col);
            }

            try (final Collection col = broker.openCollection(TestConstants.TEST_COLLECTION_URI, LockMode.READ_LOCK)) {
                try (final LockedDocument lockedDoc = col.getDocumentWithLock(broker, TEST_BIN_DOC_URI, LockMode.READ_LOCK)) {

                    // NOTE: early release of collection lock inline with async locking
                    col.close();

                    if (lockedDoc != null) {
                        assertInstanceOf(BinaryDocument.class, lockedDoc.getDocument());

                        final BinaryDocument doc = (BinaryDocument)lockedDoc.getDocument();
                        final Try<String, IOException> docContent = broker.withBinaryFile(transaction, doc, is ->
                                Try.TaggedTryUnchecked(IOException.class, () -> new String(Files.readAllBytes(is), UTF_8))
                        );
                        assertEquals(TEST_BIN_DOC, docContent.get());
                    }
                }
            }

            transaction.commit();
        }
    }
}
