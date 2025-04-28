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
package org.exist.xquery.functions.transform;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.*;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class TransformFromPkgTest {

    private static Path getConfigFile() {
        final ClassLoader loader = TransformFromPkgTest.class.getClassLoader();
        final char separator = System.getProperty("file.separator").charAt(0);
        final String packagePath = TransformFromPkgTest.class.getPackage().getName().replace('.', separator);

        try {
            return Paths.get(loader.getResource(packagePath + separator + "transform-from-pkg-test.conf.xml").toURI());
        } catch (final URISyntaxException e) {
            fail(e.getMessage());
            return null;
        }
    }

    @ClassRule
    public static ExistXmldbEmbeddedServer existXmldbEmbeddedServer = new ExistXmldbEmbeddedServer(true, false, true, getConfigFile());

    @Test
    public void transformWithModuleFromPkg() throws XMLDBException {
        final String xslt =
                "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
                        "    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
                        "    xmlns:functx=\"http://www.functx.com\"\n" +
                        "    exclude-result-prefixes=\"xs\"\n" +
                        "    version=\"2.0\">\n" +
                        "    \n" +
                        "    <xsl:import href=\"http://www.functx.com/functx.xsl\"/>\n" +
                        "    \n" +
                        "    <xsl:template match=\"/\">\n" +
                        "        <xsl:value-of select=\"functx:replace-first('hello', 'he', 'ho')\"/>\n" +
                        "    </xsl:template>\n" +
                        "    \n" +
                        "</xsl:stylesheet>";

        final String xml = "<x>bonjourno</x>";

        final String xquery = "transform:transform(" + xml + ", " + xslt + ", ())";

        final ResourceSet result = existXmldbEmbeddedServer.executeQuery(xquery);
        assertNotNull(result);
        assertEquals(1, result.getSize());
    }
}
