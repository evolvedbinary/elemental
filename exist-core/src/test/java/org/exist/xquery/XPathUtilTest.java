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
package org.exist.xquery;

import org.exist.xquery.functions.array.ArrayType;
import org.exist.xquery.value.ArrayWrapper;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class XPathUtilTest {

    @Test
    public void listStringsToSequenceStrings() throws XPathException {
        final XQueryContext mockContext = mock(XQueryContext.class);

        final List<String> strings = Arrays.asList("hello", "goodbye");

        replay(mockContext);
        final Sequence result = XPathUtil.javaObjectToXPath(strings, mockContext);
        assertEquals(strings.size(), result.getItemCount());
        for (int i = 0; i < strings.size(); i++) {
            assertEquals(Type.STRING, result.itemAt(i).getType());
            assertEquals(strings.get(i), result.itemAt(i).getStringValue());
        }
        verify(mockContext);
    }

    @Test
    public void arrayStringsToSequenceStrings() throws XPathException {
        final XQueryContext mockContext = mock(XQueryContext.class);

        final String[] strings = { "hello", "goodbye" };

        replay(mockContext);
        final Sequence result = XPathUtil.javaObjectToXPath(strings, mockContext);
        assertEquals(strings.length, result.getItemCount());
        for (int i = 0; i < strings.length; i++) {
            assertEquals(Type.STRING, result.itemAt(i).getType());
            assertEquals(strings[i], result.itemAt(i).getStringValue());
        }
        verify(mockContext);
    }

    @Test
    public void listListStringsToSequenceStrings() throws XPathException {
        final XQueryContext mockContext = mock(XQueryContext.class);

        final List<List<String>> list = Arrays.asList(
            Arrays.asList("hello"),
            Arrays.asList("goodbye", "see you again soon")
        );

        replay(mockContext);
        final Sequence result = XPathUtil.javaObjectToXPath(list, mockContext);
        assertEquals(3, result.getItemCount());
        for (int i = 0; i < list.size(); i++) {
            final List<String> subList = list.get(i);
            for (int j = 0; j < subList.size(); j++) {
                assertEquals(Type.STRING, result.itemAt(i + j).getType());
                assertEquals(subList.get(j), result.itemAt(i + j).getStringValue());
            }
        }
        verify(mockContext);
    }

    @Test
    public void arrayArrayStringsToSequenceStrings() throws XPathException {
        final XQueryContext mockContext = mock(XQueryContext.class);

        final Object[] array = new Object[2];
        array[0] = new String[]{ "hello" };
        array[1] = new String[]{ "goodbye", "see you again soon" };

        replay(mockContext);
        final Sequence result = XPathUtil.javaObjectToXPath(array, mockContext);
        assertEquals(3, result.getItemCount());
        for (int i = 0; i < array.length; i++) {
            final String[] subArray = (String[]) array[i];
            for (int j = 0; j < subArray.length; j++) {
                assertEquals(Type.STRING, result.itemAt(i + j).getType());
                assertEquals(subArray[j], result.itemAt(i + j).getStringValue());
            }
        }
        verify(mockContext);
    }

    @Test
    public void arrayWrapperStringsToArrayStrings() throws XPathException {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(1).anyTimes();

        final ArrayWrapper strings = new ArrayWrapper(new String[] { "hello", "goodbye" });

        replay(mockContext);
        final Sequence result = XPathUtil.javaObjectToXPath(strings, mockContext);
        assertEquals(1, result.getItemCount());
        final Item resultItem = result.itemAt(0);
        assertEquals(Type.ARRAY, resultItem.getType());
        final ArrayType resultArray = (ArrayType) resultItem;
        for (int i = 0; i < strings.array.length; i++) {
            final Sequence arrayItem = resultArray.get(i);
            assertTrue(arrayItem.hasOne());
            assertEquals(Type.STRING, arrayItem.itemAt(0).getType());
            assertEquals(strings.array[i], arrayItem.itemAt(0).getStringValue());
        }
        verify(mockContext);
    }

    @Test
    public void arrayArrayWrapperStringsToArrayStrings() throws XPathException {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(1).anyTimes();

        final ArrayWrapper[] strings = {
            new ArrayWrapper(new String[] { "hello" }),
            new ArrayWrapper(new String[] { "goodbye", "see you again soon" }),
        };

        replay(mockContext);
        final Sequence result = XPathUtil.javaObjectToXPath(strings, mockContext);
        assertEquals(strings.length, result.getItemCount());
        for (int i = 0; i < strings.length; i++) {
            final Item resultItem = result.itemAt(i);
            assertEquals(Type.ARRAY, resultItem.getType());

            final ArrayType resultArray = (ArrayType) resultItem;
            for (int j = 0; j < strings[i].array.length; j++) {
                final Sequence arrayItem = resultArray.get(j);
                assertTrue(arrayItem.hasOne());
                assertEquals(Type.STRING, arrayItem.itemAt(0).getType());
                assertEquals(strings[i].array[j], arrayItem.itemAt(0).getStringValue());
            }
        }
        verify(mockContext);
    }

    @Test
    public void arrayArrayWrapperStringSequenceToArrayStrings() throws XPathException {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(1).anyTimes();

        final ArrayWrapper[] strings = {
            new ArrayWrapper(new String[] { "hello" }),
            new ArrayWrapper(new Object[] { "goodbye", new String[] { "see you again soon", "42" } }),
        };

        replay(mockContext);

        final Sequence result = XPathUtil.javaObjectToXPath(strings, mockContext);
        assertEquals(strings.length, result.getItemCount());
        Item resultItem = result.itemAt(0);
        assertEquals(Type.ARRAY, resultItem.getType());
        ArrayType resultArray = (ArrayType) resultItem;
        assertEquals(1, resultArray.getSize());
        Sequence arrayEntry = resultArray.get(0);
        assertEquals(1, arrayEntry.getItemCount());
        assertEquals(Type.STRING, arrayEntry.itemAt(0).getType());
        assertEquals("hello", arrayEntry.itemAt(0).getStringValue());

        resultItem = result.itemAt(1);
        assertEquals(Type.ARRAY, resultItem.getType());
        resultArray = (ArrayType) resultItem;
        assertEquals(2, resultArray.getSize());
        arrayEntry = resultArray.get(0);
        assertEquals(1, arrayEntry.getItemCount());
        assertEquals(Type.STRING, arrayEntry.itemAt(0).getType());
        assertEquals("goodbye", arrayEntry.itemAt(0).getStringValue());
        arrayEntry = resultArray.get(1);
        assertEquals(2, arrayEntry.getItemCount());
        assertEquals(Type.STRING, arrayEntry.itemAt(0).getType());
        assertEquals("see you again soon", arrayEntry.itemAt(0).getStringValue());
        assertEquals(Type.STRING, arrayEntry.itemAt(1).getType());
        assertEquals("42", arrayEntry.itemAt(1).getStringValue());

        verify(mockContext);
    }
}
