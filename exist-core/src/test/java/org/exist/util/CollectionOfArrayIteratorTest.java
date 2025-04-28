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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class CollectionOfArrayIteratorTest {

    @Test
    public void nullCollection() {
        final CollectionOfArrayIterator<String> it = new CollectionOfArrayIterator<>(null);
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void emptyCollection() {
        final CollectionOfArrayIterator<String> it = new CollectionOfArrayIterator<>(Collections.emptyList());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void oneEmptyArray() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[0]
        ));
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void oneArray() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[] {1,2,3,4,5}
        ));
        assertTrue(it.hasNext());

        assertEquals(1, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(2, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(3, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(4, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(5, (int)it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void twoArrays() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[] {1,2,3,4,5},
                new Integer[] {66,77,88,99,111}
        ));
        assertTrue(it.hasNext());

        assertEquals(1, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(2, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(3, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(4, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(5, (int)it.next());
        assertTrue(it.hasNext());

        assertEquals(66, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(77, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(88, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(99, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(111, (int)it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void twoArraysOverlap() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[] {1,2,3,4,5},
                new Integer[] {5,6,7,8,9}
        ));
        assertTrue(it.hasNext());

        assertEquals(1, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(2, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(3, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(4, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(5, (int)it.next());

        assertTrue(it.hasNext());
        assertEquals(5, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(6, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(7, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(8, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(9, (int)it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void twoArraysBothEmpty() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[0],
                new Integer[0]
        ));
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void twoArraysFirstEmpty() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[0],
                new Integer[] {6,7,8,9,10}
        ));
        assertTrue(it.hasNext());

        assertEquals(6, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(7, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(8, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(9, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(10, (int)it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void twoArraysSecondEmpty() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[] {1,2,3,4,5},
                new Integer[0]
        ));
        assertTrue(it.hasNext());

        assertEquals(1, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(2, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(3, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(4, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(5, (int)it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void threeArrays() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[] {1,2,3,4,5},
                new Integer[] {66,77,88,99,111},
                new Integer[] {666,777,888,999,1111}
        ));
        assertTrue(it.hasNext());

        assertEquals(1, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(2, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(3, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(4, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(5, (int)it.next());
        assertTrue(it.hasNext());

        assertEquals(66, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(77, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(88, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(99, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(111, (int)it.next());
        assertTrue(it.hasNext());

        assertEquals(666, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(777, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(888, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(999, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(1111, (int)it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void threeArraysOverlap() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[] {1,2,3,4,5},
                new Integer[] {5,6,7,8,9},
                new Integer[] {9,10,11,12,13}
        ));
        assertTrue(it.hasNext());

        assertEquals(1, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(2, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(3, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(4, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(5, (int)it.next());

        assertTrue(it.hasNext());
        assertEquals(5, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(6, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(7, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(8, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(9, (int)it.next());
        assertTrue(it.hasNext());

        assertTrue(it.hasNext());
        assertEquals(9, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(10, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(11, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(12, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(13, (int)it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void threeArraysAllEmpty() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[0],
                new Integer[0],
                new Integer[0]
        ));
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void threeArraysFirstEmpty() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[0],
                new Integer[] {66,77,88,99,111},
                new Integer[] {666,777,888,999,1111}
        ));
        assertTrue(it.hasNext());

        assertEquals(66, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(77, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(88, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(99, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(111, (int)it.next());
        assertTrue(it.hasNext());

        assertEquals(666, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(777, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(888, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(999, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(1111, (int)it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void threeArraysSecondEmpty() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[] {1,2,3,4,5},
                new Integer[0],
                new Integer[] {666,777,888,999,1111}
        ));
        assertTrue(it.hasNext());

        assertEquals(1, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(2, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(3, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(4, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(5, (int)it.next());
        assertTrue(it.hasNext());

        assertEquals(666, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(777, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(888, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(999, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(1111, (int)it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    @Test
    public void threeArraysLastEmpty() {
        final CollectionOfArrayIterator<Integer> it = new CollectionOfArrayIterator<>(listOf(
                new Integer[] {1,2,3,4,5},
                new Integer[] {66,77,88,99,111},
                new Integer[0]
        ));
        assertTrue(it.hasNext());

        assertEquals(1, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(2, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(3, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(4, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(5, (int)it.next());
        assertTrue(it.hasNext());

        assertEquals(66, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(77, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(88, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(99, (int)it.next());
        assertTrue(it.hasNext());
        assertEquals(111, (int)it.next());
        assertFalse(it.hasNext());

        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (final NoSuchElementException e) {
            // no op
        }
    }

    private static <T> List<T[]> listOf(final T[]... items) {
        return Arrays.asList(items);
    }
}
