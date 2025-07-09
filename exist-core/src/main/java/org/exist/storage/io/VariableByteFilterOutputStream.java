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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream filter using VBE (Variable Byte Encoding).
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class VariableByteFilterOutputStream extends FilterOutputStream implements VariableByteOutput {

    private final VariableByteOutput vbo;

    public VariableByteFilterOutputStream(final OutputStream os) {
        super(os);
        this.vbo = new VariableByteOutputToOutputStream(os);
    }

    @Override
    public void write(final int b) throws IOException {
        vbo.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        vbo.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        vbo.write(b, off, len);
    }

    @Override
    public void writeByte(final byte b) throws IOException {
        vbo.writeByte(b);
    }

    @Override
    public void writeShort(final int s) throws IOException {
        vbo.writeShort(s);
    }

    @Override
    public void writeFixedShort(final short s) throws IOException {
        vbo.writeFixedShort(s);
    }

    @Override
    public void writeInt(final int i) throws IOException {
        vbo.writeInt(i);
    }

    @Override
    public void writeFixedInt(final int i) throws IOException {
        vbo.writeFixedInt(i);
    }

    @Override
    public void writeLong(final long l) throws IOException {
        vbo.writeLong(l);
    }

    @Override
    public void writeFixedLong(final long l) throws IOException {
        vbo.writeFixedLong(l);
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        vbo.writeUTF(s);
    }
}
