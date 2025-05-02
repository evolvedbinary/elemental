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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Cache implementation for CachingFilterInputStream Backed by a Memory Mapped
 * File
 *
 * @version 1.1
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="tobi.krebsATgmail.com">Tobi Krebs</a>
 */
public class MemoryMappedFileFilterInputStreamCache extends AbstractFilterInputStreamCache {

    private final static long DEFAULT_MEMORY_MAP_SIZE = 64 * 1024 * 1024; //64MB

    private final RandomAccessFile raf;
    private final FileChannel channel;
    private MappedByteBuffer buf;
    private Path tempFile = null;
    private final long memoryMapSize = DEFAULT_MEMORY_MAP_SIZE;

    private boolean externalFile = true;

    public MemoryMappedFileFilterInputStreamCache(final InputStream src) throws IOException {
        this(src, null);
    }

    public MemoryMappedFileFilterInputStreamCache(final InputStream src, final Path f) throws IOException {
        super(src);

        if(f == null) {
            tempFile = TemporaryFileManager.getInstance().getTemporaryFile();
            externalFile = false;
        } else {
            tempFile = f;
            externalFile = true;
        }

        /**
         * Check the applicability of these bugs to this code:
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6417205 (fixed in
         * 1.6)
         */

        this.raf = new RandomAccessFile(tempFile.toFile(), "rw");   //TODO(AR) consider moving to Files.newByteChannel(tempFile)
        this.channel = raf.getChannel();
        this.buf = channel.map(FileChannel.MapMode.READ_WRITE, 0, getMemoryMapSize());
    }

    private long getMemoryMapSize() {
        return memoryMapSize;
    }

    private void increaseSize(final long bytes) throws IOException {

        long factor = (bytes / getMemoryMapSize());
        if (factor == 0 || bytes % getMemoryMapSize() > 0) {
            factor++;
        }

        buf.force();

        //TODO revisit this based on the comment below, I now believe setting position in map does work, but you have to have the correct offset added in as well! Adam
        final int position = buf.position();
        buf = channel.map(FileChannel.MapMode.READ_WRITE, 0, buf.capacity() + (getMemoryMapSize() * factor));
        buf.position(position); //setting the position in the map() call above does not seem to work!
        //bufAccessor.refresh();
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {

        if (buf.remaining() < len) {
            //we need to remap the file
            increaseSize(len - buf.remaining());
        }

        buf.put(b, off, len);
    }

    @Override
    public void write(final int i) throws IOException {

        if (buf.remaining() < 1) {
            //we need to remap the file
            increaseSize(1);
        }

        buf.put((byte) i);
    }

    @Override
    public byte get(final int off) throws IOException {

        if (off > buf.capacity()) {
            //we need to remap the file
            increaseSize(off - buf.capacity());
        }

        return buf.get(off);
    }

    @Override
    public int getLength() {
        return buf.capacity() - buf.remaining();
    }

    @Override
    public void copyTo(final int cacheOffset, final byte[] b, final int off, final int len) throws IOException {

        if (off + len > buf.capacity()) {
            //we need to remap the file
            increaseSize(off + len - buf.capacity());
        }

        //get the current position
        final int position = buf.position();

        try {
            //move to the offset
            buf.position(cacheOffset);

            //read the data;
            final byte[] data = new byte[len];
            buf.get(data, 0, len);

            System.arraycopy(data, 0, b, off, len);
        } finally {
            //reset the position
            buf.position(position);
        }
    }

    @Override
    public void invalidate() throws IOException {
        buf.force();
        channel.close();
        raf.close();
        //System.gc();

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
