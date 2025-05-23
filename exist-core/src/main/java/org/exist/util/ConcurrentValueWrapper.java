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
package org.exist.util;

import com.evolvedbinary.j8fu.function.Consumer2E;
import com.evolvedbinary.j8fu.function.ConsumerE;
import net.jcip.annotations.ThreadSafe;
import org.exist.storage.lock.Lock.LockMode;
import org.exist.storage.lock.ManagedLock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A wrapper which allows read or modify operations
 * in a concurrent and thread-safe manner
 * to an underlying value.
 *
 * @param <T> The type of the underlying value
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@ThreadSafe
public class ConcurrentValueWrapper<T> {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final T value;

    protected ConcurrentValueWrapper(final T value) {
        this.value = value;
    }

    /**
     * Read from the value.
     *
     * @param <U> the return type.
     *
     * @param readFn A function which reads the value
     *     and returns a result.
     *
     * @return the result of the {@code readFn}.
     */
    public <U> U read(final Function<T, U> readFn) {
        try (final ManagedLock<ReadWriteLock> readLock = ManagedLock.acquire(lock, LockMode.READ_LOCK)) {
            return readFn.apply(value);
        }
    }

    /**
     * Write to the value.
     *
     * @param writeFn A function which writes to the value.
     */
    public void write(final Consumer<T> writeFn) {
        try (final ManagedLock<ReadWriteLock> writeLock = ManagedLock.acquire(lock, LockMode.WRITE_LOCK)) {
            writeFn.accept(value);
        }
    }

    /**
     * Write to the value and return a result.
     *
     * @param <U> the return type.
     *
     * @param writeFn A function which writes to the value
     *     and returns a result.
     *
     * @return the result of the write function.
     */
    public <U> U writeAndReturn(final Function<T, U> writeFn) {
        try (final ManagedLock<ReadWriteLock> writeLock = ManagedLock.acquire(lock, LockMode.WRITE_LOCK)) {
            return writeFn.apply(value);
        }
    }

    /**
     * Write to the value.
     *
     * @param writeFn A function which writes to the value.
     *
     * @param <E> An exception which may be thrown by the {@code writeFn}.
     *
     * @throws E if an exception is thrown by the {@code writeFn}.
     */
    public final <E extends Throwable> void writeE(final ConsumerE<T, E> writeFn) throws E {
        try (final ManagedLock<ReadWriteLock> writeLock = ManagedLock.acquire(lock, LockMode.WRITE_LOCK)) {
            writeFn.accept(value);
        }
    }

    /**
     * Write to the value.
     *
     * @param writeFn A function which writes to the value.
     *
     * @param <E1> An exception which may be thrown by the {@code writeFn}.
     * @param <E2> An exception which may be thrown by the {@code writeFn}.
     *
     * @throws E1 if an exception is thrown by the {@code writeFn}.
     * @throws E2 if an exception is thrown by the {@code writeFn}.
     */
    public final <E1 extends Exception, E2 extends Exception> void write2E(final Consumer2E<T, E1, E2> writeFn) throws E1, E2 {
        try (final ManagedLock<ReadWriteLock> writeLock = ManagedLock.acquire(lock, LockMode.WRITE_LOCK)) {
            writeFn.accept(value);
        }
    }
}
