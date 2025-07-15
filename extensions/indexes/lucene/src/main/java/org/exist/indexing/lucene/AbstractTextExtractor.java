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
package org.exist.indexing.lucene;

import org.exist.util.XMLString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractTextExtractor implements TextExtractor {

    protected final LuceneConfig config;
    protected final LuceneIndexConfig idxConfig;
    protected final Map<String, String> prefixToNamespaceMappings;

    protected XMLString buffer = new XMLString();

    public AbstractTextExtractor(final LuceneConfig config, final LuceneIndexConfig idxConfig, @Nullable final Map<String, String> prefixToNamespaceMappings) {
        this.config = config;
        this.idxConfig = idxConfig;
        this.prefixToNamespaceMappings = prefixToNamespaceMappings != null ? prefixToNamespaceMappings : Collections.emptyMap();
    }

    @Override
    public LuceneIndexConfig getIndexConfig() {
    	return idxConfig;
    }

    @Override
    public XMLString getText() {
        return buffer;
    }

    @Override
    public Map<String, String> getPrefixToNamespaceMappings() {
        return prefixToNamespaceMappings;
    }
}
