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
package org.exist.util;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.util.io.CachingFilterInputStream;
import org.exist.util.io.InputStreamUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class CachingFilterInputStreamInputSource extends EXistInputSource {
    private static final Logger LOG = LogManager.getLogger(CachingFilterInputStreamInputSource.class);

    private CachingFilterInputStream cachingFilterInputStream;
    private long length = -1;

    public CachingFilterInputStreamInputSource(final CachingFilterInputStream cachingFilterInputStream) {
        super();
        this.cachingFilterInputStream = cachingFilterInputStream;
    }

    @Override
    public Reader getCharacterStream() {
        assertOpen();

        return null;
    }

    /**
     * This method now does nothing, so collateral
     * effects from superclass with this one are avoided
     *
     * @throws IllegalStateException if the InputSource was previously closed
     */
    @Override
    public void setCharacterStream(final Reader r) {
        assertOpen();
        throw new IllegalStateException("CachingFilterInputStreamInputSource is immutable");
    }

    @Override
    public InputStream getByteStream() {
        assertOpen();

        try {
            return new CachingFilterInputStream(cachingFilterInputStream);
        } catch (final InstantiationException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @see EXistInputSource#getByteStreamLength()
     *
     * @throws IllegalStateException if the InputSource was previously closed
     */
    @Override
    public long getByteStreamLength() {
        assertOpen();
        if (length == -1) {
            try (final CachingFilterInputStream is = new CachingFilterInputStream(cachingFilterInputStream);
                 final CountingOutputStream cos = new CountingOutputStream(NullOutputStream.NULL_OUTPUT_STREAM)) {
                InputStreamUtil.copy(is, cos);
                length = cos.getByteCount();
            } catch (final InstantiationException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return -1;
    }

    /**
     * Set a byte stream input.
     *
     * @param is the input stream.
     *
     * @throws IllegalStateException this class is immutable!
     */
    @Override
    public void setByteStream(final InputStream is) {
        assertOpen();
        throw new IllegalStateException("CachingFilterInputStreamInputSource is immutable");
    }

    /**
     * This method now does nothing, so collateral
     * effects from superclass with this one are avoided
     *
     * @throws IllegalStateException if the InputSource was previously closed
     */
    @Override
    public void setSystemId(final String systemId) {
        assertOpen();
        // Nothing, so collateral effects are avoided!
    }

    /**
     * @see EXistInputSource#getSymbolicPath()
     *
     * @throws IllegalStateException if the InputSource was previously closed
     */
    @Override
    public String getSymbolicPath() {
        assertOpen();
        return null;
    }
}
