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
package org.exist.xmldb.function;

import org.exist.dom.persistent.DocumentImpl;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.util.LockException;
import org.exist.util.SyntaxException;
import com.evolvedbinary.j8fu.function.TriFunctionE;
import org.xmldb.api.base.ErrorCodes;
import org.xmldb.api.base.XMLDBException;

import java.io.IOException;

/**
 * Specialisation of FunctionE which deals with
 * local XMLDB operations; Predominantly converts exceptions
 * from the database into XMLDBException types
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@FunctionalInterface
public interface LocalXmldbDocumentFunction<R> extends TriFunctionE<DocumentImpl, DBBroker, Txn, R, XMLDBException> {

    @Override
    default R apply(final DocumentImpl document, final DBBroker broker, final Txn transaction) throws XMLDBException {
        try {
            return applyXmldb(document, broker, transaction);
        } catch(final PermissionDeniedException e) {
            throw new XMLDBException(ErrorCodes.PERMISSION_DENIED, e.getMessage(), e);
        } catch(final LockException e) {
            throw new XMLDBException(ErrorCodes.COLLECTION_CLOSED, e.getMessage(), e);
        } catch(final IOException | SyntaxException e) {
            throw new XMLDBException(ErrorCodes.UNKNOWN_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Signature for lambda function which takes a document.
     *
     * @param document The database collection
     * @param broker the database broker
     * @param transaction the database transaction
     *
     * @return the result of apply the function.
     *
     * @throws XMLDBException if an error occurs whilst applying the function
     * @throws PermissionDeniedException if the user has insufficient permissions
     * @throws LockException if an error occurs whilst locking a collection or document
     * @throws IOException if an IO error occurs
     * @throws SyntaxException if a syntax error occurs
     */
    R applyXmldb(final DocumentImpl document, final DBBroker broker, final Txn transaction)
            throws XMLDBException, PermissionDeniedException, LockException, IOException, SyntaxException;
}
