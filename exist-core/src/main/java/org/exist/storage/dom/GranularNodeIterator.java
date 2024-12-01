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
import org.exist.storage.lock.LockManager;
import org.exist.storage.lock.ManagedLock;
import org.exist.util.FileUtils;
import org.exist.util.LockException;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Class NodeIterator is used to iterate over nodes in the DOM storage.
 *
 * This implementation locks the DOM file to read the node and unlocks
 * it afterwards. It is thus safer than DOMFileIterator, since the node's
 * value will not change.
 */
@ThreadSafe
public class GranularNodeIterator extends AbstractNodeIterator {
    private static final Logger LOG = LogManager.getLogger(GranularNodeIterator.class);

    private final LockManager lockManager;

    public GranularNodeIterator(final DBBroker broker, final DOMFile db, final NodeHandle node, final boolean poolable) {
        super(broker, db, node, poolable);
        this.lockManager = broker.getBrokerPool().getLockManager();

    }

    @Override
    public boolean hasNext() {
        try (final ManagedLock<ReentrantLock> domFileLock = lockManager.acquireBtreeReadLock(db.getLockName())) {
            return super.hasNext();
        } catch (final LockException e) {
            LOG.warn("Failed to acquire read lock on {}", FileUtils.fileName(db.getFile()));
            return false;
        }
    }

    @Override
    public IStoredNode next() {
        try (final ManagedLock<ReentrantLock> domFileLock = lockManager.acquireBtreeReadLock(db.getLockName())) {
            return super.next();
        } catch (final LockException e) {
            LOG.warn("Failed to acquire read lock on {}", FileUtils.fileName(db.getFile()));
            //TODO : throw exception here ? -pb
            return null;
        }
    }
}
