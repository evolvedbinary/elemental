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

import java.nio.file.Path;
import java.util.Optional;

import static org.exist.xquery.FunctionDSL.deprecated;
import static org.exist.xquery.FunctionDSL.returns;
import static org.exist.xquery.functions.system.SystemModule.functionSignature;

/**
 * Get the path to Elemental Home.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class GetElementalHome extends BasicFunction {

    private static final String FS_GET_ELEMENTAL_HOME_NAME = "get-elemental-home";
    static final FunctionSignature FS_GET_ELEMENTAL_HOME = functionSignature(
            FS_GET_ELEMENTAL_HOME_NAME,
            "Returns the path from ELEMENTAL_HOME; the path of where Elemental is installed and running from",
            returns(Type.STRING, "The path from ELEMENTAL_HOME")
    );

    @Deprecated
    private static final String FS_GET_EXIST_HOME_NAME = "get-exist-home";
    @Deprecated
    static final FunctionSignature FS_GET_EXIST_HOME = deprecated(
            "Use system:get-elemental-home() instead",
            functionSignature(
                FS_GET_EXIST_HOME_NAME,
                "Returns the path from EXIST_HOME; the path of where Elemental is installed and running from",
                returns(Type.STRING, "The path from EXIST_HOME")
            )
    );

    public GetElementalHome(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final Optional<Path> maybeElementalHome = context.getBroker().getConfiguration().getElementalHome();
        if (maybeElementalHome.isEmpty()) {
            throw new XPathException(this, "ELEMENTAL_HOME has not been set");
        }

        final Path elementalHome = maybeElementalHome.get();
        return new StringValue(this, elementalHome.normalize().toAbsolutePath().toString());
    }
}
