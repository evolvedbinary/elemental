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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.exist.xquery.*;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.exist.xquery.modules.ModuleUtils;
import org.exist.xquery.modules.ModuleUtils.ContextMapEntryModifier;

import javax.annotation.Nullable;

import static org.exist.xquery.FunctionDSL.functionDefs;

/**
 * SQL Module Extension for XQuery.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class SQLModule extends AbstractInternalModule {

    protected final static Logger LOG = LogManager.getLogger(SQLModule.class);
    public final static String NAMESPACE_URI = "http://exist-db.org/xquery/sql";
    public final static String PREFIX = "sql";
    public final static String INCLUSION_DATE = "2006-09-25";
    public final static String RELEASED_IN_VERSION = "eXist-1.2";

    public static final FunctionDef[] functions = functionDefs(
            functionDefs(GetConnectionFunction.class, GetConnectionFunction.FS_GET_CONNECTION),
            functionDefs(GetConnectionFunction.class, GetConnectionFunction.FS_GET_CONNECTION_FROM_POOL),
            functionDefs(CloseConnectionFunction.class, CloseConnectionFunction.FS_CLOSE_CONNECTION),
            functionDefs(GetJNDIConnectionFunction.class, GetJNDIConnectionFunction.signatures),
            functionDefs(ExecuteFunction.class, ExecuteFunction.FS_EXECUTE),
            functionDefs(PrepareFunction.class, PrepareFunction.signatures)
    );

    public final static String CONNECTIONS_CONTEXTVAR = "_eXist_sql_connections";
    public final static String PREPARED_STATEMENTS_CONTEXTVAR = "_eXist_sql_prepared_statements";

    private static final Map<String, HikariDataSource> CONNECTION_POOLS = new ConcurrentHashMap<>();
    private static final Pattern POOL_NAME_PATTERN = Pattern.compile("(pool\\.[0-9]+)\\.name");

    public SQLModule(final Map<String, List<?>> parameters) {
        super(functions, parameters);

        // create any connection pools that are not yet created
        Matcher poolNameMatcher = null;
        for (final Map.Entry<String, List<?>> parameter : parameters.entrySet()) {
            if (poolNameMatcher == null) {
                poolNameMatcher = POOL_NAME_PATTERN.matcher(parameter.getKey());
            }  else {
                 poolNameMatcher.reset(parameter.getKey());
            }

            if (poolNameMatcher.matches()) {
                if (parameter.getValue() != null && parameter.getValue().size() == 1) {
                    final String poolId = poolNameMatcher.group(1);
                    final String poolName = parameter.getValue().get(0).toString();
                    if (poolName != null && !poolName.isEmpty()) {
                        if (!CONNECTION_POOLS.containsKey(poolName)) {

                            final Properties poolProperties = new Properties();;
                            poolProperties.setProperty("poolName", poolName);

                            final String poolPropertiesPrefix = poolId + ".properties.";
                            for (final Map.Entry<String, List<?>> poolParameter : parameters.entrySet()) {
                                if (poolParameter.getKey().startsWith(poolPropertiesPrefix)) {
                                    if (poolParameter.getValue() != null && poolParameter.getValue().size() == 1) {
                                        final String propertyName = poolParameter.getKey().replace(poolPropertiesPrefix, "");
                                        final String propertyValue = poolParameter.getValue().get(0).toString();
                                        poolProperties.setProperty(propertyName, propertyValue);
                                    }
                                }
                            }

                            final HikariConfig hikariConfig = new HikariConfig(poolProperties);
                            final HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
                            CONNECTION_POOLS.put(poolName, hikariDataSource);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getNamespaceURI() {
        return NAMESPACE_URI;
    }

    @Override
    public String getDefaultPrefix() {
        return PREFIX;
    }

    @Override
    public String getDescription() {
        return "A module for performing SQL queries against Databases, returning XML representations of the result sets.";
    }

    @Override
    public String getReleaseVersion() {
        return RELEASED_IN_VERSION;
    }

    /**
     * Gets a Connection Pool.
     *
     * @param poolName the name of the connection pool.
     *
     * @return the connection pool, or null if there is no such pool
     */
    static @Nullable HikariDataSource getPool(final String poolName) {
        return CONNECTION_POOLS.get(poolName);
    }

    /**
     * Retrieves a previously stored Connection from the Context of an XQuery.
     *
     * @param context The Context of the XQuery containing the Connection
     * @param connectionUID The UID of the Connection to retrieve from the Context of the XQuery
     * @return the database connection for the UID, or null if there is no such connection.
     */
    public static @Nullable Connection retrieveConnection(final XQueryContext context, final long connectionUID) {
        return ModuleUtils.retrieveObjectFromContextMap(context, SQLModule.CONNECTIONS_CONTEXTVAR, connectionUID);
    }

    /**
     * Stores a Connection in the Context of an XQuery.
     *
     * @param context The Context of the XQuery to store the Connection in
     * @param con The connection to store
     * @return A unique ID representing the connection
     */
    public static long storeConnection(final XQueryContext context, final Connection con) {
        return ModuleUtils.storeObjectInContextMap(context, SQLModule.CONNECTIONS_CONTEXTVAR, con);
    }

    /**
     * Removes a Connection from the Context of an XQuery.
     *
     * @param context The Context of the XQuery to remove the Connection from
     * @param connectionUID The UID of the Connection to remove from the Context of the XQuery
     * @return the database connection for the UID, or null if there is no such connection.
     */
    public static @Nullable Connection removeConnection(final XQueryContext context, final long connectionUID) {
        return ModuleUtils.removeObjectFromContextMap(context, SQLModule.CONNECTIONS_CONTEXTVAR, connectionUID);
    }

    /**
     * Retrieves a previously stored PreparedStatement from the Context of an XQuery.
     *
     * @param context The Context of the XQuery containing the PreparedStatement
     * @param preparedStatementUID The UID of the PreparedStatement to retrieve from the Context of the XQuery
     * @return the prepared statement for the UID, or null if there is no such prepared statement.
     */
    public static PreparedStatementWithSQL retrievePreparedStatement(final XQueryContext context, final long preparedStatementUID) {
        return ModuleUtils.retrieveObjectFromContextMap(context, SQLModule.PREPARED_STATEMENTS_CONTEXTVAR, preparedStatementUID);
    }

    /**
     * Stores a PreparedStatement in the Context of an XQuery.
     *
     * @param context The Context of the XQuery to store the PreparedStatement in
     * @param stmt preparedStatement The PreparedStatement to store
     * @return A unique ID representing the PreparedStatement
     */
    public static long storePreparedStatement(final XQueryContext context, final PreparedStatementWithSQL stmt) {
        return ModuleUtils.storeObjectInContextMap(context, SQLModule.PREPARED_STATEMENTS_CONTEXTVAR, stmt);
    }

    /**
     * Resets the Module Context and closes any DB connections for the XQueryContext.
     *
     * @param xqueryContext The XQueryContext
     */
    @Override
    public void reset(final XQueryContext xqueryContext, final boolean keepGlobals) {
        // reset the module context
        super.reset(xqueryContext, keepGlobals);

        // close any open PreparedStatements
        closeAllPreparedStatements(xqueryContext);

        // close any open Connections
        closeAllConnections(xqueryContext);
    }

    /**
     * Closes all the open DB Connections for the specified XQueryContext.
     *
     * @param xqueryContext The context to close JDBC Connections for
     */
    private static void closeAllConnections(final XQueryContext xqueryContext) {
        ModuleUtils.modifyContextMap(xqueryContext, SQLModule.CONNECTIONS_CONTEXTVAR, new ContextMapEntryModifier<Connection>() {

            @Override
            public void modifyWithoutResult(final Map<Long, Connection> map) {
                super.modifyWithoutResult(map);

                // empty the map
                map.clear();
            }

            @Override
            public void modifyEntry(final Entry<Long, Connection> entry) {
                final Connection con = entry.getValue();
                try {
                    // close the Connection
                    con.close();
                } catch (final SQLException se) {
                    LOG.warn("Unable to close JDBC Connection: {}", se.getMessage(), se);
                }
            }
        });
    }

    /**
     * Closes all the open DB PreparedStatements for the specified XQueryContext.
     *
     * @param xqueryContext The context to close JDBC PreparedStatements for
     */
    private static void closeAllPreparedStatements(final XQueryContext xqueryContext) {
        ModuleUtils.modifyContextMap(xqueryContext, SQLModule.PREPARED_STATEMENTS_CONTEXTVAR, new ContextMapEntryModifier<PreparedStatementWithSQL>() {

            @Override
            public void modifyWithoutResult(final Map<Long, PreparedStatementWithSQL> map) {
                super.modifyWithoutResult(map);

                // empty the map
                map.clear();
            }

            @Override
            public void modifyEntry(final Entry<Long, PreparedStatementWithSQL> entry) {
                final PreparedStatementWithSQL stmt = entry.getValue();
                try {
                    // close the PreparedStatement
                    stmt.getStmt().close();
                } catch (SQLException se) {
                    LOG.warn("Unable to close JDBC PreparedStatement: {}", se.getMessage(), se);
                }
            }
        });
    }
}
