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

import org.exist.storage.StorageAddress;
import org.exist.storage.io.VariableByteInput;
import org.exist.storage.io.VariableByteOutput;
import org.exist.storage.lock.ManagedLock;
import org.exist.util.FileUtils;
import org.exist.util.LockException;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Variable byte input stream to read a multi-page sequences.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
class MultiPageInput implements VariableByteInput, PageInput {
    private SinglePage nextPage;
    private int pageLen;
    private short offset = 0;
    private final long address;

    public MultiPageInput(final SinglePage first, final long address) {
        this.nextPage = first;
        this.offset = 6;
        this.pageLen = first.ph.getDataLength();
        final ReentrantReadWriteLock.ReadLock fileHeaderReadlock = fileHeader.readLock();
        try {
            if (this.pageLen > fileHeader.getWorkSize()) {
                this.pageLen = fileHeader.getWorkSize();
            }
        } finally {
            fileHeaderReadlock.unlock();
        }
        this.dataCache.add(first, 3);
        this.address = address;
    }

    @Override
    public long getAddress() {
        return address;
    }

    @Override
    public int read() throws IOException {
        if (offset == pageLen) {
            advance();
        }
        return nextPage.data[offset++] & 0xFF;
    }

    @Override
    public byte readByte() throws IOException {
        if (offset == pageLen) {
            advance();
        }
        return nextPage.data[offset++];
    }

    @Override
    public short readShort() throws IOException {
        if (offset == pageLen) {
            advance();
        }
        byte b = nextPage.data[offset++];
        short i = (short) (b & 0177);
        for (int shift = 7; (b & 0200) != 0; shift += 7) {
            if (offset == pageLen) {
                advance();
            }
            b = nextPage.data[offset++];
            i |= (b & 0177) << shift;
        }
        return i;
    }

    @Override
    public short readFixedShort() throws IOException {
        if (offset == pageLen) {
            advance();
        }
        // do we have to read across a page boundary?
        if (offset + 2 < pageLen) {
            return (short) ((nextPage.data[offset++] & 0xff) |
                ((nextPage.data[offset++] & 0xff) << 8));
        }

        short r = (short) (nextPage.data[offset++] & 0xff);
        if (offset == pageLen) {
            advance();
        }
        r |= (nextPage.data[offset++] & 0xff) << 8;

        return r;
    }

    @Override
    public int readInt() throws IOException {
        if (offset == pageLen) {
            advance();
        }
        byte b = nextPage.data[offset++];
        int i = b & 0177;
        for (int shift = 7; (b & 0200) != 0; shift += 7) {
            if (offset == pageLen) {
                advance();
            }
            b = nextPage.data[offset++];
            i |= (b & 0177) << shift;
        }
        return i;
    }

    @Override
    public int readFixedInt() throws IOException {
        if (offset == pageLen) {
            advance();
        }
        // do we have to read across a page boundary?
        if (offset + 4 < pageLen) {
            return ( nextPage.data[offset++] & 0xff ) |
                ( (nextPage.data[offset++] & 0xff) << 8 ) |
                ( (nextPage.data[offset++] & 0xff) << 16 ) |
                ( (nextPage.data[offset++] & 0xff) << 24 );
        }
        int r = nextPage.data[offset++] & 0xff;
        int shift = 8;
        for (int i = 0; i < 3; i++) {
            if (offset == pageLen) {
                advance();
            }
            r |= (nextPage.data[offset++] & 0xff) << shift;
            shift += 8;
        }
        return r;
    }

    @Override
    public long readLong() throws IOException {
        if (offset == pageLen) {
            advance();
        }
        byte b = nextPage.data[offset++];
        long i = b & 0177;
        for (int shift = 7; (b & 0200) != 0; shift += 7) {
            if (offset == pageLen) {
                advance();
            }
            b = nextPage.data[offset++];
            i |= (b & 0177L) << shift;
        }
        return i;
    }

    @Override
    public long readFixedLong() throws IOException {
        if (offset == pageLen) {
            advance();
        }
        // do we have to read across a page boundary?
        if (offset + 8 < pageLen) {
            return ((nextPage.data[offset++] & 0xff) << 56) |
                ((nextPage.data[offset++] & 0xff) << 48) |
                ((nextPage.data[offset++] & 0xff) << 40) |
                ((nextPage.data[offset++] & 0xff) << 32) |
                ((nextPage.data[offset++] & 0xff) << 24) |
                ((nextPage.data[offset++] & 0xff) << 16) |
                ((nextPage.data[offset++] & 0xff) << 8) |
                (nextPage.data[offset++] & 0xff);
        }

        int r = (nextPage.data[offset++] & 0xff) << 56;
        int shift = 48;
        for (int i = 0; i < 7; i++) {
            if (offset == pageLen) {
                advance();
            }
            r |= (nextPage.data[offset++] & 0xff) << shift;
            shift -= 8;
        }
        return r;
    }

    @Override
    public BigInteger readBigInteger() throws IOException {
        final int dataLength = readInt();
        final byte[] data = new byte[dataLength];
        read(data);

        return new BigInteger(data);
    }

    @Override
    public BigInteger readFixedBigInteger() throws IOException {
        final int dataLength = readFixedInt();
        final byte[] data = new byte[dataLength];
        read(data);

        return new BigInteger(data);
    }

    @Override
    public BigDecimal readBigDecimal() throws IOException {
        final int scale = readInt();
        final int precision = readInt();
        final int dataLength = readInt();
        final byte[] data = new byte[dataLength];
        read(data);

        final MathContext mathContext = new java.math.MathContext(precision);
        return new BigDecimal(new BigInteger(data), scale, mathContext);
    }

    @Override
    public BigDecimal readFixedBigDecimal() throws IOException {
        final int scale = readFixedInt();
        final int precision = readFixedInt();
        final int dataLength = readFixedInt();
        final byte[] data = new byte[dataLength];
        read(data);

        final MathContext mathContext = new java.math.MathContext(precision);
        return new BigDecimal(new BigInteger(data), scale, mathContext);
    }

    @Override
    public void skip(final int count) throws IOException {
        for (int i = 0; i < count; i++) {
            do {
                if (offset == pageLen) {
                    advance();
                }
            } while ((nextPage.data[offset++] & 0200) > 0);
        }
    }

    @Override
    public void skipBytes(final long count) throws IOException {
        for(long i = 0; i < count; i++) {
            if (offset == pageLen) {
                advance();
            }
            offset++;
        }
    }

    private void advance() throws IOException {
        final long next = nextPage.getPageHeader().getNextInChain();
        if (next < 1) {
            pageLen = -1;
            offset = 0;
            throw new EOFException();
        }


        try (final ManagedLock<ReentrantLock> bfileLock = lockManager.acquireBtreeReadLock(getLockName())) {
            nextPage = (SinglePage) getDataPage(next, false);
            pageLen = nextPage.ph.getDataLength();
            offset = 0;
            dataCache.add(nextPage);
        } catch (final LockException e) {
            throw new IOException("failed to acquire a read lock on "
                + FileUtils.fileName(getFile()));
        }
    }

    @Override
    public int available() throws IOException {
        if (pageLen < 0) {
            return 0;
        }
        int inPage = pageLen - offset;
        if (inPage == 0) {
            inPage = nextPage.getPageHeader().getNextInChain() > 0 ? 1 : 0;
        }
        return inPage;
    }

    @Override
    public int read(final byte[] data) throws IOException {
        return read(data, 0, data.length);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (pageLen < 0) {
            return -1;
        }

        for (int i = 0; i < len; i++) {
            if (offset == pageLen) {
                final long next = nextPage.getPageHeader().getNextInChain();
                if (next < 1) {
                    pageLen = -1;
                    offset = 0;
                    return i;
                }
                nextPage = (SinglePage) getDataPage(next, false);
                pageLen = nextPage.ph.getDataLength();
                offset = 0;
                dataCache.add(nextPage);
            }
            b[off + i] = nextPage.data[offset++];
        }
        return len;
    }

    @Override
    public String readUTF() throws IOException {
        final int len = readInt();
        final byte data[] = new byte[len];

        read(data);

        return new String(data, UTF_8);
    }

    @Override
    public void copyTo(final VariableByteOutput os) throws IOException {
        byte more;
        do {
            if (offset == pageLen) {
                advance();
            }
            more = nextPage.data[offset++];
            os.writeByte(more);
            more &= 0200;
        } while (more > 0);
    }

    @Override
    public void copyTo(final VariableByteOutput os, final int count) throws IOException {
        byte more;
        for (int i = 0; i < count; i++) {
            do {
                if (offset == pageLen) {
                    advance();
                }
                more = nextPage.data[offset++];
                os.writeByte(more);
            } while ((more & 0x200) > 0);
        }
    }

    @Override
    public void copyRaw(final VariableByteOutput os, final int count) throws IOException {
        for (int i = count; i != 0; ) {
            if (offset == pageLen) {
                advance();
            }
            int avail = pageLen - offset;
            if (i >= avail) {
                os.write(nextPage.data, offset, avail);
                i -= avail;
                offset = (short) pageLen;
            } else {
                os.write(nextPage.data, offset, i);
                offset += i;
                break;
            }
            //os.writeByte(nextPage.data[offset++]);
        }
    }

    @Override
    public long position() {
        return StorageAddress.createPointer((int) nextPage.getPageNum(), offset);
    }

    @Override
    public void seek(final long position) throws IOException {
        final int newPage = StorageAddress.pageFromPointer(position);
        final short newOffset = StorageAddress.tidFromPointer(position);
        final ReentrantReadWriteLock.ReadLock fileHeaderReadlock = fileHeader.readLock();
        try(final ManagedLock<ReentrantLock> bfileLock =  lockManager.acquireBtreeReadLock(getLockName())) {
            nextPage = getSinglePage(newPage);
            pageLen = nextPage.ph.getDataLength();
            if (pageLen > fileHeader.getWorkSize()) {
                pageLen = fileHeader.getWorkSize();
            }
            offset = newOffset;
            dataCache.add(nextPage);
        } catch (final LockException e) {
            throw new IOException("Failed to acquire a read lock on " + FileUtils.fileName(getFile()));
        } finally {
            fileHeaderReadlock.unlock();
        }
    }
}
