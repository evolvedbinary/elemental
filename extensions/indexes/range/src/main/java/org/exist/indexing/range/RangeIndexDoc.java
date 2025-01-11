/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 *
 * NOTE: Parts of this file contain code from The eXist-db Authors.
 *       The original license header is included below.
 *
 * ----------------------------------------------------------------------------
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
package org.exist.indexing.range;

import org.exist.dom.QName;
import org.exist.numbering.NodeId;
import org.exist.storage.NodePath;

public class RangeIndexDoc {

    private final NodeId nodeId;
    private final QName qname;
    private final NodePath path;
    private final TextCollector collector;
    private final RangeIndexConfigElement config;
    private long address = -1;

    public RangeIndexDoc(final NodeId nodeId, final QName qname, final NodePath path, final TextCollector collector, final RangeIndexConfigElement config) {
        this.nodeId = nodeId;
        this.qname = qname;
        this.path = path;
        this.collector = collector;
        this.config = config;
    }

    public void setAddress(final long address) {
        this.address = address;
    }

    public long getAddress() {
        return address;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public QName getQName() {
        return qname;
    }

    public NodePath getPath() {
        return path;
    }

    public TextCollector getCollector() {
        return collector;
    }

    public RangeIndexConfigElement getConfig() {
        return config;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(qname.toString());
        builder.append(" [");
        for (int i = 0; i < collector.getFields().size(); i++) {
            final TextCollector.Field field = collector.getFields().get(0);

            if (i > 0) {
                builder.append(", ");
            }

            final String fieldName;
            if (field.isNamed()) {
                fieldName = field.getName();
            } else {
                fieldName = "<unnamed>";
            }
            builder.append(fieldName).append(": ");
            builder.append(field.getContent());
        }
        builder.append(']');
        return builder.toString();
    }
}