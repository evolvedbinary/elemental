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
