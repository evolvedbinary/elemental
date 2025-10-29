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
package org.exist.xmlrpc;

import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.serializer.SerializerHandler;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.exist.xquery.value.ArrayWrapper;
import org.xml.sax.SAXException;

import static org.exist.xmlrpc.XmlRpcExtensionConstants.NS_XDM;
import static org.exist.xmlrpc.XmlRpcExtensionConstants.PREFIX_XDM;

/**
 * XML-RPC type serializer for objects of
 * {@link org.exist.xquery.value.ArrayWrapper}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
class ArrayWrapperSerializer extends TypeSerializerImpl {

    public static final String ARRAY_ELEM_NAME = "array";
    public static final String ARRAY_TAG = PREFIX_XDM + ':' + ARRAY_ELEM_NAME;
    public static final String DATA_TAG = "data";

    private final TypeFactory typeFactory;
    private final XmlRpcStreamConfig config;

    ArrayWrapperSerializer(final TypeFactory typeFactory, final XmlRpcStreamConfig config) {
        this.typeFactory = typeFactory;
        this.config = config;
    }

    private void writeObject(final SerializerHandler pHandler, final Object pObject) throws SAXException {
        final TypeSerializer ts = typeFactory.getSerializer(config, pObject);
        if (ts == null) {
            throw new SAXException("Unsupported Java type: " + pObject.getClass().getName());
        }
        ts.write(pHandler, pObject);
    }

    private void writeData(final SerializerHandler pHandler, final Object pObject) throws SAXException {
        final Object[] data = ((ArrayWrapper) pObject).array;
        for (int i = 0;  i < data.length;  i++) {
            writeObject(pHandler, data[i]);
        }
    }

    @Override
    public void write(final SerializerHandler pHandler, final Object pObject) throws SAXException {
        pHandler.startElement("", VALUE_TAG, VALUE_TAG, ZERO_ATTRIBUTES);
        pHandler.startPrefixMapping(PREFIX_XDM, NS_XDM);
        pHandler.startElement(NS_XDM, ARRAY_ELEM_NAME, ARRAY_TAG, ZERO_ATTRIBUTES);
        pHandler.startElement("", DATA_TAG, DATA_TAG, ZERO_ATTRIBUTES);
        writeData(pHandler, pObject);
        pHandler.endElement("", DATA_TAG, DATA_TAG);
        pHandler.endElement(NS_XDM, ARRAY_ELEM_NAME, ARRAY_TAG);
        pHandler.endPrefixMapping(PREFIX_XDM);
        pHandler.endElement("", VALUE_TAG, VALUE_TAG);
    }
}
