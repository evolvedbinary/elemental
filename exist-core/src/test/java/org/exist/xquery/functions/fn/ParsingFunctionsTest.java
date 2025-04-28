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
