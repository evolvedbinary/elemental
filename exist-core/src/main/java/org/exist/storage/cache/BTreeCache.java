/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 *
 * NOTE: Parts of this file contain code from The eXist-db Authors.
 *       The original license header is included below.
 *
 * ----------------------------------------------------------------------------
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
package org.exist.storage.cache;

import net.jcip.annotations.NotThreadSafe;
import org.exist.storage.CacheManager;

/**
 * This cache implementation always tries to keep the inner btree pages in
 * cache, while the leaf pages can be removed.
 */
@NotThreadSafe
public class BTreeCache<T extends BTreeCacheable> implements Cache<T> {

    private final BTreeCacheLevel<T> innerPageCache;
    private final BTreeCacheLevel<T> outerPageCache;

    private final String name;

    /**
     * Create a new BTree cache
     *
     * TODO (AP) the size is misleading, because inner AND outer caches of the supplied size are created.
     * We should spread the sizes. Is there a good rule of thumb for this ?
     *
     * @param name
     * @param size
     * @param growthFactor
     * @param growthThreshold
     * @param type
     */
    public BTreeCache(final String name, final int size, final double growthFactor, final double growthThreshold, final CacheType type) {
        this.name = name;
        this.innerPageCache = new BTreeCacheLevel<>(name + "_inner", size, growthFactor, growthThreshold, type);
        this.outerPageCache = new BTreeCacheLevel<>(name + "_outer", size, growthFactor, growthThreshold, type);
    }

    @Override
    public void add(final T item, final int initialRefCount) {
        add(item);
    }

    @Override
    public T get(final T item) {
        if (item.isInnerPage()) {
            return innerPageCache.get(item);
        } else {
            return outerPageCache.get(item);
        }
    }

    @Override
    public T get(final long key) {
        T result = innerPageCache.get(key);
        if (result == null) {
            result = outerPageCache.get(key);
        }
        return result;
    }

    @Override
    public void remove(final T item) {
        if (item.isInnerPage()) {
            innerPageCache.remove(item);
        } else {
            outerPageCache.remove(item);
        }
    }

    @Override
    public boolean hasDirtyItems() {
        return innerPageCache.hasDirtyItems() || outerPageCache.hasDirtyItems();
    }

    @Override
    public boolean flush() {
        final boolean inner = innerPageCache.flush();
        final boolean outer = outerPageCache.flush();
        return inner && outer;
    }

    @Override
    public int getBuffers() {
        return innerPageCache.getBuffers() + outerPageCache.getBuffers();
    }

    @Override
    public double getGrowthFactor() {
        return innerPageCache.getGrowthFactor();  // NOTE(AP) same as outerPageCache
    }

    @Override
    public void resize(final int newSize) {
        innerPageCache.resize(newSize);
        outerPageCache.resize(newSize);
    }

    @Override
    public void setCacheManager(final CacheManager manager) {
        innerPageCache.setCacheManager(manager);
        outerPageCache.setCacheManager(manager);
    }

    @Override
    public int getUsedBuffers() {
        return innerPageCache.getUsedBuffers() + outerPageCache.getUsedBuffers();
    }

    @Override
    public int getHits() {
        return innerPageCache.getHits() + outerPageCache.getHits();
    }

    @Override
    public int getFails() {
        return innerPageCache.getFails() + outerPageCache.getFails();
    }

    @Override
    public int getLoad() {
        return innerPageCache.getLoad() + outerPageCache.getLoad();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CacheType getType() {
        return innerPageCache.getType();  // NOTE(AP) same as outerPageCache
    }

    @Override
    public void add(final T item) {
        if (item.isInnerPage()) {
            innerPageCache.add(item);
        } else {
            outerPageCache.add(item);
        }
    }
}
