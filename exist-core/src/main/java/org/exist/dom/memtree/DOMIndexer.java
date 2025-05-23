/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
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
package org.exist.dom.memtree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.Namespaces;
import org.exist.collections.CollectionConfiguration;
import org.exist.dom.QName;
import org.exist.dom.persistent.AttrImpl;
import org.exist.dom.persistent.CommentImpl;
import org.exist.dom.persistent.ElementImpl;
import org.exist.dom.persistent.ProcessingInstructionImpl;
import org.exist.dom.persistent.TextImpl;
import org.exist.dom.persistent.*;
import org.exist.numbering.NodeId;
import org.exist.storage.DBBroker;
import org.exist.storage.IndexSpec;
import org.exist.storage.NodePath;
import org.exist.storage.txn.Txn;
import org.exist.util.pool.NodePool;
import org.exist.xquery.Expression;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to make a in-memory document fragment persistent. The class
 * directly accesses the in-memory document structure and writes it into a
 * temporary doc on the database. This is much faster than first serializing
 * the document tree to SAX and passing it to {@link org.exist.collections.Collection#store(org.exist.storage.txn.Txn, org.exist.storage.DBBroker, org.exist.collections.IndexInfo, org.xml.sax.InputSource)}.
 *
 * As the in-memory document fragment may not be a well-formed XML doc (having more than one root element), a wrapper element is put around the
 * content nodes.
 *
 * @author wolf
 */
public class DOMIndexer {

    private static final Logger LOG = LogManager.getLogger(DOMIndexer.class);
    private static final QName ROOT_QNAME = new QName("temp", Namespaces.EXIST_NS, Namespaces.EXIST_NS_PREFIX);

    private final DBBroker broker;
    private final Txn transaction;
    private final DocumentImpl doc;
    private final org.exist.dom.persistent.DocumentImpl targetDoc;
    private final IndexSpec indexSpec;

    private final Deque<ElementImpl> stack = new ArrayDeque<>();
    private StoredNode prevNode = null;

    private final TextImpl text = new TextImpl((Expression) null);
    private final CommentImpl comment = new CommentImpl((Expression) null);
    private final ProcessingInstructionImpl pi = new ProcessingInstructionImpl(null);

    public DOMIndexer(final DBBroker broker, final Txn transaction, final DocumentImpl doc,
                      final org.exist.dom.persistent.DocumentImpl targetDoc) {
        this.broker = broker;
        this.transaction = transaction;
        this.doc = doc;
        this.targetDoc = targetDoc;
        final CollectionConfiguration config = targetDoc.getCollection().getConfiguration(broker);
        if(config != null) {
            this.indexSpec = config.getIndexConfiguration();
        } else {
            this.indexSpec = null;
        }
    }

    /**
     * Scan the DOM tree once to determine its structure.
     *
     * @throws EXistException DOCUMENT ME
     */
    public void scan() throws EXistException {
        //Creates a dummy DOCTYPE
        final org.exist.dom.persistent.DocumentTypeImpl dt = new org.exist.dom.persistent.DocumentTypeImpl((doc != null) ? doc.getExpression() : null, "temp", null, "");
        targetDoc.setDocumentType(dt);
    }

    /**
     * Store the nodes.
     */
    public void store() {
        //Create a wrapper element as root node
        final ElementImpl elem = new ElementImpl(null, ROOT_QNAME, broker.getBrokerPool().getSymbols());
        elem.setNodeId(broker.getBrokerPool().getNodeFactory().createInstance());
        elem.setOwnerDocument(targetDoc);
        elem.setChildCount(doc.getChildCount());
        elem.addNamespaceMapping(Namespaces.EXIST_NS_PREFIX, Namespaces.EXIST_NS);
        final NodePath path = new NodePath();
        path.addComponent(ROOT_QNAME);
        stack.push(elem);
        broker.storeNode(transaction, elem, path, indexSpec);
        targetDoc.appendChild((NodeHandle) elem);
        elem.setChildCount(0);
        // store the document nodes
        int top = (doc.size > 1) ? 1 : -1;
        while(top > 0) {
            store(top, path);
            top = doc.getNextSiblingFor(top);
        }
        //Close the wrapper element
        stack.pop();
        broker.endElement(elem, path, null);
        path.removeLastComponent();
    }

    private void store(final int top, final NodePath currentPath) {
        int nodeNr = top;

        while(nodeNr > 0) {
            startNode(nodeNr, currentPath);
            int nextNode = doc.getFirstChildFor(nodeNr);

            while(nextNode == -1) {
                endNode(nodeNr, currentPath);

                if(top == nodeNr) {
                    break;
                }
                nextNode = doc.getNextSiblingFor(nodeNr);

                if(nextNode == -1) {
                    nodeNr = doc.getParentNodeFor(nodeNr);

                    if((nodeNr == -1) || (top == nodeNr)) {
                        endNode(nodeNr, currentPath);
                        nextNode = -1;
                        break;
                    }
                }
            }
            nodeNr = nextNode;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param nodeNr
     * @param currentPath DOCUMENT ME!
     */
    private void startNode(final int nodeNr, final NodePath currentPath) {
        switch(doc.nodeKind[nodeNr]) {

            case Node.ELEMENT_NODE: {
                final ElementImpl elem = (ElementImpl) NodePool.getInstance().borrowNode(Node.ELEMENT_NODE);
                if(stack.isEmpty()) {
                    elem.setNodeId(broker.getBrokerPool().getNodeFactory().createInstance());
                    initElement(nodeNr, elem);
                    stack.push(elem);
                    broker.storeNode(transaction, elem, currentPath, indexSpec);
                    targetDoc.appendChild((NodeHandle) elem);
                    elem.setChildCount(0);
                } else {
                    final ElementImpl last = stack.peek();
                    initElement(nodeNr, elem);
                    last.appendChildInternal(prevNode, elem);
                    stack.push(elem);
                    broker.storeNode(transaction, elem, currentPath, indexSpec);
                    elem.setChildCount(0);
                }
                setPrevious(null);
                currentPath.addComponent(elem.getQName());
                storeAttributes(nodeNr, elem, currentPath);
                break;
            }

            case Node.TEXT_NODE: {
                if((prevNode != null) && ((prevNode.getNodeType() == Node.TEXT_NODE) || (prevNode.getNodeType() == Node.CDATA_SECTION_NODE))) {
                    break;
                }
                final ElementImpl last = stack.peek();
                text.setData(new String(doc.characters, doc.alpha[nodeNr], doc.alphaLen[nodeNr]));
                text.setOwnerDocument(targetDoc);
                last.appendChildInternal(prevNode, text);
                setPrevious(text);
                broker.storeNode(transaction, text, null, indexSpec);
                break;
            }

            case Node.CDATA_SECTION_NODE: {
                final ElementImpl last = stack.peek();
                final org.exist.dom.persistent.CDATASectionImpl cdata = (org.exist.dom.persistent.CDATASectionImpl) NodePool.getInstance().borrowNode(Node.CDATA_SECTION_NODE);
                cdata.setData(doc.characters, doc.alpha[nodeNr], doc.alphaLen[nodeNr]);
                cdata.setOwnerDocument(targetDoc);
                last.appendChildInternal(prevNode, cdata);
                setPrevious(cdata);
                broker.storeNode(transaction, cdata, null, indexSpec);
                break;
            }

            case Node.COMMENT_NODE: {
                comment.setData(doc.characters, doc.alpha[nodeNr], doc.alphaLen[nodeNr]);
                comment.setOwnerDocument(targetDoc);
                if(stack.isEmpty()) {
                    comment.setNodeId(NodeId.DOCUMENT_NODE);
                    targetDoc.appendChild((NodeHandle) comment);
                    broker.storeNode(transaction, comment, null, indexSpec);
                } else {
                    final ElementImpl last = stack.peek();
                    last.appendChildInternal(prevNode, comment);
                    broker.storeNode(transaction, comment, null, indexSpec);
                    setPrevious(comment);
                }
                break;
            }

            case Node.PROCESSING_INSTRUCTION_NODE: {
                final QName qn = doc.nodeName[nodeNr];
                pi.setTarget(qn.getLocalPart());
                pi.setData(new String(doc.characters, doc.alpha[nodeNr], doc.alphaLen[nodeNr]));
                pi.setOwnerDocument(targetDoc);
                if(stack.isEmpty()) {
                    pi.setNodeId(NodeId.DOCUMENT_NODE);
                    targetDoc.appendChild((NodeHandle) pi);
                } else {
                    final ElementImpl last = stack.peek();
                    last.appendChildInternal(prevNode, pi);
                    setPrevious(pi);
                }
                broker.storeNode(transaction, pi, null, indexSpec);
                break;
            }

            default: {
                LOG.debug("Skipped indexing of in-memory node of type {}", doc.nodeKind[nodeNr]);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param nodeNr
     * @param elem
     */
    private void initElement(final int nodeNr, final ElementImpl elem) {
        final short attribs = (short) doc.getAttributesCountFor(nodeNr);
        elem.setOwnerDocument(targetDoc);
        elem.setAttributes(attribs);
        elem.setChildCount(doc.getChildCountFor(nodeNr) + attribs);
        elem.setNodeName(doc.nodeName[nodeNr], broker.getBrokerPool().getSymbols());
        final Map<String, String> ns = getNamespaces(nodeNr);
        if(ns != null) {
            elem.setNamespaceMappings(ns);
        }
    }

    private Map<String, String> getNamespaces(final int nodeNr) {
        int ns = doc.alphaLen[nodeNr];

        if(ns < 0) {
            return null;
        }

        final Map<String, String> map = new HashMap<>();

        while((ns < doc.nextNamespace) && (doc.namespaceParent[ns] == nodeNr)) {
            final QName qn = doc.namespaceCode[ns];

            if(XMLConstants.XMLNS_ATTRIBUTE.equals(qn.getLocalPart())) {
                map.put(XMLConstants.DEFAULT_NS_PREFIX, qn.getNamespaceURI());
            } else {
                map.put(qn.getLocalPart(), qn.getNamespaceURI());
            }
            ++ns;
        }

        return map;
    }

    /**
     * DOCUMENT ME!
     *
     * @param nodeNr
     * @param elem
     * @param path   DOCUMENT ME!
     * @throws DOMException
     */
    private void storeAttributes(final int nodeNr, final ElementImpl elem, final NodePath path) throws DOMException {
        int attr = doc.alpha[nodeNr];
        if(attr > -1) {
            while((attr < doc.nextAttr) && (doc.attrParent[attr] == nodeNr)) {
                final QName qn = doc.attrName[attr];
                final AttrImpl attrib = (AttrImpl) NodePool.getInstance().borrowNode(Node.ATTRIBUTE_NODE);
                attrib.setNodeName(qn, broker.getBrokerPool().getSymbols());
                attrib.setValue(doc.attrValue[attr]);
                attrib.setOwnerDocument(targetDoc);
                elem.appendChildInternal(prevNode, attrib);
                setPrevious(attrib);
                broker.storeNode(transaction, attrib, path, indexSpec);
                ++attr;
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param nodeNr
     * @param currentPath DOCUMENT ME!
     */
    private void endNode(final int nodeNr, final NodePath currentPath) {
        if(doc.nodeKind[nodeNr] == Node.ELEMENT_NODE) {
            final ElementImpl last = stack.pop();
            broker.endElement(last, currentPath, null);
            currentPath.removeLastComponent();
            setPrevious(last);
        }
    }

    private void setPrevious(final StoredNode previous) {
        if(prevNode != null && (prevNode.getNodeType() == Node.TEXT_NODE || prevNode.getNodeType() == Node.COMMENT_NODE || prevNode.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE)) {
            if(previous == null || prevNode.getNodeType() != previous.getNodeType()) {
                prevNode.clear();
            }
        }
        prevNode = previous;
    }
}
