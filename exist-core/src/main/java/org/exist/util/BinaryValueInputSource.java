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
import org.exist.xquery.value.BinaryValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Optional;

/**
 * Input Source for a Binary Value.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class BinaryValueInputSource extends EXistInputSource {
    private final static Logger LOG = LogManager.getLogger(BinaryValueInputSource.class);

    private Optional<BinaryValue> binaryValue = Optional.empty();
    private Optional<InputStream> inputStream = Optional.empty();
    private long length = -1;

    /**
     * Constructor which calls {@link #setBinaryValue(BinaryValue)}
     * @param binaryValue
     * The binaryValue passed to {@link #setBinaryValue(BinaryValue)}
     */
    public BinaryValueInputSource(final BinaryValue binaryValue) {
        super();
        setBinaryValue(binaryValue);
    }

    /**
     * If a binary file source has been set, the BinaryValue
     * object used for that is returned
     *
     * @return The BinaryValue object.
     */
    public BinaryValue getBinaryValue() {
        return binaryValue.orElse(null);
    }

    /**
     * This method sets the BinaryValue object
     *
     * @param binaryValue The BinaryValue.
     *
     * @throws IllegalStateException if the InputSource was previously closed
     */
    public void setBinaryValue(final BinaryValue binaryValue) {
        assertOpen();

        close();
        this.binaryValue = Optional.of(binaryValue);
        reOpen();
    }

    /**
     * This method was re-implemented to open a
     * new InputStream each time it is called.
     *
     * @return If the binaryvalue was set, and it could be opened, an InputStream object.
     * null, otherwise.
     *
     * @throws IllegalStateException if the InputSource was previously closed
     */
    @Override
    public InputStream getByteStream() {
        assertOpen();

        // close any open stream first
        close();

        if (binaryValue.isPresent()) {
            this.inputStream = Optional.of(binaryValue.get().getInputStream());
            reOpen();
            return inputStream.get();
        }

        return null;
    }

    /**
     * This method now does nothing, so collateral
     * effects from superclass with this one are avoided
     *
     * @throws IllegalStateException if the InputSource was previously closed
     */
    @Override
    public void setByteStream(final InputStream is) {
        assertOpen();
        // Nothing, so collateral effects are avoided!
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
        // Nothing, so collateral effects are avoided!
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
     * @see EXistInputSource#getByteStreamLength()
     *
     * @throws IllegalStateException if the InputSource was previously closed
     */
    @Override
    public long getByteStreamLength() {
        assertOpen();
        if (length == -1 && binaryValue.isPresent()) {
            try (final CountingOutputStream cos = new CountingOutputStream(NullOutputStream.NULL_OUTPUT_STREAM)) {
                binaryValue.get().streamBinaryTo(cos);
                length = cos.getByteCount();
            } catch(final IOException e) {
                LOG.error(e);
            }
        }

        return length;
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

    @Override
    public void close() {
        if(!isClosed()) {
            try {
                if (inputStream.isPresent()) {
                    try {
                        inputStream.get().close();
                    } catch (final IOException e) {
                        LOG.warn(e);
                    }
                    inputStream = Optional.empty();
                    length = -1;
                }
            } finally {
                super.close();
            }
        }
    }
}
