/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.util;

import io.lacuna.bifurcan.Rope;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Always-interned fast string utility.
 */
public final class Str implements Comparable<Str> {

    private final int index;
    private final boolean isEmpty;
    private Str(final int i, final boolean isEmpty) {
        this.index = i;
        this.isEmpty = isEmpty;
    }

    /**
     * Create a Str while guaranteeing that it is `intern()`-ed
     * i.e. that equal Str values share an implementation object.
     *
     * @param s sequence to store as a Str
     * @return the unique Str to wrap this sequence.
     */
    public static Str of(final CharSequence s) {
        if (s == null) {
            return null;
        } else {
            return strCache.insertIfAbsent(String.valueOf(s));
        }
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    /**
     * Helper for methods we want to provide directly on the Str
     *
     * @return the underlying rope
     */
    private String item() {
        return strCache.at(index);
    }

    public CharSequence toCharSequence() {
        return item();
    }

    public String toString() {
        return item();
    }

    public int compareTo(final Str o) {
        return item().compareTo(o.item());
    }

    public int length() {
        return item().length();
    }

    private final static StrCache<String> strCache = new StrCache<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Str str = (Str) o;

        return index == str.index;
    }

    @Override
    public int hashCode() {
        return index;
    }

    static int cacheEstimate() {
        return strCache.entryCount;
    }

    static void resetCache() {
        strCache.clear();
    }

    /**
     * Cached string -> index mapping.
     */
    private static class StrCache<T extends Comparable<T> & CharSequence> {

        final Map<T, Str> entryMap = new ConcurrentSkipListMap<>(T::compareTo);

        final ArrayList<T> entries = new ArrayList<>();
        int entryCount = 0;

        /**
         * insert a representation; assumes it did not exist in the table before
         * synchronize recording the position with adding the element;
         * because adding a new element is exceptional, this synchronization
         * should not be a performance problem.
         *
         * @param key the rope to insert
         * @return the wrapped Str of the rope
         */
        final Str insert(final T key) {
            int pos;
            synchronized (this) {
                pos = entryCount++;
                entries.add(key);
            }
            return new Str(pos, key.isEmpty());
        }

        final Str insertIfAbsent(final T key) {
            return entryMap.computeIfAbsent(key, this::insert);
        }

        final T at(final int index) {
            return entries.get(index);
        }

        /**
         * Only intended for test use
         */
        private synchronized void clear() {
            entries.clear();
            entryMap.clear();
            entryCount = 0;
        }
    }
}
