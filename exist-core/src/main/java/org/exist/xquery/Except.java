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
package org.exist.xquery;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import org.exist.xquery.value.*;

import javax.annotation.Nullable;

/**
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
public class Except extends CombiningExpression {

    public Except(final XQueryContext context, final PathExpr left, final PathExpr right) {
        super(context, left, right);
    }

    @Override
    public Sequence combine(final Sequence ls, final Sequence rs) throws XPathException {
        final Sequence result;
        if (ls.isEmpty()) {
            result = Sequence.EMPTY_SEQUENCE;
        } else if (rs.isEmpty()) {
            if (!Type.subTypeOf(ls.getItemType(), Type.NODE)) {
                throw new XPathException(this, ErrorCodes.XPTY0004, "except operand is not a node sequence");
            }
            result = ls;
        } else {
            if (!(Type.subTypeOf(ls.getItemType(), Type.NODE) && Type.subTypeOf(rs.getItemType(), Type.NODE))) {
                throw new XPathException(this, ErrorCodes.XPTY0004, "except operand is not a node sequence");
            }
            if (ls.isPersistentSet() && rs.isPersistentSet()) {
                result = ls.toNodeSet().except(rs.toNodeSet());
            } else {
                @Nullable Sequence values = null;
                @Nullable Set<Item> set = null;
                for (final SequenceIterator i = rs.unorderedIterator(); i.hasNext(); ) {
                    if (set == null) {
                        set = new ObjectAVLTreeSet<>(ItemComparator.WITHOUT_COLLATOR);
                    }
                    set.add(i.nextItem());
                }
                for (final SequenceIterator i = ls.unorderedIterator(); i.hasNext(); ) {
                    final Item next = i.nextItem();
                    if (set == null || !set.contains(next)) {
                        if (values == null) {
                            values = new ValueSequence();
                        }
                        values.add(next);
                    }
                }
                if (values != null) {
                    values.removeDuplicates();
                    result = values;
                } else {
                    result = Sequence.EMPTY_SEQUENCE;
                }
            }
        }

        return result;
    }

    @Override
    protected String getOperatorName() {
        return "except";
    }
}
