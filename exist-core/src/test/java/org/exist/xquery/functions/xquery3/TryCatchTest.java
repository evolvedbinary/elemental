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
package org.exist.xquery.functions.xquery3;

import com.googlecode.junittoolbox.ParallelRunner;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResourceSet;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import org.exist.xquery.ErrorCodes;
import org.exist.xquery.XPathException;
import org.junit.Test;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmlunit.matchers.CompareMatcher;

import static org.junit.Assert.*;

/**
 * @author wessels
 */
@RunWith(ParallelRunner.class)
public class TryCatchTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    @Test
    public void encapsulated_1() throws XMLDBException {
        final String query1 = "xquery version '3.0';"
                + "<a>{ try { 'b' + 7 } catch * { 'c' } }</a>";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query1)) {
            try (final Resource resource = results.getResource(0)) {
                final String r = (String) resource.getContent();

                assertEquals("<a>c</a>", r);
            }
        }
    }

       @Test
    public void encapsulated_2() throws XMLDBException {
        final String query1 = "xquery version '3.0';"
                + "for $i in (1,2,3,4) return <a>{ try { 'b' + $i } catch * { 'c' } }</a>";

       try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query1)) {
           assertEquals(4, results.getSize());

           try (final Resource resource = results.getResource(0)) {
               final String r = (String) resource.getContent();
               assertEquals("<a>c</a>", r);
           }
       }
    }

   @Test
    public void encapsulated_3() throws XMLDBException {
        final String query1 = "xquery version '3.0';"
                + "<foo>{ for $i in (1,2,3,4) return <a>{ try { 'b' + $i } catch * { 'c' } }</a> }</foo>";

       try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query1)) {
           assertEquals(1, results.getSize());

           try (final Resource resource = results.getResource(0)) {
               final String result = (String) resource.getContent();
               assertThat(result, CompareMatcher.isSimilarTo("<foo><a>c</a><a>c</a><a>c</a><a>c</a></foo>").ignoreWhitespace());
           }
       }
    }

    @Test
    public void xQuery3_1() throws XMLDBException {
        final String query1 = "xquery version '1.0';"
                + "try { a + 7 } catch * { 1 }";
        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query1)) {
            try (final Resource resource = results.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("1", r);
                fail("exception expected");
            }
        } catch (final Throwable t) {

            final Throwable cause = t.getCause();
            if (cause instanceof XPathException ex) {
                assertEquals("exerr:EXXQDY0003", ex.getErrorCode().getErrorQName().getStringValue());
            } else {
                throw t;
            }
        }
    }

    @Test
    public void simpleCatch() throws XMLDBException {
        final String query = "xquery version '3.0';"
                + "try { a + 7 } catch * { 1 }";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = results.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("1", r);
            }
        }
    }

    @Test
    public void catchWithCodeAndDescription() throws XMLDBException {
        final String query = "xquery version '3.0';"
                + "try { a + 7 } "
                + "catch * "
                + "{  $err:code, $err:description } ";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, results.getSize());

            try (final Resource resource = results.getResource(0)) {
                final String r1 = (String) resource.getContent();
                assertEquals(ErrorCodes.XPDY0002.getErrorQName().getStringValue(), r1);
            }

            try (final Resource resource = results.getResource(1)) {
                final String r2 = (String) resource.getContent();
                assertEquals(ErrorCodes.XPDY0002.getDescription() + " Undefined context sequence for 'child::{}a'", r2);
            }
        }
    }

    @Test
    public void catchWithError3Matches() throws XMLDBException {
        final String query = "xquery version '3.0';"
                + "try { a + 7 } "
                + "catch err:XPDY0001 { 1 }"
                + "catch err:XPDY0002 { 2 }"
                + "catch err:XPDY0003 { 3 }";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = results.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("2", r);
            }
        }
    }

    @Test(expected = XMLDBException.class)
    public void catchWithErrorNoMatches() throws XMLDBException {
        final String query = "xquery version '3.0';"
                + "try { a + 7 } "
                + "catch err:XPDY0001 { 1 }"
                + "catch err:XPDY0002 { a }"
                + "catch err:XPDY0003 { 3 }";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = results.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("2", r);
            }
        }
    }

    @Test
    public void catchWithMultipleMatches() throws XMLDBException {
        final String query1 = "xquery version '3.0';"
                + "try { a + 7 } "
                + "catch err:XPDY0001 | err:XPDY0003 { 13 }"
                + "catch err:XPDY0002 { 2 }"
                + "catch err:XPDY0004 | err:XPDY0005 { 45 }";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query1)) {
            try (final Resource resource = results.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("2", r);
            }
        }

        final String query2 = "xquery version '3.0';"
                + "try { a + 7 } "
                + "catch err:XPDY0001 | * { 13 }"
                + "catch err:XPDY0002 { 2 }"
                + "catch err:XPDY0004 | err:XPDY0005 { 45 }";

        try (final EXistResourceSet results2 = existEmbeddedServer.executeQuery(query2)) {
            try (final Resource resource = results2.getResource(0)) {
                final String r2 = (String) resource.getContent();
                assertEquals("13", r2);
            }
        }
    }


    @Test
    public void catchFnError() throws XMLDBException {
        final String query1 = "xquery version '3.0';"
                + "try {"
                + " fn:error( fn:QName('http://www.w3.org/2005/xqt-errors', 'err:FOER0000') ) "
                + "} catch * "
                + "{ $err:code }";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query1)) {
            assertEquals(1, results.getSize());
            try (final Resource resource = results.getResource(0)) {
                final String r1 = (String) resource.getContent();
                assertEquals("err:FOER0000", r1);
            }
        }


        final String query2 = "xquery version '3.0';"
                + "try {"
                + " fn:error( fn:QName('http://www.w3.org/2005/xqt-errors', 'err:FOER0000') ) "
                + "} catch * "
                + "{ $err:code }";

        try (final EXistResourceSet results2 = existEmbeddedServer.executeQuery(query2)) {
            assertEquals(1, results2.getSize());
            try (final Resource resource = results2.getResource(0)) {
                final String r2 = (String) resource.getContent();
                assertEquals("err:FOER0000", r2);
            }
        }

        
        final String query3 = "xquery version '3.0';"
                + "try {"
                + " fn:error(fn:QName('http://www.w3.org/2005/xqt-errors', 'err:FOER0000'), 'TEST') "
                + "} catch * "
                + "{ $err:code, $err:description }";

        try (final EXistResourceSet results3 = existEmbeddedServer.executeQuery(query3)) {
            assertEquals(2, results3.getSize());
            try (final Resource resource = results3.getResource(0)) {
                final String r31 = (String) resource.getContent();
                assertEquals("err:FOER0000", r31);
            }
            try (final Resource resource = results3.getResource(1)) {
                final String r32 = (String) resource.getContent();
                assertEquals("TEST", r32);
            }
        }

        final String query4 = "xquery version '3.0';"
                + "try {"
                + " fn:error(fn:QName('http://www.w3.org/2005/xqt-errors', 'err:FOER0000'), 'TEST') "
                + "} catch *  "
                + "{ $err:code, $err:description }";

        try (final EXistResourceSet results4 = existEmbeddedServer.executeQuery(query4)) {
            assertEquals(2, results4.getSize());
            try (final Resource resource = results4.getResource(0)) {
                final String r41 = (String) resource.getContent();
                assertEquals("err:FOER0000", r41);
            }
            try (final Resource resource = results4.getResource(1)) {
                final String r42 = (String) resource.getContent();
                assertEquals("TEST", r42);
            }
        }

        final String query5 = "xquery version '3.0';"
                + "try {"
                + " fn:error(fn:QName('http://www.w3.org/2005/xqt-errors', 'err:FOER0000'), 'TEST', <ab/>) "
                + "} catch *  "
                + "{ $err:code, $err:description, $err:value }";

        try (final EXistResourceSet results5 = existEmbeddedServer.executeQuery(query5)) {
            assertEquals(3, results5.getSize());
            try (final Resource resource = results5.getResource(0)) {
                final String r51 = (String) resource.getContent();
                assertEquals("err:FOER0000", r51);
            }
            try (final Resource resource = results5.getResource(1)) {
                final String r52 = (String) resource.getContent();
                assertEquals("TEST", r52);
            }
            try (final Resource resource = results5.getResource(2)) {
                final String r53 = (String) resource.getContent();
                assertEquals("<ab/>", r53);
            }
        }
    }

    @Test
    public void catchFullErrorCode() throws XMLDBException {
        final String query = "xquery version '3.0';"
                + "try { a + 7 } "
                + "catch *  "
                + "{  $err:code, $err:description, empty($err:value) } ";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query)) {
            assertEquals(3, results.getSize());

            try (final Resource resource = results.getResource(0)) {
                final String r1 = (String) resource.getContent();
                assertEquals(ErrorCodes.XPDY0002.getErrorQName().getStringValue(), r1);
            }

            try (final Resource resource = results.getResource(1)) {
                final String r2 = (String) resource.getContent();
                assertEquals(ErrorCodes.XPDY0002.getDescription() + " Undefined context sequence for 'child::{}a'", r2);
            }

            try (final Resource resource = results.getResource(2)) {
                final String r3 = (String) resource.getContent();
                assertEquals("true", r3);
            }
        }
    }

    @Test
    public void catchDefinedNamespace() throws XMLDBException {
        final String query1 = "xquery version '3.0';"
                + "declare namespace foo='http://foo.com'; "
                + "try { "
                + "     fn:error(fn:QName('http://foo.com', 'ERRORNAME'), 'ERRORTEXT') "
                + "} "
                + "catch foo:ERRORNAME  { 'good' } "
                + "catch *  { 'bad' } ";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query1)) {
            assertEquals(1, results.getSize());
            try (final Resource resource = results.getResource(0)) {
                final String r1 = (String) resource.getContent();
                assertEquals("good", r1);
            }
        }


        final String query2 = "xquery version '3.0';"
                + "declare namespace foo='http://foo.com'; "
                + "try { "
                + "     fn:error(fn:QName('http://foo.com', 'ERRORNAME'), 'ERRORTEXT') "
                + "} "
                + "catch foo:ERRORNAME { $err:code } "
                + "catch *  { 'bad' } ";

        try (final EXistResourceSet results2 = existEmbeddedServer.executeQuery(query2)) {
            assertEquals(1, results2.getSize());
            try (final Resource resource = results2.getResource(0)) {
                final String r2 = (String) resource.getContent();
                assertEquals("foo:ERRORNAME", r2);
            }
        }
    }

    @Test
    public void catchDefinedNamespace2() throws XMLDBException {
        final String query = "xquery version '3.0';"
                + "declare namespace foo='http://foo.com'; "
                + "try { "
                + "     fn:error(fn:QName('http://foo.com', 'ERRORNAME'), 'ERRORTEXT')"
                + "} "
                + "catch foo:ERRORNAME { 'good' } "
                + "catch * { 'wrong' } ";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query)) {
            assertEquals(1, results.getSize());

            try (final Resource resource = results.getResource(0)) {
                final String r1 = (String) resource.getContent();
                assertEquals("good", r1);
            }
        }
    }
}
