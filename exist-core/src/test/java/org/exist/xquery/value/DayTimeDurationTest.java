/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
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
package org.exist.xquery.value;

import org.exist.xquery.Constants.Comparison;
import org.exist.xquery.XPathException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
class DayTimeDurationTest extends AbstractTimeRelatedTest {

    @Test
    void create1() {
        assertThrows(XPathException.class, () ->
            new DayTimeDurationValue("P1Y4M")
        );
    }

    @Test
    void create2() {
        assertThrows(XPathException.class, () ->
            new DayTimeDurationValue("P1Y")
        );
    }

    @Test
    void create3() {
        assertThrows(XPathException.class, () ->
            new DayTimeDurationValue("P4M")
        );
    }

    @Test
    void stringFormat1() throws XPathException {
        final DurationValue dv = new DayTimeDurationValue("P3DT1H2M3S");
        assertEquals("P3DT1H2M3S", dv.getStringValue());
    }

    @Test
    void stringFormat2() throws XPathException {
        final DurationValue dv = new DayTimeDurationValue("P1DT25H65M66.5S");
        assertEquals("P2DT2H6M6.5S", new DurationValue(dv.getCanonicalDuration()).getStringValue());
    }

    @Test
    void stringFormat3() throws XPathException {
        final DurationValue dv = new DayTimeDurationValue("P0DT0H");
        assertEquals("PT0S", dv.getStringValue());
    }

    @Test
    void stringFormat4() throws XPathException {
        final DurationValue dv = new DayTimeDurationValue("PT5H0M0S");
        assertEquals("PT5H", dv.getStringValue());
    }

    @Test
    void convert1() throws XPathException {
        final DayTimeDurationValue dtdv = new DayTimeDurationValue("P3DT1H2M3S");
        final DurationValue dv = (DurationValue) dtdv.convertTo(Type.DURATION);
        assertNotNull(dv);
        assertEquals("P3DT1H2M3S", dv.getStringValue());
    }

    @Test
    void convert2() throws XPathException {
        final DayTimeDurationValue dtdv = new DayTimeDurationValue("P3DT1H2M3S");
        assertNotNull(dtdv);
        final AtomicValue ymd = dtdv.convertTo(Type.YEAR_MONTH_DURATION);
        assertNotNull(ymd);
        assertEquals("P0M", ymd.getStringValue());
    }

    @Test
    void getPart1() throws XPathException {
        final DurationValue dv = new DayTimeDurationValue("P3DT4H5M6S");
        assertEquals(0, dv.getPart(DurationValue.YEAR));
        assertEquals(0, dv.getPart(DurationValue.MONTH));
        assertEquals(3, dv.getPart(DurationValue.DAY));
        assertEquals(4, dv.getPart(DurationValue.HOUR));
        assertEquals(5, dv.getPart(DurationValue.MINUTE));
        assertEquals(6, dv.getSeconds(), 0);
    }

    @Test
    void getPart2() throws XPathException {
        final DurationValue dv = new DayTimeDurationValue("-P3DT4H5M6S");
        assertEquals(0, dv.getPart(DurationValue.YEAR));
        assertEquals(0, dv.getPart(DurationValue.MONTH));
        assertEquals(-3, dv.getPart(DurationValue.DAY));
        assertEquals(-4, dv.getPart(DurationValue.HOUR));
        assertEquals(-5, dv.getPart(DurationValue.MINUTE));
        assertEquals(-6, dv.getSeconds(), 0);
    }

    @Test
    void getValue1() throws XPathException {
        final DayTimeDurationValue dv = new DayTimeDurationValue("P1DT30S");
        assertEquals(1.0 * 24 * 60 * 60 + 30.0, dv.getValue(), 0.0);
    }

    @Test
    void getValue2() throws XPathException {
        final DayTimeDurationValue dv = new DayTimeDurationValue("P1D");
        assertEquals(1.0 * 24 * 60 * 60, dv.getValue(), 0.0);
    }

    @Test
    void getType() throws XPathException {
        final DurationValue dv = new DayTimeDurationValue("P3DT4H5M6S");
        assertEquals(Type.DAY_TIME_DURATION, dv.getType());
    }

    @Test
    void compare1() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("P1DT2H3M4S");
        final DurationValue dv2 = new DayTimeDurationValue("P1DT2H3M5S");
        assertEquals(-1, dv1.compareTo(null, dv2));
        assertEquals(+1, dv2.compareTo(null, dv1));
    }

    @Test
    void compare2() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("P1DT2H3M4S");
        final DurationValue dv2 = new DayTimeDurationValue("P1DT2H3M4S");
        assertEquals(0, dv1.compareTo(null, dv2));
        assertEquals(0, dv2.compareTo(null, dv1));
    }

    @Test
    void compare3() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("P1DT2H3M4S");
        final DurationValue dv2 = new DayTimeDurationValue("P1DT2H3M5S");
        assertFalse(dv1.compareTo(null, Comparison.EQ, dv2));
        assertTrue(dv1.compareTo(null, Comparison.NEQ, dv2));
        assertFalse(dv1.compareTo(null, Comparison.GT, dv2));
        assertTrue(dv1.compareTo(null, Comparison.LT, dv2));
        assertFalse(dv1.compareTo(null, Comparison.GTEQ, dv2));
        assertTrue(dv1.compareTo(null, Comparison.LTEQ, dv2));
    }

    @Test
    void compare4() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("P1DT2H3M4S");
        final DurationValue dv2 = new DayTimeDurationValue("P1DT2H3M4S");
        assertTrue(dv1.compareTo(null, Comparison.EQ, dv2));
        assertFalse(dv1.compareTo(null, Comparison.NEQ, dv2));
        assertFalse(dv1.compareTo(null, Comparison.GT, dv2));
        assertFalse(dv1.compareTo(null, Comparison.LT, dv2));
        assertTrue(dv1.compareTo(null, Comparison.GTEQ, dv2));
        assertTrue(dv1.compareTo(null, Comparison.LTEQ, dv2));
    }

    @Test
    void compare5() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("PT2H");
        final DurationValue dv2 = new DayTimeDurationValue("PT2H0M");
        assertEquals(0, dv1.compareTo(null, dv2));
        assertEquals(0, dv2.compareTo(null, dv1));
    }

    @Test
    void minMax1() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("P1DT2H3M4S");
        final DurationValue dv2 = new DayTimeDurationValue("P1DT2H3M5S");
        assertDurationEquals(dv2, dv1.max(null, dv2));
        assertDurationEquals(dv2, dv2.max(null, dv1));
        assertDurationEquals(dv1, dv1.min(null, dv2));
        assertDurationEquals(dv1, dv2.min(null, dv1));
    }

    @Test
    void plus1() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("P2DT12H5M");
        final DurationValue dv2 = new DayTimeDurationValue("P5DT12H");
        final DurationValue dv3 = new DayTimeDurationValue("P8DT5M");
        assertDurationEquals(dv3, dv1.plus(dv2));
        assertDurationEquals(dv3, dv2.plus(dv1));
    }

    @Test
    void minus1() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("P2DT12H");
        final DurationValue dv2 = new DayTimeDurationValue("P1DT10H30M");
        final DurationValue dv3 = new DayTimeDurationValue("P1DT1H30M");
        assertDurationEquals(dv3, dv1.minus(dv2));
    }

    @Test
    void mult1() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("PT2H10M");
        final DecimalValue f = new DecimalValue("2.1");
        final DurationValue dv2 = new DayTimeDurationValue("PT4H33M");
        assertDurationEquals(dv2, dv1.mult(f));
        assertDurationEquals(dv2, f.mult(dv1));
    }

    @Test
    void div1() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("P1DT2H30M10.5S");
        final DecimalValue f = new DecimalValue("1.5");
        final DurationValue dv2 = new DayTimeDurationValue("PT17H40M7S");
        assertDurationEquals(dv2, dv1.div(f));
    }

    @Test
    void div2() throws XPathException {
        final DurationValue dv1 = new DayTimeDurationValue("P2DT53M11S");
        final DurationValue dv2 = new DayTimeDurationValue("P1DT10H");
        final Double dbl = dv1.div(dv2).toJavaObject(Double.class);
        assertNotNull(dbl);
        assertEquals(1.4378349, dbl, 0.0000001);
    }
}
