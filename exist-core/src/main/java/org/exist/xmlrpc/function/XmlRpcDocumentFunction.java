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
package org.exist.xmlrpc.function;

import org.exist.EXistException;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import com.evolvedbinary.j8fu.function.TriFunction2E;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Specialisation of FunctionE which deals with
 * XML-RPC server operations; Predominantly converts exceptions
 * from the database into EXistException types
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@FunctionalInterface
public interface XmlRpcDocumentFunction<R> extends TriFunction2E<DocumentImpl, DBBroker, Txn, R, EXistException, PermissionDeniedException> {

    @Override
    default R apply(final DocumentImpl document, final DBBroker broker, final Txn transaction) throws EXistException, PermissionDeniedException {
        try {
            return applyXmlRpc(document, broker, transaction);
        } catch(final SAXException | IOException e) {
            throw new EXistException(e);
        }
    }

    /**
     * Signature for lambda function which takes a document.
     *
     * @param document the document for the function
     * @param broker the database broker
     * @param transaction the database transaction

     * @return the result of the function
     *
     * @throws EXistException if an error occurs with the database
     * @throws PermissionDeniedException if the caller has insufficient priviledges
     * @throws SAXException if a SAX error occurs
     * @throws IOException if an I/O error occurs
     */
    R applyXmlRpc(final DocumentImpl document, final DBBroker broker, final Txn transaction) throws EXistException, PermissionDeniedException, SAXException, IOException;
}
