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
package org.exist.xquery;

/**
 * Data class for a Decimal Format.
 *
 * See https://www.w3.org/TR/xpath-31/#dt-static-decimal-formats
 *
 * NOTE: UTF-16 characters are stored as code-points!
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class DecimalFormat {

    public static final DecimalFormat UNNAMED = new DecimalFormat(
            '.',
            'e',
            ',',
            '%',
            '\u2030',
            '0',
            '#',
            ';',
            "Infinity",
            "NaN",
            '-'
    );


    // used both in the picture string, and in the formatted number
    public final int decimalSeparator;
    public final int exponentSeparator;
    public final int groupingSeparator;
    public final int percent;
    public final int perMille;
    public final int zeroDigit;

    // used in the picture string
    public final int digit;
    public final int patternSeparator;

    //used in the result of formatting the number, but not in the picture string
    public final String infinity;
    public final String NaN;
    public final int minusSign;

    public DecimalFormat(final int decimalSeparator, final int exponentSeparator, final int groupingSeparator,
            final int percent, final int perMille, final int zeroDigit, final int digit,
            final int patternSeparator, final String infinity, final String NaN, final int minusSign) {
        this.decimalSeparator = decimalSeparator;
        this.exponentSeparator = exponentSeparator;
        this.groupingSeparator = groupingSeparator;
        this.percent = percent;
        this.perMille = perMille;
        this.zeroDigit = zeroDigit;
        this.digit = digit;
        this.patternSeparator = patternSeparator;
        this.infinity = infinity;
        this.NaN = NaN;
        this.minusSign = minusSign;
    }
}
