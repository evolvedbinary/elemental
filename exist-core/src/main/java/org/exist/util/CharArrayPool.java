/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.util;

import net.jcip.annotations.ThreadSafe;

/**
 * A pool for char arrays.
 *
 * This pool is used by class XMLString. Whenever an XMLString needs to
 * reallocate the backing char[], the old array is released into the pool. However,
 * only char[] with length &lt; MAX are kept in the pool. Larger char[] are rarely reused.
 *
 * The pool is bound to the current thread.
 */
@ThreadSafe
public class CharArrayPool {

    private static final int POOL_SIZE = 128;
    private static final int MIN_SIZE = 8;

    private static final ThreadLocal<PoolContent> POOLS = ThreadLocal.withInitial(PoolContent::new);

    private CharArrayPool() {
    }

    public static char[] getCharArray(final int size) {
        final PoolContent poolContent = POOLS.get();
        if (POOL_SIZE > size) {
            final int alloc = Math.max(size, MIN_SIZE);
            final char[][] pool = poolContent.pool;
            if (pool[alloc] != null) {
                final char[] b = POOLS.get().pool[alloc];
                pool[alloc] = null;
                return b;
            }
        }
        return new char[Math.max(size, MIN_SIZE)];
    }

    public static void releaseCharArray(final char[] b) {
        if (b == null || b.length > POOL_SIZE) {
            return;
        }
        final PoolContent poolContent = POOLS.get();
        poolContent.pool[b.length] = b;
     }

    private static class PoolContent {
        final char[][] pool = new char[POOL_SIZE][];
    }

}
