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

import java.util.Comparator;

/**
 * check sort(Comparable[])
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
class PlainArrayChecker extends ComparatorChecker {
	PlainArrayChecker(SortingAlgorithmTester sorter) {
		super(sorter);
	}

	Integer[] a;
	
	/**
	 * It asserts the ascending ordering of an Integer array
	 */
    @Override
	void check(int lo, int hi) {
		for (int i = lo; i < hi; i++) {
			assertTrue(a[i].intValue() <= a[i + 1].intValue());
		}
	}

	/**
	 * It loads an input int array into the internal Integer one
	 */
    @Override
	void init(int[] values)
	{
		a = new Integer[values.length];
		for (int i = 0; i < values.length; i++) {
			a[i] = Integer.valueOf(values[i]);
		}

	}
	
	/**
	 * It returns the length of the array to be used on assertion
	 */
    @Override
	int getLength() {
		return a.length;
	}
	
	/**
	 * This method invokes sort routine on selected sorter
	 */
    @Override
	void sort(int lo, int hi) {
		sorter.sort(a, lo, hi);
	}

	/**
	 * This method invokes sort routine with a given
	 * comparator on selected sorter
	 */
    @Override
	void sort(SortOrder sortOrder, int lo, int hi) {
		sorter.sort(a, getComparator(sortOrder), lo, hi);
	}

	/**
	 * This method asserts single values
	 */
    @Override
	void checkValue(int idx, int v) {
		assertEquals(v, a[idx].intValue(), "@" + idx);
	}

	/**
	 * It asserts the ascending ordering of an Integer array
	 * given an specific comparator
	 */
    @Override
	void check(SortOrder sortOrder, int lo, int hi) {
		Comparator<Integer> c = getComparator(sortOrder);
		for (int i = lo; i < hi; i++) {
			assertTrue(c.compare(a[i], a[i + 1]) <= 0);
		}
	}

	/**
	 * This method returns an Integer comparator based
	 * on input sort order
	 * @param sortOrder
	 * @return
	 */
	Comparator<Integer> getComparator(SortOrder sortOrder) {
        return switch (sortOrder) {
            case ASCENDING -> (o1, o2) -> o1.intValue() - o2.intValue();
            case DESCENDING -> (o1, o2) -> o2.intValue() - o1.intValue();
            case RANDOM -> (o1, o2) -> rnd.nextBoolean() ? -1 : 1;
            case UNSTABLE -> (o1, o2) -> {
                if (o1.intValue() <= o2.intValue())
                    return (o2.intValue() - o1.intValue()) % 3 - 1;
                else
                    return 1 - (o1.intValue() - o2.intValue()) % 3;
            };
        };
    }

}