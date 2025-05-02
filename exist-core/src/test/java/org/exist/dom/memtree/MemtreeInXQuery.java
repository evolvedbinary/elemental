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
package org.exist.dom.memtree;

import com.googlecode.junittoolbox.ParallelRunner;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@RunWith(ParallelRunner.class)
public class MemtreeInXQuery {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(true, true, true);

    @Test
    public void pi_attributes() throws XMLDBException {
        final String xquery = "let $doc := document{\n" +
                "    processing-instruction{\"ok\"}{\"ok\"},\n" +
                "    <root/>\n" +
                "}\n" +
                "return count($doc//processing-instruction()/@*)";

        final ResourceSet result = existEmbeddedServer.executeQuery(xquery);

        assertEquals(1, result.getSize());
        assertEquals(0, Integer.parseInt(result.getResource(0).getContent().toString()));

        result.clear();
    }

    @Test
    public void pi_children() throws XMLDBException {
        final String xquery = "let $doc := document{\n" +
                "    processing-instruction{\"ok\"}{\"ok\"},\n" +
                "    <root/>\n" +
                "}\n" +
                "return count($doc//processing-instruction()/node())";

        final ResourceSet result = existEmbeddedServer.executeQuery(xquery);

        assertEquals(1, result.getSize());
        assertEquals(0, Integer.parseInt(result.getResource(0).getContent().toString()));

        result.clear();
    }

    @Test
    public void pi_descendantAttributes() throws XMLDBException {
        final String xquery = "let $doc := document{\n" +
                "    processing-instruction{\"ok\"}{\"ok\"},\n" +
                "    <root/>\n" +
                "}\n" +
                "return count($doc//processing-instruction()//@*)";

        final ResourceSet result = existEmbeddedServer.executeQuery(xquery);

        assertEquals(1, result.getSize());
        assertEquals(0, Integer.parseInt(result.getResource(0).getContent().toString()));

        result.clear();
    }

    @Test
    public void attr_attributes() throws XMLDBException {
        final String xquery = "let $doc := document {\n" +
                "    element a {\n" +
                "        attribute x { \"y\" }\n" +
                "    }\n" +
                "} return\n" +
                "    count($doc/a/@x/@y)";

        final ResourceSet result = existEmbeddedServer.executeQuery(xquery);

        assertEquals(1, result.getSize());
        assertEquals(0, Integer.parseInt(result.getResource(0).getContent().toString()));

        result.clear();
    }

    @Test
    public void attr_children() throws XMLDBException {
        final String xquery = "let $doc := document {\n" +
                "    element a {\n" +
                "        attribute x { \"y\" }\n" +
                "    }\n" +
                "} return\n" +
                "    count($doc/a/@x/node())";

        final ResourceSet result = existEmbeddedServer.executeQuery(xquery);

        assertEquals(1, result.getSize());
        assertEquals(0, Integer.parseInt(result.getResource(0).getContent().toString()));

        result.clear();
    }
}
