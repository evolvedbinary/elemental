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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:patrick@reini.net">Patrick Reinhart</a>
 */
public interface ContentFile extends AutoCloseable {

    enum ContentFileType {
        /**
         * The file contains content of which the format is unknown.
         */
        UNKNOWN,

        /**
         * The file contains content that has been serialized using the XDM method.
         */
        XDM_SERIALIZATION,

        /**
         * The file contains content that has been serialized using the XQJ method.
         */
        XQJ_SERIALIZATION;
    }

    /**
     * Get the type of the Content File.
     *
     * @return the type of the Content File.
     */
    ContentFileType getType();

    @Override
    /**
     * Closes all resource held by the implementation as open files or off HEAP memory as example.
     */
    void close();

    /**
     * Returns the complete content as an byte array.
     *
     * @return the content as byte array
     */
    byte[] getBytes();

    /**
     * Returns the size of the conent in bytes
     *
     * @return the content size
     */
    long size();

    /**
     * Returns a new {@link InputStream} instance based on the content data.
     *
     * @return a new input stream based on the content.
     * @throws IOException if an error occurs accessing the content
     */
    default InputStream newInputStream() throws IOException {
        return new ByteArrayInputStream(getBytes());
    }

    /**
     * Returns a new {@link OutputStream} instance to write content data.
     * @return a new output stream for writing content
     * @throws IOException if an error occurs accessing the content
     * @throws UnsupportedOperationException if the operation is not available
     */
    default OutputStream newOutputStream() throws IOException {
        throw new UnsupportedOperationException("not supported");
    }
}
