/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
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
package org.exist.util;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Utilities for working with String.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class StringUtil {

    /**
     * Determines if a String is null or empty.
     *
     * @param string the string to test.
     *
     * @return true if the String is null or empty.
     */
    public static boolean isNullOrEmpty(@Nullable final String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Returns the input String if it is null or empty.
     * otherwise returns the provided default String.
     *
     * @param string the string to test.
     * @param defaultString the default string if the test fails
     *
     * @return the String if it is null or empty, otherwise the default String.
     */
    public static @Nullable String isNullOrEmpty(@Nullable final String string, @Nullable final String defaultString) {
        return isNullOrEmpty(string) ? string : defaultString;
    }

    /**
     * Determines if a String is not null, and is not empty.
     *
     * @param string the string to test.
     *
     * @return true if the String is not null, and is not empty.
     */
    public static boolean notNullOrEmpty(@Nullable final String string) {
        return string != null && !string.isEmpty();
    }

    /**
     * Returns the input String if it is not null, and is not empty,
     * otherwise returns the provided default String.
     *
     * @param string the string to test.
     * @param defaultString the default string if the test fails
     *
     * @return the String if it is not null, and is not empty, otherwise the default String.
     */
    public static @Nullable String notNullOrEmpty(@Nullable final String string, @Nullable final String defaultString) {
        return notNullOrEmpty(string) ? string : defaultString;
    }

    /**
     * Returns an Optional of the input String if it is not null, and is not empty, otherwise {@link Optional#empty()}.
     *
     * @param string the string to test.
     *
     * @return an Optional of the String if it is not null, and is not empty, otherwise {@link Optional#empty()}.
     */
    public static Optional<String> notNullOrEmptyOptional(@Nullable final String string) {
        if (string == null || string.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(string);
    }

    /**
     * Determines if a String is null, empty, or whitespace only.
     *
     * @param string the string to test.
     *
     * @return true if the String is null, empty, or whitespace only.
     */
    public static boolean isNullOrEmptyOrWs(@Nullable final String string) {
        return string == null || string.trim().isEmpty();
    }

    /**
     * Returns the input String if it is null, empty, or is whitespace only,
     * otherwise returns the provided default String.
     *
     * @param string the string to test.
     * @param defaultString the default string if the test fails
     *
     * @return the String if it is null, is empty, or is whitespace only, otherwise the default String.
     */
    public static @Nullable String isNullOrEmptyOrWs(@Nullable final String string, @Nullable final String defaultString) {
        return isNullOrEmptyOrWs(string) ? string : defaultString;
    }

    /**
     * Determines if a String is not null, is not empty, and is not whitespace only.
     *
     * @param string the string to test.
     *
     * @return true if the String is not null, is not empty, and is not whitespace only.
     */
    public static boolean notNullOrEmptyOrWs(@Nullable final String string) {
        return string != null && !string.trim().isEmpty();
    }

    /**
     * Returns the input String if it is not null, is not empty, and is not whitespace only,
     * otherwise returns the provided default String.
     *
     * @param string the string to test.
     * @param defaultString the default string if the test fails
     *
     * @return the String if it is not null, is not empty, and is not whitespace only, otherwise the default String.
     */
    public static @Nullable String notNullOrEmptyOrWs(@Nullable final String string, @Nullable final String defaultString) {
        return notNullOrEmptyOrWs(string) ? string : defaultString;
    }

    /**
     * Returns an Optional of the input String if it is not null, is not empty, and is not whitespace only, otherwise {@link Optional#empty()}.
     *
     * @param string the string to test.
     *
     * @return an Optional of the String if it is not null, is not empty, and is not whitespace only, otherwise {@link Optional#empty()}.
     */
    public static Optional<String> notNullOrEmptyOrWsOptional(@Nullable final String string) {
        if (string == null || string.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(string);
    }

    /**
     * Returns null if the string is empty or null, otherwise returns the string.
     *
     * @param string the string to test.
     *
     * @return null if the string is empty or null, otherwise returns the string.
     */
    public static @Nullable String nullIfEmpty(@Nullable final String string) {
        return notNullOrEmpty(string, null);
    }

    /**
     * Capitalizes the first character of a string.
     *
     * @param string the string to capitalize.
     *
     * @return the capitalized string.
     */
    public static @Nullable String capitalize(@Nullable final String string) {
        if (string == null) {
            return null;
        }

        final int firstCharCodePoint = string.codePointAt(0);
        final int capitalizedFirstCharCodePoint = Character.toTitleCase(firstCharCodePoint);
        if (firstCharCodePoint == capitalizedFirstCharCodePoint) {
            // already capitalized
            return string;
        }

        final int charsLen = string.length();
        final int[] newCodePoints = new int[charsLen];
        int newCodePointOffset = 0;
        newCodePoints[newCodePointOffset++] = capitalizedFirstCharCodePoint; // copy the first code point
        for (int existingCodePointOffset = Character.charCount(capitalizedFirstCharCodePoint); existingCodePointOffset < charsLen; ) {
            final int existingCodePoint = string.codePointAt(existingCodePointOffset);
            newCodePoints[newCodePointOffset++] = existingCodePoint; // copy the remaining ones
            existingCodePointOffset += Character.charCount(existingCodePoint);
        }

        return new String(newCodePoints, 0, newCodePointOffset);
    }

    /**
     * Returns true if the string starts with the prefix.
     *
     * @param string the string to test.
     * @param prefix the prefix to look for.
     *
     * @return true if the string starts with the prefix, false otherwise.
     */
    public static boolean startsWith(@Nullable final String string, @Nullable final String prefix) {
        if (string == null || prefix == null) {
            return false;
        }

        return string.startsWith(prefix);
    }

    /**
     * Returns true if the string starts with any one of the prefixes.
     *
     * @param string the string to test.
     * @param prefixes the prefixes to look for.
     *
     * @return true if the string starts with any one of the prefixes, false otherwise.
     */
    public static boolean startsWith(@Nullable final String string, @Nullable final String[] prefixes) {
        if (string == null || prefixes == null) {
            return false;
        }

        for (final String prefix : prefixes) {
            if (prefix != null && string.startsWith(prefix)) {
                    return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the string ends with the prefix.
     *
     * @param string the string to test.
     * @param prefix the prefix to look for.
     *
     * @return true if the string ends with the prefix, false otherwise.
     */
    public static boolean endsWith(@Nullable final String string, @Nullable final String prefix) {
        if (string == null || prefix == null) {
            return false;
        }

        return string.endsWith(prefix);
    }

    /**
     * Returns true if the string ends with any one of the prefixes.
     *
     * @param string the string to test.
     * @param prefixes the prefixes to look for.
     *
     * @return true if the string ends with any one of the prefixes, false otherwise.
     */
    public static boolean endsWith(@Nullable final String string, @Nullable final String[] prefixes) {
        if (string == null || prefixes == null) {
            return false;
        }

        for (final String prefix : prefixes) {
            if (prefix != null && string.endsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the substring before the last occurrence of a match.
     *
     * @param string the string to test
     * @param match the match to search for
     *
     * @return the substring before the last match, or if no match, the entire string.
     */
    public static @Nullable String substringBeforeLast(@Nullable final String string, @Nullable final String match) {
        if (isNullOrEmpty(string) || isNullOrEmpty(match)) {
            return string;
        }

        final int idx = string.lastIndexOf(match);
        if (idx == -1) {
            return string;
        }

        return string.substring(0, idx);
    }

    /**
     * Test if two strings are equal whilst handling null references.
     *
     * @param s1 the first string.
     * @param s2 the second string.
     *
     * @return true of the strings are equal or both are null, false otherwise.
     */
    public static boolean equals(@Nullable final String s1, @Nullable final String s2) {
        if (s1 == s2) {
            return true;
        }

        if (s1 == null) {
            return false;
        }

        return s1.equals(s2);
    }
}
