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

import org.exist.dom.persistent.AttrImpl;
import org.exist.dom.persistent.AbstractCharacterData;
import org.exist.dom.persistent.ElementImpl;
import org.exist.storage.NodePath;
import org.exist.util.XMLString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class SimpleTextCollector implements TextCollector {
    private @Nullable final BasicRangeIndexConfigElement config;
    private final boolean includeNested;
    private final XMLString buf = new XMLString();
    private final int wsTreatment;
    private final boolean caseSensitive;

    public SimpleTextCollector(final BasicRangeIndexConfigElement config, final boolean includeNested, final int wsTreatment, final boolean caseSensitive) {
        this.config = config;
        this.includeNested = includeNested;
        this.wsTreatment = wsTreatment;
        this.caseSensitive = caseSensitive;
    }

    public SimpleTextCollector(final String content) {
        this(null, true, XMLString.SUPPRESS_NONE, true);
        buf.append(content);
    }

    @Override
    public void startElement(final ElementImpl element, final NodePath path) {
        // no-op
    }

    @Override
    public void endElement(final ElementImpl element, final NodePath path) {
        // no-op
    }

    @Override
    public void characters(final AbstractCharacterData text, final NodePath path) {
        if (includeNested || (config != null && config.match(path))) {
            buf.append(text.getXMLString());
        }
    }

    @Override
    public void attribute(final AttrImpl attribute, final NodePath path) {
        // no-op
    }

    @Override
    public int length() {
        return buf.length();
    }

    @Override
    public boolean hasFields() {
        return false;
    }

    @Override
    public List<Field> getFields() {
        return Collections.singletonList(new Field(buf, wsTreatment, caseSensitive));
    }
}
