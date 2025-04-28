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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream which calculates a digest of the
 * data that is read.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class DigestInputStream extends FilterInputStream {

    private final StreamableDigest streamableDigest;

    /**
     * Creates an input stream filter which calculates a digest
     * as the underlying input stream is read.
     *
     * @param is the input stream
     * @param streamableDigest the streamable digest
     */
    public DigestInputStream(final InputStream is, final StreamableDigest streamableDigest) {
        super(is);
        this.streamableDigest = streamableDigest;
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            streamableDigest.update((byte)(b & 0xFF));
        }
        return b;
    }

    @Override
    public int read(final byte[] buf, final int off, int len) throws IOException {
        len = in.read(buf, off, len);
        if (len != -1) {
            streamableDigest.update(buf, off, len);
        }
        return len;
    }

    @Override
    public long skip(final long n) throws IOException {
        final byte[] buf = new byte[512];
        long total = 0;
        while (total < n) {
            long len = n - total;
            len = read(buf, 0, len < buf.length ? (int)len : buf.length);
            if (len == -1) {
                return total;
            }
            total += len;
        }
        return total;
    }

    public StreamableDigest getStreamableDigest() {
        return streamableDigest;
    }
}
