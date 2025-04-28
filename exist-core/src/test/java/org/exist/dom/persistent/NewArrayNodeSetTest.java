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
package org.exist.dom.persistent;

import org.exist.xquery.Constants;
import org.exist.xquery.value.SequenceIterator;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class NewArrayNodeSetTest {

    @Test
    public void iterate_loop() {
        final NewArrayNodeSet newArrayNodeSet = mockNewArrayNodeSet(99);

        final SequenceIterator it = newArrayNodeSet.iterate();
        int count = 0;
        while (it.hasNext()) {
            it.nextItem();
            count++;
        }

        assertEquals(99, count);
    }

    @Test
    public void iterate_skip_loop() {
        final NewArrayNodeSet newArrayNodeSet = mockNewArrayNodeSet(99);
        final SequenceIterator it = newArrayNodeSet.iterate();

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
    public void iterate_loop_skip_loop() {
        final NewArrayNodeSet newArrayNodeSet = mockNewArrayNodeSet(99);
        final SequenceIterator it = newArrayNodeSet.iterate();

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

    private static NewArrayNodeSet mockNewArrayNodeSet(final int size) {
        final NodeProxy mockNodes[] = new NodeProxy[size];
        for (int i = 0; i < mockNodes.length; i++) {
            final NodeProxy mockNodeProxy = createMock(NodeProxy.class);
            replay(mockNodeProxy);
            mockNodes[i] = mockNodeProxy;
        }
        return new NewArrayNodeSetStub(mockNodes);
    }

    private static class NewArrayNodeSetStub extends NewArrayNodeSet {
        public NewArrayNodeSetStub(final NodeProxy... nodes) {
            for(final NodeProxy node : nodes) {
                addInternal(node, Constants.NO_SIZE_HINT);
            }
        }

        @Override
        public SequenceIterator iterate() {
            return new NewArrayIterator();
        }
    }
}
