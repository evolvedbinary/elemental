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

import org.exist.storage.journal.Lsn;
import org.exist.util.FileUtils;
import org.exist.xquery.Constants;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A page in a paged file.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
public class Page<PAGE_HEADER extends PageHeader> implements Comparable<Page> {

    public static final long NO_PAGE = -1;

    /**  The Header for this Page */
    final PAGE_HEADER header;

    /**  The offset into the file that this page starts */
    private long offset;
    /**  This page number */
    long pageNum;

    private int refCount = 0;

    // TODO(AR) is this still a good idea to reuse these across multiple instances?
    private final byte[] tempPageData;
    private final byte[] tempHeaderData;

    public Page(final byte[] tempPageData, final byte[] tempHeaderData) {
        this.header = createPageHeader();
        this.tempPageData = tempPageData;
        this.tempHeaderData = tempHeaderData;
    }

    /**
     * Constructor for the Page object
     *
     * @param pageNum Description of the Parameter
     *
     * @throws IOException Description of the Exception
     */
    public Page(final byte[] tempPageData, final byte[] tempHeaderData, final long pageNum) throws IOException {
        this(tempPageData, tempHeaderData);
        if(pageNum == Page.NO_PAGE) {
            throw new IOException("Illegal page num: " + pageNum);
        }
        setPageNum(pageNum);
    }

    public void decRefCount() {
        refCount--;
    }

    /**
     * Gets the offset attribute of the Page object
     *
     * @return The offset value
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Gets the pageHeader attribute of the Page object
     *
     * @return The pageHeader value
     */
    public PAGE_HEADER getPageHeader() {
        return header;
    }

    /**
     * Gets the pageInfo attribute of the Page object
     *
     * @return The pageInfo value
     */
    public String getPageInfo() {
        final byte pageHeaderSize;
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            pageHeaderSize = fileHeader.getPageHeaderSize();
        } finally {
            fileHeaderReadLock.unlock();
        }

        return "page: " + pageNum +
            "; file = " + FileUtils.fileName(getFile()) +
            "; address = " + Long.toHexString(offset) +
            "; page header = " + pageHeaderSize +
            "; data start = " + Long.toHexString(offset + pageHeaderSize);
    }

    public long getPageNum() {
        return pageNum;
    }

    public int getRefCount() {
        return refCount;
    }

    public int getDataPos() {
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            return fileHeader.pageHeaderSize;
        } finally {
            fileHeaderReadLock.unlock();
        }
    }

    public void incRefCount() {
        refCount++;
    }

    public byte[] read(final RandomAccessFile raf) throws IOException {
        try {
            if (raf.getFilePointer() != offset) {
                raf.seek(offset);
            }
            Arrays.fill(tempHeaderData, (byte)0);
            raf.read(tempHeaderData);
            // Read in the header
            header.read(tempHeaderData, 0);
            // Read the working data
            final byte[] workData = new byte[header.getDataLen()];
            raf.read(workData);
            return workData;
        } catch(final Exception e) {
            LOG.warn("error while reading page: {}", getPageInfo(), e);
            throw new IOException(e.getMessage());
        }
    }

    public void setPageNum(final long pageNum) {
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            this.pageNum = pageNum;
            this.offset = fileHeader.headerSize + (pageNum * fileHeader.pageSize);
        } finally {
            fileHeaderReadLock.unlock();
        }
    }

    public void remove(final RandomAccessFile raf) throws IOException {
        write(raf, null);
    }

    void write(final RandomAccessFile raf, final byte[] data) throws IOException {
        if(data == null) {
            // Removed page: fill with 0
            Arrays.fill(tempPageData, (byte)0);
            header.setLsn(Lsn.LSN_INVALID);
        }
        // Write out the header
        header.write(tempPageData, 0);
        header.setDirty(false);
        if (data != null) {
            final int workSize;
            final byte pageHeaderSize;
            final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
            try {
                workSize = fileHeader.workSize;
                pageHeaderSize = fileHeader.pageHeaderSize;
            } finally {
                fileHeaderReadLock.unlock();
            }

            if (data.length > workSize) {
                throw new IOException("page: " + getPageInfo() + ": data length too large: " + data.length);
            } else {
                System.arraycopy(data, 0, tempPageData, pageHeaderSize, data.length);
            }
        }
        if (raf.getFilePointer() != offset) {
            raf.seek(offset);
        }
        raf.write(tempPageData);
    }

    @Override
    public boolean equals(final Object obj) {
        return ((Page)obj).pageNum == pageNum;
    }

    @Override
    public int compareTo(final Page other) {
        if (pageNum == other.pageNum) {
            return Constants.EQUAL;
        } else if(pageNum > other.pageNum) {
            return Constants.SUPERIOR;
        } else {
            return Constants.INFERIOR;
        }
    }

    public void dumpPage(final RandomAccessFile raf) throws IOException {
        if (raf.getFilePointer() != offset) {
            raf.seek(offset);
        }
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            final byte[] data = new byte[fileHeader.pageSize];
            raf.read(data);
            LOG.debug("Contents of page {}: {}", pageNum, HexEncoder.bytesToHex(data));
        } finally {
            fileHeaderReadLock.unlock();
        }
    }
}
