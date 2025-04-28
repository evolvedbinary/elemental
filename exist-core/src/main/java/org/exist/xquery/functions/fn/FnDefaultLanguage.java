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

import org.exist.dom.QName;
import org.exist.xquery.*;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

public class FnDefaultLanguage extends BasicFunction {

    public static final FunctionSignature FS_DEFAULT_LANGUAGE = FunctionDSL.functionSignature(
            new QName("default-language", Function.BUILTIN_FUNCTION_NS),
            "Returns the xs:language that is " +
                    "the value of the default language property from the dynamic context " +
                    "during the evaluation of a query or transformation in which " +
                    "fn:default-language() is executed.",
            FunctionDSL.returns(Type.LANGUAGE, Cardinality.EXACTLY_ONE, "the default language within query execution time span"));

    public FnDefaultLanguage(final XQueryContext context) {
        super(context, FS_DEFAULT_LANGUAGE);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {

        return new StringValue(this, context.getDefaultLanguage());
    }

}
