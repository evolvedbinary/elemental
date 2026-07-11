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

import org.exist.xquery.ExternalModule;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.modules.ModuleUtils;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osjava.sj.loader.JndiLoader;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Uses JNDI to provide a StubDataSourceFactory
 * which tracks whether .close is called
 * (implicitly) on a Connection after an XQuery
 * finishes executing.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ImplicitConnectionCloseIT {

    private static final String JNDI_DS_NAME = "com.fusiondb.xquery.modules.sql.MockDataSource";
    private static final String STUB_JDBC_URL = "jdbc:stub:test-1";
    private static final String STUB_JDBC_USER = "sa";
    private static final String STUB_JDBC_PASSWORD = "sa";

    @Rule
    public ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    private Context ctx = null;

    @Before
    public void setupJndiEnvironment() throws NamingException {
        final Properties properties = new Properties();
        properties.setProperty(JNDI_DS_NAME + ".type", StubDataSource.class.getName());
        properties.setProperty(JNDI_DS_NAME + ".javaxNamingSpiObjectFactory", StubDataSourceFactory.class.getName());
        properties.setProperty(JNDI_DS_NAME + ".url", STUB_JDBC_URL);
        properties.setProperty(JNDI_DS_NAME + ".user", STUB_JDBC_USER);
        properties.setProperty(JNDI_DS_NAME + ".password", STUB_JDBC_PASSWORD);
        properties.setProperty(JNDI_DS_NAME + ".description", "Stub JNDI DataSource");

        ctx = new InitialContext();
        final JndiLoader loader = new JndiLoader(ctx.getEnvironment());
        loader.load(properties, ctx);
    }

    @After
    public void teardownJndiEnvironment() throws NamingException {
        ctx.unbind(JNDI_DS_NAME);
        ctx.close();

        StubDataSourceFactory.CREATED_DATA_SOURCES.clear();
    }

    @Test
    public void getJndiConnectionIsAutomaticallyClosed() throws EXistException, XPathException, PermissionDeniedException, IOException {
        final String mainQuery =
                "import module namespace sql = \"http://exist-db.org/xquery/sql\";\n" +
                "sql:get-jndi-connection(\"" + JNDI_DS_NAME + "\", \"" + STUB_JDBC_USER + "\", \"" + STUB_JDBC_PASSWORD + "\")";

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

            // check the connections from our StubDataSource, they should all be closed
            final Deque<StubDataSource> createdDataSources = StubDataSourceFactory.CREATED_DATA_SOURCES;
            assertEquals(1, createdDataSources.size());
            final StubDataSource stubDataSource = createdDataSources.peek();
            final Deque<StubConnection> createdConnections = stubDataSource.createdConnections;
            assertEquals(1, createdConnections.size());
            final StubConnection stubConnection = createdConnections.peek();
            assertTrue(stubConnection.isClosed());

            transaction.commit();
        }
    }

    @Test
    public void getJndiConnectionFromModuleIsAutomaticallyClosed() throws EXistException, XPathException, PermissionDeniedException, IOException, LockException, SAXException {
        final String moduleQuery =
                "module namespace mymodule = \"http://mymodule.com\";\n" +
                        "import module namespace sql = \"http://exist-db.org/xquery/sql\";\n" +
                        "declare function mymodule:get-handle() {\n" +
                        "    sql:get-jndi-connection(\"" + JNDI_DS_NAME + "\", \"" + STUB_JDBC_USER + "\", \"" + STUB_JDBC_PASSWORD + "\")\n" +
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

                final org.exist.xquery.Module[] libraryModules = mainModuleXqueryContext.getModules("http://mymodule.com");
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

            // check the connections from our StubDataSource, they should all be closed
            final Deque<StubDataSource> createdDataSources = StubDataSourceFactory.CREATED_DATA_SOURCES;
            assertEquals(1, createdDataSources.size());
            final StubDataSource stubDataSource = createdDataSources.peek();
            final Deque<StubConnection> createdConnections = stubDataSource.createdConnections;
            assertEquals(1, createdConnections.size());
            final StubConnection stubConnection = createdConnections.peek();
            assertTrue(stubConnection.isClosed());

            transaction.commit();
        }
    }

    public static class StubDataSourceFactory implements ObjectFactory {
        public static final Deque<StubDataSource> CREATED_DATA_SOURCES = new ConcurrentLinkedDeque<>();

        @Override
        public Object getObjectInstance(final Object obj, final Name name, final Context nameCtx, final Hashtable<?, ?> environment) throws Exception {
            if (obj instanceof Reference) {
                final Reference reference = (Reference)obj;
                if (reference.getClassName().equals(StubDataSource.class.getName())) {
                    final StubDataSource dataSource = new StubDataSource();
                    dataSource.setURL((String)reference.get("url").getContent());
                    dataSource.setUser((String)reference.get("user").getContent());
                    dataSource.setPassword((String)reference.get("password").getContent());
                    dataSource.setDescription((String)reference.get("description").getContent());

                    CREATED_DATA_SOURCES.push(dataSource);

                    return dataSource;
                }
            }
            return null;
        }
    }

    public static class StubDataSource implements DataSource {
        public final Deque<StubConnection> createdConnections = new ArrayDeque<>();

        private String url;
        private String user;
        private String password;
        private String description;

        @Override
        public Connection getConnection() {
            final StubConnection conn = new StubConnection();
            createdConnections.push(conn);
            return conn;
        }

        @Override
        public Connection getConnection(final String username, final String password) {
            final StubConnection conn = new StubConnection(username, password);
            createdConnections.push(conn);
            return conn;
        }

        @Override
        public <T> T unwrap(final Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(final Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(final PrintWriter out) {

        }

        @Override
        public void setLoginTimeout(final int seconds) {

        }

        @Override
        public int getLoginTimeout()  {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return null;
        }

        public void setURL(final String url) {
            this.url = url;
        }

        public void setUser(final String user) {
            this.user = user;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public void setDescription(final String description) {
            this.description = description;
        }
    }

    public static class StubConnection implements Connection {
        private final String username;
        private final String password;

        public int closedCount = 0;

        public StubConnection() {
            this(null, null);
        }

        public StubConnection(final String username, final String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public Statement createStatement() {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql) {
            return null;
        }

        @Override
        public CallableStatement prepareCall(final String sql) {
            return null;
        }

        @Override
        public String nativeSQL(final String sql) {
            return null;
        }

        @Override
        public void setAutoCommit(final boolean autoCommit) {

        }

        @Override
        public boolean getAutoCommit() {
            return false;
        }

        @Override
        public void commit() {

        }

        @Override
        public void rollback() {

        }

        @Override
        public void close() {
            closedCount++;
        }

        @Override
        public boolean isClosed() {
            return closedCount > 0;
        }

        @Override
        public DatabaseMetaData getMetaData() {
            return null;
        }

        @Override
        public void setReadOnly(final boolean readOnly) {

        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public void setCatalog(final String catalog) {

        }

        @Override
        public String getCatalog() {
            return null;
        }

        @Override
        public void setTransactionIsolation(final int level) {

        }

        @Override
        public int getTransactionIsolation() {
            return Connection.TRANSACTION_NONE;
        }

        @Override
        public SQLWarning getWarnings() {
            return null;
        }

        @Override
        public void clearWarnings() {

        }

        @Override
        public Statement createStatement(final int resultSetType, final int resultSetConcurrency) {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) {
            return null;
        }

        @Override
        public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency) {
            return null;
        }

        @Override
        public Map<String, Class<?>> getTypeMap() {
            return null;
        }

        @Override
        public void setTypeMap(final Map<String, Class<?>> map) {

        }

        @Override
        public void setHoldability(final int holdability) {

        }

        @Override
        public int getHoldability() {
            return 0;
        }

        @Override
        public Savepoint setSavepoint() {
            return null;
        }

        @Override
        public Savepoint setSavepoint(final String name) {
            return null;
        }

        @Override
        public void rollback(final Savepoint savepoint) {

        }

        @Override
        public void releaseSavepoint(final Savepoint savepoint) {

        }

        @Override
        public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
            return null;
        }

        @Override
        public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final String[] columnNames) {
            return null;
        }

        @Override
        public Clob createClob() {
            return null;
        }

        @Override
        public Blob createBlob() {
            return null;
        }

        @Override
        public NClob createNClob() {
            return null;
        }

        @Override
        public SQLXML createSQLXML() {
            return null;
        }

        @Override
        public boolean isValid(final int timeout) {
            return false;
        }

        @Override
        public void setClientInfo(final String name, final String value) {

        }

        @Override
        public void setClientInfo(final Properties properties) {

        }

        @Override
        public String getClientInfo(final String name) {
            return null;
        }

        @Override
        public Properties getClientInfo() {
            return null;
        }

        @Override
        public Array createArrayOf(final String typeName, final Object[] elements) {
            return null;
        }

        @Override
        public Struct createStruct(final String typeName, final Object[] attributes) {
            return null;
        }

        @Override
        public void setSchema(final String schema) {

        }

        @Override
        public String getSchema() {
            return null;
        }

        @Override
        public void abort(final Executor executor) {

        }

        @Override
        public void setNetworkTimeout(final Executor executor, final int milliseconds) {

        }

        @Override
        public int getNetworkTimeout() {
            return 0;
        }

        @Override
        public <T> T unwrap(final Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(final Class<?> iface) {
            return false;
        }
    }
}
