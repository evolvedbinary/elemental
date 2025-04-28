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

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.util.function.BiFunction;

import org.exist.util.io.Base64OutputStream;
import org.exist.xquery.XPathException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author <a href="mailto:adam@existsolutions.com">Adam Retter</a>
 */
public class BinaryValueTypeTest {

    @Test
    public void verifyAndFormat_does_trim() throws XPathException {
        final String testValue = " HELLO \r\n";
        final BinaryValueType<Base64OutputStream> binaryValueType = new TestableBinaryValueType<>(Type.BASE64_BINARY, Base64OutputStream::new);
        final String result = binaryValueType.verifyAndFormatString(testValue);

        assertEquals(testValue.trim(), result);
    }

    @Test
    public void verifyAndFormat_replaces_whiteSpace() throws XPathException {
        final String testValue = "HELLO WO RLD";

        final BinaryValueType<Base64OutputStream> binaryValueType = new TestableBinaryValueType<>(Type.BASE64_BINARY, Base64OutputStream::new);
        final String result = binaryValueType.verifyAndFormatString(testValue);

        assertEquals(testValue.replaceAll("\\s", ""), result);
    }

    public static class TestableBinaryValueType<T extends FilterOutputStream> extends BinaryValueType<T> {

        public TestableBinaryValueType(final int xqueryType, final BiFunction<OutputStream, Boolean, T> coderFactory) {
            super(xqueryType, coderFactory);
        }

        @Override
        public void verifyString(String str) {
        }

        @Override
        protected String formatString(String str) {
            return str;
        }
    }
}