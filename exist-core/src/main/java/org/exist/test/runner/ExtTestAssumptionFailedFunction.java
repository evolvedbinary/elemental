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
import org.junit.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import javax.annotation.Nullable;

import static org.exist.xquery.FunctionDSL.optParam;
import static org.exist.xquery.FunctionDSL.param;
import static org.exist.xquery.FunctionDSL.params;

public class ExtTestAssumptionFailedFunction extends JUnitIntegrationFunction {

    private static final StringValue NAME_MAP_KEY = new StringValue("name");

    public ExtTestAssumptionFailedFunction(final XQueryContext context, final String parentName, final RunNotifier notifier) {
        super("ext-test-assumption-failed-function",
                params(
                        param("name", Type.STRING, "name of the test"),
                        optParam("error", Type.MAP, "error detail of the test")
                ), context, parentName, notifier);
    }

    @Override
    public Sequence eval(final Sequence contextSequence, final Item contextItem) throws XPathException {
        final Sequence[] args = getCurrentArguments();
        if (args.length != 2) {
            throw new XPathException(this, "ext-test-assumption-failed-function requires 2 parameters");
        }

        final Sequence argName = args[0];
        if (argName.isEmpty()) {
            throw new XPathException(this, "ext-test-assumption-failed-function requires a 'name' parameter");
        }
        final String name = safeGetStringValue(argName.itemAt(0));

        final Sequence argAssumptionError = args[1];
        @Nullable final MapType assumptionError = argAssumptionError.isEmpty() ? null : (MapType) argAssumptionError.itemAt(0);

        final Description description = createTestDescription(name);

        // notify JUnit
        try {
            final AssumptionViolatedException assumptionFailureReason = assumptionMapAsAssumptionViolationException(assumptionError);

            // NOTE: We remove the StackTrace, because it is not useful to have a Java Stack Trace pointing into the XML XQuery Test Suite code
            assumptionFailureReason.setStackTrace(new StackTraceElement[0]);

            notifier.fireTestAssumptionFailed(new Failure(description, assumptionFailureReason));
        } catch (final Throwable t) {
            //signal internal failure
            notifier.fireTestFailure(new Failure(description, t));
        }

        return Sequence.EMPTY_SEQUENCE;
    }

    public AssumptionViolatedException assumptionMapAsAssumptionViolationException(final MapType assumptionMap) {
        final String name = safeGetMapStringValue(NAME_MAP_KEY, assumptionMap, "");
        final String value = safeGetMapStringValue(VALUE_MAP_KEY, assumptionMap, "");
        return new AssumptionViolatedException("Assumption %" + name + " does not hold for: " + value);
    }
}
