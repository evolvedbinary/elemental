/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
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
package org.exist.xquery;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.lacuna.bifurcan.IMap;
import org.exist.dom.persistent.AVLTreeNodeSet;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.NodeProxy;
import org.exist.dom.memtree.DocumentBuilderReceiver;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.dom.memtree.NodeImpl;
import org.exist.numbering.NodeId;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.exist.util.serializer.DOMStreamer;
import org.exist.util.serializer.SerializerPool;
import org.exist.xmldb.LocalXMLResource;
import org.exist.xmldb.RemoteXMLResource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.functions.array.ArrayType;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;

import javax.annotation.Nullable;

import static org.exist.xquery.functions.map.MapType.newLinearMap;

public class XPathUtil {

    /**
     * Convert an XDM Sequence to a Java Object of a corresponding type.
     *
     * @param sequence The XDM Sequence to convert.
     *
     * @return A corresponding Java object representing the XDM Sequence.
     *
     * @throws XPathException if an error occurs during the conversion.
     */
    public static Object xpathToJavaObject(final Sequence sequence) throws XPathException {
        if (sequence.getItemCount() == 0) {
            return null;

        } else if (sequence.getItemCount() == 1) {
            return xpathToJavaObject(sequence.itemAt(0));

        } else {
            final int sequenceItemsType = sequence.getItemType();
            final Class<?> sequenceItemsJavaClass = xdmTypeToJavaClass(sequenceItemsType, Cardinality.EXACTLY_ONE);
            final Object javaArray = java.lang.reflect.Array.newInstance(sequenceItemsJavaClass, sequence.getItemCount());

            for (int i = 0; i < sequence.getItemCount(); i++) {
                final Object javaValue = xpathToJavaObject(sequence.itemAt(i));
                java.lang.reflect.Array.set(javaArray, i, javaValue);
            }

            return javaArray;
        }
    }

    /**
     * Describes the precision of a type conversion
     * from an XDM Type to a Java Type.
     */
    public enum TypeConversionPrecision {
        /**
         * There is a one-to-one mapping between an XDM Type and a Java Type.
         */
        ONE_TO_ONE_MAPPING(1),

        /**
         * The XDM Type can be mapped onto a Java Array Type.
         */
        ONE_TO_ARRAY_MAPPING(2),

        /**
         * The XDM Type after type promotion can be mapped to a Java Type.
         */
        VIA_TYPE_PROMOTION(3),

        /**
         * The XDM Type after type promotion and substitution can be mapped to a Java Type.
         */
        VIA_TYPE_SUBSTITUTION(4);

        private final int rank;

        TypeConversionPrecision(final int rank) {
            this.rank = rank;
        }

        /**
         * Return the most precise type conversion.
         *
         * @param other the other type precision to compare.
         *
         * @return returns the most precise conversion of the two: {@code this} or {@code other}.
         */
        public TypeConversionPrecision min(@Nullable final TypeConversionPrecision other) {
            if (other == null) {
                return this;
            }

            if (other.rank >= rank) {
                return other;
            }

            return this;
        }

        /**
         * Compare this TypeConversionPrecision to another TypeConversionPrecision.
         *
         * NOTE we can't implement (override) {@link Comparable#compareTo(Object)}
         * here as it is final in {@link java.lang.Enum}.
         *
         * @param other the other TypeConversionPrecision to be compared.
         *
         * @return a negative integer, zero, or a positive integer as this TypeConversionPrecision is less than, equal to, or greater than the other TypeConversionPrecision.
         */
        public int compare(final TypeConversionPrecision other) {
            if (other.rank > rank) {
                return -1;
            } else if (other.rank < rank) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Determines if an XDM Type is convertible to a Java Type.
     *
     * @param type the XDM Type.
     * @param cardinality the Cardinality of the XDM Type.
     * @param clazz the Java Type that we wish to test if we can convert the XDM Type to.
     *
     * @return if a conversion is possible, a TypeConversionPrecision is returned, otherwise if no conversion is possible, null is returned.
     */
    public static @Nullable TypeConversionPrecision xdmTypeConvertibleToJavaClass(final int type, final Cardinality cardinality, final Class<?> clazz) {
        if (clazz == Object.class) {
            return TypeConversionPrecision.ONE_TO_ONE_MAPPING;
        }

        final Class<?> equivalentJavaClassType;
        if (type == Type.ARRAY_ITEM && clazz.isArray()) {
            equivalentJavaClassType = xdmTypeToJavaClass(javaClassToXdmType(clazz.getComponentType()), cardinality);
        } else {
            equivalentJavaClassType = xdmTypeToJavaClass(type, cardinality);
        }

        if (isAssignableFrom(clazz, equivalentJavaClassType)) {
            return TypeConversionPrecision.ONE_TO_ONE_MAPPING;

        } else if ((clazz.isArray() && (!equivalentJavaClassType.isArray()) && clazz.getComponentType().isAssignableFrom(equivalentJavaClassType))) {
            return TypeConversionPrecision.ONE_TO_ARRAY_MAPPING;
        }

        int coercedType = type;
        final int javaEquivalentXdmType = javaClassToXdmType(clazz);

        // 1) Type Promotion, see: https://www.w3.org/TR/xpath-31#promotion
        if (type == Type.FLOAT && javaEquivalentXdmType == Type.DOUBLE) {
            coercedType = Type.DOUBLE;
        } else if (type == Type.DECIMAL) {
            if (javaEquivalentXdmType == Type.FLOAT) {
                coercedType = Type.FLOAT;
            } else if (javaEquivalentXdmType == Type.DOUBLE) {
                coercedType = Type.DOUBLE;
            }
        }

        if (coercedType == javaEquivalentXdmType) {
            return TypeConversionPrecision.VIA_TYPE_PROMOTION;
        }

        final boolean coercedTypeDerivesFromEquivalentJavaType = derivesFrom(coercedType, javaEquivalentXdmType);
        final boolean equivalentJavaTypeDerivesFromCoercedType = derivesFrom(javaEquivalentXdmType, coercedType);

        // 2) Type Substitution, see: https://www.w3.org/TR/xpath-31/#dt-subtype-substitution
        if (coercedTypeDerivesFromEquivalentJavaType || equivalentJavaTypeDerivesFromCoercedType) {
            return TypeConversionPrecision.VIA_TYPE_SUBSTITUTION;
        }

        return null;
    }

    /**
     * Similar to {@link Class#isAssignableFrom(Class)} but also considers boxing-unboxing
     * of primitive types.
     *
     * @param fromClass the class to assign from
     * @param toClass the class to assign to
     *
     * @return true if the class is assignable, false otherwise.
     */
    public static boolean isAssignableFrom(final Class<?> fromClass, final Class<?> toClass) {
        if (fromClass.isAssignableFrom(toClass)) {
            return true;
        }

        if (fromClass.isPrimitive() && !toClass.isPrimitive()) {
            if (boolean.class == fromClass && toClass == Boolean.class) {
                return true;

            } else if (float.class == fromClass && toClass == Float.class) {
                return true;

            } else if (double.class == fromClass && toClass == Double.class) {
                return true;

            } else if (short.class == fromClass && toClass == Short.class) {
                return true;

            } else if (byte.class == fromClass && toClass == Byte.class) {
                return true;

            } else if (int.class == fromClass && toClass == Integer.class) {
                return true;

            } else if (long.class == fromClass && toClass == Long.class) {
                return true;
            }
        }

        return false;
    }

    /**
     * Implementation of <a href="https://www.w3.org/TR/xpath-31/#dt-subtype-substitution">XPath 3.1 - 2.5.5 SequenceType Matching</a>.
     *
     * @param actualType The actual type, a.k.a. AT.
     * @param expectedType The expected type, a.k.a. ET.
     *
     * @return true if AT derives from ET, false otherwise.
     */
    public static boolean derivesFrom(final int actualType, final int expectedType) {

        // AT is ET
        if (actualType == expectedType) {
            return true;
        }

        // ET is the base type of AT
        if (Type.subTypeOf(actualType, expectedType)) {
            return true;
        }

        // ET is a pure union type of which AT is a member type
        if (Type.hasMember(expectedType, actualType)) {
            return true;
        }

        // There is a type MT such that derives-from(AT, MT) and derives-from(MT, ET)

        // iterate through AT's super-types
        int t;
        for (t = actualType; t != Type.ITEM && t != Type.ANY_TYPE; t = Type.getSuperType(t)) {
            // is the super-type of AT a subtype of ET
            if (Type.subTypeOf(t, expectedType)) {
                return true;
            }
        }

        // Otherwise, derives-from(AT,ET) return false
        return false;
    }

    /**
     * Returns the equivalent Java class for an XDM type.
     *
     * @param type The XDM type.
     * @param cardinality the cardinality of the XDM type.
     *
     * @return the Java class or Object if there
     * is no known mapping between the XDM type and the Java class.
     */
    public static Class<?> xdmTypeToJavaClass(final int type, final Cardinality cardinality) {
        switch (type) {
            case Type.STRING:
                if (cardinality.atMostOne()) {
                    return String.class;   // TODO(AR) consider returning CharSequence.class instead of String.class
                } else {
                    return String[].class;   // TODO(AR) consider returning CharSequence[].class instead of String[].class
                }

            case Type.BOOLEAN:
                if (cardinality == Cardinality.EXACTLY_ONE) {
                    return boolean.class;
                } else if (cardinality.atMostOne()) {
                    return Boolean.class;
                } else {
                    return Boolean[].class;
                }

            case Type.FLOAT:
                if (cardinality == Cardinality.EXACTLY_ONE) {
                    return float.class;
                } else if (cardinality.atMostOne()) {
                    return Float.class;
                } else {
                    return Float[].class;
                }

            case Type.DOUBLE:
                if (cardinality == Cardinality.EXACTLY_ONE) {
                    return double.class;
                } else if (cardinality.atMostOne()) {
                    return Double.class;
                } else {
                    return Double[].class;
                }

            case Type.SHORT:
                if (cardinality == Cardinality.EXACTLY_ONE) {
                    return short.class;
                } else if (cardinality.atMostOne()) {
                    return Short.class;
                } else {
                    return Short[].class;
                }

            case Type.BYTE:
                if (cardinality == Cardinality.EXACTLY_ONE) {
                    return byte.class;
                } else if (cardinality.atMostOne()) {
                    return Byte.class;
                } else {
                    return Byte[].class;
                }

            case Type.INT:
                if (cardinality == Cardinality.EXACTLY_ONE) {
                    return int.class;
                } else if (cardinality.atMostOne()) {
                    return Integer.class;
                } else {
                    return Integer[].class;
                }

            case Type.INTEGER:
                if (cardinality.atMostOne()) {
                    return BigInteger.class;
                } else {
                    return BigInteger[].class;
                }

            case Type.LONG:
                if (cardinality == Cardinality.EXACTLY_ONE) {
                    return long.class;
                } else if (cardinality.atMostOne()) {
                    return Long.class;
                } else {
                    return Long[].class;
                }

            case Type.DECIMAL:
                if (cardinality.atMostOne()) {
                    return BigDecimal.class;
                } else {
                    return BigDecimal[].class;
                }

            case Type.BASE64_BINARY:
                return byte[].class;

            case Type.MAP_ITEM:
                return Map.class;

            case Type.ELEMENT:
                if (cardinality.atMostOne()) {
                    return Element.class;
                } else {
                    return Element[].class;
                }

            case Type.ATTRIBUTE:
                if (cardinality.atMostOne()) {
                    return Attr.class;
                } else {
                    return Attr[].class;
                }

            case Type.TEXT:
                if (cardinality.atMostOne()) {
                    return Text.class;
                } else {
                    return Text[].class;
                }

            case Type.PROCESSING_INSTRUCTION:
                if (cardinality.atMostOne()) {
                    return ProcessingInstruction.class;
                } else {
                    return ProcessingInstruction[].class;
                }

            case Type.COMMENT:
                if (cardinality.atMostOne()) {
                    return Comment.class;
                } else {
                    return Comment.class;
                }

            case Type.DOCUMENT:
                if (cardinality.atMostOne()) {
                    return Document.class;
                } else {
                    return Document[].class;
                }

            case Type.CDATA_SECTION:
                if (cardinality.atMostOne()) {
                    return CDATASection.class;
                } else {
                    return CDATASection[].class;
                }

            // fall-through
            default:
                if (cardinality.atMostOne()) {
                    return Object.class;
                } else {
                    return Object[].class;
                }
        }
    }

    /**
     * Convert an XDM Item to a Java Object of a corresponding type.
     *
     * @param item The XDM Item to convert.
     *
     * @return A corresponding Java object representing the XDM Item.
     *
     * @throws XPathException if an error occurs during the conversion.
     */
    public static Object xpathToJavaObject(final Item item) throws XPathException {
        final Class<?> targetClazz;
        if (item.getType() == Type.ARRAY_ITEM) {
            targetClazz = xdmTypeToJavaClass(((ArrayType) item).asSequence().getItemType(), Cardinality.ZERO_OR_MORE);
        } else {
            targetClazz = xdmTypeToJavaClass(item.getType(), Cardinality.EXACTLY_ONE);
        }
        return item.toJavaObject(targetClazz);
    }

    /**
     * Returns the equivalent XDM type for a Java class.
     *
     * @param clazz The Java class.
     *
     * @return the {@link Type} or {@link Type#JAVA_OBJECT} if there
     * is no known mapping between the Java class and an XDM type.
     */
    public static int javaClassToXdmType(final Class<?> clazz) {
        if (CharSequence.class.isAssignableFrom(clazz)) {
            return Type.STRING;

        } else if (boolean.class.isAssignableFrom(clazz) || Boolean.class.isAssignableFrom(clazz)) {
            return Type.BOOLEAN;

        } else if (float.class.isAssignableFrom(clazz) || Float.class.isAssignableFrom(clazz)) {
            return Type.FLOAT;

        } else if (double.class.isAssignableFrom(clazz) || Double.class.isAssignableFrom(clazz)) {
            return Type.DOUBLE;

        } else if (short.class.isAssignableFrom(clazz) || Short.class.isAssignableFrom(clazz)) {
            return Type.SHORT;

        } else if (byte.class.isAssignableFrom(clazz) || Byte.class.isAssignableFrom(clazz)) {
            return Type.BYTE;

        } else if (char.class.isAssignableFrom(clazz) || Character.class.isAssignableFrom(clazz)) {
            return Type.BYTE;

        } else if (int.class.isAssignableFrom(clazz) || Integer.class.isAssignableFrom(clazz)) {
            return Type.INT;

        }  else if (BigInteger.class.isAssignableFrom(clazz)) {
            return Type.INTEGER;

        } else if (long.class.isAssignableFrom(clazz) || Long.class.isAssignableFrom(clazz)) {
            return Type.LONG;

        } else if (BigDecimal.class.isAssignableFrom(clazz)) {
            return Type.DECIMAL;

        } else if (byte[].class.isAssignableFrom(clazz)) {
            return Type.BASE64_BINARY;

        } else if (Map.class.isAssignableFrom(clazz)) {
            return Type.MAP_ITEM;

        } else if (Element.class.isAssignableFrom(clazz)) {
            return Type.ELEMENT;

        } else if (Attr.class.isAssignableFrom(clazz)) {
            return Type.ATTRIBUTE;

        } else if (Text.class.isAssignableFrom(clazz)) {
            return Type.TEXT;

        } else if (ProcessingInstruction.class.isAssignableFrom(clazz)) {
            return Type.PROCESSING_INSTRUCTION;

        } else if (Comment.class.isAssignableFrom(clazz)) {
            return Type.COMMENT;

        } else if (Document.class.isAssignableFrom(clazz)) {
            return Type.DOCUMENT;

        } else if (CDATASection.class.isAssignableFrom(clazz)) {
            return Type.CDATA_SECTION;

        } else if (clazz.isArray()) {
            return Type.ARRAY_ITEM;

        } else {
            return Type.JAVA_OBJECT;
        }
    }

    /**
     * Convert ta Java object to an XDM sequence. Objects of type Sequence are
     * directly returned, other objects are converted into the corresponding
     * internal types.
     *
     * @param obj The java object.
     * @param context XQuery context.
     *
     * @return the XDM sequence.
     *
     * @throws XPathException if an error occurs during the conversion.
     */
    public static final Sequence javaObjectToXPath(final Object obj, final XQueryContext context)
            throws XPathException {
        return javaObjectToXPath(obj, context, null);
    }

    /**
     * Convert a Java object to an XDM sequence. Objects of type Sequence are
     * directly returned, other objects are converted into the corresponding
     * internal types.
     *
     * @param obj The java object.
     * @param context XQuery context.
     * @param expression the expression from which the object derives.
     *
     * @return the XDM sequence.
     *
     * @throws XPathException if an error occurs during the conversion.
     */
    public static final Sequence javaObjectToXPath(final Object obj, final XQueryContext context, final Expression expression)
            throws XPathException {
        return javaObjectToXPath(obj, context, true, expression);
    }

    /**
     * Convert a Java object to an XDM sequence. Objects of type Sequence are
     * directly returned, other objects are converted into the corresponding
     * internal types.
     *
     * @param obj The java object.
     * @param context XQuery context.
     * @param expandChars true if characters should be expanded, false otherwise.
     *
     * @return the XDM sequence.
     *
     * @throws XPathException if an error occurs during the conversion.
     */
    public static final Sequence javaObjectToXPath(final Object obj, final XQueryContext context,
            final boolean expandChars) throws XPathException {
        return javaObjectToXPath(obj, context, expandChars, null);
    }

    /**
     * Convert a Java object to an XDM sequence. Objects of type Sequence are
     * directly returned, other objects are converted into the corresponding
     * internal types.
     *
     * @param obj The java object
     * @param context XQuery context
     * @param expandChars true if characters should be expanded, false otherwise.
     * @param expression the expression from which the object derives.
     *
     * @return the XDM sequence.
     *
     * @throws XPathException if an error occurs during the conversion.
     */
    public static @Nullable Sequence javaObjectToXPath(@Nullable final Object obj, final XQueryContext context, final boolean expandChars, final Expression expression) throws XPathException {
        return javaObjectToXPath(obj, context, expandChars, true, true, expression);
    }

    /**
     * Convert a Java object to an XDM sequence. Objects of type Sequence are
     * directly returned, other objects are converted into the corresponding
     * internal types.
     *
     * @param obj The java object.
     * @param context XQuery context.
     * @param expandChars true if characters should be expanded, false otherwise.
     * @param listToSequence true if lists should be converted to sequences, false otherwise.
     * @param arrayToSequence true if arrays should be converted to sequences, false otherwise.
     * @param expression the expression from which the object derives.
     *
     * @return the XDM sequence.
     *
     * @throws XPathException if an error occurs during the conversion.
     */
    public static @Nullable Sequence javaObjectToXPath(@Nullable final Object obj, final XQueryContext context, final boolean expandChars, final boolean listToSequence, final boolean arrayToSequence, final Expression expression) throws XPathException {

        if (obj == null) {
            //return Sequence.EMPTY_SEQUENCE;
            return null;

        } else if (obj instanceof Sequence) {
            return (Sequence) obj;

        } else if (obj instanceof ResourceSet) {
            final Sequence seq = new AVLTreeNodeSet();
            try {
                final DBBroker broker = context.getBroker();
                for (final ResourceIterator it = ((ResourceSet) obj).getIterator(); it.hasMoreResources();) {
                    seq.add(getNode(broker, (XMLResource) it.nextResource(), expression));
                }
            } catch (final XMLDBException xe) {
                throw new XPathException(expression, "Failed to convert ResourceSet to node: " + xe.getMessage());
            }
            return seq;

        } else if (obj instanceof XMLResource) {
            return getNode(context.getBroker(), (XMLResource) obj, expression);

        } else if (obj instanceof Node) {
            context.pushDocumentContext();
            final DOMStreamer streamer = (DOMStreamer) SerializerPool.getInstance().borrowObject(DOMStreamer.class);
            try {
                final MemTreeBuilder builder = context.getDocumentBuilder();
                builder.startDocument();
                final DocumentBuilderReceiver receiver = new DocumentBuilderReceiver(expression, builder);
                streamer.setContentHandler(receiver);
                streamer.serialize((Node) obj, false);
                if(obj instanceof Document) {
                    return builder.getDocument();
                } else {
                    return builder.getDocument().getNode(1);
                }
            } catch (final SAXException e) {
                throw new XPathException(expression,
                    "Failed to transform node into internal model: "
                        + e.getMessage());
            } finally {
                context.popDocumentContext();
                SerializerPool.getInstance().returnObject(streamer);
            }

        } else if (listToSequence && obj instanceof List<?>) {
            boolean createNodeSequence = true;

            final List<?> lst = (List<?>) obj;
            for (final Object next : lst) {
                if (!(next instanceof NodeProxy)) {
                    createNodeSequence = false;
                    break;
                }
            }
            final Sequence seq = createNodeSequence ? new AVLTreeNodeSet() : new ValueSequence(lst.size());
            for (final Object o : lst) {
                seq.add((Item) javaObjectToXPath(o, context, expandChars, listToSequence, arrayToSequence, expression));
            }
            return seq;

        } else if (obj instanceof NodeList) {
            context.pushDocumentContext();
            final DOMStreamer streamer = (DOMStreamer) SerializerPool.getInstance().borrowObject(DOMStreamer.class);
            try {
                final MemTreeBuilder builder = context.getDocumentBuilder();
                builder.startDocument();
                final DocumentBuilderReceiver receiver = new DocumentBuilderReceiver(expression, builder);
                streamer.setContentHandler(receiver);
                final ValueSequence seq = new ValueSequence();
                final NodeList nl = (NodeList) obj;
                int last = builder.getDocument().getLastNode();
                for (int i = 0; i < nl.getLength(); i++) {
                    final Node n = nl.item(i);
                    streamer.serialize(n, false);
                    final NodeImpl created = builder.getDocument().getNode(last + 1);
                    seq.add(created);
                    last = builder.getDocument().getLastNode();
                }
                return seq;
            } catch (final SAXException e) {
                throw new XPathException(expression,
                    "Failed to transform node into internal model: "
                        + e.getMessage());
            } finally {
                context.popDocumentContext();
                SerializerPool.getInstance().returnObject(streamer);
            }

        } else if (arrayToSequence && obj instanceof Object[] array) {
            boolean createNodeSequence = true;
            for (Object arrayItem : array) {
                if (!(arrayItem instanceof NodeProxy)) {
                    createNodeSequence = false;
                    break;
                }
            }

            final Sequence seq = createNodeSequence ? new AVLTreeNodeSet() : new ValueSequence();
            for (final Object arrayItem : array) {
                seq.add((Item) javaObjectToXPath(arrayItem, context, expandChars, listToSequence, arrayToSequence, expression));
            }
            return seq;

        }

        final int xdmType = javaClassToXdmType(obj.getClass());
        switch (xdmType) {

            case Type.STRING:
                final StringValue v = new StringValue(expression, (String) obj);
                return (expandChars ? v.expand() : v);

            case Type.BOOLEAN:
                return BooleanValue.valueOf(((Boolean) obj));

            case Type.FLOAT:
                return new FloatValue(expression, ((Float) obj));

            case Type.DOUBLE:
                return new DoubleValue(expression, ((Double) obj));

            case Type.SHORT:
                return new IntegerValue(expression, ((Short) obj), Type.SHORT);

            case Type.BYTE:
                if (Character.class.isAssignableFrom(obj.getClass())) {
                    return new IntegerValue(expression, ((Character) obj), Type.BYTE);
                } else {
                    return new IntegerValue(expression, ((Byte) obj).intValue(), Type.BYTE);
                }

            case Type.INT:
                return new IntegerValue(expression, (Integer) obj, Type.INT);

            case Type.INTEGER:
                return new IntegerValue(expression, (BigInteger) obj);

            case Type.LONG:
                return new IntegerValue(expression, ((Long) obj), Type.LONG);

            case Type.DECIMAL:
                return new DecimalValue(expression, (BigDecimal) obj);

            case Type.BASE64_BINARY:
                return BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), new UnsynchronizedByteArrayInputStream((byte[]) obj), expression);

            case Type.ARRAY_ITEM:
                final List<Sequence> items = new ArrayList<>();
                if (byte[].class.isAssignableFrom(obj.getClass())) {
                    for (final byte javaItem : (byte[]) obj) {
                        items.add(javaObjectToXPath(javaItem, context, expandChars, listToSequence, arrayToSequence, expression));
                    }
                } else if (short[].class.isAssignableFrom(obj.getClass())) {
                    for (final short javaItem : (short[]) obj) {
                        items.add(javaObjectToXPath(javaItem, context, expandChars, listToSequence, arrayToSequence, expression));
                    }
                } else if (int[].class.isAssignableFrom(obj.getClass())) {
                    for (final int javaItem : (int[]) obj) {
                        items.add(javaObjectToXPath(javaItem, context, expandChars, listToSequence, arrayToSequence, expression));
                    }
                } else if (long[].class.isAssignableFrom(obj.getClass())) {
                    for (final long javaItem : (long[]) obj) {
                        items.add(javaObjectToXPath(javaItem, context, expandChars, listToSequence, arrayToSequence, expression));
                    }
                } else if (float[].class.isAssignableFrom(obj.getClass())) {
                    for (final float javaItem : (float[]) obj) {
                        items.add(javaObjectToXPath(javaItem, context, expandChars, listToSequence, arrayToSequence, expression));
                    }
                } else if (double[].class.isAssignableFrom(obj.getClass())) {
                    for (final double javaItem : (double[]) obj) {
                        items.add(javaObjectToXPath(javaItem, context, expandChars, listToSequence, arrayToSequence, expression));
                    }
                } else if (boolean[].class.isAssignableFrom(obj.getClass())) {
                    for (final boolean javaItem : (boolean[]) obj) {
                        items.add(javaObjectToXPath(javaItem, context, expandChars, listToSequence, arrayToSequence, expression));
                    }
                } else if (char[].class.isAssignableFrom(obj.getClass())) {
                    for (final char javaItem : (char[]) obj) {
                        items.add(javaObjectToXPath(javaItem, context, expandChars, listToSequence, arrayToSequence, expression));
                    }
                } else {
                    for (final Object javaItem : (Object[]) obj) {
                        items.add(javaObjectToXPath(javaItem, context, expandChars, listToSequence, arrayToSequence, expression));
                    }
                }
                return new ArrayType(expression, context, items);

            case Type.MAP_ITEM:
                final Map<?, ?> javaMap = (Map<?, ?>) obj;
                final IMap<AtomicValue, Sequence> xdmMap = newLinearMap(null);
                for (final Map.Entry<?, ?> javaMapEntry : javaMap.entrySet()) {
                    xdmMap.put(
                        javaObjectToXPath(javaMapEntry.getKey(), context, expandChars, listToSequence, arrayToSequence, expression).itemAt(0).atomize(),
                        javaObjectToXPath(javaMapEntry.getValue(), context, expandChars, listToSequence, arrayToSequence, expression)
                    );
                }
                return new MapType(null, context, xdmMap.forked(), null);

            // fall-through
            case Type.JAVA_OBJECT:
            default:
                return new JavaObjectValue(obj);
        }
    }

    /**
     * Converts an XMLResource into a NodeProxy.
     *
     * @param broker The DBBroker to use to access the database
     * @param xres The XMLResource to convert
     * @return A NodeProxy for accessing the content represented by xres
     * @throws XPathException if an XMLDBException is encountered
     */
    public static final NodeProxy getNode(DBBroker broker, XMLResource xres) throws XPathException {
        return getNode(broker, xres, null);
    }

    /**
     * Converts an XMLResource into a NodeProxy.
     *
     * @param broker The DBBroker to use to access the database
     * @param xres The XMLResource to convert
     * @param expression the expression from which the resource derives
     * @return A NodeProxy for accessing the content represented by xres
     * @throws XPathException if an XMLDBException is encountered
     */
    public static final NodeProxy getNode(DBBroker broker, XMLResource xres, final Expression expression) throws XPathException {
        if (xres instanceof LocalXMLResource lres) {
            try {
                return lres.getNode();
            } catch (final XMLDBException xe) {
                throw new XPathException(expression, "Failed to convert LocalXMLResource to node: " + xe.getMessage());
            }
        }

        DocumentImpl document;
        try {
            document = broker.getCollection(XmldbURI.xmldbUriFor(xres.getParentCollection().getName())).getDocument(broker, XmldbURI.xmldbUriFor(xres.getDocumentId()));
        } catch (final URISyntaxException xe) {
            throw new XPathException(expression, xe);
        } catch (final XMLDBException xe) {
            throw new XPathException(expression, "Failed to get document for RemoteXMLResource: " + xe.getMessage());
        } catch (final PermissionDeniedException pde) {
            throw new XPathException(expression, "Failed to get document: " + pde.getMessage());
        }
        final NodeId nodeId = broker.getBrokerPool().getNodeFactory().createFromString(((RemoteXMLResource) xres).getNodeId());
        return new NodeProxy(null, document, nodeId);

    }
}
