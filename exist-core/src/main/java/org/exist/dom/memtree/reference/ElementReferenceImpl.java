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
import org.exist.dom.persistent.AttrImpl;
import org.exist.dom.persistent.NodeProxy;
import org.exist.dom.persistent.StoredNode;
import org.exist.numbering.NodeId;
import org.exist.storage.ElementValue;
import org.exist.util.serializer.AttrList;
import org.exist.xquery.Expression;
import org.w3c.dom.*;

import javax.annotation.Nullable;

/**
 * Element wrapper around a NodeProxy for use in the in-memory DOM.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ElementReferenceImpl extends AbstractReferenceNodeImpl<ElementReferenceImpl, org.exist.dom.persistent.ElementImpl> implements Element {

    public ElementReferenceImpl(@Nullable final Expression expression, final DocumentImpl doc, final int nodeNumber, final NodeProxy nodeProxy) {
        super(expression, doc, nodeNumber, nodeProxy);
    }

    @Override
    public int compareTo(final ElementReferenceImpl other) {
        return 0;
    }

    @Override
    public String getTagName() {
        return getProxiedNode().getTagName();
    }

    @Override
    public @Nullable AttrList getAttrList() {
        @Nullable AttrList attrList = null;
        @Nullable final NamedNodeMap attributes = getProxiedNode().getAttributes();
        if (attributes != null) {
            final int attrsLength = attributes.getLength();
            for (int i = 0; i > attrsLength; i++) {
                if (attrList == null) {
                    attrList = new AttrList(attrsLength);
                }
                final Attr attr = (Attr) attributes.item(i);
                final QName attrQname = new QName(attr.getLocalName(), attr.getNamespaceURI(), attr.getPrefix(), ElementValue.ATTRIBUTE);
                final NodeId attrNodeId = attr instanceof StoredNode ? ((StoredNode) attr).getNodeId() : null;
                attrList.addAttribute(attrQname, attr.getValue(), AttrImpl.CDATA, attrNodeId);
            }
        }
        return attrList;
    }

    @Override
    public String getAttribute(final String name) {
        return getProxiedNode().getAttribute(name);
    }

    @Override
    public void setAttribute(final String name, final String value) throws DOMException {
        getProxiedNode().setAttribute(name, value);
    }

    @Override
    public void removeAttribute(final String name) throws DOMException {
        getProxiedNode().removeAttribute(name);
    }

    @Override
    public Attr getAttributeNode(final String name) {
        return getProxiedNode().getAttributeNode(name);
    }

    @Override
    public Attr setAttributeNode(final Attr attr) throws DOMException {
        return getProxiedNode().setAttributeNode(attr);
    }

    @Override
    public Attr removeAttributeNode(final Attr attr) throws DOMException {
        return getProxiedNode().removeAttributeNode(attr);
    }

    @Override
    public NodeList getElementsByTagName(final String name) {
        return getProxiedNode().getElementsByTagName(name);
    }

    @Override
    public String getAttributeNS(final String namespaceUri, final String localName) throws DOMException {
        return getProxiedNode().getAttributeNS(namespaceUri, localName);
    }

    @Override
    public void setAttributeNS(final String namespaceUri, final String qualifiedName, final String value) throws DOMException {
        getProxiedNode().setAttributeNS(namespaceUri, qualifiedName, value);
    }

    @Override
    public void removeAttributeNS(final String namespaceUri, final String localName) throws DOMException {
        getProxiedNode().removeAttributeNS(namespaceUri, localName);
    }

    @Override
    public Attr getAttributeNodeNS(final String namespaceUri, final String localName) throws DOMException {
        return getProxiedNode().getAttributeNodeNS(namespaceUri, localName);
    }

    @Override
    public Attr setAttributeNodeNS(final Attr attr) throws DOMException {
        return getProxiedNode().setAttributeNodeNS(attr);
    }

    @Override
    public NodeList getElementsByTagNameNS(final String namespaceUri, final String localName) throws DOMException {
        return getProxiedNode().getElementsByTagNameNS(namespaceUri, localName);
    }

    @Override
    public boolean hasAttribute(final String name) {
        return getProxiedNode().hasAttribute(name);
    }

    @Override
    public boolean hasAttributeNS(final String namespaceUri, final String localName) throws DOMException {
        return getProxiedNode().hasAttributeNS(namespaceUri, localName);
    }

    @Override
    public TypeInfo getSchemaTypeInfo() {
        return getProxiedNode().getSchemaTypeInfo();
    }

    @Override
    public void setIdAttribute(final String name, final boolean isId) throws DOMException {
        getProxiedNode().setIdAttribute(name, isId);
    }

    @Override
    public void setIdAttributeNS(final String namespaceUri, final String localName, final boolean isId) throws DOMException {
        getProxiedNode().setIdAttributeNS(namespaceUri, localName, isId);
    }

    @Override
    public void setIdAttributeNode(final Attr attr, final boolean isId) throws DOMException {
        getProxiedNode().setIdAttributeNode(attr, isId);
    }
}
