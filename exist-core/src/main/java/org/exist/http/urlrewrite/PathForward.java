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

import org.exist.http.urlrewrite.XQueryURLRewrite.RequestWrapper;
import org.w3c.dom.Element;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

public class PathForward extends Forward {
    private final ServletConfig filterConfig;
    private String servletName;

    public PathForward(final ServletConfig filterConfig, final Element config, final String uri) throws ServletException {
        super(config, uri);
        this.filterConfig = filterConfig;
        final String url = config.getAttribute("url");
        servletName = config.getAttribute("servlet");
        if (servletName.isEmpty()) {
            servletName = null;
        }
        if (servletName == null) {
            if (url.isEmpty()) {
                throw new ServletException("<exist:forward> needs either an attribute 'url' or 'servlet'.");
            }
            setTarget(URLRewrite.normalizePath(url));
        }
    }

    protected PathForward(final PathForward other) {
        super(other);
        this.filterConfig = other.filterConfig;
        this.servletName = other.servletName;
    }

    @Override
    protected void setAbsolutePath(final RequestWrapper request) {
        request.setPaths(target, servletName);
    }


    @Override
    protected RequestDispatcher getRequestDispatcher(final HttpServletRequest request) {
        if (servletName != null) {
            return filterConfig.getServletContext().getNamedDispatcher(servletName);
        } else if (request != null) {
            return request.getRequestDispatcher(target);
        } else {
            return filterConfig.getServletContext().getRequestDispatcher(target);
        }
    }

    @Override
    protected URLRewrite copy() {
        return new PathForward(this);
    }
}
