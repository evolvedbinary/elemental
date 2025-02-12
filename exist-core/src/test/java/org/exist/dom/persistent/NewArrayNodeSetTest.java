/*
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.exist.dom.persistent;

import org.easymock.EasyMock;
import org.exist.numbering.DLN;
import org.exist.security.SecurityManager;
import org.exist.storage.BrokerPool;
import org.exist.xquery.Constants;
import org.exist.xquery.Expression;
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

    @Test
    public void duplicates() {
        NewArrayNodeSet newArrayNodeSet = mockNewArrayNodeSet("1.1", "1.2", "1.2", "1.4");
        assertEquals(4, newArrayNodeSet.size);
        newArrayNodeSet.removeDuplicates(true);
        assertEquals(3, newArrayNodeSet.size);
        NewArrayNodeSet compare = mockNewArrayNodeSet("1.1", "1.2", "1.4");
        assertEquals(3, count(newArrayNodeSet.deepIntersection((NodeSet) compare)));

        newArrayNodeSet = mockNewArrayNodeSet("1.1", "1.2", "1.2", "1.4", "1.4", "1.4.1", "1.4.2", "1.4.2");
        assertEquals(8, newArrayNodeSet.size);
        newArrayNodeSet.removeDuplicates(true);
        assertEquals(5, newArrayNodeSet.size);
        compare = mockNewArrayNodeSet("1.1", "1.2", "1.4", "1.4.1", "1.4.2");
        assertEquals(5, count(newArrayNodeSet.deepIntersection((NodeSet) compare)));

        newArrayNodeSet = mockNewArrayNodeSet("1.2", "1.2", "1.2", "1.4", "1.4", "1.4.1", "1.4.2", "1.4.2");
        assertEquals(8, newArrayNodeSet.size);
        newArrayNodeSet.removeDuplicates(true);
        assertEquals(4, newArrayNodeSet.size);
        compare = mockNewArrayNodeSet("1.2", "1.4", "1.4.1", "1.4.2");
        assertEquals(4, count(newArrayNodeSet.deepIntersection((NodeSet) compare)));

        newArrayNodeSet = mockNewArrayNodeSet("1.2");
        assertEquals(1, newArrayNodeSet.size);
        newArrayNodeSet.removeDuplicates(true);
        assertEquals(1, newArrayNodeSet.size);
        compare = mockNewArrayNodeSet("1.2");
        assertEquals(1, count(newArrayNodeSet.deepIntersection((NodeSet) compare)));

        newArrayNodeSet = mockNewArrayNodeSet("1.1", "1.2");
        assertEquals(2, newArrayNodeSet.size);
        newArrayNodeSet.removeDuplicates(true);
        assertEquals(2, newArrayNodeSet.size);
        compare = mockNewArrayNodeSet("1.1", "1.2");
        assertEquals(2, count(newArrayNodeSet.deepIntersection((NodeSet) compare)));

        newArrayNodeSet = mockNewArrayNodeSet("1.2", "1.2");
        assertEquals(2, newArrayNodeSet.size);
        newArrayNodeSet.removeDuplicates(true);
        assertEquals(1, newArrayNodeSet.size);
        compare = mockNewArrayNodeSet("1.1", "1.2");
        assertEquals(1, count(newArrayNodeSet.deepIntersection((NodeSet) compare)));

        // Regression, was 1
        newArrayNodeSet = mockNewArrayNodeSet();
        assertEquals(0, newArrayNodeSet.size);
        newArrayNodeSet.removeDuplicates(true);
        assertEquals(0, newArrayNodeSet.size);
        assertEquals(0, count(newArrayNodeSet));
    }

    private int count(NodeSet nodeSet) {
        int i = 0;
        for (NodeProxy nodeProxy : nodeSet) {
            i++;
        }
        return i;
    }

    private NewArrayNodeSet mockNewArrayNodeSet(String ... dlns) {

        final NewArrayNodeSet newArrayNodeSet = new NewArrayNodeSet();
        BrokerPool mockBrokerPool = EasyMock.createMock(BrokerPool.class);
        DocumentImpl mockDocument = EasyMock.createMock(DocumentImpl.class);
        SecurityManager mockSecurityManager = EasyMock.createMock(SecurityManager.class);
        Expression mockExpression = EasyMock.createMock(Expression.class);

        expect(mockBrokerPool.getSecurityManager()).andReturn(mockSecurityManager);
        expect(mockDocument.getExpression()).andReturn(mockExpression).anyTimes();
        expect(mockDocument.getDocId()).andReturn(1).anyTimes();
        replay(mockBrokerPool, mockDocument, mockSecurityManager, mockExpression);

        for (String dln : dlns) {
            newArrayNodeSet.add(new NodeProxy(mockDocument, new DLN(dln)));
        }
        return newArrayNodeSet;
    }

    @Test
    public void set_sorted() {
        final NewArrayNodeSet newArrayNodeSet = mockNewArrayNodeSet(0);
        assertEquals(0, newArrayNodeSet.size);
        newArrayNodeSet.setKnownSorted(true);
        assertEquals(0, newArrayNodeSet.size);
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
