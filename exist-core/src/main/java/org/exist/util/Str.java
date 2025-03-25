/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.util;

import javax.annotation.Nullable;
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

    private final static StrCache<String> strCache = new StrCache<>();

    public final static Str EMPTY = Str.of("");
    public final static Str WILDCARD = Str.of("*");

    public static String makeString(final Str str) {
        if (str == null) {
            return null;
        }
        return str.toString();
    }

    /**
     * Create a Str while guaranteeing that it is `intern()`-ed
     * i.e. that equal Str values share an implementation object.
     *
     * @param s sequence to store as a Str
     * @return the unique Str to wrap this sequence.
     */
    public static Str of(final String s) {
        if (s == null) {
            return null;
        } else {
            return strCache.insertIfAbsent(s);
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

    public static String dumpCache() {
        return strCache.dump();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Str str = (Str) o;

        return index == str.index;
    }

    /**
     * Test if two Str are equal whilst handling null references.
     *
     * @param s1 the first Str.
     * @param s2 the second Str.
     *
     * @return true of the Str are equal or both are null, false otherwise.
     */
    public static boolean equals(@Nullable final Str s1, @Nullable final Str s2) {
        if (s1 == s2) {
            return true;
        }

        if (s1 == null) {
            return false;
        }

        return s1.equals(s2);
    }

    @Override
    public int hashCode() {
        return index;
    }

    static int cacheEstimate() {
        return strCache.generationCount();
    }

    static void resetGeneration() {
        strCache.resetGeneration();
    }

    /**
     * Cached string -> index mapping.
     */
    private static class StrCache<T extends Comparable<T> & CharSequence> {

        final Map<T, Str> entryMap = new ConcurrentSkipListMap<>(T::compareTo);

        // When we clear the cache (which we anyway should only do for testing)
        // move entryStart up to entryEnd, so that all previous Str indices are
        // known to be invalid.
        final ArrayList<T> entries = new ArrayList<>();
        int generationStart = 0;
        int end = 0;

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
                pos = end++;
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

        final int generationCount() {
            return end - generationStart;
        }

        /**
         * Only intended for test use
         */
        private synchronized void resetGeneration() {
            generationStart = end;
        }

        final synchronized String dump() {
            StringBuilder sb = new StringBuilder("[");
            sb.append('(').append(end).append(')');
            for (int i = 0; i < end; i++) {
                T val = entries.get(i);
                sb.append(i).append(":|").append(val);
                Str mapped = entryMap.get(val);
                sb.append("|=").append('|').append(mapped == null ? "<<null>>" : mapped).append("|\n");
            }
            sb.append(']');

            return sb.toString();
        }
    }

    public static class XMLConstants {
        public final static Str DEFAULT_NS_PREFIX = Str.of(javax.xml.XMLConstants.DEFAULT_NS_PREFIX);
        public final static Str XML_NS_PREFIX = Str.of(javax.xml.XMLConstants.XML_NS_PREFIX);
        public final static Str XMLNS_ATTRIBUTE = Str.of(javax.xml.XMLConstants.XMLNS_ATTRIBUTE);
        public final static Str NULL_NS_URI = Str.of(javax.xml.XMLConstants.NULL_NS_URI);

    }
}
