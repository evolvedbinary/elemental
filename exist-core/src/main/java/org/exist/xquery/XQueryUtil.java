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
import org.exist.security.PermissionDeniedException;
import org.exist.source.Source;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.XQueryPool;
import org.exist.xquery.value.Sequence;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility to make compiling and executing XQuery simpler
 * and safer by ensuring cleanup when the returned object is closed.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class XQueryUtil {

    /**
     * Execute an XPath or XQuery.
     * Performs both the compile and execute steps.
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
     * Performs both the compile and execute steps.
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
        final CompilationResult compilationResult = compile(broker, source, isXPointer, cacheQuery, preCompilationContext);
        return execute(broker, compilationResult, contextSequence, outputProperties, preExecutionContext, postExecutionContext);
    }

    /**
     * Compile an XPath or XQuery.
     *
     * @param broker the database broker.
     * @param source the source of the query.
     * @param isXPointer true if the query is an XPointer, false otherwise.
     * @param cacheQuery true if the compiled query should be cached, false otherwise.
     * @param preCompilationContext access to the XQuery Context pre-compilation, or null if access is not required.
     *
     * @return the result of the compilation and the compilation time.
     *
     * @throws IOException if the source of the query cannot be read.
     * @throws PermissionDeniedException if the calling user does not have permission to execute the query.
     * @throws XPathException if the query raises an error.
     */
    public static CompilationResult compile(final DBBroker broker, final Source source, final boolean isXPointer, final boolean cacheQuery, @Nullable final ConsumerE<XQueryContext, XPathException> preCompilationContext) throws XPathException, PermissionDeniedException, IOException {
        final BrokerPool brokerPool = broker.getBrokerPool();
        final XQuery xquery = brokerPool.getXQueryService();

        @Nullable final XQueryPool xqueryPool;
        @Nullable CompiledXQuery compiledXquery = null;
        if (cacheQuery) {
            xqueryPool = brokerPool.getXQueryPool();
            compiledXquery = xqueryPool.borrowCompiledXQuery(broker, source);
        } else {
            xqueryPool = null;
        }

        @Nullable XQueryContext xqueryContext = null;
        try {
            final long compilationTime;
            if (compiledXquery != null) {
                xqueryContext = compiledXquery.getContext();
                xqueryContext.prepareForReuse();

                if (preCompilationContext != null) {
                    preCompilationContext.accept(xqueryContext);
                }

                compiledXquery.getContext().updateContext(xqueryContext);
                xqueryContext.getWatchDog().reset();

                compilationTime = XQueryUtil.CompilationResult.RETRIEVED_CACHED_COMPILED_QUERY;

            } else {
                xqueryContext = new XQueryContext(brokerPool);

                if (preCompilationContext != null) {
                    preCompilationContext.accept(xqueryContext);
                }

                final long compilationStart = System.currentTimeMillis();
                compiledXquery = xquery.compile(xqueryContext, source, isXPointer);
                compilationTime = System.currentTimeMillis() - compilationStart;
            }

            return new CompilationResult(source, xqueryPool, compiledXquery, xqueryContext, compilationTime);

        } catch (final XPathException | PermissionDeniedException | IOException e) {
            // Make sure to clean-up in case of an exception!
            if (xqueryContext != null) {
                xqueryContext.runCleanupTasks();
            }

            if (xqueryPool != null && compiledXquery != null) {
                xqueryPool.returnCompiledXQuery(source, compiledXquery);
            }

            throw e;
        }
    }

    /**
     * Execute a compiled XPath or XQuery.
     *
     * @param broker the database broker.
     * @param compilationResult the compiled query.
     * @param contextSequence the context sequence to use when executing the query, or null if there is no context sequence.
     * @param outputProperties any output properties, or null.
     * @param preExecutionContext access to the XQuery Context pre-execution, or null if access is not required.
     * @param postExecutionContext access to the XQuery Context post-execution, or null if access is not required.
     *
     * @return the result of the query and compilation and execution times.
     *
     * @throws PermissionDeniedException if the calling user does not have permission to execute the query.
     * @throws XPathException if the query raises an error.
     */
    public static QueryResult execute(final DBBroker broker, final CompilationResult compilationResult, @Nullable final Sequence contextSequence, @Nullable final Properties outputProperties, @Nullable final ConsumerE<XQueryContext, XPathException> preExecutionContext, @Nullable final BiConsumerE<XQueryContext, QueryResult, XPathException> postExecutionContext) throws XPathException, PermissionDeniedException {
        final BrokerPool brokerPool = broker.getBrokerPool();
        final XQuery xquery = brokerPool.getXQueryService();

        try {
            if (preExecutionContext != null) {
                preExecutionContext.accept(compilationResult.xqueryContext);
            }

            final long executionStart = System.currentTimeMillis();
            final Sequence result = xquery.execute(broker, compilationResult.compiledXquery, null, contextSequence, outputProperties, true);
            final long executionTime = System.currentTimeMillis() - executionStart;

            final QueryResult queryResult = new XQueryUtil.QueryResult(compilationResult, executionTime, result);

            if (postExecutionContext != null) {
                postExecutionContext.accept(compilationResult.xqueryContext, queryResult);
            }

            return queryResult;

        } catch (final XPathException | PermissionDeniedException e) {
            // Make sure to clean-up in case of an exception!
            compilationResult.xqueryContext.runCleanupTasks();

            if (compilationResult.xqueryPool != null) {
                compilationResult.xqueryPool.returnCompiledXQuery(compilationResult.source, compilationResult.compiledXquery);
            }

            throw e;
        }
    }

    public static class CompilationResult implements AutoCloseable {
        /**
         * Indicates that the query did not need to be compiled as a cached version was available.
         */
        public static final int RETRIEVED_CACHED_COMPILED_QUERY = -1;

        private final Source source;
        private @Nullable final XQueryPool xqueryPool;
        private final CompiledXQuery compiledXquery;
        private final XQueryContext xqueryContext;
        private boolean closed = false;

        public final long compilationTime;

        public CompilationResult(final Source source, @Nullable final XQueryPool xqueryPool, final CompiledXQuery compiledXquery, final XQueryContext xqueryContext, final long compilationTime) {
            this.source = source;
            this.xqueryPool = xqueryPool;
            this.compiledXquery = compiledXquery;
            this.xqueryContext = xqueryContext;
            this.compilationTime = compilationTime;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            // NOTE(AR) Only when the user has finished with the Query Result i.e. {@link #result}, can we then clean-up any associated resources.
            xqueryContext.runCleanupTasks();

            // Once we have cleaned-up if the query should be cached we return it to the query pool.
            if (xqueryPool != null && compiledXquery != null) {
                xqueryPool.returnCompiledXQuery(source, compiledXquery);
            }

            closed = true;
        }

    }

    public static class QueryResult implements AutoCloseable {
        private final Source source;
        private @Nullable final XQueryPool xqueryPool;
        private @Nullable final CompiledXQuery compiledXquery;
        private final XQueryContext xqueryContext;
        private boolean closed = false;

        public final long compilationTime;
        public final long executionTime;
        public final Sequence result;
        
        public QueryResult(final Source source, @Nullable final XQueryPool xqueryPool, @Nullable final CompiledXQuery compiledXquery, final XQueryContext xqueryContext, final long compilationTime, final long executionTime, final Sequence result) {
            this.source = source;
            this.xqueryPool = xqueryPool;
            this.compiledXquery = compiledXquery;
            this.xqueryContext = xqueryContext;

            this.compilationTime = compilationTime;
            this.executionTime = executionTime;
            this.result = result;
        }

        private QueryResult(final CompilationResult compilationResult, final long executionTime, final Sequence result) {
            this.source = compilationResult.source;
            this.xqueryPool = compilationResult.xqueryPool;
            this.compiledXquery = compilationResult.compiledXquery;
            this.xqueryContext = compilationResult.xqueryContext;

            this.compilationTime = compilationResult.compilationTime;
            this.executionTime = executionTime;
            this.result = result;

            // transfer ownership of the resources owned by compilationResult to this class
            compilationResult.closed = true;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            // NOTE(AR) Only when the user has finished with the Query Result i.e. {@link #result}, can we then clean-up any associated resources.
            xqueryContext.runCleanupTasks();

            // Once we have cleaned-up if the query should be cached we return it to the query pool.
            if (xqueryPool != null && compiledXquery != null) {
                xqueryPool.returnCompiledXQuery(source, compiledXquery);
            }

            closed = true;
        }
    }
}
