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
 */
package org.exist.xquery;

import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.value.Sequence;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class ArrowOperatorTest {

    @Rule
    public final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    /**
     * Test to ensure that the ContextParam for the Arrow Operator
     * (when directly calling a function) is correctly null'ed out
     * so that it doesn't leak memory if the query is cached for reuse.
     */
    @Test
    public void lhsContextSequenceLeakFunctionReference() throws EXistException, XPathException, PermissionDeniedException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final XQuery xquery = brokerPool.getXQueryService();

        final XQueryContext xqueryContext = new XQueryContext(brokerPool);

        final CompiledXQuery compiledXquery;
        try (final DBBroker broker = brokerPool.getBroker()) {
            compiledXquery = xquery.compile(xqueryContext, "<a/> => fn:local-name()");
            final Sequence results = xquery.execute(broker, compiledXquery, null);
            assertEquals(1, results.getItemCount());
            assertEquals("a", results.itemAt(0).toString());
        }

        // Get the ArrowOperator's Function Call
        final PathExpr pathExpr = (PathExpr) compiledXquery;
        final ArrowOperator arrowOperator = (ArrowOperator) pathExpr.steps.get(0);
        final FunctionCall functionCall = arrowOperator.functionCall;
        assertNotNull(functionCall);
        assertFalse(functionCall.steps.isEmpty());

        // Get the ContextParam#Sequence for the Function Call
        final Expression contextParamExpr = functionCall.steps.get(0);
        assertTrue(contextParamExpr instanceof ArrowOperator.ContextParam);
        final ArrowOperator.ContextParam contextParam = (ArrowOperator.ContextParam) contextParamExpr;

        // Ensure that the ContextParam#sequence has been null'ed after execution to avoid a memory-leak
        assertNull(contextParam.sequence);
    }

    /**
     * Test to ensure that the ContextParam for the Arrow Operator
     * (when indirectly calling a function due to expression evaluation)
     * is correctly null'ed out so that it doesn't leak memory if the
     * query is cached for reuse.
     */
    @Test
    public void lhsContextSequenceLeakVariableReference() throws EXistException, XPathException, PermissionDeniedException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final XQuery xquery = brokerPool.getXQueryService();

        final XQueryContext xqueryContext = new XQueryContext(brokerPool);

        final CompiledXQuery compiledXquery;
        try (final DBBroker broker = brokerPool.getBroker()) {
            compiledXquery = xquery.compile(xqueryContext, "let $f := fn:local-name#1 return <a/> => $f()");
            final Sequence results = xquery.execute(broker, compiledXquery, null);
            assertEquals(1, results.getItemCount());
            assertEquals("a", results.itemAt(0).toString());
        }

        // Get the ArrowOperator's Function Call
        final PathExpr pathExpr = (PathExpr) compiledXquery;
        final LetExpr letExpr = (LetExpr) pathExpr.steps.get(0);
        final NamedFunctionReference namedFunctionRef = (NamedFunctionReference) letExpr.inputSequence;
        final FunctionCall functionCall = namedFunctionRef.resolvedFunction;
        assertNotNull(functionCall);
        assertFalse(functionCall.steps.isEmpty());

        // Get the ContextParam#Sequence for the Function Call
        final Expression contextParamExpr = functionCall.steps.get(0);
        assertTrue(contextParamExpr instanceof ArrowOperator.ContextParam);
        final ArrowOperator.ContextParam contextParam = (ArrowOperator.ContextParam) contextParamExpr;

        // Ensure that the ContextParam#sequence has been null'ed after execution to avoid a memory-leak
        assertNull(contextParam.sequence);
    }
}
