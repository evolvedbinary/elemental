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
package org.exist.util.io;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Monitors active {@link FilterInputStreamCacheMonitor} instances.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class FilterInputStreamCacheMonitor {

    private static final FilterInputStreamCacheMonitor INSTANCE = new FilterInputStreamCacheMonitor();

    private final ConcurrentMap<FilterInputStreamCache, FilterInputStreamCacheInfo> activeCaches = new ConcurrentHashMap<>();

    private FilterInputStreamCacheMonitor() {
    }

    public static FilterInputStreamCacheMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * Intentionally package private!
     * 
     * Only for use by org.exist.util.io.FilterInputStreamCacheMonitorTest
     */
    void clear() {
        activeCaches.clear();
    }

    public void register(final FilterInputStreamCache cache) {
        final long now = System.currentTimeMillis();
        final FilterInputStreamCacheInfo info = new FilterInputStreamCacheInfo(now, cache);
        activeCaches.put(cache, info);
    }

    public Collection<FilterInputStreamCacheInfo> getActive() {
        final List<FilterInputStreamCacheInfo> list = new ArrayList(activeCaches.values());
        list.sort(Comparator.comparingLong(FilterInputStreamCacheInfo::getRegistered));
        return list;
    }

    public void deregister(final FilterInputStreamCache cache) {
        activeCaches.remove(cache);
    }

    public String dump() {
        final StringBuilder builder = new StringBuilder();
        for (final FilterInputStreamCacheInfo info : getActive()) {
            final FilterInputStreamCache cache = info.getCache();
            final String id;
            if (cache instanceof FileFilterInputStreamCache) {
                id = ((FileFilterInputStreamCache)cache).getFilePath().normalize().toAbsolutePath().toString();
            } else if (cache instanceof MemoryMappedFileFilterInputStreamCache) {
                id = ((MemoryMappedFileFilterInputStreamCache)cache).getFilePath().normalize().toAbsolutePath().toString();
            } else if (cache instanceof MemoryFilterInputStreamCache) {
                id = "mem";
            } else {
                id = "unknown";
            }
            builder.append(info.getRegistered() + ": " + id);
        }
        return builder.toString();
    }

    public static class FilterInputStreamCacheInfo {
        private final long registered;
        private final FilterInputStreamCache cache;

        public FilterInputStreamCacheInfo(final long registered, final FilterInputStreamCache cache) {
            this.registered = registered;
            this.cache = cache;
        }

        public long getRegistered() {
            return registered;
        }

        public FilterInputStreamCache getCache() {
            return cache;
        }
    }
}
