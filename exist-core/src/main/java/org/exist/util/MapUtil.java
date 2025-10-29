/*
 * Copyright (C) 2014, Evolved Binary Ltd
 *
 * This file was originally ported from FusionDB to eXist-db by
 * Evolved Binary, for the benefit of the eXist-db Open Source community.
 * Only the ported code as it appears in this file, at the time that
 * it was contributed to eXist-db, was re-licensed under The GNU
 * Lesser General Public License v2.1 only for use in eXist-db.
 *
 * This license grant applies only to a snapshot of the code as it
 * appeared when ported, it does not offer or infer any rights to either
 * updates of this source code or access to the original source code.
 *
 * The GNU Lesser General Public License v2.1 only license follows.
 *
 * =====================================================================
 *
 * Copyright (C) 2014, Evolved Binary Ltd
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
package org.exist.util;

import com.evolvedbinary.j8fu.tuple.Tuple2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple utility functions for working with Java Maps.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface MapUtil {

    /**
     * Create a Hash Map from a List of Tuples.
     *
     * @param <K> the type of the keys in the map.
     * @param <V> the types of the values in the map.
     *
     * @param entries the entries for the map.
     *
     * @return The HashMap
     */
    static <K, V> Map<K,V> HashMap(final Tuple2<K, V>... entries) {
        return HashMap(Math.max(entries.length, 16), entries);
    }

    /**
     * Create a Hash Map from a List of Tuples.
     *
     * @param <K> the type of the keys in the map.
     * @param <V> the types of the values in the map.
     *
     * @param initialCapacity allows you to oversize the map if you plan to add more entries.
     * @param entries the entries for the map.
     *
     * @return The HashMap
     */
    static <K, V> Map<K,V> HashMap(final int initialCapacity, final Tuple2<K, V>... entries) {
        final Map<K, V> map = new HashMap<>(initialCapacity);
        for (final Tuple2<K, V> entry : entries) {
            map.put(entry._1, entry._2);
        }
        return map;
    }

    static Map<Object, Object> parseXdmMapStringToJavaMap(final String xdmMapString) {
        final XdmMapStringLexer lexer = new XdmMapStringLexer(xdmMapString);
        final XdmMapStringParser parser = new XdmMapStringParser(lexer);
        return parser.parseMap();
    }

    class XdmMapStringLexer {
        private static final Pattern STRING_LITERAL = Pattern.compile("\"([^\"]*)\"");
        private static final Pattern LITERAL = Pattern.compile("[^\\s\",:{}()]+");
        private static final Pattern WHITESPACE = Pattern.compile("\\s*");

        private final Matcher stringLiteralMatcher = STRING_LITERAL.matcher("");
        private final Matcher literalMatcher = LITERAL.matcher("");
        private final Matcher whitespaceMatcher = WHITESPACE.matcher("");

        private final String input;
        private int pos = 0;

        public XdmMapStringLexer(final String input) {
            this.input = input;
        }

        public XdmMapStringToken nextToken() {
            skipWhitespace();
            if (pos >= input.length()) {
                return new XdmMapStringToken(XdmMapStringTokenType.EOF, "");
            }

            final char ch = input.charAt(pos);
            switch (ch) {
                case '{':
                    pos++;
                    return new XdmMapStringToken(XdmMapStringTokenType.LBRACE, "{");

                case '}':
                    pos++;
                    return new XdmMapStringToken(XdmMapStringTokenType.RBRACE, "}");

                case ':':
                    pos++;
                    return new XdmMapStringToken(XdmMapStringTokenType.COLON, ":");

                case ',':
                    pos++;
                    return new XdmMapStringToken(XdmMapStringTokenType.COMMA, ",");

                case '(':
                    pos++;
                    return new XdmMapStringToken(XdmMapStringTokenType.LPAREN, "(");

                case ')':
                    pos++;
                    return new XdmMapStringToken(XdmMapStringTokenType.RPAREN, ")");

                case '"':
                    stringLiteralMatcher.reset(input.substring(pos));
                    if (stringLiteralMatcher.lookingAt()) {
                        final String str = stringLiteralMatcher.group(1);
                        pos += stringLiteralMatcher.end();
                        return new XdmMapStringToken(XdmMapStringTokenType.STRING, str);
                    }
                    throw new IllegalArgumentException("Invalid string literal at position " + pos);

                default:
                    if (input.startsWith("map", pos)) {
                        pos += 3;
                        return new XdmMapStringToken(XdmMapStringTokenType.MAP, "map");
                    }

                    literalMatcher.reset(input.substring(pos));
                    if (literalMatcher.lookingAt()) {
                        final String lit = literalMatcher.group();
                        pos += literalMatcher.end();
                        return new XdmMapStringToken(XdmMapStringTokenType.LITERAL, lit);
                    }

                    throw new IllegalArgumentException("Unexpected character at position " + pos);
            }
        }

        private void skipWhitespace() {
            whitespaceMatcher.reset(input.substring(pos));
            if (whitespaceMatcher.lookingAt()) {
                pos += whitespaceMatcher.end();
            }
        }
    }

    class XdmMapStringParser {
        private final XdmMapStringLexer lexer;
        private XdmMapStringToken current;

        public XdmMapStringParser(final XdmMapStringLexer lexer) {
            this.lexer = lexer;
            this.current = lexer.nextToken();
        }

        private void consume(final XdmMapStringTokenType expected) {
            if (current.type != expected) {
                throw new IllegalArgumentException("Expected " + expected + " but found " + current.type);
            }
            current = lexer.nextToken();
        }

        public Map<Object, Object> parseMap() {
            consume(XdmMapStringTokenType.MAP);
            consume(XdmMapStringTokenType.LBRACE);
            final Map<Object, Object> map = new LinkedHashMap<>();
            mapEntry(map);
            while (current.type == XdmMapStringTokenType.COMMA) {
                consume(XdmMapStringTokenType.COMMA);
                mapEntry(map);
            }
            consume(XdmMapStringTokenType.RBRACE);
            return map;
        }

        private void mapEntry(final Map<Object, Object> map) {
            final String key = mapEntryKey();
            consume(XdmMapStringTokenType.COLON);
            final Object value = mapEntryValue();
            map.put(key, value);
        }

        private String mapEntryKey() {
            if (current.type == XdmMapStringTokenType.STRING || current.type == XdmMapStringTokenType.LITERAL) {
                final String val = current.value;
                consume(current.type);
                return val;
            } else {
                throw new IllegalArgumentException("Expected STRING or LITERAL for key but got " + current.type);
            }
        }

        private Object mapEntryValue() {
            if (current.type == XdmMapStringTokenType.LPAREN) {
                return mapEntryValueSequence();
            } else {
                return mapEntryValueItem();
            }
        }

        private Object mapEntryValueItem() {
            if (current.type == XdmMapStringTokenType.STRING) {
                final String val = current.value;
                consume(current.type);
                return val;

            } else if (current.type == XdmMapStringTokenType.LITERAL) {
                // NOTE(AR) at the moment we only support Integer literals
                final int val = Integer.parseInt(current.value);
                consume(current.type);
                return val;

            } else {
                throw new IllegalArgumentException("Expected STRING or LITERAL for value item but got " + current.type);
            }
        }

        private List<Object> mapEntryValueSequence() {
            consume(XdmMapStringTokenType.LPAREN);
            final List<Object> list = new ArrayList<>();
            list.add(mapEntryValueItem());
            while (current.type == XdmMapStringTokenType.COMMA) {
                consume(XdmMapStringTokenType.COMMA);
                list.add(mapEntryValueItem());
            }
            consume(XdmMapStringTokenType.RPAREN);
            return list;
        }
    }

    class XdmMapStringToken {
        public final XdmMapStringTokenType type;
        public final String value;

        public XdmMapStringToken(final XdmMapStringTokenType type, final String value) {
            this.type = type;
            this.value = value;
        }
    }

    enum XdmMapStringTokenType {
        MAP, LBRACE, RBRACE, LPAREN, RPAREN, COLON, COMMA, STRING, LITERAL, EOF
    }
}
