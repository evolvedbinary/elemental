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

/**
 * Implementation of an InputStream which reads from a ByteBuffer
 *
 * @version 1.0
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ByteBufferInputStream extends InputStream {

    private final ByteBufferAccessor bufAccessor;
    private boolean closed = false;
    private final static int END_OF_STREAM = -1;

    public ByteBufferInputStream(final ByteBufferAccessor bufAccessor) {
        this.bufAccessor = bufAccessor;
    }

    @Override
    public int available() throws IOException {
        int available = 0;

        if(!closed) {
            available = bufAccessor.getBuffer().capacity() - bufAccessor.getBuffer().position();
        }

        return available;
    }
    
    @Override
    public int read() throws IOException {
        isClosed();
        
        if(available() == 0) {
            return END_OF_STREAM;
        }

        return bufAccessor.getBuffer().get();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        isClosed();

        if(available() == 0) {
            return END_OF_STREAM;
        } else if(b.length > available()) {
            return read(b, 0, available());
        } else {
            final int currentPosition = bufAccessor.getBuffer().position();
            return bufAccessor.getBuffer().get(b).position() - currentPosition;
        }
    }

    @Override
    public int read(final byte[] b, final int off, int len) throws IOException {
        isClosed();

        if(available() == 0) {
            return END_OF_STREAM;
        }
        
        if(len > available()) {
            len = available();
        }

        final int currentPosition = bufAccessor.getBuffer().position();
        return bufAccessor.getBuffer().get(b, off, len).position() - currentPosition;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized void mark(final int i) {
        bufAccessor.getBuffer().mark();
    }

    @Override
    public synchronized void reset() throws IOException {
        bufAccessor.getBuffer().reset();
    }

    @Override
    public long skip(long l) throws IOException {

        if(l > available()) {
            l = available();
        }

        long newPosition = bufAccessor.getBuffer().position();
        newPosition += l;
        try {
            bufAccessor.getBuffer().position((int)newPosition);
        } catch(final IllegalArgumentException iae) {
            throw new IOException("Unable to skip " + l + " bytes", iae);
        }

        return l;
    }

    @Override
    public void close() throws IOException {

        isClosed();

        bufAccessor.getBuffer().clear();
        closed = true;
    }

    private void isClosed() throws IOException {
        if(closed) {
            throw new IOException("The stream was previously closed");
        }
    }

}