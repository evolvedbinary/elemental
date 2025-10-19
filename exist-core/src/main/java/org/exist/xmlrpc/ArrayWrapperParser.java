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

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactory;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.RecursiveTypeParserImpl;
import org.exist.xquery.value.ArrayWrapper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static org.apache.xmlrpc.serializer.TypeSerializerImpl.VALUE_TAG;
import static org.exist.xmlrpc.ArrayWrapperSerializer.*;
import static org.exist.xmlrpc.XmlRpcExtensionConstants.NS_XDM;

/**
 * XML-RPC type parser for objects of
 * {@link org.exist.xquery.value.ArrayWrapper}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
class ArrayWrapperParser extends RecursiveTypeParserImpl {

    private int level = 0;
    private List<Object> list;

    ArrayWrapperParser(final XmlRpcStreamConfig config, final NamespaceContextImpl context, final TypeFactory factory) {
        super(config, context, factory);
    }

    /**
     * Return true if this parser can parse an Element with the given namespace and name.
     *
     * @param namespaceUri the URI of the Element's namespace.
     * @param localName the local name of the Element.
     *
     * @return true if this parser can parse the Element, false otherwise.
     */
    public static boolean canParseElement(final String namespaceUri, final String localName) {
        return NS_XDM.equals(namespaceUri) && ARRAY_ELEM_NAME.equals(localName);
    }

    @Override
    public void startDocument() throws SAXException {
        this.level = 0;
        this.list = new ArrayList<>();
        super.startDocument();
    }

    @Override
    protected void addResult(final Object pValue) {
        list.add(pValue);
    }

    @Override
    public void endElement(final String pURI, final String pLocalName, final String pQName) throws SAXException {
        switch (--level) {
            case 0:
                setResult(new ArrayWrapper<>(list.toArray()));
                break;
            case 1:
                break;
            case 2:
                endValueTag();
                break;
            default:
                super.endElement(pURI, pLocalName, pQName);
        }
    }

    @Override
    public void startElement(final String pURI, final String pLocalName, final String pQName, final Attributes pAttrs) throws SAXException {
        switch (level++) {
            case 0:
                if (!NS_XDM.equals(pURI) || !ARRAY_ELEM_NAME.equals(pLocalName)) {
                    throw new SAXParseException("Expected " + ARRAY_TAG + " element, got " + new QName(pURI, pLocalName), getDocumentLocator());
                }
                break;
            case 1:
                if (!XMLConstants.NULL_NS_URI.equals(pURI) || !DATA_TAG.equals(pLocalName)) {
                    throw new SAXParseException("Expected data element, got "
                        + new QName(pURI, pLocalName),
                        getDocumentLocator());
                }
                break;
            case 2:
                if (!XMLConstants.NULL_NS_URI.equals(pURI) || !VALUE_TAG.equals(pLocalName)) {
                    throw new SAXParseException("Expected data element, got "
                        + new QName(pURI, pLocalName),
                        getDocumentLocator());
                }
                startValueTag();
                break;
            default:
                super.startElement(pURI, pLocalName, pQName, pAttrs);
                break;
        }
    }
}
