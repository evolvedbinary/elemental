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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.util.List;

/**
 * A simple hierarchy of URI Resolvers.
 *
 * The first resolver that matches returns the result.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class URIResolverHierarchy implements URIResolver {

    private final URIResolver[] uriResolvers;

    /**
     * @param uriResolvers the URI resolvers in order of precedence, most significant first.
     */
    public URIResolverHierarchy(final URIResolver... uriResolvers) {
        this.uriResolvers = uriResolvers;
    }

    /**
     * @param uriResolvers the URI resolvers in order of precedence, most significant first.
     */
    public URIResolverHierarchy(final List<URIResolver> uriResolvers) {
        this.uriResolvers = uriResolvers.toArray(new URIResolver[0]);
    }


    @Override
    public Source resolve(final String href, final String base) throws TransformerException {
        if (uriResolvers == null) {
            return null;
        }

        TransformerException firstTransformerException = null;

        for (final URIResolver uriResolver : uriResolvers) {
            try {
                final Source source = uriResolver.resolve(href, base);
                if (source != null) {
                    return source;
                }
            } catch (final TransformerException e) {
                if (firstTransformerException == null) {
                    firstTransformerException = e;
                }
            }
        }

        if (firstTransformerException != null) {
            throw firstTransformerException;
        } else {
            return null;
        }
    }
}
