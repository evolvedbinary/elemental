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
import org.exist.numbering.NodeId;
import org.exist.xquery.*;
import org.exist.xquery.value.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class FnInnerMost extends BasicFunction {

    public final static FunctionSignature FNS_INNERMOST = new FunctionSignature(
            new QName("innermost", Function.BUILTIN_FUNCTION_NS),
            "Returns every node within the input sequence that is not an ancestor of another member of the input sequence; the nodes are returned in document order with duplicates eliminated.",
            new SequenceType[] {
                    new FunctionParameterSequenceType("nodes", Type.NODE, Cardinality.ZERO_OR_MORE, "The nodes to test")
            },
            new FunctionReturnSequenceType(Type.NODE, Cardinality.ZERO_OR_MORE, "The nodes that are not an ancestor of another node in the input sequence")
    );

    public FnInnerMost(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final Sequence nodes = args[0];
        if(nodes.isEmpty()) {
            return Sequence.EMPTY_SEQUENCE;
        } else if(nodes.hasOne()) {
            return nodes;
        } else {
            final Sequence results = new ValueSequence();

            final List<NodeId> nodeIds = getNodeIds(nodes);
            final Set<NodeId> found = new HashSet<>();

            final SequenceIterator it = nodes.iterate();
            while(it.hasNext()) {
                final Item item = it.nextItem();
                final NodeValue node = ((NodeValue)item);
                final NodeId currentNodeId = node.getNodeId();

                if(!found.contains(currentNodeId) &&
                        nodeIds.parallelStream().noneMatch(nodeId -> nodeId.isDescendantOf(currentNodeId))) {
                    results.add(node);
                    found.add(currentNodeId);
                }
            }

            return results;
        }
    }

    private List<NodeId> getNodeIds(final Sequence nodes) throws XPathException {
        final List<NodeId> nodeIds = new ArrayList<>();
        final SequenceIterator it = nodes.iterate();
        while(it.hasNext()) {
            final Item item = it.nextItem();
            final NodeValue node = ((NodeValue)item);
            nodeIds.add(node.getNodeId());
        }
        return nodeIds;
    }
}
