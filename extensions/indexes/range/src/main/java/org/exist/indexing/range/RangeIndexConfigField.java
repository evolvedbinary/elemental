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
package org.exist.indexing.range;

import org.exist.indexing.range.conversion.TypeConverter;
import org.exist.storage.NodePath;
import org.exist.util.DatabaseConfigurationException;
import org.exist.util.XMLString;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Type;
import org.w3c.dom.Element;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Handles configuration of a field within an index definition:
 *
 * <pre>
 * &lt;create match="//parent"&gt;
 *   &lt;field name="field-name" match="@xml:id" type="xs:string"/&gt;
 * </pre>
 */
public class RangeIndexConfigField {

    private final String name;
    private final NodePath path;
    private @Nullable final NodePath relPath;
    private final int type;
    private @Nullable TypeConverter typeConverter = null;
    protected final boolean includeNested;
    protected final int wsTreatment;
    protected final boolean caseSensitive;

    public RangeIndexConfigField(final NodePath parentPath, final Element elem, final Map<String, String> namespaces) throws DatabaseConfigurationException {
        this.name = elem.getAttribute("name");
        if (name.isEmpty()) {
            throw new DatabaseConfigurationException("Range index module: field element requires a name attribute");
        }

        final String match = elem.getAttribute("match");
        if (!match.isEmpty()) {
            try {
                this.relPath = new NodePath(namespaces, match);
                if (relPath.length() == 0) {
                    throw new DatabaseConfigurationException("Range index module: Invalid match path in collection config: " + match);
                }
                this.path = new NodePath(parentPath);
                path.append(relPath);
            } catch (final IllegalArgumentException e) {
                throw new DatabaseConfigurationException("Range index module: invalid qname in configuration: " + e.getMessage());
            }
        } else {
            this.relPath = null;
            this.path = parentPath;
        }

        final String typeStr = elem.getAttribute("type");
        if (!typeStr.isEmpty()) {
            try {
                this.type = Type.getType(typeStr);
            } catch (final XPathException e) {
                throw new DatabaseConfigurationException("Invalid type declared for range index on " + match + ": " + typeStr);
            }
        } else {
             this.type = Type.STRING;
        }

        final String custom = elem.getAttribute("converter");
        if (!custom.isEmpty()) {
            try {
                final Class<?> customClass = Class.forName(custom);
                this.typeConverter = (org.exist.indexing.range.conversion.TypeConverter) customClass.newInstance();
            } catch (final ClassNotFoundException e) {
                RangeIndex.LOG.warn("Class for custom-type not found: {}", custom);
            } catch (final InstantiationException | IllegalAccessException e) {
                RangeIndex.LOG.warn("Failed to initialize custom-type: {}", custom, e);
            }
        } else {
             this.typeConverter = null;
        }

        final String nested = elem.getAttribute("nested");
        this.includeNested = nested.isEmpty() || nested.equalsIgnoreCase("yes");
        path.setIncludeDescendants(includeNested);

        // normalize whitespace if whitespace="normalize"
        final String whitespace = elem.getAttribute("whitespace");
        if ("trim".equalsIgnoreCase(whitespace)) {
            this.wsTreatment = XMLString.SUPPRESS_BOTH;

        } else if ("normalize".equalsIgnoreCase(whitespace)) {
            this.wsTreatment = XMLString.NORMALIZE;

        } else {
            this.wsTreatment = XMLString.SUPPRESS_NONE;
        }

        final String caseStr = elem.getAttribute("case");
        this.caseSensitive = caseStr.isEmpty() || caseStr.equalsIgnoreCase("yes");
    }

    public String getName() {
        return name;
    }

    public NodePath getPath() {
        return path;
    }

    public int getType() {
        return type;
    }

    public org.exist.indexing.range.conversion.TypeConverter getTypeConverter() {
        return typeConverter;
    }

    public boolean match(NodePath other) {
        return path.match(other);
    }

    public boolean match(NodePath parentPath, NodePath other) {
        if (relPath == null) {
            return parentPath.match(other);
        } else {
            NodePath absPath = new NodePath(parentPath);
            absPath.append(relPath);
            return absPath.match(other);
        }
    }

    public int whitespaceTreatment() {
        return wsTreatment;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean includeNested() {
        return includeNested;
    }
}
