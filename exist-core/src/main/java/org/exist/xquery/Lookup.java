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

import io.lacuna.bifurcan.IEntry;
import org.exist.xquery.functions.array.ArrayType;
import org.exist.xquery.functions.map.AbstractMapType;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.*;

import javax.annotation.Nullable;

/**
 * Implements the XQuery 3.1 lookup operator on maps and arrays.
 *
 * @author Wolfgang
 */
public class Lookup extends AbstractExpression {

    private final Expression contextExpression;
    private @Nullable Sequence keys = null;
    private @Nullable Expression keyExpression = null;

    public Lookup(final XQueryContext context, final Expression contextExpression) {
        super(context);
        this.contextExpression = contextExpression;
    }

    public Lookup(final XQueryContext context, final Expression contextExpression, final String keyString) {
        this(context, contextExpression);
        this.keys = new StringValue(contextExpression, keyString);
    }

    public Lookup(final XQueryContext context, final Expression contextExpression, final int position) {
        this(context, contextExpression);
        this.keys = new IntegerValue(contextExpression, position);
    }

    public Lookup(final XQueryContext context, final Expression contextExpression, @Nullable final Expression keyExpression) {
        this(context, contextExpression);
        this.keyExpression = keyExpression;
    }

    @Override
    public void analyze(final AnalyzeContextInfo contextInfo) throws XPathException {
        final AnalyzeContextInfo contextCopy = new AnalyzeContextInfo(contextInfo);
        if (contextExpression != null) {
            contextExpression.analyze(contextCopy);
        }
        if (keyExpression != null) {
            keyExpression.analyze(contextCopy);
        }
    }

    @Override
    public Sequence eval(Sequence contextSequence, final Item contextItem) throws XPathException {
        if (contextItem != null) {
            contextSequence = contextItem.toSequence();
        }

        final Sequence leftSeq;
        if (contextExpression == null && contextSequence == null) {
            throw new XPathException(this, ErrorCodes.XPDY0002, "Lookup has nothing to select, the context item is absent");

        } else if (contextExpression == null) {
            leftSeq = contextSequence;

        } else {
            leftSeq = contextExpression.eval(contextSequence, null);
        }
        final int contextType = leftSeq.getItemType();

        // Make compatible with baseX and Saxon
        if (leftSeq.isEmpty()) {
            return Sequence.EMPTY_SEQUENCE;
        }

        if (!(Type.subTypeOf(contextType, Type.MAP_ITEM) || Type.subTypeOf(contextType, Type.ARRAY_ITEM))) {
            throw new XPathException(this, ErrorCodes.XPTY0004, "expression to the left of a lookup operator needs to be a sequence of maps or arrays");
        }

        if (keyExpression != null) {
            keys = keyExpression.eval(contextSequence, null);
            if (keys.isEmpty()) {
                return Sequence.EMPTY_SEQUENCE;
            }
        }

        try {
            final ValueSequence result = new ValueSequence();
            for (final SequenceIterator i = leftSeq.iterate(); i.hasNext(); ) {
                final LookupSupport item = (LookupSupport) i.nextItem();

                if (keys != null) {
                    for (final SequenceIterator j = keys.iterate(); j.hasNext(); ) {
                        final AtomicValue key = j.nextItem().atomize();
                        final Sequence value = item.get(key);
                        if (value != null) {
                            result.addAll(value);
                        }
                    }

                } else if (item instanceof ArrayType) {
                    result.addAll(item.keys());

                } else if (item instanceof AbstractMapType) {
                    for (final IEntry<AtomicValue, Sequence> entry : ((AbstractMapType)item)) {
                        result.addAll(entry.value());
                    }
                }
            }

            return result;

        } catch (final XPathException e) {
            e.setLocation(getLine(), getColumn(), getSource());
            throw e;
        }
    }

    @Override
    public int returnsType() {
        return Type.ITEM;
    }

    @Override
    public Cardinality getCardinality() {
        return Cardinality.ZERO_OR_MORE;
    }

    @Override
    public void dump(final ExpressionDumper dumper) {
        if (contextExpression != null) {
            contextExpression.dump(dumper);
        }

        dumper.display("?");

        if (keyExpression == null && keys != null && keys.getItemCount() > 0) {
            try {
                dumper.display(keys.itemAt(0).getStringValue());
            } catch (final XPathException e) {
                // impossible
            }

        } else if (keyExpression != null) {
            keyExpression.dump(dumper);
        }
    }

    @Override
    public void resetState(final boolean postOptimization) {
        super.resetState(postOptimization);

        if (contextExpression != null) {
            contextExpression.resetState(postOptimization);
        }

        if (keyExpression != null) {
            keyExpression.resetState(postOptimization);
        }
    }

    public interface LookupSupport {

        Sequence get(AtomicValue key) throws XPathException;

        Sequence keys() throws XPathException;
    }
}
