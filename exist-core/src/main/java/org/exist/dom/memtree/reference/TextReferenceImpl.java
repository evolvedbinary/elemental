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
import org.w3c.dom.DOMException;
import org.w3c.dom.Text;

import javax.annotation.Nullable;

/**
 * Text wrapper around a NodeProxy for use in the in-memory DOM.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class TextReferenceImpl extends AbstractReferenceCharacterData<TextReferenceImpl, org.exist.dom.persistent.TextImpl> implements Text {

    public TextReferenceImpl(@Nullable final Expression expression, final DocumentImpl doc, final int nodeNumber, final NodeProxy nodeProxy) {
        super(expression, doc, nodeNumber, nodeProxy);
    }

    @Override
    public Text splitText(final int offset) throws DOMException {
        return getProxiedNode().splitText(offset);
    }

    @Override
    public boolean isElementContentWhitespace() {
        return getProxiedNode().isElementContentWhitespace();
    }

    @Override
    public String getWholeText() {
        return getProxiedNode().getWholeText();
    }

    @Override
    public Text replaceWholeText(final String content) throws DOMException {
        return getProxiedNode().replaceWholeText(content);
    }
}
