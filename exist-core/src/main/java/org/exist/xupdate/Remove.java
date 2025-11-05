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

import org.exist.EXistException;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.DocumentSet;
import org.exist.dom.persistent.NodeImpl;
import org.exist.dom.persistent.StoredNode;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.storage.NotificationService;
import org.exist.storage.UpdateListener;
import org.exist.storage.txn.Txn;
import org.exist.util.LockException;
import org.exist.xquery.XPathException;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Implements an XUpdate remove operation.
 * 
 * @author Wolfgang Meier
 */
public class Remove extends Modification {

    /**
     * Constructor for Remove.
     *
	 * @param broker the database broker.
	 * @param docs the document working set.
	 * @param selectStmt the select statement.
	 * @param namespaces the namespaces.
	 * @param variables the variables.
     */
	public Remove(final DBBroker broker, final DocumentSet docs, final String selectStmt, @Nullable final Map<String, String> namespaces, @Nullable final Map<String, Object> variables) {
		super(broker, docs, selectStmt, namespaces, variables);
	}

	@Override
	public long process(final Txn transaction) throws PermissionDeniedException, LockException, EXistException, XPathException, TriggerException {
		try {
			final StoredNode[] ql = selectAndLock(transaction);
			final NotificationService notifier = broker.getBrokerPool().getNotificationService();
            for (final StoredNode node : ql) {
                final DocumentImpl doc = node.getOwnerDocument();
                if (!doc.getPermissions().validate(broker.getCurrentSubject(), Permission.WRITE)) {
                    throw new PermissionDeniedException("User '" + broker.getCurrentSubject().getName() + "' does not have permission to write to the document '" + doc.getDocumentURI() + "'!");
                }

                final NodeImpl parent = (NodeImpl) getParent(node);
                if (parent == null || parent.getNodeType() != Node.ELEMENT_NODE) {
                    throw new EXistException("You cannot remove the document element. Use update instead");
                } else {
                    parent.removeChild(transaction, node);
                }
                doc.setLastModified(System.currentTimeMillis()); addModifiedDocument(doc);
                broker.storeXMLResource(transaction, doc);
                notifier.notifyUpdate(doc, UpdateListener.UPDATE);
            }
			checkFragmentation(transaction);
			return ql.length;
		} finally {
			unlockDocuments(transaction);
		}
	}

	@Override
	public String getName() {
		return "remove";
	}

}