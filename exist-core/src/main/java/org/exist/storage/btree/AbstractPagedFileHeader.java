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
 * NOTE: This file is in part based on code from The dbXML Group.
 * The original license statement is also included below.
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
 *
 * ---------------------------------------------------------------------
 *
 * dbXML License, Version 1.0
 *
 * Copyright (c) 1999-2001 The dbXML Group, L.L.C.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        The dbXML Group (http://www.dbxml.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "dbXML" and "The dbXML Group" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact info@dbxml.com.
 *
 * 5. Products derived from this software may not be called "dbXML",
 *    nor may "dbXML" appear in their name, without prior written
 *    permission of The dbXML Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE DBXML GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.exist.storage.btree;

import org.exist.util.ByteConversion;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class for Paged File Header.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:meier@ifs.tu-darmstadt.de">Wolfgang Meier</a>
 */
public abstract class AbstractPagedFileHeader implements PagedFileHeader {
    //<editor-fold desc="Description of the file header format">
    private static final int LENGTH_VERSION_ID = 2;        // sizeof(short)
    private static final int LENGTH_HEADER_SIZE = 2;       // sizeof(short)
    private static final int LENGTH_PAGE_COUNT = 8;        // sizeof(long)
    private static final int LENGTH_PAGE_SIZE = 4;         // sizeof(int)
    private static final int LENGTH_TOTAL_COUNT = 8;       // sizeof(long)
    private static final int LENGTH_FIRST_FREE_PAGE = 8;   // sizeof(long)
    private static final int LENGTH_LAST_FREE_PAGE = 8;    // sizeof(long)
    private static final int LENGTH_PAGE_HEADER_SIZE = 1;  // sizeof(byte)
    private static final int LENGTH_MAX_KEY_SIZE = 2;      // sizeof(short)
    protected static final int LENGTH_RECORD_COUNT = 8;    // sizeof(long)

    private static final int OFFSET_VERSION_ID = 0;                                                    //  0
    private static final int OFFSET_HEADER_SIZE = OFFSET_VERSION_ID + LENGTH_VERSION_ID;               //  2
    private static final int OFFSET_PAGE_SIZE = OFFSET_HEADER_SIZE + LENGTH_HEADER_SIZE;               //  4
    private static final int OFFSET_PAGE_COUNT = OFFSET_PAGE_SIZE + LENGTH_PAGE_SIZE;                  //  8
    private static final int OFFSET_TOTAL_COUNT = OFFSET_PAGE_COUNT + LENGTH_PAGE_COUNT;               // 16
    private static final int OFFSET_FIRST_FREE_PAGE = OFFSET_TOTAL_COUNT + LENGTH_TOTAL_COUNT;         // 24
    private static final int OFFSET_LAST_FREE_PAGE = OFFSET_FIRST_FREE_PAGE + LENGTH_FIRST_FREE_PAGE;  // 32
    private static final int OFFSET_PAGE_HEADER_SIZE = OFFSET_LAST_FREE_PAGE + LENGTH_LAST_FREE_PAGE;  // 40
    private static final int OFFSET_MAX_KEY_SIZE = OFFSET_PAGE_HEADER_SIZE + LENGTH_PAGE_HEADER_SIZE;  // 41
    protected static final int OFFSET_RECORD_COUNT = OFFSET_MAX_KEY_SIZE + LENGTH_MAX_KEY_SIZE;        // 43
    //</editor-fold>

    private final static byte DEFAULT_PAGE_HEADER_SIZE = 64;
    private final static short DEFAULT_MAX_KEY_SIZE = 256;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    //<editor-fold desc="Immutable state">
    private final short version;
    private final short headerSize;
    private final int pageSize;
    private final byte pageHeaderSize;
    private final short maxKeySize;
    private final int workSize;

    /**
     * This is here so that we can allocate once and reuse multiple times in {@link #write(byte[])}.
     */
    private final byte[] headerBuf;
    // </editor-fold>

    //<editor-fold desc="Mutable state">
    private long totalCount;
    private long firstFreePage;
    private long lastFreePage;
    private boolean dirty;
    //</editor-fold>

    protected AbstractPagedFileHeader(final short version, final short headerSize, final int pageSize, final byte pageHeaderSize, final short maxKeySize, final long firstFreePage, final long lastFreePage, final long totalCount) {
        this.version = version;
        this.headerSize = headerSize;
        this.pageSize = pageSize;
        this.pageHeaderSize = pageHeaderSize;
        this.maxKeySize = maxKeySize;
        this.firstFreePage = firstFreePage;
        this.lastFreePage = lastFreePage;
        this.totalCount = totalCount;
        this.workSize = this.pageSize - this.pageHeaderSize;
        this.headerBuf = new byte[headerSize];
    }

    protected AbstractPagedFileHeader(final short version, final long pageCount, final int pageSize) {
        this(version, (short) pageSize, pageSize, DEFAULT_PAGE_HEADER_SIZE, DEFAULT_MAX_KEY_SIZE, Page.NO_PAGE, Page.NO_PAGE, pageCount);
    }

    /**
     * Get the file version.
     *
     * @return the file version.
     */
    public short getVersion() {
        return this.version;
    }

    /**
     * Get the size of the header.
     *
     * @return The header size.
     */
    public short getHeaderSize() {
        return this.headerSize;
    }

    /**
     * Get the size of a page.
     *
     * @return The page size.
     */
    public int getPageSize() {
        return this.pageSize;
    }

    /**
     * Get the total number of pages in the file.
     *
     * @return the total number of pages in the file.
     */
    public long getTotalCount() {
        return this.totalCount;
    }

    /**
     * Set the total number of pages in the file.
     *
     * @param totalCount the total number of pages in the file.
     */
    public void setTotalCount(final long totalCount) {
        this.totalCount = totalCount;
        setDirty(true);
    }

    /**
     * Get the first free page.
     *
     * @return the first free page.
     */
    public long getFirstFreePage() {
        return this.firstFreePage;
    }

    /**
     * Set the first free page.
     *
     * @param firstFreePage the first free page.
     */
    public void setFirstFreePage(final long firstFreePage) {
        this.firstFreePage = firstFreePage;
        setDirty(true);
    }

    /**
     * Get the last free page.
     *
     * @return The last free page.
     */
    public long getLastFreePage() {
        return this.lastFreePage;
    }

    /**
     * Set the last free page.
     *
     * @param lastFreePage the first free page.
     */
    public void setLastFreePage(final long lastFreePage) {
        this.lastFreePage = lastFreePage;
        setDirty(true);
    }

    /**
     * Get the page header size.
     *
     * @return the page header size.
     */
    public byte getPageHeaderSize() {
        return pageHeaderSize;
    }

    /**
     * The maximum number of bytes a key can be. 256 is good
     *
     * @return The maxKeySize value
     */
    public int getMaxKeySize() {
        return this.maxKeySize;
    }

    /**
     * Gets the workSize attribute of the FileHeader object
     *
     * @return The workSize value
     */
    public int getWorkSize() {
        return this.workSize;
    }

    /**
     * Determine if the header has been modified.
     *
     * @return true if the header has been modified, false otherwise.
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Set whether the header has been modified.
     *
     * @param dirty true if the header has been modified, false otherwise.
     */
    public void setDirty(final boolean dirty) {
        this.dirty = dirty;
    }

    protected static AbstractPagedFileHeaderData readAbstractPagedFileHeaderData(final byte[] headerBuf) {
        final short version = ByteConversion.byteToShort(headerBuf, OFFSET_VERSION_ID);
        final short headerSize = ByteConversion.byteToShort(headerBuf, OFFSET_HEADER_SIZE);
        final int pageSize = ByteConversion.byteToInt(headerBuf, OFFSET_PAGE_SIZE);
        // NOTE(AR) pageCount no longer seems to be needed
//        final long pageCount = ByteConversion.byteToLong(buf, OFFSET_PAGE_COUNT);
        final long totalCount = ByteConversion.byteToLong(headerBuf, OFFSET_TOTAL_COUNT);
        final long firstFreePage = ByteConversion.byteToLong(headerBuf, OFFSET_FIRST_FREE_PAGE);
        final long lastFreePage = ByteConversion.byteToLong(headerBuf, OFFSET_LAST_FREE_PAGE);
        final byte pageHeaderSize = headerBuf[OFFSET_PAGE_HEADER_SIZE];
        final short maxKeySize = ByteConversion.byteToShort(headerBuf, OFFSET_MAX_KEY_SIZE);
        // NOTE(AR) recordCount no longer seems to be needed
//        final long recordCount = ByteConversion.byteToLong(buf, OFFSET_RECORD_COUNT);

        return new AbstractPagedFileHeaderData(version, headerSize, pageSize, totalCount, firstFreePage, lastFreePage, pageHeaderSize, maxKeySize);
    }

    protected int write(final byte[] buf) throws IOException {
        ByteConversion.shortToByte(this.version, buf, OFFSET_VERSION_ID);
        ByteConversion.shortToByte(this.headerSize, buf, OFFSET_HEADER_SIZE);
        ByteConversion.intToByte(this.pageSize, buf, OFFSET_PAGE_SIZE);
        // NOTE(AR) pageCount no longer seems to be needed
//        ByteConversion.longToByte(this.pageCount, buf, OFFSET_PAGE_COUNT);
        ByteConversion.longToByte(this.totalCount, buf, OFFSET_TOTAL_COUNT);
        ByteConversion.longToByte(this.firstFreePage, buf, OFFSET_FIRST_FREE_PAGE);
        ByteConversion.longToByte(this.lastFreePage, buf, OFFSET_LAST_FREE_PAGE);
        buf[OFFSET_PAGE_HEADER_SIZE] = this.pageHeaderSize;
        ByteConversion.shortToByte(this.maxKeySize, buf, OFFSET_MAX_KEY_SIZE);
        // NOTE(AR) recordCount no longer seems to be needed
//        ByteConversion.longToByte(this.recordCount, buf, OFFSET_RECORD_COUNT);
        return OFFSET_RECORD_COUNT + LENGTH_RECORD_COUNT;  // 51
    }

    public void write(final RandomAccessFile raf) throws IOException {
        raf.seek(0);
        write(this.headerBuf);
        raf.write(this.headerBuf);
        this.dirty = false;
    }

    public void checkVersion(final short requiredVersion, final String filename) throws IOException {
        if (getVersion() != requiredVersion) {
            throw new IOException("Database file: " +
                filename + " has a storage format incompatible with this " +
                "version of eXist-db. You need to upgrade your database by creating a backup, " +
                "cleaning your data directory and restoring the data. In some cases, " +
                "a reindex may be sufficient. " +
                "Please follow the instructions for the version you installed. " +
                "On-disk file version has storage format: " + getVersion() +
                "; However this version of eXist-db requires storage format version: " + requiredVersion);
        }
    }

    public ReentrantReadWriteLock.ReadLock readLock() {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        readLock.lock();
        return readLock;
    }

    public ReentrantReadWriteLock.WriteLock writeLock() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        return writeLock;
    }


    protected static class AbstractPagedFileHeaderData {
        final short version;
        final short headerSize;
        final int pageSize;
        final long totalCount;
        final long firstFreePage;
        final long lastFreePage;
        final byte pageHeaderSize;
        final short maxKeySize;

        private AbstractPagedFileHeaderData(final short version, final short headerSize, final int pageSize, final long totalCount, final long firstFreePage, final long lastFreePage, final byte pageHeaderSize, final short maxKeySize) {
            this.version = version;
            this.headerSize = headerSize;
            this.pageSize = pageSize;
            this.totalCount = totalCount;
            this.firstFreePage = firstFreePage;
            this.lastFreePage = lastFreePage;
            this.pageHeaderSize = pageHeaderSize;
            this.maxKeySize = maxKeySize;
        }
    }
}
