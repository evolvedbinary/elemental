/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;

/**
 * Cache implementation for CachingFilterInputStream Backed by a Random Access
 * File
 *
 * Probably slower than MemoryMappedFileFilterInputStreamCache for multiple
 * reads, but uses a fixed small amount of memory.
 *
 * @version 1.1
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="tobi.krebsATgmail.com">Tobi Krebs</a>
 */
public class FileFilterInputStreamCache extends AbstractFilterInputStreamCache {
    private final Path tempFile;
    private final boolean externalFile;
    private int length = 0;
    private int offset = 0;

    private final RandomAccessFile raf;

    public FileFilterInputStreamCache(final InputStream src) throws IOException {
        this(src, null);
    }

    public FileFilterInputStreamCache(final InputStream src, final Path f) throws IOException {
        super(src);
        if(f == null) {
            tempFile = TemporaryFileManager.getInstance().getTemporaryFile();
            externalFile = false;
        } else {
            tempFile = f;
            externalFile = true;
        }

        this.raf = new RandomAccessFile(tempFile.toFile(), "rw"); //TODO(AR) consider moving to Files.newByteChannel(tempFile
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        //force writing to be append only
        if (offset != length) {
            raf.seek(length);
            offset = length;
        }

        raf.write(b, off, len);
        length += len;
        offset += len;
    }

    @Override
    public void write(final int i) throws IOException {
        //force writing to be append only
        if (offset != length) {
            raf.seek(length);
            offset = length;
        }

        raf.write(i);
        length++;
        offset++;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public byte get(final int off) throws IOException {
        if (off != offset) {
            raf.seek(off);
            this.offset = off;
        }
        final byte b = raf.readByte();
        this.offset++;
        return b;
    }

    @Override
    public void copyTo(final int cacheOffset, final byte[] b, final int off, final int len) throws IOException {
        if (cacheOffset != offset) {
            raf.seek(cacheOffset);
            this.offset = cacheOffset;
        }
        raf.readFully(b, off, len);
        this.offset += len;
    }

    @Override
    public void invalidate() throws IOException {

        raf.close();

        if (tempFile != null && (!externalFile)) {
            TemporaryFileManager.getInstance().returnTemporaryFile(tempFile);
        }
    }

    /**
     * Get the path of the file backing the cache.
     *
     * @return the path of the file backing the cache.
     */
    public Path getFilePath() {
        return tempFile;
    }
}
