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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.storage.btree.BTreePageHeader;
import org.exist.util.ByteConversion;

/**
 * Page header for a BFile page.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
public class BFilePageHeader extends BTreePageHeader {

    private static final Logger LOG = LogManager.getLogger(BFilePageHeader.class);

    private static final int LENGTH_RECORDS_COUNT = 2; //sizeof short
    private static final int LENGTH_NEXT_TID = 2; //sizeof short

    public static final short NONE = -1;
    public static final short OVERFLOW_ERROR = -2;

    private short recordCount = 0;
    private int dataLength = 0;
    /**
     * Tuple identifier. Identifies a distinct value in a page
     */
    private short nextTID = NONE;
    private long nextInChain = NONE;
    private long lastInChain = NONE;

    public int getDataLength() {
        return this.dataLength;
    }

    public void setDataLength(final int dataLength) {
        this.dataLength = dataLength;
    }

    public long getNextInChain() {
        return this.nextInChain;
    }

    public void setNextInChain(final long nextInChain) {
        this.nextInChain = nextInChain;
    }

    public long getLastInChain() {
        return this.lastInChain;
    }

    public void setLastInChain(final long lastInChain) {
        this.lastInChain = lastInChain;
    }

    public short getNextTID() {
        if (this.nextTID == Short.MAX_VALUE) {
            LOG.error("TID limit reached. Returning OVERFLOW_ERROR!");
            return OVERFLOW_ERROR;
        }
        return ++this.nextTID;
    }

    public short getCurrentTID() {
        if (this.nextTID == Short.MAX_VALUE) {
            LOG.error("TID limit reached. Returning OVERFLOW_ERROR!");
            return OVERFLOW_ERROR;
        }
        return this.nextTID;
    }

    public short getRecordCount() {
        return this.recordCount;
    }

    public void setRecordCount(final short recordCount) {
        this.recordCount = recordCount;
    }

    public void decRecordCount() {
        this.recordCount--;
    }

    public void incRecordCount() {
        this.recordCount++;
    }

    @Override
    public int read(final byte[] data, int offset) {
        offset = super.read(data, offset);
        this.recordCount = ByteConversion.byteToShort(data, offset);
        offset += LENGTH_RECORDS_COUNT;
        this.dataLength = ByteConversion.byteToInt(data, offset);
        offset += 4;
        this.nextTID = ByteConversion.byteToShort(data, offset);
        offset += LENGTH_NEXT_TID;
        this.nextInChain = ByteConversion.byteToLong(data, offset);
        offset += 8;
        this.lastInChain = ByteConversion.byteToLong(data, offset);
        return offset + 8;
    }

    public void setNextTID(final short nextTID) {
        this.nextTID = nextTID;
    }

    @Override
    public int write(final byte[] data, int offset) {
        offset = super.write(data, offset);
        ByteConversion.shortToByte(recordCount, data, offset);
        offset += LENGTH_RECORDS_COUNT;
        ByteConversion.intToByte(dataLength, data, offset);
        offset += 4;
        ByteConversion.shortToByte(nextTID, data, offset);
        offset += LENGTH_NEXT_TID;
        ByteConversion.longToByte(nextInChain, data, offset);
        offset += 8;
        ByteConversion.longToByte(lastInChain, data, offset);
        return offset + 8;
    }
}
