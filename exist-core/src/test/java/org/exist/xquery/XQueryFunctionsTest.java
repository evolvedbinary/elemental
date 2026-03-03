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
package org.exist.xquery;

import com.googlecode.junittoolbox.ParallelRunner;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Tests for various standard XQuery functions
 *
 * @author jens
 * @author perig
 * @author wolf
 * @author adam
 * @author dannes
 * @author dmitriy
 * @author ljo
 * @author chrisdutz
 * @author harrah
 * @author gvalentino
 * @author jmvanel
 */
@RunWith(ParallelRunner.class)
public class XQueryFunctionsTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);
    
    private final static String ROOT_COLLECTION_URI = "xmldb:exist:///db";

    @Test
    public void arguments() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("declare function local:testAnyURI($uri as xs:string) as xs:string { " +
                "concat('Successfully processed as xs:string : ',$uri) " +
                "}; " +
                "let $a := xs:anyURI('http://exist.sourceforge.net/') " +
                "return local:testAnyURI($a)")) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("Successfully processed as xs:string : http://exist.sourceforge.net/", r);
            }
        }


        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("declare function local:testEmpty($blah as xs:string)  as element()* { " +
                "for $a in (1,2,3) order by $a " +
                "return () " +
                "}; " +
                "local:testEmpty('test')")) {
            assertEquals(0, result.getSize());
        }
    }

    /**
     * Tests the XQuery-/XPath-function fn:round-half-to-even
     * with the rounding value typed xs:integer
     */
    @Test
    public void roundHtE_INTEGER() throws XMLDBException {
        String query = "fn:round-half-to-even( xs:integer('1'), 0 )";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("1", r);
            }
        }

        query = "fn:round-half-to-even( xs:integer('6'), -1 )";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("10", r);
            }
        }

        query = "fn:round-half-to-even( xs:integer('5'), -1 )";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("0", r);
            }
        }
    }

    /**
     * Tests the XQuery-/XPath-function fn:round-half-to-even
     * with the rounding value typed xs:double
     */
    @Test
    public void roundHtE_DOUBLE() throws XMLDBException {
        /* List of Values to test with Rounding */
        String[] testvalues =
                {"0.5", "1.5", "2.5", "3.567812E+3", "4.7564E-3", "35612.25"};
        String[] resultvalues =
                {"0", "2", "2", "3567.81", "0", "35600"};
        int[] precision =
                {0, 0, 0, 2, 2, -2};

        for (int i = 0; i < testvalues.length; i++) {
            final String query = "fn:round-half-to-even( xs:double('" + testvalues[i] + "'), " + precision[i] + " )";
            try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
                try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                    final String r = (String) resource.getContent();
                    assertEquals(resultvalues[i], r);
                }
            }
        }
    }

    /**
     * Tests the XQuery-XPath function fn:tokenize()
     */
    @Test
    public void tokenize() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("count ( tokenize('a/b' , '/') )")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("2", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("count ( tokenize('a/b/' , '/') )")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("count ( tokenize('' , '/') )")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("0", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(
                "let $res := fn:tokenize('abracadabra', '(ab)|(a)')" +
                        "let $reference := ('', 'r', 'c', 'd', 'r', '')" +
                        "return fn:deep-equal($res, $reference)")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("true", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("tokenize('firstSecondThirdLast', '[A-Z]')")) {
            assertEquals(4, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("first", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                String r = (String) resource.getContent();
                assertEquals("econd", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                String r = (String) resource.getContent();
                assertEquals("hird", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(3)) {
                String r = (String) resource.getContent();
                assertEquals("ast", r);
            }
        }
    }

    @Test
    public void deepEqual() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(
                "let $res := ('a', 'b')" +
                        "let $reference := ('a', 'b')" +
                        "return fn:deep-equal($res, $reference)")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("true", r);
            }
        }
    }

    @Test
    public void compare() throws XPathException, XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("fn:compare(\"Strasse\", \"Stra\u00DFe\")")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("-1", r);
                //result 	= existEmbeddedServer.executeQuery("fn:compare(\"Strasse\", \"Stra\u00DFe\", \"java:GermanCollator\")");
                //r 		= (String) result.getResource(0).getContent();
                //assertEquals( "0", r );
            }
        }

        final String query = "let $a := <a><b>-1</b><b>-2</b></a> " +
            "return $a/b[compare(., '+') gt 0]";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void distinctValues() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("declare variable $c := distinct-values(('a', 'a')); $c")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("a", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("declare variable $c := distinct-values((<a>a</a>, <b>a</b>)); $c")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("a", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $seq := ('A', 2, 'B', 2) return distinct-values($seq) ")) {
            assertEquals(3, result.getSize());
        }

        final String query = "let $a := <a><b>-1</b><b>-2</b></a> " +
                "return $a/b[distinct-values(.)]";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void sum() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("declare variable $c := sum((1, 2)); $c")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("declare variable $c := sum((<a>1</a>, <b>2</b>)); $c")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                //Any untyped atomic values in the sequence are converted to xs:double values ([MK Xpath 2.0], p. 432)
                assertEquals("3", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("declare variable $c := sum((), 3); $c")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }
    }

    @Test
    public void avg() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("avg((2, 2))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("2", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("avg((<a>2</a>, <b>2</b>))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                //Any untyped atomic values in the resulting sequence
                //(typically, values extracted from nodes in a schemaless document)
                //are converted to xs:double values ([MK Xpath 2.0], p. 301)
                assertEquals("2", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("avg((3, 4, 5))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("4", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("avg((xdt:yearMonthDuration('P20Y'), xdt:yearMonthDuration('P10M')))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("P10Y5M", r);
            }
        }

        String message = "";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("avg((xdt:yearMonthDuration('P20Y') , (3, 4, 5)))")) {
            // needed to make sure result is closed
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.contains("FORG0006"));

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("avg(())")) {
            assertEquals(0, result.getSize());
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("avg(((xs:float('INF')), xs:float('-INF')))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("NaN", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("avg(((3, 4, 5), xs:float('NaN')))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("NaN", r);
            }
        }
    }

    @Test
    public void min() throws XPathException, XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("min((1, 2))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("1", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("min((<a>1</a>, <b>2</b>))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("1", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("min(())")) {
            assertEquals(0, result.getSize());
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("min((xs:dateTime('2005-12-19T16:22:40.006+01:00'), xs:dateTime('2005-12-19T16:29:40.321+01:00')))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("2005-12-19T16:22:40.006+01:00", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("min(('a', 'b'))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("a", r);
            }
        }

        String message = "";
        try {
            try (final EXistResourceSet result = existEmbeddedServer.executeQuery("min((xs:dateTime('2005-12-19T16:22:40.006+01:00'), 'a'))")) {
                // needed to make sure result is closed
            }
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.contains("FORG0006"));

        try {
            message = "";
            try (final EXistResourceSet result = existEmbeddedServer.executeQuery("min(1, 2)")) {
                // needed to make sure result is closed
            }
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        //depends on whether we have strict type checking or not
        assertTrue(message.contains("XPTY0004") | message.contains("FORG0001") | message.contains("FOCH0002"));
    }

    public void max() throws XPathException, XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("max((1, 2))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("2", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("max((<a>1</a>, <b>2</b>))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("2", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("max(())")) {
            assertEquals(0, result.getSize());
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("max((xs:dateTime('2005-12-19T16:22:40.006+01:00'), xs:dateTime('2005-12-19T16:29:40.321+01:00')))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("2005-12-19T16:29:40.321+01:00", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("max(('a', 'b'))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("b", r);
            }
        }

        String message = "";
        try {
            try (final EXistResourceSet result = existEmbeddedServer.executeQuery("max((xs:dateTime('2005-12-19T16:22:40.006+01:00'), 'a'))")) {
                // needed to make sure result is closed
            }
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.contains("FORG0006"));

        try {
            message = "";
            try (final EXistResourceSet result = existEmbeddedServer.executeQuery("max(1, 2)")) {
                // needed to make sure result is closed
            }
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        //depends on whether we have strict type checking or not
        assertTrue(message.contains("XPTY0004") | message.contains("FORG0001") | message.contains("FOCH0002"));
    }

    @Test
    public void exclusiveLock() throws XMLDBException {
        String query = "let $query1 := (<a/>)\n" +
                "let $query2 := (2, 3)\n" +
                "let $a := util:exclusive-lock(//*,($query1, $query2))\n" +
                "return $a";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(3, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("<a/>", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                String r = (String) resource.getContent();
                assertEquals("2", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }

        query = "let $query1 := (<a/>)\n" +
                "let $query2 := (2, 3)\n" +
                "let $a := util:exclusive-lock((),($query1, $query2))\n" +
                "return $a";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(3, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("<a/>", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                String r = (String) resource.getContent();
                assertEquals("2", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }

        query = "let $query1 := (<a/>)\n" +
                "let $query2 := (2, 3)\n" +
                "let $a := util:exclusive-lock((),($query1, $query2))\n" +
                "return $a";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(3, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("<a/>", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                String r = (String) resource.getContent();
                assertEquals("2", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }

        query = "let $a := util:exclusive-lock(//*,<root/>)\n" +
                "return $a";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("<root/>", r);
            }
        }
    }

    @Ignore
    @Test
    public void utilEval1() throws XMLDBException {
        String query = "<a><b/></a>/util:eval('*')";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(1, result.getSize());
        }
    }

    /**
     * @see {http://sourceforge.net/tracker/index.php?func=detail&aid=1629363&group_id=17691&atid=117691}
     */
    @Test
    public void utilEval2() throws XMLDBException {
        String query = "let $context := <item/> " +
                "return util:eval(\"<result>{$context}</result>\")";
        // TODO check result
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(1, result.getSize());
        }
    }

    @Test
    public void utilEvalForFunction() throws XMLDBException {
        String query = "declare function local:home()\n"
                + "{\n"
                + "<b>HOME</b>\n"
                + "};\n"
                + "util:eval(\"local:home()\")\n";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(1, result.getSize());
        }
    }

    @Test
    public void sharedLock() throws XMLDBException {
        String query = "let $query1 := (<a/>)\n" +
                "let $query2 := (2, 3)\n" +
                "let $a := util:shared-lock(//*,($query1, $query2))\n" +
                "return $a";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(3, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("<a/>", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                String r = (String) resource.getContent();
                assertEquals("2", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }

        query = "let $query1 := (<a/>)\n" +
                "let $query2 := (2, 3)\n" +
                "let $a := util:shared-lock((),($query1, $query2))\n" +
                "return $a";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(3, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("<a/>", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                String r = (String) resource.getContent();
                assertEquals("2", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }

        query = "let $query1 := (<a/>)\n" +
                "let $query2 := (2, 3)\n" +
                "let $a := util:shared-lock((),($query1, $query2))\n" +
                "return $a";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(3, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("<a/>", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                String r = (String) resource.getContent();
                assertEquals("2", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }

        query = "let $a := util:shared-lock(//*,<root/>)\n" +
                "return $a";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("<root/>", r);
            }
        }
    }

    @Test
    public void encodeForURI() throws XMLDBException {
        String string = "http://www.example.com/00/Weather/CA/Los%20Angeles#ocean";
        String expected = "http%3A%2F%2Fwww.example.com%2F00%2FWeather%2FCA%2FLos%2520Angeles%23ocean";
        String query = "encode-for-uri(\"" + string + "\")";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals(expected, r);
            }
        }

        string = "~b\u00e9b\u00e9";
        expected = "~b%C3%A9b%C3%A9";
        query = "encode-for-uri(\"" + string + "\")";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals(expected, r);
            }
        }

        string = "100% organic";
        expected = "100%25%20organic";
        query = "encode-for-uri(\"" + string + "\")";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals(expected, r);
            }
        }

        query = "let $a := <a><b>-1</b><b>-2</b></a> " +
                "return $a/b[encode-for-uri(.) ne '']";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void iriToURI() throws XMLDBException {
        String string = "http://www.example.com/00/Weather/CA/Los%20Angeles#ocean";
        String expected = "http://www.example.com/00/Weather/CA/Los%20Angeles#ocean";
        String query = "iri-to-uri(\"" + string + "\")";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals(expected, r);
            }
        }

        string = "http://www.example.com/~b\u00e9b\u00e9";
        expected = "http://www.example.com/~b%C3%A9b%C3%A9";
        query = "iri-to-uri(\"" + string + "\")";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals(expected, r);
            }
        }

        string = "$";
        expected = "$";
        query = "iri-to-uri(\"" + string + "\")";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals(expected, r);
            }
        }
    }

    @Test
    public void escapeHTMLURI() throws XMLDBException {
        String string = "http://www.example.com/00/Weather/CA/Los Angeles#ocean";
        String expected = "http://www.example.com/00/Weather/CA/Los Angeles#ocean";
        String query = "escape-html-uri(\"" + string + "\")";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals(expected, r);
            }
        }

        string = "javascript:if (navigator.browserLanguage == 'fr') window.open('http://www.example.com/~b\u00e9b\u00e9');";
        expected = "javascript:if (navigator.browserLanguage == 'fr') window.open('http://www.example.com/~b%C3%A9b%C3%A9');";
        query = "escape-html-uri(\"" + string + "\")";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals(expected, r);
            }
        }

        query = "escape-html-uri('$')";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("$", r);
            }
        }

        query = "let $a := <a><b>-1</b><b>-2</b></a> " +
                "return $a/b[escape-html-uri(.) ne '']";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void localName() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $a := <a><b></b></a> return fn:local-name($a)")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("a", r);
            }
        }
    }

    @Test
    public void localName_empty() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("fn:local-name(())")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("", r);
            }
        }
    }

    @Test
    public void localName_emptyElement() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<a>b</a>/fn:local-name(c)")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("", r);
            }
        }
    }

    @Test
    public void localName_emptyText() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<a>b</a>/fn:local-name(text())")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("", r);
            }
        }
    }

    @Test
    public void localName_contextItem() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $a := <a><b/></a> return $a/b/fn:local-name()")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("b", r);
            }
        }
    }

    @Test
    public void localName_contextItem_empty() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $a := <a><b/></a> return $a/b/c/fn:local-name()")) {
            assertEquals(0, result.getSize());
        }
    }

    @Test
    public void name() throws XPathException, XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $a := <a><b></b></a> return fn:name($a)")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("a", r);
            }
        }
    }

    @Test
    public void name_empty() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("fn:name(())")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("", r);
            }
        }
    }

    @Test
    public void name_emptyElement() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<a>b</a>/fn:name(c)")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("", r);
            }
        }
    }

    @Test
    public void name_emptyText() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<a>b</a>/fn:local-name(text())")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("", r);
            }
        }
    }

    @Test
    public void name_contextItem() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $a := <a><b/></a> return $a/b/fn:name()")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("b", r);
            }
        }
    }

    @Test
    public void name_contextItem_empty() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $a := <a><b/></a> return $a/b/c/fn:name()")) {
            assertEquals(0, result.getSize());
        }
    }

    @Test
    public void dateTimeConstructor() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $date := xs:date('2007-05-02+02:00') return dateTime($date, xs:time('15:12:52.421+02:00'))")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("2007-05-02T15:12:52.421+02:00", r);
            }
        }
    }

    @Test
    public void currentDateTime() throws XMLDBException {
        //Do not use this test around midnight on the last day of a month ;-)
        try (final EXistResourceSet result =  existEmbeddedServer.executeQuery("('Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec')[month-from-dateTime(current-dateTime())]")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                SimpleDateFormat df = new SimpleDateFormat("MMM", new Locale("en", "US"));
                Date date = new Date();
                assertEquals(df.format(date), r);
            }
        }

        String query = "declare option exist:current-dateTime '2007-08-23T00:01:02.062+02:00';" +
                "current-dateTime()";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("2007-08-23T00:01:02.062+02:00", r);
            }
        }
    }

    /**
     * Bugfix 3070
     *
     * @see {http://svn.sourceforge.net/exist/?rev=3070&view=rev}
     *
     * seconds-from-dateTime() returned wrong value when dateTime had
     * no millesecs available. Special value was returned.
     */
    @Test
    public void secondsFromDateTime() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("seconds-from-dateTime(xs:dateTime(\"2005-12-22T13:35:21.000\") )")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("21", r);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("seconds-from-dateTime(xs:dateTime(\"2005-12-22T13:35:21\") )")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("21", r);
            }
        }
    }

    @Test
    public void resolveQName() throws XMLDBException {
        String query = "declare namespace a=\"aes\"; " +
                "declare namespace n=\"ns1\"; " +
                "declare variable $d := <c xmlns:x=\"ns1\"><d>x:test</d></c>; " +
                "for $e in $d/d " +
                "return fn:resolve-QName($e/text(), $e)";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("x:test", r);
            }
        }

        query = "declare namespace a=\"aes\"; " +
                "declare namespace n=\"ns1\"; " +
                "declare variable $d := <c xmlns:x=\"ns1\"><d xmlns:y=\"ns1\">y:test</d></c>; " +
                "for $e in $d/d " +
                "return fn:resolve-QName($e/text(), $e)";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("y:test", r);
            }
        }
    }

    @Test
    public void namespaceURI() throws XMLDBException {
        String query = "let $var := <a xmlns='aaa'/> " +
                "return " +
                "$var[fn:namespace-uri() = 'aaa']/fn:namespace-uri()";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("aaa", r);
            }
        }

        query = "for $a in <test><a xmlns=\"aaa\"><b><c/></b></a></test>//* " +
                "return namespace-uri($a)";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(result.getSize(), 3);
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("aaa", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                String r = (String) resource.getContent();
                assertEquals("aaa", r);
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                String r = (String) resource.getContent();
                assertEquals("aaa", r);
            }
        }
    }

    @Test
    public void namespaceURI_contextItem() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $a := <a><exist:b/></a>  return $a/exist:b/fn:namespace-uri()")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("http://exist.sourceforge.net/NS/exist", r);
            }
        }
    }

    @Test
    public void namespaceURI_contextItem_empty() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $a := <a><b/></a> return $a/exist:b/c/fn:namespace-uri()")) {
            assertEquals(0, result.getSize());
        }
    }

    @Test
    public void prefixFromQName() throws XMLDBException {
        String query = "declare namespace foo = \"http://example.org\"; " +
                "declare namespace FOO = \"http://example.org\"; " +
                "fn:prefix-from-QName(xs:QName(\"foo:bar\"))";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("foo", r);
            }
        }
    }

    @Test
    public void stringJoin() throws XMLDBException {
        String query = "let $s := ('','a','b','') " +
                "return string-join($s,'/')";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("/a/b/", r);
            }
        }
    }

    @Test
    public void nodeName() throws XMLDBException {
        final String query = "let $a := <a><b>-1</b><b>-2</b></a> " +
                "for $b in $a/b[fn:node-name(.) = xs:QName('b')] return $b";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void noeName_empty() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("fn:node-name(())")) {
            assertEquals(0, result.getSize());
        }
    }

    @Test
    public void nodeName_emptyElement() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<a>b</a>/fn:node-name(c)")) {
            assertEquals(0, result.getSize());
        }
    }

    @Test
    public void nodeName_emptyText() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<a>b</a>/fn:node-name(text())")) {
            assertEquals(0, result.getSize());
        }
    }

    @Test
    public void nodeName_contextItem() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $a := <a><b/></a> return $a/b/fn:node-name()")) {
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("b", r);
            }
        }
    }

    @Test
    public void nodeName_contextItem_empty() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("let $a := <a><b/></a> return $a/b/c/fn:node-name()")) {
            assertEquals(0, result.getSize());
        }
    }

    @Test
    public void data0() throws XMLDBException {
        final String query = "let $a := <a><b>1</b><b>1</b></a> " +
                "for $b in $a/b[data() = '1'] return $b";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void data0_atomization() throws XMLDBException {
        final String query = "(<a>1<b>2</b>three</a>, <four>4</four>)/data()";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                assertEquals("12three", resource.getContent().toString());
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                assertEquals("4", resource.getContent());
            }
        }
    }

    @Test
    public void data1() throws XMLDBException {
        final String query = "let $a := <a><b>1</b><b>1</b></a> " +
                "for $b in $a/b[data() = '1'] return $b";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void data1_atomization() throws XMLDBException {
        final String query = "data((<a>1<b>2</b>three</a>, <four>4</four>, xs:integer(5)))";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(3, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                assertEquals("12three", resource.getContent().toString());
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                assertEquals("4", resource.getContent());
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                assertEquals("5", resource.getContent());
            }
        }
    }

    @Test
    public void ceiling() throws XMLDBException {
        String query = "let $a := <a><b>-1</b><b>-2</b></a> " +
                "return $a/b[abs(ceiling(.))]";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void concat() throws XMLDBException {
        String query = "let $a := <a><b>-1</b><b>-2</b></a> " +
                "return $a/b[concat('+', ., '+') = '+-2+']";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(1, result.getSize());
        }
    }

    @Test
    public void documentURI() throws XMLDBException {
        String query = "let $a := <a><b>-1</b><b>-2</b></a> " +
                "return $a/b[empty(document-uri(.))]";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void implicitTimezone() throws XMLDBException {
        String query = "declare option exist:implicit-timezone 'PT3H';" +
                "implicit-timezone()";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                String r = (String) resource.getContent();
                assertEquals("PT3H", r);
            }
        }
    }

    @Test
    public void exists() throws XMLDBException {
        String query = "let $a := <a><b>-1</b><b>-2</b></a> " +
                "return $a/b[exists(.)]";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void floor() throws XMLDBException {
        String query = "let $a := <a><b>-1</b><b>-2</b></a> " +
                "return $a/b[abs(floor(.))]";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
        }
    }

    /**
     * ensure the test collection is removed and call collection-available,
     * which should return false, no exception thrown
     */
    @Test
    public void collectionAvailable1() throws XMLDBException {
        //remove the test collection if it already exists
        String collectionName = "testCollectionAvailable1";
        String collectionPath = XmldbURI.ROOT_COLLECTION + "/" + collectionName;
        String collectionURI = ROOT_COLLECTION_URI + "/" + collectionName;

        Collection testCollection = existEmbeddedServer.getRoot().getChildCollection(collectionName);
        if (testCollection != null) {
            CollectionManagementService cms = (CollectionManagementService) existEmbeddedServer.getRoot().getService("CollectionManagementService", "1.0");
            cms.removeCollection(collectionPath);
        }

        runCollectionAvailableTest(collectionPath, false);
        runCollectionAvailableTest(collectionURI, false);
    }

    /**
     * create a collection and call collection-available, which should return true,
     * no exception thrown
     */
    @Test
    public void collectionAvailable2() throws XMLDBException {
        //add the test collection
        String collectionName = "testCollectionAvailable2";
        String collectionPath = XmldbURI.ROOT_COLLECTION + "/" + collectionName;
        String collectionURI = ROOT_COLLECTION_URI + "/" + collectionName;

        Collection testCollection = existEmbeddedServer.getRoot().getChildCollection(collectionName);
        if (testCollection == null) {
            CollectionManagementService cms = (CollectionManagementService) existEmbeddedServer.getRoot().getService("CollectionManagementService", "1.0");
            try (final Collection created = cms.createCollection(collectionPath)) { }
        }

        runCollectionAvailableTest(collectionPath, true);
        runCollectionAvailableTest(collectionURI, true);
    }

    private void runCollectionAvailableTest(String collectionPath, boolean expectedResult) throws XMLDBException {
        //collection-available should not throw an exception and should return expectedResult
        String importXMLDB = "import module namespace xdb=\"http://exist-db.org/xquery/xmldb\";\n";
        String collectionAvailable = "xdb:collection-available('" + collectionPath + "')";
        String query = importXMLDB + collectionAvailable;
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertNotNull(result);
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                assertNotNull(resource);
                String content = (String) resource.getContent();
                assertNotNull(content);
                assertEquals(expectedResult, Boolean.valueOf(content));
            }
        }
    }

    @Test
    public void base64BinaryCast() throws XMLDBException, URISyntaxException {
        final String TEST_BINARY_COLLECTION = "testBinary";
        final String TEST_COLLECTION = "/db/" + TEST_BINARY_COLLECTION;
        final String BINARY_RESOURCE_FILENAME = "logo.png";
        final String XML_RESOURCE_FILENAME = "logo.xml";

        //create a test collection
        CollectionManagementService colService = (CollectionManagementService) existEmbeddedServer.getRoot().getService("CollectionManagementService", "1.0");
        try (final Collection testCollection = colService.createCollection(TEST_BINARY_COLLECTION)) {
            assertNotNull(testCollection);

            final Path fLogo = Paths.get(getClass().getResource("value/logo.png").toURI());

            //store the eXist logo in the test collection
            try (final EXistResource br = (EXistResource) testCollection.createResource(BINARY_RESOURCE_FILENAME, BinaryResource.RESOURCE_TYPE)) {
                br.setContent(fLogo);
                testCollection.storeResource(br);
            }

            //create an XML resource with the logo base64 embedded in it
            String queryStore = "xquery version \"1.0\";\n\n"
                    + "let $embedded := <logo><image>{util:binary-doc(\"" + TEST_COLLECTION + "/" + BINARY_RESOURCE_FILENAME + "\")}</image></logo> return\n"
                    + "xmldb:store(\"" + TEST_COLLECTION + "\", \"" + XML_RESOURCE_FILENAME + "\", $embedded)";

            try (final EXistResourceSet result = existEmbeddedServer.executeQuery(queryStore)) {
                assertEquals("store, Expect single result", 1, result.getSize());
                try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                    assertEquals("Expect stored filename as result", TEST_COLLECTION + "/" + XML_RESOURCE_FILENAME, resource.getContent().toString());
                }
            }

            //retrieve the base64 image from the XML resource and try to cast to xs:base64Binary
            String queryRetreive = "xquery version \"1.0\";\n\n"
                    + "let $image := doc(\"" + TEST_COLLECTION + "/" + XML_RESOURCE_FILENAME + "\")/logo/image return\n"
                    + "$image/text() cast as xs:base64Binary";

            try (final EXistResourceSet result = existEmbeddedServer.executeQuery(queryRetreive)) {
                assertEquals("retreive, Expect single result", 1, result.getSize());
            }
        }
    }

    @Test
    public void defaultLanguage() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("default-language()")) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String defaultLanguage = (String) resource.getContent();
                assertEquals(Locale.getDefault().getLanguage(), defaultLanguage);
            }
        }
    }

    @Test
    public void enclosedExpression() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<abc>{()}{123}</abc>")) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String text = (String) resource.getContent();
                assertEquals("<abc>123</abc>", text);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<abc>{(), 123}</abc>")) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String text = (String) resource.getContent();
                assertEquals("<abc>123</abc>", text);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<abc>{()}123</abc>")) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String text = (String) resource.getContent();
                assertEquals("<abc>123</abc>", text);
            }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<root>{'time '}{()}{'is: '}{current-time()}</root>")) {
            assertEquals(1, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                final String text = (String) resource.getContent();
                assertTrue(text.startsWith("<root>time is: "));
                assertTrue(text.length() > 35);
                assertTrue(text.endsWith("</root>"));
            }
        }
    }
}
