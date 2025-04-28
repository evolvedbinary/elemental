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
import java.util.Locale;

/**
 * Format as sequence according to rule 2/3 (alphabetic digits)
 * {@see https://www.w3.org/TR/xpath-functions-31/#formatting-integers}
 */
class SequenceIntegerPicture extends IntegerPicture {

    private static final BigInteger RADIX = BigInteger.valueOf(26L);

    private final int codePoint;

    SequenceIntegerPicture(final int codePoint) {
        this.codePoint = codePoint;
    }

    /**
     * Format with a sequence as digits
     *
     * @param bigInteger the integer to format
     * @param locale     of the language to use in formatting
     * @return the formatted string
     * @throws XPathException if something went wrong
     */
    @Override
    public String formatInteger(final BigInteger bigInteger, final Locale locale) throws XPathException {
        //spec says out of range should be formatted by "1"
        if (bigInteger.compareTo(BigInteger.ZERO) <= 0) {
            return IntegerPicture.defaultPictureWithModifier(new FormatModifier("")).formatInteger(bigInteger, locale);
        }

        final StringBuilder sb = new StringBuilder();
        BigInteger acc = bigInteger;
        do {
            final BigInteger[] divideAndRemainder = acc.subtract(BigInteger.ONE).divideAndRemainder(SequenceIntegerPicture.RADIX);
            sb.append(IntegerPicture.fromCodePoint(codePoint + divideAndRemainder[1].intValue()));
            acc = divideAndRemainder[0];
        } while (acc.compareTo(BigInteger.ZERO) > 0);

        return sb.reverse().toString();
    }
}
