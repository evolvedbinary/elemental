/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.storage.dom;

import net.jcip.annotations.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.dom.persistent.IStoredNode;
import org.exist.dom.persistent.NodeHandle;
import org.exist.storage.DBBroker;
import org.exist.storage.btree.Value;
import org.exist.storage.lock.LockManager;
import org.exist.storage.lock.ManagedLock;
import org.exist.util.FileUtils;
import org.exist.util.LockException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class NodeIterator is used to iterate over nodes in the DOM storage.
 *
 * This implementation locks the DOM file to read the node and unlocks
 * it afterwards. It is thus safer than DOMFileIterator, since the node's
 * value will not change.
 */
@ThreadSafe
public class GranularRawNodeIterator extends AbstractRawNodeIterator {
    private static final Logger LOG = LogManager.getLogger(GranularNodeIterator.class);

    private final LockManager lockManager;

    /**
     * Construct the iterator. The iterator will be positioned before the specified
     * start node.
     *
     * @param broker the owner object used to acquire a lock on the underlying data file (usually a DBBroker)
     * @param db the underlying data file
     * @param node the start node where the iterator will be positioned.
     * @throws IOException if an I/O error occurs
     */
    public GranularRawNodeIterator(final DBBroker broker, final DOMFile db, final NodeHandle node) throws IOException {
        super(broker, db);
        this.lockManager = broker.getBrokerPool().getLockManager();
        seek(node);
    }

     @Override
    public Value next() {
        try (final ManagedLock<ReentrantLock> domFileLock = lockManager.acquireBtreeReadLock(db.getLockName())) {
            return super.next();
        } catch (final LockException e) {
            // IF we cannot get a lock we fail to do something. MUST be an error
            final String msg = MessageFormat.format("Failed to acquire read lock on {}", FileUtils.fileName(db.getFile()));
            LOG.error(msg);
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    public final void seek(final NodeHandle node) throws IOException {
        try (final ManagedLock<ReentrantLock> domFileLock = lockManager.acquireBtreeReadLock(db.getLockName())) {
            super.seek(node);
        } catch (final LockException e) {
            // IF we cannot get a lock we fail to do something. MUST be an error
            final String msg = MessageFormat.format("Failed to acquire read lock on {}", FileUtils.fileName(db.getFile()));
            LOG.error(msg);
            throw new RuntimeException(msg, e);
        }
    }
}
