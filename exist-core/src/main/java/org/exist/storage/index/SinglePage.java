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

import org.exist.storage.btree.Page;
import org.exist.storage.btree.PageStatus;
import org.exist.storage.btree.Value;
import org.exist.util.ByteConversion;
import org.exist.util.FileUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a single data page (as opposed to an overflow page, see: {@link OverflowPage}).
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
class SinglePage extends AbstractDataPage {

    // the raw working data of this page (without page header)
    byte[] data = null;

    // the low-level page
    final Page<BFilePageHeader> page;

    // the page header
    final BFilePageHeader ph;

    // table mapping record ids (tids) to offsets
    short[] offsets = null;

    public SinglePage() throws IOException {
        this(true);
    }

    public SinglePage(final boolean compress) throws IOException {
        page = getFreePage();
        ph = page.getPageHeader();
        ph.updateStatus(PageStatus.RECORD);
        ph.setDirty(true);
        ph.setDataLength(0);
        //ph.setNextChunk( -1 );
        final ReentrantReadWriteLock.ReadLock fileHeaderReadlock = fileHeader.readLock();
        try {
            data = new byte[fileHeader.getWorkSize()];
        } finally {
            fileHeaderReadlock.unlock();
        }
        offsets = new short[32];
        ph.setNextTID((short) 32);
        Arrays.fill(offsets, (short)-1);
    }

    public SinglePage(final Page<BFilePageHeader> p, final byte[] data, final boolean initialize) throws IOException {
        if (p == null) {
            throw new IOException("illegal page");
        }

        if (!(p.getPageHeader().getStatus() == PageStatus.RECORD || p.getPageHeader()
            .getStatus() == PageStatus.MULTI_PAGE)) {
            final IOException e = new IOException("not a data-page: "
                + p.getPageHeader().getStatus());
            LOG.debug("not a data-page: {}", p.getPageInfo(), e);
            throw e;
        }
        this.data = data;
        page = p;
        ph = page.getPageHeader();
        if(initialize) {
            offsets = new short[ph.getCurrentTID()];
            if (ph.getStatus() != PageStatus.MULTI_PAGE) {
                readOffsets();
            }
        }
    }

    @Override
    public final int findValuePosition(final short tid) throws IOException {
        return offsets[tid];
    }

    private void readOffsets() {
        //if(offsets.length > 256)
        //LOG.warn("TID size: " + ph.nextTID);
        Arrays.fill(offsets, (short)-1);
        final int dlen = ph.getDataLength();
        for(short pos = 0; pos < dlen; ) {
            final short tid = ByteConversion.byteToShort(data, pos);
            if (tid < 0) {
                LOG.error("Invalid tid found: {}; ignoring rest of page ...", tid);
                ph.setDataLength(pos);
                return;
            }
            if(tid >= offsets.length) {
                LOG.error("Problematic tid found: {}; trying to recover ...", tid);
                final short[] t = new short[tid + 1];
                Arrays.fill(t, (short)-1);
                System.arraycopy(offsets, 0, t, 0, offsets.length);
                offsets = t;
                ph.setNextTID((short)(tid + 1));
            }
            offsets[tid] = (short)(pos + 2);
            pos += ByteConversion.byteToInt(data, pos + 2) + 6;
        }
    }

    @Override
    public short getNextTID() {
        for(short i = 0; i < offsets.length; i++) {
            if(offsets[i] == -1) {
                return i;
            }
        }
        final short tid = (short)offsets.length;
        final short next = (short)(ph.getCurrentTID() * 2);
        if (next < 0 || next < ph.getCurrentTID()) {
            return -1;
        }
        final short[] t = new short[next];
        Arrays.fill(t, (short)-1);
        System.arraycopy(offsets, 0, t, 0, offsets.length);
        offsets = t;
        ph.setNextTID(next);
        return tid;
    }

    public void adjustTID(final short tid) {
        if (tid >= ph.getCurrentTID()) {
            final short next = (short)(tid * 2);
            final short[] t = new short[next];
            Arrays.fill(t, (short)-1);
            System.arraycopy(offsets, 0, t, 0, offsets.length);
            offsets = t;
            ph.setNextTID(next);
        }
    }

    public void clear() {
        Arrays.fill(data, (byte) 0);
    }

    String printContents() {
        final StringBuilder buf = new StringBuilder();
        for (short i = 0; i < offsets.length; i++) {
            if (offsets[i] > -1) {
                buf.append('[').append(i).append(", ").append(offsets[i]);
                final short len = ByteConversion.byteToShort(data, offsets[i]);
                buf.append(", ").append(len).append(']');
            }
        }
        return buf.toString();
    }

    @Override
    public void setOffset(final short tid, final int offset) {
        if (offsets == null) {
            LOG.warn("page: {} file: {} status: {}", page.getPageNum(), FileUtils.fileName(getFile()), getPageHeader().getStatus());
            throw new RuntimeException("page offsets not initialized");
        }
        offsets[tid] = (short)offset;
    }

    @Override
    public void removeTID(final short tid, final int length) throws IOException {
        final int offset = offsets[tid] - 2;
        offsets[tid] = -1;
        for(short i = 0; i < offsets.length; i++) {
            if(offsets[i] > offset) {
                offsets[i] -= length;
            }
        }
        //readOffsets(start);
    }

    @Override
    public void delete() throws IOException {
        // reset page header fields
        ph.setDataLength(0);
        ph.setNextInChain(-1L);
        ph.setLastInChain(-1L);
        ph.setNextTID((short) -1);
        ph.setRecordCount((short) 0);
        setReferenceCount(0);
        ph.setDirty(true);
        unlinkPages(page);
    }

    @Override
    public SinglePage getFirstPage() {
        return this;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public BFilePageHeader getPageHeader() {
        return ph;
    }

    @Override
    public String getPageInfo() {
        return page.getPageInfo();
    }

    @Override
    public long getPageNum() {
        return page.getPageNum();
    }

    @Override
    public void setData(final byte[] buf) {
        data = buf;
    }

    @Override
    public void write() throws IOException {
        //LOG.debug(getPath().getName() + " writing page " + getPageNum());
        writeValue(page, new Value(data));
        setDirty(false);
    }
}
