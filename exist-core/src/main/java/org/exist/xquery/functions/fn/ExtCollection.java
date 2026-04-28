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
package org.exist.xquery.functions.fn;

import org.exist.collections.Collection;
import org.exist.dom.QName;
import org.exist.dom.persistent.DefaultDocumentSet;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.DocumentSet;
import org.exist.dom.persistent.MutableDocumentSet;
import org.exist.dom.persistent.NodeProxy;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.lock.Lock;
import org.exist.storage.lock.LockManager;
import org.exist.storage.lock.ManagedDocumentLock;
import org.exist.util.LockException;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.*;
import org.exist.xquery.functions.xmldb.XMLDBModule;
import org.exist.xquery.value.*;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import static org.exist.xquery.FunctionDSL.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ExtCollection extends BasicFunction {

    private static final String FS_COLLECTION_NAME = "collection";
    static final FunctionSignature[] FS_COLLECTION = functionSignatures(
            new QName(FS_COLLECTION_NAME, FnModule.NAMESPACE_URI),
            "Returns the documents contained in the Collection specified in the input sequence. "
                    + XMLDBModule.COLLECTION_URI + " Documents contained in sub-collections are also included. "
                    + "If no value is supplied, the statically know documents are used; for the REST Server this could be the collection in the URI path.",
            returnsOptMany(Type.ITEM, "The items indicated by the Collection URI"),
            arities(
                    arity(),
                    arity(
                            optParam("collection-uri", Type.STRING,"The Collection URI")
                    )
            )
    );

    private final boolean includeSubCollections;

    public ExtCollection(final XQueryContext context, final FunctionSignature signature) {
        this(context, signature, true);
    }

    public ExtCollection(final XQueryContext context, final FunctionSignature signature, final boolean inclusive) {
        super(context, signature);
        includeSubCollections = inclusive;
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        @Nullable final URI collectionUri;
        if (args.length == 0 || args[0].isEmpty()) {
            collectionUri = null;
        } else {
            collectionUri = asUri(args[0].itemAt(0).getStringValue());
        }

        return getCollectionItems(collectionUri);
    }

    protected Sequence getCollectionItems(@Nullable final URI collectionUri) throws XPathException {
        if (collectionUri == null) {
            // no collection-uri(s)
            return getDefaultCollectionItems();
        }

        return getCollectionUriItems(collectionUri);
    }

    private Sequence getDefaultCollectionItems() throws XPathException {
        @Nullable Sequence items = null;

        @Nullable final Sequence dynamicCollection = context.getDynamicallyAvailableCollection("");
        if (dynamicCollection != null) {
            items = new ValueSequence(dynamicCollection);
        }

        if (items == null) {
            final DocumentSet staticallyKnownDocuments = context.getStaticallyKnownDocuments();
            items = new ValueSequence(staticallyKnownDocuments.getDocumentCount());
            addAll(staticallyKnownDocuments, items);
        }

        if (items == null) {
            items = Sequence.EMPTY_SEQUENCE;
        }

        return items;
    }

    private Sequence getCollectionUriItems(final URI collectionUri) throws XPathException {
        @Nullable final Sequence dynamicCollection = context.getDynamicallyAvailableCollection(collectionUri.toString());
        if (dynamicCollection != null) {
            return dynamicCollection;

        } else {
            @Nullable MutableDocumentSet docs = null;

            final XmldbURI uri = XmldbURI.create(collectionUri);
            try (@Nullable final Collection coll = context.getBroker().openCollection(uri, Lock.LockMode.READ_LOCK)) {
                if (coll == null) {
                    if (context.isRaiseErrorOnFailedRetrieval()) {
                        throw new XPathException(this, ErrorCodes.FODC0002, "Can not access collection '" + uri + "'");
                    }
                } else {
                    docs = new DefaultDocumentSet();
                    if (context.inProtectedMode()) {
                        context.getProtectedDocs().getDocsByCollection(coll, docs);
                    } else {
                        coll.allDocs(context.getBroker(), docs, includeSubCollections, context.getProtectedDocs());
                    }
                }
            } catch (final PermissionDeniedException e) {
                throw new XPathException(this, ErrorCodes.FODC0002, "Can not access collection '" + e.getMessage() + "'", new StringValue(collectionUri.toString()), e);
            } catch (final LockException e) {
                throw new XPathException(this, ErrorCodes.FODC0002, e.getMessage(), new StringValue(collectionUri.toString()), e);
            }

            if (docs == null || docs.getDocumentCount() == 0) {
                return Sequence.EMPTY_SEQUENCE;
            }

            final Sequence items = new ValueSequence(docs.getDocumentCount());
            addAll(docs, items);
            return items;
        }
    }

    protected URI asUri(final String path) throws XPathException {
        try {
            URI uri = new URI(path);
            if (!uri.isAbsolute()) {
                final AnyURIValue baseXdmUri = context.getBaseURI();
                if (baseXdmUri != null && !baseXdmUri.equals(AnyURIValue.EMPTY_URI)) {
                    URI baseUri = baseXdmUri.toURI();
                    if (!baseUri.toString().endsWith("/")) {
                        baseUri = new URI(baseUri.toString() + '/');
                    }
                    uri = baseUri.resolve(uri);
                } else if (!XmldbURI.create(uri).isAbsolute()) {
                    throw new XPathException(this, ErrorCodes.FODC0003, "$uri is a relative URI but there is no base-URI set");
                }
            }
            return uri;
        } catch (final URISyntaxException e) {
            throw new XPathException(this, ErrorCodes.FODC0004, e);
        }
    }

    private void addAll(final DocumentSet docs, final Sequence items) throws XPathException {
        final LockManager lockManager = context.getBroker().getBrokerPool().getLockManager();
        for (final Iterator<DocumentImpl> i = docs.getDocumentIterator(); i.hasNext(); ) {
            final DocumentImpl doc = i.next();

            // filter out binary documents, fn:collection should only return XML documents
            if (doc.getResourceType() == DocumentImpl.XML_FILE) {

                ManagedDocumentLock dlock = null;
                try {
                    if (!context.inProtectedMode()) {
                        dlock = lockManager.acquireDocumentReadLock(doc.getURI());
                    }
                    items.add(new NodeProxy(this, doc));
                } catch (final LockException e) {
                    throw new XPathException(this, ErrorCodes.FODC0002, e);
                } finally {
                    if (dlock != null) {
                        dlock.close();
                    }
                }
            }
        }
    }

    @Override
    public void resetState(final boolean postOptimization) {
        super.resetState(postOptimization);
        //Nothing more to do (previously was cache management)
    }
}
