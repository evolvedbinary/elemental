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
package org.exist.storage;

import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.Deque;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.jcip.annotations.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.source.DBSource;
import org.exist.source.Source;
import org.exist.util.Configuration;
import org.exist.util.Holder;
import org.exist.xquery.*;

/**
 * Global pool for compiled XQuery expressions.
 *
 * Expressions are stored and retrieved from the pool by comparing the
 * {@link org.exist.source.Source} objects from which they were created.
 *
 * For each XQuery, a maximum of {@link #DEFAULT_MAX_QUERY_STACK_SIZE} compiled
 * expressions are kept in the pool.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@ThreadSafe
public class XQueryPool implements BrokerPoolService {

    private static final Logger LOG = LogManager.getLogger(XQueryPool.class);

    public static final String CONFIGURATION_ELEMENT_NAME = "query-pool";
    public static final String MAX_STACK_SIZE_ATTRIBUTE = "max-stack-size";
    public static final String POOL_SIZE_ATTTRIBUTE = "size";

    public static final String PROPERTY_MAX_STACK_SIZE = "db-connection.query-pool.max-stack-size";
    public static final String PROPERTY_POOL_SIZE = "db-connection.query-pool.size";

    private static final int DEFAULT_MAX_POOL_SIZE = 128;
    private static final int DEFAULT_MAX_QUERY_STACK_SIZE = 64;

    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    private int maxQueryStackSize = DEFAULT_MAX_QUERY_STACK_SIZE;

    /**
     * Source -> Deque of compiled Queries
     */
    private Cache<Source, Deque<CompiledXQuery>> cache;

    @Override
    public void configure(final Configuration configuration) {
        final Integer maxStSz = (Integer) configuration.getProperty(PROPERTY_MAX_STACK_SIZE);
        final Integer maxPoolSz = (Integer) configuration.getProperty(PROPERTY_POOL_SIZE);
        final NumberFormat nf = NumberFormat.getNumberInstance();

        if (maxPoolSz != null) {
            this.maxPoolSize = maxPoolSz;
        } else {
            this.maxPoolSize = DEFAULT_MAX_POOL_SIZE;
        }

        if (maxStSz != null) {
            this.maxQueryStackSize = maxStSz;
        } else {
            this.maxQueryStackSize = DEFAULT_MAX_QUERY_STACK_SIZE;
        }

        this.cache = Caffeine.newBuilder()
                .maximumSize(maxPoolSize)
                .build();

        LOG.info("QueryPool: size = {}; maxQueryStackSize = {}", nf.format(maxPoolSize), nf.format(maxQueryStackSize));
    }

    /**
     * Returns a compiled XQuery to the XQuery pool.
     *
     * @param source The source of the compiled XQuery.
     * @param compiledXQuery The compiled XQuery to add to the XQuery pool.
     */
    public void returnCompiledXQuery(final Source source, final CompiledXQuery compiledXQuery) {
        if (compiledXQuery == null) {
            return;
        }

        cache.asMap().compute(source, (key, value) -> {
            final Deque<CompiledXQuery> deque;
            if (value != null) {
                deque = value;
            } else {
                deque = new ArrayDeque<>(maxQueryStackSize);
            }

            deque.offerFirst(compiledXQuery);

            return deque;
        });
    }

    /**
     * Borrows a compiled XQuery from the XQuery pool.
     *
     * @param broker A database broker.
     * @param source The source identifying the XQuery to borrow.
     *
     * @return The compiled XQuery identified by the source, or null if
     *     there is no valid compiled representation in the XQuery pool.
     *
     * @throws PermissionDeniedException if the caller does not have execute
     *     permission for the compiled XQuery.
     */
    public CompiledXQuery borrowCompiledXQuery(final DBBroker broker, final Source source)
            throws PermissionDeniedException {
        if (broker == null || source == null) {
            return null;
        }

        // this will be set to non-null if we can borrow a query... allows us to escape the lamba, see https://github.com/ben-manes/caffeine/issues/192#issuecomment-337365618
        final Holder<CompiledXQuery> borrowedCompiledQuery = new Holder<>();

        // get (compute by checking validity) the stack of compiled XQuerys for the source
        final Deque<CompiledXQuery> deque = cache.asMap().computeIfPresent(source, (key, value) -> {
            final CompiledXQuery firstCompiledXQuery = value.pollFirst();
            if (firstCompiledXQuery == null) {
                // deque is empty, returning null will remove the entry from the cache
                return null;
            }

            if (!isCompiledQueryValid(firstCompiledXQuery)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("{} is invalid, removing from XQuery Pool...", source.pathOrShortIdentifier());
                }

                // query is invalid, returning null will remove the entry from the cache
                return null;
            }

            // escape the result from the lambda
            borrowedCompiledQuery.value = firstCompiledXQuery;

            // query is ok, preserve the tail of the deque
            return value;
        });

        if (deque == null) {
            return null;
        }

        //check execution permission
        if (source instanceof DBSource) {
            ((DBSource) source).validate(Permission.EXECUTE);
        }

        return borrowedCompiledQuery.value;
    }

    /**
     * Determines if a compiled XQuery is still valid.
     *
     * @param broker the database broker
     * @param source the source of the query
     * @param compiledXQuery the compiled query
     *
     * @return true if the compiled query is still valid, false otherwise.
     */
    private static boolean isCompiledQueryValid(final CompiledXQuery compiledXQuery) {
        final Source cachedSource = compiledXQuery.getSource();
        final Source.Validity validity = cachedSource.isValid();

        if (validity == Source.Validity.INVALID) {
            return false;    // returning false will remove the entry from the cache
        }

        // the compiled query is no longer valid if one of the imported
        // modules may have changed
        return compiledXQuery.isValid();
    }

    /**
     * Removes all entries from the XQuery Pool.
     */
    public void clear() {
        cache.invalidateAll();
    }
}
