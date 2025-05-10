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
package org.exist.management.impl;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;

public class Database implements DatabaseMXBean {

    private final BrokerPool pool;

    public Database(final BrokerPool pool) {
        this.pool = pool;
    }

    public static String getAllInstancesQuery() {
        return getName("*");
    }

    private static String getName(final String instanceId) {
        return "org.exist.management." + instanceId + ":type=Database";
    }

    @Override
    public ObjectName getName() throws MalformedObjectNameException {
        return new ObjectName(getName(pool.getId()));
    }

    @Override
    public String getInstanceId() {
        return pool.getId();
    }

    @Override
    public String getStatus() {
        return pool.getStatus();
    }

    @Override
    public void shutdown() {
        pool.shutdown();
    }

    @Override
    public int getMaxBrokers() {
        return pool.getMax();
    }

    @Override
    public int getAvailableBrokers() {
        return pool.available();
    }

    @Override
    public int getActiveBrokers() {
        return pool.countActiveBrokers();
    }

    @Override
    public int getTotalBrokers() {
        return pool.total();
    }

    @Override
    public Map<String, ActiveBroker> getActiveBrokersMap() {
        final Map<String, ActiveBroker> brokersList = new HashMap<>();

        for (final Map.Entry<Thread, DBBroker> entry : pool.getActiveBrokers().entrySet()) {
            final Thread thread = entry.getKey();
            final DBBroker broker = entry.getValue();
            final String trace = printStackTrace(thread);
            final String watchdogTrace = pool.getWatchdog().map(wd -> wd.get(broker)).orElse(null);
            brokersList.put(broker.getId(), new ActiveBroker(thread.getName(), broker.getReferenceCount(), trace, watchdogTrace));
        }
        return brokersList;
    }

    @Override
    public long getReservedMem() {
        return pool.getReservedMem();
    }

    @Override
    public long getCacheMem() {
        return pool.getCacheManager().getTotalMem();
    }

    @Override
    public long getCollectionCacheMem() {
        return pool.getCollectionsCache().getMaxCacheSize();
    }

    @Override
    public long getUptime() {
        return System.currentTimeMillis() - pool.getStartupTime().getTimeInMillis();
    }

    @Override
    public String getExistHome() {
        return pool.getConfiguration().getExistHome().map(p -> p.toAbsolutePath().toString()).orElse(null);
    }

    public String printStackTrace(final Thread thread) {
        final StackTraceElement[] stackElements = thread.getStackTrace();
        final StringWriter writer = new StringWriter();
        final int showItems = stackElements.length > 20 ? 20 : stackElements.length;
        for (int i = 0; i < showItems; i++) {
            writer.append(stackElements[i].toString()).append('\n');
        }
        return writer.toString();
    }
}
