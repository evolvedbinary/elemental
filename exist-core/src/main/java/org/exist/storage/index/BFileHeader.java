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

import org.exist.storage.btree.BTreeFileHeader;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * BFile Header.
 * The file header stores the list of data pages containing unused space.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:meier@ifs.tu-darmstadt.de">Wolfgang Meier</a>
 */
public class BFileHeader extends BTreeFileHeader {

    private static final int OFFSET_FREE_LIST = OFFSET_FIXED_LEN + LENGTH_FIXED_LEN;  // 61

    private final FreeList freeList;

    protected BFileHeader(final short version, final short headerSize, final int pageSize, final byte pageHeaderSize, final short maxKeySize, final long firstFreePage, final long lastFreePage, final long totalCount, final long rootPage, final short fixedLen, final FreeList freeList) {
        super(version, headerSize, pageSize, pageHeaderSize, maxKeySize, firstFreePage, lastFreePage, totalCount, rootPage, fixedLen);
        this.freeList = freeList;
    }

    protected BFileHeader(final short fileVersion, final int pageSize) {
        super(fileVersion, 1024, pageSize);
        this.freeList = new FreeList();
    }

    public static BFileHeader createNew(final short version, final int pageSize) {
        return new BFileHeader(version, pageSize);
    }

    public static BTreeFileHeader load(final RandomAccessFile raf) throws IOException {
        // file header is at the start of the file, so always reposition to the start of the file
        raf.seek(0);

        // read the entire AbstractPagedFileHeader from the file
//        final int headerBufSize = OFFSET_FIXED_LEN + LENGTH_FIXED_LEN;  // 61
        // TODO(AR) how can we possibly calculate this?
        final byte[] headerBuf = new byte[headerBufSize];
        raf.read(headerBuf);

        final BTreeFileHeaderData bTreeFileHeaderData = readBTreeFileHeaderData(headerBuf);
        return new BTreeFileHeader(
            bTreeFileHeaderData.abstractPagedFileHeaderData.version,
            bTreeFileHeaderData.abstractPagedFileHeaderData.headerSize,
            bTreeFileHeaderData.abstractPagedFileHeaderData.pageSize,
            bTreeFileHeaderData.abstractPagedFileHeaderData.pageHeaderSize,
            bTreeFileHeaderData.abstractPagedFileHeaderData.maxKeySize,
            bTreeFileHeaderData.abstractPagedFileHeaderData.firstFreePage,
            bTreeFileHeaderData.abstractPagedFileHeaderData.lastFreePage,
            bTreeFileHeaderData.abstractPagedFileHeaderData.totalCount,
            bTreeFileHeaderData.rootPage,
            bTreeFileHeaderData.fixedLen
        );
    }

    public void addFreeSpace(final FreeSpace freeSpace) {
        freeList.add(freeSpace);
        setDirty(true);
    }

    public FreeSpace findFreeSpace(final int needed) {
        return freeList.find(needed);
    }

    public FreeSpace getFreeSpace(final long page) {
        return freeList.retrieve(page);
    }

    public void removeFreeSpace(final FreeSpace space) {
        if (space == null) {
            return;
        }
        freeList.remove(space);
        setDirty(true);
    }

//    public void debugFreeList() {
//        LOG.debug("{}: {}", FileUtils.fileName(getFile()), freeList.toString());
//    }

    protected static BFileHeaderData readBFileHeaderData(final byte[] headerBuf) {
        final BTreeFileHeaderData btreeFileHeaderData = readBTreeFileHeaderData(headerBuf);
        final FreeList freeList = new FreeList();
        freeList.read(headerBuf, OFFSET_FREE_LIST);

        return new BFileHeaderData(btreeFileHeaderData, freeList);
    }

    @Override
    protected int write(final byte[] buf) throws IOException {
        final int offset = super.write(buf);
        return freeList.write(buf, offset);
    }

    protected static class BFileHeaderData {
        final BTreeFileHeaderData btreeFileHeaderData;
        final FreeList freeList;

        private BFileHeaderData(final BTreeFileHeaderData btreeFileHeaderData, final FreeList freeList) {
            this.btreeFileHeaderData = btreeFileHeaderData;
            this.freeList = freeList;
        }
    }
}
