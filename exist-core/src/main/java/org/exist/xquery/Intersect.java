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
package org.exist.xquery;

import org.exist.xquery.value.*;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
public class Intersect extends CombiningExpression {

    public Intersect(final XQueryContext context, final PathExpr left, final PathExpr right) {
        super(context, left, right);
    }

    @Override
    public Sequence combine(final Sequence ls, final Sequence rs) throws XPathException {
        final Sequence result;
        if (ls.isEmpty() || rs.isEmpty()) {
            result = Sequence.EMPTY_SEQUENCE;
        } else {
            if (!(Type.subTypeOf(ls.getItemType(), Type.NODE) && Type.subTypeOf(rs.getItemType(), Type.NODE))) {
                throw new XPathException(this, ErrorCodes.XPTY0004, "intersect operand is not a node sequence");
            }
            if (ls.isPersistentSet() && rs.isPersistentSet()) {
                result = ls.toNodeSet().intersection(rs.toNodeSet());
            } else {
                result = new ValueSequence(true);
                final Set<Item> set = new TreeSet<>(new ItemComparator());
                for (final SequenceIterator i = ls.unorderedIterator(); i.hasNext(); ) {
                    set.add(i.nextItem());
                }
                for (final SequenceIterator i = rs.unorderedIterator(); i.hasNext(); ) {
                    final Item next = i.nextItem();
                    if (set.contains(next)) {
                        result.add(next);
                    }
                }
                result.removeDuplicates();
            }
        }

        return result;
    }

    @Override
    protected String getOperatorName() {
        return "intersect";
    }

    @Override
    public void accept(final ExpressionVisitor visitor) {
        visitor.visitIntersectionExpr(this);
    }
}
