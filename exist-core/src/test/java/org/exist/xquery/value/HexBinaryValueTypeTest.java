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
package org.exist.xquery.value;

import org.exist.xquery.XPathException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author <a href="mailto:adam@existsolutions.com">Adam Retter</a>
 */
public class HexBinaryValueTypeTest {
    
    @Test(expected=XPathException.class)
    public void verify_notMultipleOf2Chars_fails() throws XPathException {
        TestableHexBinaryValueType hexType = new TestableHexBinaryValueType();
        hexType.verifyString("010010101");
    }

    @Test
    public void verify_multipleOfChars_passes() throws XPathException {
        TestableHexBinaryValueType hexType = new TestableHexBinaryValueType();
        hexType.verifyString("01001010");
    }

    @Test(expected=XPathException.class)
    public void verify_notValidChars_fails() throws XPathException {
        TestableHexBinaryValueType hexType = new TestableHexBinaryValueType();
        hexType.verifyString("true");
    }

    @Test
    public void verify_validChars_passes() throws XPathException {
        TestableHexBinaryValueType hexType = new TestableHexBinaryValueType();
        hexType.verifyString("0fb7");
    }

    @Test
    public void format_upperCases() throws XPathException {
        final String hexString = "0fb7";

        TestableHexBinaryValueType hexType = new TestableHexBinaryValueType();
        final String result = hexType.formatString(hexString);

        assertEquals(hexString.toUpperCase(), result);
    }

    public class TestableHexBinaryValueType extends HexBinaryValueType {
        @Override
        public void verifyString(String str) throws XPathException {
            super.verifyString(str);
        }

        @Override
        protected String formatString(String str) {
            return super.formatString(str);
        }
    }
}
