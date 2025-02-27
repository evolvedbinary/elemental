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
package org.exist.storage.dom;

import org.exist.storage.btree.AbstractPagedFile.Page;
import org.exist.storage.btree.BTreePageHeader;
import org.exist.util.ByteConversion;

public class DOMFilePageHeader extends BTreePageHeader {

    private static final int LENGTH_RECORDS_COUNT = 2; //sizeof short
    private static final int LENGTH_DATA_LENGTH = 4; //sizeof int
    private static final int LENGTH_NEXT_PAGE_POINTER = 8; //sizeof long
    private static final int LENGTH_PREV_PAGE_POINTER = 8; //sizeof long
    private static final int LENGTH_CURRENT_TID = 2; //sizeof short

    private short records = 0;
    private int dataLength = 0;
    private long nextDataPage = Page.NO_PAGE;
    private long previousDataPage = Page.NO_PAGE;
    private short tupleID = ItemId.UNKNOWN_ID;

    private final int maxDataLength;

    public DOMFilePageHeader(final int maxDataLength) {
        this.maxDataLength = maxDataLength;
    }

    void decRecordCount() {
        if (records == 0) {
            throw new IllegalStateException("recordCount cannot be decremented below zero");
        }
        records--;
    }

    short getCurrentTupleID() {
        return tupleID;
    }

    short getNextTupleID() {
        if (++tupleID == ItemId.ID_MASK) {
            throw new IllegalStateException("No spare ids on page");
        }
        return tupleID;
    }

    boolean hasRoom() {
        return tupleID < ItemId.MAX_ID;
    }

    void setNextTupleID(final short tupleID) {
        if (tupleID > ItemId.MAX_ID) {
            throw new IllegalArgumentException("TupleID would overflow! TupleID = " + tupleID);
        }
        this.tupleID = tupleID;
    }

    int getDataLength() {
        return dataLength;
    }

    long getNextDataPage() {
        return nextDataPage;
    }

    long getPreviousDataPage() {
        return previousDataPage;
    }

    short getRecordCount() {
        return records;
    }

    void incRecordCount() {
        records++;
    }

    @Override
    public int read(final byte[] data, int offset) {
        offset = super.read(data, offset);
        this.records = ByteConversion.byteToShort(data, offset);
        offset += LENGTH_RECORDS_COUNT;
        this.dataLength = ByteConversion.byteToInt(data, offset);
        offset += LENGTH_DATA_LENGTH;
        this.nextDataPage = ByteConversion.byteToLong(data, offset);
        offset += LENGTH_NEXT_PAGE_POINTER;
        this.previousDataPage = ByteConversion.byteToLong(data, offset);
        offset += LENGTH_PREV_PAGE_POINTER;
        this.tupleID = ByteConversion.byteToShort(data, offset);
        return offset + LENGTH_CURRENT_TID;
    }

    @Override
    public int write(final byte[] data, int offset) {
        offset = super.write(data, offset);
        ByteConversion.shortToByte(records, data, offset);
        offset += LENGTH_RECORDS_COUNT;
        ByteConversion.intToByte(dataLength, data, offset);
        offset += LENGTH_DATA_LENGTH;
        ByteConversion.longToByte(nextDataPage, data, offset);
        offset += LENGTH_NEXT_PAGE_POINTER;
        ByteConversion.longToByte(previousDataPage, data, offset);
        offset += LENGTH_PREV_PAGE_POINTER;
        ByteConversion.shortToByte(tupleID, data, offset);
        return offset + LENGTH_CURRENT_TID;
    }

    void setDataLength(final int dataLength) {
        if (dataLength > maxDataLength) {
            throw new IllegalArgumentException("setDataLength(" + dataLength + ") exceeds maxDataLength: " + maxDataLength);
        }
        this.dataLength = dataLength;
    }

    void setNextDataPage(final long page) {
        nextDataPage = page;
    }

    void setPrevDataPage(final long page) {
        previousDataPage = page;
    }

    void setRecordCount(final short recs) {
        records = recs;
    }
}
