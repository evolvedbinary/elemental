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
package org.exist.xquery.functions.system;

import org.exist.xquery.BasicFunction;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

import static org.exist.xquery.FunctionDSL.returns;
import static org.exist.xquery.functions.system.SystemModule.functionSignature;

public class GetModuleLoadPath extends BasicFunction {

    private static final String FS_GET_MODULE_LOAD_PATH_NAME = "get-module-load-path";
    public static final FunctionSignature FS_GET_MODULE_LOAD_PATH = functionSignature(
			FS_GET_MODULE_LOAD_PATH_NAME,
			"Returns the path from which the module was loaded. Either a filesystem directory, or database collection. If the path cannot be determined, for example because the query module is in-memory then '.' is returned.",
			returns(Type.STRING, "The path from which the module was loaded")
    );

    public GetModuleLoadPath(final XQueryContext context) {
        super(context, FS_GET_MODULE_LOAD_PATH_);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        return new StringValue(this, context.getModuleLoadPath());
    }
}
