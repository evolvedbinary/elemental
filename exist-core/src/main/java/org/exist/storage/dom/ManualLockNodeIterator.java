/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.storage.dom;

import net.jcip.annotations.ThreadSafe;
import org.exist.dom.persistent.NodeHandle;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.LockManager;
import org.exist.storage.lock.ManagedLock;
import org.exist.util.LockException;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Class ManualLockNodeIterator is used to iterate over nodes in the DOM storage.
 * This implementation provides a lock which allows the caller to decide when to lock the DOM file.
 */
@ThreadSafe
public class ManualLockNodeIterator extends AbstractNodeIterator {
    private final LockManager lockManager;

    public ManualLockNodeIterator(final DBBroker broker, final DOMFile db, final NodeHandle node, final boolean poolable) {
        super(broker, db, node, poolable);
        this.lockManager = broker.getBrokerPool().getLockManager();
    }

    public ManagedLock<ReentrantLock> acquireReadLock() throws LockException {
        final ManagedLock<ReentrantLock> domFileLock = lockManager.acquireBtreeReadLock(db.getLockName());
        db.setOwnerObject(broker);
        return domFileLock;
    }
}
