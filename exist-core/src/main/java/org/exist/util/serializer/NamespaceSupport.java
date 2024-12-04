/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.util.serializer;

import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;

/**
 * Inadequately tested drop-in replacement for NamespaceSupport
 *
 * Performance improved because we already assume strings are {@code intern()-ed}
 * and therefore we use a HashMap which compares the keys based on identity.
 */
public class NamespaceSupport /* implements NamespaceContext */ {

    @Nullable private Deque<Map<String, String>> stack = null;

//    @Override
    public void pushContext() {
        if (stack == null) {
            stack = new ArrayDeque<>();
        }
        stack.addFirst(new IdentityHashMap<>());
    }

//    @Override
    public void popContext() {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException("No namespace context");
        }
        stack.removeFirst();
    }

//    @Override
    public @Nullable String getURI(String prefix) {
        if (XML_NS_PREFIX.equals(prefix)) {
            return XML_NS_URI;
        }

        if (XMLNS_ATTRIBUTE.equals(prefix)) {
            return XMLNS_ATTRIBUTE_NS_URI;
        }

        @Nullable String uri = null;
        if (stack != null) {

            prefix = prefix.intern();  // TODO(AR) if we can be 100% sure that all `prefix` are already interned then we could remove this...

            for (final Map<String, String> context : stack) {
                uri = context.get(prefix);
                if (uri != null) {
                    break;
                }
            }
        }

        return uri;
    }


//    @Override
    public boolean declarePrefix(@Nullable String prefix, final String uri) {
        if (XML_NS_PREFIX.equals(prefix) || XMLNS_ATTRIBUTE.equals(prefix)) {
            return false;
        }

        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException("No namespace context");
        }

        if (prefix == null) {
            prefix = "";
        } else {
            prefix = prefix.intern();  // TODO(AR) if we can be 100% sure that all `prefix` are already interned then we could remove this...
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
