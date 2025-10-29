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

import org.exist.xquery.ErrorCodes;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.*;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.exist.xquery.FunctionDSL.*;

public class ExtTestErrorFunction extends JUnitIntegrationFunction {

    private static final StringValue DESCRIPTION_MAP_KEY = new StringValue("description");
    private static final StringValue CODE_MAP_KEY = new StringValue("code");
    private static final StringValue LINE_NUMBER_MAP_KEY = new StringValue("line-number");
    private static final StringValue COLUMN_NUMBER_MAP_KEY = new StringValue("column-number");
    private static final StringValue JAVA_STACK_TRACE_MAP_KEY = new StringValue("java-stack-trace");

    public ExtTestErrorFunction(final XQueryContext context, final String parentName, final RunNotifier notifier) {
        super("ext-test-error-function",
                params(
                        param("name", Type.STRING, "name of the test"),
                        optParam("error", Type.MAP, "error detail of the test. e.g. map { \"code\": $err:code, \"description\": $err:description, \"value\": $err:value, \"module\": $err:module, \"line-number\": $err:line-number, \"column-number\": $err:column-number, \"additional\": $err:additional, \"xquery-stack-trace\": $exerr:xquery-stack-trace, \"java-stack-trace\": $exerr:java-stack-trace}")
                ), context, parentName, notifier);
    }

    @Override
    public Sequence eval(final Sequence contextSequence, final Item contextItem) throws XPathException {
        final Sequence[] args = getCurrentArguments();
        if (args.length != 2) {
            throw new XPathException(this, "ext-test-error-function requires 2 parameters");
        }

        final Sequence argName = args[0];
        if (argName.isEmpty()) {
            throw new XPathException(this, "ext-test-error-function requires a 'name' parameter");
        }
        final String name = safeGetStringValue(argName.itemAt(0));

        final Sequence argError = args[1];
        @Nullable final MapType error = argError.isEmpty() ? null : (MapType) argError.itemAt(0);

        final Description description = createTestDescription(name);

        // notify JUnit
        try {
            final Failure failure;
            if (error != null) {
                final XPathException errorReason = errorMapAsXPathException(error);
                failure = new Failure(description, errorReason);
            } else {
                failure = new Failure(description, new XPathException(this, "No error map provided to ext-test-error-function"));
            }
            notifier.fireTestFailure(failure);
        } catch (final Throwable t) {
            //signal internal failure
            notifier.fireTestFailure(new Failure(description, t));
        }

        return Sequence.EMPTY_SEQUENCE;
    }

    private XPathException errorMapAsXPathException(final MapType errorMap) {
        final Sequence seqDescription = errorMap.get(DESCRIPTION_MAP_KEY);
        final String description = safeGetMapStringValue(DESCRIPTION_MAP_KEY, errorMap, "");

        final Sequence seqErrorCode = errorMap.get(CODE_MAP_KEY);
        final ErrorCodes.ErrorCode errorCode;
        if(seqErrorCode != null && !seqErrorCode.isEmpty()) {
            errorCode = new ErrorCodes.ErrorCode(((QNameValue)seqErrorCode.itemAt(0)).getQName(), description);
        } else {
            errorCode = ErrorCodes.ERROR;
        }

        final Sequence seqLineNumber = errorMap.get(LINE_NUMBER_MAP_KEY);
        final int lineNumber = safeGetIntValue(seqLineNumber, 0);

        final Sequence seqColumnNumber = errorMap.get(COLUMN_NUMBER_MAP_KEY);
        final int columnNumber = safeGetIntValue(seqColumnNumber, 0);

        @Nullable StackTraceElement[] stackTraceElements = null;
        @Nullable final Sequence seqJavaStackTrace = errorMap.get(JAVA_STACK_TRACE_MAP_KEY);
        if (seqJavaStackTrace != null && !seqJavaStackTrace.isEmpty()) {
            stackTraceElements = convertStackTraceElements(seqJavaStackTrace);
        }

        final XPathException xpe = new XPathException(lineNumber, columnNumber, errorCode, description);
        if (stackTraceElements != null) {
            xpe.setStackTrace(stackTraceElements);
        }

        return xpe;
    }

    private static final Pattern PTN_CAUSED_BY = Pattern.compile("Caused by:\\s([a-zA-Z0-9_$\\.]+)(?::\\s(.+))?");
    private static final Pattern PTN_AT = Pattern.compile("at\\s((?:[a-zA-Z0-9_$]+)(?:\\.[a-zA-Z0-9_$]+)*)\\.((?:[a-zA-Z0-9_$-]+)|(?:<init>))\\(([a-zA-Z0-9_]+\\.java):([0-9]+)\\)");

    protected @Nullable StackTraceElement[] convertStackTraceElements(final Sequence seqJavaStackTrace) {
        StackTraceElement[] traceElements = null;

        final Matcher matcherAt = PTN_AT.matcher("");

        // index 0 is the first `Caused by: ...`
        int i = 1;
        for ( ; i < seqJavaStackTrace.getItemCount(); i++) {
            final String item = safeGetStringValue(seqJavaStackTrace.itemAt(i));
            @Nullable final StackTraceElement stackTraceElement = convertStackTraceElement(matcherAt, item);
            if (stackTraceElement == null) {
                break;
            }

            if (traceElements == null) {
                traceElements = new StackTraceElement[seqJavaStackTrace.getItemCount() - 1];
            }
            traceElements[i - 1] = stackTraceElement;
        }

        if (traceElements != null && i + 1 < seqJavaStackTrace.getItemCount()) {
            traceElements = Arrays.copyOf(traceElements, i - 2);
        }

        return traceElements;
    }

    private @Nullable StackTraceElement convertStackTraceElement(final Matcher matcherAt, final String s) {
        matcherAt.reset(s);
        if (matcherAt.matches()) {
            final String declaringClass = matcherAt.group(1);
            final String methodName = matcherAt.group(2);
            final String fileName = matcherAt.group(3);
            final String lineNumber = matcherAt.group(4);
            return new StackTraceElement(declaringClass, methodName, fileName, Integer.parseInt(lineNumber));
        } else {
            return null;
        }
    }
}
