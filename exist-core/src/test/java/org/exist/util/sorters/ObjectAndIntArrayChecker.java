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
package org.exist.util.sorters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * check sort(Object[], int[])
 *
 * This work was undertaken as part of the development of the taxonomic
 * repository at http://biodiversity.org.au . See <A
 * href="ghw-at-anbg.gov.au">Greg&nbsp;Whitbread</A> for further details.
 * 
 * @author pmurray@bigpond.com
 * @author pmurray@anbg.gov.au
 * @author https://sourceforge.net/users/paulmurray
 * @author http://www.users.bigpond.com/pmurray
 * 
 */

class ObjectAndIntArrayChecker extends SortMethodChecker {
	ObjectAndIntArrayChecker(SortingAlgorithmTester sorter) {
		super(sorter);
	}

	Integer[] a;
	int[] b;

	/**
	 * It asserts the ascending ordering of an Integer array
	 * given an specific comparator
	 */
    @Override
	void check(int lo, int hi) {
		for (int i = lo; i < hi; i++) {
			assertTrue(a[i].intValue() <= a[i + 1].intValue());
			assertEquals(a[i].intValue(), b[i]);
		}
		assertEquals(a[hi].intValue(), b[hi]);
	}

	/**
	 * It returns the length of the array to be used on assertion
	 */
    @Override
	int getLength() {
		return a.length;
	}

	/**
	 * It loads an input int array into the internal
	 * Integer and int ones
	 */
    @Override
	void init(int[] values) {
		a = new Integer[values.length];
		b = new int[values.length];
		for (int i = 0; i < values.length; i++) {
			a[i] = Integer.valueOf(values[i]);
			b[i] = values[i];
		}
	}

	/**
	 * This method invokes sort routine on selected sorter
	 */
    @Override
	void sort(int lo, int hi) {
		sorter.sort(a, lo, hi, b);
	}

	/**
	 * This method asserts single values
	 */
    @Override
	void checkValue(int idx, int v) {
		assertEquals(v, a[idx].intValue(), "@" + idx);
		assertEquals(v, b[idx], "@" + idx);
	}
}