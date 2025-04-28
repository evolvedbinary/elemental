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
package org.exist.util;

import org.exist.xslt.EXistURIResolver;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

/**
 * URI Resolver, that first rewrites URLs like:
 *  exist://localhost/db -&gt; /db.
 *  exist://localhost:1234/db -&gt; xmldb:exist://localhost:1234/db
 *  exist://some-other-host/db -&gt; xmldb:exist://some-other-host/db
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class EXistURISchemeURIResolver implements URIResolver {
    private final EXistURIResolver eXistURIResolver;

    public EXistURISchemeURIResolver(final EXistURIResolver eXistURIResolver) {
        this.eXistURIResolver = eXistURIResolver;
    }

    @Override
    public Source resolve(final String href, final String base) throws TransformerException {
        return eXistURIResolver.resolve(
                rewriteScheme(href),
                rewriteScheme(base)
        );
    }

    private String rewriteScheme(String uri) {
        if (uri != null) {
            if (uri.startsWith("exist://localhost")) {
                uri = uri.replace("exist://localhost/db", "/db");
            } else if (uri.startsWith("exist://")) {
                uri = uri.replace("exist://", "xmldb:exist://");
            }
        }

        return uri;
    }
}
