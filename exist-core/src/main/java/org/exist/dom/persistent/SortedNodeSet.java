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
package org.exist.dom.persistent;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;
import com.evolvedbinary.j8cu.list.linked.BoundedDoublyLinkedList;
import com.evolvedbinary.j8cu.list.linked.OrderedDoublyLinkedList;
import org.exist.EXistException;
import org.exist.numbering.NodeId;
import org.exist.security.Subject;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.xquery.*;
import org.exist.xquery.parser.XQueryLexer;
import org.exist.xquery.parser.XQueryParser;
import org.exist.xquery.parser.XQueryTreeParser;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import java.io.StringReader;
import java.util.*;

import static com.evolvedbinary.j8cu.list.linked.Bounded.bound;


public class SortedNodeSet extends AbstractNodeSet {

    private final BoundedDoublyLinkedList<IteratorItem> list = bound(new OrderedDoublyLinkedList<IteratorItem>(), Long.MAX_VALUE);

    private final String sortExpr;
    private final BrokerPool pool;
    private final Subject user;

    public SortedNodeSet(final BrokerPool pool, final Subject user, final String sortExpr) {
        this.sortExpr = sortExpr;
        this.pool = pool;
        this.user = user;
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean hasOne() {
        return list.size() == 1;
    }

    @Override
    public void addAll(final Sequence other) throws XPathException {
        addAll(other.toNodeSet());
    }

    @Override
    public void addAll(final NodeSet other) {
        final long start = System.currentTimeMillis();
        final MutableDocumentSet docs = new DefaultDocumentSet();
        for (final NodeProxy p : other) {
            docs.add(p.getOwnerDocument());
        }
        // TODO(pkaminsk2): why replicate XQuery.compile here?
        try(final DBBroker broker = pool.get(Optional.ofNullable(user))) {

            final XQueryContext context = new XQueryContext(pool);
            final XQueryLexer lexer = new XQueryLexer(context, new StringReader(sortExpr));
            final XQueryParser parser = new XQueryParser(lexer);
            final XQueryTreeParser treeParser = new XQueryTreeParser(context);
            parser.xpath();
            if(parser.foundErrors()) {
                //TODO : error ?
                LOG.debug(parser.getErrorMessage());
            }
            final AST ast = parser.getAST();
            LOG.debug("generated AST: {}", ast.toStringTree());
            final PathExpr sortExpression = new PathExpr(context);
            try {
                treeParser.xpath(ast, sortExpression);
                if (treeParser.foundErrors()) {
                    LOG.debug(treeParser.getErrorMessage());
                }
                sortExpression.analyze(new AnalyzeContextInfo());
                for (final SequenceIterator i = other.iterate(); i.hasNext(); ) {
                    final NodeProxy p = (NodeProxy) i.nextItem();
                    final IteratorItem item = createIteratorItem(sortExpression, p);
                    list.add(item);
                }
            } finally {
                sortExpression.getContext().runCleanupTasks();
                sortExpression.getContext().reset();
            }
        } catch(final RecognitionException | TokenStreamException re) {
            LOG.debug(re); //TODO : throw exception ! -pb
        } catch(final EXistException | XPathException e) {
            LOG.debug("Exception during sort: " + e.getMessage(), e); //TODO : throw exception ! -pb
        }
        LOG.debug("sort-expression found {} in {}ms.", list.size(), System.currentTimeMillis() - start);
    }

    private IteratorItem createIteratorItem(final Expression sortExpression, final NodeProxy nodeProxy) throws XPathException {
        final Sequence seq = sortExpression.eval(nodeProxy, null);

        // copy string values of items into an array
        int sbCapacity = 0;
        final String[] strings = new String[seq.getItemCount()];
        for (int i = 0; i < strings.length; i++) {
            final String strItem = seq.itemAt(i).getStringValue().toUpperCase();
            sbCapacity += strItem.length();
            strings[i] = strItem;
        }

        // sort and then concatenate strings
        Arrays.sort(strings);
        final StringBuilder buf = new StringBuilder(sbCapacity);
        for (int i = 0; i < strings.length; i++) {
            buf.append(strings[i]);
        }

        return new IteratorItem(nodeProxy, buf.toString());
    }

    public void addAll(final NodeList other) {
        if (!(other instanceof NodeSet)) {
            throw new RuntimeException("not implemented!");
        }
        addAll((NodeSet) other);
    }

    @Override
    public boolean contains(final NodeProxy proxy) {
        for (final IteratorItem iteratorItem : list) {
            final NodeProxy p = iteratorItem.proxy;
            if (p.compareTo(proxy) == 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsReference(final Item item) {
        for (final IteratorItem iteratorItem : list) {
            final NodeProxy p = iteratorItem.proxy;
            if (p == item) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(final Item item) {
        for (final IteratorItem iteratorItem : list) {
            final NodeProxy p = iteratorItem.proxy;
            if (p.equals(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NodeProxy get(final int pos) {
        final IteratorItem item = list.get(pos);
        return item == null ? null : item.proxy;
    }

    public NodeProxy get(final DocumentImpl doc, final NodeId nodeId) {
        final NodeProxy proxy = new NodeProxy(null, doc, nodeId);
        for (final IteratorItem iteratorItem : list) {
            final NodeProxy p = iteratorItem.proxy;
            if (p.compareTo(proxy) == 0) {
                return p;
            }
        }
        return null;
    }

    @Override
    public NodeProxy get(final NodeProxy proxy) {
        for (final IteratorItem iteratorItem : list) {
            final NodeProxy p = iteratorItem.proxy;
            if (p.compareTo(proxy) == 0) {
                return p;
            }
        }
        return null;
    }

    @Override
    public int getLength() {
        return (int) list.size();
    }

    @Override
    public long getItemCountLong() {
        return list.size();
    }

    @Override
    public Node item(final int pos) {
        final IteratorItem iteratorItem = list.get(pos);
        if (iteratorItem != null) {
            final NodeProxy p = iteratorItem.proxy;
            if (p != null) {
                return p.getOwnerDocument().getNode(p);
            }
        }
        return null;
    }

    //TODO : evaluate both semantics (item/itemAt)
    @Override
    public Item itemAt(final int pos) {
        final IteratorItem iteratorItem = list.get(pos);
        return iteratorItem == null ? null : iteratorItem.proxy;
    }

    @Override
    public NodeSetIterator iterator() {
        return new SortedNodeSetIterator(list.iterator());
    }

    @Override
    public SequenceIterator iterate() {
        return new SortedNodeSetIterator(list.iterator());
    }

    @Override
    public SequenceIterator unorderedIterator() {
        return new SortedNodeSetIterator(list.iterator());
    }

    private static class SortedNodeSetIterator implements NodeSetIterator, SequenceIterator {
        private final Iterator<IteratorItem> ii;

        public SortedNodeSetIterator(final Iterator<IteratorItem> i) {
            ii = i;
        }

        public boolean hasNext() {
            return ii.hasNext();
        }

        @Override
        public NodeProxy next() {
            if (!ii.hasNext()) {
                throw new NoSuchElementException();
            } else {
                return ii.next().proxy;
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NodeProxy peekNode() {
            return null;
        }

        @Override
        public Item nextItem() {
            if (!ii.hasNext()) {
                return null;
            } else {
                return ii.next().proxy;
            }
        }

        @Override
        public void setPosition(final NodeProxy proxy) {
            throw new UnsupportedOperationException("NodeSetIterator.setPosition() is not supported by SortedNodeSetIterator");
        }
    }

    private static class IteratorItem implements Comparable<IteratorItem> {
        private final NodeProxy proxy;
        private @Nullable final String value;

        public IteratorItem(final NodeProxy proxy, final String value) {
            this.proxy = proxy;
            this.value = value;
        }

        @Override
        public int compareTo(final IteratorItem other) {
            if (value == null) {
                return other.value == null ? Constants.EQUAL : Constants.SUPERIOR;
            } else if (other.value == null) {
                return Constants.INFERIOR;
            } else {
                return value.compareTo(other.value);
            }
        }

        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof IteratorItem)) {
                return false;
            }

            final IteratorItem otherIteratorItem = (IteratorItem) other;
            return Objects.equals(value, otherIteratorItem.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }

    @Override
    public void add(final NodeProxy proxy) {
        LOG.info("Called SortedNodeSet.add()");
    }
}
