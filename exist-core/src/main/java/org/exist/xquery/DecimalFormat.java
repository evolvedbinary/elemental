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
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
package org.exist.xquery;

import java.util.Map;

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

    /**
     * Checks that the characters used in a picture string have distinct values.
     *
     * @return true if all the characters are distinct, false otherwise.
     */
    public boolean checkDistinctCharacters() {
        final int[] characters = new int[] {
            decimalSeparator,
            exponentSeparator,
            groupingSeparator,
            percent,
            perMille,
            zeroDigit,
            digit,
            patternSeparator
        };

        for (int i = 0; i < characters.length; i++) {
            final int c = characters[i];
            for (int j = i + 1; j < characters.length; j++) {
                final int o = characters[j];
                if (c == o) {
                    return false;
                }
            }
        }

        return true;
    }

    public static DecimalFormat fromProperties(final Map<String, String> properties) {
        int decimalSeparator = UNNAMED.decimalSeparator;
        int exponentSeparator = UNNAMED.exponentSeparator;
        int groupingSeparator = UNNAMED.groupingSeparator;
        int percent = UNNAMED.percent;
        int perMille = UNNAMED.perMille;
        int zeroDigit = UNNAMED.zeroDigit;
        int digit = UNNAMED.digit;
        int patternSeparator = UNNAMED.patternSeparator;
        String infinity = UNNAMED.infinity;
        String NaN = UNNAMED.NaN;
        int minusSign = UNNAMED.minusSign;

        for (final Map.Entry<String, String> property : properties.entrySet()) {
            final String value = property.getValue();
            switch (property.getKey()) {
                case "decimal-separator":
                    decimalSeparator = value.charAt(0);
                    break;
                case "exponent-separator":
                    exponentSeparator = value.charAt(0);
                    break;
                case "grouping-separator":
                    groupingSeparator = value.charAt(0);
                    break;
                case "percent":
                    percent = value.charAt(0);
                    break;
                case "per-mille":
                    perMille = value.charAt(0);
                    break;
                case "zero-digit":
                    zeroDigit = value.charAt(0);
                    break;
                case "digit":
                    digit = value.charAt(0);
                    break;
                case "pattern-separator":
                    patternSeparator = value.charAt(0);
                    break;
                case "infinity":
                    infinity = value;
                    break;
                case "NaN":
                    NaN = value;
                    break;
                case "minus-sign":
                    minusSign = value.charAt(0);
                    break;
            }
        }

        return new DecimalFormat(decimalSeparator, exponentSeparator, groupingSeparator, percent, perMille, zeroDigit, digit, patternSeparator, infinity, NaN, minusSign);
    }
}
