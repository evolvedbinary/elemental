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
import org.exist.util.ByteConversion;

import javax.annotation.Nullable;
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
        super(fileVersion, pageSize, 1024);
        this.freeList = new FreeList();
    }

    public static BFileHeader createNew(final short version, final int pageSize) {
        return new BFileHeader(version, pageSize);
    }

    public static BFileHeader load(final RandomAccessFile raf) throws IOException {
        // NOTE(AR) as the BFileHeader is a variable size (due to the FreeList) we need to peek at the `headerSize` and setup a buffer that is big enough to read it
        raf.seek(OFFSET_HEADER_SIZE);  // seek to the header size field within the file
        final byte[] headerSizeBytes = new byte[LENGTH_HEADER_SIZE];
        raf.read(headerSizeBytes);
        final short headerSize = ByteConversion.byteToShort(headerSizeBytes, 0);

        final byte[] headerBuf = new byte[headerSize];
        // file header is at the start of the file, so always reposition to the start of the file
        raf.seek(0);
        raf.read(headerBuf);

        final BFileHeaderData bFileHeaderData = readBFileHeaderData(headerBuf);
        return new BFileHeader(
            bFileHeaderData.btreeFileHeaderData.abstractPagedFileHeaderData.version,
            bFileHeaderData.btreeFileHeaderData.abstractPagedFileHeaderData.headerSize,
            bFileHeaderData.btreeFileHeaderData.abstractPagedFileHeaderData.pageSize,
            bFileHeaderData.btreeFileHeaderData.abstractPagedFileHeaderData.pageHeaderSize,
            bFileHeaderData.btreeFileHeaderData.abstractPagedFileHeaderData.maxKeySize,
            bFileHeaderData.btreeFileHeaderData.abstractPagedFileHeaderData.firstFreePage,
            bFileHeaderData.btreeFileHeaderData.abstractPagedFileHeaderData.lastFreePage,
            bFileHeaderData.btreeFileHeaderData.abstractPagedFileHeaderData.totalCount,
            bFileHeaderData.btreeFileHeaderData.rootPage,
            bFileHeaderData.btreeFileHeaderData.fixedLen,
            bFileHeaderData.freeList
        );
    }

    public void addFreeSpace(final FreeSpace freeSpace) {
        freeList.add(freeSpace);
        setDirty(true);
    }

    public @Nullable FreeSpace findFreeSpace(final int needed) {
        return freeList.find(needed);
    }

    public @Nullable FreeSpace getFreeSpace(final long page) {
        return freeList.retrieve(page);
    }

    public void removeFreeSpace(@Nullable final FreeSpace space) {
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
        final FreeList freeList = FreeList.load(headerBuf, OFFSET_FREE_LIST);
        return new BFileHeaderData(btreeFileHeaderData, freeList);
    }

    @Override
    protected int write(final byte[] buf) throws IOException {
        final int offset = super.write(buf);
        final int bytesRemaining = getHeaderSize() - offset;
        final int maxRecords = (bytesRemaining - FreeList.HEADER_SIZE) / FreeList.RECORD_SIZE;
        return freeList.write(maxRecords, buf, offset);
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
