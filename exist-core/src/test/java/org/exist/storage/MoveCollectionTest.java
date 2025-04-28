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

import java.io.IOException;
import java.util.Optional;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.xmldb.XmldbURI;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class MoveCollectionTest {

    @Rule
    public ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    /**
     * Test move collection /db/a/b/c/d/e/f/g/h/i/j/k to /db/z/y/x/w/v/u/k
     */
    @Test
    public void moveDeep() throws EXistException, IOException, PermissionDeniedException, TriggerException, LockException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = transact.beginTransaction()) {

            final XmldbURI srcUri = XmldbURI.create("/db/a/b/c/d/e/f/g/h/i/j/k");
            final XmldbURI destUri = XmldbURI.create("/db/z/y/x/w/v/u");

            try (final Collection src = broker.getOrCreateCollection(transaction, srcUri)) {
                assertNotNull(src);
                broker.saveCollection(transaction, src);
            }

            try (final Collection dst = broker.getOrCreateCollection(transaction, destUri)) {
                assertNotNull(dst);
                broker.saveCollection(transaction, dst);
            }

            try (final Collection src = broker.openCollection(srcUri, Lock.LockMode.WRITE_LOCK);
                 final Collection dst = broker.openCollection(destUri, Lock.LockMode.WRITE_LOCK)) {

                broker.moveCollection(transaction, src, dst, src.getURI().lastSegment());
            }

            transact.commit(transaction);
        }
    }

    /**
     * Test move collection /db/a/b/c/d/e/f/g/h/i/j/k to /db/z/y/x/w/v/u/k
     *
     * Note that the collection /db/a/b/c/d/e/f/g/h/i/j/k has the sub-collections (sub-1 and sub-2),
     * this test checks that the sub-collections are correctly preserved.
     */
    @Test
    public void moveDeepWithSubCollections() throws EXistException, IOException, PermissionDeniedException, TriggerException, LockException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = transact.beginTransaction()) {

            final XmldbURI srcUri = XmldbURI.create("/db/a/b/c/d/e/f/g/h/i/j/k");
            final XmldbURI srcSubCol1Uri = srcUri.append("sub-1");
            final XmldbURI srcSubCol2Uri = srcUri.append("sub-2");
            final XmldbURI destUri = XmldbURI.create("/db/z/y/x/w/v/u");

            // create src collection
            try (final Collection src = broker.getOrCreateCollection(transaction, srcUri)) {
                assertNotNull(src);
                broker.saveCollection(transaction, src);
            }

            // create src sub-collections
            try (final Collection srcColSubCol1 = broker.getOrCreateCollection(transaction, srcSubCol1Uri)) {
                assertNotNull(srcColSubCol1);
                broker.saveCollection(transaction, srcColSubCol1);
            }
            try (final Collection srcColSubCol2 = broker.getOrCreateCollection(transaction, srcSubCol2Uri)) {
                assertNotNull(srcColSubCol2);
                broker.saveCollection(transaction, srcColSubCol2);
            }

            // create dst collection
            try (final Collection dst = broker.getOrCreateCollection(transaction, destUri)) {
                assertNotNull(dst);
                broker.saveCollection(transaction, dst);
            }

            try (final Collection src = broker.openCollection(srcUri, Lock.LockMode.WRITE_LOCK);
                 final Collection dst = broker.openCollection(destUri, Lock.LockMode.WRITE_LOCK)) {

                broker.moveCollection(transaction, src, dst, src.getURI().lastSegment());
            }

            transact.commit(transaction);
        }
    }

    /**
     * Test rename collection /db/move-collection-test-rename/before to /db/move-collection-test-rename/after
     */
    @Test
    public void rename() throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = transact.beginTransaction()) {

            final XmldbURI testColUri = XmldbURI.create("/db/move-collection-test-rename");
            final XmldbURI srcColUri = testColUri.append("before");
            final XmldbURI newName = XmldbURI.create("after");

            try (final Collection testCol = broker.getOrCreateCollection(transaction, testColUri)) {
                assertNotNull(testCol);
                broker.saveCollection(transaction, testCol);
            }

            try (final Collection srcCol = broker.getOrCreateCollection(transaction, srcColUri)) {
                assertNotNull(srcCol);
                broker.saveCollection(transaction, srcCol);
            }

            try (final Collection src = broker.openCollection(srcColUri, Lock.LockMode.WRITE_LOCK);
                 final Collection testCol = broker.openCollection(testColUri, Lock.LockMode.WRITE_LOCK)) {

                broker.moveCollection(transaction, src, testCol, newName);

                assertFalse(testCol.hasChildCollection(broker, srcColUri.lastSegment()));
                assertTrue(testCol.hasChildCollection(broker, newName));
            }

            transact.commit(transaction);
        }
    }

    /**
     * Test rename collection /db/move-collection-test-rename/before to /db/move-collection-test-rename/after
     *
     * Note that the collection /db/move-collection-test-rename/before has the sub-collections (sub-1 and sub-2),
     * this test checks that the sub-collections are correctly preserved.
     */
    @Test
    public void renameWithSubCollections() throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = transact.beginTransaction()) {

            final XmldbURI testColUri = XmldbURI.create("/db/move-collection-test-rename");
            final XmldbURI srcColUri = testColUri.append("before");
            final XmldbURI srcColSubCol1Uri = srcColUri.append("sub-1");
            final XmldbURI srcColSubCol2Uri = srcColUri.append("sub-2");
            final XmldbURI newName = XmldbURI.create("after");

            // create test collection
            try (final Collection testCol = broker.getOrCreateCollection(transaction, testColUri)) {
                assertNotNull(testCol);
                broker.saveCollection(transaction, testCol);
            }

            // create src collection
            try (final Collection srcCol = broker.getOrCreateCollection(transaction, srcColUri)) {
                assertNotNull(srcCol);
                broker.saveCollection(transaction, srcCol);
            }

            // create src sub-collections
            try (final Collection srcColSubCol1 = broker.getOrCreateCollection(transaction, srcColSubCol1Uri)) {
                assertNotNull(srcColSubCol1);
                broker.saveCollection(transaction, srcColSubCol1);
            }
            try (final Collection srcColSubCol2 = broker.getOrCreateCollection(transaction, srcColSubCol2Uri)) {
                assertNotNull(srcColSubCol2);
                broker.saveCollection(transaction, srcColSubCol2);
            }

            try (final Collection src = broker.openCollection(srcColUri, Lock.LockMode.WRITE_LOCK);
                 final Collection testCol = broker.openCollection(testColUri, Lock.LockMode.WRITE_LOCK)) {

                broker.moveCollection(transaction, src, testCol, newName);

                assertFalse(testCol.hasChildCollection(broker, srcColUri.lastSegment()));
                assertTrue(testCol.hasChildCollection(broker, newName));
            }

            transact.commit(transaction);
        }
    }
}
