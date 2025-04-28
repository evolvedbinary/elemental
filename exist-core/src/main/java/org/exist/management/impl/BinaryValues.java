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
package org.exist.management.impl;

import org.exist.storage.BrokerPool;
import org.exist.util.io.FileFilterInputStreamCache;
import org.exist.util.io.FilterInputStreamCache;
import org.exist.util.io.FilterInputStreamCacheMonitor;
import org.exist.util.io.FilterInputStreamCacheMonitor.FilterInputStreamCacheInfo;
import org.exist.util.io.MemoryMappedFileFilterInputStreamCache;
import org.exist.management.impl.BinaryInputStreamCacheInfo.CacheType;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class BinaryValues implements BinaryValuesMXBean {
    private final String instanceId;

    public BinaryValues(final BrokerPool pool) {
        this.instanceId = pool.getId();
    }

    public static String getAllInstancesQuery() {
        return getName("*");
    }

    private static String getName(final String instanceId) {
        return "org.exist.management." + instanceId + ":type=BinaryValues";
    }

    @Override
    public ObjectName getName() throws MalformedObjectNameException {
        return new ObjectName(getName(instanceId));
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public List<BinaryInputStreamCacheInfo> getCacheInstances() {
        final FilterInputStreamCacheMonitor monitor = FilterInputStreamCacheMonitor.getInstance();
        final Collection<FilterInputStreamCacheInfo> cacheInstances = monitor.getActive();

        final List<BinaryInputStreamCacheInfo> results = new ArrayList<>();
        for (final FilterInputStreamCacheInfo cacheInstance : cacheInstances) {

            final BinaryInputStreamCacheInfo result;
            final FilterInputStreamCache cache = cacheInstance.getCache();
            if (cache instanceof FileFilterInputStreamCache) {
                result = new BinaryInputStreamCacheInfo(CacheType.FILE, cacheInstance.getRegistered(),
                        Optional.of(((FileFilterInputStreamCache) cache).getFilePath()), cache.getLength());
            } else if (cache instanceof MemoryMappedFileFilterInputStreamCache) {
                result = new BinaryInputStreamCacheInfo(CacheType.MEMORY_MAPPED_FILE, cacheInstance.getRegistered(),
                        Optional.of(((MemoryMappedFileFilterInputStreamCache) cache).getFilePath()), cache.getLength());
            } else {
                result = new BinaryInputStreamCacheInfo(CacheType.MEMORY, cacheInstance.getRegistered(),
                        Optional.empty(), cache.getLength());
            }

            results.add(result);
        }

        return results;
    }
}
