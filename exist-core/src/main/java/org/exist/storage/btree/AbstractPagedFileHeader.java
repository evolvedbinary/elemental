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
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private short version;

    private boolean dirty = false;
    long firstFreePage = Page.NO_PAGE;

    short headerSize;
    long lastFreePage = Page.NO_PAGE;
    private short maxKeySize = 256;
    private long pageCount;
    byte pageHeaderSize = 64;
    int pageSize;
    private long recordCount;
    long totalCount;
    int workSize;

    private final byte[] buf;

    public AbstractPagedFileHeader(final short fileVersion, final long pageCount, final int pageSize) {
        this.pageSize = pageSize;
        this.pageCount = pageCount;
        this.totalCount = pageCount;
        this.headerSize = (short) pageSize;
        this.version = fileVersion;
        this.buf = new byte[headerSize];
        calculateWorkSize();
    }

    private void calculateWorkSize() {
        workSize = pageSize - pageHeaderSize;
    }

    /**
     * Decrement the number of records being managed by the file
     */
    public final void decRecordCount() {
        recordCount--;
        dirty = true;
    }

    /**
     * The first free page in unused secondary space
     *
     * @return The firstFreePage value
     */
    public final long getFirstFreePage() {
        return firstFreePage;
    }

    /**
     * The size of the FileHeader. Usually 1 OS Page
     *
     * @return The size value
     */
    public final short getHeaderSize() {
        return headerSize;
    }

    /**
     * The last free page in unused secondary space
     *
     * @return The lastFreePage value
     */
    public final long getLastFreePage() {
        return lastFreePage;
    }

    /**
     * The maximum number of bytes a key can be. 256 is good
     *
     * @return The maxKeySize value
     */
    public int getMaxKeySize() {
        return maxKeySize;
    }

    /**
     * The number of pages in primary storage
     *
     * @return The pageCount value
     */
    public final long getPageCount() {
        return pageCount;
    }

    /**
     * The size of a page header. 64 is sufficient
     *
     *@return The pageHeaderSize value
     */
    public final byte getPageHeaderSize() {
        return pageHeaderSize;
    }

    /**
     * The size of a page. Usually a multiple of a FS block
     *
     * @return The page size
     */
    public final int getPageSize() {
        return pageSize;
    }

    /**
     *  The number of records being managed by the file (not pages)
     *
     *@return    The number of records
     */
    public final long getRecordCount() {
        return recordCount;
    }

    /**
     * The total number of pages in the file
     *
     * @return The total number of pages
     */
    public final long getTotalCount() {
        return totalCount;
    }

    /**
     * Gets the workSize attribute of the FileHeader object
     *
     * @return The workSize value
     */
    public final int getWorkSize() {
        return workSize;
    }

    public final short getVersion() {
        return version;
    }

    /**
     * Increment the number of records being managed by the file
     */
    public final void incRecordCount() {
        recordCount++;
        dirty = true;
    }

    /**
     * Returns whether this page has been modified or not.
     *
     * @return <code>true</code> if this page has been modified
     */
    public final boolean isDirty() {
        return dirty;
    }

    public final void read(final RandomAccessFile raf) throws IOException {
        raf.seek(0);
        raf.read(buf);
        read(buf);
        calculateWorkSize();
        dirty = false;
    }

    public int read(final byte[] buf) throws IOException {
        version = ByteConversion.byteToShort(buf, AbstractPagedFile.OFFSET_VERSION_ID);
        headerSize = ByteConversion.byteToShort(buf, AbstractPagedFile.OFFSET_HEADER_SIZE);
        pageSize = ByteConversion.byteToInt(buf, AbstractPagedFile.OFFSET_PAGE_SIZE);
        pageCount = ByteConversion.byteToLong(buf, AbstractPagedFile.OFFSET_PAGE_COUNT);
        totalCount = ByteConversion.byteToLong(buf, AbstractPagedFile.OFFSET_TOTAL_COUNT);
        firstFreePage = ByteConversion.byteToLong(buf, AbstractPagedFile.OFFSET_FIRST_FREE_PAGE);
        lastFreePage = ByteConversion.byteToLong(buf, AbstractPagedFile.OFFSET_LAST_FREE_PAGE);
        pageHeaderSize = buf[AbstractPagedFile.OFFSET_PAGE_HEADER_SIZE];
        maxKeySize = ByteConversion.byteToShort(buf, AbstractPagedFile.OFFSET_MAX_KEY_SIZE);
        recordCount = ByteConversion.byteToLong(buf, AbstractPagedFile.OFFSET_RECORD_COUNT);
        return AbstractPagedFile.OFFSET_REMAINDER;
    }

    public int write(final byte[] buf) throws IOException {
        ByteConversion.shortToByte(version, buf, AbstractPagedFile.OFFSET_VERSION_ID);
        ByteConversion.shortToByte(headerSize, buf, AbstractPagedFile.OFFSET_HEADER_SIZE);
        ByteConversion.intToByte(pageSize, buf, AbstractPagedFile.OFFSET_PAGE_SIZE);
        ByteConversion.longToByte(pageCount, buf, AbstractPagedFile.OFFSET_PAGE_COUNT);
        ByteConversion.longToByte(totalCount, buf, AbstractPagedFile.OFFSET_TOTAL_COUNT);
        ByteConversion.longToByte(firstFreePage, buf, AbstractPagedFile.OFFSET_FIRST_FREE_PAGE);
        ByteConversion.longToByte(lastFreePage, buf, AbstractPagedFile.OFFSET_LAST_FREE_PAGE);
        buf[AbstractPagedFile.OFFSET_PAGE_HEADER_SIZE] = pageHeaderSize;
        ByteConversion.shortToByte(maxKeySize, buf, AbstractPagedFile.OFFSET_MAX_KEY_SIZE);
        ByteConversion.longToByte(recordCount, buf, AbstractPagedFile.OFFSET_RECORD_COUNT);
        return AbstractPagedFile.OFFSET_REMAINDER;
    }

    /**
     * Sets the dirty attribute of the FileHeader object
     *
     * @param dirty The new dirty value
     */
    public final void setDirty(final boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * The first free page in unused secondary space
     *
     * @param firstFreePage The new first free page number
     */
    public final void setFirstFreePage(final long firstFreePage) {
        this.firstFreePage = firstFreePage;
        dirty = true;
    }

    /**
     * The size of the FileHeader. Usually 1 OS Page
     *
     * @param headerSize The new headerSize value
     */
    public final void setHeaderSize(final short headerSize) {
        this.headerSize = headerSize;
        dirty = true;
    }

    /**
     * The last free page in unused secondary space
     *
     * @param lastFreePage The new lastFreePage value
     */
    public final void setLastFreePage(final long lastFreePage) {
        this.lastFreePage = lastFreePage;
        dirty = true;
    }

    /**
     * The maximum number of bytes a key can be. 256 is good
     *
     * @param maxKeySize The new maximum size for a key
     */
    public final void setMaxKeySize(final short maxKeySize) {
        this.maxKeySize = maxKeySize;
        dirty = true;
    }

    /**
     * The number of pages in primary storage
     *
     * @param  pageCount  The new pageCount value
     */
    public final void setPageCount(final long pageCount) {
        this.pageCount = pageCount;
        dirty = true;
    }

    /**
     * The size of a page header. 64 is sufficient
     *
     * @param pageHeaderSize The new pageHeaderSize value
     */
    public final void setPageHeaderSize(final byte pageHeaderSize) {
        this.pageHeaderSize = pageHeaderSize;
        calculateWorkSize();
        dirty = true;
    }

    /**
     * The size of a page. Usually a multiple of a FS block
     *
     * @param pageSize The new pageSize value
     */
    public final void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
        calculateWorkSize();
        dirty = true;
    }

    /**
     * The number of records being managed by the file (not pages)
     *
     * @param recordCount The new recordCount value
     */
    public final void setRecordCount(final long recordCount) {
        this.recordCount = recordCount;
        dirty = true;
    }

    /**
     * The number of total pages in the file
     *
     * @param totalCount The new totalCount value
     */
    public final void setTotalCount(final long totalCount) {
        this.totalCount = totalCount;
        dirty = true;
    }

    public final void write(final RandomAccessFile raf) throws IOException {
        raf.seek(0);
        write(buf);
        raf.write(buf);
        dirty = false;
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
}
