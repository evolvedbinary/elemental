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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator over a Collection of array.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class CollectionOfArrayIterator<T> implements Iterator<T> {

    private final Iterator<T[]> itArrays;

    public CollectionOfArrayIterator(final Collection<T[]> collectionOfArrays) {
        if (collectionOfArrays == null) {
            this.itArrays = Collections.emptyIterator();
        } else {
            this.itArrays = collectionOfArrays.iterator();
        }
    }

    private int arrayIdx = -1;  // -1 indicates BoF state
    private T[] array = null;

    @Override
    public boolean hasNext() {
        if (arrayIdx == -1) {
            while (itArrays.hasNext()) {
                array = itArrays.next();
                arrayIdx = 0;
                if (arrayIdx < array.length) {
                    return true;
                }
            }
            return false;
        }

        if (array == null && itArrays.hasNext()) {
            array = itArrays.next();
            arrayIdx = 0;
        }

        if (arrayIdx < array.length) {
            return true;
        } else {
            while (itArrays.hasNext()) {
                array = itArrays.next();
                arrayIdx = 0;
                if (arrayIdx < array.length) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public T next() {
        if (arrayIdx == -1) {
            array = itArrays.next();
            arrayIdx = 0;
        }

        if (arrayIdx < array.length) {
            return array[arrayIdx++];
        }

        while (itArrays.hasNext()) {
            array = itArrays.next();
            arrayIdx = 0;
            if (arrayIdx < array.length) {
                return array[arrayIdx++];
            }
        }

        throw new NoSuchElementException();
    }
}
