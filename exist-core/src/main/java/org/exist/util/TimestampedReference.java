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

import net.jcip.annotations.NotThreadSafe;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * A timestamped reference of which
 * updates to are conditional on the
 * timestamp.
 *
 * @param <V> The type of the object reference.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NotThreadSafe
public class TimestampedReference<V> {
    private long timestamp;
    @Nullable
    private V reference;

    /**
     * Creates a timestamped reference with
     * an initial null reference and millisecond
     * resolution.
     */
    public TimestampedReference() {
        this(false, null);
    }

    /**
     * Creates a timestamped reference with
     * an initial null reference.
     *
     * @param nanoResolution true for nanosecond resolution
     *     or, false for millisecond resolution.
     */
    public TimestampedReference(final boolean nanoResolution) {
        this(nanoResolution, null);
    }

    /**
     * Creates a timestamped reference.
     *
     * @param nanoResolution true for nanosecond resolution
     *     or, false for millisecond resolution.
     * @param reference the initial object reference.
     */
    public TimestampedReference(final boolean nanoResolution, @Nullable final V reference) {
        this.reference = reference;
        this.timestamp = nanoResolution ? System.nanoTime() : System.currentTimeMillis();
    }

    /**
     * Set the reference if it is older than the provided timestamp.
     *
     * @param timestamp The new/current timestamp
     * @param supplier A provider of a new object reference.
     *
     * @return the existing reference if not expired, otherwise the new
     *     reference after it is set.
     */
    @Nullable public V setIfExpired(final long timestamp, final Supplier<V> supplier) {
        if(timestamp > this.timestamp) {
            this.reference = supplier.get();
            this.timestamp = timestamp;
        }
        return this.reference;
    }

    /**
     * Set the reference if it is older than the provided timestamp or null.
     *
     * @param timestamp The new/current timestamp
     * @param supplier A provider of a new object reference.
     *
     * @return the existing reference if not expired, otherwise the new
     *     reference after it is set.
     */
    @Nullable public V setIfExpiredOrNull(final long timestamp, final Supplier<V> supplier) {
        if(timestamp > this.timestamp || this.reference == null) {
            this.reference = supplier.get();
            this.timestamp = timestamp;
        }
        return this.reference;
    }

    /**
     * Get the reference.
     *
     * @return the object reference
     */
    @Nullable public V get() {
        return this.reference;
    }
}
