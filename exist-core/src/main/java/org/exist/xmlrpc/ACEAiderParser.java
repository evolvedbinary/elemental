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

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.RecursiveTypeParserImpl;
import org.exist.security.ACLPermission.ACE_ACCESS_TYPE;
import org.exist.security.ACLPermission.ACE_TARGET;
import org.exist.security.internal.aider.ACEAider;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * XML-RPC type parser for objects of
 * {@link org.exist.security.internal.aider.ACEAider}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
class ACEAiderParser extends RecursiveTypeParserImpl {
    private int level = 0;
    private List<Object> list;

    /**
     * Creates a new instance.
     *
     * @param context The namespace context.
     * @param config  The request or response configuration.
     * @param factory The type factory.
     */
    ACEAiderParser(final XmlRpcStreamConfig config, final NamespaceContextImpl context, final TypeFactory factory) {
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
                setResult(toAceAider(list));
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
                if (!XMLConstants.NULL_NS_URI.equals(uri) || !ACEAiderSerializer.ACEAIDER_TAG.equals(localName)) {
                    throw new SAXParseException("Expected aceAider element, got "
                            + new QName(uri, localName),
                            getDocumentLocator());
                }
                break;
            case 1:
                if (!XMLConstants.NULL_NS_URI.equals(uri) || !ACEAiderSerializer.DATA_TAG.equals(localName)) {
                    throw new SAXParseException("Expected data element, got "
                            + new QName(uri, localName),
                            getDocumentLocator());
                }
                break;
            case 2:
                if (!XMLConstants.NULL_NS_URI.equals(uri) || !ACEAiderSerializer.VALUE_TAG.equals(localName)) {
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

    private static ACEAider toAceAider(final List<Object> list) throws SAXException {
        if (list.size() != 4) {
            throw new SAXException("Inavlis list size for ACEAider");
        }

        Object object = list.get(0);
        final ACE_ACCESS_TYPE aceAccessType;
        if (object instanceof String) {
            try {
                aceAccessType = ACE_ACCESS_TYPE.valueOf((String) object);
            } catch (final IllegalArgumentException e) {
                throw new SAXException(e);
            }
        } else {
            throw new SAXException("Expected ACE_ACCESS_TYPE");
        }

        object = list.get(1);
        final ACE_TARGET aceTarget;
        if (object instanceof String) {
            try {
                aceTarget = ACE_TARGET.valueOf((String) object);
            } catch (final IllegalArgumentException e) {
                throw new SAXException(e);
            }
        } else {
            throw new SAXException("Expected ACE_TARGET");
        }

        object = list.get(2);
        final String aceWho;
        if (object instanceof String) {
            aceWho = (String) object;
        } else {
            throw new SAXException("Expected String");
        }

        object = list.get(3);
        final int aceMode;
        if (object instanceof Integer) {
            aceMode = (Integer) object;
        } else {
            throw new SAXException("Expected Integer");
        }

        return new ACEAider(aceAccessType, aceTarget, aceWho, aceMode);
    }
}
