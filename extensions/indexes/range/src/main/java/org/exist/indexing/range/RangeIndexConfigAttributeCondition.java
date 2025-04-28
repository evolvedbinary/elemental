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
package org.exist.indexing.range;

import org.exist.dom.QName;
import org.exist.storage.ElementValue;
import org.exist.storage.NodePath;
import org.exist.util.DatabaseConfigurationException;
import org.exist.xquery.*;
import org.exist.xquery.modules.range.RangeQueryRewriter;
import org.exist.indexing.range.RangeIndex.Operator;
import org.exist.xquery.value.AtomicValue;
import org.exist.xquery.value.NumericValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.exist.util.StringUtil.nullIfEmpty;

/**
 *
 * A condition that can be defined for complex range config elements
 * that compares an attribute.
 *
 * @author Marcel Schaeben
 */
public class RangeIndexConfigAttributeCondition extends RangeIndexConfigCondition{

    private final String attributeName;
    private final QName attribute;
    private @Nullable final String value;
    private final Operator operator;
    private final boolean caseSensitive;
    private final boolean numericComparison;
    private @Nullable final Double numericValue;
    private @Nullable final Pattern pattern;

    private @Nullable String lowercaseValue = null;

    public RangeIndexConfigAttributeCondition(final Element elem, final NodePath parentPath) throws DatabaseConfigurationException {

        if (parentPath.getLastComponent().getNameType() == ElementValue.ATTRIBUTE) {
            throw new DatabaseConfigurationException(
                    "Range index module: Attribute condition cannot be defined for an attribute:" + parentPath);
        }

        this.attributeName = elem.getAttribute("attribute");
        if (this.attributeName.isEmpty()) {
            throw new DatabaseConfigurationException("Range index module: Empty or no attribute qname in condition");
        }

        try {
            this.attribute = new QName(QName.extractLocalName(this.attributeName), XMLConstants.NULL_NS_URI, QName.extractPrefix(this.attributeName), ElementValue.ATTRIBUTE);
        } catch (final QName.IllegalQNameException e) {
            throw new DatabaseConfigurationException("Rand index module error: " + e.getMessage(), e);
        }

        this.value = nullIfEmpty(elem.getAttribute("value"));

        // parse operator (default to 'eq' if missing)
        if (elem.hasAttribute("operator")) {
            final String operatorName = elem.getAttribute("operator");
            this.operator = Operator.getByName(operatorName.toLowerCase());
            if (this.operator == null) {
                throw new DatabaseConfigurationException("Range index module: Invalid operator specified in range index condition: " + operatorName + ".");
            }
        } else {
            this.operator = Operator.EQ;
        }


        final String caseString = elem.getAttribute("case");
        this.caseSensitive = !caseString.equalsIgnoreCase("no");
        final String numericString = elem.getAttribute("numeric");
        this.numericComparison = numericString.equalsIgnoreCase("yes");

        // try to create a pattern matcher for a 'matches' condition
        if (this.operator == Operator.MATCH) {

            if (value == null) {
                throw new DatabaseConfigurationException("Range index module: Operator 'matches' specified without value");
            }

            final int flags = this.caseSensitive ? 0 : CASE_INSENSITIVE;
            try {
                this.pattern = Pattern.compile(this.value, flags);
            } catch (final PatternSyntaxException e) {
                RangeIndex.LOG.error(e);
                throw new DatabaseConfigurationException("Range index module: Invalid regular expression in condition: " + this.value);
            }
        } else {
            this.pattern = null;
        }

        // try to parse the number value if numeric comparison is specified
        // store a reference to numeric value to avoid having to parse each time
        if (this.numericComparison) {

            switch (this.operator) {
                case MATCH:
                case STARTS_WITH:
                case ENDS_WITH:
                case CONTAINS:
                    throw new DatabaseConfigurationException("Range index module: Numeric comparison not applicable for operator: " + this.operator.name());
            }

            if (value == null) {
                throw new DatabaseConfigurationException("Range index module: Numeric 'comparison' specified without value");
            }

            try {
                this.numericValue = Double.parseDouble(this.value);
            } catch (final NumberFormatException e)  {
                throw new DatabaseConfigurationException("Range index module: Numeric attribute condition specified, but required value cannot be parsed as number: " + this.value);
            }
        } else {
            this.numericValue = null;
        }

    }

    // lazily evaluate lowercase value to convert once when needed
    private String getLowercaseValue() {
        if (this.lowercaseValue == null) {
            if (this.value != null) {
                this.lowercaseValue = this.value.toLowerCase();
            }
        }

        return this.lowercaseValue;
    }

    @Override
    public boolean matches(final Node node) {
        return node.getNodeType() == Node.ELEMENT_NODE
                && matchValue(((Element) node).getAttribute(attributeName));
    }

    private boolean matchValue(final String testValue) {
        return switch (operator) {
            case EQ -> booleanMatch(testValue);
            case NE -> !booleanMatch(testValue);
            case GT, LT, GE, LE -> matchOrdinal(operator, testValue);
            case ENDS_WITH -> stringMatch(String::endsWith, testValue);
            case STARTS_WITH -> stringMatch(String::startsWith, testValue);
            case CONTAINS -> stringMatch(String::contains, testValue);
            case MATCH -> {
                final Matcher matcher = pattern.matcher(testValue);
                yield matcher.matches();
            }
        };
    }

    private boolean stringMatch (final BiPredicate<String, String> predicate, final String testValue) {
        return caseSensitive
                ? predicate.test(testValue, value)
                : predicate.test(testValue.toLowerCase(), getLowercaseValue());
    }

    private boolean booleanMatch(final String testValue) {
        if (numericComparison) {
            final double testDouble = toDouble(testValue);
            return this.numericValue.equals(testDouble);
        }
        if (caseSensitive) {
            return value.equals(testValue);
        }
        return value.equalsIgnoreCase(testValue);
    }

    private boolean matchOrdinal(final Operator operator, final String testValue) {
        final int result;
        if (numericComparison) {
            final double testDouble = toDouble(testValue);
            result = Double.compare(testDouble, numericValue);
        } else if (caseSensitive) {
            result = testValue.compareTo(value);
        } else {
            result = testValue.toLowerCase().compareTo(getLowercaseValue());
        }

        return switch (operator) {
            case GT -> result > 0;
            case LT -> result < 0;
            case GE -> result >= 0;
            case LE -> result <= 0;
            default -> false;
        };
    }

    private Double toDouble(final String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e)  {
            RangeIndex.LOG.debug(
                    "Non-numeric value encountered for numeric condition on @'{}': {}", attributeName, value);
            return (double) 0;
        }
    }

    @Override
    public boolean find(final Predicate predicate) {
        final Expression inner = this.getInnerExpression(predicate);
        if (!(inner instanceof GeneralComparison || inner instanceof InternalFunctionCall)) {
            // predicate expression cannot be parsed as condition
            return false;
        }

        Operator rewrittenOperator;
        final Expression lhe;
        final Expression rhe;

        // get the type of the expression inside the predicate and determine right and left hand arguments
        if (inner instanceof GeneralComparison comparison) {

            rewrittenOperator = RangeQueryRewriter.getOperator(inner);
            lhe = comparison.getLeft();
            rhe = comparison.getRight();

        } else {
            // calls to matches() will not have been rewritten to a comparison, so check for function call
            final InternalFunctionCall funcCall = (InternalFunctionCall) inner;
            final Function func = funcCall.getFunction();

            if (!func.isCalledAs("matches")) {
                // predicate expression cannot be parsed as condition
                return false;
            }

            rewrittenOperator = Operator.MATCH;
            lhe = unwrapSubExpression(func.getArgument(0));
            rhe = unwrapSubExpression(func.getArgument(1));
        }

        // find the attribute name and value pair from the predicate to check against
        // first assume attribute is on the left and value is on the right
        LocationStep testStep = findLocationStep(lhe);
        AtomicValue testValue = findAtomicValue(rhe);

        if (testStep == null && testValue == null) {
            switch (operator) {
                // the equality operators are commutative so if attribute/value pair has not been found,
                // check the other way around
                case EQ, NE -> {
                    testStep = findLocationStep(rhe);
                    testValue = findAtomicValue(lhe);
                }
                // for ordinal comparisons, attribute and value can also be the other way around in the predicate
                // but the operator has to be inverted
                case GT, LT, GE, LE -> {
                    testStep = findLocationStep(rhe);
                    testValue = findAtomicValue(lhe);
                    rewrittenOperator = invertOrdinalOperator(rewrittenOperator);
                }
            }
        }

        if (testStep == null || testValue == null || !rewrittenOperator.equals(operator)) {
            return false;
        }

        final QName qname = testStep.getTest().getName();
        if (qname.getNameType() != ElementValue.ATTRIBUTE || !qname.equals(attribute)) {
            return false;
        }

        try {
            if (numericComparison) {
                return testValue instanceof NumericValue &&
                        testValue.toJavaObject(Double.class).equals(numericValue);
            }
            if (testValue instanceof StringValue) {
                final String testString = testValue.getStringValue();
                if (caseSensitive) {
                    return testString.equals(value);
                }
                return testString.equalsIgnoreCase(value);
            }
        } catch (XPathException e) {
            RangeIndex.LOG.error(
                    "Value conversion error when testing predicate for condition, value: {}", testValue.toString());
            RangeIndex.LOG.error(e);
        }
        return false;
    }

    private Expression unwrapSubExpression(Expression expr) {

        if (expr instanceof Atomize atomize) {
            expr = atomize.getExpression();
        }

        if (expr instanceof DynamicCardinalityCheck cardinalityCheck
                && expr.getSubExpressionCount() == 1) {
            expr = cardinalityCheck.getSubExpression(0);
        }

        if (expr instanceof PathExpr pathExpr &&
                expr.getSubExpressionCount() == 1) {
            expr = pathExpr.getSubExpression(0);
        }

        return expr;
    }

    private LocationStep findLocationStep(final Expression expr) {
        if (expr instanceof LocationStep step) {
            return step;
        }

        return null;
    }

    private AtomicValue findAtomicValue(final Expression expr) {
        if (expr instanceof AtomicValue atomic) {
            return atomic;
        }

        if (expr instanceof LiteralValue literal) {
            return literal.getValue();
        }

        if (!(expr instanceof VariableReference || expr instanceof Function)) {
            return null;
        }

        try {
            final ContextItemDeclaration cid = expr.getContext().getContextItemDeclartion();
            final Sequence result;
            if (cid == null) {
                result = expr.eval(null, null);
            } else {
                final Sequence contextSequence = cid.eval(null, null);
                result = expr.eval(contextSequence, null);
            }

            if (result instanceof AtomicValue atomic) {
                return atomic;
            }
            return null;
        } catch (XPathException e) {
            RangeIndex.LOG.error(e);
            return null;
        }
    }

    private Operator invertOrdinalOperator(Operator operator) {
        return switch (operator) {
            case LE -> Operator.GE;
            case GE -> Operator.LE;
            case LT -> Operator.GT;
            case GT -> Operator.LT;
            default -> null;
        };
    }
}
