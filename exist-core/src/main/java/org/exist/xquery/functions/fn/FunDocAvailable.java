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
package org.exist.xquery.functions.fn;

import org.exist.xquery.BasicFunction;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.util.DocUtils;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;

import java.net.URI;
import java.net.URISyntaxException;

import static org.exist.xquery.FunctionDSL.optParam;
import static org.exist.xquery.FunctionDSL.returns;
import static org.exist.xquery.functions.fn.FnModule.functionSignature;

/**
 * Implementation of the XPath fn:doc-available() function.
 * {@see https://www.w3.org/TR/xpath-functions-31/#func-doc-available}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class FunDocAvailable extends BasicFunction {

    public static final FunctionSignature FS_DOC_AVAILABLE = functionSignature(
        "doc-available",
        "The function returns true if and only if the function call fn:doc($uri) would return a document node.",
        returns(Type.BOOLEAN, "If a call on fn:doc($uri) would return a document node, this function returns true. In all other cases this function returns false."),
        optParam("uri", Type.STRING, "The URI to check for a document.")
    );

    public FunDocAvailable(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        if (args.length == 0) {
            return BooleanValue.FALSE;
        }

        final String uri = args[0].getStringValue();
        try {
            new URI(uri);
        } catch (final URISyntaxException e) {
            if (context.getXQueryVersion() == 31) {
                // XPath 3.1
                return BooleanValue.FALSE;
            } else {
                // XPath 2.0 and 3.0
                throw new XPathException(this, ErrorCodes.FODC0005, e.getMessage(), args[0], e);
            }
        }

        try {
            return BooleanValue.valueOf(DocUtils.isDocumentAvailable(this.context, uri, this));
        } catch (final XPathException e) {
            return BooleanValue.FALSE;
        }
    }
}
