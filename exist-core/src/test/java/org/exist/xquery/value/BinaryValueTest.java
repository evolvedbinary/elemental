/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.xquery.value;


import com.googlecode.junittoolbox.ParallelRunner;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.exist.xquery.XPathException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam@existsolutions.com">Adam Retter</a>
 */
@RunWith(ParallelRunner.class)
public class BinaryValueTest {

    @Test
    public void cast_base64_to_base64() throws XPathException {
        final BinaryValueManager binaryValueManager = new MockBinaryValueManager();

        final BinaryValue mockBase64BinaryValue = EasyMock.createMockBuilder(BinaryValue.class)
                .withConstructor(BinaryValueManager.class, BinaryValueType.class)
                .withArgs(binaryValueManager, new Base64BinaryValueType())
                .createMock();

        replay(mockBase64BinaryValue);

        final AtomicValue result = mockBase64BinaryValue.convertTo(Type.BASE64_BINARY);

        verify(mockBase64BinaryValue);

        assertEquals(mockBase64BinaryValue, result);
    }

    @Test
    public void cast_base64_to_hexBinary() throws XPathException {
        final BinaryValueManager binaryValueManager = new MockBinaryValueManager();

        final BinaryValue mockBase64BinaryValue = EasyMock.createMockBuilder(BinaryValue.class)
                .withConstructor(BinaryValueManager.class, BinaryValueType.class)
                .withArgs(binaryValueManager, new Base64BinaryValueType())
                .addMockedMethod("convertTo", BinaryValueType.class)
                .createMock();

        final BinaryValue mockHexBinaryValue = EasyMock.createMockBuilder(BinaryValue.class)
                .withConstructor(BinaryValueManager.class, BinaryValueType.class)
                .withArgs(binaryValueManager, new HexBinaryValueType())
                .createMock();

        expect(mockBase64BinaryValue.convertTo(isA(HexBinaryValueType.class))).andReturn(mockHexBinaryValue);

        replay(mockBase64BinaryValue, mockHexBinaryValue);

        final AtomicValue result = mockBase64BinaryValue.convertTo(Type.HEX_BINARY);

        verify(mockBase64BinaryValue, mockHexBinaryValue);

        assertEquals(mockHexBinaryValue, result);
    }

    @Test
    public void cast_hexBinary_to_hexBase64() throws XPathException {

        final BinaryValueManager binaryValueManager = new MockBinaryValueManager();

        final BinaryValue mockHexBinaryValue = EasyMock.createMockBuilder(BinaryValue.class)
                .withConstructor(BinaryValueManager.class, BinaryValueType.class)
                .withArgs(binaryValueManager, new HexBinaryValueType())
                .addMockedMethod("convertTo", BinaryValueType.class)
                .createMock();

        final BinaryValue mockBase64BinaryValue = EasyMock.createMockBuilder(BinaryValue.class)
                .withConstructor(BinaryValueManager.class, BinaryValueType.class)
                .withArgs(binaryValueManager, new Base64BinaryValueType())
                .createMock();

        expect(mockHexBinaryValue.convertTo(isA(Base64BinaryValueType.class))).andReturn(mockBase64BinaryValue);

        replay(mockHexBinaryValue, mockBase64BinaryValue);

        final AtomicValue result = mockHexBinaryValue.convertTo(Type.BASE64_BINARY);

        verify(mockHexBinaryValue, mockBase64BinaryValue);

        assertEquals(mockBase64BinaryValue, result);
    }
}