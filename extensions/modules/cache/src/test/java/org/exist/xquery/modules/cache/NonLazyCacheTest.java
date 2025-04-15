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
package org.exist.xquery.modules.cache;

import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.Configuration;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class NonLazyCacheTest {

    private static Path getLazyConfig() {
        try {
            return Paths.get(NonLazyCacheTest.class.getResource("/non-lazy-cache-conf.xml").toURI());
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Unable to find: non-lazy-cache-conf.xml");
        }
    }

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(null, getLazyConfig(), null, true, true);

    @SuppressWarnings("unchecked")
    @Test
    public void putOnNonLazilyCreatedCacheWithoutExplicitCreation() throws PermissionDeniedException, EXistException {
        // 1. check that the CacheModule was configured from the conf file correctly
        final Configuration configuration = existEmbeddedServer.getBrokerPool().getConfiguration();
        final Map<String, Map<String, List<Object>>> modulesParameters = (Map<String, Map<String, List<Object>>>) configuration.getProperty(XQueryContext.PROPERTY_MODULE_PARAMETERS);
        final Map<String, List<Object>> moduleParameters = modulesParameters.get(CacheModule.NAMESPACE_URI);
        final List<Object> enableLazyCreation = moduleParameters.get(CacheModule.PARAM_NAME_ENABLE_LAZY_CREATION);
        assertEquals(1, enableLazyCreation.size());
        assertEquals("false", enableLazyCreation.get(0).toString());

        // 2. try and put on a cache that can't be lazing created (due to conf file setting)
        try {
            executeQuery("cache:put('non-lazy-foo', 'bar', 'baz1')");
            fail("Should not be able to lazily create a cache when lazy creation is disabled");
        } catch (final XPathException e) {
            final ErrorCodes.ErrorCode errorCode = e.getErrorCode();
            assertEquals("Expected lazy creation disabled error", CacheModule.LAZY_CREATION_DISABLED, errorCode);
        }
    }

    private static Sequence executeQuery(final String query) throws EXistException, PermissionDeniedException, XPathException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = brokerPool.getBroker();
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final Sequence result = brokerPool.getXQueryService().execute(broker, query, null);

            transaction.commit();

            return result;
        }
    }
}
