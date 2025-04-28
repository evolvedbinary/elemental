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
package org.exist.collections;

import org.exist.EXistException;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Optional;

import static org.exist.collections.CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE_URI;
import static org.junit.Assert.assertNotNull;

public class InitCollectionConfigurationTest {

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    /**
     * Ensure that etc/collection.xconf.init was deployed at startup
     */
    @Test
    public void deployedInitCollectionConfig() throws EXistException, PermissionDeniedException, LockException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            try (final Collection collection = broker.openCollection(XmldbURI.CONFIG_COLLECTION_URI.append("db"), Lock.LockMode.READ_LOCK)) {
                final LockedDocument confDoc = collection.getDocumentWithLock(broker, DEFAULT_COLLECTION_CONFIG_FILE_URI, Lock.LockMode.READ_LOCK);

                // asymmetrical - release collection lock
                collection.close();

                assertNotNull(confDoc);
            }
        }
    }
}
