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

import org.exist.storage.btree.Page;
import org.exist.storage.cache.Cacheable;
import org.exist.util.ByteConversion;
import org.exist.util.HexEncoder;

import java.io.IOException;

/**
 * A page that stores DOM information. This page operates at a higher-level
 * than the {@link Page} class, and itself resides within the data
 * of the {@link Page}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
class DOMPage implements Cacheable {

    // TODO(AR) make these fields private access?

    // the low-level page
    final Page<DOMFilePageHeader> page;

    // the raw working data (without page header) of this DOM page
    byte[] data;

    // the current size of the used data of this DOM Page
    int len = 0;

    // fields required by Cacheable
    private int refCount = 0;

    private int timestamp = 0;

    // has the page been saved or is it dirty?
    private boolean saved = true;

    // set to true if the page has been removed from the cache
    private boolean invalidated = false;

    private final ThreadLocal<Integer> lastFound = ThreadLocal.withInitial(() -> 0);

    DOMPage(final Page<DOMFilePageHeader> page, final byte[] data, final int len) {
        this.page = page;
        this.data = data;
        this.len = len;
    }

    /**
     * Optimize scanning for records in a page
     * Based on the fact that we are looking for the same thing again,
     * Or we are looking for something after the last thing.
     *
     * So, start the scan where we left off before.
     *
     * @param targetId the tuple id we are looking for in the page
     *
     * @return a record describing the tuple, if we found it, otherwise null
     */
    RecordPos findRecord(final short targetId) throws IOException {
        final int startScan = lastFound.get();
        RecordPos rec = findRecordInRange(targetId, startScan, page.getPageHeader().getDataLength());
        if (rec == null) {
            rec = findRecordInRange(targetId, 0, startScan);
        }
        if (rec != null) {
            // start from here again next time; step back over the tuple id
            lastFound.set(rec.offset - DOMFile.LENGTH_TID);
        }
        return rec;
    }

    RecordPos findRecordInRange(final short targetId, final int from, final int to) throws IOException {
        RecordPos rec = null;
        for (int pos = from; pos < to;) {
            final short tupleID = ByteConversion.byteToShort(data, pos);
            pos += DOMFile.LENGTH_TID;
            if (ItemId.matches(tupleID, targetId)) {
                if (ItemId.isLink(tupleID)) {
                    rec = new RecordPos(pos, this, tupleID, true);
                } else {
                    rec = new RecordPos(pos, this, tupleID);
                }
                break;
            } else if (ItemId.isLink(tupleID)) {
                pos += DOMFile.LENGTH_FORWARD_LOCATION;
            } else {
                final short vlen = ByteConversion.byteToShort(data, pos);
                pos += DOMFile.LENGTH_DATA_LENGTH;
                if (vlen < 0) {
                    throw new IOException("page = " + page.getPageNum() + "; pos = " + pos + "; vlen = " + vlen + "; tupleID = " + tupleID + "; target = " + targetId);
                }
                if (ItemId.isRelocated(tupleID)) {
                    pos += DOMFile.LENGTH_ORIGINAL_LOCATION + vlen;
                } else {
                    pos += vlen;
                }
                if (vlen == DOMFile.OVERFLOW_PAGE_DATA_LENGTH) {
                    pos += DOMFile.LENGTH_OVERFLOW_LOCATION;
                }
            }
        }
        return rec;
    }

    @Override
    public long getKey() {
        return page.getPageNum();
    }

    @Override
    public int getReferenceCount() {
        return refCount;
    }

    @Override
    public int decReferenceCount() {
        //TODO : check if the decrementation is allowed ? -pb
        return refCount > 0 ? --refCount : 0;
    }

    @Override
    public int incReferenceCount() {
        //TODO : check uf the incrementation is allowed ? -pb
        if (refCount < Cacheable.MAX_REF) {
            refCount++;
        }
        return refCount;
    }

    @Override
    public void setReferenceCount(final int count) {
        refCount = count;
    }

    @Override
    public void setTimestamp(final int timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    DOMFilePageHeader getPageHeader() {
        return page.getPageHeader();
    }

    long getPageNum() {
        return page.getPageNum();
    }

    @Override
    public boolean isDirty() {
        return !saved;
    }

    void setDirty(final boolean dirty) {
        saved = !dirty;
        page.getPageHeader().setDirty(dirty);
        if (dirty) {
            lastFound.set(0);
        }
    }

    private void write() throws IOException {
        if (page == null) {
            return;
        }

        if (!page.getPageHeader().isDirty()) {
            return;
        }
        page.getPageHeader().setDataLength(len);
        writeValue(page, data);
        setDirty(false);
    }

    String dumpPage() {
        return "Contents of page " + page.getPageNum() + ": " + HexEncoder.bytesToHex(data);
    }

    @Override
    public boolean sync(final boolean syncJournal) throws IOException {
        if (isDirty()) {
            write();
            if (isRecoveryEnabled() && syncJournal && logManager.isPresent() && logManager.get().lastWrittenLsn().compareTo(page.getPageHeader().getLsn()) < 0) {
                logManager.ifPresent(l -> l.flush(true, false));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean allowUnload() {
        return true;
    }

    @Override
    public boolean equals(final Object obj) {
        if(!(obj instanceof DOMPage)) {
            return false;
        }

        final DOMPage other = (DOMPage) obj;
        return page.equals(other.page);
    }

    void invalidate() {
        invalidated = true;
    }

    boolean isInvalidated() {
        return invalidated;
    }

    /**
     * Walk through the page after records have been removed. Set the tid
     * counter to the next spare id that can be used for following
     * insertions.
     */
    void cleanUp() throws IOException {
        final int dlen = page.getPageHeader().getDataLength();
        short maxTupleID = 0;
        short recordCount = 0;
        for (int pos = 0; pos < dlen; recordCount++) {
            final short tupleID = ByteConversion.byteToShort(data, pos);
            pos += DOMFile.LENGTH_TID;
            if (ItemId.getId(tupleID) > ItemId.MAX_ID) {
                throw new IOException("TupleID overflow in page " + getPageNum());
            }
            if (ItemId.getId(tupleID) > maxTupleID) {
                maxTupleID = ItemId.getId(tupleID);
            }
            if (ItemId.isLink(tupleID)) {
                pos += DOMFile.LENGTH_FORWARD_LOCATION;
            } else {
                final short vlen = ByteConversion.byteToShort(data, pos);
                pos += DOMFile.LENGTH_DATA_LENGTH;
                if (ItemId.isRelocated(tupleID)) {
                    pos += vlen == DOMFile.OVERFLOW_PAGE_DATA_LENGTH ?
                        DOMFile.LENGTH_ORIGINAL_LOCATION + DOMFile.LENGTH_OVERFLOW_LOCATION :
                        DOMFile.LENGTH_ORIGINAL_LOCATION + vlen;
                } else {
                    pos += vlen == DOMFile.OVERFLOW_PAGE_DATA_LENGTH ? DOMFile.LENGTH_OVERFLOW_LOCATION : vlen;
                }
            }
        }
        page.getPageHeader().setNextTupleID(maxTupleID);
    }
}
