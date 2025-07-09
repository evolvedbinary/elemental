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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream filter using VBE (Variable Byte Encoding).
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class VariableByteFilterInputStream extends FilterInputStream implements VariableByteInput {

    private final VariableByteInput vbi;

    public VariableByteFilterInputStream(final InputStream is) {
        super(is);
        vbi = new VariableByteInputToInputStream(is);
    }

    @Override
    public int read() throws IOException {
        return vbi.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return vbi.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return vbi.read(b, off, len);
    }

    @Override
    public byte readByte() throws IOException {
        return vbi.readByte();
    }

    @Override
    public short readShort() throws IOException {
        return vbi.readShort();
    }

    @Override
    public short readFixedShort() throws IOException {
        return vbi.readFixedShort();
    }

    @Override
    public int readInt() throws IOException {
        return vbi.readInt();
    }

    @Override
    public int readFixedInt() throws IOException {
        return vbi.readFixedInt();
    }

    @Override
    public long readLong() throws IOException {
        return vbi.readLong();
    }

    @Override
    public long readFixedLong() throws IOException {
        return vbi.readFixedLong();
    }

    @Override
    public String readUTF() throws IOException {
        return vbi.readUTF();
    }

    @Override
    public void skip(final int count) throws IOException {
        vbi.skip(count);
    }

    @Override
    public void skipBytes(final long count) throws IOException {
        vbi.skipBytes(count);
    }

    @Override
    public void copyTo(final VariableByteOutput os) throws IOException {
        vbi.copyTo(os);
    }

    @Override
    public void copyTo(final VariableByteOutput os, final int count) throws IOException {
        vbi.copyTo(os, count);
    }

    @Override
    public void copyRaw(final VariableByteOutput os, final int bytes) throws IOException {
        vbi.copyRaw(os, bytes);
    }
}
