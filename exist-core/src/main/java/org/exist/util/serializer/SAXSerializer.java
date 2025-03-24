/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 *
 * NOTE: Parts of this file contain code from The eXist-db Authors.
 *       The original license header is included below.
 *
 * ----------------------------------------------------------------------------
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
package org.exist.util.serializer;

import org.exist.Namespaces;
import org.exist.dom.QName;
import org.exist.dom.persistent.NodeHandle;
import org.exist.storage.serializers.EXistOutputKeys;
import org.exist.util.Str;
import org.exist.util.StringUtil;
import org.exist.util.XMLString;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.transform.TransformerException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SAXSerializer extends AbstractSerializer implements ContentHandler, LexicalHandler, Receiver<NodeHandle> {

    private final NamespaceSupport nsSupport = new NamespaceSupport();
    private final Map<Str, String> namespaceDecls = new HashMap<>();
    private final Map<Str, String> optionalNamespaceDecls = new HashMap<>();
    private boolean enforceXHTML = false;

    public SAXSerializer() {
        super();
    }

    public SAXSerializer(final Writer writer, final Properties outputProperties) {
        super();
        setOutput(writer, outputProperties);
    }

    @Override
    public final void setOutput(final Writer writer, final Properties properties) {
        super.setOutput(writer, properties);

        // if set, enforce XHTML namespace on elements with no namespace
        final String xhtml = outputProperties.getProperty(EXistOutputKeys.ENFORCE_XHTML, "no");
        enforceXHTML = xhtml.equalsIgnoreCase("yes");
    }

    public Writer getWriter() {
        return receiver.getWriter();
    }

    public void setReceiver(final SerializerWriter receiver) {
        this.receiver = receiver;
    }

    @Override
    public void reset() {
        super.reset();
        nsSupport.reset();
        namespaceDecls.clear();
        optionalNamespaceDecls.clear();
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
        //Nothing to do ?
    }

    @Override
    public void startDocument() throws SAXException {
        try {
            receiver.startDocument();
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            receiver.endDocument();
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void declaration(@Nullable final String version, @Nullable final String encoding, @Nullable final String standalone) throws SAXException {
        try {
            receiver.declaration(version, encoding, standalone);
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String namespaceURI) throws SAXException {
        if (namespaceURI.equals(Namespaces.XML_NS)) {
            return;
        }
        Str pfx;
        if(prefix == null) {
            pfx = Str.XMLConstants.DEFAULT_NS_PREFIX;
        } else {
            pfx = Str.of(prefix);
        }
        final String ns = nsSupport.getURI(pfx);
        if (enforceXHTML && !Namespaces.XHTML_NS.equals(namespaceURI)) {
            namespaceURI = Namespaces.XHTML_NS;
        }
        if(ns == null || (!ns.equals(namespaceURI))) {
            optionalNamespaceDecls.put(pfx, namespaceURI);
        }
    }

    @Override
    public void endPrefixMapping(final String prefix) throws SAXException {
        optionalNamespaceDecls.remove(Str.of(prefix));
    }

    @Override
    public void startElement(String namespaceURI, final String localName, String qname, final Attributes attribs) throws SAXException {
        try {
            namespaceDecls.clear();
            nsSupport.pushContext();
            // calculate namespaces
            final Str elemPrefix = getQNamePrefix(qname);
            if (namespaceURI == null) {
                namespaceURI = XMLConstants.NULL_NS_URI;
            }
            if (enforceXHTML && elemPrefix.isEmpty() && namespaceURI.isEmpty()) {
                namespaceURI = Namespaces.XHTML_NS;
            }
            if (nsSupport.getURI(elemPrefix) == null) {
                namespaceDecls.put(elemPrefix, namespaceURI);
                nsSupport.declarePrefix(elemPrefix, namespaceURI);
            }
            // check attributes for required namespace declarations
            if (attribs != null) {
                for (int i = 0; i < attribs.getLength(); i++) {
                    final String attrName = attribs.getQName(i);
                    if (XMLConstants.XMLNS_ATTRIBUTE.equals(attrName)) {
                        if (nsSupport.getURI(Str.XMLConstants.DEFAULT_NS_PREFIX) == null) {
                            String uri = attribs.getValue(i);
                            if (enforceXHTML && !Namespaces.XHTML_NS.equals(uri)) {
                                uri = Namespaces.XHTML_NS;
                            }
                            namespaceDecls.put(Str.XMLConstants.DEFAULT_NS_PREFIX, uri);
                            nsSupport.declarePrefix(Str.XMLConstants.DEFAULT_NS_PREFIX, uri);
                        }
                    } else if (attrName.startsWith(XMLConstants.XMLNS_ATTRIBUTE + ":")) {
                        final Str attrPrefix = Str.of(attrName.substring(6));
                        if (nsSupport.getURI(attrPrefix) == null) {
                            final String uri = attribs.getValue(i);
                            namespaceDecls.put(attrPrefix, uri);
                            nsSupport.declarePrefix(attrPrefix, uri);
                        }
                    } else {
                        final int p = attrName.indexOf(':');
                        if (p > 0) {
                            final Str attrPrefix = Str.of(attrName.substring(0, p));
                            final String uri = attribs.getURI(i);
                            if (nsSupport.getURI(attrPrefix) == null) {
                                namespaceDecls.put(attrPrefix, uri);
                                nsSupport.declarePrefix(attrPrefix, uri);
                            }
                        }
                    }
                }
            }
            for (final Map.Entry<Str, String> nsEntry : optionalNamespaceDecls.entrySet()) {
                final Str prefix = nsEntry.getKey();
                final String uri = nsEntry.getValue();
                nsSupport.declarePrefix(prefix, uri);
            }

            // output the start of the element itself
            final boolean elemPrefixedNsIsDefaultNs = (!localName.equals(qname)) && StringUtil.equals(nsSupport.getURI(Str.XMLConstants.DEFAULT_NS_PREFIX), nsSupport.getURI(elemPrefix));
            if (elemPrefixedNsIsDefaultNs) {
                // NOTE(AR) remove qname namespace prefix if the prefix points to the same namespace as the default namespace - see: https://github.com/eXist-db/exist/issues/5790
                qname = localName;
            }
            receiver.startElement(namespaceURI, localName, qname);

            // output all namespace declarations
            for (final Map.Entry<Str, String> nsEntry : optionalNamespaceDecls.entrySet()) {
                final Str prefix = nsEntry.getKey();
                final String uri = nsEntry.getValue();
                if (!(elemPrefixedNsIsDefaultNs && uri.equals(namespaceURI) && elemPrefix.equals(prefix))) {
                    receiver.namespace(Str.makeString(prefix), uri);
                }
            }
            for (final Map.Entry<Str, String> nsEntry : namespaceDecls.entrySet()) {
                final Str prefix = nsEntry.getKey();
                final String uri = nsEntry.getValue();
                if(!optionalNamespaceDecls.containsKey(prefix)) {
                    if (!(elemPrefixedNsIsDefaultNs && uri.equals(namespaceURI) && elemPrefix.equals(prefix))) {
                        receiver.namespace(Str.makeString(prefix), uri);
                    }
                }
            }
            //cancels current xmlns if relevant
            if (Str.XMLConstants.DEFAULT_NS_PREFIX.equals(elemPrefix) && !namespaceURI.equals(receiver.getDefaultNamespace())) {
                receiver.namespace(XMLConstants.DEFAULT_NS_PREFIX, namespaceURI);
                nsSupport.declarePrefix(Str.XMLConstants.DEFAULT_NS_PREFIX, namespaceURI);
            }
            optionalNamespaceDecls.clear();

            // output attributes
            if(attribs != null) {
                for (int i = 0; i < attribs.getLength(); i++) {
                    if (!attribs.getQName(i).startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
                        receiver.attribute(attribs.getQName(i), attribs.getValue(i));
                    }
                }
            }
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void startElement(final QName qname, final AttrList attribs) throws SAXException {
        try {
            namespaceDecls.clear();
            nsSupport.pushContext();

            // calculate namespaces
            Str elemPrefix = qname.getPrefixStr();
            if (elemPrefix == null) {
                elemPrefix = Str.XMLConstants.DEFAULT_NS_PREFIX;
            }
            String namespaceURI = qname.getNamespaceURI();
            if(namespaceURI == null) {
                namespaceURI = XMLConstants.NULL_NS_URI;
            }
            if(enforceXHTML && elemPrefix.isEmpty() && namespaceURI.isEmpty()) {
                namespaceURI = Namespaces.XHTML_NS;
            }
            if (nsSupport.getURI(elemPrefix) == null) {
                namespaceDecls.put(elemPrefix, namespaceURI);
                nsSupport.declarePrefix(elemPrefix, namespaceURI);
            }
            // check attributes for required namespace declarations
            if (attribs != null) {
                for (int i = 0; i < attribs.getLength(); i++) {
                    final QName attrQName = attribs.getQName(i);
                    if (XMLConstants.XMLNS_ATTRIBUTE.equals(attrQName.getLocalPart())) {
                        if (nsSupport.getURI(Str.XMLConstants.DEFAULT_NS_PREFIX) == null) {
                            String uri = attribs.getValue(i);
                            if (enforceXHTML && !Namespaces.XHTML_NS.equals(uri)) {
                                uri = Namespaces.XHTML_NS;
                            }
                            namespaceDecls.put(Str.XMLConstants.DEFAULT_NS_PREFIX, uri);
                            nsSupport.declarePrefix(Str.XMLConstants.DEFAULT_NS_PREFIX, uri);
                        }
                    } else if (attrQName.getPrefix() != null && !attrQName.getPrefix().isEmpty()) {
                        final Str attrPrefix = attrQName.getPrefixStr();
                        if (nsSupport.getURI(attrPrefix) == null) {
                            final String uri = attrQName.getNamespaceURI();
                            namespaceDecls.put(attrPrefix, uri);
                            nsSupport.declarePrefix(attrPrefix, uri);
                        }
                    }
                }
            }
            for (final Map.Entry<Str, String> nsEntry : optionalNamespaceDecls.entrySet()) {
                final Str prefix = nsEntry.getKey();
                final String uri = nsEntry.getValue();
                nsSupport.declarePrefix(prefix, uri);
            }

            // output the start of the element itself
            final boolean elemPrefixedNsIsDefaultNs = StringUtil.equals(nsSupport.getURI(Str.XMLConstants.DEFAULT_NS_PREFIX), nsSupport.getURI(elemPrefix));
            if (elemPrefixedNsIsDefaultNs) {
                // NOTE(AR) remove qname namespace prefix if the prefix points to the same namespace as the default namespace - see: https://github.com/eXist-db/exist/issues/5790
                elemPrefix = Str.XMLConstants.DEFAULT_NS_PREFIX;
            }
            receiver.startElement(new QName(qname.getLocalPart(), namespaceURI, elemPrefix.toString()));

            // output all namespace declarations
            for (final Map.Entry<Str, String> nsEntry : optionalNamespaceDecls.entrySet()) {
                final Str prefix = nsEntry.getKey();
                final String uri = nsEntry.getValue();
                if (!(elemPrefixedNsIsDefaultNs && uri.equals(namespaceURI) && Str.equals(qname.getPrefixStr(), prefix))) {
                    receiver.namespace(Str.makeString(prefix), uri);
                }
            }
            for (final Map.Entry<Str, String> nsEntry : namespaceDecls.entrySet()) {
                final Str prefix = nsEntry.getKey();
                if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
                    continue;
                }
                final String uri = nsEntry.getValue();
                if(!optionalNamespaceDecls.containsKey(prefix)) {
                    if (!(elemPrefixedNsIsDefaultNs && uri.equals(namespaceURI) && Str.equals(qname.getPrefixStr(), prefix))) {
                        receiver.namespace(Str.makeString(prefix), uri);
                    }
                }
            }
            //cancels current xmlns if relevant
            if (Str.XMLConstants.DEFAULT_NS_PREFIX.equals(elemPrefix) && !namespaceURI.equals(receiver.getDefaultNamespace())) {
                receiver.namespace(XMLConstants.DEFAULT_NS_PREFIX, namespaceURI);
                nsSupport.declarePrefix(Str.XMLConstants.DEFAULT_NS_PREFIX, namespaceURI);
            }
            optionalNamespaceDecls.clear();

            // output attributes
            if(attribs != null) {
                for (int i = 0; i < attribs.getLength(); i++) {
                    if (!attribs.getQName(i).getLocalPart().startsWith(XMLConstants.XMLNS_ATTRIBUTE)) {
                        receiver.attribute(attribs.getQName(i), attribs.getValue(i));
                    }
                }
            }
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void endElement(String namespaceURI, final String localName, String qname) throws SAXException {
        try {
            final Str elemPrefix = getQNamePrefix(qname);
            if ((!localName.equals(qname)) && StringUtil.equals(nsSupport.getURI(Str.XMLConstants.DEFAULT_NS_PREFIX), nsSupport.getURI(elemPrefix))) {
                // NOTE(AR) remove qname namespace prefix if the prefix points to the same namespace as the default namespace - see: https://github.com/eXist-db/exist/issues/5790
                qname = localName;
            }

            nsSupport.popContext();

            // output the end of the element itself
            if (enforceXHTML && qname.indexOf(':') == -1 && namespaceURI.isEmpty()) {
                namespaceURI = Namespaces.XHTML_NS;
            }
            receiver.endElement(namespaceURI, localName, qname);

            receiver.setDefaultNamespace(nsSupport.getURI(Str.XMLConstants.DEFAULT_NS_PREFIX));
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void endElement(final QName qname) throws SAXException {
        try {
            Str elemPrefix = qname.getPrefixStr();
            if (elemPrefix == null) {
                elemPrefix = Str.XMLConstants.DEFAULT_NS_PREFIX;
            }

            String namespaceURI = qname.getNamespaceURI();
            if(namespaceURI == null) {
                namespaceURI = XMLConstants.NULL_NS_URI;
            }

            if (StringUtil.equals(nsSupport.getURI(Str.XMLConstants.DEFAULT_NS_PREFIX), nsSupport.getURI(elemPrefix))) {
                // NOTE(AR) remove qname namespace prefix if the prefix points to the same namespace as the default namespace - see: https://github.com/eXist-db/exist/issues/5790
                elemPrefix = Str.XMLConstants.DEFAULT_NS_PREFIX;
            }

            nsSupport.popContext();

            // output the end of the element itself
            if (enforceXHTML && elemPrefix.isEmpty() && namespaceURI.isEmpty()) {
                namespaceURI = Namespaces.XHTML_NS;
            }
            receiver.endElement(new QName(qname.getLocalPart(), namespaceURI, elemPrefix.toString()));

            receiver.setDefaultNamespace(nsSupport.getURI(Str.XMLConstants.DEFAULT_NS_PREFIX));
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    private Str getQNamePrefix(final String qname) {
        Str elemPrefix = Str.XMLConstants.DEFAULT_NS_PREFIX;
        final int p = qname.indexOf(':');
        if (p > 0) {
            elemPrefix = Str.of(qname.substring(0, p));
        }
        return elemPrefix;
    }

    @Override
    public void attribute(final QName qname, final String value) throws SAXException {
        // ignore namespace declaration attributes
        if((qname.getPrefix() != null && XMLConstants.XMLNS_ATTRIBUTE.equals(qname.getPrefix())) || XMLConstants.XMLNS_ATTRIBUTE.equals(qname.getLocalPart())) {
            return;
        }
        
        try {
            receiver.attribute(qname, value);
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int len) throws SAXException {
        try {
            receiver.characters(ch, start, len);
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void characters(final CharSequence seq) throws SAXException {
        try {
            receiver.characters(seq);
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void ignorableWhitespace(final char[] ch, final int start, final int len) throws SAXException {
        try {
            receiver.characters(ch, start, len);
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void processingInstruction(final String target, final String data) throws SAXException {
        try {
            receiver.processingInstruction(target, data);
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void cdataSection(final char[] ch, final int start, final int len) throws SAXException {
        try {
            receiver.cdataSection(ch, start, len);
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void skippedEntity(final String name) throws SAXException {
        //Nothing to do
    }

    @Override
    public void startDTD(final String name, final String publicId, final String systemId) throws SAXException {
        try {
            receiver.startDocumentType(name, publicId, systemId);
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void endDTD() throws SAXException {
        try {
            receiver.endDocumentType();
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void documentType(final String name, final String publicId, final String systemId) throws SAXException {
        try {
            receiver.documentType(name, publicId, systemId);
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void highlightText(final CharSequence seq) {
        // not supported with this receiver
    }

    @Override
    public void startEntity(final String name) throws SAXException {
        //Nothing to do
    }

    @Override
    public void endEntity(final String name) throws SAXException {
        //Nothing to do
    }

    @Override
    public void startCDATA() throws SAXException {
        try {
            receiver.startCdataSection();
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void endCDATA() throws SAXException {
        try {
            receiver.endCdataSection();
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void comment(final char[] ch, final int start, final int len) throws SAXException {
        try {
            receiver.comment(new XMLString(ch, start, len));
        } catch (final TransformerException e) {
            throw new SAXException(e.getMessage(), e);
        }
    }

    @Override
    public void setCurrentNode(final NodeHandle node) {
        // just ignore.
    }

    @Override
    public Document getDocument() {
        //just ignore.
        return null;
    }
}
