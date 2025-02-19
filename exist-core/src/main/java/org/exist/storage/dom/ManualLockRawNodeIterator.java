package org.exist.storage.dom;

import net.jcip.annotations.ThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.dom.persistent.NodeHandle;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.LockManager;
import org.exist.storage.lock.ManagedLock;
import org.exist.util.FileUtils;
import org.exist.util.LockException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.locks.ReentrantLock;

@ThreadSafe
public class ManualLockRawNodeIterator extends AbstractRawNodeIterator {
    private final LockManager lockManager;

    private static final Logger LOG = LogManager.getLogger(ManualLockRawNodeIterator.class);

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

    public ManagedLock<ReentrantLock> acquireReadLock() {
        try {
            final ManagedLock<ReentrantLock> domFileLock = lockManager.acquireBtreeReadLock(db.getLockName());
            db.setOwnerObject(broker);
            return domFileLock;
        } catch (LockException e) {
            // IF we cannot get a lock we fail to do something. MUST be an error
            final String msg = MessageFormat.format("Failed to acquire read lock on {}", FileUtils.fileName(db.getFile()));
            LOG.error(msg);
            throw new RuntimeException(msg, e);
        }
    }
}