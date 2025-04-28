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

import org.apache.commons.codec.CodecPolicy;

import java.io.OutputStream;

/**
 * Hexadecimal encoding OutputStream.
 *
 * Same as {@link org.apache.commons.codec.binary.Base16OutputStream#Base16OutputStream(OutputStream, boolean)}
 * but uses lower-case and a strict policy by default.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class HexOutputStream extends org.apache.commons.codec.binary.Base16OutputStream {

    /**
     * Creates a HexOutputStream such that all data written is Hex-encoded to the original provided OutputStream.
     *
     * @param out the OutputStream to wrap.
     * @param doEncode true to encode.
     */
    public HexOutputStream(final OutputStream out, final boolean doEncode) {
        this(out, doEncode, true);
    }

    /**
     * Creates a HexOutputStream such that all data written is Hex-encoded to the original provided OutputStream.
     *
     * @param out the OutputStream to wrap.
     * @param doEncode true to encode.
     * @param lowerCase true to use lower case, or false for upper case.
     */
    public HexOutputStream(final OutputStream out, final boolean doEncode, final boolean lowerCase) {
        super(out, doEncode, lowerCase, CodecPolicy.STRICT);
    }
}
