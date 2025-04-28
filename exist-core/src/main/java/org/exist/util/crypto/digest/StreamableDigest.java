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

/**
 * Interface for a Streamable Digest implementation.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface StreamableDigest {

    /**
     * Update the message digest calculation with more data.
     *
     * @param b the data
     */
    void update(final byte b);

    /**
     * Update the message digest calculation with more data.
     *
     * @param buf the data
     * @param offset the position in the {@code buf} to start reading from
     * @param len the number of bytes to read from the {@code offset}
     */
    void update(final byte[] buf, final int offset, final int len);

    /**
     * Updates the message digest calculation with more data.
     *
     * @param buf the data
     */
    default void update(final byte[] buf) {
        update(buf, 0, buf.length);
    }

    /**
     * Gets the type of the message digest
     *
     * @return the type of the message digest
     */
    DigestType getDigestType();

    /**
     * Gets the current message digest.
     *
     * NOTE this does not produce a copy of the digest,
     * calls to {@link #reset()} or {@code #update} will
     * modify the returned value!
     *
     * @return the message digest
     */
    byte[] getMessageDigest();

    /**
     * Gets the current message digest as a {@code Message Digest}.
     *
     * The underlying byte array will be copied.
     *
     * @return a copy of the message digest.
     */
    MessageDigest copyMessageDigest();

    /**
     * Reset the digest function so that it can be reused
     * for a new stream.
     */
    void reset();
}
