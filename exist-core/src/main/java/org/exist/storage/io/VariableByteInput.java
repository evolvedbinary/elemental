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
import java.math.BigDecimal;
import java.math.BigInteger;

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
     *
     * @throws IOException in case of an I/O error.
     */
    int read() throws IOException;

    /**
     * Fill the provided byte array with data from the input.
     * 
     * @param data the buffer to write data to.
     *
     * @return the number of bytes written to the buffer.
     *
     * @throws IOException in case of an I/O error
     */
    int read(byte[] data) throws IOException;

    /**
     * Fill the provided byte array with data from the input.
     *
     * @param data the buffer to write data to.
     * @param off the offset in the buffer to start writing at.
     * @param len the maximum number of bytes to write into the buffer.
     *
     * @return the number of bytes written to the buffer.
     *
     * @throws IOException in case of an I/O error.
     */
    int read(byte data[], int off, int len) throws IOException;
    
    /**
     * Returns a value &gt; 0 if more bytes can be read
     * from the input.
     *
     * @return the number of bytes available.
     *
     * @throws IOException in case of an I/O error.
     */
    int available() throws IOException;
    
    /**
     * Read a single byte. Throws EOFException if no
     * more bytes are available.
     *
     * @return the byte read.
     *
     * @throws IOException in case of an I/O error.
     */
    byte readByte() throws IOException;

    /**
     * Read a short value in variable byte encoding.
     *
     * @return the short read.
     *
     * @throws IOException in case of an I/O error.
     */
    short readShort() throws IOException;

    /**
     * Read a fixed size short from the input.
     *
     * Requires 2 bytes.
     *
     * @return the short.
     *
     * @throws IOException in case of an I/O error.
     */
    short readFixedShort() throws IOException;

    /**
     * Read an integer value in variable byte encoding.
     *
     * @return the int read.
     *
     * @throws IOException in case of an I/O error.
     */
    int readInt() throws IOException;

    /**
     * Read a fixed size int from the input.
     *
     * Requires 4 bytes.
     *
     * @return the int.
     *
     * @throws IOException in case of an I/O error.
     */
    int readFixedInt() throws IOException;
    
    /**
     * Read a long value in variable byte encoding.
     *
     * @return the long read.
     *
     * @throws IOException in case of an I/O error.
     */
    long readLong() throws IOException;

    /**
     * Read a fixed size long from the input.
     *
     * Requires 8 bytes.
     *
     * @return the long.
     *
     * @throws IOException in case of an I/O error.
     */
    long readFixedLong() throws IOException;

    /**
     * Read a big integer in variable byte encoding.
     *
     * @return the big integer read.
     *
     * @throws IOException in case of an I/O error.
     */
    BigInteger readBigInteger() throws IOException;

    /**
     * Read a fixed size big integer from input.
     *
     * @return the big integer read.
     *
     * @throws IOException in case of an I/O error.
     */
    BigInteger readFixedBigInteger() throws IOException;

    /**
     * Read a big decimal in variable byte encoding.
     *
     * @return the big decimal read.
     *
     * @throws IOException in case of an I/O error.
     */
    BigDecimal readBigDecimal() throws IOException;

    /**
     * Read a fixed size big decimal from input.
     *
     * @return the big decimal read.
     *
     * @throws IOException in case of an I/O error.
     */
    BigDecimal readFixedBigDecimal() throws IOException;

    /**
     * Read a string as UTF-8 encoded bytes from the input.
     *
     * @return the string.
     *
     * @throws IOException in case of an I/O error.
     */
    String readUTF() throws IOException;

    /**
     * Skip over a number of numeric values from.
     * 
     * @param count the number of numeric values to skip.
     *
     * @throws IOException in case of an I/O error.
     */
    void skip(int count) throws IOException;

    /**
     * Skip over a number of bytes.
     *
     * @param count the number of bytes to skip.
     *
     * @throws IOException in case of an I/O error.
     */
    void skipBytes(long count) throws IOException;
    
    /**
     * Copy the next numeric value from the input to the
     * specified output stream.
     * 
     * @param output the output destination to copy the data to.
     *
     * @throws IOException in case of an I/O error.
     */
    void copyTo(VariableByteOutput output) throws IOException;

    /**
     * Copy the count next numeric values from the input to
     * the specified output stream.
     * 
     * @param os the output destination to copy the data to.
     * @param count the number of bytes to copy.
     *
     * @throws IOException in case of an I/O error.
     */
    void copyTo(VariableByteOutput os, int count)
            throws IOException;

    /**
     * Copy bytes from the input to the specified output stream.
     *
     * @param os the output destination to copy the data to.
     * @param bytes the number of bytes to copy.
     *
     * @throws IOException in case of an I/O error.
     */
    void copyRaw(VariableByteOutput os, int bytes)
    	throws IOException;
}