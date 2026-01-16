/*
 * Copyright (C) 2014, Evolved Binary Ltd
 *
 * This file was originally ported from FusionDB to Elemental by
 * Evolved Binary, for the benefit of the Elemental Open Source community.
 * Only the ported code as it appears in this file, at the time that
 * it was contributed to Elemental, was re-licensed under The GNU
 * Lesser General Public License v2.1 only for use in Elemental.
 *
 * This license grant applies only to a snapshot of the code as it
 * appeared when ported, it does not offer or infer any rights to either
 * updates of this source code or access to the original source code.
 *
 * The GNU Lesser General Public License v2.1 only license follows.
 *
 * =====================================================================
 *
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

import org.exist.TestUtils;
import org.exist.storage.BrokerPool;
import org.exist.test.ExistWebServer;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.EXistXPathQueryService;
import org.exist.xmldb.XmldbURI;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import xyz.elemental.mediatype.MediaType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.util.PropertiesBuilder.propertiesBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Performs a number of tests against the read-locked source document of an XQuery that is currently running over the XML-RPC API.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@RunWith(Parameterized.class)
public class RunningXQueryXmlDbTest {

    private static final XmldbURI TEST_COLLECTION_URI = XmldbURI.DB.append("running-xquery-test");
    private static final XmldbURI LATCHED_QUERY_URI = XmldbURI.create("latched-query.xq");
    private static final String RUNNING_LATCH_REF_XQUERY_VARIABLE_NAME = "query-running-latch-ref";
    private static final String EXIT_LATCH_REF_XQUERY_VARIABLE_NAME = "query-exit-latch-ref";

    private static final long TIMEOUT_MS = 3000;
    private static final String PORT_PLACEHOLDER = "${PORT}";

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

    @Rule
    public final ExistWebServer existWebServer = new ExistWebServer(true, false,
            propertiesBuilder()
                    .put(FunctionFactory.PROPERTY_ENABLE_JAVA_BINDING, true)
                    .put(BrokerPool.PROPERTY_SHUTDOWN_DELAY, 100L)
                    .build(),
            true, true, true);

    @Nullable private ExecutorService executorService = null;
    @Nullable private Future<String> queryResultFuture = null;

    private final String query =
            "declare namespace system = 'http://exist-db.org/xquery/system';\n" +
            "declare namespace lws = 'java:" + LatchWrappersSingleton.class.getName() + "';\n" +
            "declare namespace at = 'java:" + AtomicReference.class.getName() + "';\n" +
            "declare namespace cl = 'java:" + CountDownLatch.class.getName() + "';\n" +
            "declare namespace tu = 'java:" + TimeUnit.class.getName() + "';\n" +
            "declare variable $lwsi := lws:INSTANCE();\n" +
            "declare variable $" + RUNNING_LATCH_REF_XQUERY_VARIABLE_NAME + " := lws:queryRunningLatchRef($lwsi);\n" +
            "declare variable $" + EXIT_LATCH_REF_XQUERY_VARIABLE_NAME + " := lws:queryExitLatchRef($lwsi);\n" +
            "\n" +
            "let $running-latch := at:get($" + RUNNING_LATCH_REF_XQUERY_VARIABLE_NAME + ")\n" +
            "let $_ := cl:countDown($running-latch)\n" +
            "let $start := util:system-time()\n" +
            "let $exit-latch := at:get($" + EXIT_LATCH_REF_XQUERY_VARIABLE_NAME + ")\n" +
            "let $exit-latch-zero := cl:await($exit-latch, " + TIMEOUT_MS + " cast as xs:long, tu:MILLISECONDS())\n" +
            "let $end := util:system-time()\n" +
            "return\n" +
            "  <elapsed-time exit-latch-zero='{$exit-latch-zero}'>{$end - $start}</elapsed-time>";

    /**
     * This singleton allows us to share access to the latches between this test in Java and the XQuery we are executing.
     */
    public static class LatchWrappersSingleton {
        public static final LatchWrappersSingleton INSTANCE = new LatchWrappersSingleton();

        final AtomicReference<CountDownLatch> queryRunningLatchRef = new AtomicReference<>();
        final AtomicReference<CountDownLatch> queryExitLatchRef = new AtomicReference<>();

        private LatchWrappersSingleton() {
        }
    }

    private String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(existWebServer.getPort()));
    }

    @Before
    public void runXQuery() throws InterruptedException, XMLDBException {
        // Store our XQuery as document into the database
        try (final Collection root = DatabaseManager.getCollection(getBaseUri() + TEST_COLLECTION_URI.removeLastSegment().getCollectionPath(), TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
            final CollectionManagementService cms = (CollectionManagementService) root.getService("CollectionManagementService", "1.0");
            try (final Collection testCollection = cms.createCollection(TEST_COLLECTION_URI.lastSegmentString());
                 final EXistResource queryResource = (EXistResource) testCollection.createResource(LATCHED_QUERY_URI.getCollectionPath(), BinaryResource.RESOURCE_TYPE)) {
                queryResource.setMediaType(MediaType.APPLICATION_XQUERY);
                queryResource.setContent(query.getBytes(UTF_8));
                testCollection.storeResource(queryResource);
            }
        }

        // Create a latch that we can receive a signal from when the query is running
        LatchWrappersSingleton.INSTANCE.queryRunningLatchRef.set(new CountDownLatch(1));

        // Create a latch that we can send a signal to allow the query to finish
        LatchWrappersSingleton.INSTANCE.queryExitLatchRef.set(new CountDownLatch(1));

        // Prepare a Callable that will compile and execute the XQuery (which will wait on the queryExitLatch)
        final Callable<String> latchedQueryCallable = () -> {
            try (final Collection testCollection = DatabaseManager.getCollection(getBaseUri() + TEST_COLLECTION_URI.getCollectionPath(), TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
                final EXistXPathQueryService xpathQueryService = (EXistXPathQueryService) testCollection.getService("XPathQueryService", "1.0");
                try (final EXistResourceSet result = xpathQueryService.executeStoredQuery(TEST_COLLECTION_URI.append(LATCHED_QUERY_URI).getCollectionPath())) {
                    final Resource resultResource = result.getResource(0);
                    return (String) resultResource.getContent();
                }
            } catch (final XMLDBException e) {
                e.printStackTrace();
                throw e;
            }
        };

        // Run the latched query callable from a separate thread so that we don't block our test
        this.executorService = Executors.newFixedThreadPool(2);   // We size to 2 threads so that we have one for the latched query, and one for the test method itself (see tests below)
        this.queryResultFuture = executorService.submit(latchedQueryCallable);

        // Wait for the query to start running
        final CountDownLatch latch = LatchWrappersSingleton.INSTANCE.queryRunningLatchRef.get();
        try {
            final boolean running = latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue("Timeout exceeded whilst waiting for query to notify us it is running", running);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();  // Restore interrupted flag
            throw e;
        }
    }

    @After
    public void stopXQuery() {
        @Nullable final CountDownLatch runningLatch = LatchWrappersSingleton.INSTANCE.queryRunningLatchRef.getAndSet(null);
        if (runningLatch != null) {
            while (runningLatch.getCount() > 0) {
                runningLatch.countDown();
            }
        }

        @Nullable final CountDownLatch exitLatch = LatchWrappersSingleton.INSTANCE.queryExitLatchRef.getAndSet(null);
        if (exitLatch != null) {
            while (exitLatch.getCount() > 0) {
                exitLatch.countDown();
            }
        }

        this.queryResultFuture.cancel(true);
        this.queryResultFuture = null;
        this.executorService.shutdownNow();
        this.executorService = null;
    }

    /**
     * When this test is run, the /db/running-xquery-test/latched-query.xq XQuery
     * is already executing (see {@link #runXQuery()}) in a separate thread that holds a READ_LOCK on its Source document.
     *
     * We try and read the XQuery source document under those conditions, and the read should succeed.
     */
    @Test
    public void readRunningXQuerySourceDocument() throws InterruptedException, ExecutionException, TimeoutException {
        // Prepare a Callable that will try and read the XQuery's source document
        final Callable<String> readDocumentCallable = () -> {

            // try and read the XQuery source document
            try {
                try (final Collection testCollection = DatabaseManager.getCollection(getBaseUri() + TEST_COLLECTION_URI.getCollectionPath(), TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
                    final EXistResource queryResource = (EXistResource) testCollection.getResource(LATCHED_QUERY_URI.getCollectionPath())) {
                    assertTrue(queryResource instanceof BinaryResource);
                    return new String((byte[]) queryResource.getContent(), UTF_8);
                }
            } finally {

                // Instruct the running XQuery to finish
                @Nullable final CountDownLatch exitLatch = LatchWrappersSingleton.INSTANCE.queryExitLatchRef.get();
                assertNotNull("exitLatch should not be null at this point", exitLatch);
                exitLatch.countDown();
            }
        };

        // Run the readDocumentCallable from a separate thread so that we don't block our test
        final Future<String> documentContentFuture = executorService.submit(readDocumentCallable);
        final String documentContent = documentContentFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(query, documentContent);

        // Check the results of the query
        final String queryResult = queryResultFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(queryResult);
        assertTrue(queryResult.startsWith("<elapsed-time exit-latch-zero=\"true\">PT"));
        assertTrue(queryResult.endsWith("</elapsed-time>"));
    }

    /**
     * When this test is run, the /db/running-xquery-test/latched-query.xq XQuery
     * is already executing (see {@link #runXQuery()}) in a separate thread that holds a READ_LOCK on its Source document.
     *
     * We try and replace the XQuery source document under those conditions, which should not be possible
     * as writing to the document would require a WRITE_LOCK, but a READ_LOCK is already held.
     */
    @Test
    public void replaceRunningXQuerySourceDocument() {
        // Prepare a Callable that will try and replace the XQuery's source document
        final Callable<Boolean> replaceDocumentCallable = () -> {

            final String replacementQuery = "<replaced/>";

            try (final Collection testCollection = DatabaseManager.getCollection(getBaseUri() + TEST_COLLECTION_URI.getCollectionPath(), TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
                 final EXistResource queryResource = (EXistResource) testCollection.getResource(LATCHED_QUERY_URI.getCollectionPath())) {
                queryResource.setMediaType(MediaType.APPLICATION_XQUERY);
                queryResource.setContent(replacementQuery.getBytes(UTF_8));
                testCollection.storeResource(queryResource);
            }

            // NOTE(AR) It should not have been possible to get to this point as we should not be able to acquire a WRITE_LOCK lock on the document (above) when calling broker.storeDocument, as the query thread holds a READ_LOCK on the document

            return true;
        };

        // Run the replaceDocumentCallable from a separate thread so that we don't block our test
        final Future<Boolean> replacedDocumentFuture = executorService.submit(replaceDocumentCallable);
        assertThrows(
                "We should not have been able to replace the document as that requires this thread to obtain a WRITE_LOCK on the document, but the query thread already holds a READ_LOCK on the document",
                TimeoutException.class, () ->
                        replacedDocumentFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        );
    }
}
