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

import static org.junit.jupiter.api.Assertions.*;
import static org.exist.samples.Samples.SAMPLES;

import java.io.IOException;
import java.util.Optional;

import org.exist.EXistException;
import org.exist.collections.*;
import org.exist.collections.triggers.TriggerException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.txn.*;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.util.InputStreamSupplierInputSource;
import org.exist.util.LockException;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

public class RemoveRootCollectionTest {

    private DBBroker broker;
    Collection root;

    @Test
    void removeEmptyRootCollection() throws EXistException, PermissionDeniedException, IOException, TriggerException {
        final BrokerPool pool = BrokerPool.getInstance();
        final TransactionManager transact = pool.getTransactionManager();
        try (final Txn transaction = transact.beginTransaction()) {
            broker.removeCollection(transaction, root);
            transact.commit(transaction);
        }
        assertEquals(0, root.getChildCollectionCount(broker));
        assertEquals(0, root.getDocumentCount(broker));
    }

    @Test
    void removeRootCollectionWithChildCollection() throws PermissionDeniedException, EXistException, IOException, TriggerException {
        addChildToRoot();
        final BrokerPool pool = BrokerPool.getInstance();
        final TransactionManager transact = pool.getTransactionManager();
        try (final Txn transaction = transact.beginTransaction()) {
            broker.removeCollection(transaction, root);
            transact.commit(transaction);
        }
        assertEquals(0, root.getChildCollectionCount(broker));
        assertEquals(0, root.getDocumentCount(broker));
    }

    @Disabled
    @Test
    void removeRootCollectionWithDocument() throws LockException, PermissionDeniedException, EXistException, IOException, SAXException {
        addDocumentToRoot();
        final BrokerPool pool = BrokerPool.getInstance();
        final TransactionManager transact = pool.getTransactionManager();
        try (final Txn transaction = transact.beginTransaction()) {
            broker.removeCollection(transaction, root);
            transact.commit(transaction);
        }
        assertEquals(0, root.getChildCollectionCount(broker));
        assertEquals(0, root.getDocumentCount(broker));
    }

    @RegisterExtension
    public final EmbeddedDatabaseExtension embeddedDatabase = new EmbeddedDatabaseExtension(true, true);

    @BeforeEach
    void startDB() throws EXistException, PermissionDeniedException {
        final BrokerPool pool = BrokerPool.getInstance();
        broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
        root = broker.getCollection(XmldbURI.ROOT_COLLECTION_URI);
    }

    @AfterEach
    void stopDB() {
        if (broker != null) {
            broker.close();
        }
    }

    private void addDocumentToRoot() throws EXistException, LockException, PermissionDeniedException, IOException, SAXException {
        final BrokerPool pool = BrokerPool.getInstance();
        final TransactionManager transact = pool.getTransactionManager();
        try (final Txn transaction = transact.beginTransaction()) {
            final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
            broker.storeDocument(transaction, XmldbURI.create("hamlet.xml"), new InputStreamSupplierInputSource(SAMPLES::getHamletSample), xmlMediaType, root);
            transact.commit(transaction);
        }
    }

    private void addChildToRoot() throws EXistException, PermissionDeniedException, IOException, TriggerException {
        final BrokerPool pool = BrokerPool.getInstance();
        final TransactionManager transact = pool.getTransactionManager();
        try (final Txn transaction = transact.beginTransaction()) {
            final Collection child = broker.getOrCreateCollection(transaction, XmldbURI.ROOT_COLLECTION_URI.append("child"));
            broker.saveCollection(transaction, child);
            transact.commit(transaction);
        }
    }
}
