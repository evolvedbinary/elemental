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
