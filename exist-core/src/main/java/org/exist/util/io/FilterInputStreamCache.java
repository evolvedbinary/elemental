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

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface for Cache Implementations for use by the CachingFilterInputStream
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="tobi.krebsATgmail.com">Tobi Krebs</a>
 * @version 1.1
 */
public interface FilterInputStreamCache extends Closeable {

    int END_OF_STREAM = -1;
    String INPUTSTREAM_CLOSED = "The underlying InputStream has been closed";

    //TODO ensure that FilterInputStreamCache implementations are enforced thread-safe
    /**
     * Writes len bytes from the specified byte array starting at offset off to
     * the cache. The general contract for write(b, off, len) is that some of
     * the bytes in the array b are written to the output stream in order;
     * element b[off] is the first byte written and b[off+len-1] is the last
     * byte written by this operation.
     *
     * If b is null, a NullPointerException is thrown.
     *
     * If off is negative, or len is negative, or off+len is greater than the
     * length of the array b, then an IndexOutOfBoundsException is thrown.
     *
     * @param b the data.
     * @param off the start offset in the data.
     * @param len - the number of bytes to write.
     *
     * @throws IOException - if an I/O error occurs. In particular, an
     * IOException is thrown if the cache is invalidated.
     */
    void write(byte[] b, int off, int len) throws IOException;

    /**
     * Writes the specified byte to the cache. The general contract for write is
     * that one byte is written to the cache.
     *
     * @param i - the byte.
     *
     * @throws IOException if an I/O error occurs. In particular, an IOException
     * may be thrown if cache is invalidated.
     */
    void write(int i) throws IOException;

    /**
     * Gets the length of the cache
     *
     * @return The length of the cache
     */
    int getLength();

    /**
     * Retrieves the byte at offset off from the cache
     *
     * @param off The offset to read from
     * @return The byte read from the offset
     *
     * @throws IOException if an I/O error occurs. In particular, an IOException
     * may be thrown if cache is invalidated.
     */
    byte get(int off) throws IOException;

    /**
     * Copies data from the cache to a buffer
     *
     * @param cacheOffset The offset in the cache to start copying data from
     * @param b The buffer to write to
     * @param off The offset in the buffer b at which to start writing
     * @param len The length of data to copy
     *
     * @throws IOException if an I/O error occurs. In particular, an IOException
     * may be thrown if cache is invalidated.
     */
    void copyTo(int cacheOffset, byte[] b, int off, int len) throws IOException;

    /**
     * Invalidates the cache
     *
     * Destroys the cache and releases any underlying resources
     *
     * @throws IOException if an I/O error occurs. In particular, an IOException
     * may be thrown if cache is already invalidated.
     */
    void invalidate() throws IOException;

    int available() throws IOException;

    void mark(int readlimit);

    boolean markSupported();

    int read() throws IOException;

    int read(byte[] b) throws IOException;

    int read(byte[] b, int off, int len) throws IOException;

    void reset() throws IOException;

    long skip(long n) throws IOException;

    int getSrcOffset();

    boolean isSrcClosed();
    
    boolean srcIsFilterInputStreamCache();

    /**
     * Increments the number of shared references to the cache.
     */
    void incrementSharedReferences();

    /**
     * Decrements the number of shared references to the cache.
     */
    void decrementSharedReferences();
}
