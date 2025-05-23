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
package org.exist.xquery.functions.fn.transform;

import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.AnyURIValue;
import org.junit.jupiter.api.Test;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import java.math.BigDecimal;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class FunTransformTest {

    @Test
    void versionNumbers() throws Transform.PendingException {

        Options.XSLTVersion version1 = new Options.XSLTVersion(1, 0);
        Options.XSLTVersion version2 = new Options.XSLTVersion(2, 0);
        Options.XSLTVersion version3 = new Options.XSLTVersion(3, 0);
        Options.XSLTVersion version31 = new Options.XSLTVersion(3, 1);
        assertNotEquals(version1, version2);
        assertNotEquals(version1, version3);
        assertNotEquals(version2, version3);
        assertNotEquals(version3, version31);
        assertEquals(version3, Options.XSLTVersion.fromDecimal(new BigDecimal("3.0")));
        assertNotEquals(version3, Options.XSLTVersion.fromDecimal(new BigDecimal("3.1")));
        assertEquals(version31, Options.XSLTVersion.fromDecimal(new BigDecimal("3.1")));
        assertEquals(Options.XSLTVersion.fromDecimal(new BigDecimal("3.1")), Options.XSLTVersion.fromDecimal(new BigDecimal("3.1")));
    }

    @Test
    void badVersionNumber() throws Transform.PendingException {

        assertThrows(Transform.PendingException.class, () -> {
            Options.XSLTVersion version311 = Options.XSLTVersion.fromDecimal(new BigDecimal("3.11"));
        });
    }

    @Test
    public void emptyResolution() throws XPathException, URISyntaxException {
        var base = new AnyURIValue("");
        var relative = new AnyURIValue("path/to/functions1.xsl");
        assertEquals(new AnyURIValue("path/to/functions1.xsl"), URIResolution.resolveURI(relative, base));
    }

    @Test
    public void resolution() throws XPathException, URISyntaxException {
        var base = new AnyURIValue("xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl");
        var relative = new AnyURIValue("functions1.xsl");
        assertEquals(new AnyURIValue("xmldb:exist:/db/apps/fn_transform/functions1.xsl"),
            URIResolution.resolveURI(relative, base));

        var base1_5 = new AnyURIValue("xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl");
        var relative1_5 = new AnyURIValue("/functions1.xsl");
        assertEquals(new AnyURIValue("xmldb:exist:/functions1.xsl"),
            URIResolution.resolveURI(relative1_5, base1_5));

        var base1_10 = new AnyURIValue("xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl");
        var relative1_10 = new AnyURIValue("/fn_transform/functions1.xsl");
        assertEquals(new AnyURIValue("xmldb:exist:/fn_transform/functions1.xsl"),
            URIResolution.resolveURI(relative1_10, base1_10));

        var base2 = new AnyURIValue("xmldb:exist:/db/apps/fn_transform/tei-toc2.xsl");
        assertEquals(new AnyURIValue("xmldb:exist:/db/apps/fn_transform/functions1.xsl"),
            URIResolution.resolveURI(relative, base2));

        var base3 = new AnyURIValue("https://127.0.0.1:8088/db/apps/fn_transform/tei-toc2.xsl");
        var relative3 = new AnyURIValue("functions1.xsl");
        assertEquals(new AnyURIValue("https://127.0.0.1:8088/db/apps/fn_transform/functions1.xsl"),
            URIResolution.resolveURI(relative3, base3));

        var base3_5 = new AnyURIValue("https://127.0.0.1:8088/db/apps/fn_transform/");
        var relative3_5 = new AnyURIValue("functions1.xsl");
        assertEquals(new AnyURIValue("https://127.0.0.1:8088/db/apps/fn_transform/functions1.xsl"),
            URIResolution.resolveURI(relative3_5, base3_5));

        var base3_10 = new AnyURIValue("https://127.0.0.1:8088/db/apps/fn_transform/");
        var relative3_10 = new AnyURIValue("/functions1.xsl");
        assertEquals(new AnyURIValue("https://127.0.0.1:8088/functions1.xsl"),
            URIResolution.resolveURI(relative3_10, base3_10));

        var base4 = new AnyURIValue("xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl");
        var relative4 = new AnyURIValue("xmldb:exist:///a/b/c/functions1.xsl");
        assertEquals(new AnyURIValue("xmldb:exist:///a/b/c/functions1.xsl"),
            URIResolution.resolveURI(relative4, base4));

        var base5 = new AnyURIValue("xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl");
        var relative5 = new AnyURIValue("https://127.0.0.1:8088/a/b/c/functions1.xsl");
        assertEquals(new AnyURIValue("https://127.0.0.1:8088/a/b/c/functions1.xsl"),
            URIResolution.resolveURI(relative5, base5));
    }

    /**
     * Create some UT coverage of the CompileTimeURIResolver
     * This is more significantly exercised by XQTS tests
     * Results are the same as above, wrapped in a {@code Source}
     *
     * @throws TransformerException
     */
    @Test
    public void resolverObject() throws TransformerException {
        var resolver = new URIResolution.CompileTimeURIResolver(new XQueryContext(), null) {
            @Override protected SourceWithLocation resolveDocument(final String location) {
                return new SourceWithLocation("RESOLVED::" + location);
            }
        };

        assertEquals("RESOLVED::xmldb:exist:/db/apps/fn_transform/functions1.xsl",
            ((SourceWithLocation)resolver.resolve("functions1.xsl", "xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl")).location);
        assertEquals("RESOLVED::xmldb:exist:/functions1.xsl",
            ((SourceWithLocation)resolver.resolve("/functions1.xsl", "xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl")).location);

        assertEquals("RESOLVED::xmldb:exist:/fn_transform/functions1.xsl",
            ((SourceWithLocation)resolver.resolve("/fn_transform/functions1.xsl", "xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl")).location);

        assertEquals("RESOLVED::xmldb:exist:/fn_transform/functions1.xsl",
            ((SourceWithLocation)resolver.resolve("/fn_transform/functions1.xsl", "xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl")).location);

        assertEquals("RESOLVED::https://127.0.0.1:8088/db/apps/fn_transform/functions1.xsl",
            ((SourceWithLocation)resolver.resolve("functions1.xsl", "https://127.0.0.1:8088/db/apps/fn_transform/tei-toc2.xsl")).location);

        assertEquals("RESOLVED::https://127.0.0.1:8088/db/apps/fn_transform/functions1.xsl",
            ((SourceWithLocation)resolver.resolve("functions1.xsl", "https://127.0.0.1:8088/db/apps/fn_transform/")).location);

        assertEquals("RESOLVED::https://127.0.0.1:8088/functions1.xsl",
            ((SourceWithLocation)resolver.resolve("/functions1.xsl", "https://127.0.0.1:8088/db/apps/fn_transform/")).location);

        assertEquals("RESOLVED::xmldb:exist:///a/b/c/functions1.xsl",
            ((SourceWithLocation)resolver.resolve("xmldb:exist:///a/b/c/functions1.xsl", "xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl")).location);

        assertEquals("RESOLVED::https://127.0.0.1:8088/a/b/c/functions1.xsl",
            ((SourceWithLocation)resolver.resolve("https://127.0.0.1:8088/a/b/c/functions1.xsl", "xmldb:exist:///db/apps/fn_transform/tei-toc2.xsl")).location);
    }

    /**
     * Skeleton implementation for test
     */
    private static class SourceWithLocation implements Source {

        final String location;
        SourceWithLocation(final String location) {
            this.location = location;
        }

        /**
         * implementation solely to conform to interface for test use
         *
         * @param systemId The system identifier as a URL string.
         */
        @Override
        public void setSystemId(String systemId) {
        }

        /**
         * implementation solely to conform to interface for test use
         *
         * @return always returns null; not expected to be called
         */
        @Override
        public String getSystemId() {
            return null;
        }
    }
}
