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

import com.evolvedbinary.j8fu.tuple.Tuple2;
import net.jcip.annotations.GuardedBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.storage.journal.Lsn;
import org.exist.util.FileUtils;
import org.exist.util.HexEncoder;
import org.exist.xquery.Constants;

import javax.annotation.Nullable;
import java.lang.AutoCloseable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;

/**
 *  Paged is a paged file foundation that is used by the BTree class and
 *  its subclasses.
 */
public abstract class AbstractPagedFile<HEADER extends AbstractPagedFileHeader, PAGE_HEADER extends AbstractPageHeader> implements PagedFile<HEADER, PAGE_HEADER>, AutoCloseable {

    protected static final Logger LOG = LogManager.getLogger(AbstractPagedFile.class);

    @GuardedBy("FileHeader#lock")
    protected final HEADER fileHeader;
    private final byte[] tempPageData;
    private final byte[] tempHeaderData;

    protected final BackingFile backingFile;

    /**
     * The size in bytes of the area within a page for writing content.
     * This is simply pre-calculated as {@code pageSize - pageHeaderSize}.
     */
    private final int pageContentSize;
	
    protected AbstractPagedFile(final BackingFile backingFile, final HEADER fileHeader) {
        this.backingFile = backingFile;
        this.fileHeader = fileHeader;
        this.tempPageData = new byte[fileHeader.getPageSize()];
        this.tempHeaderData = new byte[fileHeader.getPageHeaderSize()];
        this.pageContentSize = this.fileHeader.getPageSize() - this.fileHeader.getPageHeaderSize();
    }

    /**
     * Get the size in bytes of the area within a page for writing content.
     *
     * @return the page content size.
     */
    public int getPageContentSize() {
        return this.pageContentSize;
    }

    public final boolean isReadOnly() {
        return backingFile.fileLock.isShared();
    }

    /**
     * Close the underlying files.
     *
     * @throws DBException if an error occurs whilst closing
     */
    @Override
    public void close() throws DBException {
        try {
            backingFile.randomAccessFile.close();
            backingFile.fileLock.close();
        } catch (final IOException e) {
            throw new DBException("An error occurred whilst closing the database file: '" + FileUtils.fileName(backingFile.path) + "': " + e.getMessage(), e);
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
        FileUtils.deleteQuietly(backingFile.path);
    }

    /**
     * Flushes dirty data to the disk and cleans up the cache.
     * @return <code>true</code> if something has actually been cleaned
     * @throws DBException if an error occurs
     */
    public boolean flush() throws DBException {
        boolean flushed = false;
        final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
        try {
            if (fileHeader.isDirty() && !isReadOnly()) {
                fileHeader.write(backingFile.randomAccessFile);
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
        backingFile.randomAccessFile.seek(0);
        final byte[] buf = new byte[4096];
        int len;
        while ((len = backingFile.randomAccessFile.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
    }

    /**
     * getPath returns the file object for this Paged.
     *
     * @return The File
     */
    public final Path getFile() {
        return backingFile.path;
    }

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
                page = createPage(pageNum);
                page.read(backingFile.randomAccessFile);

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

                page = createPage(pageNum);
                page.read(backingFile.randomAccessFile);
            }

            // Cleanly initialize The Page Header
            page.header.updateNextPage(Page.NO_PAGE);
            page.header.updateType(PageType.UNUSED);
            fileHeader.setDirty(true);

            // write out the file header
            fileHeader.write(backingFile.randomAccessFile);

        } finally {
            fileHeaderWriteLock.unlock();
        }

        return page;
    }

    /**
     * Create page returns a new page specified by pageNum.
     *
     * @param pageNum The Page number
     *
     * @return The requested Page
     *
     * @throws IOException if an exception occurs
     */
    protected final Page<PAGE_HEADER> createPage(final long pageNum) throws IOException {
        if (pageNum == Page.NO_PAGE) {
            throw new IOException("Illegal page num: " + pageNum);
        }
        return new Page<>(createPageHeader(), pageNum, tempPageData, tempHeaderData);
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
                next = createPage(pageNum);
                next.read(backingFile.randomAccessFile);
                out.print(pageNum + ";");
                pageNum = next.header.getNextPage();
            }
            out.println();
        } finally {
            fileHeaderReadLock.unlock();
        }
    }

    /**
     * Opens a Random Access File with an appropriate filesystem lock.
     * Note that the caller is responsible for closing the file and releasing the lock
     * when they are finished with the file.
     *
     * @param path the path to the file to open/create.
     *
     * @param fallbackToReadOnly when true if the file is not writable, then it will be opened in read-only mode.
     *                           When false if the file cannot be opened in read-write mode then an exception will
     *                           be thrown.
     *
     * @return the file lock and the random access file. You can determine if the file was opened in read-only
     *         mode by calling {@link FileLock#isShared()}.
     *
     * @throws IOException if the file cannot be opened and locked as intended.
     */
    protected static BackingFile openAndLockFile(final Path path, final boolean fallbackToReadOnly) throws IOException {
        final boolean fileExists = Files.exists(path);
        final boolean fileWritable = Files.isWritable(path);
        final boolean parentWritable = Files.isWritable(path.getParent());

        // determine the mode for opening the file
        boolean readOnly = true;
        if (fileExists) {
            if (fileWritable) {
                // Read and Write
                readOnly = false;
            } else {
                // Read Only
                if (fallbackToReadOnly) {
                    readOnly = true;
                } else {
                    throw new IOException("File is not writable by this process: " + path.normalize().toAbsolutePath());
                }
            }
        } else if (parentWritable) {
            // can write to the parent, and so we can create and then Read and Write a new file
            readOnly = false;
        } else {
            throw new IOException("Either the file does not exist or the location is not writable by this process: " + path.normalize().toAbsolutePath());
        }

        // try and open the file
        @Nullable Tuple2<RandomAccessFile, FileLock> randomAccessFileWithLock = tryOpenAndLockFile(path, readOnly);
        if (randomAccessFileWithLock == null) {
            // unable to open and lock

            // check if we have hit an error condition
            if (readOnly) {
                throw new IOException("Unable to open file in read-only mode: " + path.normalize().toAbsolutePath());
            } else if (!fallbackToReadOnly) {
                throw new IOException("An exclusive lock could not be acquired because another program holds an overlapping lock on the file: " + path.normalize().toAbsolutePath());
            } else if (!fileExists) {
                throw new IOException("Unable to fallback to opening non-existent file in read-only mode: " + path.normalize().toAbsolutePath());
            }

            // fallback and try to re-open in read-only mode with a shared lock
            randomAccessFileWithLock = tryOpenAndLockFile(path, true);
            if (randomAccessFileWithLock == null) {
                throw new IOException("Unable to fallback to read-only mode on the file: " + path.normalize().toAbsolutePath());
            }
        }

        return new BackingFile(path, !fileExists, randomAccessFileWithLock._1, randomAccessFileWithLock._2);
    }

    /**
     * Try and open a Random Access File with an appropriate filesystem lock.
     * Note that the caller is responsible for closing the file and releasing the lock
     * when they are finished with the file.
     *
     * @param path the path to the file to open/create.
     *
     * @param readOnly when true the file is opened read only with a shared lock,
     *                 otherwise when false the file is opened read-write with an exclusive lock.
     *
     * @return the file lock and the random access file, or null if the file lock could not be acquired.
     *
     * @throws IOException if an I/O error occurs whilst opening or trying to lock the file.
     */
    private static @Nullable Tuple2<RandomAccessFile, FileLock> tryOpenAndLockFile(final Path path, final boolean readOnly) throws IOException {
        // try and open the file
        final RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), readOnly ? "r" : "rw");

        // try and get a lock for the file
        final FileChannel fileChannel = randomAccessFile.getChannel();
        try {
            @Nullable FileLock fileLock = fileChannel.tryLock(0L, Long.MAX_VALUE, readOnly);
            if (fileLock == null) {
                // NOTE(AR) the lock could not be acquired
                fileChannel.close();
                randomAccessFile.close();

                return null;
            }
            return Tuple(randomAccessFile, fileLock);

        } catch (final IllegalArgumentException | IllegalStateException | IOException e) {
            // release the resources we opened before re-throwing exception
            fileChannel.close();
            randomAccessFile.close();
            throw e;
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
            page.header.updateType(PageType.UNUSED);
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
                page.remove(backingFile.randomAccessFile);
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
        unlinkPages(createPage(pageNum));
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
                fileHeader.write(backingFile.randomAccessFile);
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
                    fileHeader.write(backingFile.randomAccessFile);
                    return;
                }

                Page<PAGE_HEADER> firstFreePage = createPage(firstFreePageNum);
                firstFreePage.read(backingFile.randomAccessFile);
                firstFreePageNum = firstFreePage.header.getNextPage();

                while (firstFreePageNum != Page.NO_PAGE) {
                    if (firstFreePageNum == page.pageNum) {
                        firstFreePage.header.updateNextPage(page.header.getNextPage());
                        firstFreePage.header.setDirty(true);
                        firstFreePage.write(backingFile.randomAccessFile,null);
                        return;
                    }
                    firstFreePage = createPage(firstFreePageNum);
                    firstFreePage.read(backingFile.randomAccessFile);
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
    protected final void writeValue(final Page<PAGE_HEADER> page, final Value value) throws IOException {
        final byte[] data = value.getData();
        writeValue(page, data);
    }

    protected final void writeValue(final Page<PAGE_HEADER> page, final byte[] data) throws IOException {
        final PAGE_HEADER pageHeader = page.getPageHeader();
        pageHeader.updateDataLen(getPageContentSize());
        if (data.length != pageHeader.getDataLen()) {
            //TODO : where to get this 64 from ?
            if (pageHeader.getDataLen() != fileHeader.getPageSize() - 64) {
                LOG.warn("ouch: {} != {}", getPageContentSize(), data.length);
            }
            pageHeader.updateDataLen(data.length);
        }
        page.write(backingFile.randomAccessFile, data);
    }

    /**
     * Writes the multi-Paged Value starting at the specified page number.
     *
     * @param page The starting page number
     * @param value The Value to write
     * @throws IOException if an Exception occurs
     */
    protected final void writeValue(final long page, final Value value) throws IOException {
        writeValue(createPage(page), value);
    }

    /**
     * A page in a paged file.
     *
     * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
     * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
     */
    public class Page<PAGE_HEADER extends PageHeader> implements Comparable<Page<PAGE_HEADER>> {

        public static final long NO_PAGE = -1;

        /**  The Header for this Page */
        final PAGE_HEADER header;

        /**  The offset into the file that this page starts */
        private long offset;
        /**  This page number */
        long pageNum;

        // TODO(AR) is this still a good idea to reuse these across multiple instances?
        private final byte[] tempPageData;
        private final byte[] tempHeaderData;

        public Page(final PAGE_HEADER header, final byte[] tempPageData, final byte[] tempHeaderData) {
            this.header = header;
            this.tempPageData = tempPageData;
            this.tempHeaderData = tempHeaderData;
        }

        /**
         * Constructor for the Page object
         *
         * @param pageNum Description of the Parameter
         */
        public Page(final PAGE_HEADER pageHeader, final long pageNum, final byte[] tempPageData, final byte[] tempHeaderData) {
            this(pageHeader, tempPageData, tempHeaderData);
            setPageNum(pageNum);
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
            return "page: " + pageNum +
                "; file = " + FileUtils.fileName(getFile()) +
                "; address = " + Long.toHexString(offset) +
                "; page header = " + fileHeader.getPageHeaderSize() +
                "; data start = " + Long.toHexString(offset + fileHeader.getPageHeaderSize());
        }

        public long getPageNum() {
            return pageNum;
        }

        // TODO(AR) this is called quite a lot where the return value `byte[]` is not needed, we could skip reading that extra data by calling raf.skipBytes
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
            this.pageNum = pageNum;
            this.offset = fileHeader.getHeaderSize() + (pageNum * fileHeader.getPageSize());
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
                if (data.length > getPageContentSize()) {
                    throw new IOException("page: " + getPageInfo() + ": data length too large: " + data.length);
                } else {
                    System.arraycopy(data, 0, tempPageData, fileHeader.getPageHeaderSize(), data.length);
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
        public int compareTo(final Page<PAGE_HEADER> other) {
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
            final byte[] data = new byte[fileHeader.getPageSize()];
            raf.read(data);
            LOG.debug("Contents of page {}: {}", pageNum, HexEncoder.bytesToHex(data));
        }
    }
}
