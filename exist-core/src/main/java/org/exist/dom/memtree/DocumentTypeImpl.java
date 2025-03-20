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
package org.exist.dom.memtree;

import net.jcip.annotations.ThreadSafe;
import org.exist.xquery.Expression;
import org.exist.xquery.NodeTest;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Sequence;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;

import javax.annotation.Nullable;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@ThreadSafe
public class DocumentTypeImpl extends NodeImpl<DocumentTypeImpl> implements DocumentType {

    private final String publicId;
    private final String systemId;
    private final String name;

    public DocumentTypeImpl(final DocumentImpl doc, final int nodeNumber, final String name, final String publicId, final String systemId) {
        this(null, doc, nodeNumber, name, publicId, systemId);
    }

    public DocumentTypeImpl(@Nullable final Expression expression, final DocumentImpl doc, final int nodeNumber, final String name, final String publicId, final String systemId) {
        super(expression, doc, nodeNumber);
        this.name = name;
        this.publicId = publicId;
        this.systemId = systemId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPublicId() {
        return publicId;
    }

    @Override
    public String getSystemId() {
        return systemId;
    }

    @Override
    public NamedNodeMap getEntities() {
        return null;
    }

    @Override
    public NamedNodeMap getNotations() {
        return null;
    }

    @Override
    public String getInternalSubset() {
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append("<!DOCTYPE ").append(name);

        if(publicId != null) {
            builder.append(" PUBLIC \"").append(publicId).append("\"");
        }

        if(systemId != null) {
            if(publicId == null) {
                builder.append(" SYSTEM");
            }
            builder.append(" \"").append(systemId).append("\"");
        }

        builder.append(" >");

        return builder.toString();
    }

    @Override
    public void selectAttributes(final NodeTest test, final Sequence result) throws XPathException {
    }

    @Override
    public void selectDescendantAttributes(final NodeTest test, final Sequence result) throws XPathException {
    }

    @Override
    public void selectChildren(final NodeTest test, final Sequence result) throws XPathException {

    }

    @Override
    public int compareTo(final DocumentTypeImpl other) {
        int comparison = name.compareTo(other.name);
        if (comparison != 0) {
            return comparison;
        }

        if (publicId == null && other.publicId != null) {
            return -1;
        } else if (publicId != null && other.publicId == null) {
            return 1;
        }
        comparison = publicId.compareTo(other.publicId);
        if (comparison != 0) {
            return comparison;
        }

        if (systemId == null && other.systemId != null) {
            return -1;
        } else if (systemId != null && other.systemId == null) {
            return 1;
        }
        comparison = systemId.compareTo(other.systemId);
        if (comparison != 0) {
            return comparison;
        }

        return 0;
    }
}
