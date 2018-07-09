/*
 * eXist Open Source Native XML Database
 * Copyright (C) 2004-2009 The eXist Project
 * http://exist-db.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *  $Id$
 */
package org.exist.xquery.functions.fn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.exist.collections.Collection;
import org.exist.dom.persistent.DefaultDocumentSet;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.DocumentSet;
import org.exist.dom.persistent.MutableDocumentSet;
import org.exist.dom.persistent.NewArrayNodeSet;
import org.exist.dom.persistent.NodeHandle;
import org.exist.dom.persistent.NodeProxy;
import org.exist.dom.persistent.NodeSet;
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
import org.exist.xquery.value.AnyURIValue;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;

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
                    new QName("collection", Function.BUILTIN_FUNCTION_NS),
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
        // TODO: disabled cache for now as it may cause concurrency issues
        // better use compile-time inspection and maybe a pragma to mark those
        // sections in the query that can be safely cached
        // boolean cacheIsValid = false;
        // if (cachedArgs != null)
        //    cacheIsValid = compareArguments(cachedArgs, args);
        // if (cacheIsValid) {
        //If the expression occurs in a nested context, we might have cached the
        //document set
        //if (context.getProfiler().isEnabled())
        //context.getProfiler().end(this, "fn:collection: loading documents", cached);
        //return cached;
        // }
        //Build the document set
        final DocumentSet docs;
        try {
            if (args.size() == 0) {
                //TODO : add default collection to the context
                //If the value of the default collection is undefined an error is raised [err:FODC0002].
                docs = context.getStaticallyKnownDocuments();
            } else {
                MutableDocumentSet ndocs = new DefaultDocumentSet();
                for (final String next : args) {
                    final XmldbURI uri = new AnyURIValue(next).toXmldbURI();
                    try (final Collection coll = context.getBroker().openCollection(uri, Lock.LockMode.READ_LOCK)) {
                        if (coll == null) {
                            if (context.isRaiseErrorOnFailedRetrieval()) {
                                throw new XPathException(this, ErrorCodes.FODC0002, "Can not access collection '" + uri + "'");
                            }
                        } else {
                            if (context.inProtectedMode()) {
                                context.getProtectedDocs().getDocsByCollection(coll, ndocs);
                            } else {
                                coll.allDocs(context.getBroker(), ndocs,
                                        includeSubCollections, context.getProtectedDocs());
                            }
                        }
                    }
                }
                docs = ndocs;
            }
        } catch (final XPathException e) { //From AnyURIValue constructor
            e.setLocation(line, column);
            throw new XPathException(this, ErrorCodes.FODC0002, e.getMessage());
        } catch (final PermissionDeniedException e) {
            throw new XPathException(this, ErrorCodes.FODC0002, "Can not access collection '" + e.getMessage() + "'");
        } catch (final LockException e) {
            throw new XPathException(this, ErrorCodes.FODC0002, e);

        }
        // iterate through all docs and create the node set
        final NodeSet result = new NewArrayNodeSet();
        final LockManager lockManager = context.getBroker().getBrokerPool().getLockManager();
        for (final Iterator<DocumentImpl> i = docs.getDocumentIterator(); i.hasNext(); ) {
            final DocumentImpl doc = i.next();

            ManagedDocumentLock dlock = null;
            try {
                if (!context.inProtectedMode()) {
                    dlock = lockManager.acquireDocumentReadLock(doc.getURI());
                }
                result.add(new NodeProxy(doc)); // , -1, Node.DOCUMENT_NODE));
            } catch (final LockException e) {
                throw new XPathException(this, ErrorCodes.FODC0002, e);
            } finally {
                if (dlock != null) {
                    dlock.close();
                }
            }
        }
        registerUpdateListener();
        if (context.getProfiler().isEnabled()) {
            context.getProfiler().end(this, "", result);
        }
        return result;
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
                        LOG.debug("UpdateListener: Line: " + getLine() + ": " + ExtCollection.this.toString());
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
