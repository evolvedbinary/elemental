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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.exist.dom.QName;
import org.exist.storage.NodePath;
import org.exist.util.DatabaseConfigurationException;
import org.exist.xquery.Predicate;
import org.exist.xquery.value.Type;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import java.util.*;

public class ComplexRangeIndexConfigElement extends BasicRangeIndexConfigElement {

    public static final Comparator<ComplexRangeIndexConfigElement> NUM_CONDITIONS_COMPARATOR =
            Comparator.comparingInt(ComplexRangeIndexConfigElement::getNumberOfConditions).reversed();
    public final static String FIELD_ELEMENT = "field";
    public final static String CONDITION_ELEMENT = "condition";
    private static final Logger LOG = LogManager.getLogger(ComplexRangeIndexConfigElement.class);

    private @Nullable Map<String, RangeIndexConfigField> fields = null;
    private @Nullable List<RangeIndexConfigCondition> conditions = null;

    public @Nullable List<RangeIndexConfigCondition> getConditions() {
        return conditions;
    }

    public int getNumberOfConditions() {
        if (conditions == null) {
            return 0;
        }
        return conditions.size();
    }

    public ComplexRangeIndexConfigElement(final Element node, final NodeList children, final Map<String, String> namespaces, @Nullable final Map<String, RangeIndexConfigContextElement> contextConfigs)
            throws DatabaseConfigurationException {
        super(node, namespaces, contextConfigs);

        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (FIELD_ELEMENT.equals(child.getLocalName())) {
                    final RangeIndexConfigField field = new RangeIndexConfigField(path, (Element) child, namespaces);
                    if (fields == null) {
                        fields = new HashMap<>();
                    }
                    fields.put(field.getName(), field);
                } else if (CONDITION_ELEMENT.equals(child.getLocalName())){
                    if (conditions == null) {
                        conditions = new ArrayList<>();
                    }
                    conditions.add(new RangeIndexConfigAttributeCondition((Element) child, path));
                } else if (FILTER_ELEMENT.equals(child.getLocalName())) {
                    analyzer.addFilter((Element) child);
                } else {
                    LOG.warn("Invalid element encountered for range index configuration: {}", child.getLocalName());
                }
            }
        }
    }

    @Override
    public boolean isCaseSensitive(final String fieldName) {
        if (fields == null) {
            return caseSensitive;
        }

        for (final RangeIndexConfigField field: fields.values()) {
            if (fieldName != null && fieldName.equals(field.getName())) {
                return field.isCaseSensitive();
            }
        }
        return caseSensitive;
    }

    @Override
    public boolean match(final NodePath other) {
        if (qnameIndex) {
            final QName qn1 = path.getLastComponent();
            final QName qn2 = other.getLastComponent();
            return qn1.getNameType() == qn2.getNameType() && qn2.equals(qn1);
        }
        return path.match(other);
    }

    @Override
    public boolean find(final NodePath other) {
        return getField(other) != null;
    }

    @Override
    public TextCollector newCollector(final NodePath path) {
        return new ComplexTextCollector(this, path);
    }

    @Override
    public Analyzer getAnalyzer(final String fieldName) {
        if (fields != null && fields.containsKey(fieldName)) {
            return analyzer;
        }
        return null;
    }

    public @Nullable RangeIndexConfigField getField(final NodePath path) {
        if (fields != null) {
            for (final RangeIndexConfigField field : fields.values()) {
                if (field.match(path)) {
                    return field;
                }
            }
        }
        return null;
    }

    public @Nullable RangeIndexConfigField getField(final NodePath parentPath, final NodePath path) {
        if (fields != null) {
            for (final RangeIndexConfigField field : fields.values()) {
                if (field.match(parentPath, path)) {
                    return field;
                }
            }
        }
        return null;
    }

    @Override
    public int getType(final String fieldName) {
        if (fields != null) {
            final RangeIndexConfigField field = fields.get(fieldName);
            if (field != null) {
                return field.getType();
            }
        }
        return Type.ITEM;
    }

    @Override
    public org.exist.indexing.range.conversion.TypeConverter getTypeConverter(final String fieldName) {
        if (fields != null) {
            final RangeIndexConfigField field = fields.get(fieldName);
            if (field != null) {
                return field.getTypeConverter();
            }
        }
        return null;
    }

    public boolean matchConditions(final Node node) {
        if (conditions != null) {
            for (final RangeIndexConfigCondition condition : conditions) {
                if (!condition.matches(node)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean findCondition(final Predicate predicate) {
        if (conditions != null) {
            for (final RangeIndexConfigCondition condition : conditions) {
                if (condition.find(predicate)) {
                    return true;
                }
            }
        }
        return false;
    }
}
