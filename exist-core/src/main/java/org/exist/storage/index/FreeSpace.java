/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 *
 * NOTE: Parts of this file contain code from The eXist-db Authors.
 *       The original license header is included below.
 *
 * ----------------------------------------------------------------------------
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
package org.exist.storage.index;

/**
 * Used to track the available amount of free space in a data page.
 * 
 * @see FreeList
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author wolf
 */
public class FreeSpace implements Comparable<FreeSpace> {

    public static final int LENGTH_PAGE = 8;  // sizeof(long)
    public static final int LENGTH_FREE = 4;  // sizeof(int)
    public static final int LENGTH = LENGTH_PAGE + LENGTH_FREE;

    final long page;
    int free;

    public FreeSpace(final long page, final int free) {
        this.page = page;
        this.free = free;
    }

    @Override
    public int compareTo(final FreeSpace other) {
        return Integer.compare(free, other.free);
    }
}
