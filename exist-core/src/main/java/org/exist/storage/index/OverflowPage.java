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

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.exist.storage.btree.Page;
import org.exist.storage.btree.PageStatus;
import org.exist.storage.io.VariableByteInput;
import org.exist.storage.journal.Loggable;
import org.exist.storage.txn.Txn;
import org.exist.util.ByteArray;
import org.exist.util.ByteConversion;
import org.exist.util.FileUtils;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents an overflow page (as opposed to a single data page, see: {@link SinglePage}).
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
class OverflowPage extends AbstractDataPage {
    private final SinglePage firstPage;
    private byte[] data = null;

    public OverflowPage(final Txn transaction) throws IOException {
        firstPage = new SinglePage(false);
        if (transaction != null && isRecoveryEnabled()) {
            final Loggable loggable = new OverflowCreateLoggable(fileId, transaction, firstPage.getPageNum());
            writeToLog(loggable, firstPage);
        }
        final BFilePageHeader ph = firstPage.getPageHeader();
        ph.updateStatus(PageStatus.MULTI_PAGE);
        ph.setNextInChain(0L);
        ph.setLastInChain(0L);
        ph.setDataLength(0);
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            firstPage.setData(new byte[fileHeader.getWorkSize()]);
        } finally {
            fileHeaderReadLock.unlock();
        }
        dataCache.add(firstPage, 3);
    }

    public OverflowPage(final AbstractDataPage page) {
        firstPage = (SinglePage) page;
    }

    public OverflowPage(final Page<BFilePageHeader> p, final byte[] data) throws IOException {
        firstPage = new SinglePage(p, data, false);
        firstPage.getPageHeader().updateStatus(PageStatus.MULTI_PAGE);
    }

    /**
     * Append a new chunk of data to the page
     *
     * @param transaction the database transaction
     * @param chunk chunk of data to append
     */
    public void append(final Txn transaction, final ByteArray chunk) throws IOException {
        SinglePage nextPage;
        BFilePageHeader ph = firstPage.getPageHeader();
        final int newLen = ph.getDataLength() + chunk.size();
        // get the last page and fill it
        final long next = ph.getLastInChain();
        AbstractDataPage page;
        if (next > 0) {
            page = getDataPage(next, false);
        } else {
            page = firstPage;
        }
        ph = page.getPageHeader();
        final int chunkLen = chunk.size();
        int chunkSize;
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            chunkSize = fileHeader.getWorkSize() - ph.getDataLength();
            if (chunkLen < chunkSize) {
                chunkSize = chunkLen;
            }
            // fill last page
            if (transaction != null && isRecoveryEnabled()) {
                final Loggable loggable =
                    new OverflowAppendLoggable(fileId, transaction, page.getPageNum(), chunk, 0, chunkSize);
                writeToLog(loggable, page);
            }
            chunk.copyTo(0, page.getData(), ph.getDataLength(), chunkSize);
            if (page != firstPage) {
                ph.setDataLength(ph.getDataLength() + chunkSize);
            }
            page.setDirty(true);
            // write the remaining chunks to new pages
            int remaining = chunkLen - chunkSize;
            int current = chunkSize;
            chunkSize = fileHeader.getWorkSize();
            if (remaining > 0) {
                // walk through chain of pages
                while (remaining > 0) {
                    if (remaining < chunkSize) {
                        chunkSize = remaining;
                    }

                    // add a new page to the chain
                    nextPage = createDataPage();
                    if (transaction != null && isRecoveryEnabled()) {
                        Loggable loggable = new OverflowCreatePageLoggable(transaction, fileId, nextPage.getPageNum(),
                            page.getPageNum());
                        writeToLog(loggable, nextPage);

                        loggable = new OverflowAppendLoggable(fileId, transaction, nextPage.getPageNum(),
                            chunk, current, chunkSize);
                        writeToLog(loggable, page);
                    }
                    nextPage.setData(new byte[fileHeader.getWorkSize()]);
                    page.getPageHeader().setNextInChain(nextPage.getPageNum());
                    page.setDirty(true);
                    dataCache.add(page);
                    page = nextPage;
                    // copy next chunk of data to the page
                    chunk.copyTo(current, page.getData(), 0, chunkSize);
                    page.setDirty(true);
                    if (page != firstPage) {
                        page.getPageHeader().setDataLength(chunkSize);
                    }
                    remaining = remaining - chunkSize;
                    current += chunkSize;
                }
            }
        } finally {
            fileHeaderReadLock.unlock();
        }
        ph = firstPage.getPageHeader();
        if (transaction != null && isRecoveryEnabled()) {
            final Loggable loggable = new OverflowModifiedLoggable(fileId, transaction, firstPage.getPageNum(),
                ph.getDataLength() + chunkLen, ph.getDataLength(), page == firstPage ? 0 : page.getPageNum());
            writeToLog(loggable, page);
        }
        if (page != firstPage) {
            // add link to last page
            dataCache.add(page);
            ph.setLastInChain(page.getPageNum());

        } else {
            ph.setLastInChain(0L);
        }
        // adjust length field in first page
        ph.setDataLength(newLen);
        ByteConversion.intToByte(firstPage.getPageHeader().getDataLength() - 6, firstPage.getData(), 2);
        firstPage.setDirty(true);
        // keep the first page in cache
        dataCache.add(firstPage, 2);
    }

    @Override
    public void delete() throws IOException {
        delete(null);
    }

    public void delete(final Txn transaction) throws IOException {
        long next = firstPage.getPageNum();
        SinglePage page = firstPage;
        do {
            next = page.ph.getNextInChain();
            if (transaction != null && isRecoveryEnabled()) {
                int dataLen = page.ph.getDataLength();
                final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
                try {
                    if (dataLen > fileHeader.getWorkSize()) {
                        dataLen = fileHeader.getWorkSize();
                    }
                } finally {
                    fileHeaderReadLock.unlock();
                }
                final Loggable loggable = new OverflowRemoveLoggable(fileId, transaction,
                    page.ph.getStatus(), page.getPageNum(),
                    page.getData(), dataLen,
                    page.ph.getNextInChain());
                writeToLog(loggable, page);
            }

            page.getPageHeader().setNextInChain(-1L);
            page.setDirty(true);
            dataCache.remove(page);
            page.delete();
            if (next > 0) {
                page = getSinglePage(next);
            }
        } while (next > 0);
    }

    public VariableByteInput getDataStream(final long pointer) {
        final BFile.MultiPageInput input = new BFile.MultiPageInput(firstPage, pointer);
        return input;
    }

    @Override
    public byte[] getData() throws IOException {
        if (data != null) {
            return data;
        }

        SinglePage page = firstPage;
        long next;
        byte[] temp;
        int len;

        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try(final UnsynchronizedByteArrayOutputStream os = new UnsynchronizedByteArrayOutputStream(page.getPageHeader().getDataLength())) {
            do {
                temp = page.getData();
                next = page.getPageHeader().getNextInChain();
                len = next > 0 ? fileHeader.getWorkSize() : page
                    .getPageHeader().getDataLength();
                os.write(temp, 0, len);

                if (next > 0) {
                    page = (SinglePage) getDataPage(next, false);
                    dataCache.add(page);
                }
            } while (next > 0);
            data = os.toByteArray();
            if (data.length != firstPage.getPageHeader().getDataLength()) {
                LOG.warn("{} read={}; expected={}", FileUtils.fileName(getFile()), data.length, firstPage.getPageHeader().getDataLength());
            }
            return data;
        } finally {
            fileHeaderReadLock.unlock();
        }
    }

    @Override
    public SinglePage getFirstPage() {
        return firstPage;
    }

    @Override
    public BFilePageHeader getPageHeader() {
        return firstPage.getPageHeader();
    }

    @Override
    public String getPageInfo() {
        return "MULTI_PAGE: " + firstPage.getPageInfo();
    }

    @Override
    public long getPageNum() {
        return firstPage.getPageNum();
    }

    @Override
    public void setData(final byte[] buf) {
        setData(null, buf);
    }

    public void setData(final Txn transaction, final byte[] data) {
        this.data = data;
        try {
            write(transaction);
        } catch (final IOException e) {
            LOG.error(e);
        }
    }

    @Override
    public void write() throws IOException {
        write(null);
    }

    public void write(final Txn transaction) throws IOException {
        if (data == null) {
            return;
        }

        final ReentrantReadWriteLock.ReadLock fileHeaderReadlock = fileHeader.readLock();
        try {
            int chunkSize = fileHeader.getWorkSize();
            int remaining = data.length;
            int current = 0;
            long next = 0L;
            SinglePage page = firstPage;
            page.getPageHeader().setDataLength(remaining);
            SinglePage nextPage;
            long prevPageNum = Page.NO_PAGE;
            // walk through chain of pages
            while (remaining > 0) {
                if (remaining < chunkSize) {
                    chunkSize = remaining;
                }
                page.clear();
                // copy next chunk of data to the page
                if (transaction != null && isRecoveryEnabled()) {
                    final Loggable loggable = new OverflowStoreLoggable(fileId, transaction, page.getPageNum(), prevPageNum,
                        data, current, chunkSize);
                    writeToLog(loggable, page);
                }
                System.arraycopy(data, current, page.getData(), 0, chunkSize);
                if (page != firstPage) {
                    page.getPageHeader().setDataLength(chunkSize);
                }
                page.setDirty(true);
                remaining -= chunkSize;
                current += chunkSize;
                next = page.getPageHeader().getNextInChain();
                if (remaining > 0) {
                    if (next > 0) {
                        // load next page in chain
                        nextPage = (SinglePage) getDataPage(next, false);
                        dataCache.add(page);
                        prevPageNum = page.getPageNum();
                        page = nextPage;
                    } else {
                        // add a new page to the chain
                        nextPage = createDataPage();
                        if (transaction != null && isRecoveryEnabled()) {
                            final Loggable loggable = new CreatePageLoggable(transaction, fileId, nextPage.getPageNum());
                            writeToLog(loggable, nextPage);
                        }
                        nextPage.setData(new byte[fileHeader.getWorkSize()]);
                        nextPage.getPageHeader().setNextInChain(0L);
                        page.getPageHeader().setNextInChain(
                            nextPage.getPageNum());
                        dataCache.add(page);
                        prevPageNum = page.getPageNum();
                        page = nextPage;
                    }
                } else {
                    page.getPageHeader().setNextInChain(0L);
                    if (page != firstPage) {
                        page.setDirty(true);
                        dataCache.add(page);
                        firstPage.getPageHeader().setLastInChain(
                            page.getPageNum());
                    } else {
                        firstPage.getPageHeader().setLastInChain(0L);
                    }
                    firstPage.setDirty(true);
                    dataCache.add(firstPage, 3);
                }
            }


            if (next > 0) {
                // there are more pages in the chain:
                // remove them
                while (next > 0) {
                    nextPage = (SinglePage) getDataPage(next, false);

                    next = nextPage.getPageHeader().getNextInChain();

                    if (transaction != null && isRecoveryEnabled()) {
                        final Loggable loggable = new OverflowRemoveLoggable(fileId, transaction,
                            nextPage.getPageHeader().getStatus(), nextPage.getPageNum(),
                            nextPage.getData(), nextPage.getPageHeader().getDataLength(),
                            nextPage.getPageHeader().getNextInChain());
                        writeToLog(loggable, nextPage);
                    }

                    nextPage.setDirty(true);
                    nextPage.delete();
                    dataCache.remove(nextPage);
                }
            }
            firstPage.getPageHeader().setDataLength(data.length);
            firstPage.setDirty(true);
            dataCache.add(firstPage, 3);
            //            LOG.debug(firstPage.getPageNum() + " data length: " + firstPage.ph.getDataLength());
        } finally {
            fileHeaderReadlock.unlock();
        }
    }

    @Override
    public int findValuePosition(final short tid) throws IOException {
        return 2;
    }

    @Override
    public short getNextTID() {
        return 1;
    }

    @Override
    public void removeTID(final short tid, final int length) {
        //
    }

    @Override
    public void setOffset(final short tid, final int offset) {
        //
    }
}

