/*
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

import org.exist.EXistException;
import org.exist.collections.triggers.TriggerException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.util.DatabaseConfigurationException;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URISyntaxException;
import java.util.Optional;

public class OpenCollectionTest {

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, false);

    private static XmldbURI TEST_COLLECTION = XmldbURI.ROOT_COLLECTION_URI.append("testCollection");

    @BeforeAll
    static void init(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, TriggerException {
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            broker.getOrCreateCollection(transaction, TEST_COLLECTION);
            transaction.commit();

            try (final Collection col = broker.openCollection(TEST_COLLECTION, Lock.LockMode.READ_LOCK)) {
                assertNotNull(col);
            }
        }
    }

    /**
     * Test opening a collection using a full XmldbURI including scheme.
     */
    @Test
    void loadFullXmldbURI(final BrokerPool pool) throws PermissionDeniedException, IOException, EXistException, URISyntaxException, DatabaseConfigurationException {
        loadCollection(pool, XmldbURI.xmldbUriFor("xmldb:exist:///db/testCollection"));
    }

    @Test
    void loadRelativeXmldbURI(final BrokerPool pool) throws PermissionDeniedException, IOException, EXistException, URISyntaxException, DatabaseConfigurationException {
        loadCollection(pool, XmldbURI.xmldbUriFor("testCollection"));
    }

    private void loadCollection(final BrokerPool pool, final XmldbURI uri) throws DatabaseConfigurationException, IOException, EXistException, PermissionDeniedException {
        // Restart database, otherwise collection would be read from cache
        EMBEDDED_DATABASE.restart();

        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            try (final Collection col = broker.openCollection(uri, Lock.LockMode.READ_LOCK)) {
                assertNotNull(col);
            }
        }
    }
}
