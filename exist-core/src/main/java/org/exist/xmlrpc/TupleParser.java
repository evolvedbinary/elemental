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

import com.evolvedbinary.j8fu.tuple.*;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.RecursiveTypeParserImpl;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * XML-RPC type parser for sub-classes of
 * {@link com.evolvedbinary.j8fu.tuple.Tuple}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
class TupleParser extends RecursiveTypeParserImpl {
    private int level = 0;
    private List<Object> list;

    /**
     * Creates a new instance.
     *
     * @param context The namespace context.
     * @param config The request or response configuration.
     * @param factory The type factory.
     */
    TupleParser(final XmlRpcStreamConfig config, final NamespaceContextImpl context, final TypeFactory factory) {
        super(config, context, factory);
    }

    @Override
    public void startDocument() throws SAXException {
        level = 0;
        list = new ArrayList<>();
        super.startDocument();
    }

    @Override
    protected void addResult(final Object value) {
        list.add(value);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qname) throws SAXException {
        switch (--level) {
            case 0:
                setResult(toTuple(list));
                break;
            case 1:
                break;
            case 2:
                endValueTag();
                break;
            default:
                super.endElement(uri, localName, qname);
        }
    }

    @Override
    public void startElement(final String uri, final String localName, final String qname, final Attributes attrs) throws SAXException {
        switch (level++) {
            case 0:
                if (!XMLConstants.NULL_NS_URI.equals(uri) || !TupleSerializer.TUPLE_TAG.equals(localName)) {
                    throw new SAXParseException("Expected tuple element, got "
                            + new QName(uri, localName),
                            getDocumentLocator());
                }
                break;
            case 1:
                if (!XMLConstants.NULL_NS_URI.equals(uri) || !TupleSerializer.DATA_TAG.equals(localName)) {
                    throw new SAXParseException("Expected data element, got "
                            + new QName(uri, localName),
                            getDocumentLocator());
                }
                break;
            case 2:
                if (!XMLConstants.NULL_NS_URI.equals(uri) || !TypeSerializerImpl.VALUE_TAG.equals(localName)) {
                    throw new SAXParseException("Expected value element, got "
                            + new QName(uri, localName),
                            getDocumentLocator());
                }
                startValueTag();
                break;
            default:
                super.startElement(uri, localName, qname, attrs);
                break;
        }
    }

    private Tuple toTuple(final List<Object> list) throws SAXException {
        switch (list.size()) {
            case 2:
                return new Tuple2<>(list.get(0), list.get(1));
            case 3:
                return new Tuple3<>(list.get(0), list.get(1), list.get(2));
            case 4:
                return new Tuple4<>(list.get(0), list.get(1), list.get(2), list.get(3));
            case 5:
                return new Tuple5<>(list.get(0), list.get(1), list.get(2), list.get(3), list.get(4));
            case 6:
                return new Tuple6<>(list.get(0), list.get(1), list.get(2), list.get(3), list.get(4), list.get(5));
            default:
                throw new SAXException("Unsupported Tuple arity: " + list.size());
        }
    }
}
