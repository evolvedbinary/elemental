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

import org.exist.util.LockException;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides a simple wrapper around a Lock
 * so that it may be used in a try-with-resources
 * statement
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ManagedLock<T> implements AutoCloseable {
    protected final T lock;
    private final Runnable closer;
    protected volatile boolean closed = false;

    ManagedLock(final T lock, final Runnable closer) {
        this.lock = lock;
        this.closer = closer;
    }

    /**
     * Acquires and manages a lock with a specific mode
     *
     * @param lock The lock to call {@link Lock#acquire(Lock.LockMode)} on
     * @param mode the mode of the lock
     *
     * @return A managed lock which will be released with {@link #close()}
     *
     * @throws LockException if a lock error occurs
     */
    public static ManagedLock<Lock> acquire(final Lock lock, final Lock.LockMode mode) throws LockException {
        if(!lock.acquire(mode)) {
            throw new LockException("Unable to acquire lock");
        }
        return new ManagedLock<>(lock, () -> lock.release(mode));
    }

    /**
     * Acquires and manages a lock with a specific mode
     *
     * @param lock The lock to call {@link Lock#acquire(Lock.LockMode)} on
     * @param mode the mode of the lock
     * @param type the type of the lock
     *
     * @return A managed lock which will be released with {@link #close()}
     * @throws LockException if a lock error occurs
     */
    public static ManagedLock<Lock> acquire(final Lock lock, final Lock.LockMode mode, final Lock.LockType type) throws LockException {
        if(!lock.acquire(mode)) {
            throw new LockException("Unable to acquire lock: " + type);
        }
        return new ManagedLock<>(lock, () -> lock.release(mode));
    }

    /**
     * Attempts to acquire and manage a lock with a specific mode
     *
     * @param lock The lock to call {@link Lock#attempt(Lock.LockMode)} on
     * @param mode the mode of the lock
     *
     * @return A managed lock which will be released with {@link #close()}
     * @throws LockException if a lock error occurs
     */
    public static ManagedLock<Lock> attempt(final Lock lock, final Lock.LockMode mode) throws LockException {
        if(!lock.attempt(mode)) {
            throw new LockException("Unable to attempt to acquire lock");
        }
        return new ManagedLock<>(lock, () -> lock.release(mode));
    }

    /**
     * Acquires and manages a lock with a specific mode
     *
     * @param lock The lock to call {@link java.util.concurrent.locks.Lock#lock()} on
     * @param mode the mode of the lock
     *
     * @return A managed lock which will be released with {@link #close()}
     */
    public static ManagedLock<java.util.concurrent.locks.ReadWriteLock> acquire(final java.util.concurrent.locks.ReadWriteLock lock, final Lock.LockMode mode) {
        final java.util.concurrent.locks.Lock modeLock = switch (mode) {
            case READ_LOCK -> lock.readLock();
            case WRITE_LOCK -> lock.writeLock();
            default -> throw new IllegalArgumentException();
        };

        modeLock.lock();
        return new ManagedLock<>(lock, modeLock::unlock);
    }

    /**
     * Attempts to acquire and manage a lock with a specific mode
     *
     * @param lock The lock to call {@link java.util.concurrent.locks.Lock#tryLock()} on
     * @param mode the mode of the lock
     *
     * @return A managed lock which will be released with {@link #close()}
     * @throws LockException if a lock error occurs
     */
    public static ManagedLock<java.util.concurrent.locks.ReadWriteLock> attempt(final java.util.concurrent.locks.ReadWriteLock lock, final Lock.LockMode mode) throws LockException {
        final java.util.concurrent.locks.Lock modeLock = switch (mode) {
            case READ_LOCK -> lock.readLock();
            case WRITE_LOCK -> lock.writeLock();
            default -> throw new IllegalArgumentException();
        };

        if(!modeLock.tryLock()) {
            throw new LockException("Unable to attempt to acquire lock");
        }
        return new ManagedLock<>(lock, modeLock::unlock);
    }

    /**
     * Acquires and manages a lock
     *
     * @param lock The lock to call {@link java.util.concurrent.locks.Lock#lock()} on
     *
     * @return A managed lock which will be released with {@link #close()}
     */
    public static ManagedLock<ReentrantLock> acquire(final ReentrantLock lock) {
        lock.lock();
        return new ManagedLock<>(lock, lock::unlock);
    }

    /**
     * Attempts to acquire and manage a lock
     *
     * @param lock The lock to call {@link java.util.concurrent.locks.Lock#tryLock()} on
     *
     * @return A managed lock which will be released with {@link #close()}
     * @throws LockException if a lock error occurs
     */
    public static ManagedLock<ReentrantLock> attempt(final ReentrantLock lock) throws LockException {
        if(!lock.tryLock()) {
            throw new LockException("Unable to attempt to acquire lock");
        }
        return new ManagedLock<>(lock, lock::unlock);
    }

    /**
     * Determines if the lock has already been released
     *
     * @return true if the lock has already been released
     */
    boolean isReleased() {
        return closed;
    }

    /**
     * Releases the lock
     */
    @Override
    public void close() {
        if (!closed) {
            closer.run();
        }
        this.closed = true;
    }
}
