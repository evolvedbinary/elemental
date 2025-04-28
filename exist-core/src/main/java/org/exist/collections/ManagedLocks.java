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
package org.exist.collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.storage.lock.ManagedLock;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Simple container for a List of ManagedLocks
 * which allows ARM (Automatic Resource Management)
 * via {@link AutoCloseable}
 *
 * Locks will be released in the reverse order to which they
 * are provided
 */
public class ManagedLocks<T extends ManagedLock> implements Iterable<T>, AutoCloseable {

    private final static Logger LOG = LogManager.getLogger(ManagedLocks.class);

    private final List<T> managedLocks;

    /**
     * @param managedLocks A list of ManagedLocks which should
     *   be in the same order that they were acquired
     */
    public ManagedLocks(final java.util.List<T> managedLocks) {
        this.managedLocks = managedLocks;
    }

    /**
     * @param managedLocks An array / var-args of ManagedLocks
     *   which should be in the same order that they were acquired
     */
    public ManagedLocks(final T... managedLocks) {
        this.managedLocks = Arrays.asList(managedLocks);
    }

    @Override
    public Iterator<T> iterator() {
        return new ManagedLockIterator();
    }

    private class ManagedLockIterator implements Iterator<T> {
        private final Iterator<T> iterator = managedLocks.iterator();

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public T next() {
            return iterator.next();
        }
    }

    @Override
    public void close() {
        closeAll(managedLocks);
    }

    /**
     * Closes all the locks in the provided list.
     *
     * Locks will be closed in reverse (acquisition) order.
     *
     * If a {@link RuntimeException} occurs when closing
     * any lock. The first exception will be recorded and
     * lock closing will continue. After all locks are closed
     * the first encountered exception is rethrown.
     *
     * @param <T> The type of the ManagedLocks
     * @param managedLocks A list of locks, the list should be ordered in lock acquisition order.
     */
    public static <T extends ManagedLock> void closeAll(final List<T> managedLocks) {
        RuntimeException firstException = null;

        for(int i = managedLocks.size() - 1; i >= 0; i--) {
            final T managedLock = managedLocks.get(i);
            try {
                managedLock.close();
            } catch (final RuntimeException e) {
                LOG.error(e);
                if(firstException == null) {
                    firstException = e;
                }
            }
        }

        if(firstException != null) {
            throw firstException;
        }
    }
}
