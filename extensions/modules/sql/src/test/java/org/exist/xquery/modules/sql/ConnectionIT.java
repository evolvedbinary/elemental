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
import org.exist.collections.Collection;
import org.exist.security.PermissionDeniedException;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.Holder;
import org.exist.util.LockException;
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
import xyz.elemental.mediatype.MediaType;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
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

                // check there is an active connection
                final long connectionHandle = result.itemAt(0).toJavaObject(long.class);
                assertNotEquals(0, connectionHandle);
            }

            // now the query has finished executing and been reset, check the connections map is empty
            assertEquals(0, connectionsCountHolder.value.intValue());

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
            final MediaType xqueryMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XQUERY);
            try (final Collection collection = broker.openCollection(XmldbURI.create("/db"), Lock.LockMode.WRITE_LOCK)) {
                broker.storeDocument(transaction, XmldbURI.create("mymodule.xqm"), new StringInputSource(moduleQuery.getBytes(UTF_8)), xqueryMediaType, collection);
            }

            // will hold the number of open connections in the Main Module once the query has finished executing
            final Holder<Integer> mainModuleConnectionsCountHolder = new Holder<>();
            // will hold the number of open connections in the Library Module once the query has finished executing
            final Holder<Integer> libraryModuleConnectionsCountHolder = new Holder<>();
            final BiConsumerE<XQueryContext, XQueryUtil.QueryResult, XPathException> postExecutionContext = (mainModuleXqueryContext, result) -> {
                final int mainModuleConnectionsCount = ModuleUtils.readContextMap(mainModuleXqueryContext, SQLModule.CONNECTIONS_CONTEXTVAR, Map::size);
                mainModuleConnectionsCountHolder.value = mainModuleConnectionsCount;

                final Module[] libraryModules = mainModuleXqueryContext.getModules("http://mymodule.com");
                final ExternalModule libraryModule = (ExternalModule) libraryModules[0];
                final XQueryContext libraryModuleXqueryContext = libraryModule.getContext();
                final int libraryModuleConnectionsCount = ModuleUtils.readContextMap(libraryModuleXqueryContext, SQLModule.CONNECTIONS_CONTEXTVAR, Map::size);
                libraryModuleConnectionsCountHolder.value = libraryModuleConnectionsCount;
            };

            // execute query
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, mainQuerySource, false, null, null, null, null, postExecutionContext)) {

                // check that the handle for the sql connection that was created was valid
                final Sequence result = queryResult.result;
                assertEquals(1, result.getItemCount());
                assertTrue(result.itemAt(0) instanceof IntegerValue);
                assertEquals(Type.LONG, result.itemAt(0).getType());

                // check there is an active connection
                final long connectionHandle = result.itemAt(0).toJavaObject(long.class);
                assertNotEquals(0, connectionHandle);
            }

            // now the query has finished executing and been reset, check the connections map is empty for the Main Module
            assertEquals(0, mainModuleConnectionsCountHolder.value.intValue());

            // now the query has finished executing and been reset, check the connections map is empty for the Library Module
            assertEquals(0, libraryModuleConnectionsCountHolder.value.intValue());

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

            // will hold the number of open connections once the query has finished executing
            final Holder<Integer> connectionsCountHolder = new Holder<>();
            final BiConsumerE<XQueryContext, XQueryUtil.QueryResult, XPathException> postExecutionContext = (xqueryContext, result) -> {
                final int connectionsCount = ModuleUtils.readContextMap(xqueryContext, SQLModule.CONNECTIONS_CONTEXTVAR, Map::size);
                connectionsCountHolder.value = connectionsCount;
            };

            // execute query
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, source, false, null, null, null, null, postExecutionContext)) {
                // check that the handle for the sql connection was closed
                final Sequence result = queryResult.result;
                final boolean connectionIsClosed = result.itemAt(0).toJavaObject(boolean.class);
                assertTrue(connectionIsClosed);
            }

            // now the query has finished executing and been reset, check the connections map is empty
            assertEquals(0, connectionsCountHolder.value.intValue());

            transaction.commit();
        }
    }
}
