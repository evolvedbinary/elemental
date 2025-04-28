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
package org.exist.xquery.modules.sql;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.EXistException;
import org.exist.dom.memtree.ElementImpl;
import org.exist.security.PermissionDeniedException;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Element;

import java.io.IOException;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.xquery.modules.sql.Util.executeQuery;
import static org.exist.xquery.modules.sql.Util.withCompiledQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * SQL Execute Integration Tests.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ExecuteIT {

    @Rule
    public final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @Rule
    public final H2DatabaseResource h2Database = new H2DatabaseResource();

    @Test
    public void executeResultsInSqlNS() throws EXistException, XPathException, PermissionDeniedException, IOException {
        executeForNS(SQLModule.NAMESPACE_URI, SQLModule.PREFIX);
    }

    @Test
    public void executeResultsInCustomNS() throws EXistException, XPathException, PermissionDeniedException, IOException {
        executeForNS("http://custom/ns", "custom");
    }

    private void executeForNS(final String namespace, final String prefix) throws EXistException, XPathException, PermissionDeniedException, IOException {
        final String mainQuery =
            "import module namespace sql = \"http://exist-db.org/xquery/sql\";\n" +
                "let $conn := sql:get-connection(\"" + h2Database.getDriverClass().getName() + "\", \"" + h2Database.getUrl() + "\", \"" + h2Database.getUser() + "\", \"" + h2Database.getPassword() + "\")\n" +
                "return\n" +
                "    sql:execute($conn, \"SELECT 'Hello World' FROM DUAL;\", true(), \"" + namespace + "\", \"" + prefix + "\")";

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final Source mainQuerySource = new StringSource(mainQuery);
        try (final DBBroker broker = pool.getBroker();
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            final Tuple2<String, String> namespaceAndPrefix = withCompiledQuery(broker, mainQuerySource, mainCompiledQuery -> {
                final XQueryContext mainQueryContext = mainCompiledQuery.getContext();

                // execute the query
                final Sequence result = executeQuery(broker, mainCompiledQuery);

                // check that the namespace of the result element is in the 'sql' namespace
                assertEquals(1, result.getItemCount());
                assertTrue(result.itemAt(0) instanceof Element);
                assertEquals(Type.ELEMENT, result.itemAt(0).getType());
                final Element element = (ElementImpl) result.itemAt(0);

                return Tuple(element.getNamespaceURI(), element.getPrefix());
            });

            assertEquals(namespace, namespaceAndPrefix._1);
            assertEquals(prefix, namespaceAndPrefix._2);

            transaction.commit();
        }
    }
}
