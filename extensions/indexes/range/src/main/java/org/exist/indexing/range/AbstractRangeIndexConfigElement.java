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
import org.exist.indexing.lucene.LuceneIndexConfig;
import org.exist.storage.NodePath;
import org.exist.util.DatabaseConfigurationException;
import org.w3c.dom.Element;

import javax.annotation.Nullable;
import java.util.Map;

import static org.exist.indexing.lucene.LuceneIndexConfig.MATCH_ATTR;
import static org.exist.indexing.lucene.LuceneIndexConfig.QNAME_ATTR;

public abstract class AbstractRangeIndexConfigElement implements RangeIndexConfigElement {
    protected final NodePath path;
    protected final boolean qnameIndex;
    private @Nullable RangeIndexConfigElement nextConfig = null;

    public AbstractRangeIndexConfigElement(final Element element, final Map<String, String> namespaces) throws DatabaseConfigurationException {
        boolean qni= false;
        NodePath p = null;
        final String match = element.getAttribute(MATCH_ATTR);
        if (!match.isEmpty()) {
            try {
                p = new NodePath(namespaces, match);
                if (p.length() == 0) {
                    throw new DatabaseConfigurationException("Range index module: Invalid match path in collection config: " + match);
                }
            } catch (IllegalArgumentException e) {
                throw new DatabaseConfigurationException("Range index module: invalid qname in configuration: " + e.getMessage());
            }
        } else if (element.hasAttribute(QNAME_ATTR)) {
            final QName qname = LuceneIndexConfig.parseQName(element, namespaces);
            p = new NodePath(NodePath.SKIP);
            p.addComponent(qname);
            qni = true;
        }
        this.path = p;
        this.qnameIndex = qni;
    }

    @Override
    public NodePath getNodePath() {
        return path;
    }

    @Override
    public void add(final RangeIndexConfigElement config) {
        if (this.nextConfig == null) {
            this.nextConfig = config;
        } else {
            this.nextConfig.add(config);
        }
    }

    @Override
    public @Nullable RangeIndexConfigElement getNext() {
        return this.nextConfig;
    }
}
