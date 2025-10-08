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
package org.exist.test.runner;

import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.junit.ComparisonFailure;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import static org.exist.xquery.FunctionDSL.param;
import static org.exist.xquery.FunctionDSL.params;

public class ExtTestFailureFunction extends JUnitIntegrationFunction {

    private static final StringValue XPATH_MAP_KEY = new StringValue("xpath");
    private static final StringValue ERROR_MAP_KEY = new StringValue("error");
    private static final StringValue RESULT_MAP_KEY = new StringValue("result");

    public ExtTestFailureFunction(final XQueryContext context, final String parentName, final RunNotifier notifier) {
        super("ext-test-failure-function",
                params(
                        param("name", Type.STRING, "name of the test"),
                        param("expected", Type.MAP_ITEM, "expected result of the test"),
                        param("actual", Type.MAP_ITEM, "actual result of the test")
                ), context, parentName, notifier);
    }

    @Override
    public Sequence eval(final Sequence contextSequence, final Item contextItem) throws XPathException {
        final Sequence[] args = getCurrentArguments();
        if (args.length != 3) {
            throw new XPathException(this, "ext-test-failure-function requires 3 parameters");
        }

        final Sequence argName = args[0];
        if (argName.isEmpty()) {
            throw new XPathException(this, "ext-test-failure-function requires a 'name' parameter");
        }
        final String name = safeGetStringValue(argName.itemAt(0));

        final Sequence argExpected = args[1];
        if (argExpected.isEmpty()) {
            throw new XPathException(this, "ext-test-failure-function requires an 'expected' parameter");
        }
        final MapType expected = (MapType) argExpected.itemAt(0);

        final Sequence argActual = args[2];
        if (argActual.isEmpty()) {
            throw new XPathException(this, "ext-test-failure-function requires an 'actual' parameter");
        }
        final MapType actual = (MapType) argActual.itemAt(0);

        final Description description = createTestDescription(name);

        // notify JUnit
        try {
            final AssertionError failureReason = new ComparisonFailure("", expectedToString(expected), actualToString(actual));

            // NOTE: We remove the StackTrace, because it is not useful to have a Java Stack Trace pointing into the XML XQuery Test Suite code
            failureReason.setStackTrace(new StackTraceElement[0]);

            notifier.fireTestFailure(new Failure(description, failureReason));
        } catch (final Throwable t) {
            //signal internal failure
            notifier.fireTestFailure(new Failure(description, t));
        }

        return Sequence.EMPTY_SEQUENCE;
    }

    private String expectedToString(final MapType expected) {
        final Sequence seqExpectedValue = expected.get(VALUE_MAP_KEY);
        if (!seqExpectedValue.isEmpty()) {
            return safeSequenceToXmlString(context, seqExpectedValue);
        }

        final Sequence seqExpectedXPath = expected.get(XPATH_MAP_KEY);
        if (!seqExpectedXPath.isEmpty()) {
            return "XPath: " + safeSequenceToXmlString(context, seqExpectedXPath);
        }

        final Sequence seqExpectedError = expected.get(ERROR_MAP_KEY);
        if (!seqExpectedError.isEmpty()) {
            return "Error: " + safeSequenceToXmlString(context, seqExpectedError);
        }

        throw new IllegalStateException("Could not extract expected value");
    }

    private String actualToString(final MapType actual) {
        final Sequence seqActualError = actual.get(ERROR_MAP_KEY);
        if (!seqActualError.isEmpty()) {
            return safeSequenceToAdaptiveString(context, seqActualError);
        }

        final Sequence seqActualResult = actual.get(RESULT_MAP_KEY);
        if (!seqActualResult.isEmpty()) {
            return safeSequenceToXmlString(context, seqActualResult);
        } else {
            return "";  // empty-sequence()
        }
    }
}
