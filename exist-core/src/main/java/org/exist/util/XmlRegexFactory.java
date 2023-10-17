/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.sf.saxon.regex.RegularExpression;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.xquery.XQueryContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple Xml Regular Expression Pattern Factory.
 * Based on a similar cache for Java patterns,
 * @see PatternFactory
 *
 * Patterns are Cached in an LRU like Cache
 *
 * @author <a href="mailto:alan@evolvedbinary.com">Alan Paxton</a>
 */
public class XmlRegexFactory {

    protected final static Logger LOG = LogManager.getLogger(XmlRegexFactory.class);

    private static final XmlRegexFactory instance = new XmlRegexFactory();

    private final Cache<Pair<String, String>, RegularExpression> cache;

    private XmlRegexFactory() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .build();
    }

    public static XmlRegexFactory getInstance() {
        return instance;
    }

    public RegularExpression getXmlRegex(final XQueryContext context, final String pattern, final String flags) throws net.sf.saxon.trans.XPathException {

        final Pair<String, String> key = Pair.of(pattern, flags);
        RegularExpression regex = cache.getIfPresent(key);
        if (regex == null) {
                List<String> warnings = new ArrayList<>(1);
                regex = context.getBroker().getBrokerPool()
                    .getSaxonConfiguration()
                    .compileRegularExpression(pattern, flags, "XP30", warnings);

                for (final String warning : warnings) {
                    LOG.warn(warning);
                }
                cache.put(key, regex);
        }

        return regex;
    }
}
