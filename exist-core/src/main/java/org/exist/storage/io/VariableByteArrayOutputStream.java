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

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.exist.util.ByteArray;
import org.exist.util.FixedByteArray;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A byte array output stream using VBE (Variable Byte Encoding).
 *
 * The choice of the backing buffer is quite tricky, we have two easy options:
 *
 * 1. org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream
 *    This allocates multiple underlying buffers in sequence, which means that appends to the buffer always allocate
 *    a new buffer, and so there is no GC overhead for appending. However, for serialization #toArray() involves
 *    allocating a new array and copying data from those multiple buffers into the new array, this requires 2x
 *    memory.
 *    NOTE: Previously this classes {@link VariableByteArrayOutputStream#toByteArray()} made a copy anyway, and so
 *    previously required 2x memory.
 *
 * 2. it.unimi.dsi.fastutil.io.UnsynchronizedByteArrayOutputStream
 *    This allocates a single underlying buffer, appends that
 *    would overflow the underlying buffer cause a new buffer to be allocated, data copied, and the old buffer left
 *    to GC. This means that appends which require resizing the buffer can be expensive. However, #toArray() is not
 *    needed as access to the underlying array is permitted, so this is very cheap for serializing.
 *
 * Likely there are different scenarios where each is more appropriate.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class VariableByteArrayOutputStream extends OutputStream implements VariableByteOutput {

    private final UnsynchronizedByteArrayOutputStream os;
    private final VariableByteFilterOutputStream vbfo;

    public VariableByteArrayOutputStream() {
        this(512);
    }

    public VariableByteArrayOutputStream(final int size) {
        this.os = UnsynchronizedByteArrayOutputStream.builder().setBufferSize(size).get();
        this.vbfo = new VariableByteFilterOutputStream(os);
    }

    public void clear() {
        os.reset();
    }

    @Override
    public void close() throws IOException {
        vbfo.close();
    }

    public int size() {
        return os.size();
    }

    public byte[] toByteArray() {
        return os.toByteArray();
    }

    public ByteArray data() {
        return new FixedByteArray(toByteArray());
    }

    @Override
    public void write(final int b) throws IOException {
        vbfo.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        vbfo.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        vbfo.write(b, off, len);
    }

    @Override
    public void writeByte(final byte b) throws IOException {
        vbfo.writeByte(b);
    }

    @Override
    public void writeShort(final int s) throws IOException {
        vbfo.writeShort(s);
    }

    @Override
    public void writeFixedShort(final short s) throws IOException {
        vbfo.writeFixedShort(s);
    }

    @Override
    public void writeInt(final int i) throws IOException {
        vbfo.writeInt(i);
    }

    @Override
    public void writeFixedInt(final int i) throws IOException {
        vbfo.writeFixedInt(i);
    }

    @Override
    public void writeLong(final long l) throws IOException {
        vbfo.writeLong(l);
    }

    @Override
    public void writeFixedLong(final long l) throws IOException {
        vbfo.writeFixedLong(l);
    }

    @Override
    public void writeBigInteger(final BigInteger bi) throws IOException {
        vbfo.writeBigInteger(bi);
    }

    @Override
    public void writeFixedBigInteger(final BigInteger bi) throws IOException {
        vbfo.writeFixedBigInteger(bi);
    }

    @Override
    public void writeBigDecimal(final BigDecimal bd) throws IOException {
        vbfo.writeBigDecimal(bd);
    }

    @Override
    public void writeFixedBigDecimal(final BigDecimal bd) throws IOException {
        vbfo.writeFixedBigDecimal(bd);
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        vbfo.writeUTF(s);
    }

    @Override
    public void flush() throws IOException {
        vbfo.flush();
    }
}
