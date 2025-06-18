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
package org.exist.xquery.modules.compression;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.collections.Collection;
import org.exist.dom.persistent.*;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock.LockMode;
import org.exist.storage.lock.LockManager;
import org.exist.storage.lock.ManagedDocumentLock;
import org.exist.storage.serializers.Serializer;
import org.exist.util.FileUtils;
import org.exist.util.LockException;
import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.*;
import org.exist.xquery.value.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.exist.util.StringUtil.isNullOrEmpty;

/**
 * Compresses a sequence of resources and/or collections
 * 
 * @author <a href="mailto:adam@exist-db.org">Adam Retter</a>
 * @author <a href="mailto:ljo@exist-db.org">Leif-Jöran Olsson</a>
 * @version 1.0
 */
public abstract class AbstractCompressFunction extends BasicFunction
{
    private final static Logger logger = LogManager.getLogger(AbstractCompressFunction.class);

    protected final static SequenceType SOURCES_PARAM = new FunctionParameterSequenceType("sources", Type.ANY_TYPE, Cardinality.ONE_OR_MORE,
            "The sequence of URI's and/or Entrys. If an URI points to a collection then the collection, its resources and sub-collections are zipped recursively. " +
            "If URI points to file (available only to the DBA role.) then file or directory are zipped. " +
            "An Entry takes the format <entry name=\"filename.ext\" type=\"collection|uri|binary|xml|text\" method=\"deflate|store\">data</entry>. The method attribute is only effective for the compression:zip function.");
    protected final static SequenceType COLLECTION_HIERARCHY_PARAM = new FunctionParameterSequenceType("use-collection-hierarchy", Type.BOOLEAN, Cardinality.EXACTLY_ONE, "Indicates whether the Collection hierarchy (if any) should be preserved in the zip file.");
    protected final static SequenceType STRIP_PREFIX_PARAM = new FunctionParameterSequenceType("strip-prefix", Type.STRING, Cardinality.EXACTLY_ONE, "This prefix is stripped from the Entrys name");
    protected final static SequenceType ENCODING_PARAM = new FunctionParameterSequenceType("encoding", Type.STRING, Cardinality.EXACTLY_ONE, "This encoding to be used for filenames inside the compressed file");

    private enum ZipMethod {
        DEFLATE,
        STORE
    }


    public AbstractCompressFunction(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }
	
	private String removeLeadingOffset(String uri, final String stripOffset) {
		// remove leading offset
		if (uri.startsWith(stripOffset)) {
			uri = uri.substring(stripOffset.length());
		}
		// remove leading /
		if (uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		return uri;
	}

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
		// are there some uri's to tar?
		if (args[0].isEmpty()) {
			return Sequence.EMPTY_SEQUENCE;
		}

		// use a hierarchy in the tar file?
        final boolean useHierarchy = args[1].effectiveBooleanValue();

		// Get offset
        final String stripOffset;
		if (args.length == 3) {
			stripOffset = args[2].getStringValue();
		} else {
            stripOffset = "";
        }

		// Get encoding
        try {
            final Charset encoding;
            if ((args.length >= 4) && !args[3].isEmpty()) {
                encoding = Charset.forName(args[3].getStringValue());
            } else {
                encoding = StandardCharsets.UTF_8;
            }

            try (final UnsynchronizedByteArrayOutputStream baos = new UnsynchronizedByteArrayOutputStream();
                 final OutputStream os = stream(baos, encoding);
                 final StringBuilderWriter sbWriter = new StringBuilderWriter()) {

                final CRC32 chksum = new CRC32();

                // iterate through the argument sequence
                for (final SequenceIterator i = args[0].iterate(); i.hasNext(); ) {
                    final Item item = i.nextItem();

                    if (item instanceof Element) {
                        Element element = (Element) item;
                        compressElement(os, element, useHierarchy, stripOffset, sbWriter, chksum);
                    } else {
                        compressFromUri(os, ((AnyURIValue) item).toURI(), useHierarchy, stripOffset, ZipMethod.DEFLATE, null, sbWriter, chksum);
                    }
                }

                os.flush();

                if(os instanceof DeflaterOutputStream) {
                    ((DeflaterOutputStream) os).finish();
                }

                return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), new UnsynchronizedByteArrayInputStream(baos.toByteArray()), this);
            }
		} catch (final UnsupportedCharsetException | IOException e) {
			throw new XPathException(this, e.getMessage(), e);
		}
	}

    private void compressFromUri(final OutputStream os, final URI uri, final boolean useHierarchy, final String stripOffset, final ZipMethod method, final String resourceName, final StringBuilderWriter sbWriter, final CRC32 chksum) throws XPathException
        {
            try {
                if ("file".equals(uri.getScheme())) {

                    if (!context.getSubject().hasDbaRole()) {
                        XPathException xPathException = new XPathException(this, "Permission denied, calling user '" + context.getSubject().getName() + "' must be a DBA to call this function.");
                        logger.error("Invalid user", xPathException);
                        throw xPathException;
                    }

                    // got a file
                    final Path file = Paths.get(uri.getPath());
                    compressFile(os, file, useHierarchy, stripOffset, method, resourceName, chksum);

                } else {

                    final XmldbURI xmldburi = XmldbURI.create(uri);

                    // try for a collection
                    try (final Collection collection = context.getBroker().openCollection(xmldburi, LockMode.READ_LOCK)) {
                        if(collection != null) {
                            compressCollection(os, collection, useHierarchy, stripOffset, method, sbWriter, chksum);
                            return;
                        }
                    } catch (final PermissionDeniedException | LockException | SAXException | IOException pde) {
                        throw new XPathException(this, pde.getMessage());
                    }


                    // otherwise, try for a doc
                    try (final Collection collection = context.getBroker().openCollection(xmldburi.removeLastSegment(), LockMode.READ_LOCK)) {
                        if(collection == null) {
                            throw new XPathException(this, "Invalid URI: " + uri.toString());
                        }

                        try (final LockedDocument doc = collection.getDocumentWithLock(context.getBroker(), xmldburi.lastSegment(), LockMode.READ_LOCK)) {

                            // NOTE: early release of Collection lock inline with Asymmetrical Locking scheme
                            collection.close();

                            if (doc == null) {
                                throw new XPathException(this, "Invalid URI: " + uri.toString());
                            }

                            compressResource(os, doc.getDocument(), useHierarchy, stripOffset, method, resourceName, sbWriter, chksum);
                            return;
                        }
                    } catch (final PermissionDeniedException | LockException | SAXException | IOException pde) {
                        throw new XPathException(this, pde.getMessage());
                    }
                }

            } catch (final IOException e) {
                throw new XPathException(this, e.getMessage());
            }

        }

    /**
     * Adds a file to an archive.
     *
     * @param os The Output Stream to add the file to.
     * @param file The file to add to the archive.
     * @param useHierarchy Whether to use a folder hierarchy in the archive file that reflects the collection hierarchy.
     * @param stripOffset a string that should be stripped from the start of the entry name.
     * @param method the Zip method.
     * @param name the name of the entry.
     * @param chksum an object that is used to calculate the checksum.
     */
    private void compressFile(final OutputStream os, final Path file, final boolean useHierarchy, final String stripOffset, final ZipMethod method, final String name, final CRC32 chksum) throws IOException {

        if (!Files.isDirectory(file)) {

            // create an entry in the Tar for the document
            final Object entry;
            if (name != null) {
                entry = newEntry(name);
            } else if (useHierarchy) {
                entry = newEntry(removeLeadingOffset(file.toAbsolutePath().toString(), stripOffset));
            } else {
                entry = newEntry(file.getFileName().toString());
            }

            final byte[] value = Files.readAllBytes(file);

            // close the entry
            if (entry instanceof ZipEntry && method == ZipMethod.STORE) {
                ((ZipEntry) entry).setMethod(ZipOutputStream.STORED);
                chksum.reset();
                chksum.update(value);
                ((ZipEntry) entry).setCrc(chksum.getValue());
                ((ZipEntry) entry).setSize(value.length);
            }

            putEntry(os, entry);
            os.write(value);
            closeEntry(os);

        } else {

            for (final Path child : FileUtils.list(file)) {
                compressFile(os, file.resolve(child), useHierarchy, stripOffset, method, null, chksum);
            }

        }

    }

    /**
     * Adds an element to an archive.
     *
     * @param os The Output Stream to add the element to.
     * @param element The element to add to the archive.
     * @param useHierarchy Whether to use a folder hierarchy in the archive file that reflects the collection hierarchy.
     * @param stripOffset a string that should be stripped from the start of the entry name.
     * @param sbWriter a StringBuilderWriter to reuse
     * @param chksum an object that is used to calculate the checksum.
     */
	private void compressElement(final OutputStream os, final Element element, final boolean useHierarchy,
            final String stripOffset, final StringBuilderWriter sbWriter, final CRC32 chksum) throws XPathException {

        final String ns = element.getNamespaceURI();
        if (!(element.getNodeName().equals("entry") || (ns != null && !ns.isEmpty()))) {
            throw new XPathException(this, "Item must be type of xs:anyURI or element entry.");
        }

        if (element.getChildNodes().getLength() > 1) {
            throw new XPathException(this, "Entry content is not valid XML fragment.");
        }

        String name = element.getAttribute("name");
//            if(name == null)
//                throw new XPathException(this, "Entry must have name attribute.");

        final String type = element.getAttribute("type");
        ZipMethod method;
        try {
            method = ZipMethod.valueOf(element.getAttribute("method").toUpperCase());
        } catch (final IllegalArgumentException e) {
            method = ZipMethod.DEFLATE;
        }

        if ("uri".equals(type)) {
            @Nullable final String uri = element.getFirstChild().getNodeValue();
            if (isNullOrEmpty(uri)) {
                throw new XPathException(this, "Entry with type uri must contain a URI.");
            }
            compressFromUri(os, URI.create(uri), useHierarchy, stripOffset, method, name, sbWriter, chksum);
            return;
        }

        if (useHierarchy) {
            name = removeLeadingOffset(name, stripOffset);
        } else {
            name = name.substring(name.lastIndexOf("/") + 1);
        }

        if ("collection".equals(type)) {
            name += "/";
        }

        @Nullable Object entry = null;
        try {
            entry = newEntry(name);

            if (!"collection".equals(type)) {
                final byte[] value;
                final Node content = element.getFirstChild();

                if (content == null) {
                    value = new byte[0];
                } else {
                    if (content.getNodeType() == Node.TEXT_NODE) {
                        String text = content.getNodeValue();
                        if ("binary".equals(type)) {
                            //base64 binary
                            value = Base64.decodeBase64(text);
                        } else {
                            //text
                            value = text.getBytes();
                        }
                    } else {
                        //xml
                        final Serializer serializer = context.getBroker().borrowSerializer();
                        try {
                            serializer.setUser(context.getSubject());
                            serializer.setProperty("omit-xml-declaration", "no");
                            getDynamicSerializerOptions(serializer);
                            sbWriter.getBuilder().setLength(0);
                            serializer.serialize((NodeValue) content, sbWriter);
                            value = sbWriter.toString().getBytes(StandardCharsets.UTF_8);
                        } finally {
                            context.getBroker().returnSerializer(serializer);
                        }
                    }
                }

                if (entry instanceof ZipEntry && method == ZipMethod.STORE) {
                    ((ZipEntry) entry).setMethod(ZipOutputStream.STORED);
                    chksum.reset();
                    chksum.update(value);
                    ((ZipEntry) entry).setCrc(chksum.getValue());
                    ((ZipEntry) entry).setSize(value.length);
                }
                putEntry(os, entry);

                os.write(value);
            }
        } catch (final IOException | SAXException ioe) {
            throw new XPathException(this, ioe.getMessage(), ioe);
        } finally {
            if (entry != null) {
                try {
                    closeEntry(os);
                } catch (final IOException ioe) {
                    throw new XPathException(this, ioe.getMessage(), ioe);
                }
            }
        }
	}

    private void getDynamicSerializerOptions(final Serializer serializer) throws SAXException {
        final Option option = context.getOption(Option.SERIALIZE_QNAME);
        if (option != null) {
            final String[] params = option.tokenizeContents();
            for (final String param : params) {
                // OutputKeys.INDENT
                final String[] kvp = Option.parseKeyValuePair(param);
                serializer.setProperty(kvp[0], kvp[1]);
            }
        }
    }

	/**
	 * Adds a document to an archive.
	 *
	 * @param os The Output Stream to add the document to.
	 * @param doc The document to add to the archive.
	 * @param useHierarchy Whether to use a folder hierarchy in the archive file that reflects the collection hierarchy.
	 * @param stripOffset a string that should be stripped from the start of the entry name.
	 * @param method the Zip method.
	 * @param name the name of the entry.
	 * @param sbWriter a StringBuilderWriter to reuse
	 * @param chksum an object that is used to calculate the checksum.
	 */
	private void compressResource(final OutputStream os, final DocumentImpl doc, final boolean useHierarchy, final String stripOffset, final ZipMethod method, final String name, final StringBuilderWriter sbWriter, final CRC32 chksum) throws IOException, SAXException {
		// create an entry in the Tar for the document
        final Object entry;
        if (name != null) {
            entry = newEntry(name);
        } else if (useHierarchy) {
            final String docCollection = doc.getCollection().getURI().toString();
            final XmldbURI collection = XmldbURI.create(removeLeadingOffset(docCollection, stripOffset));
			entry = newEntry(collection.append(doc.getFileURI()).toString());
		} else {
			entry = newEntry(doc.getFileURI().toString());
		}

        final byte[] value;
        if (doc.getResourceType() == DocumentImpl.XML_FILE) {
            // xml file
            final Serializer serializer = context.getBroker().borrowSerializer();
            try {
                serializer.setUser(context.getSubject());
                serializer.setProperty("omit-xml-declaration", "no");
                getDynamicSerializerOptions(serializer);
                sbWriter.getBuilder().setLength(0);
                serializer.serialize(doc, sbWriter);
                String strDoc = sbWriter.toString();
                value = strDoc.getBytes();
            } finally {
                context.getBroker().returnSerializer(serializer);
            }
        } else if (doc.getResourceType() == DocumentImpl.BINARY_FILE && doc.getContentLength() > 0) {
            // binary file
            try (final InputStream is = context.getBroker().getBinaryResource((BinaryDocument) doc);
                 final UnsynchronizedByteArrayOutputStream baos = new UnsynchronizedByteArrayOutputStream(doc.getContentLength() == -1 ? 1024 : (int) doc.getContentLength())) {
                baos.write(is);
                value = baos.toByteArray();
            }
        } else {
            value = new byte[0];
        }

		// close the entry
        if (entry instanceof ZipEntry && method == ZipMethod.STORE) {
            ((ZipEntry) entry).setMethod(ZipOutputStream.STORED);
            chksum.reset();
            chksum.update(value);
            ((ZipEntry) entry).setCrc(chksum.getValue());
            ((ZipEntry) entry).setSize(value.length);
        }

		putEntry(os, entry);
        os.write(value);
		closeEntry(os);
	}
	
	/**
	 * Adds a Collection and its child collections and resources recursively to an archive.
	 *
	 * @param os The Output Stream to add the Collection to.
	 * @param col The Collection to add to the archive.
	 * @param useHierarchy Whether to use a folder hierarchy in the archive file that reflects the collection hierarchy.
	 * @param stripOffset a string that should be stripped from the start of the entry name.
	 * @param method the Zip method.
	 * @param sbWriter a StringBuilderWriter to reuse
	 * @param chksum an object that is used to calculate the checksum.
	 */
	private void compressCollection(final OutputStream os, final Collection col, final boolean useHierarchy, final String stripOffset, final ZipMethod method, final StringBuilderWriter sbWriter, final CRC32 chksum) throws IOException, SAXException, LockException, PermissionDeniedException {
		// iterate over child documents
        final DBBroker broker = context.getBroker();
        final LockManager lockManager = broker.getBrokerPool().getLockManager();
        final MutableDocumentSet childDocs = new DefaultDocumentSet();
		col.getDocuments(broker, childDocs);
		for (final Iterator<DocumentImpl> itChildDocs = childDocs.getDocumentIterator(); itChildDocs.hasNext();) {
			final DocumentImpl childDoc = itChildDocs.next();
			try (final ManagedDocumentLock updateLock = lockManager.acquireDocumentReadLock(childDoc.getURI())) {
				compressResource(os, childDoc, useHierarchy, stripOffset, method, null, sbWriter, chksum);
			}
		}
		// iterate over child collections
		for (final Iterator<XmldbURI> itChildCols = col.collectionIterator(broker); itChildCols.hasNext();) {
			// get the child collection
            final XmldbURI childColURI = itChildCols.next();
            final Collection childCol = broker.getCollection(col.getURI().append(childColURI));
			// recurse
			compressCollection(os, childCol, useHierarchy, stripOffset, method, sbWriter, chksum);
		}
	}
	
	protected abstract OutputStream stream(UnsynchronizedByteArrayOutputStream baos, Charset encoding);
	
	protected abstract Object newEntry(String name);
	
	protected abstract void putEntry(Object os, Object entry) throws IOException;

	protected abstract void closeEntry(Object os) throws IOException;
}
