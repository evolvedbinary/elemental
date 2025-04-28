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

import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.exist.dom.QName;
import org.exist.util.ParametersExtractor;
import org.exist.xquery.*;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.Properties;

import static org.exist.xquery.FunctionDSL.*;
import static org.exist.xquery.modules.sql.SQLModule.NAMESPACE_URI;
import static org.exist.xquery.modules.sql.SQLModule.PREFIX;


/**
 * SQL Module Extension function for XQuery to retrieve a connection.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class GetConnectionFunction extends BasicFunction {
    private static final FunctionReturnSequenceType RETURN_TYPE = returnsOpt(Type.LONG, "an xs:long representing the connection handle. The connection will be closed (or returned to the pool) automatically when the calling XQuery finishes execution, if you need to return it sooner you can call sql:close-connection#1");
    private static final FunctionParameterSequenceType JDBC_PASSWORD_PARAM = param("password", Type.STRING, "The SQL database password");
    private static final FunctionParameterSequenceType JDBC_USERNAME_PARAM = param("username", Type.STRING, "The SQL database username");
    private static final FunctionParameterSequenceType JDBC_PROPERTIES_PARAM = optParam("properties", Type.ELEMENT, "The JDBC database connection properties in the form <properties><property name=\"\" value=\"\"/></properties>.");
    private static final FunctionParameterSequenceType JDBC_URL_PARAM = param("url", Type.STRING, "The JDBC connection URL");
    private static final FunctionParameterSequenceType JDBC_DRIVER_CLASSNAME_PARAM = param("driver-classname", Type.STRING, "The JDBC driver classname");
    private static final FunctionParameterSequenceType CONNECTION_POOL_PARAM = param("pool-name", Type.STRING, "The connection pool name (as configured in conf.xml)");

    private static final Logger LOGGER = LogManager.getLogger(GetConnectionFunction.class);

    private static final String FN_GET_CONNECTION = "get-connection";
    public static final FunctionSignature[] FS_GET_CONNECTION = functionSignatures(
        FN_GET_CONNECTION,
        "Opens a connection to a SQL Database",
        RETURN_TYPE,
        arities(
                arity(JDBC_DRIVER_CLASSNAME_PARAM, JDBC_URL_PARAM),
                arity(JDBC_DRIVER_CLASSNAME_PARAM, JDBC_URL_PARAM, JDBC_PROPERTIES_PARAM),
                arity(JDBC_DRIVER_CLASSNAME_PARAM, JDBC_URL_PARAM, JDBC_USERNAME_PARAM, JDBC_PASSWORD_PARAM)
        )
    );

    private static final String FN_GET_CONNECTION_FROM_POOL = "get-connection-from-pool";
    public static final FunctionSignature[] FS_GET_CONNECTION_FROM_POOL = functionSignatures(
            FN_GET_CONNECTION_FROM_POOL,
            "Retrieves a connection to a SQL Database from a connection pool",
            RETURN_TYPE,
            arities(
                    arity(CONNECTION_POOL_PARAM),
                    arity(CONNECTION_POOL_PARAM, JDBC_USERNAME_PARAM, JDBC_PASSWORD_PARAM)
            )
    );

    public GetConnectionFunction(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    /**
     * Evaluate the call to the xquery get-connection() or get-connection-from-pool() functions,
     * it is really the main entry point of this class.
     *
     * @param args arguments from the get-connection() function call
     * @param contextSequence the Context Sequence to operate on (not used here internally!)
     *
     * @return A xs:long representing a handle to the connection
     *
     * @throws XPathException if an error occurs.
     */
    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final Connection connection;
        if (isCalledAs(FN_GET_CONNECTION)) {
            // was a db driver and url specified?
            if (args[0].isEmpty() || args[1].isEmpty()) {
                return Sequence.EMPTY_SEQUENCE;
            }
            connection = getConnection(args);

        } else if (isCalledAs(FN_GET_CONNECTION_FROM_POOL)) {
            connection = getConnectionFromPool(args);

        } else {
            throw new XPathException(this, "No function: " + getName() + "#" + getSignature().getArgumentCount());
        }

        // store the Connection and return the uid handle of the Connection
        return new IntegerValue(this, SQLModule.storeConnection(context, connection), Type.LONG);
    }

    private Connection getConnection(final Sequence[] args) throws XPathException {
        // get the db connection details
        final String dbURL = args[1].getStringValue();

        try {

            if (args.length == 2) {

                // try and get the connection
                return DriverManager.getConnection(dbURL);

            } else if (args.length == 3) {

                // try and get the connection
                final Properties props = ParametersExtractor.parseProperties(((NodeValue) args[2].itemAt(0)).getNode());
                return DriverManager.getConnection(dbURL, props);

            } else if (args.length == 4) {
                final String dbUser = args[2].getStringValue();
                final String dbPassword = args[3].getStringValue();

                // try and get the connection
                return DriverManager.getConnection(dbURL, dbUser, dbPassword);

            } else {
                throw new XPathException(this, "No function: " + getName() + "#" + getSignature().getArgumentCount());
            }

        } catch (final SQLException sqle) {
            LOGGER.error("sql:get-connection() Cannot connect to database: {}", dbURL, sqle);
            throw new XPathException(this, "sql:get-connection() Cannot connect to database: " + dbURL, sqle);
        }
    }

    private Connection getConnectionFromPool(final Sequence[] args) throws XPathException {
        final String poolName = args[0].getStringValue();
        final HikariDataSource pool = SQLModule.getPool(poolName);
        if (pool == null) {
            throw new XPathException(this, "There is no configured connection pool named: " + poolName);
        }

        try {
            if (args.length == 3) {
                final String username = args[1].getStringValue();
                final String password = args[2].getStringValue();
                return pool.getConnection(username, password);
            } else {
                return pool.getConnection();
            }
        } catch (final SQLException sqle) {
            LOGGER.error("sql:get-connection-from-pool() Cannot retrieve connection from pool: " + poolName, sqle);
            throw new XPathException(this, "sql:get-connection-from-pool() Cannot retrieve connection from pool: " + poolName, sqle);
        }
    }

    private static FunctionSignature[] functionSignatures(final String name, final String description, final FunctionReturnSequenceType returnType, final FunctionParameterSequenceType[][] variableParamTypes) {
        return FunctionDSL.functionSignatures(new QName(name, NAMESPACE_URI, PREFIX), description, returnType, variableParamTypes);
    }
}
