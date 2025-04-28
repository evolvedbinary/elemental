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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.DatabaseConfigurationException;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;

/**
 * Tests for exercising the BrokerPoolService
 * infrastructure.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class BrokerPoolServiceTest {

    // NOTE: this is a concurrent list because it is shared between the test and the BackgroundJobsBrokerPoolService
    private final List<Future<List<BackgroundJobsBrokerPoolService.TimestampAndId>>> futures = new CopyOnWriteArrayList<>();

    /**
     * This test registers a custom BrokerPoolService
     * and then runs the database.
     *
     * The custom BrokerPoolService launches a bunch of
     * background jobs when the database starts, and then
     * attempts to stop them cleanly when the database
     * stops.
     */
    @Test
    public void backgroundJobsShutdownCleanly() throws EXistException, IOException, DatabaseConfigurationException, InterruptedException, ExecutionException {
        // Create and add our BackgroundJobsBrokerPoolService to the BrokerPool
        final BrokerPoolService testBrokerPoolService = new BackgroundJobsBrokerPoolService(futures);
        final Properties configProps = new Properties();
        configProps.put("exist.testBrokerPoolService", testBrokerPoolService);

        // run the database
        final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(configProps, true, true);
        existEmbeddedServer.startDb();
        try {

            // do nothing for a while (background jobs will be running)
            Thread.sleep(3000);

        } finally {
            existEmbeddedServer.stopDb();
        }

        // check results
        int totalBackgroundJobResults = 0;
        for (final Future<List<BackgroundJobsBrokerPoolService.TimestampAndId>> future : futures) {
            try {
                final List<BackgroundJobsBrokerPoolService.TimestampAndId> backgroundJobResult = future.get();

                // should contain at least 1 result
                assertTrue(backgroundJobResult.size() >= 1);

                totalBackgroundJobResults += backgroundJobResult.size();

            } catch (final CancellationException e) {
                // ignore as the background job was cancelled OK
            } catch (final ExecutionException e) {
                // a background task threw an exception... shouldn't happen, so throw it!
                throw e;
            } catch (final InterruptedException e) {
                // interrupted while waiting on the future, can't recover...

                // restore interrupt flag status
                Thread.currentThread().interrupt();

                throw e;
            }
        }

        assertTrue(totalBackgroundJobResults > 0);
    }

    public static class BackgroundJobsBrokerPoolService implements BrokerPoolService {
        private static final Logger LOG = LogManager.getLogger(BackgroundJobsBrokerPoolService.class);

        private static final int NUM_THREADS = 10;
        private static final int NUM_JOBS = 30;  // It is intentional that there are more jobs than threads
        private static final long JOB_LOOP_DELAY = 1000;

        private final AtomicReference<BrokerPool> brokerPoolRef = new AtomicReference<>();
        private final ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
        private final List<Future<List<BackgroundJobsBrokerPoolService.TimestampAndId>>> futures;
        private volatile boolean stopBackgroundJobs = false;

        public BackgroundJobsBrokerPoolService(final List<Future<List<BackgroundJobsBrokerPoolService.TimestampAndId>>> futures) {
            this.futures = futures;
        }

        @Override
        public void startMultiUser(final BrokerPool brokerPool) throws BrokerPoolServiceException {
            if (!brokerPoolRef.compareAndSet(null, brokerPool)) {
                throw new BrokerPoolServiceException("BrokerPool reference is already set");
            }

            startBackgroundJobs();
        }

        @Override
        public void stopMultiUser(final BrokerPool brokerPool) throws BrokerPoolServiceException {
            if (brokerPoolRef.get() != brokerPool) {
                throw new BrokerPoolServiceException("BrokerPool does not match BrokerPoolRef");
            }

            stopBackgroundJobs();

            if (!brokerPoolRef.compareAndSet(brokerPool, null)) {
                throw new BrokerPoolServiceException("Could not clear BrokerPool reference");
            }
        }

        private void startBackgroundJobs() {
            for (int i = 0; i < NUM_JOBS; i++) {
                final Future<List<TimestampAndId>> future = executorService.submit(new BackgroundJob(i));
                futures.add(future);
            }
        }

        private void stopBackgroundJobs() {
            // let the executor service know we will be shutting down
            executorService.shutdown();

            // signal the background jobs that they should stop
            stopBackgroundJobs = true;

            // wait for all jobs to finish... or timeout!
            final long timeout = NUM_JOBS * JOB_LOOP_DELAY * 2;
            try {
                final boolean cleanFinish = executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
                if (!cleanFinish) {
                    LOG.warn("Clean shutdown of background tasks failed... calling shutdownNow");
                    // do the best we can
                    executorService.shutdownNow();
                }

            } catch (final InterruptedException e) {
                // restore interrupt flag status
                Thread.currentThread().interrupt();

                LOG.error(e.getMessage(), e);

                // ... nothing else that we can do!
                executorService.shutdownNow();
            }
        }

        private class BackgroundJob implements Callable<List<TimestampAndId>> {
            private final List<TimestampAndId> list = new ArrayList<>();
            private final int jobId;

            public BackgroundJob(final int jobId) {
                this.jobId = jobId;
            }

            @Override
            public List<TimestampAndId> call() throws Exception {
                do {
                    final BrokerPool brokerPool = brokerPoolRef.get();
                    if (brokerPool == null) {
                        throw new Exception("Unable to get BrokerPoolRef");
                    }

                    try (final DBBroker broker = brokerPool.getBroker()) {
                        final long timestamp = System.nanoTime();
                        final String id = "job_" + jobId + "_" + broker.getId();

                        list.add(new TimestampAndId(timestamp, id));
                    }

                    // just to slow things down a little
                    Thread.sleep(JOB_LOOP_DELAY);

                } while (!stopBackgroundJobs);

                return list;
            }
        }

        private static class TimestampAndId {
            public final long timestamp;
            public final String id;

            private TimestampAndId(final long timestamp, final String id) {
                this.timestamp = timestamp;
                this.id = id;
            }
        }
    }
}
