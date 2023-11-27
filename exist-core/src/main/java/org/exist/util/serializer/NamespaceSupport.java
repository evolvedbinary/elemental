/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.util.serializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Inadequately tested drop-in replacement for NamespaceSupport
 *
 * Performance improved because we already assume strings are {@code intern()-ed}
 */
public class NamespaceSupport {

    private final static List<HashMap<String, String>> stack = new ArrayList<>();
    static {
        stack.add(new HashMap<>());
    }

    private final static String XML = "xml";
    private final static String XMLNS = "xmlns";

    private final static String XML_URI = "http://www.w3.org/XML/1998/namespace";
    public void reset() {
    }

    public String getURI(String prefix) {
        if (XML.equals(prefix)) {
            return XML_URI;
        }
        for (int i = stack.size() -1; i >= 0; i--) {
            final String result = stack.get(i).get(prefix);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public void pushContext() {
        stack.add(new HashMap<>());
    }

    public boolean declarePrefix(String elemPrefix, String namespaceURI) {
        if (XML.equals(elemPrefix) || XMLNS.equals(elemPrefix)) {
            return false;
        }
        stack.get(stack.size() - 1).put(elemPrefix, namespaceURI);
        return true;
    }

    public void popContext() {
        if (stack.size() > 1) {
            stack.remove(stack.size() - 1);
        }
    }
}
