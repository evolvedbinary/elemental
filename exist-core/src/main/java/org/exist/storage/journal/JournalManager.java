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
package org.exist.storage.journal;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.storage.BrokerPool;
import org.exist.storage.BrokerPoolService;
import org.exist.storage.BrokerPoolServiceException;
import org.exist.storage.recovery.RecoveryManager;
import org.exist.util.Configuration;
import org.exist.util.ReadOnlyException;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Journal Manager just adds some light-weight
 * wrapping around {@link Journal}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@ThreadSafe
public class JournalManager implements BrokerPoolService {
    private static final Logger LOG = LogManager.getLogger(JournalManager.class);

    @GuardedBy("this") private Path journalDir;
    @GuardedBy("this") private boolean groupCommits;
    // package-private accessibility for testing
    @GuardedBy("this") Journal journal;
    @GuardedBy("this") private boolean journallingDisabled = false;
    @GuardedBy("this") private boolean initialized = false;

    private final List<JournalListener> journalListeners = new CopyOnWriteArrayList<>();

    @Override
    public synchronized void configure(final Configuration configuration) {
        this.journalDir = (Path) Optional.ofNullable(configuration.getProperty(Journal.PROPERTY_RECOVERY_JOURNAL_DIR))
                .orElse(configuration.getProperty(BrokerPool.PROPERTY_DATA_DIR));
        this.groupCommits = configuration.getProperty(BrokerPool.PROPERTY_RECOVERY_GROUP_COMMIT, false);
        if (LOG.isDebugEnabled()) {
            LOG.debug("GroupCommits = {}", groupCommits);
        }
    }

    @Override
    public synchronized void prepare(final BrokerPool pool) throws BrokerPoolServiceException {
        if (!journallingDisabled) {
            try {
                this.journal = new Journal(pool, journalDir);
                this.journal.initialize();
                this.initialized = true;
            } catch(final EXistException | ReadOnlyException e) {
                throw new BrokerPoolServiceException(e);
            }
        }
    }

    public synchronized void disableJournalling() {
        this.journallingDisabled = true;
    }

    /**
     * Write a single entry to the journal
     *
     * @see Journal#writeToLog(Loggable)
     *
     * @param loggable The entry to write in the journal
     *
     * @throws JournalException of the journal entry cannot be written
     */
    public synchronized void journal(final Loggable loggable) throws JournalException {
        if (!journallingDisabled) {
            journal.writeToLog(loggable);
        }
    }

    /**
     * Write a group of entrys to the journal
     *
     * @see Journal#writeToLog(Loggable)
     * @see Journal#flushToLog(boolean)
     *
     * @param loggable The entry to write in the journalGroup
     *
     * @throws JournalException of the journal group cannot be written
     */
    public synchronized void journalGroup(final Loggable loggable) throws JournalException {
        if (!journallingDisabled) {
            journal.writeToLog(loggable);
            if (!groupCommits) {
                journal.flushToLog(true);
            }
        }
    }

    /**
     * @see Journal#checkpoint(long, boolean)
     *
     * Create a new checkpoint. A checkpoint fixes the current database state. All dirty pages
     * are written to disk and the journal file is cleaned.
     *
     * This method is called from
     * {@link org.exist.storage.BrokerPool} within pre-defined periods. It
     * should not be called from somewhere else. The database needs to
     * be in a stable state (all transactions completed, no operations running).
     *
     * @param transactionId The id of the transaction for the checkpoint
     * @param switchFiles Whether a new journal file should be started
     *
     * @throws JournalException of the journal checkpoint cannot be written
     */
    public synchronized void checkpoint(final long transactionId, final boolean switchFiles) throws JournalException {
        if (!journallingDisabled) {
            journal.checkpoint(transactionId, switchFiles);

            // notify each listener, de-registering those who want no further events
            journalListeners.forEach(listener -> {
                if(!listener.afterCheckpoint(transactionId)) {
                    journalListeners.remove(listener);
                }
            });
        }
    }

    /**
     * Flush the Journal.
     *
     * @param fsync true to use fsync
     * @param forceSync true to force an fsync
     *
     * See {@link Journal#flushToLog(boolean, boolean)}.
     */
    public synchronized void flush(final boolean fsync, final boolean forceSync) {
        journal.flushToLog(fsync, forceSync);
    }



    /**
     * Shut down the journal. This will write a checkpoint record
     * to the log, so recovery manager knows the file has been
     * closed in a clean way.
     *
     * @param transactionId The id of the transaction for the shutdown
     * @param checkpoint Whether to write a checkpoint before shutdown
     */
    public synchronized void shutdown(final long transactionId, final boolean checkpoint) {
        if(initialized) {
            journal.shutdown(transactionId, checkpoint);
            initialized = false;
        }
    }

    /**
     * @see Journal#lastWrittenLsn()
     *
     * @return the last written LSN
     */
    public synchronized Lsn lastWrittenLsn() {
        return journal.lastWrittenLsn();
    }



    public RecoveryManager.JournalRecoveryAccessor getRecoveryAccessor(final RecoveryManager recoveryManager) {
        return recoveryManager.new JournalRecoveryAccessor(
                journal::setInRecovery, journal::getFiles, journal::getFile, journal::setCurrentJournalFileNumber,
                () -> { journal.switchFiles(); return null; });
    }

    /**
     * Add a callback which can listen for Journal events.
     *
     * @param listener the journal listener
     */
    public void listen(final JournalListener listener) {
        this.journalListeners.add(listener);
    }

    /**
     * Callback for Journal events
     */
    public interface JournalListener {

        /**
         * Called after the journal has written a checkpoint
         *
         * @param txnId The id of the transaction written in the checkpoint
         *
         * @return true if the listener should continue to receive events, false
         *    if the listener should be de-registered and receive no further events.
         */
        boolean afterCheckpoint(final long txnId);
    }

}
