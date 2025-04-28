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
package org.exist.xquery;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class WatchdogTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    @BeforeClass
    public static void setup() throws XMLDBException {
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

        try (final Collection dbCollection = existEmbeddedServer.getRoot();
        final Collection watchdogTestCollection = existEmbeddedServer.createCollection(dbCollection, "watchdog-test")) {
            final Resource nodesModule = watchdogTestCollection.createResource("nodes.xqm", BinaryResource.class);
            nodesModule.setContent(queryModule);
            watchdogTestCollection.storeResource(nodesModule);
        }
    }

    @Test
    public void outputSizeLimitUnderInImportModule() throws XMLDBException {
        final String query =
            "declare option exist:output-size-limit \"100\";\n" +
            "import module namespace nodes = \"http://nodes\" at \"/db/watchdog-test/nodes.xqm\";\n" +
            "nodes:many()";

        final String result = existEmbeddedServer.executeOneValue(query);
        assertNotNull(result);
    }

    @Test
    public void outputSizeLimitOverInImportModule() {
        final String query =
            "declare option exist:output-size-limit \"4\";\n" +
            "import module namespace nodes = \"http://nodes\" at \"/db/watchdog-test/nodes.xqm\";\n" +
            "nodes:many()";

        try {
            existEmbeddedServer.executeOneValue(query);
        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().startsWith("exerr:ERROR The constructed document fragment exceeded the predefined output-size-limit"));
        }
    }

    @Test
    public void outputSizeLimitUnderInLoadXQueryModule() throws XMLDBException {
        final String query =
            "declare option exist:output-size-limit \"100\";\n" +
            "let $nodes-mod := fn:load-xquery-module(\"http://nodes\",  map{ \"location-hints\": \"/db/watchdog-test/nodes.xqm\" })\n" +
            "let $nodes-mod-many := $nodes-mod?functions?(QName(\"http://nodes\", \"many\"))?0\n" +
            "return\n" +
            "    $nodes-mod-many()";

        final String result = existEmbeddedServer.executeOneValue(query);
        assertNotNull(result);
    }

    @Test
    public void outputSizeLimitOverInLoadXQueryModule() throws XMLDBException {
        final String query =
            "declare option exist:output-size-limit \"4\";\n" +
                "let $nodes-mod := fn:load-xquery-module(\"http://nodes\",  map{ \"location-hints\": \"/db/watchdog-test/nodes.xqm\" })\n" +
                "let $nodes-mod-many := $nodes-mod?functions?(QName(\"http://nodes\", \"many\"))?0\n" +
                "return\n" +
                "    $nodes-mod-many()";

        try {
            existEmbeddedServer.executeOneValue(query);
        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().startsWith("exerr:ERROR The constructed document fragment exceeded the predefined output-size-limit"));
        }
    }
}
