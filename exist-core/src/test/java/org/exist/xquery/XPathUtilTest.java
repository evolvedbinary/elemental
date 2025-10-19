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

import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
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
}
