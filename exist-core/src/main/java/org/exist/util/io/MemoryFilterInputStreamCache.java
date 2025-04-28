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

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Cache implementation for CachingFilterInputStream Backed by an in-memory byte
 * array
 *
 * @version 1.1
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="tobi.krebsATgmail.com">Tobi Krebs</a>
 */
public class MemoryFilterInputStreamCache extends AbstractFilterInputStreamCache {

    private UnsynchronizedByteArrayOutputStream cache = new UnsynchronizedByteArrayOutputStream();

    public MemoryFilterInputStreamCache(InputStream src) {
        super(src);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        cache.write(b, off, len);
    }

    @Override
    public void write(final int i) throws IOException {
        cache.write(i);
    }

    @Override
    public byte get(final int off) {
        return cache.toByteArray()[off];
    }

    @Override
    public int getLength() {
        return cache.size();
    }

    @Override
    public void copyTo(final int cacheOffset, final byte[] b, final int off, final int len) {
        System.arraycopy(cache.toByteArray(), cacheOffset, b, off, len);
    }

    @Override
    public void invalidate() throws IOException {
        if (cache != null) {
            cache.close();
            cache = null;
        }
    }

    /**
     * Updates to the cache are not reflected in the underlying input stream
     */
    //TODO refactor this so that updates to the cache are reflected
    /*@Override
     public InputStream getIndependentInputStream() {
     return new ByteArrayInputStream(cache.toByteArray());
     }*/
}
