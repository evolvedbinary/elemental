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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class TimestampedReferenceTest {

    @Test
    public void setIfExpired_expired() {
        final TimestampedReference<String> timestampedReference = new TimestampedReference<>(true, "Original");
        assertEquals("Original", timestampedReference.get());

        timestampedReference.setIfExpired(System.nanoTime(), () -> "Updated");
        assertEquals("Updated", timestampedReference.get());
    }

    @Test
    public void setIfExpired_notExpired() {
        final long firstTimestamp = System.nanoTime();

        final TimestampedReference<String> timestampedReference = new TimestampedReference<>(true, "Original");
        assertEquals("Original", timestampedReference.get());

        timestampedReference.setIfExpired(firstTimestamp, () -> "Updated");
        assertEquals("Original", timestampedReference.get());
    }

    @Test
    public void setIfExpiredOrNull_expired() {
        final TimestampedReference<String> timestampedReference = new TimestampedReference<>(true, "Original");
        assertEquals("Original", timestampedReference.get());

        timestampedReference.setIfExpiredOrNull(System.nanoTime(), () -> "Updated");
        assertEquals("Updated", timestampedReference.get());
    }

    @Test
    public void setIfExpiredOrNull_notExpired() {
        final long firstTimestamp = System.nanoTime();

        final TimestampedReference<String> timestampedReference = new TimestampedReference<>(true, "Original");
        assertEquals("Original", timestampedReference.get());

        timestampedReference.setIfExpiredOrNull(firstTimestamp, () -> "Updated");
        assertEquals("Original", timestampedReference.get());
    }

    @Test
    public void setIfExpiredOrNull_expiredAndNull() {
        final TimestampedReference<String> timestampedReference = new TimestampedReference<>(true);
        assertEquals(null, timestampedReference.get());

        timestampedReference.setIfExpiredOrNull(System.nanoTime(), () -> "Updated");
        assertEquals("Updated", timestampedReference.get());
    }

    @Test
    public void setIfExpiredOrNull_notExpiredAndNull() {
        final long firstTimestamp = System.nanoTime();

        final TimestampedReference<String> timestampedReference = new TimestampedReference<>(true, null);
        assertEquals(null, timestampedReference.get());

        timestampedReference.setIfExpiredOrNull(firstTimestamp, () -> "Updated");
        assertEquals("Updated", timestampedReference.get());
    }
}
