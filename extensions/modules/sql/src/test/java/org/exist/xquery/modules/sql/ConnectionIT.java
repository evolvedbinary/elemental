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
import org.exist.collections.Collection;
import org.exist.security.PermissionDeniedException;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.*;
import org.exist.xquery.modules.ModuleUtils;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.xquery.modules.sql.Util.executeQuery;
import static org.exist.xquery.modules.sql.Util.withCompiledQuery;
import static org.junit.Assert.*;

/**
 * SQL Connection Integration Tests.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ConnectionIT {

    @Rule
    public ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @Rule
    public H2DatabaseResource h2Database = new H2DatabaseResource();

    @Test
    public void getConnectionIsAutomaticallyClosed() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final String mainQuery =
                "import module namespace sql = \"http://exist-db.org/xquery/sql\";\n" +
                "sql:get-connection(\"" + h2Database.getDriverClass().getName() + "\", \"" + h2Database.getUrl() + "\", \"" + h2Database.getUser() + "\", \"" + h2Database.getPassword() + "\")";

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final Source mainQuerySource = new StringSource(mainQuery);
        try (final DBBroker broker = pool.getBroker();
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            final XQueryContext escapedMainQueryContext = withCompiledQuery(broker, mainQuerySource, mainCompiledQuery -> {
                final XQueryContext mainQueryContext = mainCompiledQuery.getContext();

                // execute the query
                final Sequence result = executeQuery(broker, mainCompiledQuery);

                // check that the handle for the sql connection that was created was valid
                assertEquals(1, result.getItemCount());
                assertTrue(result.itemAt(0) instanceof IntegerValue);
                assertEquals(Type.LONG, result.itemAt(0).getType());
                final long connectionHandle = result.itemAt(0).toJavaObject(long.class);
                assertFalse(connectionHandle == 0);

                // intentionally escape the context from the lambda
                return mainQueryContext;
            });

            // check the connections map is empty
            final int connectionsCount = ModuleUtils.readContextMap(escapedMainQueryContext, SQLModule.CONNECTIONS_CONTEXTVAR, Map::size);
            assertEquals(0, connectionsCount);

            transaction.commit();
        }
    }

    @Test
    public void getConnectionFromModuleIsAutomaticallyClosed() throws EXistException, XPathException, PermissionDeniedException, IOException, LockException, SAXException {
        final String moduleQuery =
                "module namespace mymodule = \"http://mymodule.com\";\n" +
                "import module namespace sql = \"http://exist-db.org/xquery/sql\";\n" +
                "declare function mymodule:get-handle() {\n" +
                "    sql:get-connection(\"" + h2Database.getDriverClass().getName() + "\", \"" + h2Database.getUrl() + "\", \"" + h2Database.getUser() + "\", \"" + h2Database.getPassword() + "\")\n" +
                "};\n";

        final String mainQuery =
                "import module namespace mymodule = \"http://mymodule.com\" at \"xmldb:exist:///db/mymodule.xqm\";\n" +
                "mymodule:get-handle()";
        final Source mainQuerySource = new StringSource(mainQuery);

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            // store module
            try (final Collection collection = broker.openCollection(XmldbURI.create("/db"), Lock.LockMode.WRITE_LOCK)) {
                broker.storeDocument(transaction, XmldbURI.create("mymodule.xqm"), new StringInputSource(moduleQuery.getBytes(UTF_8)), MimeType.XQUERY_TYPE, collection);
            }

            final Tuple2<XQueryContext, ModuleContext> escapedContexts = withCompiledQuery(broker, mainQuerySource, mainCompiledQuery -> {
                final XQueryContext mainQueryContext = mainCompiledQuery.getContext();

                // get the context of the library module
                final org.exist.xquery.Module[] libraryModules = mainQueryContext.getModules("http://mymodule.com");
                assertEquals(1, libraryModules.length);
                assertTrue(libraryModules[0] instanceof ExternalModule);
                final ExternalModule libraryModule = (ExternalModule) libraryModules[0];
                final XQueryContext libraryQueryContext = libraryModule.getContext();
                assertTrue(libraryQueryContext instanceof ModuleContext);

                // execute the query
                final Sequence result = executeQuery(broker, mainCompiledQuery);

                // check that the handle for the sql connection that was created was valid
                assertEquals(1, result.getItemCount());
                assertTrue(result.itemAt(0) instanceof IntegerValue);
                assertEquals(Type.LONG, result.itemAt(0).getType());
                final long connectionHandle = result.itemAt(0).toJavaObject(long.class);
                assertFalse(connectionHandle == 0);

                // intentionally escape the contexts from the lambda
                return Tuple(mainQueryContext, (ModuleContext) libraryQueryContext);
            });

            final XQueryContext escapedMainQueryContext = escapedContexts._1;
            final ModuleContext escapedLibraryQueryContext = escapedContexts._2;
            assertTrue(escapedMainQueryContext != escapedLibraryQueryContext);

            // check the connections were closed in the main module
            final int mainConnectionsCount = ModuleUtils.readContextMap(escapedMainQueryContext, SQLModule.CONNECTIONS_CONTEXTVAR, Map::size);
            assertEquals(0, mainConnectionsCount);

            // check the connections were closed in the library module
            final int libraryConnectionsCount = ModuleUtils.readContextMap(escapedLibraryQueryContext, SQLModule.CONNECTIONS_CONTEXTVAR, Map::size);
            assertEquals(0, libraryConnectionsCount);

            transaction.commit();
        }
    }

    @Test
    public void getConnectionCanBeExplicitlyClosed() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final String query =
                "import module namespace sql = \"http://exist-db.org/xquery/sql\";\n" +
                        "let $conn := sql:get-connection(\"" + h2Database.getDriverClass().getName() + "\", \"" + h2Database.getUrl() + "\", \"" + h2Database.getUser() + "\", \"" + h2Database.getPassword() + "\")\n" +
                        "return sql:close-connection($conn)";

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final Source source = new StringSource(query);
        try (final DBBroker broker = pool.getBroker();
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            // execute query
            final Tuple2<XQueryContext, Boolean> contextAndResult = withCompiledQuery(broker, source, compiledXQuery -> {
                final Sequence result = executeQuery(broker, compiledXQuery);
                return Tuple(compiledXQuery.getContext(), result.itemAt(0).toJavaObject(boolean.class));
            });

            // check that the handle for the sql connection was closed
            assertTrue(contextAndResult._2);

            // check the connections were closed
            final int connectionsCount = ModuleUtils.readContextMap(contextAndResult._1, SQLModule.CONNECTIONS_CONTEXTVAR, Map::size);
            assertEquals(0, connectionsCount);

            transaction.commit();
        }
    }
}
