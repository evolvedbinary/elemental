/*
 * Copyright (C) 2014, Evolved Binary Ltd
 *
 * This file was originally ported from FusionDB to Elemental by
 * Evolved Binary, for the benefit of the Elemental Open Source community.
 * Only the ported code as it appears in this file, at the time that
 * it was contributed to Elemental, was re-licensed under The GNU
 * Lesser General Public License v2.1 only for use in Elemental.
 *
 * This license grant applies only to a snapshot of the code as it
 * appeared when ported, it does not offer or infer any rights to either
 * updates of this source code or access to the original source code.
 *
 * The GNU Lesser General Public License v2.1 only license follows.
 *
 * =====================================================================
 *
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
package org.exist.source;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import com.evolvedbinary.j8fu.function.*;
import org.exist.EXistException;
import org.exist.dom.QName;
import org.exist.dom.persistent.BinaryDocument;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.security.Subject;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock.LockMode;
import org.exist.storage.serializers.Serializer;
import org.exist.storage.txn.TransactionException;
import org.exist.storage.txn.Txn;
import org.exist.util.io.InputStreamUtil;
import org.exist.util.io.TemporaryFileManager;
import org.exist.util.serializer.SAXSerializer;
import org.exist.util.serializer.SerializerPool;
import org.exist.xmldb.XmldbURI;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Source implementation that given a URI reads from a resource stored in the database.
 *
 * Read locks are only taken on the document
 * during {@link #from(BrokerPool, XmldbURI, boolean, boolean)},
 * {@link #isValid()}, {@link #getPermissions()},
 * and {@link #getContent()}.
 *
 * Calling {@link #getInputStream()} or {@link #getReader()} witll
 * return an object that holds a read lock on the document until
 * {@link AutoCloseable#close()} is called on it.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>.
 */
public class DbUriSource extends AbstractSource implements DbStoreSource {

    private final BrokerPool brokerPool;
    private final Optional<Subject> dbSubject;
    private final XmldbURI docUri;
    private String encoding = UTF_8.name();
    private final boolean checkEncoding;
    private final long docLastModified;
    private final boolean errorOnModified;

    private DbUriSource(final BrokerPool brokerPool, final Optional<Subject> dbSubject, final boolean checkXQEncoding, final XmldbURI docUri, final long docLastModified, final boolean errorOnModified) {
        super(hashKey(docUri.getCollectionPath()));
        this.brokerPool = brokerPool;
        this.dbSubject = dbSubject;
        this.checkEncoding = checkXQEncoding;
        this.docUri = docUri;
        this.docLastModified = docLastModified;
        this.errorOnModified = errorOnModified;
    }

    /**
     * Create a new DbUriSource.
     *
     * @param brokerPool the Broker Pool.
     * @param docUri URI to the document in the database.
     * @param checkXQEncoding true if the encoding should be checked from the XQuery.
     * @param errorOnModified when true and error is raised if the document was modified since the source was created.
     *
     * @throws NoSuchDocumentException the provided docUri does not exist in the database.
     * @throws EXistException an error occurred whilst accessing the database.
     * @throws PermissionDeniedException the calling user has insufficient permissions to access the document in the database.
     */
    public static DbUriSource from(final BrokerPool brokerPool, final XmldbURI docUri, final boolean checkXQEncoding, final boolean errorOnModified) throws NoSuchDocumentException, EXistException, PermissionDeniedException {
        return from(brokerPool, null, docUri, checkXQEncoding, errorOnModified);
    }

    /**
     * Create a new DbUriSource.
     *
     * @param brokerPool the Broker Pool.
     * @param dbSubject the database user for database operations, or null to use the current subject.
     * @param docUri URI to the document in the database.
     * @param checkXQEncoding true if the encoding should be checked from the XQuery.
     * @param errorOnModified when true and error is raised if the document was modified since the source was created.
     *
     * @throws NoSuchDocumentException the provided docUri does not exist in the database.
     * @throws EXistException an error occurred whilst accessing the database.
     * @throws PermissionDeniedException the calling user has insufficient permissions to access the document in the database.
     */
    public static DbUriSource from(final BrokerPool brokerPool, @Nullable final Subject dbSubject, final XmldbURI docUri, final boolean checkXQEncoding, final boolean errorOnModified) throws NoSuchDocumentException, EXistException, PermissionDeniedException {
        final long docLastModified = DbUriSource.<Long, NoSuchDocumentException>readDocument(brokerPool, Optional.ofNullable(dbSubject), docUri, (broker, transaction, doc) -> {
            if (doc == null) {
                throw new NoSuchDocumentException("No such document: " + docUri.getCollectionPath());
            }
            return doc.getLastModified();
        });

        return new DbUriSource(brokerPool, Optional.ofNullable(dbSubject), checkXQEncoding, docUri, docLastModified, errorOnModified);
    }

    /**
     * Create a new DbUriSource.
     *
     * @param brokerPool the Broker Pool.
     * @param doc the document in the database.
     * @param checkXQEncoding true if the encoding should be checked from the XQuery.
     * @param errorOnModified when true and error is raised if the document was modified since the source was created.
     */
    public static DbUriSource from(final BrokerPool brokerPool, final DocumentImpl doc, final boolean checkXQEncoding, final boolean errorOnModified) {
        return from(brokerPool, null, doc, checkXQEncoding, errorOnModified);
    }

    /**
     * Create a new DbUriSource.
     *
     * @param brokerPool the Broker Pool.
     * @param dbSubject the database user for database operations, or null to use the current subject.
     * @param doc the document in the database.
     * @param checkXQEncoding true if the encoding should be checked from the XQuery.
     * @param errorOnModified when true and error is raised if the document was modified since the source was created.
     */
    public static DbUriSource from(final BrokerPool brokerPool, @Nullable final Subject dbSubject, final DocumentImpl doc, final boolean checkXQEncoding, final boolean errorOnModified) {
        return new DbUriSource(brokerPool, Optional.ofNullable(dbSubject), checkXQEncoding, doc.getURI(), doc.getLastModified(), errorOnModified);
    }

    @Override
    public String path() {
        return docUri.getCollectionPath();
    }

    @Override
    public String type() {
        return "DB";
    }

    @Override
    public XmldbURI getDocumentPath() {
        return docUri;
    }

    @Override
    public Validity isValid() {
        try {
            return readDocument((broker, transaction, doc) -> {
                if (doc == null) {
                    return Validity.INVALID;

                } else if (doc.getLastModified() > docLastModified) {
                    return Validity.INVALID;

                } else {
                    return Validity.VALID;
                }
            });
        } catch (final EXistException | PermissionDeniedException pde) {
            return Validity.INVALID;
        }
    }

    @Override
    public Reader getReader() throws IOException {
        final BufferedInputStream bis = new BufferedInputStream(getInputStream());
        try {
            bis.mark(128);
            checkEncoding(bis);
            bis.reset();
            return new InputStreamReader(bis, encoding);
        } catch (final IOException e) {
            bis.close();
            throw e;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        @Nullable DBBroker broker = null;
        @Nullable Txn transaction = null;
        @Nullable LockedDocument lockedDoc = null;

        try {
            broker = brokerPool.get(dbSubject);
            transaction = brokerPool.getTransactionManager().beginTransaction();
            lockedDoc = broker.getXMLResource(docUri, LockMode.READ_LOCK);

            if (lockedDoc == null) {
                throw new NoSuchDocumentException("No such document: " + docUri.getCollectionPath());
            }

            final DocumentImpl doc = lockedDoc.getDocument();
            if (doc.getLastModified() > docLastModified && errorOnModified) {
                throw new IOException("Document has been modified since DbUriSource was created: " + docUri.getCollectionPath());
            }

            if (doc instanceof BinaryDocument) {
                // Get InputStream of Binary document
                return BinaryDocumentInputStream.of(broker, transaction, lockedDoc);

            } else {
                // Get InputStream of XML document
                return XMLDocumentInputStream.of(broker, transaction, lockedDoc);
            }
        } catch (final EXistException | PermissionDeniedException | IOException e) {
            if (lockedDoc != null) {
                lockedDoc.close();
            }

            if (transaction != null) {
                transaction.abort();
                transaction.close();
            }

            if (broker != null) {
                broker.close();
            }

            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw new IOException(e.getMessage(), e);
            }
        }
    }

    @Override
    public String getContent() throws IOException {
        try (final InputStream is = new BufferedInputStream(getInputStream())) {
            return InputStreamUtil.readString(is, Charset.forName(encoding));
        }
    }

    @Override
    public QName isModule() throws IOException {
        try (final InputStream is = getInputStream()) {
            return getModuleDecl(is);
        }
    }

    private void checkEncoding(final InputStream is) {
        if (checkEncoding) {
            final String checkedEnc = guessXQueryEncoding(is);
            if(checkedEnc != null) {
                encoding = checkedEnc;
            }
        }
    }

    @Override
    public String toString() {
        return docUri.getCollectionPath();
    }

    @Override
    public Permission getPermissions() throws IOException {
        try {
            return readDocument((broker, transaction, doc) -> doc.getPermissions());
        } catch (final EXistException | PermissionDeniedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int hashCode() {
        return getDocumentPath().hashCode();
    }

    private <T, E extends Exception> T readDocument(final TriFunctionE<DBBroker, Txn, DocumentImpl, T, E> documentOp) throws E, EXistException, PermissionDeniedException {
        return readDocument(brokerPool, dbSubject, docUri, documentOp);
    }

    private static <T, E extends Exception> T readDocument(final BrokerPool brokerPool, final Optional<Subject> dbSubject, final XmldbURI docUri, final TriFunctionE<DBBroker, Txn, DocumentImpl, T, E> documentOp) throws E, EXistException, PermissionDeniedException {
        try (final DBBroker broker = brokerPool.get(dbSubject);
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction();
             final LockedDocument lockedDoc = broker.getXMLResource(docUri, LockMode.READ_LOCK)) {
            DocumentImpl doc = null;
            if (lockedDoc != null) {
                doc = lockedDoc.getDocument();
            }

            final T result = documentOp.apply(broker, transaction, doc);

            transaction.commit();

            return result;
        }
    }

    public static class NoSuchDocumentException extends EXistException {
        public NoSuchDocumentException(final String message) {
            super(message);
        }
    }

    /**
     * An InputStream for a binary document that takes ownership of the DBBroker, Txn, and Document Lock.
     */
    public static class BinaryDocumentInputStream extends FilterInputStream {

        private final DBBroker broker;
        private final Txn transaction;
        private final LockedDocument lockedDocument;
        private boolean closed = false;

        private BinaryDocumentInputStream(final DBBroker broker, final Txn transaction, final LockedDocument lockedDocument, final InputStream is) {
            super(is);
            this.broker = broker;
            this.transaction = transaction;
            this.lockedDocument = lockedDocument;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }

            @Nullable IOException ioe = null;
            try {
                super.close();
            } catch (final IOException e) {
                ioe = e;
            }

            lockedDocument.close();

            @Nullable TransactionException te = null;
            try {
                if (ioe != null) {
                    transaction.abort();
                } else {
                    transaction.commit();
                }
                transaction.close();
            } catch (final TransactionException e) {
                te = e;
            }

            broker.close();

            closed = true;

            if (ioe != null) {
                throw ioe;
            }

            if (te != null) {
                throw new IOException(te);
            }
        }

        public static BinaryDocumentInputStream of(final DBBroker broker, final Txn transaction, final LockedDocument lockedDocument) throws IOException {
            final DocumentImpl doc = lockedDocument.getDocument();
            if (!(doc instanceof BinaryDocument)) {
                throw new IllegalArgumentException("Expected binary document but received: " + lockedDocument.getClass().getSimpleName());
            }

            final InputStream is = broker.getBinaryResource(transaction, (BinaryDocument) doc);
            return new BinaryDocumentInputStream(broker, transaction, lockedDocument, is);
        }
    }

    /**
     * An InputStream for an XML document that takes ownership of the DBBroker, Txn, Document Lock, and a Temporary File.
     */
    public static class XMLDocumentInputStream extends FilterInputStream {

        private final DBBroker broker;
        private final Txn transaction;
        private final LockedDocument lockedDocument;
        private final TemporaryFileManager temporaryFileManager;
        private final Path temporaryFile;
        private boolean closed = false;

        private XMLDocumentInputStream(final DBBroker broker, final Txn transaction, final LockedDocument lockedDocument, final TemporaryFileManager temporaryFileManager, final Path temporaryFile, final InputStream is) {
            super(is);
            this.broker = broker;
            this.transaction = transaction;
            this.lockedDocument = lockedDocument;
            this.temporaryFileManager = temporaryFileManager;
            this.temporaryFile = temporaryFile;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }

            @Nullable IOException ioe = null;
            try {
                super.close();
            } catch (final IOException e) {
                ioe = e;
            }

            temporaryFileManager.returnTemporaryFile(temporaryFile);

            lockedDocument.close();

            @Nullable TransactionException te = null;
            try {
                if (ioe != null) {
                    transaction.abort();
                } else {
                    transaction.commit();
                }
                transaction.close();
            } catch (final TransactionException e) {
                te = e;
            }

            broker.close();

            closed = true;

            if (ioe != null) {
                throw ioe;
            }

            if (te != null) {
                throw new IOException(te);
            }
        }

        public static XMLDocumentInputStream of(final DBBroker broker, final Txn transaction, final LockedDocument lockedDocument) throws IOException {
            final DocumentImpl doc = lockedDocument.getDocument();
            if (doc instanceof BinaryDocument) {
                throw new IllegalArgumentException("Expected XML document but received: " + lockedDocument.getClass().getSimpleName());
            }

            // Serialize XML to temporary file
            final TemporaryFileManager temporaryFileManager = TemporaryFileManager.getInstance();
            @Nullable Path temporaryFile = temporaryFileManager.getTemporaryFile();
            try {

                final Properties outputProperties = new Properties();
                outputProperties.setProperty("method", "xml");
                outputProperties.setProperty("media-type", "application/xml; charset=UTF-8");
                outputProperties.setProperty("indent", "yes");
                outputProperties.setProperty("omit-xml-declaration", "yes");

                final Serializer serializer = broker.borrowSerializer();
                serializer.setProperties(outputProperties);
                try {
                    final SAXSerializer sax = (SAXSerializer) SerializerPool.getInstance().borrowObject(SAXSerializer.class);
                    try (final OutputStream os = new BufferedOutputStream(Files.newOutputStream(temporaryFile));
                         final Writer writer = new OutputStreamWriter(os, UTF_8)) {
                        sax.setOutput(writer, outputProperties);
                        serializer.setSAXHandlers(sax, sax);

                        serializer.toSAX(doc);

                    } finally {
                        SerializerPool.getInstance().returnObject(sax);
                    }
                } finally {
                    broker.returnSerializer(serializer);
                }
            } catch (final SAXException e) {
                if (temporaryFile != null) {
                    temporaryFileManager.returnTemporaryFile(temporaryFile);
                }
            }

            final InputStream is = Files.newInputStream(temporaryFile);
            return new XMLDocumentInputStream(broker, transaction, lockedDocument, temporaryFileManager, temporaryFile, is);
        }
    }
}
