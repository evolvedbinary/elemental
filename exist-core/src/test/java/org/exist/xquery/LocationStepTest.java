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

import org.exist.xquery.util.ExpressionDumper;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertArrayEquals;

public class LocationStepTest {

    @Test
    public void insertPredicateNoPrevious() {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(Expression.EXPRESSION_ID_INVALID);

        final Predicate mockPredicate1 = mock(Predicate.class);
        final Predicate mockPredicate2 = mock(Predicate.class);
        final Predicate mockPredicate3 = mock(Predicate.class);
        final Predicate mockNotPreviousPredicate = mock(Predicate.class);

        mockPredicate1.dump(anyObject(ExpressionDumper.class));
        mockPredicate2.dump(anyObject(ExpressionDumper.class));
        mockNotPreviousPredicate.dump(anyObject(ExpressionDumper.class));

        replay(mockContext, mockPredicate1, mockPredicate2, mockPredicate3, mockNotPreviousPredicate);

        final LocationStep locationStep = new LocationStep(mockContext, Constants.UNKNOWN_AXIS);

        locationStep.addPredicate(mockPredicate1);
        locationStep.addPredicate(mockPredicate2);

        locationStep.insertPredicate(mockNotPreviousPredicate, mockPredicate3);
        assertArrayEquals(new Predicate[]{ mockPredicate1, mockPredicate2}, locationStep.getPredicates());

        verify(mockContext, mockPredicate1, mockPredicate2, mockPredicate3, mockNotPreviousPredicate);
    }

    @Test
    public void insertPredicateInMiddleOddFirst() {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(Expression.EXPRESSION_ID_INVALID);

        final Predicate mockPredicate1 = mock(Predicate.class);
        final Predicate mockPredicate2 = mock(Predicate.class);
        final Predicate mockPredicate3 = mock(Predicate.class);

        replay(mockContext, mockPredicate1, mockPredicate2, mockPredicate3);

        final LocationStep locationStep = new LocationStep(mockContext, Constants.UNKNOWN_AXIS);

        locationStep.addPredicate(mockPredicate1);
        locationStep.addPredicate(mockPredicate2);

        locationStep.insertPredicate(mockPredicate1, mockPredicate3);
        assertArrayEquals(new Predicate[]{ mockPredicate1, mockPredicate3, mockPredicate2}, locationStep.getPredicates());

        verify(mockContext, mockPredicate1, mockPredicate2, mockPredicate3);
    }

    @Test
    public void insertPredicateInMiddleOddSecond() {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(Expression.EXPRESSION_ID_INVALID);

        final Predicate mockPredicate1 = mock(Predicate.class);
        final Predicate mockPredicate2 = mock(Predicate.class);
        final Predicate mockPredicate3 = mock(Predicate.class);

        replay(mockContext, mockPredicate1, mockPredicate2, mockPredicate3);

        final LocationStep locationStep = new LocationStep(mockContext, Constants.UNKNOWN_AXIS);

        locationStep.addPredicate(mockPredicate1);
        locationStep.addPredicate(mockPredicate2);

        locationStep.insertPredicate(mockPredicate2, mockPredicate3);
        assertArrayEquals(new Predicate[]{ mockPredicate1, mockPredicate2, mockPredicate3}, locationStep.getPredicates());

        verify(mockContext, mockPredicate1, mockPredicate2, mockPredicate3);
    }

    @Test
    public void insertPredicateInMiddleEvenFirst() {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(Expression.EXPRESSION_ID_INVALID);

        final Predicate mockPredicate1 = mock(Predicate.class);
        final Predicate mockPredicate2 = mock(Predicate.class);
        final Predicate mockPredicate3 = mock(Predicate.class);
        final Predicate mockPredicate4 = mock(Predicate.class);

        replay(mockContext, mockPredicate1, mockPredicate2, mockPredicate3, mockPredicate4);

        final LocationStep locationStep = new LocationStep(mockContext, Constants.UNKNOWN_AXIS);

        locationStep.addPredicate(mockPredicate1);
        locationStep.addPredicate(mockPredicate2);
        locationStep.addPredicate(mockPredicate3);

        locationStep.insertPredicate(mockPredicate1, mockPredicate4);
        assertArrayEquals(new Predicate[]{ mockPredicate1, mockPredicate4, mockPredicate2, mockPredicate3}, locationStep.getPredicates());

        verify(mockContext, mockPredicate1, mockPredicate2, mockPredicate3, mockPredicate4);
    }

    @Test
    public void insertPredicateInMiddleEvenSecond() {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(Expression.EXPRESSION_ID_INVALID);

        final Predicate mockPredicate1 = mock(Predicate.class);
        final Predicate mockPredicate2 = mock(Predicate.class);
        final Predicate mockPredicate3 = mock(Predicate.class);
        final Predicate mockPredicate4 = mock(Predicate.class);

        replay(mockContext, mockPredicate1, mockPredicate2, mockPredicate3, mockPredicate4);

        final LocationStep locationStep = new LocationStep(mockContext, Constants.UNKNOWN_AXIS);

        locationStep.addPredicate(mockPredicate1);
        locationStep.addPredicate(mockPredicate2);
        locationStep.addPredicate(mockPredicate3);

        locationStep.insertPredicate(mockPredicate2, mockPredicate4);
        assertArrayEquals(new Predicate[]{ mockPredicate1, mockPredicate2, mockPredicate4, mockPredicate3}, locationStep.getPredicates());

        verify(mockContext, mockPredicate1, mockPredicate2, mockPredicate3, mockPredicate4);
    }

    @Test
    public void insertPredicateInMiddleEvenThird() {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(Expression.EXPRESSION_ID_INVALID);

        final Predicate mockPredicate1 = mock(Predicate.class);
        final Predicate mockPredicate2 = mock(Predicate.class);
        final Predicate mockPredicate3 = mock(Predicate.class);
        final Predicate mockPredicate4 = mock(Predicate.class);

        replay(mockContext, mockPredicate1, mockPredicate2, mockPredicate3, mockPredicate4);

        final LocationStep locationStep = new LocationStep(mockContext, Constants.UNKNOWN_AXIS);

        locationStep.addPredicate(mockPredicate1);
        locationStep.addPredicate(mockPredicate2);
        locationStep.addPredicate(mockPredicate3);

        locationStep.insertPredicate(mockPredicate3, mockPredicate4);
        assertArrayEquals(new Predicate[]{ mockPredicate1, mockPredicate2, mockPredicate3, mockPredicate4}, locationStep.getPredicates());

        verify(mockContext, mockPredicate1, mockPredicate2, mockPredicate3, mockPredicate4);
    }
}
