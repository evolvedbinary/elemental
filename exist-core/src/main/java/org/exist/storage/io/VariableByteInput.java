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

import java.io.IOException;

/**
 * Interface for reading variable byte encoded values.
 * 
 * Variable byte encoding offers a good compression ratio if the stored
 * values are rather small, i.e. much smaller than the possible maximum for
 * the given type.
 * 
 * @author wolf
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface VariableByteInput {

    /**
     * Read a single byte and return as an int value.
     * 
     * @return the byte value as int or -1 if no more bytes are available.
     * @throws IOException in case of an I/O error
     */
    int read() throws IOException;

    /**
     * Fill the provided byte array with data from the input.
     * 
     * @param data the buffer to read
     * @throws IOException in case of an I/O error
     * @return the number of bytes read
     */
    int read(byte[] data) throws IOException;
    
    int read(byte b[], int off, int len) throws IOException;
    
    /**
     * Returns a value &gt; 0 if more bytes can be read
     * from the input.
     *
     * @throws IOException in case of an I/O error
     * @return the number of bytes available
     */
    int available() throws IOException;
    
    /**
     * Read a single byte. Throws EOFException if no
     * more bytes are available.
     *
     * @throws IOException in case of an I/O error
     * @return the byte read
     */
    byte readByte() throws IOException;

    /**
     * Read a short value in variable byte encoding.
     *
     * @throws IOException in case of an I/O error
     * @return the short read
     */
    short readShort() throws IOException;

    /**
     * Read a fixed size short from the input.
     *
     * Requires 2 bytes.
     *
     * @return the short.
     */
    short readFixedShort() throws IOException;

    /**
     * Read an integer value in variable byte encoding.
     *
     * @throws IOException in case of an I/O error
     * @return the int read
     */
    int readInt() throws IOException;

    /**
     * Read a fixed size int from the input.
     *
     * Requires 4 bytes.
     *
     * @return the int.
     */
    int readFixedInt() throws IOException;
    
    /**
     * Read a long value in variable byte encoding.
     *
     * @throws IOException in case of an I/O error
     * @return the long read
     */
    long readLong() throws IOException;

    /**
     * Read a fixed size long from the input.
     *
     * Requires 8 bytes.
     *
     * @return the long.
     */
    long readFixedLong() throws IOException;

    /**
     * Read a string as UTF-8 encoded bytes from the input.
     *
     * @return the string.
     */
    String readUTF() throws IOException;

    /**
     * Read the following count numeric values from the input
     * and drop them.
     * 
     * @param count the number of bytes to skip
     * @throws IOException in case of an I/O error
     */
    void skip(int count) throws IOException;

    void skipBytes(long count) throws IOException;
    
    /**
     * Copy the next numeric value from the input to the
     * specified output stream.
     * 
     * @param output the output destination to copy the data to
     * @throws IOException in case of an I/O error
     */
    void copyTo(VariableByteOutput output) throws IOException;

    /**
     * Copy the count next numeric values from the input to
     * the specified output stream.
     * 
     * @param os the output destination to copy the data to
     * @param count the number of bytes to copy
     * @throws IOException in case of an I/O error
     */
    void copyTo(VariableByteOutput os, int count)
            throws IOException;
    
    void copyRaw(VariableByteOutput os, int bytes)
    	throws IOException;
}