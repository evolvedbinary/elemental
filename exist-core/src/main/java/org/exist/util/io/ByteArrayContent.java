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
package org.exist.util.io;

import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:patrick@reini.net">Patrick Reinhart</a>
 */
public final class ByteArrayContent extends AbstractContentFile {
    private static final byte[] EMPTY_BUFFER = new byte[0];

    private byte[] data;

    public static ByteArrayContent of(final ContentFileType type, final byte[] data) {
        return new ByteArrayContent(type, data);
    }

    public static ByteArrayContent of(final ContentFileType type, final String data) {
        return of(type, data.getBytes(StandardCharsets.UTF_8));
    }

    private ByteArrayContent(final ContentFileType type, final byte[] data) {
        super(type);
        this.data = data;
    }

    @Override
    public void close() {
        data = null;
    }

    @Override
    public byte[] getBytes() {
        return data == null ? EMPTY_BUFFER : data;
    }

    @Override
    public long size() {
        return data == null ? 0 : data.length;
    }
}
