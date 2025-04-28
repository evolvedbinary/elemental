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
package org.exist.xslt;

import org.exist.repo.PkgXsltModuleURIResolver;
import org.exist.storage.BrokerPool;
import org.exist.util.EXistURISchemeURIResolver;
import org.exist.util.URIResolverHierarchy;

import javax.annotation.Nullable;
import javax.xml.transform.URIResolver;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for XSLT URI Resolution.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class XsltURIResolverHelper {

    /**
     * Get a URI Resolver for XSLT Modules.
     *
     * @param brokerPool the database
     * @param defaultResolver the default fallback resolver, or null
     * @param base the URI base, or null
     * @param avoidSelf true to avoid nesting {@link URIResolverHierarchy}
     *
     * @return the URIResolver, or null if there is no resolver
     */
    public static @Nullable URIResolver getXsltURIResolver(final BrokerPool brokerPool,
            @Nullable final URIResolver defaultResolver, @Nullable final String base, final boolean avoidSelf) {
        final List<URIResolver> resolvers = new ArrayList<>();

        if (base != null) {
            // database resolver
            resolvers.add(new EXistURISchemeURIResolver(new EXistURIResolver(brokerPool, base, false)));
        }

        // EXpath Pkg resolver
        brokerPool.getExpathRepo().map(repo -> resolvers.add(new PkgXsltModuleURIResolver(repo)));

        // default resolver
        if (defaultResolver != null) {
            if (avoidSelf) {
                if (!defaultResolver.getClass().getName().equals(URIResolverHierarchy.class.getName())) {
                    resolvers.add(defaultResolver);
                }
            } else {
                resolvers.add(defaultResolver);
            }
        }

        if (resolvers.size() > 0) {
            return new URIResolverHierarchy(resolvers);
        } else {
            return null;
        }
    }
}
