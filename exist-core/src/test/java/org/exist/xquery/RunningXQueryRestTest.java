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

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.exist.TestUtils;
import org.exist.storage.BrokerPool;
import org.exist.test.ExistWebServer;
import org.exist.xmldb.XmldbURI;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import xyz.elemental.mediatype.MediaType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.exist.util.PropertiesBuilder.propertiesBuilder;
import static org.junit.Assert.*;

/**
 * Performs a number of tests against the read-locked source document of an XQuery that is currently running over the REST API.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class RunningXQueryRestTest {

    private static final XmldbURI TEST_COLLECTION_URI = XmldbURI.DB.append("running-xquery-test");
    private static final XmldbURI LATCHED_QUERY_URI = XmldbURI.create("latched-query.xq");
    private static final String RUNNING_LATCH_REF_XQUERY_VARIABLE_NAME = "query-running-latch-ref";
    private static final String EXIT_LATCH_REF_XQUERY_VARIABLE_NAME = "query-exit-latch-ref";

    private static final long TIMEOUT_MS = 3000;

    @Rule
    public final ExistWebServer existWebServer = new ExistWebServer(true, false,
            propertiesBuilder()
                    .put(FunctionFactory.PROPERTY_ENABLE_JAVA_BINDING, true)
                    .put(BrokerPool.PROPERTY_SHUTDOWN_DELAY, 100L)
                    .build(),
            true, true, false);

    @Nullable private Executor executor = null;
    @Nullable private String uri = null;
    @Nullable private String docUri = null;
    @Nullable private String latchedQueryLastModified = null;
    @Nullable private ExecutorService executorService = null;
    @Nullable private Future<byte[]> queryResultFuture = null;

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

    @Before
    public void runXQuery() throws IOException, InterruptedException {
        this.executor = Executor
                .newInstance()
                .auth(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)
                .authPreemptive(new HttpHost("localhost", existWebServer.getPort()));

        this.uri = "http://localhost:" + existWebServer.getPort() + "/exist/rest" + TEST_COLLECTION_URI.getCollectionPath();
        this.docUri = uri + LATCHED_QUERY_URI.getCollectionPath();

        // Store our XQuery as document into the database
        final HttpResponse storeResponse = executor.execute(
                Request
                        .Put(docUri)
                        .addHeader("Content-Type", MediaType.APPLICATION_XQUERY)
                        .bodyByteArray(query.getBytes(UTF_8))
        ).returnResponse();
        assertEquals(SC_CREATED, storeResponse.getStatusLine().getStatusCode());

        // Record the last modified time of the XQuery in the database
        final HttpResponse headResponse = executor.execute(
                Request
                        .Head(docUri)
                        .addHeader("Accept", MediaType.APPLICATION_XML)
        ).returnResponse();
        assertEquals(SC_OK, headResponse.getStatusLine().getStatusCode());
        this.latchedQueryLastModified = headResponse.getFirstHeader("Last-Modified").getValue();

        // Create a latch that we can receive a signal from when the query is running
        LatchWrappersSingleton.INSTANCE.queryRunningLatchRef.set(new CountDownLatch(1));

        // Create a latch that we can send a signal to allow the query to finish
        LatchWrappersSingleton.INSTANCE.queryExitLatchRef.set(new CountDownLatch(1));

        // Prepare a Callable that will compile and execute the XQuery (which will wait on the queryExitLatch)
        final Callable<byte[]> latchedQueryCallable = () -> {

            final HttpResponse getResponse = executor.execute(
                    Request
                            .Get(docUri)
                            .addHeader("Accept", MediaType.APPLICATION_XML)
            ).returnResponse();
            assertEquals(SC_OK, getResponse.getStatusLine().getStatusCode());

            try (final UnsynchronizedByteArrayOutputStream baos = UnsynchronizedByteArrayOutputStream.builder().get()) {
                getResponse.getEntity().writeTo(baos);
                return baos.toByteArray();
            } catch (final Exception e) {
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
        this.latchedQueryLastModified = null;
        this.docUri = null;
        this.uri = null;
        this.executor = null;
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
                final HttpResponse headResponse = executor.execute(
                        Request
                                .Head(docUri)
                                .addHeader("Accept", MediaType.APPLICATION_XML)
                ).returnResponse();
                assertEquals(SC_OK, headResponse.getStatusLine().getStatusCode());

                return headResponse.getFirstHeader("Last-Modified").getValue();

            } finally {

                // Instruct the running XQuery to finish
                @Nullable final CountDownLatch exitLatch = LatchWrappersSingleton.INSTANCE.queryExitLatchRef.get();
                assertNotNull("exitLatch should not be null at this point", exitLatch);
                exitLatch.countDown();
            }
        };

        // Run the readDocumentCallable from a separate thread so that we don't block our test
        final Future<String> documentLastModifiedFuture = executorService.submit(readDocumentCallable);
        final String documentLastModified = documentLastModifiedFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(latchedQueryLastModified, documentLastModified);

        // Check the results of the query
        final byte[] queryResult = queryResultFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(queryResult);
        final String queryResultStr = new String(queryResult, UTF_8);
        assertTrue(queryResultStr.startsWith("<elapsed-time exit-latch-zero=\"true\">PT"));
        assertTrue(queryResultStr.endsWith("</elapsed-time>"));
    }

    /**
     * When this test is run, the /db/running-xquery-test/latched-query.xq XQuery
     * is already executing (see {@link #runXQuery()}) in a separate thread,
     * that should have released the READ_LOCK on its Source document after query compilation.
     *
     * We try and replace the XQuery source document under those conditions, which should be possible
     * as no lock is held on the source document after compilation.
     */
    @Test
    public void replaceRunningXQuerySourceDocument() throws InterruptedException, ExecutionException, TimeoutException {
        // Prepare a Callable that will try and replace the XQuery's source document
        final Callable<Integer> replaceDocumentCallable = () -> {

            try {
                final String replacementQuery = "<replaced/>";

                final HttpResponse storeResponse = executor.execute(
                        Request
                                .Put(docUri)
                                .addHeader("Content-Type", MediaType.APPLICATION_XQUERY)
                                .bodyByteArray(replacementQuery.getBytes(UTF_8))
                ).returnResponse();

                return storeResponse.getStatusLine().getStatusCode();

            } finally {
                // Instruct the running XQuery to finish
                @Nullable final CountDownLatch exitLatch = LatchWrappersSingleton.INSTANCE.queryExitLatchRef.get();
                assertNotNull("exitLatch should not be null at this point", exitLatch);
                exitLatch.countDown();
            }
        };

        // Run the replaceDocumentCallable from a separate thread so that we don't block our test
        final Future<Integer> replacedDocumentFuture = executorService.submit(replaceDocumentCallable);
        final Integer replaceDocumentStatus = replacedDocumentFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(SC_CREATED, replaceDocumentStatus.intValue());

        // Check the results of the query
        final byte[] queryResult = queryResultFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(queryResult);
        final String queryResultStr = new String(queryResult, UTF_8);
        assertTrue(queryResultStr.startsWith("<elapsed-time exit-latch-zero=\"true\">PT"));
        assertTrue(queryResultStr.endsWith("</elapsed-time>"));
    }
}
