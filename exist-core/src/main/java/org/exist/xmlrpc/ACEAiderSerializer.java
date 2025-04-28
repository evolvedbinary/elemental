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
package org.exist.xmlrpc;

import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.exist.security.internal.aider.ACEAider;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * XML-RPC type serializer for objects of
 * {@link org.exist.security.internal.aider.ACEAider}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
class ACEAiderSerializer extends TypeSerializerImpl {
    static final String ACEAIDER_TAG = "aceAider";
    static final String DATA_TAG = "data";

    private final TypeFactory typeFactory;
    private final XmlRpcStreamConfig config;

    ACEAiderSerializer(final TypeFactory typeFactory, final XmlRpcStreamConfig config) {
        this.typeFactory = typeFactory;
        this.config = config;
    }

    private void writeObject(final ContentHandler handler, final Object object) throws SAXException {
        final TypeSerializer ts = typeFactory.getSerializer(config, object);
        if (ts == null) {
            throw new SAXException("Unsupported Java type: " + object.getClass().getName());
        }
        ts.write(handler, object);
    }

    private void writeData(final ContentHandler handler, final Object object) throws SAXException {
        final ACEAider aceAider = (ACEAider) object;
        writeObject(handler, aceAider.getAccessType().name());
        writeObject(handler, aceAider.getTarget().name());
        writeObject(handler, aceAider.getWho());
        writeObject(handler, aceAider.getMode());
    }

    @Override
    public void write(final ContentHandler pHandler, final Object pObject) throws SAXException {
        pHandler.startElement("", VALUE_TAG, VALUE_TAG, ZERO_ATTRIBUTES);
        pHandler.startElement("", ACEAIDER_TAG, ACEAIDER_TAG, ZERO_ATTRIBUTES);
        pHandler.startElement("", DATA_TAG, DATA_TAG, ZERO_ATTRIBUTES);
        writeData(pHandler, pObject);
        pHandler.endElement("", DATA_TAG, DATA_TAG);
        pHandler.endElement("", ACEAIDER_TAG, ACEAIDER_TAG);
        pHandler.endElement("", VALUE_TAG, VALUE_TAG);
    }
}
