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
package org.exist.xquery;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.exist.xquery.Cardinality.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class CardinalityTest {

    @Test
    public void atLeastOne() {
        assertFalse(EMPTY_SEQUENCE.atLeastOne());
        assertFalse(ZERO_OR_ONE.atLeastOne());
        assertTrue(ONE_OR_MORE.atLeastOne());
        assertFalse(ZERO_OR_MORE.atLeastOne());
    }

    @Test
    public void atMostOne() {
        assertTrue(EMPTY_SEQUENCE.atMostOne());
        assertTrue(ZERO_OR_ONE.atMostOne());
        assertFalse(ONE_OR_MORE.atMostOne());
        assertFalse(ZERO_OR_MORE.atMostOne());
    }

    @Test
    public void isSubCardinalityOrEqualOf() {
        isSubCardinalityOrEqualOf(EMPTY_SEQUENCE, EMPTY_SEQUENCE);
        notSubCardinalityOrEqualOf(EMPTY_SEQUENCE, EXACTLY_ONE);
        isSubCardinalityOrEqualOf(EMPTY_SEQUENCE, ZERO_OR_ONE);
        notSubCardinalityOrEqualOf(EMPTY_SEQUENCE, ONE_OR_MORE);
        isSubCardinalityOrEqualOf(EMPTY_SEQUENCE, ZERO_OR_MORE);

        notSubCardinalityOrEqualOf(EXACTLY_ONE, EMPTY_SEQUENCE);
        isSubCardinalityOrEqualOf(EXACTLY_ONE, EXACTLY_ONE);
        isSubCardinalityOrEqualOf(EXACTLY_ONE, ZERO_OR_ONE);
        isSubCardinalityOrEqualOf(EXACTLY_ONE, ONE_OR_MORE);
        isSubCardinalityOrEqualOf(EXACTLY_ONE, ZERO_OR_MORE);

        notSubCardinalityOrEqualOf(ZERO_OR_ONE, EMPTY_SEQUENCE);
        notSubCardinalityOrEqualOf(ZERO_OR_ONE, EXACTLY_ONE);
        isSubCardinalityOrEqualOf(ZERO_OR_ONE, ZERO_OR_ONE);
        notSubCardinalityOrEqualOf(ZERO_OR_ONE, ONE_OR_MORE);
        isSubCardinalityOrEqualOf(ZERO_OR_ONE, ZERO_OR_MORE);

        notSubCardinalityOrEqualOf(ONE_OR_MORE, EMPTY_SEQUENCE);
        notSubCardinalityOrEqualOf(ONE_OR_MORE, EXACTLY_ONE);
        notSubCardinalityOrEqualOf(ONE_OR_MORE, ZERO_OR_ONE);
        isSubCardinalityOrEqualOf(ONE_OR_MORE, ONE_OR_MORE);
        isSubCardinalityOrEqualOf(ONE_OR_MORE, ZERO_OR_MORE);

        notSubCardinalityOrEqualOf(ZERO_OR_MORE, EMPTY_SEQUENCE);
        notSubCardinalityOrEqualOf(ZERO_OR_MORE, EXACTLY_ONE);
        notSubCardinalityOrEqualOf(ZERO_OR_MORE, ZERO_OR_ONE);
        notSubCardinalityOrEqualOf(ZERO_OR_MORE, ONE_OR_MORE);
        isSubCardinalityOrEqualOf(ZERO_OR_MORE, ZERO_OR_MORE);
    }

    private static void isSubCardinalityOrEqualOf(final Cardinality subject, final Cardinality test) {
        assertTrue(subject.name() + ".isSubCardinalityOrEqualOf(" + test.name() + ") == false, expected true",
                subject.isSubCardinalityOrEqualOf(test));
    }

    private static void notSubCardinalityOrEqualOf(final Cardinality subject, final Cardinality test) {
        assertFalse(subject.name() + ".isSubCardinalityOrEqualOf(" + test.name() + ") == true, expected false",
                subject.isSubCardinalityOrEqualOf(test));
    }

    @Test
    public void isSuperCardinalityOf() {
        isSuperCardinalityOrEqualOf(EMPTY_SEQUENCE, EMPTY_SEQUENCE);
        notSuperCardinalityOrEqualOf(EMPTY_SEQUENCE, EXACTLY_ONE);
        notSuperCardinalityOrEqualOf(EMPTY_SEQUENCE, ZERO_OR_ONE);
        notSuperCardinalityOrEqualOf(EMPTY_SEQUENCE, ONE_OR_MORE);
        notSuperCardinalityOrEqualOf(EMPTY_SEQUENCE, ZERO_OR_MORE);

        notSuperCardinalityOrEqualOf(EXACTLY_ONE, EMPTY_SEQUENCE);
        isSuperCardinalityOrEqualOf(EXACTLY_ONE, EXACTLY_ONE);
        notSuperCardinalityOrEqualOf(EXACTLY_ONE, ZERO_OR_ONE);
        notSuperCardinalityOrEqualOf(EXACTLY_ONE, ONE_OR_MORE);
        notSuperCardinalityOrEqualOf(EXACTLY_ONE, ZERO_OR_MORE);

        isSuperCardinalityOrEqualOf(ZERO_OR_ONE, EMPTY_SEQUENCE);
        isSuperCardinalityOrEqualOf(ZERO_OR_ONE, EXACTLY_ONE);
        isSuperCardinalityOrEqualOf(ZERO_OR_ONE, ZERO_OR_ONE);
        notSuperCardinalityOrEqualOf(ZERO_OR_ONE, ONE_OR_MORE);
        notSuperCardinalityOrEqualOf(ZERO_OR_ONE, ZERO_OR_MORE);

        notSuperCardinalityOrEqualOf(ONE_OR_MORE, EMPTY_SEQUENCE);
        isSuperCardinalityOrEqualOf(ONE_OR_MORE, EXACTLY_ONE);
        notSuperCardinalityOrEqualOf(ONE_OR_MORE, ZERO_OR_ONE);
        isSuperCardinalityOrEqualOf(ONE_OR_MORE, ONE_OR_MORE);
        notSuperCardinalityOrEqualOf(ONE_OR_MORE, ZERO_OR_MORE);

        isSuperCardinalityOrEqualOf(ZERO_OR_MORE, EMPTY_SEQUENCE);
        isSuperCardinalityOrEqualOf(ZERO_OR_MORE, EXACTLY_ONE);
        isSuperCardinalityOrEqualOf(ZERO_OR_MORE, ZERO_OR_ONE);
        isSuperCardinalityOrEqualOf(ZERO_OR_MORE, ONE_OR_MORE);
        isSuperCardinalityOrEqualOf(ZERO_OR_MORE, ZERO_OR_MORE);
    }

    private static void isSuperCardinalityOrEqualOf(final Cardinality subject, final Cardinality test) {
        assertTrue(subject.name() + ".isSuperCardinalityOrEqualOf(" + test.name() + ") == false, expected true",
                subject.isSuperCardinalityOrEqualOf(test));
    }

    private static void notSuperCardinalityOrEqualOf(final Cardinality subject, final Cardinality test) {
        assertFalse(subject.name() + ".isSuperCardinalityOrEqualOf(" + test.name() + ") == true, expected false",
                subject.isSuperCardinalityOrEqualOf(test));
    }

    @Test
    public void superCardinalityOf() {
        assertEquals(EMPTY_SEQUENCE, Cardinality.superCardinalityOf(EMPTY_SEQUENCE, EMPTY_SEQUENCE));
        assertEquals(ZERO_OR_ONE, Cardinality.superCardinalityOf(EMPTY_SEQUENCE, EXACTLY_ONE));
        assertEquals(ZERO_OR_ONE, Cardinality.superCardinalityOf(EMPTY_SEQUENCE, ZERO_OR_ONE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(EMPTY_SEQUENCE, ONE_OR_MORE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(EMPTY_SEQUENCE, ZERO_OR_MORE));

        assertEquals(ZERO_OR_ONE, Cardinality.superCardinalityOf(EXACTLY_ONE, EMPTY_SEQUENCE));
        assertEquals(EXACTLY_ONE, Cardinality.superCardinalityOf(EXACTLY_ONE, EXACTLY_ONE));
        assertEquals(ZERO_OR_ONE, Cardinality.superCardinalityOf(EXACTLY_ONE, ZERO_OR_ONE));
        assertEquals(ONE_OR_MORE, Cardinality.superCardinalityOf(EXACTLY_ONE, ONE_OR_MORE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(EXACTLY_ONE, ZERO_OR_MORE));

        assertEquals(ZERO_OR_ONE, Cardinality.superCardinalityOf(ZERO_OR_ONE, EMPTY_SEQUENCE));
        assertEquals(ZERO_OR_ONE, Cardinality.superCardinalityOf(ZERO_OR_ONE, EXACTLY_ONE));
        assertEquals(ZERO_OR_ONE, Cardinality.superCardinalityOf(ZERO_OR_ONE, ZERO_OR_ONE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(ZERO_OR_ONE, ONE_OR_MORE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(ZERO_OR_ONE, ZERO_OR_MORE));

        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(ONE_OR_MORE, EMPTY_SEQUENCE));
        assertEquals(ONE_OR_MORE, Cardinality.superCardinalityOf(ONE_OR_MORE, EXACTLY_ONE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(ONE_OR_MORE, ZERO_OR_ONE));
        assertEquals(ONE_OR_MORE, Cardinality.superCardinalityOf(ONE_OR_MORE, ONE_OR_MORE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(ONE_OR_MORE, ZERO_OR_MORE));

        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(ZERO_OR_MORE, EMPTY_SEQUENCE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(ZERO_OR_MORE, EXACTLY_ONE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(ZERO_OR_MORE, ZERO_OR_ONE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(ZERO_OR_MORE, ONE_OR_MORE));
        assertEquals(ZERO_OR_MORE, Cardinality.superCardinalityOf(ZERO_OR_MORE, ZERO_OR_MORE));
    }
}
