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
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
package org.exist.dom.memtree;

import com.googlecode.junittoolbox.ParallelRunner;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.EXistResourceSet;
import org.junit.*;
import org.junit.runner.RunWith;
import org.xmldb.api.base.XMLDBException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
@RunWith(ParallelRunner.class)
public class MemtreeInXQueryTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(true, true, true);

    @Test
    public void pi_attributes() throws XMLDBException {
        final String xquery = "let $doc := document{\n" +
                "    processing-instruction{\"ok\"}{\"ok\"},\n" +
                "    <root/>\n" +
                "}\n" +
                "return count($doc//processing-instruction()/@*)";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(xquery)) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                assertEquals(0, Integer.parseInt(resource.getContent().toString()));
            }
        }
    }

    @Test
    public void pi_children() throws XMLDBException {
        final String xquery = "let $doc := document{\n" +
                "    processing-instruction{\"ok\"}{\"ok\"},\n" +
                "    <root/>\n" +
                "}\n" +
                "return count($doc//processing-instruction()/node())";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(xquery)) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                assertEquals(0, Integer.parseInt(resource.getContent().toString()));
            }
        }
    }

    @Test
    public void pi_descendantAttributes() throws XMLDBException {
        final String xquery = "let $doc := document{\n" +
                "    processing-instruction{\"ok\"}{\"ok\"},\n" +
                "    <root/>\n" +
                "}\n" +
                "return count($doc//processing-instruction()//@*)";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(xquery)) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                assertEquals(0, Integer.parseInt(resource.getContent().toString()));
            }
        }
    }

    @Test
    public void attr_attributes() throws XMLDBException {
        final String xquery = "let $doc := document {\n" +
                "    element a {\n" +
                "        attribute x { \"y\" }\n" +
                "    }\n" +
                "} return\n" +
                "    count($doc/a/@x/@y)";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(xquery)) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                assertEquals(0, Integer.parseInt(resource.getContent().toString()));
            }
        }
    }

    @Test
    public void attr_children() throws XMLDBException {
        final String xquery = "let $doc := document {\n" +
                "    element a {\n" +
                "        attribute x { \"y\" }\n" +
                "    }\n" +
                "} return\n" +
                "    count($doc/a/@x/node())";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(xquery)) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                assertEquals(0, Integer.parseInt(resource.getContent().toString()));
            }
        }
    }
}
