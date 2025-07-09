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

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Abstract base class for implementations of VariableByteOutput.
 *
 * Note that the VBE scheme used by this class
 * does not offer any advantage for negative numbers, in fact
 * it requires significantly more storage for those; see the javadoc
 * on the appropriate encoding method for details.
 *
 * If support for negative numbers is desired then, the reader
 * should look to zig-zag encoding as used in the varint's of
 * Google's Protocol Buffers https://developers.google.com/protocol-buffers/docs/encoding#signed-integers
 * or Hadoop's VarInt encoding, see org.apache.hadoop.io.file.tfile.Utils#writeVInt(java.io.DataOutput, int).
 *
 * VBE is never an alternative to having advance knowledge of number
 * ranges and using fixed size byte arrays to represent them.
 *
 * Rather, for example, it is useful when you have an int that could be
 * in any range between 0 and {@link Integer#MAX_VALUE}, but is likely
 * less than 2,097,151, in that case you would save at least 1 byte for
 * each int value that is written to the output stream that is
 * less than 2,097,151.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractVariableByteOutput implements VariableByteOutput {

    @Override
    public void writeByte(final byte b) throws IOException {
        write(b);
    }

    @Override
    public void write(final byte[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(final byte[] buf, final int off, final int len) throws IOException {
        for (int i = off; i < off + len; i++) {
            write(buf[i]);
        }
    }

    @Override
    public void writeShort(int s) throws IOException {
        while ((s & ~0177) != 0) {
            write((byte) ((s & 0177) | 0200));
            s >>>= 7;
        }
        write((byte) s);
    }

    @Override
    public void writeFixedShort(final short s) throws IOException {
        write((byte) ((s >>> 0) & 0xff));
        write((byte) ((s >>> 8) & 0xff));
    }

    @Override
    public void writeInt(int i) throws IOException {
        while ((i & ~0177) != 0) {
            write((byte) ((i & 0177) | 0200));
            i >>>= 7;
        }
        write((byte) i);
    }

    @Override
    public void writeFixedInt(final int i) throws IOException {
        write((byte) ((i >>> 0) & 0xff));
        write((byte) ((i >>> 8) & 0xff));
        write((byte) ((i >>> 16) & 0xff));
        write((byte) ((i >>> 24) & 0xff));
    }

    @Override
    public void writeLong(long l) throws IOException {
        while ((l & ~0177) != 0) {
            write((byte) ((l & 0177) | 0200));
            l >>>= 7;
        }
        write((byte) l);
    }

    @Override
    public void writeFixedLong(final long l) throws IOException {
        write((byte) ((l >>> 56) & 0xff));
        write((byte) ((l >>> 48) & 0xff));
        write((byte) ((l >>> 40) & 0xff));
        write((byte) ((l >>> 32) & 0xff));
        write((byte) ((l >>> 24) & 0xff));
        write((byte) ((l >>> 16) & 0xff));
        write((byte) ((l >>> 8) & 0xff));
        write((byte) ((l >>> 0) & 0xff));
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        final byte[] data = s.getBytes(UTF_8);
        writeInt(data.length);
        write(data, 0, data.length);
    }
}
