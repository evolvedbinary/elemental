package org.exist.storage.dom;

import net.jcip.annotations.ThreadSafe;
import org.exist.dom.persistent.NodeHandle;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.LockManager;
import org.exist.storage.lock.ManagedLock;
import org.exist.util.LockException;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

@ThreadSafe
public class ManualLockRawNodeIterator extends AbstractRawNodeIterator {
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
    public ManualLockRawNodeIterator(final DBBroker broker, final DOMFile db, final NodeHandle node) throws IOException {
        super(broker, db);
        this.lockManager = broker.getBrokerPool().getLockManager();
        seek(node);
    }

    public ManagedLock<ReentrantLock> acquireReadLock() throws LockException {
        final ManagedLock<ReentrantLock> domFileLock = lockManager.acquireBtreeReadLock(db.getLockName());
        db.setOwnerObject(broker);
        return domFileLock;
    }
}