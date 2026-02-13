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

import com.evolvedbinary.j8fu.function.BiConsumerE;
import com.evolvedbinary.j8fu.function.ConsumerE;
import com.evolvedbinary.j8fu.function.Function2E;
import org.exist.security.PermissionDeniedException;
import org.exist.source.Source;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.XQueryPool;
import org.exist.xquery.value.Sequence;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Properties;

public class XQueryUtil {

    /**
     * Execute an XPath or XQuery.
     *
     * @param broker the database broker.
     * @param source the source of the query.
     * @param cacheQuery true if the compiled query should be cached, false otherwise.
     * @param contextSequence the context sequence to use when executing the query, or null if there is no context sequence.
     * @param outputProperties any output properties, or null.
     * @param preCompilationContext access to the XQuery Context pre-compilation, or null if access is not required.
     * @param preExecutionContext access to the XQuery Context pre-execution, or null if access is not required.
     * @param postExecutionContext access to the XQuery Context post-execution, or null if access is not required.
     *
     * @return the result of the query and compilation and execution times.
     *
     * @throws IOException if the source of the query cannot be read.
     * @throws PermissionDeniedException if the calling user does not have permission to execute the query.
     * @throws XPathException if the query raises an error.
     */
    public static XQueryUtil.QueryResult query(final DBBroker broker, final Source source, final boolean cacheQuery, @Nullable final Sequence contextSequence, @Nullable final Properties outputProperties, @Nullable final ConsumerE<XQueryContext, XPathException> preCompilationContext, @Nullable final ConsumerE<XQueryContext, XPathException> preExecutionContext, @Nullable final BiConsumerE<XQueryContext, QueryResult, XPathException> postExecutionContext) throws XPathException, PermissionDeniedException, IOException {
        return query(broker, source, false, cacheQuery, contextSequence, outputProperties, preCompilationContext, preExecutionContext, postExecutionContext);
    }

    /**
     * Execute an XPath or XQuery.
     *
     * @param broker the database broker.
     * @param source the source of the query.
     * @param isXPointer true if the query is an XPointer, false otherwise.
     * @param cacheQuery true if the compiled query should be cached, false otherwise.
     * @param contextSequence the context sequence to use when executing the query, or null if there is no context sequence.
     * @param outputProperties any output properties, or null.
     * @param preCompilationContext access to the XQuery Context pre-compilation, or null if access is not required.
     * @param preExecutionContext access to the XQuery Context pre-execution, or null if access is not required.
     * @param postExecutionContext access to the XQuery Context post-execution, or null if access is not required.
     *
     * @return the result of the query and compilation and execution times.
     *
     * @throws IOException if the source of the query cannot be read.
     * @throws PermissionDeniedException if the calling user does not have permission to execute the query.
     * @throws XPathException if the query raises an error.
     */
    public static XQueryUtil.QueryResult query(final DBBroker broker, final Source source, final boolean isXPointer, final boolean cacheQuery, @Nullable final Sequence contextSequence, @Nullable final Properties outputProperties, @Nullable final ConsumerE<XQueryContext, XPathException> preCompilationContext, @Nullable final ConsumerE<XQueryContext, XPathException> preExecutionContext, @Nullable final BiConsumerE<XQueryContext, QueryResult, XPathException> postExecutionContext) throws XPathException, PermissionDeniedException, IOException {
        final BrokerPool brokerPool = broker.getBrokerPool();
        final XQuery xquery = brokerPool.getXQueryService();

        @Nullable XQueryPool xqueryPool = null;
        @Nullable CompiledXQuery compiledXquery = null;
        @Nullable XQueryContext xqueryContext = null;
        try {
            if (cacheQuery) {
                xqueryPool = brokerPool.getXQueryPool();
                compiledXquery = xqueryPool.borrowCompiledXQuery(broker, source);
            } else {
                xqueryPool = null;
            }

            final long compilationTime;

            if (compiledXquery != null) {
                xqueryContext = compiledXquery.getContext();
                xqueryContext.prepareForReuse();

                if (preCompilationContext != null) {
                    preCompilationContext.accept(xqueryContext);
                }

                compiledXquery.getContext().updateContext(xqueryContext);
                xqueryContext.getWatchDog().reset();

                compilationTime = XQueryUtil.QueryResult.RETRIEVED_CACHED_COMPILED_QUERY;

            } else {
                xqueryContext = new XQueryContext(brokerPool);

                if (preCompilationContext != null) {
                    preCompilationContext.accept(xqueryContext);
                }

                final long compilationStart = System.currentTimeMillis();
                compiledXquery = xquery.compile(xqueryContext, source, isXPointer);
                compilationTime = System.currentTimeMillis() - compilationStart;
            }

            if (preExecutionContext != null) {
                preExecutionContext.accept(xqueryContext);
            }

            final long executionStart = System.currentTimeMillis();
            final Sequence result = xquery.execute(broker, compiledXquery, null, contextSequence, outputProperties, true);
            final long executionTime = System.currentTimeMillis() - executionStart;

            final QueryResult queryResult = new XQueryUtil.QueryResult(compilationTime, executionTime, result);

            if (postExecutionContext != null) {
                postExecutionContext.accept(xqueryContext, queryResult);
            }

            return queryResult;

        } finally {
            if (xqueryContext != null) {
                xqueryContext.runCleanupTasks();
            }

            if (xqueryPool != null && compiledXquery != null) {
                xqueryPool.returnCompiledXQuery(source, compiledXquery);
            }
        }
    }

    public static class QueryResult {
        public static final int RETRIEVED_CACHED_COMPILED_QUERY = -1;

        public final long compilationTime;
        public final long executionTime;
        public final Sequence result;

        public QueryResult(final long compilationTime, final long executionTime, final Sequence result) {
            this.compilationTime = compilationTime;
            this.executionTime = executionTime;
            this.result = result;
        }
    }

    /**
     * Compile a query and perform an operation with the compiled query.
     *
     * @param <T> the type of the result of the operation.
     *
     * @param broker the database broker.
     * @param source the source of the query.
     *
     * @param op the operation to perform with the compiled query.
     *
     * @return the result of the operation.
     */
    public static <T> T withCompiledQuery(final DBBroker broker, final Source source, final Function2E<CompiledXQuery, T, XPathException, PermissionDeniedException> op) throws XPathException, PermissionDeniedException, IOException {
        final BrokerPool pool = broker.getBrokerPool();
        final XQuery xqueryService = pool.getXQueryService();
        final XQueryPool xqueryPool = pool.getXQueryPool();
        final CompiledXQuery compiledQuery = compileQuery(broker, xqueryService, xqueryPool, source);
        try {
            return op.apply(compiledQuery);
        } finally {
            if (compiledQuery != null) {
                if (compiledQuery.getContext() != null) {
                    compiledQuery.getContext().runCleanupTasks();
                }
                xqueryPool.returnCompiledXQuery(source, compiledQuery);
            }
        }
    }

    private static CompiledXQuery compileQuery(final DBBroker broker, final XQuery xqueryService, final XQueryPool xqueryPool, final Source query) throws PermissionDeniedException, XPathException, IOException {
        @Nullable CompiledXQuery compiled = null;
        @Nullable XQueryContext context = null;
        try {
            compiled = xqueryPool.borrowCompiledXQuery(broker, query);
            if (compiled == null) {
                context = new XQueryContext(broker.getBrokerPool());
            } else {
                context = compiled.getContext();
                context.prepareForReuse();
            }

            if (compiled == null) {
                compiled = xqueryService.compile(context, query);
            } else {
                compiled.getContext().updateContext(context);
                context.getWatchDog().reset();
            }

            return compiled;

        } catch (final PermissionDeniedException | XPathException | IOException e) {
            if (context != null) {
                context.runCleanupTasks();
            }
            if (compiled != null) {
                xqueryPool.returnCompiledXQuery(query, compiled);
            }
            throw e;
        }
    }

    /**
     * Execute a compiled query.
     *
     * @param broker the database broker.
     * @param compiledXQuery the compiled query.
     *
     * @return the result sequence.
     */
    public static Sequence executeQuery(final DBBroker broker, final CompiledXQuery compiledXQuery) throws PermissionDeniedException, XPathException {
        final BrokerPool pool = broker.getBrokerPool();
        final XQuery xqueryService = pool.getXQueryService();
        return xqueryService.execute(broker, compiledXQuery, null, null, null, true);
    }
}
