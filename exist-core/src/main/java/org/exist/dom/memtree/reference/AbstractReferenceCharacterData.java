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

import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.persistent.NodeProxy;
import org.exist.xquery.Expression;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;

import javax.annotation.Nullable;

public class AbstractReferenceCharacterData<T extends AbstractReferenceCharacterData<T, P>, P extends org.exist.dom.persistent.AbstractCharacterData<P>> extends AbstractReferenceNodeImpl<T, P> implements CharacterData {

    public AbstractReferenceCharacterData(@Nullable final Expression expression, final DocumentImpl doc, final int nodeNumber, final NodeProxy nodeProxy) {
        super(expression, doc, nodeNumber, nodeProxy);
    }

    @Override
    public int compareTo(final T other) {
        return getProxiedNode().compareTo(other.getProxiedNode());
    }

    @Override
    public String getData() throws DOMException {
        return getProxiedNode().getData();
    }

    @Override
    public void setData(final String data) throws DOMException {
        getProxiedNode().setData(data);
    }

    @Override
    public int getLength() {
        return getProxiedNode().getLength();
    }

    @Override
    public String substringData(final int offset, final int count) throws DOMException {
        return getProxiedNode().substringData(offset, count);
    }

    @Override
    public void appendData(final String arg) throws DOMException {
        getProxiedNode().appendData(arg);
    }

    @Override
    public void insertData(final int offset, final String arg) throws DOMException {
        getProxiedNode().insertData(offset, arg);
    }

    @Override
    public void deleteData(int offset, int count) throws DOMException {

    }

    @Override
    public void replaceData(int offset, int count, String arg) throws DOMException {

    }
}
