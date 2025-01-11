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

import javax.annotation.Nullable;
import java.util.*;

public class ComplexTextCollector implements TextCollector {

    private final NodePath parentPath;
    private final ComplexRangeIndexConfigElement config;
    private @Nullable List<Field> fields = null;
    private @Nullable RangeIndexConfigField currentField = null;
    private int length = 0;

    public ComplexTextCollector(final ComplexRangeIndexConfigElement configuration,final NodePath parentPath) {
        this.config = configuration;
        this.parentPath = new NodePath(parentPath, false);
    }

    @Override
    public void startElement(final ElementImpl element, final NodePath path) {
       final RangeIndexConfigField fieldConf = config.getField(parentPath, path);
        if (fieldConf != null) {
            currentField = fieldConf;
            final Field field = new Field(currentField.getName(), false, fieldConf.whitespaceTreatment(), fieldConf.isCaseSensitive());
            if (fields == null) {
                fields = new LinkedList<>();
            }
            fields.add(field);
        }

    }

    @Override
    public void endElement(final ElementImpl element, final NodePath path) {
        if (currentField != null && currentField.match(path)) {
            currentField = null;
        }
    }

    @Override
    public void attribute(final AttrImpl attribute, final NodePath path) {
        final RangeIndexConfigField fieldConf = config.getField(parentPath, path);
        if (fieldConf != null) {
            final Field field = new Field(fieldConf.getName(), true, fieldConf.whitespaceTreatment(), fieldConf.isCaseSensitive());
            field.append(attribute.getValue());
            if (fields == null) {
                fields = new LinkedList<>();
            }
            fields.add(0, field);
        }
    }

    @Override
    public void characters(final AbstractCharacterData text, final NodePath path) {
        if (currentField != null && fields != null) {
            final Field field = fields.get(fields.size() - 1);
            if (!field.isAttribute() && (currentField.includeNested() || currentField.match(path))) {
                field.append(text.getXMLString());
                length += text.getXMLString().length();
            }
        }
    }

    @Override
    public boolean hasFields() {
        return true;
    }

    @Override
    public int length() {
        return length;
    }

    public List<Field> getFields() {
        return fields;
    }

    public ComplexRangeIndexConfigElement getConfig() {
        return config;
    }
}
