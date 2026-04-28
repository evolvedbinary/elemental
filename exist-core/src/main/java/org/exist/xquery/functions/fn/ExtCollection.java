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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.exist.collections.Collection;
import org.exist.dom.persistent.*;
import org.exist.dom.QName;
import org.exist.numbering.NodeId;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.UpdateListener;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author wolf
 */
public class ExtCollection extends Function {

    private static final Logger LOG = LogManager.getLogger(ExtCollection.class);

    public final static FunctionSignature signature =
            new FunctionSignature(
                    new QName("collection", FnModule.NAMESPACE_URI),
                    "Returns the documents contained in the collections specified in " +
                            "the input sequence. " + XMLDBModule.COLLECTION_URI +
                            " Documents contained in sub-collections are also included. If no value is supplied, the statically know documents are used, for the REST Server this could be the addressed collection.",
                    new SequenceType[]{
                            //Different from the official specs
                            new FunctionParameterSequenceType("collection-uris", Type.STRING,
                                    Cardinality.ZERO_OR_MORE, "The collection-URIs for which to include the documents")},
                    new FunctionReturnSequenceType(Type.ITEM, Cardinality.ZERO_OR_MORE,
                            "The document nodes contained in or under the given collections"),
                    true);

    private final boolean includeSubCollections;
    private UpdateListener listener = null;

    public ExtCollection(final XQueryContext context) {
        this(context, signature, true);
    }

    public ExtCollection(final XQueryContext context, final FunctionSignature signature, final boolean inclusive) {
        super(context, signature);
        includeSubCollections = inclusive;
    }

    @Override
    public Sequence eval(final Sequence contextSequence, final Item contextItem) throws XPathException {
        if (context.getProfiler().isEnabled()) {
            context.getProfiler().start(this);
            context.getProfiler().message(this, Profiler.DEPENDENCIES, "DEPENDENCIES", Dependency.getDependenciesName(this.getDependencies()));
            if (contextSequence != null) {
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT SEQUENCE", contextSequence);
            }
            if (contextItem != null) {
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT ITEM", contextItem.toSequence());
            }
        }

        final List<String> args = getParameterValues(contextSequence, contextItem);

        @Nullable final List<URI> collectionUris;
        if (args.isEmpty()) {
            collectionUris = null;
        } else {
            collectionUris = new ArrayList<>(args.size());
            for (final String arg : args) {
                collectionUris.add(asUri(arg));
            }
        }

        final Sequence result = getCollectionItems(collectionUris);

        registerUpdateListener();
        if (context.getProfiler().isEnabled()) {
            context.getProfiler().end(this, "", result);
        }

        return result;
    }

    protected Sequence getCollectionItems(@Nullable final List<URI> collectionUris) throws XPathException {
        if (collectionUris == null) {
            // no collection-uri(s)
            return getDefaultCollectionItems();
        }

        return getCollectionUriItems(collectionUris);
    }

    private Sequence getDefaultCollectionItems() throws XPathException {
        final DocumentSet staticallyKnownDocuments = context.getStaticallyKnownDocuments();
        final Sequence items = new ValueSequence(staticallyKnownDocuments.getDocumentCount());
        addAll(staticallyKnownDocuments, items);

        @Nullable final Sequence dynamicCollection = context.getDynamicallyAvailableCollection("");
        if (dynamicCollection != null) {
            items.addAll(dynamicCollection);
        }

        return items;
    }

    private Sequence getCollectionUriItems(final List<URI> collectionUris) throws XPathException {
        @Nullable Sequence items = null;
        for (final URI collectionUri : collectionUris) {
            items = getCollectionUriItems(collectionUri, items);
        }

        if (items == null) {
            items = Sequence.EMPTY_SEQUENCE;
        }

        return items;
    }

    private @Nullable Sequence getCollectionUriItems(final URI collectionUri, @Nullable Sequence items) throws XPathException {
        @Nullable final Sequence dynamicCollection = context.getDynamicallyAvailableCollection(collectionUri.toString());
        if (dynamicCollection != null) {
            if (items == null) {
                items = dynamicCollection;
            } else {
                items.addAll(dynamicCollection);
            }

            return items;

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
            } catch (final XPathException e) {  // From AnyURIValue constructor
                throw new XPathException(this, ErrorCodes.FODC0002, e.getMessage(), new StringValue(collectionUri.toString()), e);
            } catch (final PermissionDeniedException e) {
                throw new XPathException(this, ErrorCodes.FODC0002, "Can not access collection '" + e.getMessage() + "'", new StringValue(collectionUri.toString()), e);
            } catch (final LockException e) {
                throw new XPathException(this, ErrorCodes.FODC0002, e.getMessage(), new StringValue(collectionUri.toString()), e);
            }

            if (docs == null || docs.getDocumentCount() == 0) {
                return Sequence.EMPTY_SEQUENCE;
            }

            if (items == null) {
                items = new ValueSequence(docs.getDocumentCount());
            } else {
                addAll(docs, items);
            }

            return items;
        }
    }

    private URI asUri(final String path) throws XPathException {
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

    private List<String> getParameterValues(final Sequence contextSequence, final Item contextItem) throws XPathException {
        final List<String> args = new ArrayList<>(getArgumentCount() + 10);
        for (int i = 0; i < getArgumentCount(); i++) {
            final Sequence seq = getArgument(i).eval(contextSequence, contextItem);
            for (final SequenceIterator j = seq.iterate(); j.hasNext(); ) {
                final Item next = j.nextItem();
                args.add(next.getStringValue());
            }
        }
        return args;
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

    protected void registerUpdateListener() {
        if (listener == null) {
            listener = new UpdateListener() {

                @Override
                public void documentUpdated(final DocumentImpl document, final int event) {
                    //Nothing to do (previously was cache management)
                }

                @Override
                public void unsubscribe() {
                    ExtCollection.this.listener = null;
                }

                @Override
                public void nodeMoved(final NodeId oldNodeId, final NodeHandle newNode) {
                    // not relevant
                }

                @Override
                public void debug() {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("UpdateListener: Line: {}: {}", getLine(), ExtCollection.this.toString());
                    }
                }
            };
            context.registerUpdateListener(listener);
        }
    }

    @Override
    public void resetState(final boolean postOptimization) {
        super.resetState(postOptimization);
        //Nothing more to do (previously was cache management)
    }
}
