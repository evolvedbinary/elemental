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
package org.exist.xquery.functions.fn;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.XMLDBException;

import java.nio.charset.StandardCharsets;

import static org.exist.util.ByteOrderMark.*;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ParsingFunctionsTest {

    private static final String UTF8_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    @Test
    public void parseWithoutBomWithoutDecl() throws XMLDBException {
        final String query = "parse-xml('<elem1/>')";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("<elem1/>", result);
    }

    @Test
    public void parseWithoutBomWithDecl() throws XMLDBException {
        final String query = "parse-xml('" + UTF8_DECL + "<elem2/>')";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("<elem2/>", result);
    }

    @Test
    public void parseWithUtf8BomWithoutDecl() throws XMLDBException {
        final String query = "parse-xml('" + UTF8_BOM + "<elem3/>')";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("<elem3/>", result);
    }

    @Test
    public void parseWithUtf8BomWithDecl() throws XMLDBException {
        final String query = "parse-xml('" + UTF8_BOM + UTF8_DECL + "<elem4/>')";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("<elem4/>", result);
    }

    @Test
    public void parseWithUtf16BEBomWithoutDecl() throws XMLDBException {
        final String query = "parse-xml('" + UTF16_BE_BOM + new String(new byte[]{0x00, 0x3c, 0x00, 0x65, 0x00, 0x6c, 0x00, 0x65, 0x00, 0x6d, 0x00, 0x35, 0x00, 0x2f, 0x00, 0x3e}, StandardCharsets.UTF_16BE) + "')";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("<elem5/>", result);
    }

    @Test
    public void parseWithUtf16LEBomWithoutDecl() throws XMLDBException {
        final String query = "parse-xml('" + UTF16_LE_BOM + new String(new byte[]{0x3c, 0x00, 0x65, 0x00, 0x6c, 0x00, 0x65, 0x00, 0x6d, 0x00, 0x36, 0x00, 0x2f, 0x00, 0x3e, 0x00}, StandardCharsets.UTF_16LE) + "')";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("<elem6/>", result);
    }

    @Test
    public void parseFragmentWithoutBomWithoutDecl() throws XMLDBException {
        final String query = "parse-xml-fragment('<elem1/>')";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("<elem1/>", result);
    }

    @Test
    public void parseFragmentWithUtf8BomWithoutDecl() throws XMLDBException {
        final String query = "parse-xml-fragment('" + UTF8_BOM + "<elem3/>')";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("<elem3/>", result);
    }

    @Test
    public void parseFragmentWithUtf16BEBomWithoutDecl() throws XMLDBException {
        final String query = "parse-xml-fragment('" + UTF16_BE_BOM + new String(new byte[]{0x00, 0x3c, 0x00, 0x65, 0x00, 0x6c, 0x00, 0x65, 0x00, 0x6d, 0x00, 0x35, 0x00, 0x2f, 0x00, 0x3e}, StandardCharsets.UTF_16BE) + "')";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("<elem5/>", result);
    }

    @Test
    public void parseFragmentWithUtf16LEBomWithoutDecl() throws XMLDBException {
        final String query = "parse-xml-fragment('" + UTF16_LE_BOM + new String(new byte[]{0x3c, 0x00, 0x65, 0x00, 0x6c, 0x00, 0x65, 0x00, 0x6d, 0x00, 0x36, 0x00, 0x2f, 0x00, 0x3e, 0x00}, StandardCharsets.UTF_16LE) + "')";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("<elem6/>", result);
    }
}
