/*
 * eXist Open Source Native XML Database Copyright (C) 2001-04 Wolfgang M. Meier
 * wolfgang@exist-db.org http://exist.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 * $Id$
 */
package org.exist.xupdate;

import java.util.Map;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.DocumentImpl;
import org.exist.dom.DocumentSet;
import org.exist.dom.NodeImpl;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.util.LockException;
import org.exist.xquery.XPathException;
import org.w3c.dom.NodeList;

/**
 * Implements an XUpdate insert-after or insert-before modification.
 * 
 * @author Wolfgang Meier
 */
public class Insert extends Modification {

    public final static int INSERT_BEFORE = 0;

    public final static int INSERT_AFTER = 1;

    private int mode = INSERT_BEFORE;

    /**
     * Constructor for Insert.
     * 
     * @param pool
     * @param user
     * @param selectStmt
     */
    public Insert(DBBroker broker, DocumentSet docs, String selectStmt,
            Map namespaces, Map variables) {
        super(broker, docs, selectStmt, namespaces,  variables);
    }

    public Insert(DBBroker broker, DocumentSet docs, String selectStmt,
            int mode, Map namespaces, Map variables) {
        this(broker, docs, selectStmt, namespaces, variables);
        this.mode = mode;
    }

    /**
     * @see org.exist.xupdate.Modification#process(org.exist.dom.DocumentSet)
     */
    public long process() throws PermissionDeniedException, LockException,
            EXistException, XPathException {
        NodeList children = content;
        if (children.getLength() == 0) return 0;
        try {
            NodeImpl[] ql = selectAndLock();
            IndexListener listener = new IndexListener(ql);
            NodeImpl node;
            NodeImpl parent;
            DocumentImpl doc = null;
            Collection collection = null, prevCollection = null;
            DocumentSet modifiedDocs = new DocumentSet();
            int len = children.getLength();
            LOG.debug("found " + len + " nodes to insert");
            for (int i = 0; i < ql.length; i++) {
                node = ql[i];
                doc = (DocumentImpl) node.getOwnerDocument();
                doc.setIndexListener(listener);
                collection = doc.getCollection();
                if (prevCollection != null && collection != prevCollection)
                        doc.getBroker().saveCollection(prevCollection);
                if (!doc.getPermissions().validate(broker.getUser(),
                        Permission.UPDATE))
                        throw new PermissionDeniedException(
                                "permission to remove document denied");
                modifiedDocs.add(doc);
                parent = (NodeImpl) node.getParentNode();
                switch (mode) {
                    case INSERT_BEFORE:
                        parent.insertBefore(children, node);
                        break;
                    case INSERT_AFTER:
                        ((NodeImpl) parent).insertAfter(children, node);
                        break;
                }
                doc.clearIndexListener();
                doc.setLastModified(System.currentTimeMillis());
                prevCollection = collection;
            }
            if (doc != null) doc.getBroker().saveCollection(collection);
            checkFragmentation(modifiedDocs);
            return ql.length;
        } finally {
            unlockDocuments();
        }
    }

    /**
     * @see org.exist.xupdate.Modification#getName()
     */
    public String getName() {
        return (mode == INSERT_BEFORE ? "insert-before" : "insert-after");
    }

}