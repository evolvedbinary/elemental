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
package org.exist.util.crypto.digest;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An input stream which calculates a digest of the
 * data that is written.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class DigestOutputStream extends FilterOutputStream {

    private final StreamableDigest streamableDigest;

    /**
     * Creates an output stream filter which calculates a digest
     * as the underlying output stream is written.
     *
     * @param os the input stream
     * @param streamableDigest the streamable digest
     */
    public DigestOutputStream(final OutputStream os, final StreamableDigest streamableDigest) {
        super(os);
        this.streamableDigest = streamableDigest;
    }

    @Override
    public void write(final int b) throws IOException {
        out.write(b);
        streamableDigest.update((byte) (b & 0xFF));
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
        streamableDigest.update(b, off, len);
    }

    public StreamableDigest getStreamableDigest() {
        return streamableDigest;
    }
}
