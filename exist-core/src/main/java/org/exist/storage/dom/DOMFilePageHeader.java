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

import org.exist.storage.btree.BTreePageHeader;
import org.exist.util.ByteConversion;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.exist.storage.btree.Page.NO_PAGE;

public class DOMFilePageHeader extends BTreePageHeader {

    private static final int LENGTH_RECORDS_COUNT = 2; //sizeof short
    private static final int LENGTH_DATA_LENGTH = 4; //sizeof int
    private static final int LENGTH_NEXT_PAGE_POINTER = 8; //sizeof long
    private static final int LENGTH_PREV_PAGE_POINTER = 8; //sizeof long
    private static final int LENGTH_CURRENT_TID = 2; //sizeof short

    private int dataLength = 0;
    private long nextDataPage = NO_PAGE;
    private long previousDataPage = NO_PAGE;
    private short tupleID = ItemId.UNKNOWN_ID;
    private short records = 0;

    DOMFilePageHeader() {
        super();
    }

    DOMFilePageHeader(final byte[] data, final int offset) throws IOException {
        super(data, offset);
    }

    void decRecordCount() {
        //TODO : check negative value ? -pb
        records--;
    }

    short getCurrentTupleID() {
        //TODO : overflow check ? -pb
        return tupleID;
    }

    short getNextTupleID() {
        if (++tupleID == ItemId.ID_MASK) {
            throw new RuntimeException("No spare ids on page");
        }
        return tupleID;
    }

    boolean hasRoom() {
        return tupleID < ItemId.MAX_ID;
    }

    void setNextTupleID(final short tupleID) {
        if (tupleID > ItemId.MAX_ID) {
            throw new RuntimeException("TupleID overflow! TupleID = " + tupleID);
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
    public int read(final byte[] data, int offset) throws IOException {
        offset = super.read(data, offset);
        records = ByteConversion.byteToShort(data, offset);
        offset += LENGTH_RECORDS_COUNT;
        dataLength = ByteConversion.byteToInt(data, offset);
        offset += LENGTH_DATA_LENGTH;
        nextDataPage = ByteConversion.byteToLong(data, offset);
        offset += LENGTH_NEXT_PAGE_POINTER;
        previousDataPage = ByteConversion.byteToLong(data, offset);
        offset += LENGTH_PREV_PAGE_POINTER;
        tupleID = ByteConversion.byteToShort(data, offset);
        return offset + LENGTH_CURRENT_TID;
    }

    @Override
    public int write(final byte[] data, int offset) throws IOException {
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
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            if (dataLength > fileHeader.getWorkSize()) {
                LOG.error("data too long for file header !");
                //TODO  :throw exception ? -pb
            }
        } finally {
            fileHeaderReadLock.unlock();
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
