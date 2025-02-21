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

import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.storage.BrokerPool;
import org.exist.storage.journal.Lsn;
import org.exist.util.FileUtils;

import java.lang.AutoCloseable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  Paged is a paged file foundation that is used by the BTree class and
 *  its subclasses.
 */
public abstract class AbstractPagedFile<HEADER extends AbstractPagedFileHeader, PAGE_HEADER extends AbstractPageHeader> implements PagedFile<HEADER, PAGE_HEADER>, AutoCloseable {

    protected static final Logger LOG = LogManager.getLogger(AbstractPagedFile.class);

    protected static int PAGE_SIZE = 4096;

    protected final short fileVersion;
    @GuardedBy("FileHeader#lock")
    protected final HEADER fileHeader;
    private final byte[] tempPageData;
    private final byte[] tempHeaderData;

    protected RandomAccessFile raf;
    private Path file;
    private boolean readOnly = false;
    private boolean fileIsNew = false;
	
    protected AbstractPagedFile(final BrokerPool pool, final short fileVersion) {
        this.fileVersion = fileVersion;
        this.fileHeader = createFileHeader(pool.getPageSize());
        this.tempPageData = new byte[fileHeader.getPageSize()];
        this.tempHeaderData = new byte[fileHeader.getPageHeaderSize()];
    }

    public final static void setPageSize(final int pageSize) {
        PAGE_SIZE = pageSize;
    }

    public final static int getPageSize() {
        return PAGE_SIZE;
    }

    public final boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Close the underlying files.
     *
     * @throws DBException if an error occurs whilst closing
     */
    @Override
    public void close() throws DBException {
        try {
            raf.close();
        } catch (final IOException e) {
            throw new DBException("An error occurred whilst closing the database file '"
                    + file == null ? "null" : FileUtils.fileName(file) + "': " + e.getMessage(), e);
        }
    }

    /**
     * Completely close down the instance and
     * all underlying resources and caches.
     *
     * @throws DBException if an error occurs whilst closing and removing the file
     */
    public final void closeAndRemove() throws DBException {
        close();
        FileUtils.deleteQuietly(file);
    }

    public boolean create() throws DBException {
        final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
        try {
            fileHeader.write(raf);
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            throw new DBException(0, "Error creating " + FileUtils.fileName(file));
        } finally {
            fileHeaderWriteLock.unlock();
        }
    }

    public boolean exists() {
        return !fileIsNew;
    }

    /**
     * Flushes {@link AbstractPagedFile#flush()} dirty data to the disk and cleans up the cache.
     * @return <code>true</code> if something has actually been cleaned
     * @throws DBException if an error occurs
     */
    public boolean flush() throws DBException {
        boolean flushed = false;
        final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
        try {
            if(fileHeader.isDirty() && !readOnly) {
                fileHeader.write(raf);
                flushed = true;
            }
        } catch (final IOException ioe) {
            throw new DBException(ioe);
        } finally {
            fileHeaderWriteLock.unlock();
        }
        return flushed;
    }

    /**
     * Backup the entire contents of the underlying file to 
     * an output stream.
     * 
     * @param os the output stream
     * @throws IOException if an I/O error occurs
     */
    public void backupToStream(final OutputStream os) throws IOException {
        raf.seek(0);
        final byte[] buf = new byte[4096];
        int len;
        while ((len = raf.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
    }

    /**
     * getPath returns the file object for this Paged.
     *
     * @return The File
     */
    public final Path getFile() {
        return file;
    }

    /**
     * getFileHeader returns the FileHeader
     *
     * @return The FileHeader
     */
//    public FileHeader getFileHeader() {
//        return fileHeader;
//    }

    protected final Page<PAGE_HEADER> getFreePage() throws IOException {
        return getFreePage(true);
    }

    /**
     * Returns the first free page it can find, either by reusing a deleted page
     * or by appending a new one to secondary storage.
     *
     * @param reuseDeleted if set to false, the method will not try to reuse a
     * previously deleted page. This is required by btree page split operations to avoid 
     * concurrency conflicts within a transaction.
     *
     * @return a free page
     *
     * @throws IOException if an I/O error occurs
     */
    protected Page<PAGE_HEADER> getFreePage(final boolean reuseDeleted) throws IOException {
        final Page<PAGE_HEADER> page;
        final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
        try {
            long pageNum = fileHeader.getFirstFreePage();
            if (reuseDeleted && pageNum != Page.NO_PAGE) {

                // Steal a deleted page
                page = new Page<>(tempPageData, tempHeaderData, pageNum);
                page.read(raf);

                fileHeader.setFirstFreePage(page.header.getNextPage());
                if (fileHeader.getFirstFreePage() == Page.NO_PAGE) {
                    fileHeader.setLastFreePage(Page.NO_PAGE);
                }
            } else {
                // Grow the file
                pageNum = fileHeader.getTotalCount();
                if(pageNum == Integer.MAX_VALUE) {
                    throw new IOException("page limit reached: " + pageNum);
                }
                fileHeader.setTotalCount(pageNum + 1);

                page = new Page<>(tempPageData, tempHeaderData, pageNum);
                page.read(raf);
            }

            // Cleanly initialize The Page Header
            page.header.updateNextPage(Page.NO_PAGE);
            page.header.updateStatus(PageStatus.UNUSED);
            fileHeader.setDirty(true);

            // write out the file header
            fileHeader.write(raf);

        } finally {
            fileHeaderWriteLock.unlock();
        }

        return page;
    }

    /**
     * getPage returns the page specified by pageNum.
     *
     * @param pageNum The Page number
     *
     * @return The requested Page
     * @throws IOException if an exception occurs
     */
    protected final Page<PAGE_HEADER> getPage(final long pageNum) throws IOException {
        return new Page<>(tempPageData, tempHeaderData, pageNum);
    }

    /**
     * Gets the opened attribute of the Paged object
     *
     * @return The opened value
     */
    public boolean isOpened() {
        return true;
    }

    /**
     * @param requiredVersion The required version of the file
     * @return true if opened
     * @throws DBException if the paged file cannot be opened
     */
    public boolean open(final short requiredVersion) throws DBException {
        try {
            if (exists()) {
                final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
                try {
                    fileHeader.read(raf);
                    if (fileHeader.getVersion() != requiredVersion) {
                        throw new DBException("Database file " +
                            FileUtils.fileName(getFile()) + " has a storage format incompatible with this " +
                            "version of eXist. You need to upgrade your database by creating a backup, " +
                            "cleaning your data directory and restoring the data. In some cases, " +
                            "a reindex may be sufficient. " +
                            "Please follow the instructions for the version you installed. " +
                            "File version is: " + fileHeader.getVersion() +
                            "; db requires version: " + requiredVersion);
                    }
                } finally {
                    fileHeaderWriteLock.unlock();
                }
                return true;
            } else {
                return false;
            }
        } catch (final Exception e) {
            e.printStackTrace();
            throw new DBException(0, "Error opening " + FileUtils.fileName(file) + ": " + e.getMessage());
        }
    }

    /**
     * Debug.
     *
     * @param out the output print stream
     *
     * @throws IOException Description of the Exception
     */
    public void printFreeSpaceList(final PrintStream out) throws IOException {
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            long pageNum = fileHeader.getFirstFreePage();
            out.println("first free page: " + pageNum);
            Page<PAGE_HEADER> next;
            out.println("free pages for " + FileUtils.fileName(getFile()));
            while (pageNum != Page.NO_PAGE) {
                next = getPage(pageNum);
                next.read(raf);
                out.print(pageNum + ";");
                pageNum = next.header.getNextPage();
            }
            out.println();
        } finally {
            fileHeaderReadLock.unlock();
        }
    }

    /**
     * setFile sets the file object for this Paged.
     *
     * @param file The File
     *
     * @throws DBException if a database error occurs
     */
    protected final void setFile(final Path file) throws DBException {
        this.file = file;
        fileIsNew = !Files.exists(file);
        try {
            if ((!Files.exists(file)) || Files.isWritable(file)) {
                try {
                    raf = new RandomAccessFile(file.toFile(), "rw");
                    final FileChannel channel = raf.getChannel();   
                    final FileLock lock = channel.tryLock();
                    if (lock == null) {
                        readOnly = true;
                    }
                //TODO : who will release the lock ? -pb
                } catch (final NonWritableChannelException e) {
                    //No way : switch to read-only mode
                    readOnly = true;
                    raf = new RandomAccessFile(file.toFile(), "r");
                    LOG.warn(e);
                }
            } else {
                readOnly = true;
                raf = new RandomAccessFile(file.toFile(), "r");
            }
        } catch (final IOException e) {
            LOG.warn("An exception occurred while opening database file {}: {}", file.toAbsolutePath().toString(), e.getMessage(), e);
        }
    }

    /**
     * Unlinks a set of pages starting at the specified page.
     *
     * @param page The starting Page to unlink
     * @throws IOException If an exception occurs
     */
    protected void unlinkPages(final Page<PAGE_HEADER> page) throws IOException {
        //Mmmmh... is this null test accurate ? -pb
        if (page != null) {
            // Walk the chain and add it to the unused list
            page.header.updateStatus(PageStatus.UNUSED);
            page.header.setLsn(Lsn.LSN_INVALID);
            final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
            try {
                if (fileHeader.getFirstFreePage() == Page.NO_PAGE) {
                    fileHeader.setFirstFreePage(page.pageNum);
                    page.header.updateNextPage(Page.NO_PAGE);
                } else {
                    final long firstFreePage = fileHeader.getFirstFreePage();
                    fileHeader.setFirstFreePage(page.pageNum);
                    page.header.updateNextPage(firstFreePage);
                }
                page.remove(raf);
                fileHeader.setDirty(true);
            } finally {
                fileHeaderWriteLock.unlock();
            }
        }
    }

    /**
     * Unlinks a set of pages starting at the specified page
     * number.
     *
     * @param pageNum A page number
     * @throws IOException if an exception occurs
     */
    protected final void unlinkPages(final long pageNum) throws IOException {
        unlinkPages(getPage(pageNum));
    }

    /**
     * Clears the {@link AbstractPagedFileHeader#getFirstFreePage()} and
     *  {@link AbstractPagedFileHeader#getLastFreePage()}.
     *
     * This is needed in recovery, as the free page list
     * may have become corrupted.
     *
     * Unfortunately this means we loose some space
     * that we will never recover, but it does mean
     * we are more likely to correctly recover.
     *
     * @throws IOException if an exception occurs
     */
    protected void dropFreePageList() throws IOException {
        boolean updated = false;
        final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
        try {
            if (fileHeader.getFirstFreePage() != Page.NO_PAGE) {
                fileHeader.setFirstFreePage(Page.NO_PAGE);
                updated = true;
            }
            if (fileHeader.getLastFreePage() != Page.NO_PAGE) {
                fileHeader.setLastFreePage(Page.NO_PAGE);
                updated = true;
            }

            if (updated) {
                fileHeader.write(raf);
            }
        } finally {
            fileHeaderWriteLock.unlock();
        }
    }

    protected void reuseDeleted(final Page<PAGE_HEADER> page) throws IOException {
        final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
        try {
            if (page != null && fileHeader.getFirstFreePage() != Page.NO_PAGE) {

                long firstFreePageNum = fileHeader.getFirstFreePage();
                if (firstFreePageNum == page.pageNum) {
                    fileHeader.setFirstFreePage(page.header.getNextPage());
                    fileHeader.write(raf);
                    return;
                }

                Page<PAGE_HEADER> firstFreePage = getPage(firstFreePageNum);
                firstFreePage.read(raf);
                firstFreePageNum = firstFreePage.header.getNextPage();

                while (firstFreePageNum != Page.NO_PAGE) {
                    if (firstFreePageNum == page.pageNum) {
                        firstFreePage.header.updateNextPage(page.header.getNextPage());
                        firstFreePage.header.setDirty(true);
                        firstFreePage.write(raf,null);
                        return;
                    }
                    firstFreePage = getPage(firstFreePageNum);
                    firstFreePage.read(raf);
                    firstFreePageNum = firstFreePage.header.getNextPage();
                }
            }
        } finally {
            fileHeaderWriteLock.unlock();
        }
    }

    /**
     * Writes the multi-paged value starting at the specified Page.
     *
     * @param page The starting Page
     * @param value The value to write
     *
     * @throws IOException if an Exception occurs
     */
    protected final void writeValue(final Page page, final Value value) throws IOException {
        final byte[] data = value.getData();
        writeValue(page, data);
    }

    protected final void writeValue(final Page<PAGE_HEADER> page, final byte[] data) throws IOException {
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        final int workSize;
        try {
            workSize = fileHeader.getWorkSize();
        } finally {
            fileHeaderReadLock.unlock();
        }
        final PAGE_HEADER pageHeader = page.getPageHeader();
        pageHeader.updateDataLen(workSize);
        if (data.length != pageHeader.getDataLen()) {
            //TODO : where to get this 64 from ?
            if (pageHeader.getDataLen() != getPageSize() - 64) {
                LOG.warn("ouch: {} != {}", workSize, data.length);
            }
            pageHeader.updateDataLen(data.length);
        }
        page.write(raf, data);
    }

    /**
     * Writes the multi-Paged Value starting at the specified page number.
     *
     * @param page The starting page number
     * @param value The Value to write
     * @throws IOException if an Exception occurs
     */
    protected final void writeValue(final long page, final Value value) throws IOException {
        writeValue(getPage(page), value);
    }
}
