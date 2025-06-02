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

import org.exist.dom.QName;
import org.exist.xquery.*;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.QNameValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;

import javax.annotation.Nullable;

import static org.exist.xquery.FunctionDSL.*;
import static org.exist.xquery.functions.system.SystemModule.functionSignature;

/**
 * Determine whether a function is available for invocation.
 * 
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class FunctionAvailable extends BasicFunction {

    private static String FS_FUNCTIONAL_AVAILABLE_NAME = "function-available";
    public final static FunctionSignature FS_FUNCTIONAL_AVAILABLE = functionSignature(
        FS_FUNCTIONAL_AVAILABLE_NAME,
        "Returns true if a function is available for invocation, false otherwise.",
        returns(Type.BOOLEAN, "true if the function is available for invocation, false otherwise."),
        param("function-name", Type.QNAME, "The fully qualified name of the function"),
        param("arity", Type.INTEGER, "The arity of the function")
    );

    public FunctionAvailable(final XQueryContext context) {
        super(context, FS_FUNCTIONAL_AVAILABLE);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        final QName name = ((QNameValue) args[0].itemAt(0)).getQName();
        final int arity = ((IntegerValue) args[1].itemAt(0)).getInt();

        final boolean isFunctionAvailable;
        if (name.getNamespaceURI().startsWith("java:")) {
            isFunctionAvailable = isJavaBindingAvailable(name, arity);
        } else {
            isFunctionAvailable = isXQueryFunctionAvailable(name, arity);
        }

        return BooleanValue.valueOf(isFunctionAvailable);

    }

    private boolean isJavaBindingAvailable(final QName name, final int arity) {
        final boolean enableJavaBinding = context.getBroker().getConfiguration().getProperty(FunctionFactory.PROPERTY_ENABLE_JAVA_BINDING, false);
        if (!enableJavaBinding) {
            return false;
        }

        return JavaBinding.isReflectiveCallAvailable(name, arity);
    }

    private boolean isXQueryFunctionAvailable(final QName name, final int arity) throws XPathException {
        @Nullable final org.exist.xquery.Module[] modules = context.getModules(name.getNamespaceURI());
        if (modules == null || modules.length == 0) {
            return context.resolveFunction(name, arity) != null;
        }

        for (final org.exist.xquery.Module module : modules) {
            if (module instanceof InternalModule
                    && ((InternalModule) module).getFunctionDef(name, arity) != null) {
                return true;

            } else if (module instanceof ExternalModule
                    && ((ExternalModule)module).getFunction(name, arity, context) != null) {
                return true;
            }
        }

        return false;
    }
}