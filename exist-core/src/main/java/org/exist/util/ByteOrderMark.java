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
package org.exist.util;

import java.nio.charset.StandardCharsets;

/**
 * Byte Order Mark utilities.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ByteOrderMark {
    public static final byte[] UTF8_BOM_BYTES = new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF};
    public static final String UTF8_BOM = new String(UTF8_BOM_BYTES, StandardCharsets.UTF_8);


    public static final byte[] UTF16_BE_BOM_BYTES = new byte[] {(byte)0xFE, (byte)0xFF};
    public static final String UTF16_BE_BOM = new String(UTF16_BE_BOM_BYTES, StandardCharsets.UTF_16BE);

    public static final byte[] UTF16_LE_BOM_BYTES = new byte[] {(byte)0xFF, (byte)0xFE};
    public static final String UTF16_LE_BOM = new String(UTF16_LE_BOM_BYTES, StandardCharsets.UTF_16LE);

    /**
     * Strip BOM from the start of an XML string.
     *
     * @param xml the XML as a string
     *
     * @return the XML without a BOM.
     */
    public static String stripXmlBom(final String xml) {
        if (xml.startsWith(UTF8_BOM) || xml.startsWith(UTF16_BE_BOM) || xml.startsWith(UTF16_LE_BOM)) {
            return xml.substring(1);
        }
        return xml;
    }
}
