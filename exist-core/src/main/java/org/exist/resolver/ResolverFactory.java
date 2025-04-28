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
package org.exist.resolver;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.xml.sax.InputSource;
import org.xmlresolver.Resolver;
import org.xmlresolver.ResolverFeature;
import org.xmlresolver.XMLResolverConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;

/**
 * Factory for creating Resolvers.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface ResolverFactory {

    /**
     * Create a Resolver that is configured for specific catalogs.
     *
     * @param catalogs the list of catalogs, the first entry in the tuple is their URI (and/or location),
     *                 and the optional second argument is an InputSource for obtaining them directly.
     *
     * @return the resolver
     *
     * @throws URISyntaxException if one of the catalog URI is invalid
     */
    static Resolver newResolver(final List<Tuple2<String, Optional<InputSource>>> catalogs) throws URISyntaxException {
        final XMLResolverConfiguration resolverConfiguration = new XMLResolverConfiguration();
        resolverConfiguration.setFeature(ResolverFeature.RESOLVER_LOGGER_CLASS, "org.xmlresolver.logging.SystemLogger");
        resolverConfiguration.setFeature(ResolverFeature.CATALOG_LOADER_CLASS, "org.xmlresolver.loaders.ValidatingXmlLoader");
        resolverConfiguration.setFeature(ResolverFeature.CLASSPATH_CATALOGS, true);
        resolverConfiguration.setFeature(ResolverFeature.URI_FOR_SYSTEM, true);
        // See: https://xmlresolver.org/ch06.html#xml.catalog.alwaysResolve
        resolverConfiguration.setFeature(ResolverFeature.ALWAYS_RESOLVE, false);

        for (final Tuple2<String, Optional<InputSource>> catalog : catalogs) {
            String strCatalogUri = catalog._1;
            strCatalogUri = sanitizeCatalogUri(strCatalogUri);
            if (catalog._2.isPresent()) {
                resolverConfiguration.addCatalog(new URI(strCatalogUri), catalog._2.get());
            } else {
                resolverConfiguration.addCatalog(strCatalogUri);
            }
        }

        return new Resolver(resolverConfiguration);
    }

    /**
     * Sanitize the Catalog URI.
     *
     * Mainly deals with converting Windows file paths to URI.
     *
     * @param strCatalogUri The Catalog URI string
     *
     * @return The sanitized Catalog URI string
     */
    static String sanitizeCatalogUri(String strCatalogUri) {
        if (strCatalogUri.indexOf('\\') > -1) {
            // convert from Windows file path
            strCatalogUri = Paths.get(strCatalogUri).toUri().toString();
        }
        return strCatalogUri;
    }

    /**
     * Catalog URI if stored in database must start with
     * URI Scheme xmldb:// (and NOT xmldb:exist://) so that
     * the {@link Resolver} can use {@link org.exist.protocolhandler.protocols.xmldb.Handler}
     * to resolve any relative URI resources from the database.
     *
     * @param catalogs the catalog URIs
     *
     * @return the catalog URIs suitable for use with the {@link Resolver}.
     */
    static List<Tuple2<String, Optional<InputSource>>> fixupExistCatalogUris(final List<Tuple2<String, Optional<InputSource>>> catalogs) {
        return catalogs.stream().map(catalog -> Tuple(fixupExistCatalogUri(catalog._1), catalog._2)).collect(Collectors.toList());
    }

    /**
     * Catalog URI if stored in database must start with
     * URI Scheme xmldb:// (and NOT xmldb:exist://) so that
     * the {@link Resolver} can use {@link org.exist.protocolhandler.protocols.xmldb.Handler}
     * to resolve any relative URI resources from the database.
     *
     * @param catalogUri the catalog URI
     *
     * @return the catalog URI suitable for use with the {@link Resolver}.
     */
    static String fixupExistCatalogUri(String catalogUri) {
        if (catalogUri.startsWith("xmldb:exist://")) {
            catalogUri = catalogUri.replace("xmldb:exist://", "xmldb://");
        } else if (catalogUri.startsWith("/db")) {
            catalogUri = "xmldb://" + catalogUri;
        }
        return catalogUri;
    }

}
