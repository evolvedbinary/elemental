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
package org.exist.xquery.functions.response;

import org.exist.http.servlets.ResponseWrapper;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Abstract for functions in the {@link ResponseModule}
 * which need access to the http response, and
 * should raise an {@link ErrorCodes#XPDY0002} if
 * the request is not available.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class StrictResponseFunction extends ResponseFunction {

    public StrictResponseFunction(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public final Sequence eval(final Sequence[] args, final Optional<ResponseWrapper> request)
            throws XPathException {
        return eval(
                args,
                request.orElseThrow(() -> new XPathException(this, ErrorCodes.XPDY0002, "No response object found in the current XQuery context."))
        );
    }

    /**
     * Evaluate the function with the Http Response.
     *
     * @param args the arguments to the function.
     * @param response the http response
     *
     * @return the result of the function.
     *
     * @throws XPathException an XPath Exception
     */
    protected abstract Sequence eval(final Sequence[] args, @Nonnull final ResponseWrapper response) throws XPathException;
}
