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
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.integer.IntegerPicture;
import org.exist.xquery.value.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.exist.xquery.FunctionDSL.*;
import static org.exist.xquery.functions.fn.FnModule.functionSignatures;

/**
 * Implements fn:format-integer as per W3C XPath and XQuery Functions and Operators 3.1
 * <p>
 * fn:format-number($value as integer?, $picture as xs:string) as xs:string
 * fn:format-number($value as integer?, $picture as xs:string, $lang as xs:string) as xs:string
 *
 * @author <a href="mailto:alan@evolvedbinary.com">Alan Paxton</a>
 */
public class FnFormatIntegers extends BasicFunction {

    private static final FunctionParameterSequenceType FS_PARAM_VALUE = optParam("value", Type.INTEGER, "The number to format");
    private static final FunctionParameterSequenceType FS_PARAM_PICTURE = param("picture", Type.STRING, "The picture string to use for formatting. To understand the picture string syntax, see: https://www.w3.org/TR/xpath-functions-31/#func-format-number");

    private static final String FS_FORMAT_INTEGER_NAME = "format-integer";
    static final FunctionSignature[] FS_FORMAT_INTEGER = functionSignatures(
            FS_FORMAT_INTEGER_NAME,
            "Returns a string containing an integer formatted according to a given picture string.",
            returns(Type.STRING, "The formatted string representation of the supplied integer"),
            arities(
                    arity(
                            FS_PARAM_VALUE,
                            FS_PARAM_PICTURE
                    ),
                    arity(
                            FS_PARAM_VALUE,
                            FS_PARAM_PICTURE,
                            optParam("lang", Type.STRING, "The language in which to format the integers.")
                    )
            )
    );

    public FnFormatIntegers(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence)
            throws XPathException {
        // If $value is an empty sequence, the function returns a zero-length string
        // https://www.w3.org/TR/xpath-functions-31/#func-format-integer
        if (args[0].isEmpty()) {
            return Sequence.EMPTY_SEQUENCE;
        }

        // If the value of $value is negative, the rules below are applied to the absolute value of $value,
        // and a minus sign is prepended to the result.
        final IntegerValue integerValue = (IntegerValue) args[0].itemAt(0);
        final BigInteger bigInteger = integerValue.toJavaObject(BigInteger.class);

        final IntegerPicture picture = IntegerPicture.fromString(args[1].getStringValue());

        // Build a list of languages to try
        // the called picture will use the first one with a valid locale
        final List<String> languages = new ArrayList<>(2);
        if (args.length == 3 && !args[2].isEmpty()) {
            languages.add(args[2].getStringValue());
        }
        languages.add(context.getDefaultLanguage());

        return new StringValue(this, picture.formatInteger(bigInteger, languages));
    }
}
