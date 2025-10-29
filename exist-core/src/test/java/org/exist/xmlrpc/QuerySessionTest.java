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
package org.exist.xmlrpc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.TestDataGenerator;
import org.exist.TestUtils;
import org.exist.storage.ReindexRecoveryTest;
import org.exist.test.ExistWebServer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XQueryService;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class QuerySessionTest {

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "local", "xmldb:exist://" },
            { "remote", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc" }
        });
    }

    @Parameterized.Parameter
    public String apiName;

    @Parameterized.Parameter(value = 1)
    public String baseUri;

    private boolean storedTestData = false;

    private static final Logger LOG = LogManager.getLogger(QuerySessionTest.class);

    @ClassRule
    public final static ExistWebServer existWebServer = new ExistWebServer(true, false, true, true);
    private static final String PORT_PLACEHOLDER = "${PORT}";

    private final static String generateXQ =
            "declare function local:random-sequence($length as xs:integer, $G as map(xs:string, item())) {\n"
            + "  if ($length eq 0)\n"
            + "  then ()\n"
            + "  else ($G?number, local:random-sequence($length - 1, $G?next()))\n"
            + "};\n"
            + "let $rnd := fn:random-number-generator() return"
            + "<book id=\"{$filename}\" n=\"{$count}\">"
			+ "   <chapter xml:id=\"chapter{$count}\">"
			+ "       <title>{local:random-sequence(7, $rnd)}</title>"
			+ "       {"
			+ "           for $section in 1 to 8 return"
			+ "               <section id=\"sect{$section}\">"
			+ "                   <title>{local:random-sequence(7, $rnd)}</title>"
			+ "                   {"
			+ "                       for $para in 1 to 10 return"
			+ "                           <para>{local:random-sequence(120, $rnd)}</para>"
			+ "                   }"
			+ "               </section>"
			+ "       }"
			+ "   </chapter>"
			+ "</book>";

    private final static String QUERY =
            "declare variable $n external;" +
            "//chapter[@xml:id eq $n]";

    private String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(existWebServer.getPort()));
    }

    private final static int N_THREADS = 10;

    private final static int DOC_COUNT = 100;

    private Random random = new Random();

    @Test
    public void manualRelease() throws XMLDBException {
        Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/rpctest", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        XQueryService service = (XQueryService) test.getService("XQueryService", "1.0");
        ResourceSet result = service.query("//chapter[@xml:id eq 'chapter1']");
        assertTrue(result.getSize() > 0);

        if (!"local".equals(apiName)) {
            // clear should release the query result on the server
            result.clear();

            // As the result has been cleared already, we should get an exception below
            try {
                result.getMembersAsResource();
                fail("Expected XMLDBException from calling Resource#getMembersAsResource() after ResourceSet#clear() when using the Remote XML:DB API");
            } catch (final XMLDBException e) {
                assertEquals("Failed to invoke method retrieveAllFirstChunk in class org.exist.xmlrpc.RpcConnection: result set unknown or timed out", e.getMessage());
            }
        }
    }

    @Test
    public void runTasks() {
        ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
        for (int i = 0; i < 100; i++) {
            executor.submit(new QueryTask(QUERY));
        }

        executor.shutdown();
		boolean terminated = false;
		try {
			terminated = executor.awaitTermination(60 * 60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}
		Assert.assertTrue(terminated);
    }

    private class QueryTask implements Runnable {

        private String query;

        private QueryTask(String query) {
            this.query = query;
        }

        @Override
        public void run() {
            try {
                final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/rpctest", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
                final XQueryService service = (XQueryService) test.getService("XQueryService", "1.0");
                final int n = random.nextInt(DOC_COUNT) + 1;
                service.declareVariable("n", "chapter" + n);
                final ResourceSet result = service.query(query);
                assertEquals(1, result.getSize());
            } catch (final XMLDBException e) {
                LOG.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }

    @Before
    public void storeTestData() throws XMLDBException, SAXException {
        if (!storedTestData) {
            // NOTE(AR) we only need to store the test data once!
            final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);

            final CollectionManagementService mgmt = (CollectionManagementService) root.getService("CollectionManagementService", "1.0");
            final Collection test = mgmt.createCollection("rpctest");

            final TestDataGenerator generator = new TestDataGenerator("xdb", DOC_COUNT);
            try {
                final Path[] files = generator.generate(test, generateXQ);
                for (int i = 0; i < files.length; i++) {
                    final Resource resource = test.createResource(files[i].getFileName().toString(), "XMLResource");
                    resource.setContent(files[i].toFile());
                    test.storeResource(resource);
                }
            } finally {
                generator.releaseAll();
            }

            storedTestData = true;
        }
    }
}
