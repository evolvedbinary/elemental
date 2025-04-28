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
 */
package org.exist.dom.memtree.reference;

import org.exist.dom.QName;
import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.memtree.NodeImpl;
import org.exist.dom.persistent.AttrImpl;
import org.exist.dom.persistent.NodeProxy;
import org.exist.xquery.Expression;
import org.exist.xquery.NodeTest;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Sequence;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;

/**
 * DOM Wrapper around a NodeProxy for use in the in-memory DOM.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractReferenceNodeImpl<T extends AbstractReferenceNodeImpl<T, P>, P extends org.exist.dom.persistent.NodeImpl<P>> extends NodeImpl<T> {

    protected final NodeProxy nodeProxy;

    public AbstractReferenceNodeImpl(@Nullable final Expression expression, final DocumentImpl doc, final int nodeNumber, final NodeProxy nodeProxy) {
        super(expression, doc, nodeNumber);
        this.nodeProxy = nodeProxy;
    }

    /**
     * Get the node proxy.
     *
     * @return the node proxy.
     */
    public NodeProxy getNodeProxy() {
        return nodeProxy;
    }

    @SuppressWarnings("unchchecked")
    protected P getProxiedNode() {
        return (P) nodeProxy.getNode();
    }

    @Override
    public String toString() {
        return "reference[ " + getProxiedNode().toString() + " ]";
    }

    @Override
    public String getNamespaceURI() {
        return getProxiedNode().getNamespaceURI();
    }

    @Override
    public String getLocalName() {
        return getProxiedNode().getLocalName();
    }

    @Override
    public NamedNodeMap getAttributes() {
        return getProxiedNode().getAttributes();
    }

    @Override
    public NodeList getChildNodes() {
        return getProxiedNode().getChildNodes();
    }

    @Override
    public Node getFirstChild() {
        return getProxiedNode().getFirstChild();
    }

    @Override
    public Node getLastChild() {
        return getProxiedNode().getLastChild();
    }

    @Override
    public boolean hasAttributes() {
        return getProxiedNode().hasAttributes();
    }

    @Override
    public void selectAttributes(final NodeTest test, final Sequence result) throws XPathException {
        selectAttributes(getProxiedNode(), test, result);
    }

    @Override
    public boolean hasChildNodes() {
        return getProxiedNode().hasChildNodes();
    }

    private void selectAttributes(final Node node, final NodeTest test, final Sequence result) throws XPathException {
        @Nullable final NamedNodeMap attrs = node.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                final Node attr = attrs.item(i);
                if (test.matches(attr)) {
                    result.add(new NodeProxy((org.exist.dom.persistent.AttrImpl) attr));
                }
            }
        }
    }

    @Override
    public void selectChildren(final NodeTest test, final Sequence result) throws XPathException {
        @Nullable final NodeList children = getProxiedNode().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (test.matches(child)) {
                result.add(new NodeProxy((org.exist.dom.persistent.NodeHandle) child));
            }
        }
    }

    @Override
    public void selectDescendantAttributes(final NodeTest test, final Sequence result) throws XPathException {
        selectDescendantAttributes(getProxiedNode(), test, result);
    }

    private void selectDescendantAttributes(final Node node, final NodeTest test, final Sequence result) throws XPathException {
        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            selectAttributes(child, test, result);
            selectDescendantAttributes(child, test, result);
        }
    }

    @Override
    public String getNodeValue() throws DOMException {
        return getProxiedNode().getNodeValue();
    }

    @Override
    public short getNodeType() {
        return getProxiedNode().getNodeType();
    }

    @Override
    public QName getQName() {
        return getProxiedNode().getQName();
    }
}