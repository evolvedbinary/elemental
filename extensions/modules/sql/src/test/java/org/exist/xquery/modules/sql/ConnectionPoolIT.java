/*
 * Copyright (C) 2014, Evolved Binary Ltd
 *
 * This file was originally ported from FusionDB to eXist-db by
 * Evolved Binary, for the benefit of the eXist-db Open Source community.
 * Only the ported code as it appears in this file, at the time that
 * it was contributed to eXist-db, was re-licensed under The GNU
 * Lesser General Public License v2.1 only for use in eXist-db.
 *
 * This license grant applies only to a snapshot of the code as it
 * appeared when ported, it does not offer or infer any rights to either
 * updates of this source code or access to the original source code.
 *
 * The GNU Lesser General Public License v2.1 only license follows.
 *
 * =====================================================================
 *
 * Copyright (C) 2014, Evolved Binary Ltd
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
 */
package org.exist.xquery.modules.sql;

import com.evolvedbinary.j8fu.function.BiConsumerE;
import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.Holder;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.modules.ModuleUtils;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

public class ConnectionPoolIT {

    @Rule
    public ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @Test
    public void getConnectionFromPoolIsAutomaticallyClosed() throws EXistException, XPathException, PermissionDeniedException, IOException {
        // NOTE: pool-1 is configured in src/test/resources-filtered/conf.xml
        final String mainQuery =
                "import module namespace sql = \"http://exist-db.org/xquery/sql\";\n" +
                        "sql:get-connection-from-pool(\"pool-1\")";
        final Source mainQuerySource = new StringSource(mainQuery);

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.getBroker();
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            // will hold the number of open connections once the query has finished executing
            final Holder<Integer> connectionsCountHolder = new Holder<>();
            final BiConsumerE<XQueryContext, XQueryUtil.QueryResult, XPathException> postExecutionContext = (xqueryContext, result) -> {
                final int connectionsCount = ModuleUtils.readContextMap(xqueryContext, SQLModule.CONNECTIONS_CONTEXTVAR, Map::size);
                connectionsCountHolder.value = connectionsCount;
            };

            // execute query
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, mainQuerySource, false, null, null, null, null, postExecutionContext)) {
                // check that the handle for the sql connection that was created was valid
                final Sequence result = queryResult.result;
                assertEquals(1, result.getItemCount());
                assertTrue(result.itemAt(0) instanceof IntegerValue);
                assertEquals(Type.LONG, result.itemAt(0).getType());
                final long connectionHandle = result.itemAt(0).toJavaObject(long.class);
                assertNotEquals(0, connectionHandle);
            }

            // now the query has finished executing and been reset, check the connections map is empty
            assertEquals(0, connectionsCountHolder.value.intValue());

            transaction.commit();
        }
    }
}
