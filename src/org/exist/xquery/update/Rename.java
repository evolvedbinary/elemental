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
import org.exist.dom.AttrImpl;
import org.exist.dom.DocumentImpl;
import org.exist.dom.DocumentSet;
import org.exist.dom.ElementImpl;
import org.exist.dom.NodeImpl;
import org.exist.dom.QName;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.NotificationService;
import org.exist.storage.UpdateListener;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.util.LockException;
import org.exist.xquery.Dependency;
import org.exist.xquery.Expression;
import org.exist.xquery.Profiler;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.util.Error;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.util.Messages;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.QNameValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.w3c.dom.Node;

/**
 * @author wolf
 *
 */
public class Rename extends Modification {

	/**
	 * @param context
	 * @param select
	 * @param value
	 */
	public Rename(XQueryContext context, Expression select, Expression value) {
		super(context, select, value);
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.AbstractExpression#eval(org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
	 */
	public Sequence eval(Sequence contextSequence, Item contextItem) throws XPathException {
        if (context.getProfiler().isEnabled()) {
            context.getProfiler().start(this);       
            context.getProfiler().message(this, Profiler.DEPENDENCIES, "DEPENDENCIES", Dependency.getDependenciesName(this.getDependencies()));
            if (contextSequence != null)
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT SEQUENCE", contextSequence);
            if (contextItem != null)
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT ITEM", contextItem.toSequence());
        }
        
		if (contextItem != null)
			contextSequence = contextItem.toSequence();
		Sequence inSeq = select.eval(contextSequence);
		if (inSeq.getLength() == 0)
			return Sequence.EMPTY_SEQUENCE;
		if (!Type.subTypeOf(inSeq.getItemType(), Type.NODE))
			throw new XPathException(getASTNode(), Messages.getMessage(Error.UPDATE_SELECT_TYPE));
		
		Sequence contentSeq = value.eval(contextSequence);
		if (contentSeq.getLength() == 0)
			throw new XPathException(getASTNode(), Messages.getMessage(Error.UPDATE_EMPTY_CONTENT));
        
        QName newQName;
        Item item = contentSeq.itemAt(0);
        if (item.getType() == Type.QNAME) {
            newQName = ((QNameValue) item).getQName();
        } else {
            newQName = QName.parse(context, item.getStringValue());
        }

		try {
            TransactionManager transact = context.getBroker().getBrokerPool().getTransactionManager();
            Txn transaction = transact.beginTransaction();
            NodeImpl[] ql = selectAndLock(inSeq.toNodeSet());
            DocumentImpl doc = null;
            DocumentSet modifiedDocs = new DocumentSet();
            NodeImpl node;
            NodeImpl parent;
            IndexListener listener = new IndexListener(ql);
            NotificationService notifier = context.getBroker().getBrokerPool().getNotificationService();
            for (int i = 0; i < ql.length; i++) {
                node = ql[i];
                doc = (DocumentImpl) node.getOwnerDocument();
                if (!doc.getPermissions().validate(context.getUser(),
                        Permission.UPDATE))
                        throw new XPathException(getASTNode(),
                                "permission denied to update document");
                doc.setIndexListener(listener);
                modifiedDocs.add(doc);
                parent = (NodeImpl) node.getParentNode();
                switch (node.getNodeType()) {
                    case Node.ELEMENT_NODE:
                        ElementImpl newElem = new ElementImpl((ElementImpl) node);
                        newElem.setNodeName(newQName);
                        parent.updateChild(transaction, node, newElem);
                        break;
                    case Node.ATTRIBUTE_NODE:
                        AttrImpl newAttr = new AttrImpl((AttrImpl) node);
                        newAttr.setNodeName(newQName);
                        parent.updateChild(transaction, node, newAttr);
                        break;
                    default:
                        throw new XPathException(getASTNode(), "unsupported node-type");
                }

                doc.clearIndexListener();
                doc.setLastModified(System.currentTimeMillis());
                context.getBroker().storeDocument(transaction, doc);
                notifier.notifyUpdate(doc, UpdateListener.UPDATE);
            }
            checkFragmentation(transaction, modifiedDocs);
            transact.commit(transaction);
        } catch (PermissionDeniedException e) {
            throw new XPathException(getASTNode(), e.getMessage(), e);
		} catch (EXistException e) {
            throw new XPathException(getASTNode(), e.getMessage(), e);
		} catch (LockException e) {
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
		dumper.display("update rename").nl();
		dumper.startIndent();
		select.dump(dumper);
		dumper.endIndent();
		dumper.nl().display(" to ").nl();
		dumper.startIndent();
		value.dump(dumper);
		dumper.nl().endIndent();
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("update rename ");		
		result.append(select.toString());		
		result.append(" to ");		
		result.append(value.toString());
		return result.toString();
	}	

}
