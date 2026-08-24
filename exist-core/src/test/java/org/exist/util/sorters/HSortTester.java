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

package org.exist.util.sorters;

import org.exist.dom.persistent.NodeProxy;
import org.exist.util.HSort;

import java.util.Comparator;
import java.util.List;

/**
 * Interface to the improved heapsort methods.
 *
 * @author José María Fernández (jmfg@users.sourceforge.net)
 * @see HSort
 *
 */

class HSortTester extends SortingAlgorithmTester {
    @Override
    public <C extends Comparable<? super C>> void invokeSort(C[] a, int lo, int hi) {
        HSort.sort(a, lo, hi);
    }

    @Override
    public <C> void invokeSort(C a[], Comparator<C> c, int lo, int hi) {
        HSort.sort(a, c, lo, hi);
    }

    @Override
    public <C extends Comparable<? super C>> void sort(C[] a, int lo, int hi) {
        HSort.sort(a, lo, hi);
    }

    @Override
    public <C extends Comparable<? super C>> void sort(C[] a, int lo, int hi, int[] b) {
        HSort.sort(a, lo, hi, b);
    }

    @Override
    public <C> void sort(C[] a, Comparator<C> c, int lo,
                         int hi) {
        HSort.sort(a, c, lo, hi);
    }

    @Override
    public <C extends Comparable<? super C>> void sort(List<C> a, int lo, int hi) {
        HSort.sort(a, lo, hi);
    }

    @Override
    public void sort(int lo, int hi, NodeProxy[] a) {
        sort(a, lo, hi);
    }

    @Override
    public void sort(long[] a, int lo, int hi, Object[] b) {
        HSort.sort(a, lo, hi, b);
    }

    @Override
    public void sortByNodeId(NodeProxy[] a, int lo, int hi) {
        HSort.sortByNodeId(a, lo, hi);
    }
}
