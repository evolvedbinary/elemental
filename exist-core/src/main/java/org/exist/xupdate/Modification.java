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
package org.exist.xupdate;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.evolvedbinary.j8fu.function.ConsumerE;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.ManagedLocks;
import org.exist.collections.triggers.DocumentTrigger;
import org.exist.collections.triggers.DocumentTriggers;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.DefaultDocumentSet;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.DocumentSet;
import org.exist.dom.persistent.MutableDocumentSet;
import org.exist.dom.persistent.NodeSet;
import org.exist.dom.persistent.StoredNode;
import org.exist.security.PermissionDeniedException;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.ManagedDocumentLock;
import org.exist.storage.txn.Txn;
import org.exist.util.LockException;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;

/**
 * Base class for all XUpdate modifications.
 * 
 * @author Wolfgang Meier
 */
public abstract class Modification {

	protected final static Logger LOG = LogManager.getLogger(Modification.class);

	/** select Statement in the current XUpdate definition;
	 * defines the set of nodes to which this XUpdate might apply. */
	protected String selectStmt = null;
	
    /**
     * NodeList to keep track of created document fragments within
     * the currently processed XUpdate modification.
     * see {@link XUpdateProcessor#contents}
     */
	protected NodeList content = null;
	protected DBBroker broker;
	/** Documents concerned by this XUpdate modification,
	 * i.e. the set of documents to which this XUpdate might apply. */
	@Nullable protected final DocumentSet docs;
	@Nullable private Map<String, String> namespaces = null;
	@Nullable private Map<String, Object> variables = null;
	@Nullable private ManagedLocks<ManagedDocumentLock> lockedDocumentsLocks = null;
	@Nullable private MutableDocumentSet modifiedDocuments = null;
    @Nullable private Int2ObjectMap<DocumentTrigger> triggers = null;

	@SuppressWarnings("unused")
	private Modification() {
        this.docs = null;
    }

	/**
	 * Constructor for Modification.
	 *
	 * @param broker the database broker
	 * @param docs the document set
	 * @param selectStmt the select statement
	 * @param namespaces the namespace bindings
	 * @param variables the variable bindings
	 */
	public Modification(final DBBroker broker, final DocumentSet docs, final String selectStmt, @Nullable final Map<String, String> namespaces, @Nullable final Map<String, Object> variables) {
		this.selectStmt = selectStmt;
		this.broker = broker;
		this.docs = docs;
        if (namespaces != null) {
            this.namespaces = new Object2ObjectArrayMap<>(namespaces);
        }
        if (variables != null) {
            this.variables = new Object2ObjectRBTreeMap<>(variables);
        }
	}

	/**
     * Process the modification. This is the main method that has to be implemented 
     * by all subclasses.
     * 
     * @param transaction the database transaction
	 * @return long the number of updates processed
	 *
     * @throws PermissionDeniedException if the caller has insufficient priviledges
     * @throws LockException if a lock error occurs
     * @throws EXistException if the database raises an error
     * @throws XPathException if the XPath raises an error
	 * @throws TriggerException if a trigger raises an error
	 */
	public abstract long process(final Txn transaction) throws PermissionDeniedException, LockException, EXistException, XPathException, TriggerException;

	public abstract String getName();

	public void setContent(final NodeList nodes) {
		this.content = nodes;
	}

	/**
	 * Evaluate the select expression.
	 * 
	 * @param docs the documents to evaludate the expression over
	 *
	 * @return The selected nodes.
	 *
	 * @throws PermissionDeniedException if the caller has insufficient priviledges
	 * @throws EXistException if the database raises an error
	 * @throws XPathException if the XPath raises an error
	 */
	protected NodeList select(final DocumentSet docs) throws PermissionDeniedException, EXistException, XPathException {
		final Source source = new StringSource(selectStmt);

		final ConsumerE<XQueryContext, XPathException> setupXqueryContextPreCompilation = xqueryContext -> {
			xqueryContext.setStaticallyKnownDocuments(docs);
			declareNamespaces(xqueryContext);
			declareVariables(xqueryContext);
		};

		try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, source, true, null, null, setupXqueryContextPreCompilation, null, null)) {
			final Sequence resultSeq = queryResult.result;
			if (!(resultSeq.isEmpty() || Type.subTypeOf(resultSeq.getItemType(), Type.NODE))) {
				throw new EXistException("select expression should evaluate to a node-set; got " +
					Type.getTypeName(resultSeq.getItemType()));
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("found {} for select: {}", resultSeq.getItemCount(), selectStmt);
			}

			return resultSeq.toNodeSet();
		} catch (final IOException e) {
			throw new EXistException("An exception occurred while compiling the query: " + e.getMessage());
		}
	}

	/**
	 * @param context the xquery context
	 * @throws XPathException if an error occurs whilst declaring the variables
	 */
	protected void declareVariables(final XQueryContext context) throws XPathException {
        if (variables != null) {
            for (final Map.Entry<String, Object> entry : variables.entrySet()) {
                context.declareVariable(entry.getKey(), true, entry.getValue());
            }
        }
	}

	/**
	 * @param context the xquery context
	 * @throws XPathException if an error occurs whilst declaring the namespaces
	 */
	protected void declareNamespaces(final XQueryContext context) throws XPathException {
        if (namespaces != null) {
            for (final Map.Entry<String, String> entry : namespaces.entrySet()) {
                context.declareNamespace(entry.getKey(), entry.getValue());
            }
        }
	}

	/**
	 * Acquire a lock on all documents processed by this modification. We have
	 * to avoid that node positions change during the operation.
	 * feature trigger_update :
	 * At the same time we leverage on the fact that it's called before 
	 * database modification to call the eventual triggers.
	 *
	 * @param transaction the database transaction.
	 * 
	 * @return The selected document nodes.
	 *
	 * @throws LockException if a lock error occurs
	 * @throws PermissionDeniedException if the caller has insufficient priviledges
	 * @throws EXistException if the database raises an error
	 * @throws XPathException if the XPath raises an error
	 * @throws TriggerException if a trigger raises an error
	 */
	protected final StoredNode[] selectAndLock(final Txn transaction) throws LockException, PermissionDeniedException, EXistException, XPathException, TriggerException {
		final java.util.concurrent.locks.Lock globalLock = broker.getBrokerPool().getGlobalUpdateLock();
		globalLock.lock();
	    try {
	        final NodeList nl = select(docs);
	        final DocumentSet lockedDocuments = ((NodeSet)nl).getDocumentSet();
	        
		    // acquire a lock on all documents
	        // we have to avoid that node positions change
	        // during the modification
	        lockedDocumentsLocks = lockedDocuments.lock(broker, true);
	        
		    final StoredNode[] ql = new StoredNode[nl.getLength()];
			for (int i = 0; i < ql.length; i++) {
				ql[i] = (StoredNode) nl.item(i);
				final DocumentImpl doc = ql[i].getOwnerDocument();
				
				// call the eventual triggers
				// TODO -jmv separate loop on docs and not on nodes
			
				//prepare Trigger
				prepareTrigger(transaction, doc);
			}
			return ql;
	    } finally {
	        globalLock.unlock();
	    }
	}

	/**
	 * Release all acquired document locks;
	 * feature trigger_update :
	 * at the same time we leverage on the fact that it's called after 
	 * database modification to call the eventual triggers
	 *
	 * @param transaction the database transaction.
	 *
	 * @throws TriggerException if a trigger raises an error
	 */
	protected final void unlockDocuments(final Txn transaction) throws TriggerException {
		if (lockedDocumentsLocks == null) {
			return;
		}

		try {
			//finish Trigger
            if (modifiedDocuments != null) {
                final Iterator<DocumentImpl> iterator = modifiedDocuments.getDocumentIterator();
                while (iterator.hasNext()) {
                    finishTrigger(transaction, iterator.next());
                }
            }
		} finally {
            if (triggers != null) {
                triggers.clear();
            }
            if (modifiedDocuments != null) {
                modifiedDocuments.clear();
            }

			//unlock documents
            if (lockedDocumentsLocks != null) {
                lockedDocumentsLocks.close();
                lockedDocumentsLocks = null;
            }
		}
	}
	
	/**
	 * Check if any of the modified documents needs defragmentation.
	 * 
	 * Defragmentation will take place if the number of split pages in the
	 * document exceeds the limit defined in the configuration file.
	 *
	 * @param transaction the database transaction.
	 *
	 * @throws EXistException if an error occurs
	 */
	protected void checkFragmentation(final Txn transaction) throws EXistException {
        int fragmentationLimit = -1;
        final Object property = broker.getBrokerPool().getConfiguration().getProperty(DBBroker.PROPERTY_XUPDATE_FRAGMENTATION_FACTOR);
        if (property != null) {
            fragmentationLimit = (Integer) property;
        }

        if (modifiedDocuments != null) {
            for (final Iterator<DocumentImpl> i = modifiedDocuments.getDocumentIterator(); i.hasNext(); ) {
                final DocumentImpl next = i.next();
                if (next.getSplitCount() > fragmentationLimit) {
                    broker.defragXMLResource(transaction, next);
                }
                broker.checkXMLResourceConsistency(next);
            }
        }
	}
	
	/**
	 * Fires the prepare function for the UPDATE_DOCUMENT_EVENT trigger for the Document doc
	 *  
	 * @param transaction The database transaction
	 * @param doc The document to trigger for
	 *
	 * @throws TriggerException if a trigger raises an error
	 */
	private void prepareTrigger(final Txn transaction, final DocumentImpl doc) throws TriggerException {
	    final Collection col = doc.getCollection();
        final DocumentTrigger trigger = new DocumentTriggers(broker, transaction, col);
        trigger.beforeUpdateDocument(broker, transaction, doc);
        if (triggers == null) {
            triggers = new Int2ObjectOpenHashMap<>();
        }
        triggers.put(doc.getDocId(), trigger);
	}
	
	/** 
	 * Fires the finish function for UPDATE_DOCUMENT_EVENT for the documents trigger
	 * 
	 * @param transaction The transaction
	 * @param doc The document to trigger for
	 *
	 * @throws TriggerException if a trigger raises an error
	 */
	private void finishTrigger(final Txn transaction, final DocumentImpl doc) throws TriggerException {
        if (triggers != null) {
            final DocumentTrigger trigger = triggers.get(doc.getDocId());
            if (trigger != null) {
                trigger.afterUpdateDocument(broker, transaction, doc);
            }
        }
	}

    @Override
	public String toString() {
		//		buf.append(XMLUtil.dump(content));
		return "<xu:" + getName() + " select=\"" + selectStmt + "\">" + "</xu:" +	getName() +	">";
	}

	/**
	 * Get's the parent of the node.
	 *
	 * @param node The node of which to retrieve the parent.
	 *
	 * @return the parent node, or null if not available
	 */
	protected @Nullable Node getParent(@Nullable final Node node) {
		if (node == null) {
			return null;
		} else if (node instanceof Attr) {
			return ((Attr) node).getOwnerElement();
		} else {
			return node.getParentNode();
		}
	}

    protected void addModifiedDocument(final DocumentImpl document) {
        if (modifiedDocuments == null) {
            modifiedDocuments = new DefaultDocumentSet();
        }
        modifiedDocuments.add(document);
    }
}
