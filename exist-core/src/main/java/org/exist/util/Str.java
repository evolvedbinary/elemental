/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.util;

import javax.annotation.Nullable;

/**
 * "Fast" string which uses "fingerprinting"
 * to make both equality comparison, and string ordering efficient for
 * shorter/simpler strings.
 * <p>
 * The tactic is to create a fingerprint (a {@code long}, i.e. 64 bits) for a string.
 * The fingerprint holds
 * - a prefix (the first 8 bits of the first 6 characters)
 * - a length (0...254, or 255 representing length >= 255).
 * - bitwise or of the top 8 bits of the first 6 characters.
 * <p>
 * Specifically
 * - byte 7 - or together the msb bits of the first n characters
 * - byte 6 - min(length of string, 255)
 * - bytes 5...0 - least significant 8 bits of the first 6 characters; 0-padded
 * <p>
 * This means that when byte 7 is 0, we can mask out the top 16 bits of the fingerprint
 * and compare the remainder to provide a string hint as to which of a pair of {@code Str} precedes the other.
 * We can also definitively declare two {@code Strs} to be equal when bytes 5...0 are equal, byte 7 is 0, and
 * lengths are equal, and no longer than 6.
 */
public final class Str implements Comparable<Str> {

    private final long fingerprint;
    private final String value;
    private Str(final long fingerprint, final String value) {
        this.fingerprint = fingerprint;
        this.value = value;
    }

    public final static Str EMPTY = Str.of("");
    public final static Str WILDCARD = Str.of("*");

    public static String makeString(final Str str) {
        if (str == null) {
            return null;
        }
        return str.toString();
    }

    /**
     * Yield a Str for the supplied String
     * Efficiently cache it so that mostly the same String yields the same Str
     *
     * @param s sequence to store as a Str
     * @return a Str that wraps this String.
     */
    public static Str of(final String s) {
        if (s == null) {
            return null;
        } else {
            return Str.from(s);
        }
    }

    /**
     * Helper to create a str object for an uncached String
     * @param s String to cache
     * @return a {@link Str} for the string
     */
    private static Str from(final String s) {
        final long fingerprint = fingerprint(s);
        return new Str(fingerprint, s);
    }

    /**
     * TODO (AP) efficient way is to use the fingerprint,
     * which should encode the length
     *
     * @return true iff the string is empty
     */
    public boolean isEmpty() {
        return ((fingerprint & LENGTH_MASK) == 0L);
    }

    public CharSequence toCharSequence() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * TODO (AP) use the fingerprint
     *
     * @param o the object to be compared.
     * @return -1, 0, 1 as this <, =, > {@code o}
     */
    @Override
    public int compareTo(final Str o) {

        if (lexicographicPrefix() && o.lexicographicPrefix()) {
            long left = fingerprint & PREFIX_MASK;
            long right = o.fingerprint & PREFIX_MASK;
            if (left < right) return -1;
            if (left > right) return 1;
            if (shortSimpleString() && o.shortSimpleString()) {
                return 0;
            }
        }

        // revert to actual strings.
        return value.compareTo(o.value);
    }

    /**
     * @return true iff the string is entirely encoded in the fingerprint (and no high bits are set)
     */
    private boolean shortSimpleString() {
        return (fingerprint >>> LEN_SHIFT) <= MAX_FINGERPRINT_ENCODED_LEN;
    }

    /**
     * @return true iff the string prefix can be used for comparison shortcut (no high bits in the prefix)
     */
    private boolean lexicographicPrefix() {
        return (fingerprint >>> LEN_SHIFT) <= 0xff;
    }

    /**
     * Fetch the length of this {@code Str}
     *
     * The exact length is encoded in the fingerprint if length < 255.
     *
     * @return the length of the string which this @{Str represents}
     */
    public int length() {
        long lsb = (fingerprint >>> LEN_SHIFT) & 0xff;
        if (lsb < 0xff) {
            return (int) lsb;
        }
        return value.length();
    }

    // Configuration for masking
    private final static int LEN_SHIFT = 48;
    private final static int MAX_FINGERPRINT_ENCODED_LEN = 6;

    private final static long PREFIX_MASK = 0xffffffffffffL;

    private final static long LENGTH_MASK = 0xffL << LEN_SHIFT;

    /**
     * Create a fingerprint for a string, to make comparison more efficient
     * <p>
     * @param s string to generate fingerprint of
     * @return the fingerprint of the supplied string
     */
    static long fingerprint(String s) {

        long result = 0L;

        final int len = Math.min(s.length(), 0xff);
        short msbBits = 0;

        // add as many character lower bytes as fit, leaving space for byte 6, byte 7
        for (int i = 0; i < Math.min(len, Long.BYTES - 2); i++) {
            //lower byte of the character, which we expect is usually the more variable byte
            final char c = s.charAt(i);
            result = (result << 8) | (byte)c;
            msbBits |= (short)c;
        }

        // pad with 0s to make compareTo correct
        // we want abc000 vs abcd00
        for (int i = len; i < Long.BYTES - 2; i++) {
            result = (result << 8) | (byte)0;
        }

        // patch in length at byte 6, and msbBits at byte 7
        result |= ((msbBits & 0xff00L) | len) << LEN_SHIFT;

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Str str = (Str) o;

        // distinct fingerprints can never be equal
        if (fingerprint != str.fingerprint) return false;

        // fingerprint for short strings can be definitive
        // if short enough that all characters are encoded
        // and none of the characters have a high bit set
        if (shortSimpleString()) {
            return true;
        }

        // fall back to comparing the strings
        return value.equals(str.value);
    }

    public static boolean equals(@Nullable final Str s1, @Nullable final Str s2) {
        if (s1 == s2) {
            return true;
        }

        if (s1 == null) {
            return false;
        }

        return s1.equals(s2);
    }

    /**
     * Hashcode does not need to look at the string (for efficiency)
     *
     * @return a hash calculation based solely on the fingerprint
     */
    @Override
    public int hashCode() {
        // fingerprint bytes
        // 7 or of top 8 bits of each char
        // 6 length up to 255
        // 5 char 0 (bottom 8 bits)
        // 4 char 1
        // 3 char 2
        // 2 char 3
        // 1 char 4
        // 0 char 5
        // >>> 40 gets char 0 into the least bit of the hash, length above it
        // >>> 20 gets char 1 into the middle of the hash
        return (int) (fingerprint ^ (fingerprint >>> 40) ^ (fingerprint >>> 20));
    }


    /**
     * Cached string -> index mapping.
     */
    public static class XMLConstants {
        public final static Str DEFAULT_NS_PREFIX = Str.of(javax.xml.XMLConstants.DEFAULT_NS_PREFIX);
        public final static Str XML_NS_PREFIX = Str.of(javax.xml.XMLConstants.XML_NS_PREFIX);
        public final static Str XMLNS_ATTRIBUTE = Str.of(javax.xml.XMLConstants.XMLNS_ATTRIBUTE);
        public final static Str NULL_NS_URI = Str.of(javax.xml.XMLConstants.NULL_NS_URI);

    }
}
