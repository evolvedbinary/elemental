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
package org.exist.xmldb;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.Namespaces;
import org.exist.dom.persistent.DocumentTypeImpl;
import org.exist.storage.serializers.EXistOutputKeys;
import org.exist.storage.serializers.Serializer;
import org.exist.util.ExistSAXParserFactory;
import org.exist.util.io.ContentFile;
import org.exist.util.io.TemporaryFileManager;
import org.exist.util.io.VirtualTempPath;
import org.exist.util.serializer.DOMSerializer;
import org.exist.util.serializer.DOMStreamer;
import org.exist.util.serializer.SAXSerializer;
import org.exist.util.serializer.SerializerPool;
import org.exist.xqj.Marshaller;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xmldb.api.base.ErrorCodes;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;
import xyz.elemental.mediatype.MediaType;

public class RemoteXMLResource
        extends AbstractRemoteResource
        implements XMLResource {

    protected final static Logger LOG = LogManager.getLogger(RemoteXMLResource.class);

    /**
     * Use external XMLReader to parse XML.
     */
    private XMLReader xmlReader = null;

    private final Optional<String> id;
    private final int handle;
    private int pos = -1;
    private String content = null;
    private Node root = null;

    private Properties outputProperties = null;
    private LexicalHandler lexicalHandler = null;

    /**
     * Construct a remote XML Resource.
     *
     * @param parent the parent collection
     * @param docId the document if of the remote resource
     * @param id the id of the remote resource
     *
     * @throws XMLDBException if an error occurs during construction
     *
     * @deprecated Use {@link #RemoteXMLResource(RemoteCollection, XmldbURI, Optional, Optional)}.
     */
    @Deprecated
    public RemoteXMLResource(final RemoteCollection parent, final XmldbURI docId, final Optional<String> id)
            throws XMLDBException {
        this(parent, -1, -1, docId, id, Optional.empty());
    }

    /**
     * Construct a remote XML Resource.
     *
     * @param parent the parent collection
     * @param docId the document if of the remote resource
     * @param id the id of the remote resource
     * @param type the type of the remote resource
     *
     * @throws XMLDBException if an error occurs during construction
     * @deprecated Use {@link #RemoteXMLResource(RemoteCollection, int, int, XmldbURI, Optional, Optional)}.
     */
    @Deprecated
    public RemoteXMLResource(final RemoteCollection parent, final XmldbURI docId, final Optional<String> id, final Optional<String> type)
            throws XMLDBException {
        this(parent, -1, -1, docId, id, type);
    }

    /**
     * Construct a remote XML Resource.
     *
     * @param parent the parent collection
     * @param handle the handle to the remote resource
     * @param pos the position of the remote resource
     * @param docId the document if of the remote resource
     * @param id the id of the remote resource
     *
     * @throws XMLDBException if an error occurs during construction
     *
     * @deprecated Use {@link #RemoteXMLResource(RemoteCollection, int, int, XmldbURI, Optional, Optional)}.
     */
    @Deprecated
    public RemoteXMLResource(
            final RemoteCollection parent,
            final int handle,
            final int pos,
            final XmldbURI docId,
            final Optional<String> id) throws XMLDBException {
        this(parent, handle, pos, docId, id, Optional.empty());
    }

    public RemoteXMLResource(
            final RemoteCollection parent,
            final int handle,
            final int pos,
            final XmldbURI docId,
            final Optional<String> id,
            final Optional<String> type)
            throws XMLDBException {
        super(parent, docId, MediaType.APPLICATION_XML, type);
        this.handle = handle;
        this.pos = pos;
        this.id = id;
    }

    @Override
    public String getId() throws XMLDBException {
        return id.map(x -> x.equals("1") ? getDocumentId() : getDocumentId() + '_' + id).orElse(getDocumentId());
    }

    @Override
    public Properties getProperties() {
        return outputProperties == null ? super.getProperties() : outputProperties;
    }

    @Override
    public void setProperties(final Properties properties) {
        this.outputProperties = properties;
    }

    @Override
    public String getDocumentId() {
        return path.lastSegment().toString();
    }

    @Override
    public Object getContent() throws XMLDBException {
        if (root != null && contentFile == null) {
            // NOTE(AR) root will be serialized to contentFile once and then reused
            final ContentFile tempFile = serializeNodeToContentFile(root);
            setContent(tempFile);
        }

        if (content != null) {
            return new StringValue(content).getStringValue(true);
        }

        final Object res = super.getContent();
        if (res != null) {
            if (res instanceof byte[]) {
                return new String((byte[]) res, UTF_8);

            } else if (res instanceof Item) {
                final Item item = (Item) res;
                if (Type.subTypeOf(item.getType(), Type.NODE)) {
                    root = (Node) item;
                    // Serialize the Node to a String
                    try (final Writer writer = new StringWriter()) {
                        final Properties properties = new Properties();
                        properties.putAll(getProperties());
                        // NOTE(AR) We don't want to do XDM Serialization here as it has been previously handled
                        properties.setProperty(EXistOutputKeys.XDM_SERIALIZATION, "no");
                        serializeNode(root, properties, writer);
                        content = writer.toString();
                    } catch (final TransformerException | IOException e) {
                        throw new XMLDBException(ErrorCodes.VENDOR_ERROR, e.getMessage(), e);
                    }
                    return content;
                } else {
                    return item.toString();
                }
            } else {
                return res;
            }
        }
        return null;
    }

    private String getNamespace(final Node node) {
        @Nullable String namespace;
        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE:
                namespace = getNamespace(node.getFirstChild());
                break;

            case Node.ELEMENT_NODE:
            case Node.ATTRIBUTE_NODE:
                namespace = node.getNamespaceURI();
                break;

            default:
                namespace = XMLConstants.NULL_NS_URI;
        }

        if (namespace == null) {
            namespace = XMLConstants.NULL_NS_URI;
        }

        return namespace;
    }

    private ContentFile serializeNodeToContentFile(final Node node) throws XMLDBException {
        try  {
            final ContentFile.ContentFileType type;
            if (Marshaller.NAMESPACE.equals(getNamespace(node))) {
                type = ContentFile.ContentFileType.XQJ_SERIALIZATION;
            } else {
                type = ContentFile.ContentFileType.XDM_SERIALIZATION;
            }

            final Properties properties = new Properties(getProperties());
            final VirtualTempPath tempFile = new VirtualTempPath(type, getInMemorySize(properties), TemporaryFileManager.getInstance());
            try (final OutputStream out = tempFile.newOutputStream();
                 final OutputStreamWriter osw = new OutputStreamWriter(out, UTF_8)) {
                serializeNode(node, properties, osw);
            }
            return tempFile;
        } catch (final TransformerException | IOException ioe) {
            freeResources();
            throw new XMLDBException(ErrorCodes.VENDOR_ERROR, ioe.getMessage(), ioe);
        }
    }

    private void serializeNode(final Node node, final Properties properties, final Writer writer) throws TransformerException {
        final DOMSerializer xmlout = new DOMSerializer(writer, properties);
        xmlout.serialize(node);
    }

    @Override
    public Node getContentAsDOM() throws XMLDBException {
        if (root != null) {
            return root;
        }

        final Node node;
        InputStream cis = null;
        try {
            if (content != null) {
                cis = new UnsynchronizedByteArrayInputStream(content.getBytes(UTF_8));
            } else {
                cis = getStreamContent();
            }

            if (contentFile != null && contentFile.getType() == ContentFile.ContentFileType.XQJ_SERIALIZATION) {
                try {
                    final Sequence result = Marshaller.demarshall(cis);
                    // NOTE(AR) we are only expecting one value here!
                    if (!result.hasOne()) {
                        throw new XMLDBException(ErrorCodes.VENDOR_ERROR, "Expected a single Item from XQJ deserialization");
                    }
                    final Item item = result.itemAt(0);
                    if (!Type.subTypeOf(item.getType(), Type.NODE)) {
                        throw new XMLDBException(ErrorCodes.VENDOR_ERROR, "Expected a Node from XQJ deserialization");
                    }

                    node = (Node) item;

                } catch (final XMLStreamException | XPathException e) {
                    throw new XMLDBException(ErrorCodes.VENDOR_ERROR, e.getMessage(), e);
                }

            } else {
                try {
                    final InputSource is = new InputSource(cis);

                    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    factory.setValidating(false);
                    final DocumentBuilder builder = factory.newDocumentBuilder();
                    final Document doc = builder.parse(is);

                    final boolean isDocumentNode = type.map(t -> t.equals("document-node()")).orElse(true);
                    if (isDocumentNode) {
                        node = doc;
                    } else {
                        node = doc.getFirstChild();
                    }
                } catch (final SAXException | IOException | ParserConfigurationException e) {
                    throw new XMLDBException(ErrorCodes.VENDOR_ERROR, e.getMessage(), e);
                }
            }
        } finally {
            if (cis != null) {
                try {
                    cis.close();
                } catch (final IOException ioe) {
                    LOG.warn(ioe.getMessage(), ioe);
                }
            }
        }

        // NOTE(AR) cache the Node representation
        this.root = node;
        return root;
    }

    @Override
    public void getContentAsSAX(final ContentHandler handler) throws XMLDBException {
        if (root != null) {
            final String option = collection.getProperty(Serializer.GENERATE_DOC_EVENTS, "false");
            final DOMStreamer streamer = (DOMStreamer) SerializerPool.getInstance().borrowObject(DOMStreamer.class);
            try {
                streamer.setContentHandler(handler);
                streamer.setLexicalHandler(lexicalHandler);
                streamer.serialize(root, option.equalsIgnoreCase("true"));

                return;

            } catch (final SAXException e) {
                throw new XMLDBException(ErrorCodes.INVALID_RESOURCE, e.getMessage(), e);
            } finally {
                SerializerPool.getInstance().returnObject(streamer);
            }
        }

        final InputSource is;
        InputStream cis = null;

        try {
            if (content != null) {
                is = new InputSource(new StringReader(content));
            } else {
                cis = getStreamContent();
                is = new InputSource(cis);
            }

            XMLReader reader = xmlReader;
            if (reader == null) {
                final SAXParserFactory saxFactory = ExistSAXParserFactory.getSAXParserFactory();
                saxFactory.setNamespaceAware(true);
                saxFactory.setValidating(false);
                final SAXParser sax = saxFactory.newSAXParser();
                reader = sax.getXMLReader();
                reader.setFeature(FEATURE_SECURE_PROCESSING, true);
            }

            reader.setContentHandler(handler);
            if (lexicalHandler != null) {
                reader.setProperty(Namespaces.SAX_LEXICAL_HANDLER, lexicalHandler);
            }
            reader.parse(is);
        } catch (final ParserConfigurationException | SAXException | IOException e) {
            throw new XMLDBException(ErrorCodes.VENDOR_ERROR, e.getMessage(), e);
        } finally {
            if (cis != null) {
                try {
                    cis.close();
                } catch (final IOException ioe) {
                    LOG.warn(ioe.getMessage(), ioe);
                }
            }
        }
    }

    @Override
    public Object getExtendedContent() throws XMLDBException {
        return getExtendedContentInternal(content, idIsPresent(), handle, pos);
    }

    @Override
    public void setContent(final Object value) throws XMLDBException {
        this.content = null;
        this.root = null;
        this.file = null;
        this.contentFile = null;
        this.inputSource = null;

        if (!super.setContentInternal(value)) {
            if (value instanceof String) {
                content = (String) value;
            } else if (value instanceof byte[]) {
                content = new String((byte[]) value, UTF_8);

            } else {
                content = value.toString();
            }
        }
    }

    @Override
    public void setContentAsDOM(final Node root) throws XMLDBException {
        this.content = null;
        this.root = root;
        this.file = null;
        this.contentFile = null;
        this.inputSource = null;
    }

    @Override
    public ContentHandler setContentAsSAX() throws XMLDBException {
        freeResources();
        this.content = null;
        this.root = null;
        this.file = null;
        this.contentFile = null;
        this.inputSource = null;
        return new InternalXMLSerializer();
    }

    public boolean idIsPresent() {
        return id.isPresent();
    }
    
    public String getNodeId() {
        return id.orElse("1");
    }

    /**
     * Sets the external XMLReader to use.
     *
     * @param xmlReader the XMLReader
     */
    public void setXMLReader(final XMLReader xmlReader) {
        this.xmlReader = xmlReader;
    }

    private class InternalXMLSerializer extends SAXSerializer {
        private VirtualTempPath  tempFile = null;
        private OutputStreamWriter writer = null;

        public InternalXMLSerializer() {
            super();
        }

        @Override
        public void startDocument() throws SAXException {
            try {
                tempFile = new VirtualTempPath(ContentFile.ContentFileType.UNKNOWN, getInMemorySize(getProperties()), TemporaryFileManager.getInstance());
                writer = new OutputStreamWriter(tempFile.newOutputStream(), UTF_8);
                setOutput(writer, new Properties());

            } catch (final IOException ioe) {
                throw new SAXException("Unable to create temp file for serialization data", ioe);
            }

            super.startDocument();
        }

        @Override
        public void endDocument() throws SAXException {
            super.endDocument();

            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }

                setContent(tempFile);
            } catch (final IOException | XMLDBException e) {
                throw new SAXException("Unable to set file content containing serialized data", e);
            }
        }
    }

    @Override
    public boolean getSAXFeature(final String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        return false;
    }

    @Override
    public void setSAXFeature(final String name, final boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
    }

    @Override
    public void setLexicalHandler(final LexicalHandler handler) {
        this.lexicalHandler = handler;
    }

    @Override
    public DocumentType getDocType() throws XMLDBException {
        final List<String> params = new ArrayList<>(1);
        params.add(path.toString());

        final Object[] request = (Object[]) collection.execute("getDocType", params);
        final DocumentType result;
        if (!"".equals(request[0])) {
            result = new DocumentTypeImpl(null, (String) request[0], (String) request[1], (String) request[2]);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public void setDocType(final DocumentType doctype) throws XMLDBException {
        if (doctype != null) {
            final List<String> params = new ArrayList<>(4);
            params.add(path.toString());
            params.add(doctype.getName());
            params.add(doctype.getPublicId() == null ? "" : doctype.getPublicId());
            params.add(doctype.getSystemId() == null ? "" : doctype.getSystemId());

            collection.execute("setDocType", params);
        }
    }

    @Override
    public void getContentIntoAStream(final OutputStream os)
            throws XMLDBException {
        getContentIntoAStreamInternal(os, content, idIsPresent(), handle, pos);
    }

    @Override
    public void getContentAsStream(OutputStream os) throws XMLDBException {
       getContentIntoAStream(os);
    }

    @Override
    public InputStream getStreamContent() throws XMLDBException {
        return getStreamContentInternal(content, idIsPresent(), handle, pos);
    }

    @Override
    public long getStreamLength()
            throws XMLDBException {
        return getStreamLengthInternal(content);
    }

    @Override
    public void close() {
        if (!isClosed()) {
            this.content = null;
            this.root = null;
        }
        super.close();
    }
}
