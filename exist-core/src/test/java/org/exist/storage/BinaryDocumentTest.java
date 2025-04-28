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
package org.exist.storage;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BinaryDocumentTest {

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @Test
    public void removeCollection() throws PermissionDeniedException, IOException, SAXException, LockException, EXistException {
        final XmldbURI testCollectionUri = XmldbURI.create("/db/remove-collection-test");
        final XmldbURI thingUri = testCollectionUri.append("thing");

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            // create a collection
            final Collection thingCollection = broker.getOrCreateCollection(transaction, thingUri);
            broker.saveCollection(transaction, thingCollection);

            // add a binary document to the collection
            broker.storeDocument(transaction, XmldbURI.create("file1.bin"), new StringInputSource("binary-file1".getBytes(UTF_8)), MimeType.BINARY_TYPE, thingCollection);

            // remove the collection
            assertTrue(broker.removeCollection(transaction, thingCollection));

            // try and store a binary doc with the same name as the thing collection (should succeed)
            final Collection testCollection = broker.getCollection(testCollectionUri);
            broker.storeDocument(transaction, XmldbURI.create("thing"), new StringInputSource("binary-file2".getBytes(UTF_8)), MimeType.BINARY_TYPE, testCollection);
        }
    }

    @Test
    public void overwriteCollection() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final XmldbURI testCollectionUri = XmldbURI.create("/db/overwrite-collection-test");
        final XmldbURI thingUri = testCollectionUri.append("thing");

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            // create a collection
            final Collection thingCollection = broker.getOrCreateCollection(transaction, thingUri);
            broker.saveCollection(transaction, thingCollection);

            // attempt to create a binary document with the same uri as the thingCollection (should fail)
            final Collection testCollection = broker.getCollection(testCollectionUri);

            try {
                broker.storeDocument(transaction, thingUri.lastSegment(), new StringInputSource("binary-file".getBytes(UTF_8)), MimeType.BINARY_TYPE, testCollection);
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
