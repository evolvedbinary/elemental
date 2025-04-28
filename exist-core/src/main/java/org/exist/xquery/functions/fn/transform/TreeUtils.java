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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.sf.saxon.s9api.XdmNode;
import org.exist.xquery.value.NodeValue;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TreeUtils {

    private TreeUtils() {
        super();
    }

    static StringBuilder pathTo(final Node node) {
        if (node instanceof Document) {
            final Document document = (Document) node;
            return new StringBuilder().append(document.getDocumentURI());
        }
        final List<Node> priors = new ArrayList<>();
        Node prev = node;
        while (prev != null) {
            priors.add(prev);
            prev = prev.getPreviousSibling();
        }
        final Node parent = priors.get(0).getParentNode();
        final StringBuilder sb;
        if (parent == null || parent instanceof Document) {
            sb = new StringBuilder();
        } else {
            sb = pathTo(parent).append('/');
        }
        for (final Node prior : priors) {
            sb.append(((NodeValue)prior).getQName()).append(';');
        }

        return sb;
    }

    static IntList treeIndex(Node node, final boolean implicitDocument) {
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
            node = ((Attr) node).getOwnerElement();
        }

        Node parent = node.getParentNode();
        if (parent == null) {
            if (node.getNodeType() != Node.DOCUMENT_NODE && implicitDocument) {
                return IntArrayList.of(0);
            } else {
                return new IntArrayList();
            }
        }

        final IntList index = treeIndex(parent, implicitDocument);
        Node sibling = node.getPreviousSibling();
        int position = 0;
        while (sibling != null) {
            position += 1;
            sibling = sibling.getPreviousSibling();
        }
        index.add(position);

        return index;
    }

    static @Nullable XdmNode xdmNodeAtIndex(final XdmNode xdmNode, final IntList index) {
        if (index.isEmpty()) {
            return xdmNode;
        } else {
            final int firstIndex = index.getInt(0);
            int i = 0;
            for (final XdmNode child : xdmNode.children()) {
                if (i++ == firstIndex) {
                    return xdmNodeAtIndex(child, index.subList(1, index.size()));
                }
            }
        }
        return null;
    }
}
