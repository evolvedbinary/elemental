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
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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

/**
 * A byte array input using VBE (Variable Byte Encoding).
 * 
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class VariableByteArrayInput extends AbstractVariableByteInput {

    private byte[] data;
    protected int position;
    private int end;

    public VariableByteArrayInput(final byte[] data) {
        super();
        this.data = data;
        this.position = 0;
        this.end = data.length;
    }

    public VariableByteArrayInput(final byte[] data, final int offset, final int length) {
        super();
        this.data = data;
        this.position = offset;
        this.end = offset + length;
    }

    public void initialize(final byte[] data, final int offset, final int length) {
        this.data = data;
        this.position = offset;
        this.end = offset + length;
    }

    @Override
    public int read() {
        if (position == end) {
            return -1;
        }
        return data[position++] & 0xFF;
    }

    @Override
    public int available() {
        return end - position;
    }

    @Override
    public void skip(final int count) {
        for (int i = 0; i < count; i++) {
            while (position < end && (data[position++] & 128) > 0) {
                //Nothing to do
            }
        }
    }

    @Override
    public void skipBytes(final long count) {
        for (long i = 0; i < count && position < end; i++) {
            position++;
        }
    }
}