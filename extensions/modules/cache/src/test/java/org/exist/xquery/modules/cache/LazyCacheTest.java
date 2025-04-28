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
package org.exist.xquery.modules.cache;

import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.Configuration;
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

public class LazyCacheTest {

    private static Path getLazyConfig() {
        try {
            return Paths.get(LazyCacheTest.class.getResource("/lazy-cache-conf.xml").toURI());
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Unable to find: lazy-cache-conf.xml");
        }
    }

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(null, getLazyConfig(), null, true, true);

    @SuppressWarnings("unchecked")
    @Test
    public void putOnLazilyCreatedCache() throws XPathException, PermissionDeniedException, EXistException {
        // 1. check that the CacheModule was configured from the conf file correctly
        final Configuration configuration = existEmbeddedServer.getBrokerPool().getConfiguration();
        final Map<String, Map<String, List<Object>>> modulesParameters = (Map<String, Map<String, List<Object>>>) configuration.getProperty(XQueryContext.PROPERTY_MODULE_PARAMETERS);
        final Map<String, List<Object>> moduleParameters = modulesParameters.get(CacheModule.NAMESPACE_URI);
        final List<Object> enableLazyCreation = moduleParameters.get(CacheModule.PARAM_NAME_ENABLE_LAZY_CREATION);
        assertEquals(1, enableLazyCreation.size());
        assertEquals("true", enableLazyCreation.get(0).toString());

        // 2. try and put on a cache that can be lazing created (due to conf file setting)
        Sequence result = executeQuery("cache:put('lazy-foo', 'bar', 'baz1')");
        assertNotNull(result);
        assertTrue(result.isEmpty());

        result = executeQuery("cache:put('lazy-foo', 'bar', 'baz2')");
        assertNotNull(result);
        assertEquals(1, result.getItemCount());
        assertEquals("baz1", result.itemAt(0).getStringValue());
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
