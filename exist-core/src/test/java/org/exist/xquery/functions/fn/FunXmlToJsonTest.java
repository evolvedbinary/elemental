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

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class FunXmlToJsonTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    @Test
    public void arrayInFnNs() throws XMLDBException {
        final String query =
            "fn:xml-to-json(\n" +
            "  <array xmlns=\"http://www.w3.org/2005/xpath-functions\">\n" +
            "    <string>Curly</string>\n" +
            "    <string>Larry</string>\n" +
            "    <string>Moe</string>\n" +
            "  </array>\n" +
            ")";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("[\"Curly\",\"Larry\",\"Moe\"]", result);
    }

    @Test
    public void arrayOutsideFnNs() {
        final String query =
            "fn:xml-to-json(\n" +
            "  <array>\n" +
            "    <string>Curly</string>\n" +
            "    <string>Larry</string>\n" +
            "    <string>Moe</string>\n" +
            "  </array>\n" +
            ")";
        try {
            existEmbeddedServer.executeOneValue(query);
        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().startsWith("err:FOJS0006"));
            return;
        }

        fail("Expected XPathException: err:FOJS0006");
    }

    @Test
    public void mapInFnNs() throws XMLDBException {
        final String query =
            "fn:xml-to-json(\n" +
                "  <map xmlns=\"http://www.w3.org/2005/xpath-functions\">\n" +
                "    <string key=\"Fruit\">Apple</string>\n" +
                "    <string key=\"Vegetable\">Carrot</string>\n" +
                "  </map>" +
                ")";
        final String result = existEmbeddedServer.executeOneValue(query);
        assertEquals("{\"Fruit\":\"Apple\",\"Vegetable\":\"Carrot\"}", result);
    }

    @Test
    public void mapOutsideFnNs() {
        final String query =
            "fn:xml-to-json(\n" +
                "  <map>\n" +
                "    <string key=\"Fruit\">Apple</string>\n" +
                "    <string key=\"Vegetable\">Carrot</string>\n" +
                "  </map>" +
                ")";
        try {
            existEmbeddedServer.executeOneValue(query);
        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().startsWith("err:FOJS0006"));
            return;
        }

        fail("Expected XPathException: err:FOJS0006");
    }

}
