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
package org.exist.xquery.functions.integer;

import org.exist.xquery.XPathException;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Format numbers according to rule 9 (any other numbering sequence)
 * {@see https://www.w3.org/TR/xpath-functions-31/#formatting-integers}
 */
class NumberingPicture extends IntegerPicture {

    // Set up the code point ranges we accept as numberings.
    private static final Map<Integer, Integer> rangesForCodePoint = new HashMap<>();

    static {
        NumberingPicture.range(0x391, 0x3A9);
        NumberingPicture.range(0x3B1, 0x3C9);
        NumberingPicture.range('①', '⑳');
        NumberingPicture.range('⑴', '⒇');
        NumberingPicture.range('⒈', '⒛');
    }

    private final int indexCodePoint;
    private final int limitForRange;
    private final IntegerPicture defaultPicture;
    private final FormatModifier formatModifier;

    /**
     * Define a range using characters
     *
     * @param from first item in range (1)
     * @param to   last item in range (inclusive)
     */
    private static void range(final char from, final char to) {
        final char[] fromChars = {from};
        final char[] toChars = {to};
        NumberingPicture.rangesForCodePoint.put(Character.codePointAt(fromChars, 0), Character.codePointAt(toChars, 0));
    }

    /**
     * Define a range using code points
     *
     * @param from first item in range (1)
     * @param to   last item in range (inclusive)
     */
    private static void range(final int from, final int to) {
        NumberingPicture.rangesForCodePoint.put(from, to);
    }

    private NumberingPicture(final int indexCodePoint, final int limitForRange, final FormatModifier formatModifier) throws XPathException {
        this.indexCodePoint = indexCodePoint;
        this.limitForRange = limitForRange;
        this.defaultPicture = IntegerPicture.defaultPictureWithModifier(formatModifier);
        this.formatModifier = formatModifier;
    }

    /**
     * Try to create a numbering picture from an index code point
     * Check that the code point is in a known range,
     * and if it is then return the numbering picture for that range.
     * <p>
     * Otherwise,
     *
     * @param indexCodePoint codePoint which may be part of a numbering range
     * @param formatModifier format modifier to be used by a generated formatter
     * @return a numbering picture if the code point is in range, otherwise {@link Optional#empty()} }
     * @throws XPathException if the picture cannot be created
     */
    public static Optional<IntegerPicture> fromIndexCodePoint(final int indexCodePoint, final FormatModifier formatModifier) throws XPathException {
        if (!NumberingPicture.rangesForCodePoint.containsKey(indexCodePoint)) {
            return Optional.empty();
        }
        final int limitForRange = NumberingPicture.rangesForCodePoint.get(indexCodePoint);
        return Optional.of(new NumberingPicture(indexCodePoint, limitForRange, formatModifier));
    }

    /**
     * Format according to a numbering
     *
     * @param bigInteger the integer to format
     * @param locale     of the language to use in formatting
     * @return the formatted string output
     * @throws XPathException if something went wrong
     */
    @Override
    public String formatInteger(final BigInteger bigInteger, final Locale locale) throws XPathException {
        //spec says out of range should be formatted by "1"
        if (bigInteger.compareTo(BigInteger.valueOf(1)) < 0 || bigInteger.compareTo(BigInteger.valueOf(limitForRange - indexCodePoint + 1L)) > 0) {
            return defaultPicture.formatInteger(bigInteger, locale);
        }

        final StringBuilder result = new StringBuilder();
        result.append(IntegerPicture.fromCodePoint(bigInteger.intValue() + indexCodePoint - 1));
        if (formatModifier.numbering == FormatModifier.Numbering.ORDINAL && bigInteger.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0 && bigInteger.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0) {
            result.append(IntegerPicture.ordinalSuffix(bigInteger.intValue(), locale));
        }
        return result.toString();
    }
}
