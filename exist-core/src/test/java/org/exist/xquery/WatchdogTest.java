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
package org.exist.xquery;

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class WatchdogTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    @BeforeAll
    static void setup() throws XMLDBException {
        final String queryModule = "module namespace nodes = \"http://nodes\";\n" +
            "\n" +
            "declare function nodes:many()\n" +
            "{\n" +
            "  <a id=\"too-many\" output-size-limit=\"{util:get-option('exist:output-size-limit')}\">\n" +
            "    <b><c/><d/><e><f/></e><g/></b>\n" +
            "    <b><c/><d/><e><f/></e><g/></b>\n" +
            "    <b><c/><d/><e><f/></e><g/></b>\n" +
            "  </a>\n" +
            "};";

        try (final Collection dbCollection = XMLDB_EMBEDDED_DATABASE.getRoot();
             final Collection watchdogTestCollection = XMLDB_EMBEDDED_DATABASE.createCollection(dbCollection, "watchdog-test");
             final Resource nodesModule = watchdogTestCollection.createResource("nodes.xqm", BinaryResource.class)) {
            nodesModule.setContent(queryModule);
            watchdogTestCollection.storeResource(nodesModule);
        }
    }

    @Test
    void outputSizeLimitUnderInImportModule() throws XMLDBException {
        final String query =
            "declare option exist:output-size-limit \"100\";\n" +
            "import module namespace nodes = \"http://nodes\" at \"/db/watchdog-test/nodes.xqm\";\n" +
            "nodes:many()";

        final String result = XMLDB_EMBEDDED_DATABASE.executeOneValue(query);
        assertNotNull(result);
    }

    @Test
    void outputSizeLimitOverInImportModule() {
        final String query =
            "declare option exist:output-size-limit \"4\";\n" +
            "import module namespace nodes = \"http://nodes\" at \"/db/watchdog-test/nodes.xqm\";\n" +
            "nodes:many()";

        try {
            XMLDB_EMBEDDED_DATABASE.executeOneValue(query);
        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().startsWith("exerr:ERROR The constructed document fragment exceeded the predefined output-size-limit"));
        }
    }

    @Test
    void outputSizeLimitUnderInLoadXQueryModule() throws XMLDBException {
        final String query =
            "declare option exist:output-size-limit \"100\";\n" +
            "let $nodes-mod := fn:load-xquery-module(\"http://nodes\",  map{ \"location-hints\": \"/db/watchdog-test/nodes.xqm\" })\n" +
            "let $nodes-mod-many := $nodes-mod?functions?(QName(\"http://nodes\", \"many\"))?0\n" +
            "return\n" +
            "    $nodes-mod-many()";

        final String result = XMLDB_EMBEDDED_DATABASE.executeOneValue(query);
        assertNotNull(result);
    }

    @Test
    void outputSizeLimitOverInLoadXQueryModule() throws XMLDBException {
        final String query =
            "declare option exist:output-size-limit \"4\";\n" +
                "let $nodes-mod := fn:load-xquery-module(\"http://nodes\",  map{ \"location-hints\": \"/db/watchdog-test/nodes.xqm\" })\n" +
                "let $nodes-mod-many := $nodes-mod?functions?(QName(\"http://nodes\", \"many\"))?0\n" +
                "return\n" +
                "    $nodes-mod-many()";

        try {
            XMLDB_EMBEDDED_DATABASE.executeOneValue(query);
        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().startsWith("exerr:ERROR The constructed document fragment exceeded the predefined output-size-limit"));
        }
    }
}
