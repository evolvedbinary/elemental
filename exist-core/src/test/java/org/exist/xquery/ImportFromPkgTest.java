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
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ImportFromPkgTest {

    private static Path getConfigFile() {
        final ClassLoader loader = ImportFromPkgTest.class.getClassLoader();
        final char separator = System.getProperty("file.separator").charAt(0);
        final String packagePath = ImportFromPkgTest.class.getPackage().getName().replace('.', separator);

        try {
            return Paths.get(loader.getResource(packagePath + separator + "import-from-pkg-test.conf.xml").toURI());
        } catch (final URISyntaxException e) {
            fail(e.getMessage());
            return null;
        }
    }

    @ClassRule
    public static ExistXmldbEmbeddedServer existXmldbEmbeddedServer = new ExistXmldbEmbeddedServer(true, false, true, getConfigFile());

    @Test
    public void printPackages() throws XMLDBException {
        //final String query = "import module namespace packages=\"http://exist-db.org/apps/existdb-packages\" at \"/db/apps/packageservice/modules/packages.xqm\";\n" +
        //"packages:get-local-packages()";

        final String query = "xmldb:get-child-resources('/db/system/repo/functx-1.0.1/functx/')";

        final ResourceSet resultSet = existXmldbEmbeddedServer.executeQuery(query);

        for (int i = 0; i < resultSet.getSize(); i++) {
            System.out.println(resultSet.getResource(i).getContent().toString());
        }
    }

    @Test
    public void importFunctxNs() throws XMLDBException {
        final String query =
            "import module namespace functx = \"http://www.functx.com\";\n" +
            "\n" +
            "<test>{functx:index-of-string('hello', 'll')}</test>";
        final ResourceSet resultSet = existXmldbEmbeddedServer.executeQuery(query);
        assertNotNull(resultSet);
        assertEquals(1, resultSet.getSize());
        final Resource result = resultSet.getResource(0);
        assertNotNull(result);
        assertEquals("<test>3</test>", result.getContent().toString());
    }

    @Test
    public void importFunctxLocationHintDb() throws XMLDBException {
        final String query =
            "import module namespace functx = \"http://www.functx.com\" at \"/db/system/repo/functx-1.0.1/functx/functx.xq\";\n" +
            "\n" +
            "<test>{functx:index-of-string('hello', 'll')}</test>";
        final ResourceSet resultSet = existXmldbEmbeddedServer.executeQuery(query);
        assertNotNull(resultSet);
        assertEquals(1, resultSet.getSize());
        final Resource result = resultSet.getResource(0);
        assertNotNull(result);
        assertEquals("<test>3</test>", result.getContent().toString());
    }

    @Test
    public void importFunctxLocationHintXmldb() throws XMLDBException {
        final String query =
            "import module namespace functx = \"http://www.functx.com\" at \"xmldb:exist:///db/system/repo/functx-1.0.1/functx/functx.xq\";\n" +
                "\n" +
                "<test>{functx:index-of-string('hello', 'll')}</test>";
        final ResourceSet resultSet = existXmldbEmbeddedServer.executeQuery(query);
        assertNotNull(resultSet);
        assertEquals(1, resultSet.getSize());
        final Resource result = resultSet.getResource(0);
        assertNotNull(result);
        assertEquals("<test>3</test>", result.getContent().toString());
    }

    @Test
    public void declareFunctx() {
        try {
            final String query =
                "declare namespace functx = \"http://www.functx.com\";\n" +
                "\n" +
                "<test>{functx:index-of-string('hello', 'll')}</test>";
            existXmldbEmbeddedServer.executeQuery(query);
        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().startsWith("err:XPST0017 Call to undeclared function: functx:index-of-string"));
            return;
        }

        fail("Expected XPathException: err:XPST0017 Call to undeclared function: functx:index-of-string");
    }
}
