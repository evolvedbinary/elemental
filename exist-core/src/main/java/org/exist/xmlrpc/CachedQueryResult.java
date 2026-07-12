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
package org.exist.xmlrpc;

import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * Simple container for the results of a query. Used to cache
 * query results that may be retrieved later by the client.
 *
 * @author wolf
 * @author jmfernandez
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class CachedQueryResult extends AbstractCachedResult {

    private static final Logger LOG = LogManager.getLogger(CachedQueryResult.class);
    private final XQueryUtil.QueryResult queryResult;
    private @Nullable final Properties serialization;

    public CachedQueryResult(final XQueryUtil.QueryResult queryResult, @Nullable final Properties outputProperties) {
        super(queryResult.totalTime());
        this.queryResult = queryResult;
        this.serialization = outputProperties;
    }

    /**
     * Get the total time to produce the query result.
     * This is compilation time (if any) plus the execution time.
     *
     * @return total time to produce the query result.
     */
    public long totalTime() {
        return queryResult.totalTime();
    }

    @Override
    public Sequence getResult() {
        return queryResult.result;
    }

    public @Nullable Properties getSerialization() {
        return serialization;
    }

    @Override
    protected void doClose() {
        queryResult.close();
    }
}
