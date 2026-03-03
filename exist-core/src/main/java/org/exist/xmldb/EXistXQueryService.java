/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
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
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
package org.exist.xmldb;

import java.io.Writer;

import org.exist.source.Source;
import org.exist.xquery.XPathException;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;

/**
 * Execute XQuery expressions on the database.
 *
 * This interface is similar to {@link org.xmldb.api.modules.XPathQueryService}, but
 * provides additional methods to compile an XQuery into an internal representation, which
 * can be executed repeatedly. Since XQuery scripts can be very large, compiling an expression
 * in advance can save a lot of time.
 *
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
public interface EXistXQueryService extends XQueryService {

    /**
     * Process a query based on the result of a previous query.
     * The XMLResource contains the result received from a previous
     * query.
     *
     * @param res an XMLResource as obtained from a previous query.
     * @param query the XPath query
     *
     * @return The query results
     *
     * @throws XMLDBException If an error occurs whilst executing the query
     */
    EXistResourceSet query(XMLResource res, String query) throws XMLDBException;

    /**
     * Tries to compile the specified XQuery and returns a handle to the compiled
     * code, which can then be passed to {@link #execute(CompiledExpression)}.
     * If a static error is detected, an {@link XPathException} will be thrown.
     *
     * @param query The XQuery to compile
     *
     * @return a compiled representation of the query
     *
     * @throws XMLDBException if an error occurs whilst compiling the query,
     *     or a static error is detected.
     * @throws XPathException if an error occurs whilst executing the query.
     */
    CompiledExpression compileAndCheck(String query) throws XMLDBException, XPathException;

    /**
     * Executes the query.
     *
     * @param querySource The source of the query
     *
     * @return The results of the query
     *
     * @throws XMLDBException if an error occurs whilst executing the query
     */
    EXistResourceSet execute(Source querySource) throws XMLDBException;

    /**
     * Execute a compiled query based on the result of a previous query.
     * The XMLResource contains the result received from a previous
     * query.
     *
     * The implementation should pass all namespaces and variables declared through
     * {@link EXistXQueryService} to the compiled XQuery code.
     *
     * Note: {@link CompiledExpression} is not thread safe. Please make sure you don't
     * call the same compiled expression from two threads at the same time.
     *
     * @param res an XMLResource as obtained from a previous query.
     * @param compiledExpression a compiled query
     *
     * @return The results of the query
     *
     * @throws XMLDBException if an error occurs whilst executing the query
     */
    EXistResourceSet execute(XMLResource res, CompiledExpression compiledExpression) throws XMLDBException;

    /**
     * Clears any previously declared variables
     *
     * @throws XMLDBException if an error occurs whilst clearning the variables.
     */
    void clearVariables() throws XMLDBException;

    /**
     * Return a diagnostic dump of the query. The query should have been executed
     * before calling this function.
     *
     * @param compiledExpression The compiled query to dump.
     * @param writer a writer which received a dump of the query.
     *
     * @throws XMLDBException if an error occurs whilst dumping the query
     */
    void dump(CompiledExpression compiledExpression, Writer writer) throws XMLDBException;
}
