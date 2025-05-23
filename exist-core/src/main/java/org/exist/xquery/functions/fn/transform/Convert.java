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
 */
package org.exist.xquery.functions.fn.transform;

import it.unimi.dsi.fastutil.ints.IntList;
import net.sf.saxon.s9api.*;
import net.sf.saxon.type.BuiltInAtomicType;
import org.exist.dom.QName;
import org.exist.dom.persistent.NodeProxy;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.XPathException;
import org.exist.xquery.functions.array.ArrayType;
import org.exist.xquery.functions.fn.FnTransform;
import org.exist.xquery.value.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Type conversion to and from Saxon
 *
 * <p>
 *     Used to convert values to/from Saxon when we use Saxon as the XSLT transformer
 *     as in the fn:transform implementation {@link FnTransform}
 *     A very minimal set of conversions where they are absolutely needed.
 *     Most conversion is carried out via a document, but that is insufficient in a few cases
 *     (where the delivery-format is raw and a template has a specified output type
 *     e.g. <xsl:template name='main' as='xs:integer'></xsl:template>
 *
 *     The correct path would be to make this conversion comprehensive and use it in all cases.
 *     It's not clear how easy or hard that would be.
 * </p>
 */
class Convert {

    private Convert() {
        super();
    }

    static class ToExist {

        private ToExist() { super(); }

        static Sequence of(final XdmValue xdmValue) throws XPathException {
            if (xdmValue.size() == 0) {
                return Sequence.EMPTY_SEQUENCE;
            }

            final ValueSequence valueSequence = new ValueSequence();
            for (final XdmItem xdmItem : xdmValue) {

                valueSequence.add(ToExist.ofItem(xdmItem));
            }
            return valueSequence;
        }

        static Item ofItem(final XdmItem xdmItem) throws XPathException {

            if (xdmItem.isAtomicValue()) {
                final net.sf.saxon.value.AtomicValue atomicValue = (net.sf.saxon.value.AtomicValue) xdmItem.getUnderlyingValue();
                final BuiltInAtomicType atomicType = atomicValue.getPrimitiveType();
                if (atomicType == BuiltInAtomicType.INTEGER) {
                    return new IntegerValue(atomicValue.getStringValue());
                } else if (atomicType == BuiltInAtomicType.DOUBLE) {
                    return new DoubleValue(atomicValue.getStringValue());
                } else {
                    throw new XPathException(ErrorCodes.XPTY0004,
                            "net.sf.saxon.value.AtomicValue " + atomicValue + COULD_NOT_BE_CONVERTED + "atomic value");
                }
            } else if (xdmItem instanceof XdmNode) {
                return ToExist.ofNode((XdmNode)xdmItem);
            }

            throw new XPathException(ErrorCodes.XPTY0004,
                    "XdmItem " + xdmItem + COULD_NOT_BE_CONVERTED + "Sequence");
        }

        static NodeValue ofNode(final XdmNode xdmNode) throws XPathException {

            throw new XPathException(ErrorCodes.XPTY0004,
                    "XdmNode " + xdmNode + COULD_NOT_BE_CONVERTED + " Node");
        }
    }

    static final private String COULD_NOT_BE_CONVERTED = " could not be converted to an eXist ";

    abstract static class ToSaxon {

        abstract DocumentBuilder newDocumentBuilder();

        static net.sf.saxon.s9api.QName of(final QName qName) {
            return new net.sf.saxon.s9api.QName(qName.getPrefix() == null ? "" : qName.getPrefix(), qName.getNamespaceURI(), qName.getLocalPart());
        }

        static net.sf.saxon.s9api.QName of(final QNameValue qName) {
            return of(qName.getQName());
        }

        XdmValue of(final Item item) throws XPathException {
            if (item instanceof NodeProxy nodeProxy) {
                return ofNode(nodeProxy.getNode());
            }
            final int itemType = item.getType();
            if (Type.subTypeOf(itemType, Type.ANY_ATOMIC_TYPE)) {
                return ofAtomic((AtomicValue) item);
            } else if (Type.subTypeOf(itemType, Type.NODE)) {
                if (item instanceof NodeProxy) {
                    return ofNode(((NodeProxy) item).getNode());
                } else {
                    return ofNode((Node) item);
                }
            }
            throw new XPathException(ErrorCodes.XPTY0004,
                    "Item " + item + " of type " + Type.getTypeName(itemType) + COULD_NOT_BE_CONVERTED + "XdmValue");
        }

        static private XdmValue ofAtomic(final AtomicValue atomicValue) throws XPathException {
            final int itemType = atomicValue.getType();
            if (Type.subTypeOf(itemType, Type.INTEGER)) {
                return XdmValue.makeValue(((IntegerValue) atomicValue).getInt());
            } else if (Type.subTypeOf(itemType, Type.NUMERIC)) {
                return XdmValue.makeValue(((NumericValue) atomicValue).getDouble());
            } else if (Type.subTypeOf(itemType, Type.BOOLEAN)) {
                return XdmValue.makeValue(((BooleanValue) atomicValue).getValue());
            } else if (Type.subTypeOf(itemType, Type.STRING)) {
                return XdmValue.makeValue(((StringValue) atomicValue).getStringValue());
            }

            throw new XPathException(ErrorCodes.XPTY0004,
                    "Atomic value " + atomicValue + " of type " + Type.getTypeName(itemType) +
                            COULD_NOT_BE_CONVERTED + "XdmValue");
        }

        private XdmValue ofNode(final Node node) throws XPathException {

            final DocumentBuilder sourceBuilder = newDocumentBuilder();
            try {
                if (node instanceof Document) {
                    return sourceBuilder.build(new DOMSource(node));

                } else {
                    //The source must be part of a document
                    final Document document = node.getOwnerDocument();
                    if (document == null) {
                        throw new XPathException(ErrorCodes.XPTY0004, "Node " + node + COULD_NOT_BE_CONVERTED + "XdmValue, as it is not part of a document.");
                    }
                    final boolean implicitDocument = node instanceof org.exist.dom.memtree.NodeImpl && !((org.exist.dom.memtree.DocumentImpl) node.getOwnerDocument()).isExplicitlyCreated();
                    final IntList nodeIndex = TreeUtils.treeIndex(node, implicitDocument);
                    final XdmNode xdmDocument = sourceBuilder.build(new DOMSource(document));
                    final XdmNode xdmNode = TreeUtils.xdmNodeAtIndex(xdmDocument, nodeIndex);
                    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                        final net.sf.saxon.s9api.QName attrName = new net.sf.saxon.s9api.QName(node.getPrefix() == null ? XMLConstants.DEFAULT_NS_PREFIX : node.getPrefix(), node.getNamespaceURI(), node.getLocalName());
                        final Iterator<XdmNode> itAttr = xdmNode.axisIterator(Axis.ATTRIBUTE, attrName);
                        if (itAttr.hasNext()) {
                            return itAttr.next();
                        }
                        throw new XPathException(ErrorCodes.XPTY0004, "Node " + node + COULD_NOT_BE_CONVERTED + "XdmValue");

                    } else {
                        return xdmNode;
                    }
                }
            } catch (final SaxonApiException e) {
                throw new XPathException(ErrorCodes.XPTY0004, "Node " + node + COULD_NOT_BE_CONVERTED + "XdmValue", e);
            }
        }

        XdmValue[] of(final ArrayType values) throws XPathException {
            final int size = values.getSize();
            final XdmValue[] result = new XdmValue[size];
            for (int i = 0; i < size; i++) {
                final Sequence sequence = values.get(i);
                result[i] = XdmValue.makeValue(listOf(sequence));
            }
            return result;
        }

        XdmValue of(final Sequence value) throws XPathException {
            if (value instanceof NodeProxy nodeProxy) {
                return ofNode(nodeProxy.getNode());
            }
            return XdmValue.makeSequence(listOf(value));
        }

        private List<XdmValue> listOf(final Sequence value) throws XPathException {
            final int size = value.getItemCount();
            final List<XdmValue> result = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                result.add(of(value.itemAt(i)));
            }
            return result;
        }
    }
}
