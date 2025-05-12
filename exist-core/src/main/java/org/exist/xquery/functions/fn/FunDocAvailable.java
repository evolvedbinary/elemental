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
 * See <a href="https://www.w3.org/TR/xpath-functions-31/#func-doc-available">14.6.2 fn:doc-available</a> in the
 * W3C XPath and XQuery Functions and Operators 3.1 specification.
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
