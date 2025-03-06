/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.util;

import io.lacuna.bifurcan.Rope;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Always-cache fast string utility.
 */
public class Str {

    private final int index;
    private final boolean isEmpty;

    private Str(final int i, final boolean isEmpty) {
        index = i;
        this.isEmpty = isEmpty;
    }

    /**
     * Create a Str while guaranteeing that it is `intern()`-ed
     * i.e. that equal Str values share an implementation object.
     *
     * @param s
     * @return the unique Str to wrap this sequence.
     */
    public static Str of(final CharSequence s) {
        return strCache.insertIfAbsent(Rope.from(s));
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     * Helper for methods we want to provide directly on the Str
     *
     * @return the underlying rope
     */
    private Rope rope() {
        return strCache.at(index);
    }

    public CharSequence toCharSequence() {
        return rope().toCharSequence();
    }

    public String toString() {
        return rope().toString();
    }

    public int compareTo(final Str o) {
        return rope().compareTo(o.rope());
    }

    public int length() {
        return rope().size();
    }

    private final static StrCache strCache = new StrCache();

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

    /**
     * Cached string -> index mapping.
     */
    private static class StrCache {

        final Map<Rope, Str> entryMap = new ConcurrentSkipListMap<>(Rope::compareTo);

        final ArrayList<Rope> entries = new ArrayList<>();
        int entryCount = 0;

        /**
         * insert a rope; assumes it did not exist in the table before
         * synchronize recording the position with adding the element;
         * because adding a new element is exceptional, this synchronization
         * should not be a performance problem.
         *
         * @param key the rope to insert
         * @return the wrapped Str of the rope
         */
        final Str insert(final Rope key) {
            int pos;
            synchronized (this) {
                pos = entryCount++;
                entries.add(key);
            }
            return new Str(pos, key.size() == 0);
        }

        final Str insertIfAbsent(final Rope key) {
            return entryMap.computeIfAbsent(key, this::insert);
        }

        final Rope at(final int index) {
            return entries.get(index);
        }
    }
}
