/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.storage.cache;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.jcip.annotations.NotThreadSafe;

import java.util.Iterator;

/**
 * A level of a BTree cache - either inner nodes, or outer nodes
 */
@NotThreadSafe
public class BTreeCacheLevel<T extends BTreeCacheable> extends LRUCache<T> {

    public BTreeCacheLevel(final String name, final int size, final double growthFactor, final double growthThreshold, final CacheType type) {
        super(name, size, growthFactor, growthThreshold, type);
    }

    @Override
    public void add(final T item) {
        map.put(item.getKey(), item);
        if (map.size() >= max + 1) {
            removeNext(item);
        }
    }

    private void removeNext(final T item) {
        final Iterator<Long2ObjectMap.Entry<T>> iterator = map.fastEntrySetIterator();
        while (iterator.hasNext()) {
            final Long2ObjectMap.Entry<T> next = iterator.next();
            final T cached = next.getValue();
            if (cached.allowUnload() && cached.getKey() != item.getKey()) {
                cached.sync(true);
                map.remove(next.getLongKey());

                // Continue if we are still oversize,
                // which might be possible if all entries were !allowUnload on a previous loop
                if (map.size() <= max) break;
            }
        }

        accounting.replacedPage(item);
        if (growthFactor > 1.0 && accounting.resizeNeeded()) {
            cacheManager.requestMem(this);
        }
    }
}
