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
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
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

import java.util.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.exist.dom.QName;
import org.exist.dom.QName.IllegalQNameException;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;

/**
 * General purpose class to stream a DOM node to SAX.
 *
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
public class DOMStreamer {

    private static final Logger LOG = LogManager.getLogger(DOMStreamer.class);

    private ContentHandler contentHandler = null;
    private LexicalHandler lexicalHandler = null;
    private final NamespaceSupport nsSupport = new NamespaceSupport();
    private final Map<String, String> namespaceDecls = new HashMap<>();
    private final Deque<ElementInfo> stack = new ArrayDeque<>();

    public DOMStreamer() {
    }

    public DOMStreamer(final ContentHandler contentHandler, final LexicalHandler lexicalHandler) {
        this.contentHandler = contentHandler;
        this.lexicalHandler = lexicalHandler;
    }

    public void setContentHandler(final ContentHandler handler) {
        contentHandler = handler;
    }

    public void setLexicalHandler(final LexicalHandler handler) {
        lexicalHandler = handler;
    }

    /**
     * Reset internal state for reuse. Registered handlers will be set
     * to null.
     */
    public void reset() {
        nsSupport.reset();
        namespaceDecls.clear();
        stack.clear();
        contentHandler = null;
        lexicalHandler = null;
    }

    /**
     * Serialize the given node and all its descendants to SAX.
     *
     * @param node the node to serialize
     * @throws SAXException if an error occurs during serialization.
     */
    public void serialize(final Node node) throws SAXException {
        serialize(node, false);
    }

    /**
     * Serialize the given node and all its descendants to SAX. If
     * callDocumentEvents is set to false, startDocument/endDocument
     * events will not be fired.
     *
     * @param node the node to serialize
     * @param callDocumentEvents whether we shoiuld call the document events startDocument/endDocument
     * @throws SAXException if an error occurs during serialization.
     */
    public void serialize(Node node, final boolean callDocumentEvents) throws SAXException {
        if (callDocumentEvents) {
            contentHandler.startDocument();
        }
        final Node top = node;
        while (node != null) {
            startNode(node);
            Node nextNode = node.getFirstChild();
            while (nextNode == null) {
                endNode(node);
                if (top != null && top.equals(node)) {
                    break;
                }
                nextNode = node.getNextSibling();
                if (nextNode == null) {
                    node = node.getParentNode();
                    if (node == null || (top != null && top.equals(node))) {
                        endNode(node);
                        //nextNode = null;
                        break;
                    }
                }
            }
            node = nextNode;
        }
        if (callDocumentEvents) {
            contentHandler.endDocument();
        }
    }

    protected void startNode(final Node node) throws SAXException {
        String cdata;
        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE:
            case Node.DOCUMENT_FRAGMENT_NODE:
                break;
            case Node.ELEMENT_NODE:
                namespaceDecls.clear();
                nsSupport.pushContext();
                @Nullable String uri = node.getNamespaceURI();
                @Nullable String prefix = node.getPrefix();
                if (uri == null) {
                    uri = XMLConstants.NULL_NS_URI;
                }
                if (prefix == null) {
                    prefix = XMLConstants.DEFAULT_NS_PREFIX;
                }
                if ((!(XMLConstants.NULL_NS_URI.equals(uri) && XMLConstants.DEFAULT_NS_PREFIX.equals(prefix))) && nsSupport.getURI(prefix) == null) {
                    namespaceDecls.put(prefix, uri);
                    nsSupport.declarePrefix(prefix, uri);
                }
                // check attributes for required namespace declarations
                final NamedNodeMap attrs = node.getAttributes();
                Attr nextAttr;
                String attrName;
                for (int i = 0; i < attrs.getLength(); i++) {
                    nextAttr = (Attr) attrs.item(i);
                    attrName = nextAttr.getName();
                    if (XMLConstants.XMLNS_ATTRIBUTE.equals(attrName)) {
                        if (nsSupport.getURI(XMLConstants.NULL_NS_URI) == null) {
                            uri = nextAttr.getValue();
                            namespaceDecls.put(XMLConstants.DEFAULT_NS_PREFIX, uri);
                            nsSupport.declarePrefix(XMLConstants.DEFAULT_NS_PREFIX, uri);
                        }
                    } else if (attrName.startsWith(XMLConstants.XMLNS_ATTRIBUTE + ":")) {
                        prefix = attrName.substring(6);
                        if (nsSupport.getURI(prefix) == null) {
                            uri = nextAttr.getValue();
                            namespaceDecls.put(prefix, uri);
                            nsSupport.declarePrefix(prefix, uri);
                        }
                    } else if (attrName.indexOf(':') > 0) {
                        prefix = nextAttr.getPrefix();
                        if (prefix == null) {
                            prefix = XMLConstants.DEFAULT_NS_PREFIX;
                        }
                        uri = nextAttr.getNamespaceURI();
                        if (nsSupport.getURI(prefix) == null) {
                            namespaceDecls.put(prefix, uri);
                            nsSupport.declarePrefix(prefix, uri);
                        }
                    }
                }
                final ElementInfo info = new ElementInfo(node);
                String[] declaredPrefixes = null;
                if (!namespaceDecls.isEmpty()) {
                    declaredPrefixes = new String[namespaceDecls.size()];
                }
                // output all namespace declarations
                Map.Entry<String, String> nsEntry;
                int j = 0;
                for (final Iterator<Map.Entry<String, String>> i = namespaceDecls.entrySet().iterator(); i.hasNext(); j++) {
                    nsEntry = i.next();
                    declaredPrefixes[j] = nsEntry.getKey();
                    contentHandler.startPrefixMapping(declaredPrefixes[j], nsEntry.getValue());
                }
                info.prefixes = declaredPrefixes;
                stack.push(info);
                // output attributes
                final AttributesImpl saxAttrs = new AttributesImpl();
                String attrNS;
                String attrLocalName;
                for (int i = 0; i < attrs.getLength(); i++) {
                    nextAttr = (Attr) attrs.item(i);
                    attrNS = nextAttr.getNamespaceURI();
                    if (attrNS == null) {
                        attrNS = XMLConstants.NULL_NS_URI;
                    }
                    attrLocalName = nextAttr.getLocalName();
                    if (attrLocalName == null) {
                        try {
                            attrLocalName = QName.extractLocalName(nextAttr.getNodeName());
                        } catch (final IllegalQNameException e) {
                            throw new SAXException(e);
                        }
                    }
                    saxAttrs.addAttribute(
                            attrNS,
                            attrLocalName,
                            nextAttr.getNodeName(),
                            "CDATA",
                            nextAttr.getValue()
                    );
                }
                String localName = node.getLocalName();
                if (localName == null) {
                    try {
                        localName = QName.extractLocalName(node.getNodeName());
                    } catch (final IllegalQNameException e) {
                        throw new SAXException(e);
                    }
                }
                String namespaceURI = node.getNamespaceURI();
                if (namespaceURI == null) {
                    namespaceURI = XMLConstants.NULL_NS_URI;
                }
                contentHandler.startElement(namespaceURI, localName,
                        node.getNodeName(), saxAttrs);
                break;
            case Node.TEXT_NODE:
                cdata = ((CharacterData) node).getData();
                contentHandler.characters(cdata.toCharArray(), 0, cdata.length());
                break;
            case Node.CDATA_SECTION_NODE:
                cdata = ((CharacterData) node).getData();
                if (lexicalHandler != null) {
                    lexicalHandler.startCDATA();
                }
                contentHandler.characters(cdata.toCharArray(), 0, cdata.length());
                if (lexicalHandler != null) {
                    lexicalHandler.endCDATA();
                }
                break;
            case Node.ATTRIBUTE_NODE:
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                contentHandler.processingInstruction(
                        ((ProcessingInstruction) node).getTarget(),
                        ((ProcessingInstruction) node).getData());
                break;
            case Node.COMMENT_NODE:
                if (lexicalHandler != null) {
                    cdata = ((Comment) node).getData();
                    lexicalHandler.comment(cdata.toCharArray(), 0, cdata.length());
                }
                break;
            default:
                //TODO : what kind of default here ? -pb
                LOG.error("Unknown node type: {}", node.getNodeType());
                break;
        }
    }

    protected void endNode(final Node node) throws SAXException {
        if (node == null) {
            return;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            final ElementInfo info = stack.pop();
            nsSupport.popContext();
            String localName = node.getLocalName();
            if (localName == null) {
                try {
                    localName = QName.extractLocalName(node.getNodeName());
                } catch (final IllegalQNameException e) {
                    throw new SAXException(e);
                }
            }
            String namespaceURI = node.getNamespaceURI();
            if (namespaceURI == null) {
                namespaceURI = XMLConstants.NULL_NS_URI;
            }
            contentHandler.endElement(namespaceURI, localName, node.getNodeName());
            if (info.prefixes != null) {
                for (int i = 0; i < info.prefixes.length; i++) {
                    contentHandler.endPrefixMapping(info.prefixes[i]);
                }
            }
        }
    }

    private static class ElementInfo {
        final Node element;
        String[] prefixes = null;

        public ElementInfo(final Node element) {
            this.element = element;
        }
    }
}