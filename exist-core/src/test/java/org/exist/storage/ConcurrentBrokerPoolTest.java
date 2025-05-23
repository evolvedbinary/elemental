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
package org.exist.storage;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.journal.Journal;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.*;
import org.exist.xmldb.XmldbURI;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ConcurrentBrokerPoolTest {

    private final ThreadGroup threadGroup = new ThreadGroup("concurrentBrokerPoolTest");
    private final AtomicInteger threadNum = new AtomicInteger();
    private final int MAX_CONCURRENT_THREADS = 6;

    /**
     * Tests storing documents across multiple db instances within the same JVM in parallel.
     *
     * Creates n tasks which are distributed over {@code MAX_CONCURRENT_THREADS} threads.
     *
     * Within the same JVM, each task:
     *   1. Gets a new BrokerPool instance from the global BrokerPools
     *   2. With the BrokerPool instance:
     *     2.1 starts the instance
     *     2.2 stores a document into the instance's /db collection
     *     2.2 stops the instance
     *   3. Returns the instance to the global BrokerPools
     */
    @Test
    public void multiInstanceStore() throws InterruptedException, ExecutionException, DatabaseConfigurationException, PermissionDeniedException, EXistException, IOException, URISyntaxException {
        final ThreadFactory threadFactory = runnable -> new Thread(threadGroup, runnable, "leaseStoreRelease-" + threadNum.getAndIncrement());
        final ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS, threadFactory);

        // the number of instances to use
        final int instances = 10;

        // setup store data tasks
        final List<Callable<Tuple2<Path, UUID>>> tasks = IntStream.range(0, instances)
                .mapToObj(i -> new StoreInstance())
                .collect(Collectors.toList());

        // store data
        final List<Future<Tuple2<Path, UUID>>> futures = executorService.invokeAll(tasks);
        executorService.shutdown();

        // validate stored data
        for (final Future<Tuple2<Path, UUID>> future : futures) {
            validateStoredDoc(future.get());
        }
    }

    private void validateStoredDoc(final Tuple2<Path, UUID> pathUuid) throws EXistException, IOException, DatabaseConfigurationException, PermissionDeniedException, URISyntaxException {
        final Path dataDir = pathUuid._1;
        assertTrue(Files.exists(dataDir));
        final UUID uuid = pathUuid._2;

        final Properties config = new Properties();
        config.put(BrokerPool.PROPERTY_DATA_DIR, dataDir);
        config.put(Journal.PROPERTY_RECOVERY_JOURNAL_DIR, dataDir);

        final ExistEmbeddedServer server = new ExistEmbeddedServer("validate-" + uuid.toString(), getConfigFile(getClass()), config, true, false);
        server.startDb();
        try {
            try (final DBBroker broker = server.getBrokerPool().getBroker()) {
                try (final LockedDocument doc = broker.getXMLResource(XmldbURI.DB.append(docName(uuid)), Lock.LockMode.READ_LOCK)) {
                    assertNotNull(doc);

                    final Source expected = Input.fromString(docContent(uuid)).build();
                    final Source actual = Input.fromNode(doc.getDocument()).build();

                    final Diff diff = DiffBuilder.compare(expected)
                            .withTest(actual)
                            .checkForSimilar()
                            .build();

                    // ASSERT
                    assertFalse(diff.toString(), diff.hasDifferences());

                }
            }
        } finally {
            server.stopDb();

            // clear temp files
            FileUtils.deleteQuietly(dataDir);
        }
    }

    private static XmldbURI docName(final UUID uuid) {
        return XmldbURI.create(uuid.toString() + ".xml");
    }

    private static String docContent(final UUID uuid) {
        return "<uuid>" + uuid.toString() + "</uuid>";
    }

    private static Path getConfigFile(final Class instance) throws URISyntaxException {
        return Paths.get(instance.getResource("ConcurrentBrokerPoolTest.conf.xml").toURI());
    }

    private static class StoreInstance implements Callable<Tuple2<Path, UUID>> {
        private final UUID uuid = UUID.randomUUID();

        @Override
        public Tuple2<Path, UUID> call() throws Exception {
            final ExistEmbeddedServer server = new ExistEmbeddedServer("store-" + uuid.toString(), getConfigFile(getClass()), null, true, true);

            server.startDb();
            try {
                store(server.getBrokerPool());
                return Tuple(server.getTemporaryStorage().get(), uuid);
            } finally {
                server.stopDb(false);  // NOTE: false flag ensures we don't delete the temporary storage!
            }
        }

        private void store(final BrokerPool brokerPool) throws EXistException, PermissionDeniedException, LockException, SAXException, IOException {
            try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
                    final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {
                try (final Collection collection = broker.openCollection(XmldbURI.DB, Lock.LockMode.WRITE_LOCK)){

                    final String docContent = docContent(uuid);

                    broker.storeDocument(transaction, docName(uuid), new StringInputSource(docContent), MimeType.XML_TYPE, collection);

                    transaction.commit();
                }
            }
        }
    }
}
