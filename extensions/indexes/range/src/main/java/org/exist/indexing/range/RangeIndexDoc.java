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

import javax.annotation.Nullable;
import java.util.List;

public class RangeIndexDoc {

    private final NodeId nodeId;
    private final QName qname;
    private final NodePath path;
    private final TextCollector collector;
    private @Nullable final List<StaticContext> preContexts;
    private @Nullable final List<CompletableContext> postContexts;
    private final RangeIndexConfigElement config;
    private long address = -1;

    public RangeIndexDoc(final NodeId nodeId, final QName qname, final NodePath path, final TextCollector collector, @Nullable final List<StaticContext> preContexts, @Nullable final List<CompletableContext> postContexts, final RangeIndexConfigElement config) {
        this.nodeId = nodeId;
        this.qname = qname;
        this.path = path;
        this.collector = collector;
        this.preContexts = preContexts;
        this.postContexts = postContexts;
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

    public @Nullable List<StaticContext> getPreContexts() {
        return preContexts;
    }

    public @Nullable List<CompletableContext> getPostContexts() {
        return postContexts;
    }

    public RangeIndexConfigElement getConfig() {
        return config;
    }
}