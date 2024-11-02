/*
 * Copyright (c) 2014, Evolved Binary Ltd
 */
package org.exist.xquery;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.xquery.functions.fn.FunPosition;
import org.exist.xquery.value.Type;

import javax.annotation.Nullable;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;

/**
 * Rewrite position() withing a predicate to a POSITIONAL version of the predicate where possible.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class PositionalPredicateRewriter extends QueryRewriter {

    public PositionalPredicateRewriter(final XQueryContext context) {
        super(context);
    }

    @Override
    public Pragma rewriteLocationStep(final LocationStep locationStep) throws XPathException {

        @Nullable final Predicate[] predicates = locationStep.getPredicates();
        if (predicates != null) {
            for (final Predicate predicate : predicates) {
                @Nullable final Tuple2<Expression, LiteralValue> predicateOptimizableInnerExpr = canOptimizeToPositionalPredicate(predicate);
                if (predicateOptimizableInnerExpr != null) {
                    predicate.replace(predicateOptimizableInnerExpr._1, predicateOptimizableInnerExpr._2);
                }
            }
        }

        return null;
    }

    /**
     * Test whether we can optimise position() withing a predicate to a POSITIONAL version of a predicate.
     *
     * @param predicate the predicate to test
     *
     * @return the inner expr to be replaced and the literal value to replace it with if it can be optimised, null otherwise.
     */
    private @Nullable Tuple2<Expression, LiteralValue> canOptimizeToPositionalPredicate(final Predicate predicate) {
        if (predicate.getSubExpressionCount() == 1) {
            final org.exist.xquery.Expression predInnerExpr = predicate.getSubExpression(0);
            if (predInnerExpr instanceof ValueComparison) {
                final ValueComparison valueComparison = (ValueComparison) predInnerExpr;
                if (valueComparison.getRelation() == Constants.Comparison.EQ) {
                    final org.exist.xquery.Expression left = valueComparison.getLeft();
                    final org.exist.xquery.Expression right = valueComparison.getRight();

                    if (left instanceof InternalFunctionCall
                        && right instanceof org.exist.xquery.LiteralValue) {

                        final InternalFunctionCall leftInternalFunctionCall = (InternalFunctionCall) left;
                        final org.exist.xquery.LiteralValue rightLiteralValue = (org.exist.xquery.LiteralValue) right;

                        if (leftInternalFunctionCall.getFunction() instanceof FunPosition && rightLiteralValue.returnsType() == Type.INTEGER) {
                            // NOTE(AR) if the predicate is like [position() eq 1] or [position() = 1] then we could rewrite it to [1]
                            return Tuple(predInnerExpr, rightLiteralValue);
                        }

                    } else if (left instanceof org.exist.xquery.LiteralValue
                        && right instanceof InternalFunctionCall) {

                        final org.exist.xquery.LiteralValue leftLiteralValue = (org.exist.xquery.LiteralValue) left;
                        final InternalFunctionCall rightInternalFunctionCall = (InternalFunctionCall) right;

                        if (leftLiteralValue.returnsType() == Type.INTEGER
                            && rightInternalFunctionCall.getFunction() instanceof FunPosition) {
                            // NOTE(AR) if the predicate is like [1 eq position()]  or [1 = position()] then we could rewrite it to [1]
                            return Tuple(predInnerExpr, leftLiteralValue);
                        }
                    }
                }
            }
        }
        return null;
    }
}
