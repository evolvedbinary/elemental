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

import com.evolvedbinary.j8fu.function.ConsumerE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.dom.QName;
import org.exist.security.PermissionDeniedException;
import org.exist.source.SourceFactory;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.EXistXPathQueryService;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Type;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.*;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.matchers.CompareMatcher;
import xyz.elemental.mediatype.MediaType;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * I propose that we put here in XQueryTest the tests involving all the
 * others constructs of the XQuery language, besides XPath expressions.
 * And in {@link XPathQueryTest} we will put the tests involving only XPath expressions.
 *
 * TODO maybe move the various eXist XQuery extensions in another class ...
 */
public class XQueryTest {

    private final static Logger LOG = LogManager.getLogger(XQueryTest.class);

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    private static final String NUMBERS_XML = "numbers.xml";
    private static final String BOWLING_XML = "bowling.xml";
    private static final String attributesSERIALIZATION = "attributes_serialization.xml";
    private static final String MODULE1_NAME = "module1.xqm";
    private static final String MODULE2_NAME = "module2.xqm";
    private static final String MODULE3_NAME = "module3.xqm";
    private static final String MODULE4_NAME = "module4.xqm";
    private static final String MODULE5_NAME = "module5.xqm";
    private static final String MODULE6_NAME = "module6.xqm";
    private static final String MODULE7_NAME = "module7.xqm";
    private static final String MODULE8_NAME = "module8.xqm";
    private static final String FATHER_MODULE_NAME = "father.xqm";
    private static final String CHILD1_MODULE_NAME = "child1.xqm";
    private static final String CHILD2_MODULE_NAME = "child2.xqm";
    private static final String NAMESPACED_NAME = "namespaced.xml";
    private final static String URI = XmldbURI.LOCAL_DB;
    private final static String numbers =
            "<test>" + "<item id='1'><price>5.6</price><stock>22</stock></item>" + "<item id='2'><price>7.4</price><stock>43</stock></item>" + "<item id='3'><price>18.4</price><stock>5</stock></item>" + "<item id='4'><price>65.54</price><stock>16</stock></item>" + "</test>";
    private final static String module1 =
            "module namespace blah=\"blah\";\n" + "declare variable $blah:param := \"value-1\";";
    private final static String module2 =
            "module namespace foo=\"\";\n" + "declare variable $foo:bar := \"bar\";";
    private final static String module3 =
            "module namespace foo=\"foo\";\n" + "declare variable $bar:bar := \"bar\";";
    private final static String module4 =
            "module namespace foo=\"foo\";\n" //An external prefix in the statically known namespaces
            + "declare variable $exist:bar external;\n" + "declare function foo:bar() {\n" + "$exist:bar\n" + "};";
    private final static String module5 =
            "module namespace foo=\"foo\";\n" + "declare variable $foo:bar := \"bar\";";
    private final static String module6 =
            "module namespace foo=\"foo\";\n" + "declare variable $foo:bar := \"bar\";" + "declare variable $foo:bar := \"bar\";";
    private final static String module7 =
            "module namespace foo=\"foo\";\n" +
            "declare namespace xhtml=\"http://www.w3.org/1999/xhtml\";\n" +
            "declare function foo:link() { <a href='#'>Link</a> };" +
            "declare function foo:copy($node) { element { node-name($node) } { $node/text() } };";
    private final static String module8 =
            "module namespace dr = \"double-root2\"; \n"
            +"declare function dr:documentIn() as document-node() { \n"
            +" let $doc :=  <root> <contents/> </root> \n"
            +" return document { $doc } \n" 
            +"};";
    
    private final static String fatherModule =
            "module namespace foo=\"foo\";\n" + "import module namespace foo1=\"foo1\" at \"" + URI + "/test/" + CHILD1_MODULE_NAME + "\";\n" + "import module namespace foo2=\"foo2\" at \"" + URI + "/test/" + CHILD2_MODULE_NAME + "\";\n" + "declare variable $foo:bar := \"bar\";\n " + "declare variable $foo:bar1 := $foo1:bar;\n" + "declare variable $foo:bar2 := $foo2:bar;\n";
    private final static String child1Module =
            "module namespace foo=\"foo1\";\n" + "import module namespace blah=\"blah\" at \"" + URI + "/test/" + MODULE1_NAME + "\";\n" + "declare variable $foo:bar := \"bar1\";";
    private final static String child2Module =
            "module namespace foo=\"foo2\";\n" + "import module namespace blah=\"blah\" at \"" + URI + "/test/" + MODULE1_NAME + "\";\n" + "declare variable $foo:bar := \"bar2\";";
    private final static String namespacedDocument =
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
            "xmlns:x=\"http://exist.sourceforge.net/dc-ext\">\n" +
            "    <rdf:Description id=\"3\">\n" +
            "        <dc:title>title</dc:title>\n" +
            "        <dc:creator>creator</dc:creator>\n" +
            "        <x:place>place</x:place>\n" +
            "        <x:edition>place</x:edition>\n" +
            "    </rdf:Description>\n" +
            "</rdf:RDF>";
    private final static String bowling =
            "<series>" +
            "<game>" +
            "<frame/>" +
            "</game>" +
            "<game>" +
            "<frame/>" +
            "</game>" +
            "</series>";
    private final static String attributes =
        "<blob>" +
        "<test att='a' />" +
        "<test att='b' />" +
        "<test att='c' />" +
        "</blob>";

    private static int stringSize = 512;
    private static int nbElem = 1;
    private String file_name = "detail_xml.xml";
    private String xml;

    @Before
    public void setup() throws XMLDBException {
        final CollectionManagementService service =
                existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
        try (final Collection created = service.createCollection("test")) { }
    }

    @After
    public void tearDown() throws XMLDBException {
        final CollectionManagementService service =
                existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
        service.removeCollection("test");
    }

    private Collection getTestCollection() throws XMLDBException {
        return DatabaseManager.getCollection("xmldb:exist:///db/test", "admin", "");
    }

    @Test
    public void let() throws XMLDBException, IOException, SAXException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);

        //Non null context sequence
        String query = "/test/item[let $id := ./@id return $id]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 4, result.getSize());
        }

        query = "/test/item[let $id := ./@id return not(/test/set[@id=$id])]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 4, result.getSize());
            query = "let $test := <test><a> a </a><a>a</a></test> " +
                "return distinct-values($test/a/normalize-space(.))";
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
        }

        //Ordered value sequence
        query = "let $unordset := (for $val in reverse(1 to 100) return " +
                "<value>{$val}</value>)" +
                "let $ordset := (for $newval in $unordset " +
                "where $newval mod 2 eq 1 " +
                "order by $newval " +
                "return $newval/text()) " +
                "return $ordset/ancestor::node()";

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 50, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<value>99</value>"));
            }
            try (final Resource resource = result.getResource(49)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<value>1</value>"));
            }
        }
    }

    @Test
    public void testFor() throws XMLDBException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);

        String query = "for $f in /*/item return $f";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 4, result.getSize());
        }

        query = "for $f in /*/item  order by $f ascending  return $f";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query);
             final XMLResource resource = (XMLResource) result.getResource(0)) {
            assertEquals("XQuery: " + query, "3", ((Element) resource.getContentAsDOM()).getAttribute("id"));
        }

        query = "for $f in /*/item  order by $f descending  return $f";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query);
             final XMLResource resource = (XMLResource) result.getResource(0)) {
            assertEquals("XQuery: " + query, "2", ((Element) resource.getContentAsDOM()).getAttribute("id"));
        }

        query = "for $f in /*/item  order by xs:double($f/price) descending  return $f";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query);
             final XMLResource resource = (XMLResource) result.getResource(0)) {
            assertEquals("XQuery: " + query, "4", ((Element) resource.getContentAsDOM()).getAttribute("id"));
        }

        query = "for $f in //item where $f/@id = '3' return $f";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query);
             final XMLResource resource = (XMLResource) result.getResource(0)) {
            assertEquals("XQuery: " + query, "3", ((Element) resource.getContentAsDOM()).getAttribute("id"));
        }

        //Non null context sequence
        query = "/test/item[for $id in ./@id return $id]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query);
             final XMLResource resource = (XMLResource) result.getResource(0)) {
            assertEquals("XQuery: " + query, 4, result.getSize());
        }

        //Ordered value sequence
        query = "let $doc := <doc><value>Z</value><value>Y</value><value>X</value></doc> " +
                "return " +
                "let $ordered_values := " +
                "	for $value in $doc/value order by $value ascending " +
                "	return $value " +
                "for $value in $doc/value " +
                "	return $value[. = $ordered_values[position() = 1]]";

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query);
             final XMLResource resource = (XMLResource) result.getResource(0)) {
            assertEquals("XQuery: " + query, "<value>X</value>", resource.getContent());
        }

        //Ordered value sequence
        query = "for $e in (1) order by $e return $e";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query);
             final XMLResource resource = (XMLResource) result.getResource(0)) {
            assertEquals("XQuery: " + query, "1", resource.getContent());
        }
    }

    @Test
    public void recursion() throws XMLDBException {
        final String q1 =
                "declare function local:append($head, $i) {\n" +
                "   if ($i < 5000) then\n" +
                "       local:append(($head, $i), $i + 1)\n" +
                "   else\n" +
                "       $head\n" +
                "};\n" +
                "local:append((), 0)";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(q1)) {
                assertEquals(result.getSize(), 5000);
            }
        }
    }

    @Test
    public void constructedNode1() throws XMLDBException {
        final String q1 = "let $a := <A/> for $b in $a//B/string() return \"Oops!\"";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(q1)) {
                assertEquals(0, result.getSize());
            }
        }
    }

    @Test
    public void combiningNodeSequences() throws XMLDBException {
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            String query = "let $a := <a/> \n" +
                    "let $aa := ($a, $a) \n" +
                    "for $b in ($aa intersect $aa \n)" +
                    "return $b";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, 1, result.getSize());
                assertEquals("XQuery: " + query, "<a/>", resource.getContent());
            }

            query = "let $a := <a/> \n" +
                    "let $aa := ($a, $a) \n" +
                    "for $b in ($aa union $aa \n)" +
                    "return $b";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, 1, result.getSize());
                assertEquals("XQuery: " + query, "<a/>", resource.getContent());
            }

            query = "let $a := <a/> \n" +
                    "let $aa := ($a, $a) \n" +
                    "for $b in ($aa except $aa \n)" +
                    "return $b";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals("XQuery: " + query, 0, result.getSize());
            }
        }
    }

    /**
     * @author Gev
     */
    @Test
    public void inMemoryNodeSequences() throws XMLDBException {
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);

            String query = "let $c := (<a/>,<b/>) return <t>text{$c[1]}</t>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<t>text<a/>\n</t>", resource.getContent());
            }

            query = "let $c := (<a/>,<b/>) return <t><text/>{$c[1]}</t>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<t>\n    <text/>\n    <a/>\n</t>", resource.getContent());
            }

            query = "let $c := (<a/>,<b/>) return <t>{\"text\"}{$c[1]}</t>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<t>text<a/>\n</t>", resource.getContent());
            }

            query = "let $c := (<a/>,\"b\") return <t>text{$c[1]}</t>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<t>text<a/>\n</t>", resource.getContent());
            }

            query = "let $c := (<a/>,\"b\") return <t><text/>{$c[1]}</t>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<t>\n    <text/>\n    <a/>\n</t>", resource.getContent());
            }

            query = "let $c := (<a/>,\"b\") return <t>{\"text\"}{$c[1]}</t>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<t>text<a/>\n</t>", resource.getContent());
            }

            query = "let $c := (<a/>,<b/>) return <t>{<text/>,$c[1]}</t>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<t>\n    <text/>\n    <a/>\n</t>", resource.getContent());
            }

            query = "let $c := (<a/>,<b/>) return <t>{\"text\",$c[1]}</t>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<t>text<a/>\n</t>", resource.getContent());
            }

            query = "let $c := (<a/>,\"b\") return <t>{<text/>,$c[1]}</t>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<t>\n    <text/>\n    <a/>\n</t>", resource.getContent());
            }

            query = "let $c := (<a/>,\"b\") return <t>{\"text\",$c[1]}</t>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<t>text<a/>\n</t>", resource.getContent());
            }
        }
    }

    @Test
    public void variable() throws XMLDBException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);

        String query = "xquery version \"1.0\";\n" + "declare namespace param=\"param\";\n" + "declare variable $param:a := \"a\";\n" + "declare function param:a() {$param:a};\n" + "let $param:a := \"b\" \n" + "return ($param:a, $param:a)";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 2, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "b", resource.getContent());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("XQuery: " + query, "b", resource.getContent());
            }
        }

        query = "xquery version \"1.0\";\n" + "declare namespace param=\"param\";\n" + "declare variable $param:a := \"a\";\n" + "declare function param:a() {$param:a};\n" + "let $param:a := \"b\" \n" + "return param:a(), param:a()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 2, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "a", resource.getContent());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("XQuery: " + query, "a", resource.getContent());
            }
        }

        query = "declare variable $foo := \"foo1\";\n" + "let $foo := \"foo2\" \n" + "for $bar in (1 to 1) \n" + "  let $foo := \"foo3\" \n" + "  return $foo";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "foo3", resource.getContent());
            }
        }

        String message = "";
        try {
            query = "xquery version \"1.0\";\n" + "declare variable $a := \"1st instance\";\n" + "declare variable $a := \"2nd instance\";\n" + "$a";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                // needed to ensure that result is closed
            }
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XQST0049") > -1);

        query = "xquery version \"1.0\";\n" + "declare namespace param=\"param\";\n" + "declare function param:f() { $param:a };\n" + "declare variable $param:a := \"a\";\n" + "param:f()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "a", resource.getContent());
            }
        }

        query = "let $a := <root> " +
                "<b name='1'>" +
                "  <c name='x'> " +
                "    <bar name='2'/> " +
                "    <bar name='3'> " +
                "      <bar name='4'/> " +
                "    </bar> " +
                "  </c> " +
                "</b> " +
                "</root> " +
                "let $b := for $bar in $a/b/c/bar " +
                "where ($bar/../@name = 'x') " +
                "return $bar " +
                "return $b";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 2, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XQuery: " + query, "2", ((Element) resource.getContentAsDOM()).getAttribute("name"));
            }
            try (final XMLResource resource = (XMLResource) result.getResource(1)) {
                assertEquals("XQuery: " + query, "3", ((Element) resource.getContentAsDOM()).getAttribute("name"));
            }
        }
    }

    @Test
    public void virtualNodesets() throws XMLDBException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);
        service.setProperty(OutputKeys.INDENT, "no");

        String query = "let $node := (<c id='OK'><b id='cool'/></c>)/descendant::*/attribute::id " +
                "return <a>{$node}</a>";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a id='cool'/>"));
            }
        }

        query = "let $node := (<c id='OK'><b id='cool'/></c>)/descendant-or-self::*/child::b " +
                "return <a>{$node}</a>";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a><b id='cool'/></a>"));
            }
        }

        query = "let $node := (<c id='OK'><b id='cool'/></c>)/descendant-or-self::*/descendant::b " +
                "return <a>{$node}</a>";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a><b id='cool'/></a>"));
            }
        }

        query = "let $doc := <a id='a'><b id='b'/></a> " +
                "return $doc/*/(<id>{@id}</id>)";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<id id='b' />"));
            }
        }
    }

    @Test
    public void whereClause() throws XMLDBException, IOException, SAXException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);
        service.setProperty(OutputKeys.INDENT, "no");

        final String query = "let $a := element node1 { " +
                "attribute id {'id'}, " +
                "element node1 {'1'}, " +
                "element node2 {'2'} " +
                "} " +
                "for $x in $a " +
                "where $x/@id eq 'id' " +
                "return $x";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(NUMBERS_XML, query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<node1 id='id'><node1>1</node1><node2>2</node2></node1>"));
            }
        }
    }

    @Test
    public void typedVariables() throws XMLDBException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);

        String query = "let $v as element()* := ( <assign/> , <assign/> )\n" + "let $w := <r>{ $v }</r>\n" + "let $x as element()* := $w/assign\n" + "return $x";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 2, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XQuery: " + query, Node.ELEMENT_NODE, resource.getContentAsDOM().getNodeType());
                assertEquals("XQuery: " + query, "assign", resource.getContentAsDOM().getNodeName());
            }
        }

        query = "let $v as node()* := ()\n" + "return $v";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 0, result.getSize());
        }

        query = "let $v as item()* := ()\n" + "return $v";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 0, result.getSize());
        }

        query = "let $v as empty-sequence() := ()\n" + "return $v";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 0, result.getSize());
        }

        query = "let $v as item() := ()\n" + "return $v";
        String message = "";
        boolean exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue("XQuery: " + query, exceptionThrown);

        query = "let $v as item()* := ( <a/> , 1 )\n" + "return $v";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 2, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XQuery: " + query, Node.ELEMENT_NODE, resource.getContentAsDOM().getNodeType());
                assertEquals("XQuery: " + query, "a", resource.getContentAsDOM().getNodeName());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("XQuery: " + query, "1", resource.getContent());
            }
        }

        query = "let $v as node()* := ( <a/> , 1 )\n" + "return $v";
        exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue(exceptionThrown);

        query = "let $v as item()* := ( <a/> , 1 )\n" + "let $w as element()* := $v\n" + "return $w";
        exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue(exceptionThrown);

        query = "declare variable $v as element()* := ( <assign/> , <assign/> );\n" + "declare variable $w := <r>{ $v }</r>;\n" + "declare variable $x as element()* := $w/assign;\n" + "$x";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 2, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XQuery: " + query, Node.ELEMENT_NODE, resource.getContentAsDOM().getNodeType());
                assertEquals("XQuery: " + query, "assign", resource.getContentAsDOM().getNodeName());
            }
        }

        query = "declare variable $v as node()* := ();\n" + "$v";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 0, result.getSize());
        }

        query = "declare variable $v as item()* := ();\n" + "$v";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 0, result.getSize());
        }

        query = "declare variable $v as empty-sequence() := ();\n" + "$v";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 0, result.getSize());
        }

        query = "declare variable $v as item() := ();\n" + "$v";
        exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue("XQuery: " + query, exceptionThrown);

        query = "declare variable $v as item()* := ( <a/> , 1 );\n" + "$v";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 2, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XQuery: " + query, Node.ELEMENT_NODE, resource.getContentAsDOM().getNodeType());
                assertEquals("XQuery: " + query, "a", resource.getContentAsDOM().getNodeName());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("XQuery: " + query, "1", resource.getContent());
            }
        }

        query = "declare variable $v as node()* := ( <a/> , 1 );\n" + "$v";
        exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue(exceptionThrown);

        query = "declare variable $v as item()* := ( <a/> , 1 );\n" + "declare variable $w as element()* := $v;\n" + "$w";
        exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue(exceptionThrown);

        query = "let $v as document-node() :=  doc('" + XmldbURI.ROOT_COLLECTION + "/test/" + NUMBERS_XML + "') \n" + "return $v";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            //TODO : no way to test the node type ?
            //assertEquals( "XQuery: " + query, Node.DOCUMENT_NODE, ((XMLResource)result.getResource(0)));
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                final Node n = resource.getContentAsDOM();
                assertTrue(n instanceof Document);
                assertEquals("XQuery: " + query, "test", ((Document) n).getDocumentElement().getNodeName());
            }
        }
    }

    @Test
    public void precedence() throws XMLDBException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);
        final String query = "xquery version \"1.0\";\n" + "declare namespace blah=\"blah\";\n" + "declare variable $blah:param := \"value-1\";\n" + "let $blah:param := \"value-2\"\n" + "(:: FLWOR expressions have a higher precedence than the comma operator ::)\n" + "return $blah:param, $blah:param ";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 2, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "value-2", resource.getContent());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("XQuery: " + query, "value-1", resource.getContent());
            }
        }
    }

    @Test
    public void improbableAxesAndNodeTestsCombinations() throws XMLDBException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);
        String query = "let $a := <x>a<!--b-->c</x>/self::comment() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/parent::comment() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/ancestor::comment() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/ancestor-or-self::comment() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

//			This one is intercepted by the parser
        query = "let $a := <x>a<!--b-->c</x>/attribute::comment() return <z>{$a}</z>";
        String message = "";
        boolean exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue(exceptionThrown);

//			This one is intercepted by the parser
        query = "let $a := <x>a<!--b-->c</x>/namespace::comment() return <z>{$a}</z>";
        exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue(exceptionThrown);

        query = "let $a := <x>a<!--b-->c</x>/self::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/parent::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/ancestor::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/ancestor-or-self::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/child::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/descendant::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/descendant-or-self::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/preceding::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/preceding-sibling::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/following::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

        query = "let $a := <x>a<!--b-->c</x>/following-sibling::attribute() return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<z/>", resource.getContent());
            }
        }

//			This one is intercepted by the parser
        query = "let $a := <x>a<!--b-->c</x>/namespace::attribute() return <z>{$a}</z>";
        exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue(exceptionThrown);

        //TODO : uncomment when PI are OK

        /*
        query = "let $a := <x>a<?foo ?>c</x>/self::processing-instruction('foo') return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals( "XQuery: " + query, 1, result.getSize() );
            assertEquals( "XQuery: " + query, "<z/>", ((XMLResource)result.getResource(0)).getContent());
        }

        query = "let $a := <x>a<?foo ?>c</x>/parent::processing-instruction('foo') return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals( "XQuery: " + query, 1, result.getSize() );
            assertEquals( "XQuery: " + query, "<z/>", ((XMLResource)result.getResource(0)).getContent());
        }

        query = "let $a := <x>a<?foo ?>c</x>/ancestor::processing-instruction('foo') return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals( "XQuery: " + query, 1, result.getSize() );
            assertEquals( "XQuery: " + query, "<z/>", ((XMLResource)result.getResource(0)).getContent());
        }

        query = "let $a := <x>a<?foo ?>c</x>/ancestor-or-self::processing-instruction('foo') return <z>{$a}</z>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals( "XQuery: " + query, 1, result.getSize() );
            assertEquals( "XQuery: " + query, "<z/>", ((XMLResource)result.getResource(0)).getContent());
        }
        */

//			This one is intercepted by the parser
        query = "let $a := <x>a<?foo ?>c</x>/attribute::processing-instruction('foo') return <z>{$a}</z>";
        exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue(exceptionThrown);

//			This one is intercepted by the parser
        query = "let $a := <x>a<?foo ?>c</x>/namespace::processing-instruction('foo') return <z>{$a}</z>";
        exceptionThrown = false;
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue(exceptionThrown);
    }

    @Test
    public void namespace() throws XMLDBException, IOException, SAXException {
        final XPathQueryService service;
        try (final Collection testCollection = getTestCollection()) {
            try (final Resource doc = testCollection.createResource(MODULE1_NAME, BinaryResource.class)) {
                doc.setContent(module1);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

           try (final Resource doc = testCollection.createResource(MODULE2_NAME, BinaryResource.class)) {
               doc.setContent(module2);
               ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
               testCollection.storeResource(doc);
           }

            try (final Resource doc = testCollection.createResource(NAMESPACED_NAME, XMLResource.class)) {
                doc.setContent(namespacedDocument);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XML);
                testCollection.storeResource(doc);
            }

            service = testCollection.getService(XPathQueryService.class);
        }

        String query = "xquery version \"1.0\";\n" + "import module namespace blah=\"blah\" at \"" + URI + "/test/" + MODULE1_NAME + "\";\n" + "(:: redefine existing prefix ::)\n" + "declare namespace blah=\"bla\";\n" + "$blah:param";
        String message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XQST0033") > -1);

        query = "xquery version \"1.0\";\n" + "import module namespace blah=\"blah\" at \"" + URI + "/test/" + MODULE1_NAME + "\";\n" + "(:: redefine existing prefix with same getUri ::)\n" + "declare namespace blah=\"blah\";\n" + "declare variable $blah:param := \"value-2\";\n" + "$blah:param";
        message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XQST0033") > -1);

        query = "xquery version \"1.0\";\n" + "import module namespace foo=\"ho\" at \"" + URI + "/test/" + MODULE1_NAME + "\";\n" + "$foo:bar";
        message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("does not match namespace URI") > -1);

        query = "xquery version \"1.0\";\n" + "import module namespace foo=\"ho\" at \"" + URI + "/test/" + MODULE2_NAME + "\";\n" + "$bar";
        message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("No namespace defined for prefix") > -1);

        query = "xquery version \"1.0\";\n" + "import module namespace foo=\"blah\" at \"" + URI + "/test/" + MODULE2_NAME + "\";\n" + "$bar";
        message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("No namespace defined for prefix") > -1);

        query = "declare namespace x = \"http://www.foo.com\"; \n" +
                "let $a := doc('" + XmldbURI.ROOT_COLLECTION + "/test/" + NAMESPACED_NAME + "') \n" +
                "return $a//x:edition";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 0, result.getSize());
        }

        query = "declare namespace x = \"http://www.foo.com\"; \n" +
                "declare namespace y = \"http://exist.sourceforge.net/dc-ext\"; \n" +
                "let $a := doc('" + XmldbURI.ROOT_COLLECTION + "/test/" + NAMESPACED_NAME + "') \n" +
                "return $a//y:edition";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "<x:edition xmlns:x=\"http://exist.sourceforge.net/dc-ext\">place</x:edition>",
                    resource.getContent());
            }
        }

        query = "<result xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>{//rdf:Description}</result>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(query,
                    "<result xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
                        "    <rdf:Description id=\"3\">\n" +
                        "        <dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">title</dc:title>\n" +
                        "        <dc:creator xmlns:dc=\"http://purl.org/dc/elements/1.1/\">creator</dc:creator>\n" +
                        "        <x:place xmlns:x=\"http://exist.sourceforge.net/dc-ext\">place</x:place>\n" +
                        "        <x:edition xmlns:x=\"http://exist.sourceforge.net/dc-ext\">place</x:edition>\n" +
                        "    </rdf:Description>\n" +
                        "</result>",
                    resource.getContent());
            }
        }

        query = "<result xmlns='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>{//Description}</result>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query,
                    "<result xmlns=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
                        "    <Description id=\"3\">\n" +
                        "        <dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">title</dc:title>\n" +
                        "        <dc:creator xmlns:dc=\"http://purl.org/dc/elements/1.1/\">creator</dc:creator>\n" +
                        "        <x:place xmlns:x=\"http://exist.sourceforge.net/dc-ext\">place</x:place>\n" +
                        "        <x:edition xmlns:x=\"http://exist.sourceforge.net/dc-ext\">place</x:edition>\n" +
                        "    </Description>\n" +
                        "</result>",
                    resource.getContent());
            }
        }

        //Interesting one : let's see with XQuery gurus :-)
        //declare namespace fn="";
        //fn:current-time()
        /*
        If the URILiteral part of a namespace declaration is a zero-length string,
        any existing namespace binding for the given prefix is removed from
        the statically known namespaces. This feature provides a way
        to remove predeclared namespace prefixes such as local.
         */
        query = "declare option exist:serialize 'indent=no';" +
                "for $x in <parent4 xmlns=\"http://www.example.com/parent4\"><child4/></parent4> " +
                "return <new>{$x//*:child4}</new>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<new><child4 xmlns='http://www.example.com/parent4'/></new>"));
            }
        }
    }

    @Test
    public void namespaceWithTransform() throws XMLDBException {
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);

            String query =
                    "xquery version \"1.0\";\n" +
                            "declare namespace transform=\"http://exist-db.org/xquery/transform\";\n" +
                            "declare variable $xml := \n" +
                            "	<node>text</node>\n" +
                            ";\n" +
                            "declare variable $xslt := \n" +
                            "	<xsl:stylesheet xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">\n" +
                            "		<xsl:template match=\"node\">\n" +
                            "			<div><xsl:value-of select=\".\"/></div>\n" +
                            "		</xsl:template>\n" +
                            "	</xsl:stylesheet>\n" +
                            ";\n" +
                            "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                            "	<body>\n" +
                            "		{transform:transform($xml, $xslt, ())}\n" +
                            "	</body>\n" +
                            "</html>";

            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {

                //check there is one result
                assertEquals(1, result.getSize());

                try (final Resource resource = result.getResource(0)) {
                    String content = (String) resource.getContent();

                    //check the namespace
                    assertTrue(content.startsWith("<html xmlns=\"http://www.w3.org/1999/xhtml\">"));

                    //check the content
                    assertTrue(content.indexOf("<div>text</div>") > -1);
                }
            }
        }
    }

    @Test
    public void module() throws XMLDBException {
        final XPathQueryService service;
        try (final Collection testCollection = getTestCollection()) {
            try (final Resource doc = testCollection.createResource(MODULE1_NAME, BinaryResource.class)) {
                doc.setContent(module1);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            try (final Resource doc = testCollection.createResource(MODULE3_NAME, BinaryResource.class)) {
                doc.setContent(module3);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            try (final Resource doc = testCollection.createResource(MODULE4_NAME, BinaryResource.class)) {
                doc.setContent(module4);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            try (final Resource doc = testCollection.createResource(FATHER_MODULE_NAME, BinaryResource.class)) {
                doc.setContent(fatherModule);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            try (final Resource doc = testCollection.createResource(CHILD1_MODULE_NAME, BinaryResource.class)) {
                doc.setContent(child1Module);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            try (final Resource doc = testCollection.createResource(CHILD2_MODULE_NAME, BinaryResource.class)) {
                doc.setContent(child2Module);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            service = testCollection.getService(XPathQueryService.class);
        }

        String query = "xquery version \"1.0\";\n" + "import module namespace blah=\"blah\" at \"" + URI + "/test/" + MODULE1_NAME + "\";\n" + "$blah:param";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "value-1", resource.getContent());
            }
        }

//            query = "xquery version \"1.0\";\n" + "import module namespace blah=\"blah\" at \"" + getUri + "/test/" + MODULE1_NAME + "\";\n" + "(:: redefine variable ::)\n" + "declare variable $blah:param := \"value-2\";\n" + "$blah:param";
//            try {
//                message = "";
//                result = service.query(query);
//            } catch (XMLDBException e) {
//                message = e.getMessage();
//            }
//            assertTrue(message.indexOf("XQST0049") > -1);
        query = "xquery version \"1.0\";\n" + "import module namespace blah=\"blah\" at \"" + URI + "/test/" + MODULE1_NAME + "\";\n" + "declare namespace blah2=\"blah\";\n" + "$blah2:param";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "value-1", resource.getContent());
            }
        }

        query = "xquery version \"1.0\";\n" + "import module namespace blah=\"bla\" at \"" + URI + "/test/" + MODULE1_NAME + "\";\n" + "$blah:param";
        String message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("does not match namespace URI") > -1);
        query = "xquery version \"1.0\";\n" + "import module namespace foo=\"foo\" at \"" + URI + "/test/" + FATHER_MODULE_NAME + "\";\n" + "$foo:bar, $foo:bar1, $foo:bar2";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 3, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "bar", resource.getContent());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("XQuery: " + query, "bar1", resource.getContent());
            }
            try (final Resource resource = result.getResource(2)) {
                assertEquals("XQuery: " + query, "bar2", resource.getContent());
            }
        }

//			Non-transitive inheritance check
        query = "xquery version \"1.0\";\n" + "import module namespace foo=\"foo\" at \"" + URI + "/test/" + FATHER_MODULE_NAME + "\";\n" + "declare namespace foo1=\"foo1\"; \n" + "$foo1:bar";
        message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XPST0008") > -1);

//			Non-transitive inheritance check
        query = "xquery version \"1.0\";\n" + "import module namespace foo=\"foo\" at \"" + URI + "/test/" + FATHER_MODULE_NAME + "\";\n" + "declare namespace foo2=\"foo2\"; \n" + "$foo2:bar";
        message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XPST0008") > -1);

        query = "xquery version \"1.0\";\n" + "import module namespace foo1=\"foo\" at \"" + URI + "/test/" + CHILD1_MODULE_NAME + "\";\n" + "import module namespace foo2=\"foo\" at \"" + URI + "/test/" + CHILD1_MODULE_NAME + "\";\n" + "$foo1:bar";
        message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
//			Should be a XQST0047 error
        assertTrue(message.indexOf("does not match namespace URI") > -1);
        query = "xquery version \"1.0\";\n" + "import module namespace foo=\"foo\" at \"" + URI + "/test/" + MODULE3_NAME + "\";\n" + "$bar:bar";
        message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("No namespace defined for prefix") > -1);

        query = "xquery version \"1.0\";\n" + "import module namespace foo=\"foo\" at \"" + URI + "/test/" + MODULE4_NAME + "\";\n" + "foo:bar()";
        message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            //WARNING !
            //This result is false ! The external vairable has not been resolved
            //Furthermore it is not in the module's namespace !
            assertEquals("XQuery: " + query, 0, result.getSize());
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        //This is the good result !
        //assertTrue(message.indexOf("XQST0048") > -1);
    }

    @Test
    public void modulesAndNS() throws XMLDBException, IOException, SAXException {
        final XPathQueryService service;
        try (final Collection testCollection = getTestCollection()) {

            try (final Resource doc = testCollection.createResource(MODULE7_NAME, BinaryResource.class)) {
                doc.setContent(module7);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            service = testCollection.getService(XPathQueryService.class);
        }
        service.setProperty(OutputKeys.INDENT, "no");
        String query = "xquery version \"1.0\";\n" +
                "import module namespace foo=\"foo\" at \"" + URI + "/test/" + MODULE7_NAME + "\";\n" +
                "<div xmlns='http://www.w3.org/1999/xhtml'>" +
                "{ foo:link() }" +
                "</div>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<div xmlns='http://www.w3.org/1999/xhtml'><a xmlns=\"\" href='#'>Link</a></div>"));
            }
        }

        query = "xquery version \"1.0\";\n" +
                "import module namespace foo=\"foo\" at \"" + URI + "/test/" + MODULE7_NAME + "\";\n" +
                "<div xmlns='http://www.w3.org/1999/xhtml'>" +
                "{ foo:copy(<a>Link</a>) }" +
                "</div>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<div xmlns='http://www.w3.org/1999/xhtml'><a>Link</a></div>"));
            }
        }
    }

    @Test
    public void importExternalClasspathMainModule() throws EXistException, IOException, PermissionDeniedException, XPathException, QName.IllegalQNameException {
        final long timestamp = System.currentTimeMillis();
        final BrokerPool brokerPool = BrokerPool.getInstance();
        try (final DBBroker broker = brokerPool.getBroker()) {
            final org.exist.source.Source source = SourceFactory.getSource(broker, "/", "resource:org/exist/xquery/external-classpath-main-module.xq", false);

            final ConsumerE<XQueryContext, XPathException> setupXqueryContextPreCompilation = xqueryContext -> {
                try {
                    xqueryContext.declareVariable(new QName("s"), true, new IntegerValue(timestamp));
                } catch (final QName.IllegalQNameException e) {
                    throw new XPathException(e.getMessage(), e);
                }
            };

            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, source, false, null, null, setupXqueryContextPreCompilation, null, null)) {

                assertEquals(1, queryResult.result.getItemCount());
                final Item item = queryResult.result.itemAt(0);
                assertTrue(Type.subTypeOf(item.getType(), Type.NODE));

                final Source expected = Input.fromString("<echo>" + timestamp + "</echo>").build();
                final Source actual = Input.fromNode((Node) item).build();
                final Diff diff = DiffBuilder.compare(expected)
                        .withTest(actual)
                        .checkForSimilar()
                        .build();
                assertFalse(diff.toString(), diff.hasDifferences());
            }
        }
    }

    @Test
    public void importExternalClasspathLibraryModule() throws XMLDBException {
        final long timestamp = System.currentTimeMillis();
        final EXistXPathQueryService service;
        try (final Collection testCollection = getTestCollection()) {

            try(final Resource doc = testCollection.createResource("import-external-classpath.xq", BinaryResource.class)) {
                doc.setContent(
                        "import module namespace ext1 = \"http://import-external-classpath-library-module-test.com\" at \"resource:org/exist/xquery/external-classpath-library-module.xqm\";\n"
                                + "ext1:echo(" + timestamp + ")"
                );
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            service = (EXistXPathQueryService) testCollection.getService(XPathQueryService.class);
        }
        try (final EXistResourceSet result = service.executeStoredQuery("/db/test/import-external-classpath.xq")) {

            assertEquals(1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final Source expected = Input.fromString("<echo>" + timestamp + "</echo>").build();
                final Source actual = Input.fromString(resource.getContent().toString()).build();
                final Diff diff = DiffBuilder.compare(expected)
                        .withTest(actual)
                        .checkForIdentical()
                        .build();
                assertFalse(diff.toString(), diff.hasDifferences());
            }
        }
    }

    @Test
    public void doubleDocNode_2078755() throws XMLDBException {
        final XPathQueryService service;
        try (final Collection testCollection = getTestCollection()) {

            try (final Resource doc = testCollection.createResource(MODULE8_NAME, BinaryResource.class)) {
                doc.setContent(module8);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            service = testCollection.getService(XPathQueryService.class);
        }
        service.setProperty(OutputKeys.INDENT, "no");
        String query = "import module namespace dr = \"double-root2\" at \"" + URI + "/test/" + MODULE8_NAME + "\";\n"
                +"let $doc1 := dr:documentIn() \n"
                +"let $count1 := count($doc1/element()) \n"
                +"let $doc2 := dr:documentIn() \n"
                +"let $count2 := count($doc2/element()) \n"
                +"return ($count1, $count2) \n";

        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(2, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("1", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("1", resource.getContent().toString());
            }
        }
    }

    @Test
    public void globalVars() throws XMLDBException {
        final XQueryService service;
        try (final Collection testCollection = getTestCollection()) {

            try (final Resource doc = testCollection.createResource(MODULE5_NAME, BinaryResource.class)) {
                doc.setContent(module5);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            try (final Resource doc = testCollection.createResource(MODULE6_NAME, BinaryResource.class)) {
                doc.setContent(module6);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
                service = (XQueryService) testCollection.getService(XPathQueryService.class);
            }
        }
        String query = "xquery version \"1.0\";\n" + "import module namespace foo=\"foo\" at \"" + URI + "/test/" + MODULE5_NAME + "\";\n" + "$foo:bar";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(result.getSize(), 1);
            try (final Resource resource = result.getResource(0)) {
                assertEquals(resource.getContent(), "bar");
            }
        }

        query = "xquery version \"1.0\";\n" + "declare variable $local:a := 'abc';" + "$local:a";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(result.getSize(), 1);
            try (final Resource resource = result.getResource(0)) {
                assertEquals(resource.getContent(), "abc");
            }
        }

        boolean gotException = false;
        query = "xquery version \"1.0\";\n" + "import module namespace foo=\"foo\" at \"" + URI + "/test/" + MODULE6_NAME + "\";\n" + "$foo:bar";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            assertTrue("Test should generate err:XQST0049, got: " + e.getMessage(), e.getMessage().indexOf("err:XQST0049") > -1);
            gotException = true;
        }
        assertTrue("Duplicate global variable should generate error", gotException);

        gotException = false;
        query = "xquery version \"1.0\";\n" + "declare variable $local:a := 'abc';" + "declare variable $local:a := 'abc';" + "$local:a";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            assertTrue("Test should generate err:XQST0049, got: " + e.getMessage(), e.getMessage().indexOf("err:XQST0049") > -1);
            gotException = true;
        }
        assertTrue("Duplicate global variable should generate error", gotException);
    }

    @Test
    public void functionDoc() throws XMLDBException, IOException, SAXException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);
        String query = "doc('" + XmldbURI.ROOT_COLLECTION + "/test/" + NUMBERS_XML + "')";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final Node n = ((XMLResource) resource).getContentAsDOM();
                assertTrue(n instanceof Document);
                final Source expected = Input.fromString(numbers).build();
                final Source actual = Input.fromNode(n).build();

                final Diff diff = DiffBuilder.compare(expected).withTest(actual)
                        .checkForSimilar()
                        .build();
                assertFalse(diff.toString(), diff.hasDifferences());
            }
        }

        //ignore eXist namespace's attributes
        //assertEquals(1, d.getAllDifferences().size());

        query = "let $v := ()\n" + "return doc($v)";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 0, result.getSize());
        }

        query = "doc('" + XmldbURI.ROOT_COLLECTION + "/test/dummy" + NUMBERS_XML + "')";
        boolean exceptionThrown = false;
        String message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            //TODO : to be decided !
            //assertTrue(exceptionThrown);
            assertEquals(0, result.getSize());
        } catch (final XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }

        query = "doc-available('" + XmldbURI.ROOT_COLLECTION + "/test/" + NUMBERS_XML + "')";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "true", resource.getContent());
            }
        }

        query = "let $v := ()\n" + "return doc-available($v)";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "false", resource.getContent());
            }
        }

        query = "doc-available('" + XmldbURI.ROOT_COLLECTION + "/test/dummy" + NUMBERS_XML + "')";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "false", resource.getContent());
            }
        }
    }

    /**
     * This test only works if there is an Internet access
     */
    @Test
    public void functionDocExternal() throws XMLDBException {
        boolean hasInternetAccess = false;

        //Checking that we have an Internet Access
        try {
            URL url = new URL("http://www.w3.org/");
            URLConnection con = url.openConnection();
            if (con instanceof HttpURLConnection httpConnection) {
                hasInternetAccess = (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK);
            }
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            //Ignore
        }
        assumeTrue("No Internet access: skipping 'functionDocExternal' tests", hasInternetAccess);

        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);
        String query = "if (doc-available(\"http://www.w3.org/XML/Core/\")) then doc(\"http://www.w3.org/XML/Core/\") else ()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
        }

        query = "if (doc-available(\"http://www.w3.org/XML/dummy\")) then doc(\"http://www.w3.org/XML/dummy\") else ()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 0, result.getSize());
        }

        query = "doc-available(\"http://www.w3.org/XML/Core/\")";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "true", resource.getContent());
            }
        }

        query = "doc-available(\"http://www.google.com/404\")";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "false", resource.getContent());
            }
        }

        //A redirected 404
        query = "doc-available(\"http://java.sun.com/404\")";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "false", resource.getContent());
            }
        }

        query = "if (doc-available(\"file:////doesnotexist.xml\")) then doc(\"file:////doesnotexist.xml\") else ()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 0, result.getSize());
        }

        query = "doc-available(\"file:////doesnotexist.xml\")";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "false", resource.getContent());
            }
        }
    }

    private String makeString(final int n) {
        final char buf[] = new char[n];
        Arrays.fill(buf, 'a');
        return new String(buf);
    }

    @Test
    public void textConstructor() throws XMLDBException {
        String query = "text{ \"a\" }, text{ \"b\" }, text{ \"c\" }, text{ \"d\" }";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals("XQuery: " + query, 4, result.getSize());

                try (final Resource resource = result.getResource(0)) {
                    assertEquals("XQuery: " + query, "a", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals("XQuery: " + query, "b", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(2)) {
                    assertEquals("XQuery: " + query, "c", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(3)) {
                    assertEquals("XQuery: " + query, "d", resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void userEscalationForInMemoryNodes() throws XMLDBException {
        String query = "xmldb:login(\"xmldb:exist:///db\", \"guest\", \"guest\"), sm:id()/sm:id/sm:effective/sm:username/text(), let $node := <node id=\"1\">value</node>, $null := $node[@id eq '1'] return sm:id()/sm:id/sm:effective/sm:username/text()";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource loggedIn = result.getResource(0)) {
                    //check the login as guest worked
                    assertEquals("Logged in as quest: " + loggedIn.getContent().toString(), "true", loggedIn.getContent().toString());
                }

                try (final Resource currentUser = result.getResource(1)) {
                    //check that we are guest
                    assertEquals("After Login as guest, User should be guest and is: " + currentUser.getContent().toString(), "guest", currentUser.getContent().toString());
                }

                try (final Resource currentUserAfterInMemoryOp = result.getResource(2)) {
                    //check that we are still guest
                    assertEquals("After Query, User should still be guest and is: " + currentUserAfterInMemoryOp.getContent().toString(), "guest", currentUserAfterInMemoryOp.getContent().toString());
                }
            }
        }
    }

    @Test
    public void constructedAttributeValue() throws XMLDBException {
        String query = "let $attr := attribute d { \"xxx\" } " + "return string($attr)";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("xxx", resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void attributeAxis() throws XMLDBException {
        createXMLContentWithLargeString();
        final XPathQueryService service = storeXMLStringAndGetQueryService(file_name, xml);

        String query = "let $node := (<c id=\"OK\">b</c>)/descendant-or-self::*/attribute::id " +
                "return <a>{$node}</a>";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
             final XMLResource resource = (XMLResource) result.getResource(0)) {
            assertEquals("XQuery: " + query, "OK", ((Element) resource.getContentAsDOM()).getAttribute("id"));
        }
    }

    @Test
    public void instanceOfDocumentNode() throws XMLDBException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);

        String query = "let $doc := document { <element/> } " +
                "return $doc/root() instance of document-node()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, "true", resource.getContent().toString());
            }
        }
    }

    @Test
    public void instanceOfNamespaceNode() throws XMLDBException {
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("namespace test { 'test' } instance of namespace-node()")) {
             assertEquals(1, result.getSize());
             try (final Resource resource = result.getResource(0)) {
                 assertEquals("true", resource.getContent().toString());
             }
        }

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery("<x/> instance of namespace-node()")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("false", resource.getContent().toString());
            }
        }
    }

    @Test
    public void largeAttributeSimple() throws XMLDBException {
        final String large = createXMLContentWithLargeString();
        final XPathQueryService service = storeXMLStringAndGetQueryService(file_name, xml);
        final String query = "doc('" + file_name + "') / details/metadata[@docid= '" + large + "' ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(file_name, query)) {
            assertEquals("XQuery: " + query, nbElem, result.getSize());
        }
    }

    @Test
    public void cdataSerialization() throws XMLDBException {
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            final String query = "let $doc := document{ <root><![CDATA[gaga]]></root> } return $doc/root/string()";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
                 final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XQuery: " + query, "gaga", resource.getContent().toString());
            }
        }
    }

    @Test
    public void cdataQuery() throws XMLDBException {
        final String xml = "<root><node><![CDATA[world]]></node></root>";

        final XPathQueryService service = storeXMLStringAndGetQueryService("cdata.xml", xml);
        service.setProperty(OutputKeys.INDENT, "no");

        String query = "//text()";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("cdata.xml", query)) {
            assertEquals(1, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XQuery: " + query, "world", resource.getContent().toString());
            }
        }

        query = "//node/text()";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("cdata.xml", query)) {
            assertEquals(1, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XQuery: " + query, "world", resource.getContent().toString());
            }
        }

        query = "//node/node()";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("cdata.xml", query)) {
            assertEquals(1, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XQuery: " + query, "world", resource.getContent().toString());
            }
        }

        query = "/root[node = 'world']";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("cdata.xml", query)) {
            assertEquals(1, result.getSize());
            // NOTE - no cdata-section-elements specified for XDM serialization
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XQuery: " + query, "<root><node>world</node></root>", resource.getContent().toString());
            }
        }
    }

    /**
     * Tests that no result will be returned if an attribute's value is selected on a node which wasn't found
     */
    @Test
    public void attributeForNoResult() throws XMLDBException {
        final String query = "let $a := <a><b>-1</b><b>-2</b></a> " + //
                "return /a[./c]/@id/string()";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(0, result.getSize());
            }
        }
    }

    @Test
    public void largeAttributeContains() throws XMLDBException {
        createXMLContentWithLargeString();
        final XPathQueryService service = storeXMLStringAndGetQueryService(file_name, xml);

        final String query = "doc('" + file_name + "') / details/metadata[ contains(@docid, 'aa') ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(file_name, query)) {
            assertEquals("XQuery: " + query, nbElem, result.getSize());
        }
    }

    @Test
    public void largeAttributeKeywordOperator() throws XMLDBException {
        final String large = createXMLContentWithLargeString();
        final XPathQueryService service = storeXMLStringAndGetQueryService(file_name, xml);

        final String query = "doc('" + file_name + "') / details/metadata[ @docid = '" + large + "' ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(file_name, query)) {
            assertEquals("XQuery: " + query, nbElem, result.getSize());
        }
    }

    @Test
    public void attributeNamespace() throws XMLDBException {
        final String query = "declare function local:copy($nodes as node()*) as node()* {" + "for $n in $nodes return " + "if ($n instance of element()) then " + "  element {node-name($n)} {(local:copy($n/@*), local:copy($n/node()))} " + "else if ($n instance of attribute()) then " + "  attribute {node-name($n)} {$n} " + "else if ($n instance of text()) then " + "  text {$n} " + "else " + "  <Other/>" + "};" + "let $c :=" + "<c:C  xmlns:c=\"http://c\" xmlns:d=\"http://d\" d:d=\"ddd\">" + "ccc" + "</c:C>" + "return local:copy($c)";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("<c:C xmlns:c=\"http://c\" xmlns:d=\"http://d\" d:d=\"ddd\">" + "ccc" + "</c:C>", resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void nameConflicts() throws XMLDBException {
        final String query = "let $a := <name name=\"Test\"/> return <wrap>{$a//@name}</wrap>";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("<wrap name=\"Test\"/>", resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void serialization() throws XMLDBException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);

        String query = "let $a := <test><foo name='bar'/><foo name='bar'/></test>" +
                "return <attribute>{$a/foo/@name}</attribute>";
        String message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XQDY0025") > -1);

        query = "let $a := <foo name='bar'/> return $a/@name";
        message = "";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to make sure that result is closed
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        //TODO : how toserialize this resultand get the error ? -pb
        //assertTrue(message.indexOf("XQDY0025") > -1);
    }

    /** CAUTION side effect on field xml
     * @return the large string contained in the atrbute(s)
     */
    private String createXMLContentWithLargeString() {
        final String large = makeString(stringSize);
        final String head = "<details format='xml'>";
        final String elem = "<metadata docid='" + large + "'></metadata>";
        final String tail = "</details>";
        xml = head;
        for (int i = 0; i < nbElem; i++) {
            xml += elem;
        }
        xml += tail;
        return large;
    }

    @Test
    public void retrieveLargeAttribute() throws XMLDBException {
        createXMLContentWithLargeString();
        storeXMLStringAndGetQueryService(file_name, xml);
        try (final Collection testCollection = getTestCollection();
             final XMLResource res = (XMLResource) testCollection.getResource(file_name)) {
            assertNotNull(res);
        }
    }

    @Test
    public void largeAttributeText() throws XMLDBException {
        final String large = "challengesininformationretrievalandlanguagemodelingreportofaworkshopheldatthecenterforintelligentinformationretrievaluniversityofmassachusettsamherstseptember2002-extdocid-howardturtlemarksandersonnorbertfuhralansmeatonjayaslamdragomirradevwesselkraaijellenvoorheesamitsinghaldonnaharmanjaypontejamiecallannicholasbelkinjohnlaffertylizliddyronirosenfeldvictorlavrenkodavidjharperrichschwartzjohnpragerchengxiangzhaijinxixusalimroukosstephenrobertsonandrewmccallumbrucecroftrmanmathasuedumaisdjoerdhiemstraeduardhovyralphweischedelthomashofmannjamesallanchrisbuckleyphilipresnikdavidlewis2003";
        String xml = "<details format='xml'><metadata docid='" + large +
                "'></metadata></details>";
        final String FILE_NAME = "detail_xml.xml";
        XPathQueryService service = storeXMLStringAndGetQueryService(FILE_NAME, xml);

        String query = "doc('" + FILE_NAME + "') / details/metadata[@docid= '" + large + "' ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(FILE_NAME, query)) {
            assertEquals(1, result.getSize());
        }

        xml = "<details format='xml'><metadata><docid>" + large +
                "</docid></metadata></details>";
        service = storeXMLStringAndGetQueryService(FILE_NAME, xml);

        query = "doc('"+ FILE_NAME+"') / details/metadata[ docid= '" + large + "' ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(FILE_NAME, query)) {
            assertEquals(1, result.getSize());
        }
    }

    @Ignore
    @Test
    public void xupdateWithAdjacentTextNodes() throws XMLDBException {
        final String query = "let $name := xmldb:store('/db' , 'xupdateTest.xml', <test>aaa</test>)" +
                "let $xu :=" +
                "<xu:modifications xmlns:xu='http://www.xmldb.org/xupdate' version='1.0'>" +
                "<xu:append select='/test'>" +
                "<xu:text>yyy</xu:text>" +
                "</xu:append>" +
                "</xu:modifications>" +
                "let $count := xmldb:update('/db' , $xu)" +
                "for $textNode in doc('/db/xupdateTest.xml')/test/text()" +
                "	return <text id='{util:node-id($textNode)}'>{$textNode}</text>";

        final XPathQueryService service = storeXMLStringAndGetQueryService(NUMBERS_XML, numbers);
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
        }
    }

    //TODO : understand this test and make sure that the expected result is correct
    //expected:<3> but was:<2>
    @Ignore
    @Test
    public void xupdateAttributesAndElements() throws XMLDBException {
        final String query =
                "declare function local:update-game($game) {\n" +
                "local:update-frames($game),\n" +
                "update insert\n" +
                "<stats>\n" +
                "<strikes>4</strikes>\n" +
                "<spares>\n" +
                "<attempted>4</attempted>\n" +
                "</spares>\n" +
                "</stats>\n" +
                "into $game\n" +
                "};\n" +
                "declare function local:update-frames($game) {\n" +
                // Uncomment this, and it works:
                //"for $frame in $game/frame return update insert <processed/> into $frame,\n" +
                "for $frame in $game/frame\n" +
                "return update insert attribute points {4} into $frame\n" +
                "};\n" +
                "let $series := doc('bowling.xml')/series\n" +
                "let $nul1 := for $game in $series/game return local:update-game($game)\n" +
                "return $series/game/stats\n";

        final XPathQueryService service = storeXMLStringAndGetQueryService(BOWLING_XML, bowling);
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 3, result.getSize());
        }
    }

    @Test
    public void nodeName() throws XMLDBException {
        final String query = "declare function local:name($node as node()) as xs:string? { " + " if ($node/self::element() != '') then name($node) else () }; " + " let $n := <!-- Just a comment! --> return local:name($n) ";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals("XQuery: " + query, 0, result.getSize());
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1691112&group_id=17691&atid=117691">http://sourceforge.net/tracker/index.php?func=detail&aid=1691112&group_id=17691&atid=117691</a>
     */
    //DWES Funny in sandbox and REST it fails ; here it is OK... sometimes
    @Test
    public void order_1691112() throws XMLDBException {
        final String query = "declare namespace tt = \"http://example.com\";" +
                "declare function tt:function( $function as element(Function)) {" +
                "  let $functions :=" +
                "    for $subfunction in $function/Function" +
                "    return tt:function($subfunction)" +
                "   let $unused := distinct-values($functions/NonExistingElement)" +
                "  return" +
                "  <Function>" +
                "  {" +
                "    $function/Name," +
                "    $functions" +
                "  }" +
                "  </Function>" +
                "};" +
                "let $funcs :=" +
                "  <Function>" +
                "      <Name>Airmount 1</Name>" +
                "      <Function>" +
                "          <Name>Position</Name>" +
                "      </Function>" +
                "      <Function>" +
                "          <Name>Velocity</Name>" +
                "      </Function>" +
                "  </Function>" +
                "return" +
                "  tt:function($funcs)";

        final String expectedresult =
                "<Function>\n" +
                "    <Name>Airmount 1</Name>\n" +
                "    <Function>\n" +
                "        <Name>Position</Name>\n" +
                "    </Function>\n" +
                "    <Function>\n" +
                "        <Name>Velocity</Name>\n" +
                "    </Function>\n" +
                "</Function>";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            for (int i = 0; i < 25; i++) { // repeat a few times
                try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                    assertEquals(1, result.getSize());
                    try (final Resource resource = result.getResource(0)) {
                        assertEquals(expectedresult, resource.getContent().toString());
                    }
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1691177&group_id=17691&atid=117691">http://sourceforge.net/tracker/index.php?func=detail&aid=1691177&group_id=17691&atid=117691</a>
     */
    @Test
    public void attribute_1691177() throws XMLDBException {
        final String query = "declare namespace xmldb = \"http://exist-db.org/xquery/xmldb\"; " + "let $uri := xmldb:store(\"/db\", \"insertAttribDoc.xml\", <C/>) " + "let $node := doc($uri)/element() " + "let $attrib := <Value f=\"ATTRIB VALUE\"/>/@* " + "return update insert $attrib into $node  ";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals("XQuery: " + query, 0, result.getSize());
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1691174&group_id=17691&atid=117691">http://sourceforge.net/tracker/index.php?func=detail&aid=1691174&group_id=17691&atid=117691</a>
     */
    @Test
    public void attribute_1691174() throws XMLDBException {
        final String query = "declare function local:show($el1, $el2) { "
                + "	<Foobar> "
                + "	{ (\"first: \", $el1, \" second: \", $el2) } "
                + "	</Foobar> " + "}; "
                + "declare function local:attrib($n as node()) { "
                + "	<Attrib>{$n}</Attrib> "
                + "}; "
                + "local:show( "
                + "	<Attrib name=\"value\"/>, "
                + "	local:attrib(attribute name {\"value\"})  (: Exist bug! :) "
                + ")  ";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals("XQuery: " + query, 1, result.getSize());
            }
        }
    }

    @Test
    public void qnameToString_1632365() throws XMLDBException {
        final String query = "let $qname := QName(\"http://test.org\", \"test:name\") " +
                "return xs:string($qname)";
        final String expectedresult = "test:name";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(expectedresult, resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void comments_1715035() throws XMLDBException {
        String query = "<!-- < aa > -->";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, resource.getContent().toString());
                }
            }

            query = "<?pi \"<\"aa\">\"?>";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void documentNode_1730690() throws XMLDBException {
        final String query = "let $doc := document { <element/> } " +
                "return $doc/root() instance of document-node()";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("true", resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void enclosedExpressions() throws XMLDBException, IOException, SAXException {
        final String query = "let $a := <docum><titolo>titolo</titolo><autor>giulio</autor></docum> " +
                "return <row>{$a/titolo/text()} {' '} {$a/autor/text()}</row>";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource resource = result.getResource(0)) {
                    assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<row>titolo giulio</row>"));
                }
            }
        }
    }

    @Test
    public void orderCompareAtomicType_1733265() throws XMLDBException {
        String query = "( ) = \"A\"";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("false", resource.getContent().toString());
                }
            }

            query = "\"A\" = ( )";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("false", resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void positionInPredicate() throws XMLDBException {
        String query = "let $example := <Root> <Element>1</Element> <Element>2</Element> </Root>" +
                "return  $example/Element[1] ";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("<Element>1</Element>", resource.getContent().toString());
                }
            }

            query = "let $example := <Root> <Element>1</Element> <Element>2</Element> </Root>" +
                    "return  $example/Element[position() = 1] ";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("<Element>1</Element>", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1740880">http://sourceforge.net/support/tracker.php?aid=1740880</a>
     */
    @Test
    public void elementConstructionWithNamespace_1740880() throws XMLDBException {
        final String query = "let $a := <foo:Bar xmlns:foo=\"urn:foo\"/> " +
                "let $b := element { QName(\"urn:foo\", \"foo:Bar\") } { () } " +
                "return deep-equal($a, $b) ";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("true", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * http://sourceforge.net/support/tracker.php?aid=1740883
     */
    @Test
    public void noErrorNeOperatorWithSequence_1740883() {
        try {
            final String query = "let $foo := <Foo> <Bar>A</Bar> <Bar>B</Bar> <Bar>C</Bar> </Foo> " +
                    "return $foo[Bar ne \"B\"]";

            try (final Collection testCollection = getTestCollection()) {
                final XPathQueryService service = testCollection.getService(XPathQueryService.class);

                try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                    // needed to ensure that result is closed
                }

                fail("result should have yielded into an error like " +
                        "'A sequence of more than one item is not allowed as the first " + "operand of 'ne'");
            }
        } catch (XMLDBException e) {
            if (!e.getMessage().contains("one item")) {
                LOG.error(e.getMessage(), e);
                fail(e.getMessage());
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1740885">http://sourceforge.net/support/tracker.php?aid=1740885</a>
     */
    @Test
    public void neOperatorDoesNotWork_1740885() throws XMLDBException {
        final String query = "let $foo := <Foo> <Bar>A</Bar> <Bar>B</Bar> <Bar>C</Bar> </Foo> return $foo/Bar[. ne \"B\"]";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(2, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("First", "<Bar>A</Bar>", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals("Second", "<Bar>C</Bar>", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1740891">http://sourceforge.net/support/tracker.php?aid=1740891</a>
     */
    @Test
    public void evalLoosesContext_1740891() throws XMLDBException {
        final String module = "module namespace tst = \"urn:test\"; " +
                "declare namespace util = \"http://exist-db.org/xquery/util\";" +
                "declare function tst:bar() as element(Bar)* { " +
                "let $foo := <Foo><Bar/><Bar/><Bar/></Foo> " +
                "let $query := \"$foo/Bar\" " +
                "let $bar := util:eval($query) " +
                "return $bar };";

        final String module_name = "module.xqy";

        // Store module
        final XPathQueryService service;
        try (final Collection testCollection = getTestCollection()) {

            try (final Resource doc = testCollection.createResource(module_name, BinaryResource.class)) {
                doc.setContent(module);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);
            }

            service = testCollection.getService(XPathQueryService.class);
        }

        final String query = "import module namespace tst = \"urn:test\"" +
                "at \"xmldb:exist:///db/test/module.xqy\"; " +
                "tst:bar()";

        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(3, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("First", "<Bar/>", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("Second", "<Bar/>", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(2)) {
                assertEquals("Third", "<Bar/>", resource.getContent().toString());
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1740886">http://sourceforge.net/support/tracker.php?aid=1740886</a>
     */
    @Test
    public void cardinalityIssues_1740886() throws XMLDBException, IOException, SAXException {
        final String xmldoc = "<Foo><Bar/><Bar/><Bar/></Foo>";
        final String query =
                "declare namespace tst = \"urn:test\"; " +
                "declare option exist:serialize 'indent=no';" +
                //======
                "declare function tst:bar( $foo as element(Foo) ) as element(Foo) { " +
                "let $dummy := $foo/Bar " +
                "return $foo }; " +
                //====== if you leave /test out......
                "let $foo := doc(\"/db/test/foo.xml\")/element() " +
                "return tst:bar($foo)";

        final XPathQueryService service = storeXMLStringAndGetQueryService("foo.xml", xmldoc);
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo(xmldoc));
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1755910">http://sourceforge.net/support/tracker.php?aid=1755910</a>
     */
    @Test
    public void qnameString_1755910() throws XMLDBException {
        final String query = "let $qname1 := QName(\"http://www.w3.org/2001/XMLSchema\", \"xs:element\") " + "let $qname2 := QName(\"http://foo.com\", \"foo:bar\") " + "return (xs:string($qname1), xs:string($qname2))";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(2, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("First", "xs:element", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals("Second", "foo:bar", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1665215">http://sourceforge.net/support/tracker.php?aid=1665215</a>
     */
    @Test
    public void predicateMinLast_1665215() throws XMLDBException {
        final String query = "declare option exist:serialize 'indent=no';" +
                "let $data :=<parent><child>1</child><child>2</child><child>3</child><child>4</child></parent>" +
                "return <result>{$data/child[min((last(),3))]}</result>";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("First", "<result><child>3</child></result>", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1665213">http://sourceforge.net/support/tracker.php?aid=1665213</a>
     */
    @Test
    public void predicatePositionLast_1665213() throws XMLDBException {
        // OK, regression
        String query = "(1, 2, 3)[ position() = last() ]";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("First", "3", resource.getContent().toString());
                }
            }


            query = "(1, 2, 3)[(position()=last() and position() < 4)]";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("First", "3", resource.getContent().toString());
                }
            }

            query = "(1, 2, 3)[(position()=last())]";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("First", "3", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1769086">http://sourceforge.net/support/tracker.php?aid=1769086</a>
     */
    @Test
    public void cce_IndexOf_1769086() throws XMLDBException {
        final String query = "(\"One\", \"Two\", \"Three\")[index-of((\"1\", \"2\", \"3\"), \"2\")]";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("First", "Two", resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void shortVersionPositionPredicate() throws XMLDBException {
        final String query = "declare option exist:serialize 'indent=no';" + "let $foo :=  <foo>    <bar baz=\"\"/>  </foo>" + "let $bar1 := $foo/bar[exists(@baz)][1]" + "let $bar2 := $foo/bar[exists(@baz)][position() = 1]" + "return  <found> <bar1>{$bar1}</bar1> <bar2>{$bar2}</bar2> </found>";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<found><bar1><bar baz=\"\"/></bar1><bar2><bar baz=\"\"/></bar2></found>", resource.getContent().toString());
                }
            }
        }
    }

    /***
     * An exception occurred during query execution: XPTY0004: Invalid type for
     * variable $arg1. Expected xs:string, got xs:integer
     *
     * <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1787285&group_id=17691&atid=117691">http://sourceforge.net/tracker/index.php?func=detail&aid=1787285&group_id=17691&atid=117691</a>
     */
    @Test
    public void wrongInvalidTypeError_1787285() throws XMLDBException {
        final String query = "let $arg1 as xs:string := \"A String\"" + "let $arg2 as xs:integer := 3 return $arg2";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "3", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * Regression
     *
     * <a href="http://sourceforge.net/support/tracker.php?aid=1805612">http://sourceforge.net/support/tracker.php?aid=1805612</a>
     *
     * Same as {@link #asDouble_1840775()}
     */
    @Ignore
    @Test
    public void wrongAttributeTypeCheck_1805612() throws XMLDBException {
        // OK
        String query = "declare namespace tst = \"http://test\"; "
                + "declare function tst:foo($a as element()?) {   $a }; "
                + "tst:foo( <result/> )";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<result/>", resource.getContent().toString());
                }
            }

            // NOK
            query = "declare namespace tst = \"http://test\"; "
                    + "declare function tst:foo($a as element()?) {   $a }; "
                    + "tst:foo( "
                    + "  let $a as xs:boolean := true()  "
                    + "  return <result/> "
                    + ")";

            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<result/>", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * Regression
     *
     * <a href="http://sourceforge.net/support/tracker.php?aid=1805609">http://sourceforge.net/support/tracker.php?aid=1805609</a>
     */
    @Test
    public void wrongAttributeCardinalityCount_1805609() throws XMLDBException {

        // OK
        String query = "element {\"a\"} { <element b=\"\" c=\"\" />/attribute()[namespace-uri(.) != " + "\"http://www.asml.com/metainformation\"]}";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<a b=\"\" c=\"\"/>", resource.getContent().toString());
                }
            }

            // NOK
            query = "element {\"a\"} { <element b=\"\" c=\"\"/>" + "/attribute()[namespace-uri(.) != \"http://www.asml.com/metainformation\"]}";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<a b=\"\" c=\"\"/>", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * Regression
     *
     * <a href="http://sourceforge.net/support/tracker.php?aid=1806901">http://sourceforge.net/support/tracker.php?aid=1806901</a>
     */
    @Test
    public void doubleDefaultNamespace_1806901() throws XMLDBException {
        // OK
        final String query = "declare namespace xf = \"http://a\"; " + "declare option exist:serialize 'indent=no';" + "<html xmlns=\"http://b\"><xf:model><xf:instance xmlns=\"\"/></xf:model></html>";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<html xmlns=\"http://b\"><xf:model xmlns:xf=\"http://a\">" + "<xf:instance xmlns=\"\"/></xf:model></html>",
                            resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1828168">http://sourceforge.net/support/tracker.php?aid=1828168</a>
     */
    @Test
    public void predicateInPredicateEmptyResult_1828168() throws XMLDBException {
        String query = "let $docs := <Document/> return $docs[a[1] = 'b']";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(0, result.getSize());
            }

            query = "<a/>[() = 'b']";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(0, result.getSize());
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1846228">http://sourceforge.net/support/tracker.php?aid=1846228</a>
     */
    @Test
    public void namespaceHandlingSameModule_1846228() throws XMLDBException {
        final String query = "declare option exist:serialize 'indent=no';" +
                "declare function local:table () {" +
                "<d>Bar</d>};" +
                "<foobar xmlns=\"http://www.w3.org/1999/xhtml\">" +
                "<a><b>Foo</b></a>" +
                "<c>{local:table()}</c>" +
                "</foobar>";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query,
                            "<foobar xmlns=\"http://www.w3.org/1999/xhtml\">" +
                                    "<a><b>Foo</b></a>" +
                                    "<c><d xmlns=\"\">Bar</d></c>" +
                                    "</foobar>", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * In a path expression, a step returning an empty sequence stops the evaluation
     * (and return an empty sequence) as confirmed by Michael Kay on the XQuery mailing list
     *
     * @see <a href="http://sourceforge.net/support/tracker.php?aid=1841105">http://sourceforge.net/support/tracker.php?aid=1841105</a>
     */
    @Test
    public void stringOfEmptySequence_1841105() throws XMLDBException {
        // OK
        final String query = "empty( ()/string() )";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "true", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=2871975">http://sourceforge.net/support/tracker.php?aid=2871975</a>
     */
    @Ignore
    @Test
    public void stringOfEmptySequenceWithExplicitContext_2871975() throws XMLDBException {
        // OK
        String query = "empty( ()/string() )";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "true", resource.getContent().toString());
                }
            }

            // NOK
            query = "empty( ()/string(.) )";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "true", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1970717">http://sourceforge.net/support/tracker.php?aid=1970717</a>
     */
    @Test
    public void constructTextNodeWithEmptyString_1970717() throws XMLDBException {
        final String query = "text {\"\"} =\"\"";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "true", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1848497">http://sourceforge.net/support/tracker.php?aid=1848497</a>
     */
    @Ignore
    @Test
    public void attributeNamespaceDeclaration_1848497() throws XMLDBException {
        final String query = "declare namespace foo = \"foo\";" +
                "declare function foo:boe() { \"boe\" };" +
                "<xml xmlns:foo2=\"foo\">{ foo2:boe() }</xml>";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<xml xmlns:foo2=\"foo\">boe</xml>", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1884403">http://sourceforge.net/support/tracker.php?aid=1884403</a>
     */
    @Test
    public void atomization_1884403() throws XMLDBException {
        final String query = "declare namespace tst = \"tt\"; " +
                "declare function tst:foo() as xs:string { <string>myTxt</string> }; " +
                "tst:foo()";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "myTxt", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1884360">http://sourceforge.net/support/tracker.php?aid=1884360</a>
     */
    @Test
    public void cardinalityAttributeNamespace_1884360() throws XMLDBException {
        final String query = "let $el := <element a=\"1\" b=\"2\"/> " +
                "for $attr in $el/attribute()[namespace-uri(.) ne \"h\"] " +
                "return <c>{$attr}</c>";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(2, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<c a=\"1\"/>", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals(query, "<c b=\"2\"/>", resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void currentDateTimeInModules_1894009() throws XMLDBException {
        final String module = "module namespace dt = \"dt\";\n" +
                "\n" +
                "declare function dt:fib($n) {\n" +
                "  if ($n < 2) then $n else dt:fib($n - 1) + dt:fib($n - 2) \n" +
                "};\n" +
                "\n" +
                "declare function dt:dateTime() {\n" +
                "  (: Do something time consuming first. :)  \n" +
                "  let $a := dt:fib(25)" +
                "  return current-dateTime()\n" +
                "};";

        final String module_name = "dt.xqm";

        // Store module
        final XPathQueryService service;
        try (final Collection testCollection = getTestCollection()) {

            try (final Resource doc = testCollection.createResource(module_name, BinaryResource.class)) {
                doc.setContent(module);
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);

                service = testCollection.getService(XPathQueryService.class);
            }
        }

        final String query = "import module namespace dt = \"dt\" at" +
                "  \"xmldb:exist:///db/test/dt.xqm\"; " +
                "(<this>{current-dateTime()}</this>, <this>{dt:dateTime()}</this>)";

        try (final EXistResourceSet result = (EXistResourceSet) service.query(query);
             final Resource resource0 = result.getResource(0);
             final Resource resource1 = result.getResource(1)) {
            assertEquals(2, result.getSize());
            assertEquals("First", resource0.getContent().toString(), resource1.getContent().toString());
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1909505">http://sourceforge.net/support/tracker.php?aid=1909505</a>
     */
    @Test
    public void testXmldbStoreComment_1909505() throws XMLDBException {
        final String query = "declare option exist:serialize 'indent=no';" +
                "let $docIn := <a><!-- b --></a>" +
                "let $uri := xmldb:store(\"/db\", \"commenttest.xml\", $docIn)" +
                "let $docOut := doc($uri) return $docOut";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<a><!-- b --></a>", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1938498">http://sourceforge.net/support/tracker.php?aid=1938498</a>
     */
    @Test
    public void memproc_1938498() throws XMLDBException {
        final String xmldocument = "<Root><Child/></Root>";
        final String location = "1938498.xml";
        final String query = "let $test := doc(\"1938498.xml\")" + "let $inmems := <InMem>{$test}</InMem>" + "return <Test>{$inmems/X}</Test>";
        final String output = "<Test/>";

        final XPathQueryService service = storeXMLStringAndGetQueryService(location, xmldocument);

        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, output, resource.getContent().toString());
            }
        }
    }

    @Test
    public void cce_SaxException() throws XMLDBException {
        final String xmldocument = "<a><b><c>mmm</c></b></a>";
        final String location = "ccesax.xml";
        final String query =
                "declare namespace xmldb = \"http://exist-db.org/xquery/xmldb\"; "
                + "declare option exist:serialize 'indent=no';"
                + "let $results := doc(\"ccesax.xml\")/element() "
                + "let $output := let $body := <e>{$results/b/c}</e>  return <d>{$body}</d> "
                + "let $id := $output/e/c "
                + "let $store := xmldb:store(\"/db\", \"output.xml\", $output)"
                + "return doc('/db/output.xml')";
//            String output = "<d><b><c>mmm</c></b></d>";
        final String output = "<d><e><c>mmm</c></e></d>";

        final XPathQueryService service = storeXMLStringAndGetQueryService(location, xmldocument);

        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("XQuery: " + query, output, resource.getContent().toString());
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=2003042">http://sourceforge.net/support/tracker.php?aid=2003042</a>
     */
    @Test
    public void xpty0018_MixNodesAtomicValues_2003042() throws XMLDBException {
        final String query = "declare option exist:serialize 'indent=no'; <a>{2}<b/></a>";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                //checked with saxon
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<a>2<b/></a>", resource.getContent().toString());
                }
            }
        }
    }
    
    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1816496">http://sourceforge.net/support/tracker.php?aid=1816496</a>
     */
    @Test
    public void divYieldsWrongInf_1816496() throws XMLDBException {
        String query = "let $negativeZero := xs:double(-1.0e-1024) let $positiveZero := xs:double(1.0e-1024) "
                +"return ("
                +"(xs:double(1)  div xs:double(0)),   (xs:double(1)  div $positiveZero),  (xs:double(1)  div $negativeZero), "
                +"(xs:double(-1) div xs:double(0)),   (xs:double(-1) div $positiveZero),  (xs:double(-1) div $negativeZero), "
                +"($negativeZero div $positiveZero),  ($positiveZero div $negativeZero), "
                +"(xs:double(0) div $positiveZero),   (xs:double(0) div $negativeZero),  "
                +"(xs:double(0) div xs:double(0))  "
                +")";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(11, result.getSize());

                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "INF", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals(query, "INF", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(2)) {
                    assertEquals(query, "-INF", resource.getContent().toString());
                }

                try (final Resource resource = result.getResource(3)) {
                    assertEquals(query, "-INF", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(4)) {
                    assertEquals(query, "-INF", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(5)) {
                    assertEquals(query, "INF", resource.getContent().toString());
                }

                try (final Resource resource = result.getResource(6)) {
                    assertEquals(query, "NaN", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(7)) {
                    assertEquals(query, "NaN", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(8)) {
                    assertEquals(query, "NaN", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(9)) {
                    assertEquals(query, "NaN", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(10)) {
                    assertEquals(query, "NaN", resource.getContent().toString());
                }
            }

            query = "xs:float(2) div xs:float(0)";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "INF",
                            resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="https://github.com/eXist-db/exist/issues/3441">https://github.com/eXist-db/exist/issues/3441</a>
     */
    @Test
    public void divErrorArgVariable() throws XMLDBException {
        final String query = "let $x := 2 " +
                "return 1 div $x * 4";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "2", resource.getContent().toString());
                }
            }
        }
    }

    /**
            * <a href="https://github.com/eXist-db/exist/issues/3441">https://github.com/eXist-db/exist/issues/3441</a>
            */
    @Test
    public void divErrorArgVariable2() throws XMLDBException {
        String query = "let $x := 2 \n" +
                "let $y := 1 div $x * 4\n" +
                "return $y";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "2", resource.getContent().toString());
                }
            }
        }
    }
    
    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1841635">http://sourceforge.net/support/tracker.php?aid=1841635</a>
     */
    @Test
    public void resolveBaseURI_1841635() throws XMLDBException {
        final String xmldoc = "<Root><Node1><Node2><Node3></Node3></Node2></Node1></Root>";

        final XPathQueryService service = storeXMLStringAndGetQueryService("baseuri.xml", xmldoc);
            
        String query = "doc('/db/test/baseuri.xml')/Root/Node1/base-uri()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("xmldb:exist:///db/test/baseuri.xml", resource.getContent().toString());
            }
        }

        query = "doc('/db/test/baseuri.xml')/Root/Node1/base-uri()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("xmldb:exist:///db/test/baseuri.xml", resource.getContent().toString());
            }
        }

        query = "doc('/db/test/baseuri.xml')/Root/Node1/Node2/base-uri()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("xmldb:exist:///db/test/baseuri.xml", resource.getContent().toString());
            }
        }

        query = "doc('/db/test/baseuri.xml')/Root/Node1/Node2/Node3/base-uri()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("xmldb:exist:///db/test/baseuri.xml", resource.getContent().toString());
            }
        }
    }

    /**
     * @see <a href="https://github.com/eXist-db/exist/issues/3497">[BUG] ()/fn:base-uri() incorrectly raises XPDY0002</a>
     */
    @Test
    public void resolveBaseURIErrorCases() throws XMLDBException {
        final XPathQueryService service = existEmbeddedServer.getRoot().getService(XPathQueryService.class);

        String query = "()/fn:base-uri()";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(0, result.getSize());
        }

        query = "()/base-uri(.)";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(0, result.getSize());
        }

        query = "base-uri(.)";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(0, result.getSize());

            fail("Should have raised error: XPDY0002");

        } catch (final XMLDBException e) {
            assertEquals(org.xmldb.api.base.ErrorCodes.VENDOR_ERROR, e.errorCode);
            final Throwable cause = e.getCause();
            assertEquals(XPathException.class, cause.getClass());
            assertEquals(ErrorCodes.XPDY0002, ((XPathException) cause).getErrorCode());
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=2429093">http://sourceforge.net/support/tracker.php?aid=2429093</a>
     */
    @Test
    public void xpty0018_mixedsequences_2429093() throws XMLDBException {
        String query = "declare variable $a := <A><B/></A>;\n" +
                "($a/B, \"delete\") ";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(2, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<B/>", resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals(query, "delete", resource.getContent().toString());
                }
            }
        }
    }

    @Test
    public void messageDigester() throws XMLDBException {
        String query = "let $value:=\"ABCDEF\"\n" +
                "let $alg:=\"MD5\"\n" +
                "return\n" +
                "(util:hash($value, $alg), util:hash($value, $alg, xs:boolean('true')))";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(2, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "8827a41122a5028b9808c7bf84b9fcf6",
                            resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals(query, "iCekESKlAouYCMe/hLn89g==",
                            resource.getContent().toString());
                }
            }

            query = "let $value:=\"ABCDEF\"\n" +
                    "let $alg:=\"SHA-1\"\n" +
                    "return\n" +
                    "(util:hash($value, $alg), util:hash($value, $alg, xs:boolean('true')))";

            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(2, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "970093678b182127f60bb51b8af2c94d539eca3a",
                            resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals(query, "lwCTZ4sYISf2C7UbivLJTVOeyjo=",
                            resource.getContent().toString());
                }
            }

            query = "let $value:=\"ABCDEF\"\n" +
                    "let $alg:=\"SHA-256\"\n" +
                    "return\n" +
                    "(util:hash($value, $alg), util:hash($value, $alg, xs:boolean('true')))";

            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(2, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "e9c0f8b575cbfcb42ab3b78ecc87efa3b011d9a5d10b09fa4e96f240bf6a82f5",
                            resource.getContent().toString());
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals(query, "6cD4tXXL/LQqs7eOzIfvo7AR2aXRCwn6TpbyQL9qgvU=",
                            resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/tracker/?func=detail&aid=2846187&group_id=17691&atid=317691">http://sourceforge.net/tracker/?func=detail&aid=2846187&group_id=17691&atid=317691</a>
     */
    @Test
    public void dynamicallySizedNamePool() throws XMLDBException {
        final String query = "<root> { for $i in 1 to 2000  "
                + "return element {concat(\"elt-\", $i)} {} } </root>";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                // needed to ensure that result is closed
            }
        }
    }


    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=2903815">http://sourceforge.net/support/tracker.php?aid=2903815</a>
     */
    @Test
    public void replaceBug_2903815() throws XMLDBException {
        String query = "let $f := <z>fred</z>" +
                "let $s:= <s>xxxxtxxx</s>" +
                "let $t := <t>t</t>" +
                "return replace($s,$t,$f)";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "xxxxfredxxx",
                            resource.getContent().toString());
                }
            }

            query = "let $f := \"fred\"" +
                    "let $s:= <s>xxxxtxxx</s>" +
                    "let $t := <t>t</t>" +
                    "return replace($s,$t,$f)";
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "xxxxfredxxx", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1840775">http://sourceforge.net/support/tracker.php?aid=1840775</a>
     *
     * Same as {@link #wrongAttributeTypeCheck_1805612()}
     */
    @Ignore
    @Test
    public void asDouble_1840775() throws XMLDBException {
        final String query = "declare function local:testCase($failure as element(Failure)?)"
                + "as element(TestCase) { <TestCase/> };"
                + "local:testCase("
                + "(: work-around for this eXist 1.1.2dev-rev:6992-20071127 bug: let $ltValue := 0.0 :)"
                + "let $ltValue as xs:double := 0.0e0 return <Failure/>)";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                // needed to ensure that result is closed
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=2117655">http://sourceforge.net/support/tracker.php?aid=2117655</a>
     */
    @Test
    public void typeMismatch_2117655() throws XMLDBException {
        final String query = "declare namespace t = \"test\"; "
                +"declare function t:foo() as xs:string{"
                + "<Value>23</Value>}; "
                + "t:foo()";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "23", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1959010">http://sourceforge.net/support/tracker.php?aid=1959010</a>
     */
    @Test
    public void noNamepaceDefinedForPrefix_1959010() throws XMLDBException {
        final String query =
                 "declare function local:copy($nodes as node()*) as node()* "
                +"{ "
                +"for $n in $nodes "
                +"return "
                +"   if ($n instance of element()) then "
                +"       element {node-name($n)} {(local:copy($n/@*), local:copy($n/node()))} "
                +"   else if ($n instance of attribute()) then "
                +"       attribute {node-name($n)} {$n} "
                +"   else if ($n instance of text()) then "
                +"       text {$n} "
                +"   else "
                +"       <Other/> "
                +"}; "

                +"let $c := <c:C xmlns:c=\"http://c\" xmlns:d=\"http://d\" d:d=\"ddd\">ccc</c:C> "
                +"return local:copy($c)";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "<c:C xmlns:c=\"http://c\" xmlns:d=\"http://d\" d:d=\"ddd\">ccc</c:C>", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1807014">http://sourceforge.net/support/tracker.php?aid=1807014</a>
     */
    @Test
    public void wrongAddNamespace_1807014() throws XMLDBException {
        final XPathQueryService service;
        try (final Collection testCollection = getTestCollection()) {

            try (final Resource doc = testCollection.createResource("a.xqy", BinaryResource.class)) {
                doc.setContent("module namespace a = \"http://www.a.com\"; "
                        + "declare function a:selectionList() as element(ul) { "
                        + "<ul class=\"a\"/> "
                        + "};");
                ((EXistResource) doc).setMediaType(MediaType.APPLICATION_XQUERY);
                testCollection.storeResource(doc);

                service = testCollection.getService(XPathQueryService.class);
            }
        }

        final String query =
                "declare option exist:serialize 'indent=no';"
                        + "import module namespace a = \"http://www.a.com\" at \"xmldb:exist://db/test/a.xqy\"; "
                        + "<html xmlns=\"http://www.w3.org/1999/xhtml\"> "
                        + "{ a:selectionList() } "
                        + "</html>";

        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals(query, "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                        + "<ul xmlns=\"\" class=\"a\"/></html>",
                    resource.getContent().toString());
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1789370">http://sourceforge.net/support/tracker.php?aid=1789370</a>
     */
    @Test
    public void orderBy_1789370() throws XMLDBException {
        final String query =
                 "(for $vi in <elem>text</elem> order by $vi return $vi)/text()";
        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, "text", resource.getContent().toString());
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1817822">http://sourceforge.net/support/tracker.php?aid=1817822</a>
     */
    @Test
    public void variableScopeBug_1817822() throws XMLDBException {
        final String query =
                     "declare namespace test = \"http://example.com\"; "
                    +"declare function test:expression($expr) as xs:double? { "
                    +" typeswitch($expr) "
                    +"   case element(Value) return test:value($expr) "
                    +"   case element(SomethingRandom) return test:product($expr/*) "
                    +"   default return () "
                    +"}; "

                    +"declare function test:value($expr) { "
                    +"   xs:double($expr) "
                    +"}; "

                    +"declare function test:product($expressions) { "
                    +"   test:expression($expressions[1]) "
                    +"   * "
                    +"   test:expression($expressions[2]) "
                    +"}; "

                    +"let $values := (<Value>2</Value>,<Value>3</Value>) "
                    +"let $a := test:expression(<AnotherSomethingRandom/>) "
                    +"let $b := test:product($values) "
                    +"return <Result>{$b}</Result>";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, resource.getContent().toString(), "<Result>6</Result>");
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1718626">http://sourceforge.net/support/tracker.php?aid=1718626</a>
     */
    @Test
    public void constructednodePosition_1718626() throws XMLDBException {
        final String query =
                 "declare variable $categories := "
                +" <categories> "
                +"         <category uid=\"1\">Fruit</category> "
                +"         <category uid=\"2\">Vegetable</category> "
                +"         <category uid=\"3\">Meat</category> "
                +"         <category uid=\"4\">Dairy</category> "
                +" </categories> "
                +" ; "

                +" $categories/category[1], "
                +" $categories/category[position() eq 1]";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(2, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, resource.getContent().toString(), "<category uid=\"1\">Fruit</category>");
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals(query, resource.getContent().toString(), "<category uid=\"1\">Fruit</category>");
                }
            }
        }
    }

    /**
     * <a href="http://sourceforge.net/support/tracker.php?aid=1460791">http://sourceforge.net/support/tracker.php?aid=1460791</a>
     */
    @Test
    public void descendantOrSelf_1460791() throws XMLDBException {
        final String query =
                 "declare option exist:serialize 'indent=no';"
                +"let $test:=<z> <a> aaa </a> <z> zzz </z> </z> "
                +"return "
                +"( "
                +"<one> {$test//z} </one>, "
                +"<two> {$test/descendant-or-self::node()/child::z} </two> "
                +"(: note that these should be the same *by definition* :) "
                +")";

        try (final Collection testCollection = getTestCollection()) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
                assertEquals(2, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(query, resource.getContent().toString(), "<one><z> zzz </z></one>");
                }
                try (final Resource resource = result.getResource(1)) {
                    assertEquals(query, resource.getContent().toString(), "<two><z> zzz </z></two>");
                }
            }
        }
    }

    @Test
    public void attributesSerialization() throws XMLDBException {
        final XPathQueryService service = storeXMLStringAndGetQueryService(attributesSERIALIZATION, attributes);

        String query = "//@* \n";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            // needed to ensure that result is closed
        } catch (final Exception e) {
            //SENR0001 : OK - this is expected
        }
        query = "declare option exist:serialize 'method=text'; \n"
            + "//@* \n";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals("XQuery: " + query, 3, result.getSize());
        }
    }

    @Test(expected=XPathException.class)
    public void pathOperatorContainingNodesAndNonNodes() throws XMLDBException, XPathException {
        final String query = "declare function local:test() { (1,<n/>) };\n" +
                "<x/>/local:test()";
        try {
            try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
                // needed to ensure that result set is closed
            }
        } catch(final XMLDBException e) {
            if(e.getCause() instanceof XPathException xpe) {
                assertEquals(ErrorCodes.XPTY0018, xpe.getErrorCode());
                throw xpe;
            } else {
                throw e;
            }
        }
    }

    @Test
    public void exprContainingNodesAndNonNodes() throws XMLDBException, XPathException {
        final String query = "declare function local:test() { (1,<n/>) };\n" +
                "local:test()";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(2, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("1", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("<n/>", resource.getContent().toString());
            }
        }
    }

    /**
     * <a href="https://github.com/eXist-db/exist/issues/1121">https://github.com/eXist-db/exist/issues/1121</a>
     */
    @Test
    public void multipleExprsContainingNodesAndNonNodes() throws XMLDBException, XPathException {
        final String query = "declare variable $a := 'a';\n" +
                "declare function local:test() { (1,<n/>) };\n" +
                "local:test()";
        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)){
            assertEquals(2, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("1", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("<n/>", resource.getContent().toString());
            }
        }
    }

    // ======================================
    /**
     * @return
     * @throws XMLDBException
     */
    private XPathQueryService storeXMLStringAndGetQueryService(final String documentName, final String content) throws XMLDBException {
        try (final Collection testCollection = getTestCollection();
             final XMLResource doc = testCollection.createResource(documentName, XMLResource.class)) {
            doc.setContent(content);
            testCollection.storeResource(doc);
            return testCollection.getService(XPathQueryService.class);
        }
    }
}
