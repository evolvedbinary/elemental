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
