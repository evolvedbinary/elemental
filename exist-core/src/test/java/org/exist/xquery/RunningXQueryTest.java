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

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.QName;
import org.exist.dom.persistent.BinaryDocument;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.PermissionDeniedException;
import org.exist.source.DbUriSource;
import org.exist.source.Source;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.blob.BlobId;
import org.exist.storage.lock.Lock;
import org.exist.storage.lock.LockManager;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.exist.util.PropertiesBuilder.propertiesBuilder;
import static org.exist.util.io.InputStreamUtil.readAll;
import static org.junit.Assert.*;

/**
 * Performs a number of tests against the read-locked source document of an XQuery that is currently running.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class RunningXQueryTest {

    private static final XmldbURI TEST_COLLECTION_URI = XmldbURI.DB.append("running-xquery-test");
    private static final XmldbURI LATCHED_QUERY_URI = XmldbURI.create("latched-query.xq");
    private static final String RUNNING_LATCH_REF_XQUERY_VARIABLE_NAME = "query-running-latch-ref";
    private static final String EXIT_LATCH_REF_XQUERY_VARIABLE_NAME = "query-exit-latch-ref";

    private static final long TIMEOUT_MS = 3000;

    @Rule
    public final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(
        propertiesBuilder()
            .put(FunctionFactory.PROPERTY_ENABLE_JAVA_BINDING, true)
            .put(BrokerPool.PROPERTY_SHUTDOWN_DELAY, 100L)
            .build(),
        true, true);

    @Nullable private ExecutorService executorService = null;
    private final AtomicReference<LockedDocument> lockedQuerySourceDocumentRef = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> queryRunningLatchRef = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> queryExitLatchRef = new AtomicReference<>();
    @Nullable private Future<Sequence> queryResultFuture = null;

    private final String query =
            "declare namespace system = 'http://exist-db.org/xquery/system';\n" +
            "declare namespace at = 'java:" + AtomicReference.class.getName() + "';\n" +
            "declare namespace cl = 'java:" + CountDownLatch.class.getName() + "';\n" +
            "declare namespace tu = 'java:" + TimeUnit.class.getName() + "';\n" +
            "declare variable $" + RUNNING_LATCH_REF_XQUERY_VARIABLE_NAME + " external;\n" +
            "declare variable $" + EXIT_LATCH_REF_XQUERY_VARIABLE_NAME + " external;\n" +
            "\n" +
            "let $running-latch := at:get($" + RUNNING_LATCH_REF_XQUERY_VARIABLE_NAME + ")\n" +
            "let $_ := cl:countDown($running-latch)\n" +
            "let $start := util:system-time()\n" +
            "let $exit-latch := at:get($" + EXIT_LATCH_REF_XQUERY_VARIABLE_NAME + ")\n" +
            "let $exit-latch-zero := cl:await($exit-latch, " + TIMEOUT_MS + " cast as xs:long, tu:MILLISECONDS())\n" +
            "let $end := util:system-time()\n" +
            "return\n" +
            "  <elapsed-time exit-latch-zero='{$exit-latch-zero}'>{$end - $start}</elapsed-time>";

    @Before
    public void runXQuery() throws PermissionDeniedException, EXistException, IOException, SAXException, LockException, InterruptedException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();

        // Store our XQuery as document into the database
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction();
             final Collection testCollection = broker.getOrCreateCollection(transaction, TEST_COLLECTION_URI)) {

            final MediaType xqueryMediaType = broker.getBrokerPool().getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XQUERY);
            broker.storeDocument(transaction, LATCHED_QUERY_URI, new StringInputSource(query.getBytes(StandardCharsets.UTF_8)), xqueryMediaType, testCollection);

            transaction.commit();
        }

        // Create a latch that we can receive a signal from when the query is running
        this.queryRunningLatchRef.set(new CountDownLatch(1));

        // Create a latch that we can send a signal to allow the query to finish
        this.queryExitLatchRef.set(new CountDownLatch(1));

        // Prepare a Callable that will compile and execute the XQuery (which will wait on the queryExitLatch)
        final Callable<Sequence> latchedQueryCallable = () -> {
            try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
                 final Txn transaction = brokerPool.getTransactionManager().beginTransaction();
                 final Collection testCollection = broker.getOrCreateCollection(transaction, TEST_COLLECTION_URI)) {

                // Retrieve and READ LOCK our query's source document
                final LockedDocument lockedQuerySourceDocument = testCollection.getDocumentWithLock(broker, LATCHED_QUERY_URI, Lock.LockMode.READ_LOCK);
                this.lockedQuerySourceDocumentRef.set(lockedQuerySourceDocument);

                // NOTE: early release of Collection lock inline with Asymmetrical Locking scheme
                testCollection.close();

                // Compile our XQuery
                final XQuery xquery = brokerPool.getXQueryService();
                final CompiledXQuery compiledQuery;
                final XQueryContext xqueryContext = new XQueryContext(brokerPool);

                // Bind the latch references as external variables for the query
                xqueryContext.declareVariable(new QName(RUNNING_LATCH_REF_XQUERY_VARIABLE_NAME), true, this.queryRunningLatchRef);
                xqueryContext.declareVariable(new QName(EXIT_LATCH_REF_XQUERY_VARIABLE_NAME), true, this.queryExitLatchRef);

                final Source querySource = DbUriSource.from(broker.getBrokerPool(), broker.getCurrentSubject(), lockedQuerySourceDocumentRef.get().getDocument(), true, false);
                compiledQuery = xquery.compile(xqueryContext, querySource);

                final Sequence result = xquery.execute(broker, compiledQuery, null);

                transaction.commit();

                return result;
            } catch (final XPathException e) {
                e.printStackTrace();
                throw e;
            }
        };

        // Run the latched query callable from a separate thread so that we don't block our test
        this.executorService = Executors.newFixedThreadPool(2);   // We size to 2 threads so that we have one for the latched query, and one for the test method itself (see tests below)
        this.queryResultFuture = executorService.submit(latchedQueryCallable);

        // Wait for the query to start running
        final CountDownLatch latch = this.queryRunningLatchRef.get();
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
        @Nullable final CountDownLatch runningLatch = this.queryRunningLatchRef.getAndSet(null);
        if (runningLatch != null) {
            while (runningLatch.getCount() > 0) {
                runningLatch.countDown();
            }
        }

        @Nullable final CountDownLatch exitLatch = this.queryExitLatchRef.getAndSet(null);
        if (exitLatch != null) {
            while (exitLatch.getCount() > 0) {
                exitLatch.countDown();
            }
        }

        @Nullable final LockedDocument doc = this.lockedQuerySourceDocumentRef.getAndSet(null);
        if (doc != null) {
            final XmldbURI docUri = doc.getDocument().getURI();
            final LockManager lockManager = existEmbeddedServer.getBrokerPool().getLockManager();
            if (lockManager.isDocumentLockedForRead(docUri) || lockManager.isDocumentLockedForWrite(docUri)) {
                doc.close();
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

            final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();

            // try and read the XQuery source document
            try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
                 final Txn transaction = brokerPool.getTransactionManager().beginTransaction();
                 final Collection testCollection = broker.getOrCreateCollection(transaction, TEST_COLLECTION_URI)) {

                final String documentContent;
                try (final LockedDocument lockedDocument = testCollection.getDocumentWithLock(broker, LATCHED_QUERY_URI, Lock.LockMode.READ_LOCK)) {
                    final DocumentImpl document = lockedDocument.getDocument();
                    assertTrue(document instanceof BinaryDocument);

                    // NOTE: early release of Collection lock inline with Asymmetrical Locking scheme
                    testCollection.close();

                    final BinaryDocument binaryDocument = ((BinaryDocument) document);
                    final BlobId blobId = binaryDocument.getBlobId();
                    try (final InputStream is = brokerPool.getBlobStore().get(transaction, blobId)) {
                        documentContent = new String(readAll(is), StandardCharsets.UTF_8);
                    }
                }

                transaction.commit();

                return documentContent;

            } finally {

                // Instruct the running XQuery to finish
                @Nullable final CountDownLatch exitLatch = queryExitLatchRef.get();
                assertNotNull("exitLatch should not be null at this point", exitLatch);
                exitLatch.countDown();
            }
        };

        // Run the readDocumentCallable from a separate thread so that we don't block our test
        final Future<String> documentContentFuture = executorService.submit(readDocumentCallable);
        final String documentContent = documentContentFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(query, documentContent);

        // Check the results of the query
        final Sequence queryResult = queryResultFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(queryResult);
        assertEquals(1, queryResult.getItemCount());
        final Item resultItem = queryResult.itemAt(0);
        assertEquals(Type.ELEMENT, resultItem.getType());
        final Element resultElement = (Element) resultItem;
        assertTrue(Boolean.parseBoolean(resultElement.getAttribute("exit-latch-zero")));
        assertTrue(resultElement.getTextContent().startsWith("PT"));
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

            final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();

            // try and replace the XQuery source document
            try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
                 final Txn transaction = brokerPool.getTransactionManager().beginTransaction();
                 final Collection testCollection = broker.getOrCreateCollection(transaction, TEST_COLLECTION_URI)) {

                final String replacementQuery = "<replaced/>";

                final MediaType xqueryMediaType = broker.getBrokerPool().getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XQUERY);
                broker.storeDocument(transaction, LATCHED_QUERY_URI, new StringInputSource(replacementQuery.getBytes(StandardCharsets.UTF_8)), xqueryMediaType, testCollection);

                // NOTE(AR) It should not have been possible to get to this point as we should not be able to acquire a WRITE_LOCK lock on the document (above) when calling broker.storeDocument, as the query thread holds a READ_LOCK on the document

                transaction.commit();

                return true;
            }
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
