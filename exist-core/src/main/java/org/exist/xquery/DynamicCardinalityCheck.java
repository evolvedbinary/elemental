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

import org.exist.dom.persistent.DocumentSet;
import org.exist.xquery.util.Error;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;

import javax.annotation.Nullable;

/**
 * Runtime-check for the cardinality of a function parameter.
 * 
 * @author wolf
 */
public class DynamicCardinalityCheck extends AbstractExpression {

    final private Expression expression;
    final private Cardinality requiredCardinality;
    private Error error;

    public DynamicCardinalityCheck(final XQueryContext context, final Cardinality requiredCardinality,
            final Expression expr, final Error error) {
        super(context);
        this.requiredCardinality = requiredCardinality;
        this.expression = expr;
        this.error = error;
        setLocation(expression.getLine(), expression.getColumn());
    }

    @Override
    public void analyze(final AnalyzeContextInfo contextInfo) throws XPathException {
        contextInfo.setParent(this);
        expression.analyze(contextInfo);
    }

    @Override
    public Sequence eval(final Sequence contextSequence, final Item contextItem) throws XPathException {
        if (context.getProfiler().isEnabled()) {
            context.getProfiler().start(this);
            context.getProfiler().message(this, Profiler.DEPENDENCIES, "DEPENDENCIES", Dependency.getDependenciesName(this.getDependencies()));
            if (contextSequence != null) {
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT SEQUENCE", contextSequence);
            }
            if (contextItem != null) {
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT ITEM", contextItem.toSequence());
            }
        }

        final Sequence seq = expression.eval(contextSequence, contextItem);

        final Cardinality actualCardinality;
        if (isEmpty(seq)) {
            actualCardinality = Cardinality.EMPTY_SEQUENCE;
        } else if (hasMany(seq)) {
            actualCardinality = Cardinality._MANY;
        } else {
            actualCardinality = Cardinality.EXACTLY_ONE;
        }

        if (!requiredCardinality.isSuperCardinalityOrEqualOf(actualCardinality)) {
            error.addArgs(ExpressionDumper.dump(expression), requiredCardinality.getHumanDescription(), seq.getItemCount());
            throw new XPathException(this, error.toString());
        }

        if (context.getProfiler().isEnabled()) {
            context.getProfiler().end(this, "", seq);
        }

        return seq;
    }

    private boolean isEmpty(final Sequence sequence) throws XPathException {
        final boolean empty = sequence.isEmpty();
        throwIfDeferredFunctionCallAndHasException(sequence);
        return empty;
    }

    private boolean hasMany(final Sequence sequence) throws XPathException {
        final boolean hasMany = sequence.hasMany();
        throwIfDeferredFunctionCallAndHasException(sequence);
        return hasMany;
    }

    private void throwIfDeferredFunctionCallAndHasException(final Sequence sequence) throws XPathException {
        if (sequence instanceof DeferredFunctionCall) {
            @Nullable final XPathException caughtException = ((DeferredFunctionCall) sequence).getCaughtException();
            if (caughtException != null) {
                throw caughtException;
            }
        }
    }

    @Override
    public void dump(final ExpressionDumper dumper) {
        if(dumper.verbosity() > 1) {
            dumper.display("dynamic-cardinality-check"); 
            dumper.display("("); 
            dumper.display("\"" + requiredCardinality.getHumanDescription() + "\"");
            dumper.display(", ");
        }
        expression.dump(dumper);
        if(dumper.verbosity() > 1)
            {dumper.display(")");}
    }

    public String toString() {
        return expression.toString();
    }

    @Override
    public int returnsType() {
        return expression.returnsType();
    }

    @Override
    public int getDependencies() {
        return expression.getDependencies();
    }

    public void setContextDocSet(final DocumentSet contextSet) {
        super.setContextDocSet(contextSet);
        expression.setContextDocSet(contextSet);
    }

    @Override
    public void resetState(final boolean postOptimization) {
        super.resetState(postOptimization);
        expression.resetState(postOptimization);
    }

    @Override
    public void accept(final ExpressionVisitor visitor) {
        expression.accept(visitor);
    }

    @Override
    public int getSubExpressionCount() {
        return 1;
    }

    @Override
    public Expression getSubExpression(final int index) {
        if (index == 0) {
            return expression;
        }
        throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + getSubExpressionCount());
    }
}
