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

import static org.exist.xquery.functions.fn.FnModule.functionSignature;
import static org.exist.xquery.FunctionDSL.param;
import static org.exist.xquery.FunctionDSL.returns;

import org.exist.dom.QName;
import org.exist.xquery.*;
import org.exist.xquery.value.*;

public class FnInvisibleXml extends BasicFunction {

    private static final String FS_INVISIBLE_XML_NAME = "invisible-xml";

    public final static FunctionSignature FS_INVISIBLE_XML = new FunctionSignature(
            new QName(FS_INVISIBLE_XML_NAME, FnModule.NAMESPACE_URI),
            "Evaluates invisible XML.",
            null,
            new FunctionReturnSequenceType(Type.FUNCTION, Cardinality.EXACTLY_ONE, "The parser function."));

    public FnInvisibleXml(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final IxmlParserFunction fn = new IxmlParserFunction(context); // QUESTIONS : where context??
        final FunctionCall call = new FunctionCall(context, fn);
        return new FunctionReference(call);
    }

    private static class IxmlParserFunction extends UserDefinedFunction {

        IxmlParserFunction(final XQueryContext context) {
            super(context, functionSignature(
                    "invisible-xml",
                    "Gets the next random number generator.",
                    returns(Type.STRING, "just a random string for now"),
                    param("Parser inpuit", Type.STRING, "param description")));
        }

        @Override
        public Sequence eval(final Sequence contextSequence, final Item contextItem) throws XPathException {
            String parserInput = getCurrentArguments()[0].itemAt(0).getStringValue();
            return new StringValue(parserInput);
        }

        @Override
        public void accept(final ExpressionVisitor visitor) {
            if (visited) {
                return;
            }
            visited = true;
        }
    }
}
