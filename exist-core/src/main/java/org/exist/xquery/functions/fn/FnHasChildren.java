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
package org.exist.xquery.functions.fn;

import org.exist.dom.QName;
import org.exist.dom.persistent.NodeProxy;
import org.exist.xquery.*;
import org.exist.xquery.value.*;
import org.w3c.dom.Node;

public class FnHasChildren extends Function {

    private final static QName QN_HAS_CHILDREN = new QName("has-children", Function.BUILTIN_FUNCTION_NS);

    public final static FunctionSignature FNS_HAS_CHILDREN_0 = new FunctionSignature(
            QN_HAS_CHILDREN,
            "Returns true if the context item has one or more child nodes",
            FunctionSignature.NO_ARGS,
            new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE, "true if the context item has one of more child nodes, false otherwise")
    );

    public final static FunctionSignature FNS_HAS_CHILDREN_1 = new FunctionSignature(
            QN_HAS_CHILDREN,
            "Returns true if the supplied node has one or more child nodes",
            new SequenceType[] {
                    new FunctionParameterSequenceType("node", Type.NODE, Cardinality.ZERO_OR_ONE, "The node to test")
            },
            new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE, "true if $node has one of more child nodes, false otherwise")
    );

    public FnHasChildren(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence contextSequence, final Item contextItem) throws XPathException {
        final NodeValue node;

        if(getArgumentCount() == 0) {
            // default to the context item

            if(contextSequence == null) {

                if (contextItem == null) {
                    throw new XPathException(this, ErrorCodes.XPDY0002, "Context item is absent");
                }
                contextSequence = contextItem.toSequence();
            }

            if(contextSequence.isEmpty()) {
                return BooleanValue.FALSE;
            }

            final Item item = contextSequence.itemAt(0);
            if(!Type.subTypeOf(item.getType(), Type.NODE)) {
                throw new XPathException(this, ErrorCodes.XPTY0004, "Context item is not a node()");
            }

            node = (NodeValue)item;

        } else {
            final Sequence arg0 = getArgument(0).eval(contextSequence, contextItem);
            if(getArgumentCount() == 1 && arg0.isEmpty()) {
                return BooleanValue.FALSE;
            } else {
                node = (NodeValue)arg0.itemAt(0);
            }
        }

        final Node w3cNode;
        if(node instanceof NodeProxy) {
            w3cNode = node.getNode();
        } else if(node instanceof org.exist.dom.memtree.NodeImpl) {
            w3cNode = ((org.exist.dom.memtree.NodeImpl)node);
        } else {
            throw new XPathException(this, ErrorCodes.XPTY0004, "Context item is not a node()");
        }

        return BooleanValue.valueOf(w3cNode.hasChildNodes());
    }
}
