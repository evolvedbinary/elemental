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
package org.exist.xquery.value;

import org.exist.xquery.Expression;
import org.exist.xquery.OrderSpec;
import org.exist.xquery.XPathException;
import org.junit.Test;

import java.util.Arrays;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class OrderedValueSequenceTest {

    @Test
    public void iterate_loop() throws XPathException {
        final OrderedValueSequence orderedValueSequence = mockOrderedValueSequence(99);

        final SequenceIterator it = orderedValueSequence.iterate();
        int count = 0;
        while (it.hasNext()) {
            it.nextItem();
            count++;
        }

        assertEquals(99, count);
    }

    @Test
    public void iterate_skip_loop() throws XPathException {
        final OrderedValueSequence orderedValueSequence = mockOrderedValueSequence(99);
        final SequenceIterator it = orderedValueSequence.iterate();

        assertEquals(99, it.skippable());

        assertEquals(10, it.skip(10));

        assertEquals(89, it.skippable());

        int count = 0;
        while (it.hasNext()) {
            it.nextItem();
            count++;
        }

        assertEquals(89, count);
    }

    @Test
    public void iterate_loop_skip_loop() throws XPathException {
        final OrderedValueSequence orderedValueSequence = mockOrderedValueSequence(99);
        final SequenceIterator it = orderedValueSequence.iterate();

        int len = 20;
        int count = 0;
        for (int i = 0; it.hasNext() && i < len; i++) {
            it.nextItem();
            count++;
        }
        assertEquals(20, count);

        assertEquals(79, it.skippable());

        assertEquals(10, it.skip(10));

        assertEquals(69, it.skippable());

        count = 0;
        while (it.hasNext()) {
            it.nextItem();
            count++;
        }

        assertEquals(69, count);
    }

    private static OrderedValueSequence mockOrderedValueSequence(final int size) throws XPathException {
        final Expression mockSortExpr = createMock(Expression.class);
        expect(mockSortExpr.eval(null, null)).andReturn(Sequence.EMPTY_SEQUENCE).anyTimes();
        replay(mockSortExpr);

        final OrderedValueSequence orderedValueSequence = new OrderedValueSequence(Arrays.asList(new OrderSpec(null, mockSortExpr)), size);
        for (int i = 0; i < size; i++) {
            final Item item = createMock(Item.class);
            expect(item.getType()).andReturn(Type.ANY_TYPE);
            replay(item);
            orderedValueSequence.add(item);
        }
        return orderedValueSequence;
    }
}
