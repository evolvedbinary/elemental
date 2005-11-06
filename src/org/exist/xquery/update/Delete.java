/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 The eXist Team
 *
 *  http://exist-db.org
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.xquery.update;

import org.exist.EXistException;
import org.exist.dom.DocumentImpl;
import org.exist.dom.DocumentSet;
import org.exist.dom.NodeImpl;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.NotificationService;
import org.exist.storage.UpdateListener;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.util.LockException;
import org.exist.xquery.Expression;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.util.Error;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.util.Messages;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.w3c.dom.Node;

/**
 * @author wolf
 *
 */
public class Delete extends Modification {

	/**
	 * @param context
	 * @param select
	 * @param value
	 */
	public Delete(XQueryContext context, Expression select) {
		super(context, select, null);
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.AbstractExpression#eval(org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
	 */
	public Sequence eval(Sequence contextSequence, Item contextItem)
			throws XPathException {
		if (contextItem != null)
			contextSequence = contextItem.toSequence();
		Sequence inSeq = select.eval(contextSequence);
		if (inSeq.getLength() == 0)
			return Sequence.EMPTY_SEQUENCE;
		if (!Type.subTypeOf(inSeq.getItemType(), Type.NODE))
			throw new XPathException(getASTNode(), Messages.getMessage(Error.UPDATE_SELECT_TYPE));
        TransactionManager transact = context.getBroker().getBrokerPool().getTransactionManager();
        Txn transaction = transact.beginTransaction();
		try {
			NotificationService notifier = context.getBroker().getBrokerPool().getNotificationService();
            NodeImpl[] ql = selectAndLock(inSeq.toNodeSet());
            IndexListener listener = new IndexListener(ql);
            NodeImpl node;
            NodeImpl parent;
            DocumentImpl doc = null;
            DocumentSet modifiedDocs = new DocumentSet();
            for (int i = 0; i < ql.length; i++) {
                node = ql[i];
                doc = (DocumentImpl) node.getOwnerDocument();
                if (!doc.getPermissions().validate(context.getUser(),
                        Permission.UPDATE)) {
                    transact.abort(transaction);    
                    throw new PermissionDeniedException("permission to remove document denied");
                }
                doc.setIndexListener(listener);
                modifiedDocs.add(doc);
                parent = (NodeImpl) node.getParentNode();
                if (parent.getNodeType() != Node.ELEMENT_NODE) {
                    LOG.debug("parent = " + parent.getNodeType() + "; " + parent.getNodeName());
                    transact.abort(transaction);
                    throw new XPathException(getASTNode(),
                            "you cannot remove the document element. Use update "
                                    + "instead");
                } else
                    parent.removeChild(transaction, node);
                doc.clearIndexListener();
                doc.setLastModified(System.currentTimeMillis());
                context.getBroker().storeDocument(transaction, doc);
                notifier.notifyUpdate(doc, UpdateListener.UPDATE);
            }
            checkFragmentation(transaction, modifiedDocs);
            transact.commit(transaction);
        } catch (EXistException e) {
            transact.abort(transaction);
            throw new XPathException(getASTNode(), e.getMessage(), e);
		} catch (PermissionDeniedException e) {
            transact.abort(transaction);
            throw new XPathException(getASTNode(), e.getMessage(), e);
		} catch (LockException e) {
            transact.abort(transaction);
            throw new XPathException(getASTNode(), e.getMessage(), e);
		} finally {
            unlockDocuments();
        }
		return Sequence.EMPTY_SEQUENCE;
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.Expression#dump(org.exist.xquery.util.ExpressionDumper)
	 */
	public void dump(ExpressionDumper dumper) {
		// TODO Auto-generated method stub

	}
	
	public String toString() {
		return "'Delete' string representation";
	}	

}
