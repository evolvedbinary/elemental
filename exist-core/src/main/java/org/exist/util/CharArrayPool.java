/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.util;

import net.jcip.annotations.ThreadSafe;

import javax.annotation.Nullable;

/**
 * A pool for char arrays.
 *
 * This pool is used by class XMLString. Whenever an XMLString needs to
 * reallocate the backing char[], the old array is released into the pool. However,
 * only char[] with length &lt; MAX are kept in the pool. Larger char[] are rarely reused.
 *
 * The pool is bound to the current thread.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>.
 */
@ThreadSafe
public class CharArrayPool {

    private static class PoolData {
        int maxIndex = -1;
        int lastGetIndex = -1;
        int lastReleaseIndex = -1;
        final char[][] pool = new char[POOL_SIZE][];
    }

    private static final int POOL_SIZE = 128;
    private static final int MIN_CHAR_ARRAY_SIZE = 8;
    private static final int MAX_CHAR_ARRAY_SIZE = 128;

    private static final ThreadLocal<PoolData> POOLS = ThreadLocal.withInitial(PoolData::new);

    private CharArrayPool() {
    }

    public static char[] getCharArray(int minSize) {
        if (minSize > MAX_CHAR_ARRAY_SIZE) {
            // the requested minSize is over the size that is permitted in the pool, so just return a new char[] array from outside the pool
            return new char[minSize];
        }

        // ensure the minSize is at least MIN_CHAR_ARRAY_SIZE
        minSize = Math.max(MIN_CHAR_ARRAY_SIZE, minSize);

        @Nullable char[] array = null;
        final PoolData localPoolData = POOLS.get();

        // Get the array last released to the pool if it is of minSize (as it is likely that get/release is called multiple times for similar size strings)
        if (localPoolData.lastReleaseIndex != -1) {
            array = localPoolData.pool[localPoolData.lastReleaseIndex];
            if (array.length >= minSize) {
                localPoolData.pool[localPoolData.lastReleaseIndex] = null;    // remove array from pool
                localPoolData.lastGetIndex = localPoolData.lastReleaseIndex;  // record the index of this get
                localPoolData.lastReleaseIndex = -1;                          // mark lastReleaseIndex as invalid
                return array;
            }
        }

        // Attempt to find an array (of minSize) in the pool by reading from right-to-left (as items recently returned are more likely to be on the right)
        for (int i = localPoolData.maxIndex; i > -1; i--) {
            array = localPoolData.pool[i];
            if (array != null && array.length >= minSize) {
                localPoolData.pool[i] = null;    // remove array from pool
                localPoolData.lastGetIndex = i;  // record the index of this get
                return array;
            }
        }

        // not found in the pool, so just return a new char[] array from outside the pool
        return new char[minSize];
    }

    public static void releaseCharArray(@Nullable final char[] array) {
        if (array == null || array.length > MAX_CHAR_ARRAY_SIZE) {
            // the array is over the size that is permitted in the pool, so do nothing as it should not be placed in the pool
            return;
        }

        final PoolData localPoolData = POOLS.get();

        // If the pool is empty, place the array at the start of the pool
        if (localPoolData.maxIndex == -1) {
            localPoolData.pool[0] = array;
            localPoolData.lastReleaseIndex = 0;  // record the index of this release
            localPoolData.maxIndex = 0;
            return;
        }

        // Get the index of the array last get'ed from the pool (as it is likely that get/release is called multiple times for similar size strings)
        if (localPoolData.lastGetIndex != -1) {
            localPoolData.pool[localPoolData.lastGetIndex] = array;                                     // place array in the pool
            localPoolData.lastReleaseIndex = localPoolData.lastGetIndex;                                // record the index of this release
            localPoolData.lastGetIndex = -1;                                                            // mark lastGetIndex as invalid
            localPoolData.maxIndex = Math.max(localPoolData.maxIndex, localPoolData.lastReleaseIndex);  // update the maxIndex
            return;
        }

        // Attempt to find an empty space in the pool by reading from right-to-left (as items recently released are more likely to be on the right)
        for (int i = localPoolData.maxIndex; i > -1; i--) {
            if (localPoolData.pool[i] == null) {
                localPoolData.pool[i] = array;       // place array in the pool
                localPoolData.lastReleaseIndex = i;  // record the index of this release

                //TODO(AR) if we were to record the right-most not-null entry, we could potentially shrink the maxIndex of the pool here, which would reduce the search space in the next lookup(s)

                return;
            }
        }

        // couldn't find an existing space in the pool, if there is remaining space then append it
        if (localPoolData.maxIndex < POOL_SIZE - 1) {
            localPoolData.pool[++localPoolData.maxIndex] = array;     // place array in the pool
            localPoolData.lastReleaseIndex = localPoolData.maxIndex;  // record the index of this release
            return;
        }

        // NOTE(AR): If we reach here, the pool is at maximum capacity and is full, we might in future want to record stats about this so we could configure a larger/smaller pool?

     }
}
