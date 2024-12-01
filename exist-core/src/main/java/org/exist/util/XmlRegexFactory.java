/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.util;

import com.evolvedbinary.j8fu.Either;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.sf.saxon.regex.RegularExpression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.Expression;
import org.exist.xquery.XPathException;

import javax.annotation.Nullable;
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
 * @author <a href="mailto:adan@evolvedbinary.com">Adam Retter</a>
 */
public class XmlRegexFactory {

    private static final Logger LOG = LogManager.getLogger(XmlRegexFactory.class);
    private static final XmlRegexFactory instance = new XmlRegexFactory();
    private static final ThreadLocal<List<String>> REGULAR_EXPRESSION_COMPILATION_WARNINGS = ThreadLocal.withInitial(() -> new ArrayList<>(1));

    private final Cache<String, Either<XPathException, RegularExpression>> cache;

    private XmlRegexFactory() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .build();
    }

    public static XmlRegexFactory getInstance() {
        return instance;
    }

    public RegularExpression getXmlRegex(final Expression regexExpr, final String pattern, @Nullable final String flags) throws XPathException {
        final String key = pattern + flags;
        final Either<XPathException, RegularExpression> maybeRegex = cache.get(key, _key -> compile(regexExpr, pattern, flags));
        return Either.valueOrThrow(maybeRegex);
    }

    private static Either<XPathException, RegularExpression> compile(final Expression regexExpr, final String pattern, @Nullable final String flags) {
        try {
            final List<String> warnings = REGULAR_EXPRESSION_COMPILATION_WARNINGS.get();
            if (!warnings.isEmpty()) {
                warnings.clear();
            }

            final RegularExpression regularExpression = regexExpr.getContext().getBroker().getBrokerPool()
                .getSaxonConfiguration()
                .compileRegularExpression(pattern, flags != null ? flags : "", "XP30", warnings);

            // log any warnings
            for (final String warning : warnings) {
                LOG.warn(warning);
            }

            return Either.Right(regularExpression);

        } catch (final net.sf.saxon.trans.XPathException e) {
            return Either.Left(translateRegexException(regexExpr, e));
        }
    }

    /**
     * Translate a Saxon XPathException for a Regular Expression into an eXist-db XPathException.
     *
     * @param regexExpr the expression that originated the Regular Expression.
     * @param xpathException the Saxon XPathException
     *
     * @return the eXist-db XPathException.
     */
    public static XPathException translateRegexException(final Expression regexExpr, final net.sf.saxon.trans.XPathException xpathException) {
        switch (xpathException.getErrorCodeLocalPart()) {
            case "FORX0001":
                return new XPathException(regexExpr, ErrorCodes.FORX0001, xpathException.getMessage());
            case "FORX0002":
                return new XPathException(regexExpr, ErrorCodes.FORX0002, xpathException.getMessage());
            case "FORX0003":
                return new XPathException(regexExpr, ErrorCodes.FORX0003, xpathException.getMessage());
            case "FORX0004":
                return new XPathException(regexExpr, ErrorCodes.FORX0004, xpathException.getMessage());
            default:
                return new XPathException(regexExpr, xpathException.getMessage());
        }
    }
}
