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

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import xyz.elemental.mediatype.MediaType;

import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class BinaryDocumentTest {

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, true);

    @Test
    void removeCollection() throws PermissionDeniedException, IOException, SAXException, LockException, EXistException {
        final XmldbURI testCollectionUri = XmldbURI.create("/db/remove-collection-test");
        final XmldbURI thingUri = testCollectionUri.append("thing");

        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            // create a collection
            final Collection thingCollection = broker.getOrCreateCollection(transaction, thingUri);
            broker.saveCollection(transaction, thingCollection);

            final MediaType binMediaType = pool.getMediaTypeService().getMediaTypeResolver().forUnknown();

            // add a binary document to the collection
            broker.storeDocument(transaction, XmldbURI.create("file1.bin"), new StringInputSource("binary-file1".getBytes(UTF_8)), binMediaType, thingCollection);

            // remove the collection
            assertTrue(broker.removeCollection(transaction, thingCollection));

            // try and store a binary doc with the same name as the thing collection (should succeed)
            final Collection testCollection = broker.getCollection(testCollectionUri);
            broker.storeDocument(transaction, XmldbURI.create("thing"), new StringInputSource("binary-file2".getBytes(UTF_8)), binMediaType, testCollection);
        }
    }

    @Test
    void overwriteCollection() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final XmldbURI testCollectionUri = XmldbURI.create("/db/overwrite-collection-test");
        final XmldbURI thingUri = testCollectionUri.append("thing");

        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            // create a collection
            final Collection thingCollection = broker.getOrCreateCollection(transaction, thingUri);
            broker.saveCollection(transaction, thingCollection);

            // attempt to create a binary document with the same uri as the thingCollection (should fail)
            final Collection testCollection = broker.getCollection(testCollectionUri);

            try {
                final MediaType binMediaType = pool.getMediaTypeService().getMediaTypeResolver().forUnknown();
                broker.storeDocument(transaction, thingUri.lastSegment(), new StringInputSource("binary-file".getBytes(UTF_8)), binMediaType, testCollection);
                fail("Should not have been able to overwrite Collection with Binary Document");

            } catch (final EXistException e) {
                assertEquals(
                        "The collection '" + testCollectionUri.getRawCollectionPath() + "' already has a sub-collection named '" + thingUri.lastSegment().toString() + "', you cannot create a Document with the same name as an existing collection.",
                        e.getMessage()
                );
            }
        }
    }
}
