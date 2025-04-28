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
package org.exist.xquery.functions.util;

import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.inspect.InspectFunctionHelper;
import org.exist.xquery.value.FunctionReference;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;

import static org.exist.xquery.FunctionDSL.param;
import static org.exist.xquery.FunctionDSL.returns;
import static org.exist.xquery.functions.util.UtilModule.functionSignature;

public class InspectFunction extends BasicFunction {

    public static final String FN_INSPECT_FUNCTION_NAME = "inspect-function";
    public static final FunctionSignature FN_INSPECT_FUNCTION = functionSignature(
            FN_INSPECT_FUNCTION_NAME,
            "Returns an XML fragment describing the function referenced by the passed function item.",
            returns(Type.NODE, "the signature of the function"),
            param("function", Type.FUNCTION, "The function item to inspect")
    );

    public InspectFunction(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final FunctionReference ref = (FunctionReference) args[0].itemAt(0);
        final FunctionSignature sig = ref.getSignature();
        try {
            context.pushDocumentContext();
            final MemTreeBuilder builder = context.getDocumentBuilder();
            final int nodeNr = InspectFunctionHelper.generateDocs(sig, null, builder);
            return builder.getDocument().getNode(nodeNr);
        } finally {
            context.popDocumentContext();
        }
    }
}
