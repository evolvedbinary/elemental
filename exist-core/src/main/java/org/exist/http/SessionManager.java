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
package org.exist.http;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import net.jcip.annotations.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.xquery.XQueryUtil;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@ThreadSafe
public class SessionManager {

    private static final Logger LOG = LogManager.getLogger(SessionManager.class);
    private static final long TIMEOUT = 120_000;  // ms (e.g. 2 minutes)

    private static final RemovalListener<Integer, QueryAndResult> REMOVAL_LISTENER = (sessionId, queryAndResult, removalCause) -> {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Removing cached query result for session: {}", sessionId);
        }

        // NOTE(AR) make sure to release any resources still held by the query result and potentially send it back to the query pool for reuse
        queryAndResult.result.close();
    };

    private final AtomicInteger sessionIdCounter = new AtomicInteger();
    private final Cache<Integer, QueryAndResult> cache;

    private static class QueryAndResult {
        final String query;
        final XQueryUtil.QueryResult result;

        private QueryAndResult(final String query, final XQueryUtil.QueryResult result) {
            this.query = query;
            this.result = result;
        }
    }

    public SessionManager() {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(TIMEOUT, TimeUnit.MILLISECONDS)
                .removalListener(REMOVAL_LISTENER)
                .build();
    }

    public int add(final String query, final XQueryUtil.QueryResult result) {
        final int sessionId = sessionIdCounter.getAndIncrement();
        cache.put(sessionId, new QueryAndResult(query, result));
        return sessionId;
    }

    public @Nullable XQueryUtil.QueryResult get(final String query, final int sessionId) {
        if (sessionId < 0 || sessionId >= sessionIdCounter.get()) {
            return null; // out of scope
        }

        final QueryAndResult cached = cache.getIfPresent(sessionId);
        if (cached == null) {
            return null;
        }

        if (cached.query.equals(query)) {
            return cached.result;
        } else {
            // wrong query
            return null;
        }
    }

    public void release(final int sessionId) {
        if (sessionId < 0 || sessionId >= sessionIdCounter.get()) {
            return; // out of scope
        }
        cache.invalidate(sessionId);
    }
}
