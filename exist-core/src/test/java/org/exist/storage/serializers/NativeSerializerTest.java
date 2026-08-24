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
package org.exist.storage.serializers;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import io.lacuna.bifurcan.IEntry;
import org.easymock.Capture;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.util.Configuration;
import org.exist.util.serializer.SAXSerializer;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.array.ArrayType;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static com.evolvedbinary.j8fu.function.FunctionE.identity;
import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.easymock.EasyMock.*;
import static org.exist.Namespaces.EXIST_NS;
import static org.exist.Namespaces.EXIST_NS_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeSerializerTest {

    private static final int DEFAULT_START = 1;
    private static final long DEFAULT_COMPILATION_TIME = 0;
    private static final long DEFAULT_EXECUTION_TIME = 0;

    public enum Wrapped {
        NOT_WRAPPED,
        WRAPPED;
    }

    public enum Typed {
        NOT_TYPED,
        TYPED;
    }

    @ParameterizedTest
    @CsvSource({"NOT_WRAPPED,NOT_TYPED", "WRAPPED,NOT_TYPED", "NOT_WRAPPED,TYPED", "WRAPPED,TYPED"})
    void serializeInteger(final Wrapped wrapped, final Typed typed) throws SAXException, XPathException, IOException {
        final Sequence sequence = new ValueSequence();
        sequence.add(new IntegerValue(123));
        sequence.add(new IntegerValue(456));

        assertSerialize(sequence, wrapped == Wrapped.WRAPPED, typed == Typed.TYPED);
    }

    @ParameterizedTest
    @CsvSource({"NOT_WRAPPED,NOT_TYPED", "WRAPPED,NOT_TYPED", "NOT_WRAPPED,TYPED", "WRAPPED,TYPED"})
    void serializeText(final Wrapped wrapped, final Typed typed) throws SAXException, XPathException, IOException {
        final MemTreeBuilder builder = new MemTreeBuilder();
        builder.startDocument();
        final int text1Id = builder.characters("hello");
        builder.comment("separator");
        final int text2Id = builder.characters("world");
        builder.endDocument();

        final Sequence sequence = new ValueSequence();
        sequence.add(builder.getDocument().getNode(text1Id));
        sequence.add(builder.getDocument().getNode(text2Id));

        assertSerialize(sequence, wrapped == Wrapped.WRAPPED, typed == Typed.TYPED);
    }

    @ParameterizedTest
    @CsvSource({"NOT_WRAPPED,NOT_TYPED", "WRAPPED,NOT_TYPED", "NOT_WRAPPED,TYPED", "WRAPPED,TYPED"})
    void serializeArray(final Wrapped wrapped, final Typed typed) throws SAXException, XPathException, IOException {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(1).anyTimes();

        replay(mockContext);

        final Sequence sequence = new ValueSequence();
        final ArrayType array1 = new ArrayType(mockContext, List.of(ValueSequence.of(identity(), new StringValue("hello"), new IntegerValue(42)), ValueSequence.of(identity(), new StringValue("goodbye"))));
        sequence.add(array1);
        final ArrayType array2 = new ArrayType(mockContext, List.of(ValueSequence.of(identity(), new StringValue("in the beginning")), ValueSequence.of(identity(), new StringValue("but at the end"), new IntegerValue(42))));
        sequence.add(array2);

        verify(mockContext);

        assertSerialize(sequence, wrapped == Wrapped.WRAPPED, typed == Typed.TYPED);
    }

    @ParameterizedTest
    @CsvSource({"NOT_WRAPPED,NOT_TYPED", "WRAPPED,NOT_TYPED", "NOT_WRAPPED,TYPED", "WRAPPED,TYPED"})
    void serializeMap(final Wrapped wrapped, final Typed typed) throws SAXException, XPathException, IOException {
        final XQueryContext mockContext = mock(XQueryContext.class);
        expect(mockContext.nextExpressionId()).andReturn(1).anyTimes();

        replay(mockContext);

        final Sequence sequence = new ValueSequence();
        final MapType map1 = new MapType(mockContext, null, List.of(Tuple(new StringValue("key1"), ValueSequence.of(identity(), new StringValue("hello"), new IntegerValue(42))), Tuple(new StringValue("key2"), ValueSequence.of(identity(), new StringValue("goodbye")))));
        sequence.add(map1);
        final MapType map2 = new MapType(mockContext, null, List.of(Tuple(new StringValue("key3"), ValueSequence.of(identity(), new StringValue("in the beginning"), new StringValue("but at the end"), new IntegerValue(42)))));
        sequence.add(map2);

        verify(mockContext);

        assertSerialize(sequence, wrapped == Wrapped.WRAPPED, typed == Typed.TYPED);
    }

    @ParameterizedTest
    @CsvSource({"NOT_WRAPPED,NOT_TYPED", "WRAPPED,NOT_TYPED", "NOT_WRAPPED,TYPED", "WRAPPED,TYPED"})
    void serializeMixed(final Wrapped wrapped, final Typed typed) throws SAXException, XPathException, IOException {
        final MemTreeBuilder builder = new MemTreeBuilder();
        builder.startDocument();
        final int text1Id = builder.characters("hello");
        builder.comment("separator");
        final int text2Id = builder.characters("world");
        builder.endDocument();

        final Sequence sequence = new ValueSequence();
        sequence.add(new IntegerValue(123));
        sequence.add(builder.getDocument().getNode(text1Id));
        sequence.add(new StringValue("Some string"));
        sequence.add(builder.getDocument().getNode(text2Id));
        sequence.add(new StringValue("Another string"));

        assertSerialize(sequence, wrapped == Wrapped.WRAPPED, typed == Typed.TYPED);
    }

    private void assertSerialize(final Sequence sequence, final boolean wrapped, final boolean typed) throws IOException, SAXException, XPathException {
        final String expected;
        if (wrapped && typed) {
            final List<String> items = type(sequence, false);
            expected = wrap(items);
        } else if (wrapped) {
            final List<String> items = toStrings(sequence);
            expected = wrap(items);
        } else if (typed) {
            final List<String> items = type(sequence, true);
            expected = join(items, "");
        } else {
            expected = join(sequence, "");
        }

        final String serialized = serialize(sequence, wrapped, typed);

        if (wrapped) {
            assertThat(serialized, CompareMatcher.isIdenticalTo(expected));
        } else if (typed) {
            assertThat(wrapInElement(serialized), CompareMatcher.isIdenticalTo(wrapInElement(expected)));
        } else {
            assertEquals(expected, serialized);
        }
    }

    private static String join(final List<String> strings, final String separator) {
        return String.join(separator, strings);
    }

    private static String join(final Sequence sequence, final String separator) throws XPathException {
        return join(toStrings(sequence), separator);
    }

    private static List<String> toStrings(final Sequence sequence) throws XPathException {
        final List<String> strings = new ArrayList<>(sequence.getItemCount());
        for (final SequenceIterator it = sequence.iterate(); it.hasNext();) {
            final Item item = it.nextItem();
            if (item.getType() == Type.ARRAY_ITEM) {
                final StringBuilder arrayStringBuilder = new StringBuilder();
                for (final Sequence arrayItem : ((ArrayType) item).toArray()) {
                    arrayStringBuilder.append(join(toStrings(arrayItem), ""));
                }
                strings.add(arrayStringBuilder.toString());

            } else if (item.getType() == Type.MAP_ITEM) {
                final StringBuilder mapStringBuilder = new StringBuilder();
                for (final IEntry<AtomicValue, Sequence> mapEntry : (MapType) item) {
                    mapStringBuilder.append(mapEntry.key().getStringValue());
                    mapStringBuilder.append(join(toStrings(mapEntry.value()), ""));
                }
                strings.add(mapStringBuilder.toString());

        } else {
                strings.add(item.getStringValue());
            }
        }
        return strings;
    }

    private static String wrapInElement(final String string) {
        return "<stub-element>" + string + "</stub-element>";
    }

    private static List<String> type(final Sequence sequence, final boolean explicitNamespace) throws XPathException {
        final String namespace;
        if (explicitNamespace) {
            namespace = " xmlns:" + EXIST_NS_PREFIX + "=\"" + EXIST_NS + "\"";
        } else {
            namespace = "";
        }

        final List<String> typed = new ArrayList<>(sequence.getItemCount());
        for (final SequenceIterator it = sequence.iterate(); it.hasNext();) {
            final Item item = it.nextItem();

            if (item.getType() == Type.TEXT) {
                typed.add("<exist:text" + namespace + ">" + item.getStringValue() + "</exist:text>");

            } else if (item.getType() == Type.ARRAY_ITEM) {
                final StringBuilder builder = new StringBuilder();
                builder.append("<exist:array").append(namespace).append('>');
                for (final Sequence arrayItem : ((ArrayType) item).toArray()) {
                    builder.append("<exist:sequence>");
                    builder.append(join(type(arrayItem, explicitNamespace), ""));
                    builder.append("</exist:sequence>");
                }
                builder.append("</exist:array>");
                typed.add(builder.toString());

            } else if (item.getType() == Type.MAP_ITEM) {
                final StringBuilder builder = new StringBuilder();
                builder.append("<exist:map").append(namespace).append('>');
                for (final IEntry<AtomicValue, Sequence> mapEntry : (MapType) item) {
                    builder.append("<exist:entry>");
                    builder.append("<exist:key>");
                    builder.append(atomicValue(namespace, mapEntry.key()));
                    builder.append("</exist:key>");
                    builder.append("<exist:sequence>");
                    builder.append(join(type(mapEntry.value(), explicitNamespace), ""));
                    builder.append("</exist:sequence>");
                    builder.append("</exist:entry>");
                }
                builder.append("</exist:map>");
                typed.add(builder.toString());

            } else {
                typed.add(atomicValue(namespace, item));
            }
        }
        return typed;
    }

    private static String atomicValue(final String namespace, final Item item) throws XPathException {
        return "<exist:value" + namespace + " exist:type=\"" + Type.getTypeName(item.getType()) + "\">" + item.getStringValue() + "</exist:value>";
    }

    private static String wrap(final List<String> items) {
        final StringBuilder builder = new StringBuilder()
            .append("<exist:result xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" exist:hits=\"")
            .append(items.size())
            .append("\" exist:start=\"")
            .append(DEFAULT_START)
            .append("\" exist:count=\"")
            .append(items.size())
            .append("\" exist:compilation-time=\"")
            .append(DEFAULT_COMPILATION_TIME)
            .append("\" exist:execution-time=\"")
            .append(DEFAULT_EXECUTION_TIME)
            .append("\">");

        for (final String item : items) {
            builder.append(item);
        }

        return builder.append("</exist:result>").toString();
    }

    private String serialize(final Sequence sequence, final boolean wrapped, final boolean typed) throws IOException, SAXException {
        final int count = sequence.getItemCount();

        final Configuration mockConfiguration = mock(Configuration.class);
        expect(mockConfiguration.getProperty(anyString())).andReturn(null).anyTimes();
        final Capture<Object> capturePropertyDefault = newCapture();
        expect(mockConfiguration.getProperty(anyString(), capture(capturePropertyDefault))).andAnswer(() -> capturePropertyDefault.getValue());

        replay(mockConfiguration);

        final Serializer serializer = new NativeSerializer(null, mockConfiguration);
        try (final Writer writer = new StringWriter()) {
            final SAXSerializer saxSerializer = new SAXSerializer(writer, null);
            serializer.setSAXHandlers(saxSerializer, saxSerializer);
            serializer.toSAX(sequence, DEFAULT_START, count, wrapped, typed, DEFAULT_COMPILATION_TIME, DEFAULT_EXECUTION_TIME);
            return writer.toString();

        } finally {
            verify(mockConfiguration);
        }
    }
}
