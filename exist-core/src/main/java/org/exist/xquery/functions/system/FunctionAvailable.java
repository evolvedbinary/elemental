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
package org.exist.xquery.functions.system;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.dom.QName;
import org.exist.xquery.*;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.QNameValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;

import javax.annotation.Nullable;

/**
 * Return whether the function is available
 * 
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class FunctionAvailable extends BasicFunction {

    protected final static Logger logger = LogManager.getLogger(FunctionAvailable.class);
    public final static FunctionSignature signature =
            new FunctionSignature(
            new QName("function-available", SystemModule.NAMESPACE_URI, SystemModule.PREFIX),
            "Returns whether a function is available.",
            new SequenceType[]{
                new FunctionParameterSequenceType("function-name", Type.QNAME, Cardinality.EXACTLY_ONE, "The fully qualified name of the function"),
                new FunctionParameterSequenceType("arity", Type.INTEGER, Cardinality.EXACTLY_ONE, "The arity of the function")
            },
            new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE, "true() if the function exists, false() otherwise."));

    public FunctionAvailable(XQueryContext context) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final QName functionName = ((QNameValue)args[0].itemAt(0)).getQName();
        final int arity = ((IntegerValue)args[1].itemAt(0)).getInt();
        
        @Nullable final org.exist.xquery.Module[] modules = context.getModules(functionName.getNamespaceURI());
        boolean found = false;
        if (modules == null || modules.length == 0) {
            found = context.resolveFunction(functionName, arity) != null;
        } else {
            for (final org.exist.xquery.Module module : modules) {
                if (module instanceof InternalModule) {
                    found = ((InternalModule)module).getFunctionDef(functionName, arity) != null;
                } else if (module instanceof ExternalModule) {
                    found = ((ExternalModule)module).getFunction(functionName, arity, context) != null;
                }

                if (found) {
                    break;
                }
            }
        }
        
        return BooleanValue.valueOf(found);
    }    
}