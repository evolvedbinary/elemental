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
package org.exist.storage.lock;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class LockTableBenchmark {

    private static final int DATA_SUB_COLLECTIONS = 13;
    private static final int DOCUMENTS = 20;

    private static final int EVENTS_BTREE_READ_LOCK = 593062;
    private static final int EVENTS_COLLECTION_INTENTION_READ_LOCK = 86619;
    private static final int EVENTS_COLLECTION_READ_LOCK = 19630;
    private static final int EVENTS_DOCUMENT_READ_LOCK = 60370;


    @State(Scope.Benchmark)
    public static class LockTableState {
        private final LockTable lockTable = new LockTable(null);
    }

    @State(Scope.Thread)
    public static class EventsState {
        private int btreeReads = 0;
        private int collectionIntentionReads = 0;
        private int collectionReads = 0;
        private int documentReads = 0;

        private int dataSubCollectionIndex = 0;
        private int documentsIndex = 0;
    }

    @Benchmark
    public void testEvent(final LockTableState lockTableState, final EventsState eventsState) {
        while (!(eventsState.collectionIntentionReads >= EVENTS_COLLECTION_INTENTION_READ_LOCK
                && eventsState.collectionReads >= EVENTS_COLLECTION_READ_LOCK
                && eventsState.documentReads >= EVENTS_DOCUMENT_READ_LOCK
                && eventsState.btreeReads >= EVENTS_BTREE_READ_LOCK
        )) {

            final long groupId = System.nanoTime();

            if (eventsState.btreeReads < EVENTS_BTREE_READ_LOCK) {
                lockTableState.lockTable.attempt(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                lockTableState.lockTable.acquired(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                eventsState.btreeReads++;
                lockTableState.lockTable.released(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
            }

            boolean didCollectionIntentionRead = false;
            if (eventsState.collectionIntentionReads < EVENTS_COLLECTION_INTENTION_READ_LOCK) {
                lockTableState.lockTable.attempt(groupId, "/db", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                lockTableState.lockTable.acquired(groupId, "/db", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                eventsState.collectionIntentionReads++;

                if (eventsState.btreeReads < EVENTS_BTREE_READ_LOCK) {
                    lockTableState.lockTable.attempt(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    lockTableState.lockTable.acquired(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    eventsState.btreeReads++;
                    lockTableState.lockTable.released(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                }

                lockTableState.lockTable.attempt(groupId, "/db/apps", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                lockTableState.lockTable.acquired(groupId, "/db/apps", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                lockTableState.lockTable.released(groupId, "/db", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                eventsState.collectionIntentionReads++;

                if (eventsState.btreeReads < EVENTS_BTREE_READ_LOCK) {
                    lockTableState.lockTable.attempt(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    lockTableState.lockTable.acquired(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    eventsState.btreeReads++;
                    lockTableState.lockTable.released(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                }

                lockTableState.lockTable.attempt(groupId, "/db/apps/docs", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                lockTableState.lockTable.acquired(groupId, "/db/apps/docs", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                lockTableState.lockTable.released(groupId, "/db/apps", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                eventsState.collectionIntentionReads++;

                if (eventsState.btreeReads < EVENTS_BTREE_READ_LOCK) {
                    lockTableState.lockTable.attempt(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    lockTableState.lockTable.acquired(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    eventsState.btreeReads++;
                    lockTableState.lockTable.released(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                }

                lockTableState.lockTable.attempt(groupId, "/db/apps/docs/data", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                lockTableState.lockTable.acquired(groupId, "/db/apps/docs/data", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                lockTableState.lockTable.released(groupId, "/db/apps/docs", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
                eventsState.collectionIntentionReads++;

                if (eventsState.btreeReads < EVENTS_BTREE_READ_LOCK) {
                    lockTableState.lockTable.attempt(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    lockTableState.lockTable.acquired(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    eventsState.btreeReads++;
                    lockTableState.lockTable.released(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                }

                didCollectionIntentionRead = true;
            }

            if (eventsState.dataSubCollectionIndex > DATA_SUB_COLLECTIONS) {
                eventsState.dataSubCollectionIndex = 0;
            }
            final String dataSubCollection = "/db/apps/docs/data/" + eventsState.dataSubCollectionIndex++;

            boolean didCollectionRead = false;
            if (eventsState.collectionReads < EVENTS_COLLECTION_READ_LOCK) {

                if (eventsState.btreeReads < EVENTS_BTREE_READ_LOCK) {
                    lockTableState.lockTable.attempt(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    lockTableState.lockTable.acquired(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    eventsState.btreeReads++;
                    lockTableState.lockTable.released(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                }

                lockTableState.lockTable.attempt(groupId, dataSubCollection, Lock.LockType.COLLECTION, Lock.LockMode.READ_LOCK);
                lockTableState.lockTable.acquired(groupId, dataSubCollection, Lock.LockType.COLLECTION, Lock.LockMode.READ_LOCK);
                eventsState.collectionReads++;

                didCollectionRead = true;
            }

            if (didCollectionIntentionRead) {
                lockTableState.lockTable.released(groupId, "/db/apps/docs/data", Lock.LockType.COLLECTION, Lock.LockMode.INTENTION_READ);
            }

            if (eventsState.documentReads < EVENTS_DOCUMENT_READ_LOCK) {
                if (eventsState.btreeReads < EVENTS_BTREE_READ_LOCK) {
                    lockTableState.lockTable.attempt(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    lockTableState.lockTable.acquired(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                    eventsState.btreeReads++;
                    lockTableState.lockTable.released(groupId, "BTREE", Lock.LockType.BTREE, Lock.LockMode.READ_LOCK);
                }

                if (eventsState.documentsIndex > DOCUMENTS) {
                    eventsState.documentsIndex = 0;
                }
                final String document = dataSubCollection + '/' + eventsState.documentsIndex++;
                lockTableState.lockTable.attempt(groupId, document, Lock.LockType.DOCUMENT, Lock.LockMode.READ_LOCK);
                lockTableState.lockTable.acquired(groupId, document, Lock.LockType.DOCUMENT, Lock.LockMode.READ_LOCK);
                eventsState.documentReads++;

                lockTableState.lockTable.released(groupId, document, Lock.LockType.DOCUMENT, Lock.LockMode.READ_LOCK);
            }

            if (didCollectionRead) {
                lockTableState.lockTable.released(groupId, dataSubCollection, Lock.LockType.COLLECTION, Lock.LockMode.READ_LOCK);
            }
        }
    }

    public static void main(final String args[]) {
        // NOTE: just for running with the java debugger
        LockTableBenchmark lockTableBenchmark = new LockTableBenchmark();
        LockTableState lockTableState = new LockTableState();
        EventsState eventsState = new EventsState();

        lockTableBenchmark.testEvent(lockTableState, eventsState);

        lockTableState.lockTable.shutdown();
    }
}
