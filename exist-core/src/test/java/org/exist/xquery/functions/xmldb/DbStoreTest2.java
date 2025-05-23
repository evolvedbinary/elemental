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
package org.exist.xquery.functions.xmldb;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.concurrent.DBUtils;
import org.junit.*;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XPathQueryService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertNotNull;

/**
 * Due to limitation of ExistXmldbEmbeddedServer we need to split this test to two files.
 * It's not possible to have two instances of ExistXmldbEmbeddedServer at the same time.
 */
public class DbStoreTest2 {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServerWithAnyURI = new ExistXmldbEmbeddedServer(false, true,
            true, getConfig());

    private static final int BUFFER_SIZE = 1024 * 1024 * 4; // 4MiB buffer
    private static final long FILE_SIZE = 3l * 1024l * 1024l * 1024l; //3GiB file.
    private static final String TEST_COLLECTION = "testAnyUri2";

    private static Path largeFileLocation = null;
    private static Path jettyRootDir = null;
    private static Path pictureLocation = null;

    //Second jetty server to mock HTTP resources for tests.
    private static Server jettyServer = null;
    private static int jettyPort = 30350;

    private final static Path getConfig() {
        try {
            final URL path = DbStoreTest.class.getClassLoader().getResource("org/exist/xmldb/allowAnyUri.xml");
            return Paths.get(path.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("unable to parse URI for resource", e);
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {

        jettyPort += new Random().nextInt(15000);

        jettyRootDir = Files.createTempDirectory("dbstore2-test");
        largeFileLocation = jettyRootDir.resolve("large-file.bin");
        pictureLocation = jettyRootDir.resolve("picture.jpg");

        try (final FileOutputStream fOut = new FileOutputStream(pictureLocation.toFile(), true)) {
            final byte buff[] = new byte[BUFFER_SIZE];
            fOut.write(buff);
        }

        jettyServer = new Server(jettyPort);
        final ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        final String dir = jettyRootDir.toAbsolutePath().toFile().getCanonicalPath();
        resource_handler.setResourceBase(dir);

        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resource_handler, new DefaultHandler()});

        jettyServer.setHandler(handlers);
        jettyServer.start();

    }

    @AfterClass
    public static void afterClass() throws Exception {
        jettyServer.stop();
        FileUtils.deleteDirectory(jettyRootDir.toFile());
    }

    @Test
    public final void testWithAnyUriEnabled() throws XMLDBException {
        final Collection rootCol = existEmbeddedServerWithAnyURI.getRoot();
        Collection testCol = rootCol.getChildCollection(TEST_COLLECTION);
        if (testCol == null) {
            testCol = DBUtils.addCollection(rootCol, TEST_COLLECTION);
            assertNotNull(testCol);
        }

        final XPathQueryService xpqs = testCol.getService(XPathQueryService.class);
        final ResourceSet rs =
                xpqs.query(
                        "xmldb:store(\n" +
                                "        '/db',\n" +
                                "        'image.jpg',\n" +
                                "        xs:anyURI('http://localhost:" + jettyPort + "/picture.jpg'),\n" +
                                "        'image/png'\n" +
                                "    )");
        assertNotNull(rs);
    }

    @Test
    public final void testLargeFileStore() throws XMLDBException, IOException {
        final byte buff[] = new byte[BUFFER_SIZE];
        try (final FileOutputStream fOut = new FileOutputStream(largeFileLocation.toFile(), true)) {
            for (long written = 0; written < FILE_SIZE; written += BUFFER_SIZE) {
                fOut.write(buff);
            }
            fOut.flush();
        }

        final Collection rootCol = existEmbeddedServerWithAnyURI.getRoot();
        Collection testCol = rootCol.getChildCollection(TEST_COLLECTION);
        if (testCol == null) {
            testCol = DBUtils.addCollection(rootCol, TEST_COLLECTION);
            assertNotNull(testCol);
        }

        final XPathQueryService xpqs = testCol.getService(XPathQueryService.class);
        final ResourceSet rs =
                xpqs.query(
                        "xmldb:store(\n" +
                                "        '/db',\n" +
                                "        'image.jpg',\n" +
                                "        xs:anyURI('http://localhost:" + jettyPort + "/large-file.bin'),\n" +
                                "        'image/png'\n" +
                                "    )");
        assertNotNull(rs);
    }
}