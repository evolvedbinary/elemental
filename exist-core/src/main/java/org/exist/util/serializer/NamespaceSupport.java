/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.util.serializer;

import org.exist.util.Str;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;

/**
 * Inadequately tested drop-in replacement for NamespaceSupport
 */
public class NamespaceSupport /* implements NamespaceContext */ {

    @Nullable private Deque<Map<Str, String>> stack = null;

//    @Override
    public void pushContext() {
        if (stack == null) {
            stack = new ArrayDeque<>();
        }
        stack.addFirst(new HashMap<>());
    }

//    @Override
    public void popContext() {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException("No namespace context");
        }
        stack.removeFirst();
    }

//    @Override
    public @Nullable String getURI(Str prefix) {
        if (Str.XMLConstants.XML_NS_PREFIX.equals(prefix)) {
            return XML_NS_URI;
        }

        if (Str.XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
            return XMLNS_ATTRIBUTE_NS_URI;
        }

        @Nullable String uri = null;
        if (stack != null) {

            for (final Map<Str, String> context : stack) {
                uri = context.get(prefix);
                if (uri != null) {
                    break;
                }
            }
        }

        return uri;
    }


//    @Override
    public boolean declarePrefix(@Nullable Str prefix, final String uri) {
        if (Str.XMLConstants.XML_NS_PREFIX.equals(prefix) || Str.XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
            return false;
        }

        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException("No namespace context");
        }

        if (prefix == null) {
            prefix = Str.EMPTY;
        }

        stack.peekFirst().put(prefix, uri);

        return true;
    }

//    @Override
    public void reset() {
        if (stack != null) {
            stack.clear();
        }
    }
}
