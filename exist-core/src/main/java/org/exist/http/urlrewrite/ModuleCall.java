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
package org.exist.http.urlrewrite;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.dom.QName;
import org.exist.xquery.Module;
import org.exist.xquery.*;
import org.exist.xquery.value.Sequence;
import org.w3c.dom.Element;

import javax.annotation.Nullable;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;

public class ModuleCall extends URLRewrite {
    private static final Logger LOG = LogManager.getLogger(ModuleCall.class);

    private final FunctionCall call;

    public ModuleCall(final Element config, final XQueryContext context, final String uri) throws ServletException {
        super(config, uri);
        String funcName = config.getAttribute("function");
        if (funcName.isEmpty()) {
            throw new ServletException("<exist:call> requires an attribute 'function'.");
        }
        int arity = 0;
        final int p = funcName.indexOf('/');
        if (p > -1) {
            final String arityStr = funcName.substring(p + 1);
            try {
                arity = Integer.parseInt(arityStr);
            } catch (final NumberFormatException e) {
                throw new ServletException("<exist:call>: could not parse parameter count in function attribute: " + arityStr);
            }
            funcName = funcName.substring(0, p);
        }
        try {
            final QName fqn = QName.parse(context, funcName);
            @Nullable final Module[] modules = context.getModules(fqn.getNamespaceURI());
            UserDefinedFunction func = null;
            if (modules == null || modules.length == 0) {
                func = context.resolveFunction(fqn, arity);
            } else {
                for (final Module module : modules) {
                    func = ((ExternalModule) module).getFunction(fqn, arity, context);
                    if (func != null) {
                        break;
                    }
                }
            }

            if (func == null) {
                throw new ServletException("<exist:call> could not resolve function: " + fqn + "#" + arity +".");
            }

            call = new FunctionCall(context, func);
            call.setArguments(new ArrayList<>());
        } catch (final XPathException | QName.IllegalQNameException e) {
            throw new ServletException(e);
        }
    }

    protected ModuleCall(final ModuleCall other) {
        super(other);
        this.call = other.call;
    }

    @Override
    public void doRewrite(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
        try {
            final ContextItemDeclaration cid = call.getContext().getContextItemDeclartion();
            final Sequence contextSequence;
            if (cid != null) {
                contextSequence = cid.eval(null, null);
            } else {
                contextSequence = null;
            }

            final Sequence result = call.eval(contextSequence, null);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found: {}", result.getItemCount());
            }
            request.setAttribute(XQueryURLRewrite.RQ_ATTR_RESULT, result);

        } catch (final XPathException e) {
            throw new ServletException("Called function threw exception: " + e.getMessage(), e);
        }
    }

    @Override
    protected URLRewrite copy() {
        return new ModuleCall(this);
    }
}
