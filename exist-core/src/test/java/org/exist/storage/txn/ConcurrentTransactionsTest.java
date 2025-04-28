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
package org.exist.storage.txn;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.ExistEmbeddedServer;
import org.exist.test.TransactionTestDSL;
import org.exist.util.InputStreamSupplierInputSource;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.xmldb.XmldbURI;
import org.junit.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.*;

import static org.exist.test.TransactionTestDSL.ExecutionListener;
import static org.exist.test.TransactionTestDSL.NULL_SCHEDULE_LISTENER;
import static org.exist.test.TransactionTestDSL.STD_OUT_SCHEDULE_LISTENER;
import static org.exist.test.TransactionTestDSL.TransactionOperation.*;
import static org.exist.test.TransactionTestDSL.TransactionScheduleBuilder.biSchedule;
import static org.exist.test.TestConstants.TEST_COLLECTION_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.exist.samples.Samples.SAMPLES;

/**
 * Tests for Transactional Operations on the database.
 *
 * Each transaction executes in its own thread according
 * to the schedule which we describe with the {@link TransactionTestDSL}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ConcurrentTransactionsTest {

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    // flip this to `true` if you want to see a trace of the transaction schedule execution on Standard Out
    private static final boolean DEBUG_TRACING = false;
    private static final ExecutionListener EXECUTION_LISTENER = DEBUG_TRACING ? STD_OUT_SCHEDULE_LISTENER : NULL_SCHEDULE_LISTENER;

    @Test
    public void getDocuments() throws ExecutionException, InterruptedException {
        final String documentUri = "/db/test/hamlet.xml";

        final Tuple2<DocumentImpl, DocumentImpl> result = biSchedule()
                .firstT1(getDocument(documentUri))
                                                                .andThenT2(getDocument(documentUri))
                .andThenT1(commit())
                                                                .andThenT2(commit())
                .build()
            .execute(existEmbeddedServer.getBrokerPool(), EXECUTION_LISTENER);

        assertNotNull(result);
        assertNotNull(result._1);
        assertNotNull(result._2);

        assertEquals(documentUri, result._1.getURI().getCollectionPath());
        assertEquals(documentUri, result._2.getURI().getCollectionPath());
    }

    @Test
    public void getDeleteUpdate() throws ExecutionException, InterruptedException {
        final String documentUri = "/db/test/hamlet.xml";

        final Tuple2<Void, Void> result = biSchedule()
                .firstT1(getDocument(documentUri))
                                                        .andThenT2(getDocument(documentUri))
                .andThenT1(deleteDocument())
                .andThenT1(commit())
                                                        .andThenT2(updateDocument("update value /title[1] with 'updated by t2 in various test'"))
                                                        .andThenT2(commit())
                .build()

            .execute(existEmbeddedServer.getBrokerPool(), EXECUTION_LISTENER);
    }

    @Test
    public void delete_read() throws ExecutionException, InterruptedException {
        final String documentUri = "/db/test/hamlet.xml";

        final Tuple2<Void, DocumentImpl> result = biSchedule()
                .firstT1(getDocument(documentUri))
                .andThenT1(deleteDocument())
                                                                .andThenT2(getDocument(documentUri))
                .build()
            .execute(existEmbeddedServer.getBrokerPool(), EXECUTION_LISTENER);

        assertNull(null, result._1);

        // NOTE: This is null because eXist-db has no real transaction isolation (allows dirty reads), the document delete by t1, is seen by t2 even though t2 started before t1 committed
        assertNull(null, result._2);  // should be null as document was deleted!
    }

    @Test
    public void delete_commit_read() throws ExecutionException, InterruptedException {
        final String documentUri = "/db/test/hamlet.xml";

        final Tuple2<Void, DocumentImpl> result = biSchedule()
                .firstT1(getDocument(documentUri))
                .andThenT1(deleteDocument())
                .andThenT1(commit())
                                                                .andThenT2(getDocument(documentUri))
                .build()
            .execute(existEmbeddedServer.getBrokerPool(), EXECUTION_LISTENER);

        assertNull(result._1);
        assertNull(result._2);  // should be null as document was deleted!
    }

    /**
     * NOTE: Aborting a transaction in eXist-db does not rollback the changes
     * made by the transaction.
     */
    @Test
    public void delete_abort_read() throws ExecutionException, InterruptedException {
        final String documentUri = "/db/test/hamlet.xml";

        final Tuple2<Void, DocumentImpl> result = biSchedule()
                .firstT1(getDocument(documentUri))
                .andThenT1(deleteDocument())
                .andThenT1(abort())
                                                                .andThenT2(getDocument(documentUri))
                .build()
            .execute(existEmbeddedServer.getBrokerPool(), EXECUTION_LISTENER);

        assertNull(result._1);
        assertNull(result._2);
//        assertNotNull(result._2);
//        assertEquals(documentUri, result._2.getURI().getCollectionPath());  // should not be null as transaction T1 was aborted!
    }

    @Before
    public void setupDocs() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException, URISyntaxException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = transact.beginTransaction()) {

            final Collection test = broker.getOrCreateCollection(transaction, TEST_COLLECTION_URI);
            assertNotNull(test);
            broker.saveCollection(transaction, test);

            broker.storeDocument(transaction, XmldbURI.create("hamlet.xml"), new InputStreamSupplierInputSource(SAMPLES::getHamletSample), MimeType.XML_TYPE, test);

            transact.commit(transaction);
        }
    }

    @After
    public void removeDocs() throws EXistException, PermissionDeniedException, IOException, TriggerException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = transact.beginTransaction()) {

            final Collection test = broker.getCollection(TEST_COLLECTION_URI);
            if(test != null) {
                broker.removeCollection(transaction, test);
            }

            transact.commit(transaction);
        }
    }
}
