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

import org.exist.dom.QName;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

public class FnInvisibleXml extends BasicFunction {

    private static final String FS_INVISIBLE_XML_NAME = "invisible-xml";

    /**
     * Note for the arguments one of them takses 2 types and I need to create a super type use transform.java as example
     */

    public final static FunctionSignature FNS_INVISIBLE_XML = new FunctionSignature(
            new QName(FS_INVISIBLE_XML_NAME, FnModule.NAMESPACE_URI),
            "Evaluates invisible XML.",
            null,
            new FunctionReturnSequenceType(Type.STRING, Cardinality.EXACTLY_ONE, "The evaluated string.")
    );

    public FnInvisibleXml(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        return new StringValue("TODO TODO TODO, maybe next");
    }
}