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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.exist.dom.QName;
import org.exist.xquery.*;
import org.exist.xquery.value.*;

import java.sql.Connection;
import java.sql.SQLException;

import static org.exist.xquery.FunctionDSL.*;
import static org.exist.xquery.modules.sql.SQLModule.*;


/**
 * SQL Module Extension function for XQuery to explicitly close a connection.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class CloseConnectionFunction extends BasicFunction {

    private static final Logger LOGGER = LogManager.getLogger(GetConnectionFunction.class);

    private static final String FN_CLOSE_CONNECTION = "close-connection";
    public static final FunctionSignature FS_CLOSE_CONNECTION = functionSignature(
            FN_CLOSE_CONNECTION,
            "Closes a connection to a SQL Database, or if the connection was taken from a connection pool then it is returned to the pool",
            returns(Type.BOOLEAN, "true if the connection was closed, false if there was no such connection"),
            param("connection-handle", Type.LONG, "an xs:long representing the connection handle")
    );

    public CloseConnectionFunction(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    /**
     * Evaluate the call to the xquery close-connection().
     *
     * @param args arguments from the close-connection() function call
     * @param contextSequence the Context Sequence to operate on (not used here internally!)
     *
     * @return An empty sequence
     *
     * @throws XPathException if an error occurs.
     */
    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final IntegerValue connectionHandle = (IntegerValue) args[0].itemAt(0);
        final long connectionUid = connectionHandle.toJavaObject(long.class);

        final Connection connection = SQLModule.removeConnection(context, connectionUid);
        if (connection == null) {
            return BooleanValue.FALSE;
        }

        try {
            if (connection.isClosed()) {
                LOGGER.warn("sql:close-connection() Cannot close connection with handle: {}, as it is already closed!", connectionUid);
                return BooleanValue.FALSE;
            }

            connection.close();
            return BooleanValue.TRUE;

        } catch (final SQLException e) {
            throw new XPathException(this, "Unable to close connection with handle: " + connectionUid + ". " + e.getMessage());
        }
    }

    private static FunctionSignature functionSignature(final String name, final String description, final FunctionReturnSequenceType returnType, final FunctionParameterSequenceType... paramTypes) {
        return FunctionDSL.functionSignature(new QName(name, NAMESPACE_URI, PREFIX), description, returnType, paramTypes);
    }
}
