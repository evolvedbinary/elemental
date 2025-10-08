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
package org.exist.test.runner;

import org.apache.commons.io.output.StringBuilderWriter;
import org.exist.dom.QName;
import org.exist.util.serializer.XQuerySerializer;
import org.exist.xquery.ExpressionVisitor;
import org.exist.xquery.UserDefinedFunction;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;

import java.util.Properties;

import static org.exist.xquery.FunctionDSL.functionSignature;
import static org.exist.xquery.FunctionDSL.returnsNothing;

/**
 * Base class for XQuery functions that integrate with JUnit.
 *
 * @author Adam Retter
 */
public abstract class JUnitIntegrationFunction extends UserDefinedFunction {

    protected static final StringValue VALUE_MAP_KEY = new StringValue("value");

    private static final Properties XML_SERIALIZATION_PROPERTIES = new Properties();
    static {
        XML_SERIALIZATION_PROPERTIES.setProperty(OutputKeys.METHOD, "xml");
    }

    private static final Properties ADAPTIVE_SERIALIZATION_PROPERTIES = new Properties();
    static {
        ADAPTIVE_SERIALIZATION_PROPERTIES.setProperty(OutputKeys.METHOD, "adaptive");
    }

    protected final String suiteName;
    protected final RunNotifier notifier;

    public JUnitIntegrationFunction(final String functionName, final FunctionParameterSequenceType[] paramTypes, final XQueryContext context, final String suiteName, final RunNotifier notifier) {
        super(context,
                functionSignature(
                        new QName(functionName,  XMLConstants.NULL_NS_URI),
                        "External JUnit integration function",
                        returnsNothing(),
                        paramTypes
                ));
        this.suiteName = suiteName;
        this.notifier = notifier;
    }

    @Override
    public void accept(final ExpressionVisitor visitor) {
        if (visited) {
            return;
        }
        visited = true;
    }

    /**
     * Create a JUnit description of the test.
     *
     * @param name the name of the test.
     *
     * @return the test description.
     */
    protected Description createTestDescription(final String name) {
        return Description.createTestDescription(suiteName, name);
    }

    protected static String safeGetStringValue(final Item item) {
        try {
            return item.getStringValue();
        } catch (final XPathException e) {
            LOG.error("Unable to get String Value of item: " + e.getMessage(), e);
            return "<Error JUnitIntegrationFunction#safeGetStringValue(Item): " + e.getMessage() + ">";
        }
    }

    protected static int safeGetIntValue(@Nullable final Sequence sequence, final int index) {
        if (sequence != null && !sequence.isEmpty()) {
            return safeGetIntValue(sequence.itemAt(index));
        }
        return -1;
    }

    protected static int safeGetIntValue(final Item item) {
        try {
            return item.toJavaObject(int.class);
        } catch (final XPathException e) {
            LOG.error("Unable to get Int Value of item: " + e.getMessage(), e);
            return -1;
        }
    }

    protected static String safeSequenceToXmlString(final XQueryContext context, final Sequence sequence) {
        return safeSequenceToString(context, sequence, XML_SERIALIZATION_PROPERTIES);
    }

    protected static String safeSequenceToAdaptiveString(final XQueryContext context, final Sequence sequence) {
        return safeSequenceToString(context, sequence, ADAPTIVE_SERIALIZATION_PROPERTIES);
    }

    private static String safeSequenceToString(final XQueryContext context, final Sequence sequence, final Properties properties) {
        try (final StringBuilderWriter writer = new StringBuilderWriter()) {
            final XQuerySerializer xquerySerializer = new XQuerySerializer(context.getBroker(), properties, writer);
            xquerySerializer.serialize(sequence);
            return writer.toString();
        } catch (final XPathException | SAXException e) {
            LOG.error("Unable to convert Sequence to String: " + e.getMessage(), e);
            return "<Error JUnitIntegrationFunction#safeSequenceToString(XQueryContext, Sequence): " + e.getMessage() + ">";
        }
    }

    protected static @Nullable String safeGetMapStringValue(final StringValue key, final MapType map, @Nullable final String defaultValue) {
        final Sequence mapValue = map.get(key);
        final String value;
        if (mapValue != null && !mapValue.isEmpty()) {
            value = safeGetStringValue(mapValue.itemAt(0));
        } else {
            value = defaultValue;
        }
        return value;
    }
}
