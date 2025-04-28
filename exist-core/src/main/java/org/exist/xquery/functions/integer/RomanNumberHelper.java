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

import java.util.TreeMap;

/**
 * Roman numerals, in support of formatting by {@link RomanIntegerPicture}
 *
 * The source code for this class is taken from the stackoverflow answer
 * https://stackoverflow.com/questions/12967896/converting-integers-to-roman-numerals-java
 * written by https://stackoverflow.com/users/1420681/ben-hur-langoni-junior
 * and is therefore used and made available in accordance with
 * https://creativecommons.org/licenses/by-sa/3.0
 * 
 */
class RomanNumberHelper {

    private static final TreeMap<Integer, String> map = new TreeMap<>();

    private RomanNumberHelper() {}

    static {

        RomanNumberHelper.map.put(1000, "M");
        RomanNumberHelper.map.put(900, "CM");
        RomanNumberHelper.map.put(500, "D");
        RomanNumberHelper.map.put(400, "CD");
        RomanNumberHelper.map.put(100, "C");
        RomanNumberHelper.map.put(90, "XC");
        RomanNumberHelper.map.put(50, "L");
        RomanNumberHelper.map.put(40, "XL");
        RomanNumberHelper.map.put(10, "X");
        RomanNumberHelper.map.put(9, "IX");
        RomanNumberHelper.map.put(5, "V");
        RomanNumberHelper.map.put(4, "IV");
        RomanNumberHelper.map.put(1, "I");

    }

    public static String toRoman(final int number) {
        final int l = RomanNumberHelper.map.floorKey(number);
        if (number == l) {
            return RomanNumberHelper.map.get(number);
        }
        return RomanNumberHelper.map.get(l) + RomanNumberHelper.toRoman(number - l);
    }

}
