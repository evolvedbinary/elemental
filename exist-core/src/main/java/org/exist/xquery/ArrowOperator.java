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

import org.exist.dom.QName;
import org.exist.dom.QName.IllegalQNameException;
import org.exist.xquery.util.ExpressionDumper;
import org.exist.xquery.value.FunctionReference;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Implements the XQuery 3.1 arrow operator.
 *
 * @author wolf
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ArrowOperator extends AbstractExpression {

    private @Nullable QName functionName = null;
    private Expression leftExpr;
    @Nullable FunctionCall functionCall = null;
    private @Nullable Expression functionSpecExpr = null;
    private List<Expression> functionParameters;
    private AnalyzeContextInfo cachedContextInfo;

    public ArrowOperator(final XQueryContext context, final Expression leftExpr) throws
            XPathException {
        super(context);
        this.leftExpr = leftExpr;
    }

    /**
     * Set the XPath/XQuery function to be called by name.
     *
     * @param functionName       the fully qualified name of the function to call.
     * @param functionParameters the parameters for the function.
     * @throws XPathException if the fully qualified name is invalid.
     */
    public void setArrowFunction(final String functionName, final List<Expression> functionParameters) throws XPathException {
        try {
            this.functionName = QName.parse(context, functionName, context.getDefaultFunctionNamespace());
            this.functionParameters = functionParameters;
            // defer resolving the function to analyze to make sure all functions are known
        } catch (final IllegalQNameException e) {
            throw new XPathException(this, ErrorCodes.XPST0081, "No namespace defined for prefix " + functionName);
        }
    }

    /**
     * Set the XPath/XQuery function to be called by XPath expression.
     *
     * @param functionSpecExpr   the expr that specifies the function to call.
     * @param functionParameters the parameters for the function.
     */
    public void setArrowFunction(final PathExpr functionSpecExpr, final List<Expression> functionParameters) {
        this.functionSpecExpr = functionSpecExpr.simplify();
        this.functionParameters = functionParameters;
    }

    @Override
    public void analyze(final AnalyzeContextInfo contextInfo) throws XPathException {
        if (getContext().getXQueryVersion() < 31) {
            throw new XPathException(this, ErrorCodes.EXXQDY0003, "The Arrow Operator is not available before XQuery 3.1");
        }

        if (functionName != null) {
            functionCall = NamedFunctionReference.lookupFunction(this, context, functionName, functionParameters.size() + 1);
        }

        this.cachedContextInfo = contextInfo;
        leftExpr.analyze(contextInfo);

        if (functionCall != null) {
            functionCall.analyze(contextInfo);
        }

        if (functionSpecExpr != null) {
            functionSpecExpr.analyze(contextInfo);
        }
    }

    @Override
    public Sequence eval(Sequence contextSequence, final Item contextItem) throws XPathException {
        if (contextItem != null) {
            contextSequence = contextItem.toSequence();
        }
        contextSequence = leftExpr.eval(contextSequence, null);

        @Nullable FunctionReference functionReference = null;
        try {
            if (functionCall != null) {
                // Prepare call by name
                functionReference = new FunctionReference(this, functionCall);

            } else {
                // Prepare call by expression
                final Sequence funcSeq = functionSpecExpr.eval(contextSequence, contextItem);
                if (funcSeq.getCardinality() != Cardinality.EXACTLY_ONE) {
                    throw new XPathException(this, ErrorCodes.XPTY0004, "Expected exactly one function item, got " + funcSeq.getItemCount() + ". Expression: " + ExpressionDumper.dump(functionSpecExpr));
                }

                final Item item0 = funcSeq.itemAt(0);
                if (!Type.subTypeOf(item0.getType(), Type.FUNCTION_REFERENCE)) {
                    throw new XPathException(this, ErrorCodes.XPTY0004, "Type error: expected function, got " + Type.getTypeName(item0.getType()));
                }

                functionReference = (FunctionReference) item0;
            }

            // Make the call
            final List<Expression> fparams = new ArrayList<>(functionParameters.size() + 1);
            fparams.add(new ContextParam(context, contextSequence));
            fparams.addAll(functionParameters);

            functionReference.setArguments(fparams);
            // need to create a new AnalyzeContextInfo to avoid memory leak
            // cachedContextInfo will stay in memory
            functionReference.analyze(new AnalyzeContextInfo(cachedContextInfo));
            // Evaluate the function
            return functionReference.eval(null);

        } finally {
            if (functionReference != null) {
                functionReference.close();
            }
        }
    }

    @Override
    public int returnsType() {
        return functionCall == null ? Type.ITEM : functionCall.returnsType();
    }

    @Override
    public Cardinality getCardinality() {
        return functionCall == null ? super.getCardinality() : functionCall.getCardinality();
    }

    @Override
    public void dump(final ExpressionDumper dumper) {
        leftExpr.dump(dumper);
        dumper.display(" => ");

        if (functionCall != null) {
            dumper.display(functionCall.getFunction().getName()).display('(');
        } else {
            functionSpecExpr.dump(dumper);
        }

        for (int i = 0; i < functionParameters.size(); i++) {
            if (i > 0) {
                dumper.display(", ");
                functionParameters.get(i).dump(dumper);
            }
        }
        dumper.display(')');
    }

    @Override
    public void resetState(final boolean postOptimization) {
        super.resetState(postOptimization);
        leftExpr.resetState(postOptimization);

        if (functionCall != null) {
            functionCall.resetState(postOptimization);
        }

        if (functionSpecExpr != null) {
            functionSpecExpr.resetState(postOptimization);
        }

        for (final Expression param : functionParameters) {
            param.resetState(postOptimization);
        }
    }

    static class ContextParam extends Function.Placeholder {
        @Nullable
        Sequence sequence;

        ContextParam(final XQueryContext context, @Nullable final Sequence sequence) {
            super(context);
            this.sequence = sequence;
        }

        @Override
        public void analyze(final AnalyzeContextInfo contextInfo) throws XPathException {
        }

        @Override
        public Sequence eval(final Sequence contextSequence, final Item contextItem) throws XPathException {
            return sequence;
        }

        @Override
        public int returnsType() {
            if (sequence == null) {
                return Type.ITEM;
            }
            return sequence.getItemType();
        }

        @Override
        public void dump(final ExpressionDumper dumper) {
        }
    }
}
