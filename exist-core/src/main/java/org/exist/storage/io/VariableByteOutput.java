/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.storage.io;

import java.io.IOException;

/**
 * Interface for writing variable byte encoded values.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface VariableByteOutput {

    /**
     * Write a byte to the output.
     *
     * @param b the byte to write.
     */
    void write(final int b) throws IOException;

    /**
     * Write a byte to the output.
     *
     * @param b the byte to write.
     */
    void writeByte(final byte b) throws IOException;

    /**
     * Write bytes to the output.
     *
     * @param buf the bytes to write.
     */
    void write(final byte[] buf) throws IOException;

    /**
     * Write bytes to the output.
     *
     * @param buf the bytes to write.
     * @param off the offset to read the bytes from.
     * @param len the length of bytes to read.
     */
    void write(final byte[] buf, final int off, final int len) throws IOException;

    /**
     * Writes a VBE short to the output.
     *
     * The encoding scheme requires the following storage
     * for numbers between (inclusive):
     *
     *  {@link Short#MIN_VALUE} and -1, 5 bytes
     *  0 and 127, 1 byte
     *  128 and 16383, 2 bytes
     *  16384 and {@link Short#MAX_VALUE}, 3 bytes
     *
     *  @param s the short to write.
     */
    void writeShort(int s) throws IOException;

    /**
     * Write a fixed size short to the output.
     *
     * Requires 2 bytes.
     *
     * @param s the short to write.
     */
    void writeFixedShort(final short s) throws IOException;

    /**
     * Writes a VBE int to the output.
     *
     * The encoding scheme requires the following storage
     * for numbers between (inclusive):
     *
     *  {@link Integer#MIN_VALUE} and -1, 5 bytes
     *  0 and 127, 1 byte
     *  128 and 16383, 2 bytes
     *  16384 and 2097151, 3 bytes
     *  2097152 and 268435455, is 4 bytes
     *  268435456 and {@link Integer#MAX_VALUE}, 5 bytes
     *
     *  @param i the integer to write.
     */
    void writeInt(int i) throws IOException;

    /**
     * Write a fixed size int to the output.
     *
     * Requires 4 bytes.
     *
     * @param i the integer to write.
     */
    void writeFixedInt(final int i) throws IOException;

    /**
     * Writes a VBE long to the output.
     *
     * The encoding scheme requires the following storage
     * for numbers between (inclusive):
     *
     *  {@link Long#MIN_VALUE} and -1, 10 bytes
     *  0 and 127, 1 byte
     *  128 and 16383, 2 bytes
     *  16384 and 2097151, 3 bytes
     *  2097152 and 268435455, is 4 bytes
     *  268435456 and 34359738367, 5 bytes
     *  34359738368 and 4398046511103, 6 bytes
     *  4398046511104 and 562949953421311, 7 bytes
     *  562949953421312 and 72057594037927935, 8 bytes
     *  72057594037927936 and 9223372036854775807, 9 bytes
     *  9223372036854775808 and {@link Long#MAX_VALUE}, 10 bytes
     *
     * @param l the long to write.
     */
    void writeLong(long l) throws IOException;

    /**
     * Write a fixed size long to the output.
     *
     * Requires 8 bytes.
     *
     * @param l the long to write.
     */
    void writeFixedLong(final long l) throws IOException;

    /**
     * Write a string as UTF-8 encoded bytes to the output.
     *
     * @param s the string to write.
     */
    void writeUTF(final String s) throws IOException;
}
