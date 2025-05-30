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
package org.exist.repo;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.ManagedCollectionLock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.InputStreamSupplierInputSource;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.Sequence;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Optional;

import static org.exist.collections.CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE;

public class PackageTriggerTest {

    static final String xarFile = "exist-expathrepo-trigger-test.xar"; // NOTE(AR) a copy of this should be present in `src/test/resources`.
    static final XmldbURI triggerTestCollection = XmldbURI.create("/db");
    static final XmldbURI xarUri = triggerTestCollection.append(xarFile);

    @ClassRule
    public static ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(false, true);

    @BeforeClass
    public static void setup() throws PermissionDeniedException, SAXException, EXistException, IOException, LockException, XPathException {

        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();

        // Create a collection to test the trigger in
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Collection collection = broker.getOrCreateCollection(transaction, triggerTestCollection);
            broker.saveCollection(transaction, collection);
            transaction.commit();
        }

        // Store XAR in database
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final ManagedCollectionLock collectionLock = brokerPool.getLockManager().acquireCollectionWriteLock(xarUri.removeLastSegment())) {
                final Collection collection = broker.getOrCreateCollection(transaction, xarUri.removeLastSegment());

                broker.storeDocument(transaction, xarUri.lastSegment(), new InputStreamSupplierInputSource(() -> PackageTriggerTest.class.getResourceAsStream("/" + xarFile)), MimeType.EXPATH_PKG_TYPE, collection);
                broker.saveCollection(transaction, collection);
            }

            transaction.commit();
        }

        // Install and deploy XAR
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()))) {
            final XQuery xquery = brokerPool.getXQueryService();
            final Sequence result = xquery.execute(broker, "repo:install-and-deploy-from-db('/db/" + xarFile + "')", null);
            Assert.assertEquals(1, result.getItemCount());
        }

        // Store collection.xconf in newly created collection under /db/system/config
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()))) {
            final XQuery xquery = brokerPool.getXQueryService();
            final Sequence result = xquery.execute(broker, "xmldb:create-collection('/db/system/config/db','trigger-test'), " +
                    "xmldb:store('/db/system/config/db/trigger-test', '" + DEFAULT_COLLECTION_CONFIG_FILE + "', " +
                    "<collection xmlns=\"http://exist-db.org/collection-config/1.0\"><triggers><trigger class=\"org.exist.repo.ExampleTrigger\"/></triggers></collection>)", null);
            Assert.assertEquals(2, result.getItemCount());
        }

    }


    @Test
    public void checkTriggerFires() throws EXistException, PermissionDeniedException, XPathException {

        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();

        // Create collection and store document to fire trigger
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()))) {
            final XQuery xquery = brokerPool.getXQueryService();
            final Sequence result = xquery.execute(broker, "xmldb:create-collection('/db','trigger-test')", null);
            Assert.assertEquals(1, result.getItemCount());
        }

        // Create collection and store document to fire trigger
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()))) {
            final XQuery xquery = brokerPool.getXQueryService();
            final Sequence result = xquery.execute(broker, "xmldb:store('/db/trigger-test', 'test.xml', <a>b</a>)", null);
            Assert.assertEquals(1, result.getItemCount());
        }

        // Verify two documents are now in collection
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()))) {
            final XQuery xquery = brokerPool.getXQueryService();
            final Sequence result = xquery.execute(broker, "xmldb:get-child-resources('/db/trigger-test')", null);
            Assert.assertEquals("After trigger execution two documents should be in the collection.", 2, result.getItemCount());
        }

    }
}