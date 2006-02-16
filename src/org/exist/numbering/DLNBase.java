/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-06 The eXist Project
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *  $Id$
 */
package org.exist.numbering;

import org.exist.storage.io.VariableByteInput;

import java.io.IOException;

/**
 * Base class representing a node id in the form of a dynamic level number (DLN).
 * See {@link DLN}. DLNBase handles the efficient binary encoding of node ids.
 *
 * Level values are stored consecutively, using a fixed prefix free encoding. The number of
 * units to be used for encoding a single level value is dynamically adjusted. We start with
 * one unit and use its n - 1 lower bits. If the number exceeds the lower bits, we add another
 * unit and set the highest bit to 1. This process is repeated for larger numbers. As a result,
 * the first 1 bits of a level id indicate the number of fixed-size units used for encoding a level id.
 * We thus don't need separator bits between the units themselves.
 *
 * @author wolf
 *
 */
public class DLNBase implements Comparable {

    /**
     * The default number of bits used per fixed
     * size unit.
     */
    public final static int BITS_PER_UNIT = 4;

    /**
     * Lists the maximum number that can be encoded
     * by a given number of units. PER_COMPONENT_SIZE[0]
     * corresponds to 1 unit used, PER_COMPONENT_SIZE[1]
     * to 2 units, and so on. With BITS_PER_UNIT = 4, the largest 
     * number to be encoded by 1 unit is 7, for 2 units it's 71, for
     * 3 units 583 ...
     */
    protected final static int[] PER_COMPONENT_SIZE = initComponents();

    private final static int[] initComponents() {
        int size[] = new int[10];
        size[0] = 7;  // = Math.pow(2, 3) - 1;
        int components, numBits;
        for (int i = 1; i < size.length; i++) {
            components = i + 1;
            numBits = components * BITS_PER_UNIT - components;
            size[i] = (int)(Math.pow(2, numBits)) + size[i - 1];
        }
        return size;
    }

    protected final static int UNIT_SHIFT = 3;

    // the bits are stored in a byte[] 
    protected byte[] bits;

    // the current index into the byte[] used
    // for appending new bits
    protected int bitIndex = -1;

    public DLNBase() {
        bits = new byte[1];
    }

    public DLNBase(DLNBase dln) {
        this.bits = new byte[dln.bits.length];
        System.arraycopy(dln.bits, 0, this.bits, 0, dln.bits.length);
        this.bitIndex = dln.bitIndex;
    }

    public DLNBase(int units, byte[] data, int startOffset) {
        int blen = (int) Math.ceil((units * BITS_PER_UNIT) / 8.0);
        bits = new byte[blen];
        System.arraycopy(data, startOffset, bits, 0, blen);
        bitIndex = units * BITS_PER_UNIT - 1;
    }

    protected DLNBase(byte[] data, int nbits) {
        int remainder = nbits % 8;
        int len = nbits / 8;
        bits = new byte[len + (remainder > 0 ? 1 : 0)];
        if (len > 0)
            System.arraycopy(data, 0, bits, 0, len);
        if (remainder > 0) {
            byte b = 0;
            for (int i = 0; i < remainder; i++) {
                if ((data[len] & (1 << ((7 - i) & 7))) != 0) {
                    b |= 1 << (7 - i);
                }
            }
            bits[len] = b;
        }
        bitIndex = nbits - 1;
    }

    public DLNBase(VariableByteInput is) throws IOException {
        int units = is.readByte();
        int blen = (int) Math.ceil((units * BITS_PER_UNIT) / 8.0);
        bits = new byte[blen];
        is.read(bits);
        bitIndex = units * BITS_PER_UNIT - 1;
    }

    /**
     * Set the level id which starts at offset to the
     * given id value.
     *
     * @param offset
     * @param levelId
     */
    public void setLevelId(int offset, int levelId) {
        bitIndex = offset - 1;
        setCurrentLevelId(levelId);
    }

    /**
     * Adds a new level to the node id, using levelId
     * as initial value.
     *
     * @param levelId initial value
     */
    public void addLevelId(int levelId) {
        setCurrentLevelId(levelId);
    }

    /**
     * Increments the last level id by one.
     */
    public void incrementLevelId() {
        int last = lastLevelOffset();
        bitIndex = last - 1;
        setCurrentLevelId(getLevelId(last) + 1);
    }

    /**
     * Set the level id for the last level that has been written.
     * The data array will be resized automatically if the bit set is
     * too small to encode the id.
     *
     * @param levelId
     */
    protected void setCurrentLevelId(int levelId) {
        int units = getUnitsRequired(levelId);
        int numBits = bitWidth(units);
        if (units > 1)
            levelId -= PER_COMPONENT_SIZE[units - 2];
        for (int i = 1; i < units; i++) {
            setNextBit(true);
        }

        setNextBit(false);

        for (int i = numBits - 1; i >= 0; i--) {
            setNextBit(((levelId >>> i) & 1) != 0);
        }
    }

    /**
     * Returns the id starting at offset.
     *
     * @param startBit
     * @return the level id
     */
    public int getLevelId(int startBit) {
        int units = unitsUsed(startBit, bits);
        startBit += units;
        int numBits = bitWidth(units);
        int id = 0;
        for (int i = numBits - 1; i >= 0; i--) {
            if ((bits[startBit >> UNIT_SHIFT] & (1 << ((7 - startBit++) & 7))) != 0) {
                id |= 1 << i;
            }
        }
        if (units > 1)
            id += PER_COMPONENT_SIZE[units - 2];
        return id;
    }

    /**
     * Returns the number of units currently used
     * to encode the id. The size of a single unit is
     * given by {@link #BITS_PER_UNIT}.
     *
     * @return the number of units
     */
    public byte units() {
        return (byte) ((bitIndex + 1) / BITS_PER_UNIT);
    }

    /**
     * Returns the size of this id by counting the bits
     * used to encode it.
     *
     * @return the size in bits
     */
    public int size() {
        return bits.length;
    }

    private static int unitsUsed(int startBit, byte[] bits) {
        int units = 1;
        while ((bits[startBit >> UNIT_SHIFT] & (1 << ((7 - startBit++) & 7))) != 0) {
            ++units;
        }
        return units;
    }

    /**
     * Returns the number of level in this id, which corresponds
     * to the depth at which the node occurs within the node tree.
     *
     * @return the number of levels in this id
     */
    public int getLevelCount() {
        int bit = 0;
        int count = 0;
        while (bit > -1 && bit <= bitIndex) {
            int units = unitsUsed(bit, bits);
            bit += units;
            bit += bitWidth(units);
            count++;
        }
        return count;
    }

    /**
     * Return all level ids converted to int.
     *
     * @return all level ids in this node id.
     */
    public int[] getLevelIds() {
        int count = getLevelCount();
        int[] ids = new int[count];
        int offset = 0;
        for (int i = 0; i < count; i++) {
            ids[i] = getLevelId(offset);
            offset += getUnitsRequired(ids[i]) * BITS_PER_UNIT;
        }
        return ids;
    }

    /**
     * Find the last level in the id and return its offset.
     *
     * @return start-offset of the last level id.
     */
    protected int lastLevelOffset() {
        int bit = 0;
        int lastOffset = 0;
        while (bit > -1 && bit <= bitIndex) {
            lastOffset = bit;
            int units = unitsUsed(bit, bits);
            bit += units;
            bit += bitWidth(units);
        }
        return lastOffset;
    }

    public boolean startsWith(DLNBase other) {
        return true;
    }

    /**
     * Set (or unset) then next bit in the current sequence
     * of bits. The current position is moved forward and the
     * bit set is resized if necessary.
     *
     * @param value the value of the bit to set, i.e. 1 (true) or 0 (false)
     */
    private void setNextBit(boolean value) {
        ++bitIndex;
        if ((bitIndex >> UNIT_SHIFT) >= bits.length) {
            byte[] new_bits = new byte[bits.length + 1];
            System.arraycopy(bits, 0, new_bits, 0, bits.length);
            bits = new_bits;
        }

        if (value)
            bits[bitIndex >> UNIT_SHIFT] |= 1 << ((7 - bitIndex) & 7);
        else
            bits[bitIndex >> UNIT_SHIFT] &= ~(1 << ((7 - bitIndex) & 7));
    }

    /**
     * Calculates the number of bits available in a bit set
     * that uses the given number of units. These are the bits
     * that can be actually used for the id, not including the
     * trailing address bits.
     * 
     * @param units
     * @return number of bits available
     */
    protected static int bitWidth(int units) {
        return (units * BITS_PER_UNIT) - units;
    }

    /**
     * Calculates the minimum number of units that would be required
     * to properly encode the given integer.
     * 
     * @param levelId the integer to encode in the level id
     * @return number of units required
     */
    protected static int getUnitsRequired(int levelId) {
        for (int i = 0; i < PER_COMPONENT_SIZE.length; i++) {
            if (levelId < PER_COMPONENT_SIZE[i])
                return i + 1;
        }
        // can't happen
        throw new IllegalStateException("Number of nodes exceeds the internal limit");
    }

    public void serialize(byte[] data, int offset) {
        System.arraycopy(bits, 0, data, offset, bits.length);
    }

    public int compareTo(final DLNBase other) {
        final int a1len = bits.length;
        final int a2len = other.bits.length;

        int limit = a1len <= a2len ? a1len : a2len;
        byte[] obits = other.bits;
        for (int i = 0; i < limit; i++) {
            byte b1 = bits[i];
            byte b2 = obits[i];
            if (b1 != b2)
                return (b1 & 0xFF) - (b2 & 0xFF);
        }
        return (a1len - a2len);
    }

    public int compareTo(Object obj) {
        DLNBase other = (DLNBase) obj;
        return compareTo(other);
    }

    public String debug() {
        StringBuffer buf = new StringBuffer();
        buf.append(toString());
        buf.append(" = ");
        buf.append(toBitString());
        buf.append(" [");
        buf.append(bitIndex + 1);
        buf.append(']');
        return buf.toString();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        int[] ids = getLevelIds();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) buf.append('.');
            buf.append(ids[i]);
        }
        return buf.toString();
    }

    public String toBitString() {
        StringBuffer buf = new StringBuffer();
        int len = bits.length;
        for (int i = 0; i < len; i++) {
            buf.append(toBitString(bits[i]));
        }
        return buf.toString();
    }

    private final static char[] digits = { '0', '1' };

    /**
     * Returns a string showing the bit representation
     * of the given byte.
     * 
     * @param b the byte to display
     * @return string representation
     */
    private static String toBitString(byte b) {
        char[] buf = new char[8];
        int charPos = 8;
        int radix = 2;
        int mask = radix - 1;
        for (int i = 0; i < 8; i++) {
            buf[--charPos] = digits[b & mask];
            b >>>= 1;
        }
        return new String(buf);
    }
}
