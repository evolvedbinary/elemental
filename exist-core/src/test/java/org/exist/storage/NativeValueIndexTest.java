/*
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
package org.exist.storage;

import org.exist.xquery.XPathException;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ParameterizedClass
@CsvSource({
    "xs:string," + Type.STRING,
    "xs:int," + Type.INT
})
public class NativeValueIndexTest {

    @Parameter(0)
    public String typeName;
    @Parameter(1)
    public int type;

    @Test
    public void convertToAtomicNull() {
        final AtomicValue result = NativeValueIndex.convertToAtomic(type, null);
        assertNull(result);
    }

    @Test
    public void convertToAtomicEmptyString() {
        final AtomicValue result = NativeValueIndex.convertToAtomic(type, "");
        assertNull(result);
    }

    @Test
    public void convertToAtomic() throws XPathException {
        final String mockValue = "1234567890";
        final AtomicValue result = NativeValueIndex.convertToAtomic(type, mockValue);
        assertEquals(type, result.getType());
        assertEquals(mockValue, result.getStringValue());
    }
}
