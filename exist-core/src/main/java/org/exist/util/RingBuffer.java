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
 * Simple Ring Buffer implementation.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NotThreadSafe
public class RingBuffer<T> {
    private final int capacity;
    private final T[] elements;

    private int writePos;
    private int available;

    @SuppressWarnings("unchecked")
    public RingBuffer(final int capacity, final Supplier<T> constructor) {
        this.capacity = capacity;
        this.elements = (T[])new Object[capacity];
        for (int i = 0; i < capacity; i++) {
            elements[i] = constructor.get();
        }

        this.available = capacity;
        this.writePos = capacity;
    }

    public @Nullable T takeEntry() {
        if(available == 0){
            return null;
        }
        int nextSlot = writePos - available;
        if(nextSlot < 0){
            nextSlot += capacity;
        }
        final T nextObj = elements[nextSlot];
        available--;
        return nextObj;
    }

    public void returnEntry(final T element) {
        if(available < capacity){
            if(writePos >= capacity){
                writePos = 0;
            }
            elements[writePos] = element;
            writePos++;
            available++;
        }
    }
}
