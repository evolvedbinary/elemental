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

import com.evolvedbinary.j8cu.list.linked.BoundedDoublyLinkedList;
import com.evolvedbinary.j8cu.list.linked.OrderedDoublyLinkedList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.util.ByteConversion;

import javax.annotation.Nullable;

import java.util.Iterator;

import static com.evolvedbinary.j8cu.list.linked.Bounded.bound;

/**
 * Manages a list of pages containing unused sections.
 * 
 * Class {@link org.exist.storage.index.BFile} stores all data in variable
 * length records. As records may grow or shrink, the database has to keep
 * track of the amount of free space currently available in pages. Class 
 * {@link org.exist.storage.index.BFile} will always check if FreeList has a page
 * that can be filled before creating a new page.
 * 
 * FreeList implements a linked list of {@link FreeSpace} objects. Each object
 * in the list describes a page and the unused space in this page.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author wolf
 */
public class FreeList {

    private static final Logger LOG = LogManager.getLogger(FreeList.class);

    public static final int HEADER_SIZE = 4;  // sizeof(int);
    public static final int RECORD_SIZE = FreeSpace.LENGTH;

    // TODO(AR) consider using an ordered map of Long -> Int instead of the FreeSpace class.
    private final BoundedDoublyLinkedList<FreeSpace> list = bound(new OrderedDoublyLinkedList<FreeSpace>(16), Long.MAX_VALUE);

    /**
     * Append a new FreeSpace object to this list.
     *  
     * @param freeSpace the free space
     */
    public void add(final FreeSpace freeSpace) {
        list.add(freeSpace);
    }

    /**
     * Remove a FreeSpace object from this list.
     * 
     * @param freeSpace the free space
     */
    public void remove(final FreeSpace freeSpace) {
        list.removeOne(freeSpace);
    }

    /**
     * Retrieve the record stored for the given page number.
     * 
     * @param pageNum the page number
     * @return the free space
     */
    public @Nullable FreeSpace retrieve(final long pageNum) {
        for (final FreeSpace freeSpace : list) {
            if (freeSpace.page == pageNum) {
                return freeSpace;
            }
        }
        return null;
    }

    /**
     * Try to find a page that has at least requiredSize bytes
     * available. This method selects the page with the smallest
     * possible space. This guarantees that all pages will be filled before
     * creating a new page. 
     * 
     * @param requiredSize the required size
     *
     * @return the free space
     */
    public @Nullable FreeSpace find(final int requiredSize) {
        for (final FreeSpace freeSpace : list) {
            if (freeSpace.free >= requiredSize) {
                return freeSpace;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        for (final FreeSpace freeSpace : list) {
            if (buf.length() != 0) {
                buf.append(' ');
            }
            buf.append("[").append(freeSpace.page).append(", ");
            buf.append(freeSpace.free).append("]");
        }
        return buf.toString();
    }

    /**
     * Load a FreeList from a buffer.
     * 
     * @param buf the buffer to read from
     * @param offset the position in the buffer to read from
     *
     * @return the freelist.
     */
    public static FreeList load(final byte[] buf, int offset) {
        final FreeList freeList = new FreeList();
        final int fsize = ByteConversion.byteToInt(buf, offset);
        offset += 4;
        long page;
        int space;
        for (int i = 0; i < fsize; i++) {
            page = ByteConversion.byteToLong(buf, offset);
            offset += 8;
            space = ByteConversion.byteToInt(buf, offset);
            offset += 4;
            freeList.add(new FreeSpace(page, space));
        }
        return freeList;
    }

    /**
     * Write the list.
     * 
     * As the list is written to the file header, its maximum length
     * has to be restricted. The method will thus only store
     * maxRecords entries and throw away the rest. Usually, this should not
     * happen very often, so it is ok to waste some space.
     *
     * @param maxRecords the max records to write.
     * @param buf the buffer to write to
     * @param offset the position in the buffer to write to
     * @return the offset after writing
     */
    public int write(final int maxRecords, final byte[] buf, int offset) {
        //does the free-space list fit into the file header?
        if (list.size() > maxRecords) {
            LOG.warn("FreeList contains " + list.size() + " records, but page can only store " + maxRecords + "; smallest FreeSpace records will be discarded.");
        }

        // write the header (i.e. the number of records that will be stored)
        ByteConversion.intToByte((int) Math.min(maxRecords, list.size()), buf, offset);
        offset += 4;

        // write the records
        int records = 0;
        final Iterator<FreeSpace> itFreeSpace = list.reverseIterator();  // reverse iterator as they are ordered smallest first, and we want the largest
        while (itFreeSpace.hasNext()) {
            // write a record
            final FreeSpace freeSpace = itFreeSpace.next();
            ByteConversion.longToByte(freeSpace.page, buf, offset);
            offset += 8;
            ByteConversion.intToByte(freeSpace.free, buf, offset);
            offset += 4;

            if (++records == maxRecords) {
                // we have written the maximum number of records
                break;
            }
        }

        return offset;
    }
}
