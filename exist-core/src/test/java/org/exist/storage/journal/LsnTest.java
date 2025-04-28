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
package org.exist.storage.journal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class LsnTest {

    @Test
    public void compareTo() {
        assertEquals(0, Lsn.LSN_INVALID.compareTo(Lsn.LSN_INVALID));
        assertEquals(-1, Lsn.LSN_INVALID.compareTo(new Lsn((short)0, 0)));
        assertEquals(1, new Lsn((short)0, 0).compareTo(Lsn.LSN_INVALID));

        assertEquals(0, new Lsn((short)0, 0).compareTo(new Lsn((short)0, 0)));

        assertEquals(0, new Lsn((short)1, 123).compareTo(new Lsn((short)1, 123)));
        assertEquals(-1, new Lsn((short)1, 123).compareTo(new Lsn((short)1, 124)));
        assertEquals(1, new Lsn((short)1, 124).compareTo(new Lsn((short)1, 122)));

        assertEquals(-1, new Lsn((short)1, 123).compareTo(new Lsn((short)2, 123)));
        assertEquals(1, new Lsn((short)2, 123).compareTo(new Lsn((short)1, 123)));

        assertEquals(-1, new Lsn((short)1, Long.MAX_VALUE).compareTo(new Lsn((short)2, Long.MIN_VALUE)));
        assertEquals(-1, new Lsn((short)1, Long.MAX_VALUE).compareTo(new Lsn((short)2, 0)));
        assertEquals(-1, new Lsn((short)1, Long.MAX_VALUE).compareTo(new Lsn((short)2, 1)));
        assertEquals(-1, new Lsn((short)1, Long.MAX_VALUE).compareTo(new Lsn((short)2, Long.MAX_VALUE)));

        assertEquals(1, new Lsn((short)2, Long.MIN_VALUE).compareTo(new Lsn((short)1, Integer.MAX_VALUE)));
        assertEquals(1, new Lsn((short)2, 0).compareTo(new Lsn((short)1, Integer.MAX_VALUE)));
        assertEquals(1, new Lsn((short)2, 1).compareTo(new Lsn((short)1, Integer.MAX_VALUE)));
        assertEquals(1, new Lsn((short)2, Long.MAX_VALUE).compareTo(new Lsn((short)1, Integer.MAX_VALUE)));
    }

    @Test
    public void equalsTo() {
        assertTrue(Lsn.LSN_INVALID.equals(Lsn.LSN_INVALID));
        assertTrue(Lsn.LSN_INVALID.equals(new Lsn((short)-1, -1)));
        assertTrue(new Lsn((short)-1, -1).equals(Lsn.LSN_INVALID));

        assertFalse(new Lsn((short)0, 0).equals(Lsn.LSN_INVALID));
        assertFalse(Lsn.LSN_INVALID.equals(new Lsn((short)0, 0)));
    }

}
