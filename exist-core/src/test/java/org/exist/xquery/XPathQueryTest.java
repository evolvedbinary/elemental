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

import org.exist.test.ExistWebServer;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.EXistXPathQueryService;
import org.exist.xmldb.EXistXQueryService;
import org.exist.xmldb.XmldbURI;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmldb.api.base.Collection;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XPathQueryService;
import org.xmldb.api.modules.XQueryService;
import org.xmlunit.matchers.CompareMatcher;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class XPathQueryTest {

    @ClassRule
    public static final TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static final ExistWebServer existWebServer = new ExistWebServer(true, true, true, true);
    private static final String PORT_PLACEHOLDER = "${PORT}";

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "local", XmldbURI.LOCAL_DB },
                { "remote", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc" +  XmldbURI.ROOT_COLLECTION}
        });
    }

    @Parameterized.Parameter
    public String apiName;

    @Parameterized.Parameter(value = 1)
    public String baseUri;

    private final String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(existWebServer.getPort()));
    }

    private final static String nested =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<test><c></c><b><c><b></b></c></b><b></b><c></c></test>";
    
    private final static String numbers =
            "<test>"
            + "<item id='1' type='alphanum'><price>5.6</price><stock>22</stock></item>"
            + "<item id='2'><price>7.4</price><stock>43</stock></item>"
            + "<item id='3'><price>18.4</price><stock>5</stock></item>"
            + "<item id='4'><price>65.54</price><stock>16</stock></item>"
            + "</test>";
    
    private final static String numbers2 =
            "<test xmlns=\"http://numbers.org\">"
            + "<item id='1' type='alphanum'><price>5.6</price><stock>22</stock></item>"
            + "<item id='2'><price>7.4</price><stock>43</stock></item>"
            + "<item id='3'><price>18.4</price><stock>5</stock></item>"
            + "<item id='4'><price>65.54</price><stock>16</stock></item>"
            + "</test>";
    
    private final static String namespaces =
            "<test xmlns='http://www.foo.com'>"
            + "  <section>"
            + "      <title>Test Document</title>"
            + "      <c:comment xmlns:c='http://www.other.com'>This is my comment</c:comment>"
            + "  </section>"
            + "</test>";
    
    private final static String strings =
            "<test>"
            + "<string>Hello World!</string>"
            + "<string value='Hello World!'/>"
            + "<string>Hello</string>"
            + "</test>";
    
    private final static String nested2 =
            "<RootElement>" +
            "<ChildA>" +
            "<ChildB id=\"2\"/>" +
            "</ChildA>" +
            "</RootElement>";
    
    private final static String nested3 =
            "<test>" +
            "   <a>" +
            "       <t>1</t>" +
            "       <a>" +
            "           <t>2</t>" +
            "           <a>" +
            "               <t>3</t>" +
            "           </a>" +
            "       </a>" +
            "   </a>" +
            "</test>";
    
    private final static String siblings =
            "<!-- 1 --><!-- 2 -->" +
            "<test>" +
            "   <a> <s>A</s> <n>1</n> </a>" +
            "   <a> <s>Z</s> <n>2</n> </a>" +
            "   <a> <s>B</s> <n>3</n> </a>" +
            "   <a> <s>Z</s> <n>4</n> </a>" +
            "   <a> <s>C</s> <n>5</n> </a>" +
            "   <a> <s>Z</s> <n>6</n> </a>" +
            "</test>" +
            "<!-- 3 -->";

    private final static String siblings_attr = "<a b='c' bb='cc'/>";

    private final static String siblings_named1 =
            "<x>\n" +
            "    <y n=\"1\"/>\n" +
            "    <y n=\"2\"/>\n" +
            "    <y n=\"3\"/>\n" +
            "</x>";

    private final static String siblings_named2 =
            "<y>\n" +
            "    <y n=\"1\"/>\n" +
            "    <y n=\"2\"/>\n" +
            "    <y n=\"3\"/>\n" +
            "</y>";

    private final static String ids_content =
            "<test xml:space=\"preserve\">" +
            "<a ref=\"id1\"/>" +
            "<a ref=\"id1\"/>" +
            "<d ref=\"id2\"/>" +
            "<b id=\"id1\"><name>one</name></b>" +
            "<c xml:id=\"     id2     \"><name>two</name></c>" +
            "</test>";

    private final static String ids =
            "<!DOCTYPE test [" +
            "<!ELEMENT test (a | b | c | d)*>" +
            "<!ATTLIST test xml:space CDATA #IMPLIED>" +
            "<!ELEMENT a EMPTY>" +
            "<!ELEMENT b (name)>" +
            "<!ELEMENT c (name)>" +
            "<!ELEMENT d EMPTY>" +
            "<!ATTLIST d ref IDREF #IMPLIED>" +
            "<!ELEMENT name (#PCDATA)>" +
            "<!ATTLIST a ref IDREF #IMPLIED>" +
            "<!ATTLIST b id ID #IMPLIED>" +
            "<!ATTLIST c xml:id ID #IMPLIED>]>" +
            ids_content;
    
    private final static String date =
            "<timestamp date=\"2006-04-29+02:00\"/>";
    
    private final static String quotes =
            "<test><title>&quot;Hello&quot;</title></test>";
    
    private final static String ws =
            "<test><parent xml:space=\"preserve\"><text> </text><text xml:space=\"default\"> </text></parent></test>";
    
    private final static String self =
            "<test-self><a>Hello</a><b>World!</b></test-self>";

    private final static String predicates =
        "<elem1>\n" +
        " <elem2>\n" +
        "    <elem3/>\n" +
        " </elem2>\n" +
        " <elem2>\n" +
        "    <elem3>val1</elem3>\n" +
        " </elem2>\n" +
        " <elem2>\n" +
        "    <elem3>val2</elem3>\n" +
        " </elem2>\n" +
        "</elem1>";
    
    // Added by Geoff Shuetrim (geoff@galexy.net) to highlight problems with XPath queries of elements called 'xpointer'.
    private final static String xpointerElementName =
            "<test><xpointer/></test>";

    private final static String cdata_content = "Hello there \"Bob?\"";
    private final static String cdata_xml = "<elem1><![CDATA[" + cdata_content + "]]></elem1>";
    
    private Collection testCollection;
    
    @Before
    public void setUp() throws Exception {
        // initialize driver
        final Class<?> cl = Class.forName("org.exist.xmldb.DatabaseImpl");
        final Database database = (Database) cl.newInstance();
        database.setProperty("create-database", "true");
        DatabaseManager.registerDatabase(database);

        try (final Collection root = DatabaseManager.getCollection(getBaseUri(), "admin", "")) {
            final CollectionManagementService service = root.getService(CollectionManagementService.class);
            testCollection = service.createCollection("test");
            assertNotNull(testCollection);
        }
    }

    @After
    public void tearDown() throws XMLDBException {
        testCollection.close();
    }

    @Test
    public void childWildcards() throws XMLDBException {
        final String docName = "testChildWildcards.xml";
        final XQueryService service =
            storeXMLStringAndGetQueryService(docName, "<test xmlns=\"http://test\"/>");

        service.setNamespace("t", "http://test");

        queryResourceV(service, docName, "/t:test", 1); //make sure all is well!

        queryResourceV(service, docName, "/*", 1);
        queryResourceV(service, docName, "/t:*", 1);
        queryResourceV(service, docName, "/*:test", 1);

        queryResourceV(service, docName, "/child::*", 1);
        queryResourceV(service, docName, "/child::t:*", 1);
        queryResourceV(service, docName, "/child::*:test", 1);
    }

    @Test
    public void pathExpression() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("numbers.xml", numbers);

        //Invalid path expression left operand (not a node set).
        String message = "";
        try {
            queryAndAssertV(service, "('a', 'b', 'c')/position()", -1, null);
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue("Exception wanted: " + message, message.indexOf("XPTY0019") > -1);

        //Undefined context sequence
        message = "";
        try {
            queryAndAssertV(service, "for $a in (<a/>, <b/>, doh, <c/>) return $a", -1, null);
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue("Exception wanted: " + message, message.indexOf("XPDY0002") > -1);

        message = "";
        try {
            //"1 to 2" is resolved as a (1, 2), i.e. a sequence of *integers* which is *not* a singleton
            queryAndAssertV(service, "let $a := (1, 2, 3) for $b in $a[1 to 2] return $b", -1, null);
        } catch (final XMLDBException e) {
            message = e.getMessage();
        }
        //No effective boolean value for such a kind of sequence !
        assertTrue("Exception wanted: " + message, message.indexOf("FORG0006") >-1);

        queryAndAssertV(service, "let $a := ('a', 'b', 'c') return $a[2 to 2]", 1, null);
        queryAndAssertV(service, "let $a := ('a', 'b', 'c') return $a[(2 to 2)]", 1, null);
        queryAndAssertV(service, "let $x := <a min='1' max='10'/> return ($x/@min to $x/@max)", 10, null);
        queryAndAssertV(service, "(1,2,3)[xs:decimal(.)]", 3, null);
        queryAndAssertV(service, "(1,2,3)[. lt 3]", 2, null);
        queryAndAssertV(service, "(0, 1, 2)[if(. eq 1) then 0 else position()]", 2, null);
        queryAndAssertV(service, "(1, 2, 3)[if(1) then 1 else last()]", 1, null);
        queryAndAssertV(service, "(1, 2, 3)[if(1) then 1 else position()]", 1, null);
        queryAndAssertV(service, "()/position()", 0, null);
        queryAndAssertV(service, "(0, 1, 2)[if(. eq 1) then 2 else 3]", 2, null);
        queryAndAssertV(service, "(0, 1, 2)[remove((1, 'a string'), 2)]", 1, null);
        queryAndAssertV(service, "let $page-ix := (1,3) return ($page-ix[1] to $page-ix[2])", 3, null);
    }

    /** test simple queries involving attributes */
    @Test
    public void attributes() throws XMLDBException {
        final String testDocument = "numbers.xml";

        final XQueryService service = storeXMLStringAndGetQueryService(
                testDocument, numbers);

        String query = "/test/item[ @id='1' ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(testDocument, query)) {
            assertEquals("XPath: " + query, 1, result.getSize());

            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                Node node = resource.getContentAsDOM();
                if (node.getNodeType() == Node.DOCUMENT_NODE)
                    node = node.getFirstChild();
                assertEquals("XPath: " + query, "item", node.getNodeName());
            }
        }

        query = "/test/item [ @type='alphanum' ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(testDocument, query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }
    }

    @Test
    public void starAxis() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("numbers.xml", numbers);

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", "/*/item")) {
            assertEquals("XPath: /*/item", 4, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", "/test/*")) {
            assertEquals("XPath: /test/*", 4, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", "/test/descendant-or-self::*")) {
            assertEquals("XPath: /test/descendant-or-self::*", 13, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", "/*/*")) {
            //Strange !!! Should be 8
            assertEquals("XPath: /*/*", 4, result.getSize());
        }
    }

    @Test
    public void starAxisConstraints() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("namespaces.xml", namespaces);
        service.setNamespace("t", "http://www.foo.com");

        String query = "// t:title/text() [ . != 'aaaa' ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }

        query = "/t:test/*:section[contains(., 'comment')]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }

        query = "/t:test/t:*[contains(., 'comment')]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }

        query = "/t:test/t:section[contains(., 'comment')]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }

        query = "/t:test/t:section/*[contains(., 'comment')]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }

        query = "/ * / * [ t:title ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }

        query = "/ t:test / t:section [ t:title ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }

        query = "/ t:test / t:section";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }
    }

    @Test
    public void starAxisConstraints2() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("namespaces.xml", namespaces);
        service.setNamespace("t", "http://www.foo.com");

        String query =  "/ * [ ./ * / t:title ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }

        query =  "/ * [ * / t:title ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }
    }

    @Test
    public void starAxisConstraints3() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("namespaces.xml", namespaces);
        service.setNamespace("t", "http://www.foo.com");

        final String query =  "// * [ . = 'Test Document' ]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
        }
    }

    @Test
    public void root() throws XMLDBException {
        storeXMLStringAndGetQueryService("nested2.xml", nested2);
        final XQueryService service = storeXMLStringAndGetQueryService("numbers.xml", numbers);

        String query = "let $doc := <a><b/></a> return root($doc)";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());

            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                Node node = resource.getContentAsDOM();
                //Oh dear ! Don't tell me that *I* have written this :'( -pb
                if (node.getNodeType() == Node.DOCUMENT_NODE) {
                    node = node.getFirstChild();
                }
                assertEquals("XPath: " + query, "a", node.getNodeName());
            }
        }

        query = "let $c := (<a/>,<b/>,<c/>,<d/>,<e/>) return count($c/root())";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("5", resource.getContent().toString());
            }
        }
    }

    @Test
    public void name() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("nested2.xml", nested2);

        final String query = "(<a/>,<b/>)/name()";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("nested2.xml", query)) {
            assertEquals("XPath: " + query, 2, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("a", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("b", resource.getContent().toString());
            }
        }
    }

    @Test
    public void parentAxis() throws XMLDBException {
        XQueryService service =
                storeXMLStringAndGetQueryService("nested2.xml", nested2);

        queryResourceV(service, "nested2.xml", "(<a/>, <b/>, <c/>)/parent::*", 0);
        queryResourceV(service, "nested2.xml", "/RootElement//ChildB/parent::*", 1);
        queryResourceV(service, "nested2.xml", "/RootElement//ChildB/parent::*/ChildB", 1);
        queryResourceV(service, "nested2.xml", "/RootElement/ChildA/parent::*/ChildA/ChildB", 1);

        service = storeXMLStringAndGetQueryService("numbers2.xml", numbers2);
        service.setNamespace("n", "http://numbers.org");
        queryResourceV(service, "numbers2.xml", "//n:price[. = 18.4]/parent::*[@id = '3']", 1);
        queryResourceV(service, "numbers2.xml", "//n:price[. = 18.4]/parent::n:item[@id = '3']", 1);
        queryResourceV(service, "numbers2.xml", "//n:price/parent::n:item[@id = '3']", 1);

        try (final EXistResourceSet result = queryResource(service, "numbers2.xml", "//n:price[. = 18.4]/parent::n:*/string(@id)", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("3", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers2.xml", "//n:price[. = 18.4]/parent::*:item/string(@id)", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("3", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers2.xml", "//n:price[. = 18.4]/../string(@id)", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("3", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers2.xml", "//n:price[. = 18.4]/parent::n:item/string(@id)", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("3", resource.getContent().toString());
            }
        }
        queryResourceV(service, "numbers2.xml",
            "for $price in //n:price where $price/parent::*[@id = '3']/n:stock = '5' return $price", 1);
    }

    @Test
    public void parentSelfAxis() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("nested2.xml", nested2);
        storeXMLStringAndGetQueryService("numbers.xml", numbers);
        queryResourceV(service, "nested2.xml", "/RootElement/descendant::*/parent::ChildA", 1);
        queryResourceV(service, "nested2.xml", "/RootElement/descendant::*[self::ChildB]/parent::RootElement", 0);
        queryResourceV(service, "nested2.xml", "/RootElement/descendant::*[self::ChildA]/parent::RootElement", 1);
        queryResourceV(service, "nested2.xml", "let $a := ('', 'b', '', '') for $b in $a[.] return <blah>{$b}</blah>", 1);

        final String query = "let $doc := <root><page><a>a</a><b>b</b></page></root>" +
                "return " +
                "for $element in $doc/page/* " +
                "return " +
                "if($element[self::a] or $element[self::b]) then (<found/>) else (<notfound/>)";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals(2, result.getSize());
        }
    }

    @Test
    public void selfAxis() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("self.xml", self);

        queryResourceV(service, "self.xml", "/test-self/self::document-node()", 0);
        queryResourceV(service, "self.xml", "/test-self/self::node()", 1);
        queryResourceV(service, "self.xml", "/test-self/self::attribute()", 0);
        queryResourceV(service, "self.xml", "/test-self/self::element()", 1);
        queryResourceV(service, "self.xml", "/test-self/self::comment()", 0);
        queryResourceV(service, "self.xml", "/test-self/self::processing-instruction()", 0);
        queryResourceV(service, "self.xml", "/test-self/self::text()", 0);
        queryResourceV(service, "self.xml", "/test-self/self::namespace-node()", 0);

        queryResourceV(service, "self.xml", "/test-self/*[not(self::a)]", 1);
        queryResourceV(service, "self.xml", "/test-self/*[self::a]", 1);

        queryResourceV(service, "self.xml", "/self::document-node()", 1);
        queryResourceV(service, "self.xml", "/self::node()", 1);
        queryResourceV(service, "self.xml", "/self::attribute()", 0);
        queryResourceV(service, "self.xml", "/self::element()", 0);
        queryResourceV(service, "self.xml", "/self::comment()", 0);
        queryResourceV(service, "self.xml", "/self::processing-instruction()", 0);
        queryResourceV(service, "self.xml", "/self::text()", 0);
        queryResourceV(service, "self.xml", "/self::namespace-node()", 0);
    }

    @Test
    public void ancestorAxis() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("nested3.xml", nested3);

        // test ancestor axis with positional predicate
        queryResourceV(service, "nested3.xml", "//a[ancestor::a[2]/t = '1']", 1);
        queryResourceV(service, "nested3.xml", "//a[ancestor::*[2]/t = '1']", 1);
        queryResourceV(service, "nested3.xml", "//a[ancestor::a[1]/t = '2']", 1);
        queryResourceV(service, "nested3.xml", "//a[ancestor::*[1]/t = '2']", 1);
        queryResourceV(service, "nested3.xml", "//a[ancestor-or-self::*[3]/t = '1']", 1);
        queryResourceV(service, "nested3.xml", "//a[ancestor-or-self::a[3]/t = '1']", 1);
        // Following test fails
//            queryResource(service, "nested3.xml", "//a[ancestor-or-self::*[2]/t = '2']", 1);
        queryResourceV(service, "nested3.xml", "//a[ancestor-or-self::a[2]/t = '2']", 1);
        queryResourceV(service, "nested3.xml", "//a[t = '3'][ancestor-or-self::a[3]/t = '1']", 1);
        queryResourceV(service, "nested3.xml", "//a[t = '3'][ancestor-or-self::*[3]/t = '1']", 1);
    }

    @Test
    public void ancestorIndex() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("nested2.xml", nested2);

        queryResourceV(service, "nested2.xml", "//ChildB/ancestor::*[1]/self::ChildA", 1);
        queryResourceV(service, "nested2.xml", "//ChildB/ancestor::*[2]/self::RootElement", 1);
        queryResourceV(service, "nested2.xml", "//ChildB/ancestor::*[position() = 1]/self::ChildA", 1);
        queryResourceV(service, "nested2.xml", "//ChildB/ancestor::*[position() = 2]/self::RootElement", 1);
        queryResourceV(service, "nested2.xml", "//ChildB/ancestor::*[position() = 2]/self::RootElement", 1);
        queryResourceV(service, "nested2.xml", "(<a/>, <b/>, <c/>)/ancestor::*", 0);
    }

    @Test
    public void precedingSiblingAxis_persistent() throws XMLDBException, IOException, SAXException {
        XQueryService service =
                storeXMLStringAndGetQueryService("siblings.xml", siblings);
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "//a[preceding-sibling::*[1]/s = 'B']", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a> <s>Z</s> <n>4</n> </a>"));
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "//a[preceding-sibling::a[1]/s = 'B']", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a> <s>Z</s> <n>4</n> </a>"));
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "//a[preceding-sibling::*[2]/s = 'B']", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a> <s>C</s> <n>5</n> </a>"));
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "//a[preceding-sibling::a[2]/s = 'B']", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a> <s>C</s> <n>5</n> </a>"));
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "/test/preceding-sibling::node()", 2)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 1 -->", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("<!-- 2 -->", resource.getContent().toString());
            }
        }

        queryResourceV(service, "siblings.xml", "/node()[1]/preceding-sibling::node()", 0);

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "/node()[2]/preceding-sibling::node()", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 1 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "/node()[3]/preceding-sibling::node()", 2)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 1 -->", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("<!-- 2 -->", resource.getContent().toString());
            }
        }

        queryResourceV(service, "siblings.xml", "/comment()[1]/preceding-sibling::comment()", 0);

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "/comment()[2]/preceding-sibling::comment()[1]", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 1 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "/comment()[3]/preceding-sibling::comment()[1]", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 2 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "/comment()[3]/preceding-sibling::comment()[2]", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 1 -->", resource.getContent().toString());
            }
        }

        service = storeXMLStringAndGetQueryService("siblings_attr.xml", siblings_attr);
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        queryResourceV(service, "siblings_attr.xml", "/a/@bb/preceding-sibling::*", 0);

        service = storeXMLStringAndGetQueryService("siblings_named1.xml", siblings_named1);
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        queryResourceV(service, "siblings_named1.xml", "//y[@n eq '2']/preceding-sibling::*:y", 1);
        queryResourceV(service, "siblings_named1.xml", "//y[@n eq '2']/preceding-sibling::y", 1);

        service = storeXMLStringAndGetQueryService("siblings_named2.xml", siblings_named2);
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        queryResourceV(service, "siblings_named2.xml", "//y[@n eq '2']/preceding-sibling::*:y", 1);
        queryResourceV(service, "siblings_named2.xml", "//y[@n eq '2']/preceding-sibling::y", 1);
    }

    @Test
    public void precedingSiblingAxis_memtree() throws XMLDBException, IOException, SAXException {
        final XQueryService service = getQueryService();
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        try (final EXistResourceSet result = (EXistResourceSet) service.query("(<a/>, <b/>, <c/>)/preceding-sibling::*")) {
            assertEquals(0, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := <doc><div id='1'/><div id='2'><div id='3'/></div><div id='4'/><div id='5'><div id='6'/></div></doc> " +
                "return $doc/div/preceding-sibling::div")) {
            assertEquals(3, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<div id='1'/>"));
            }
            try (final Resource resource = result.getResource(1)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<div id='2'><div id='3'/></div>"));
            }
            try (final Resource resource = result.getResource(2)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<div id='4'/>"));
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/node()[1]/preceding-sibling::node()")) {
            assertEquals(0, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/node()[2]/preceding-sibling::node()")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 1 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/node()[3]/preceding-sibling::node()")) {
            assertEquals(2, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 1 -->", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("<!-- 2 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/comment()[1]/preceding-sibling::comment()")) {
            assertEquals(0, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/comment()[2]/preceding-sibling::comment()[1]")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 1 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/comment()[3]/preceding-sibling::comment()[1]")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 2 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/comment()[3]/preceding-sibling::comment()[2]")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 1 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $elem := <a b='c' bb='cc'/> return $elem/@bb/preceding-sibling::*")) {
            assertEquals(0, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <x><y n=\"1\"/><y n=\"2\"/><y n=\"3\"/></x> } return $doc //y[@n eq '2']/preceding-sibling::*:y")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <x><y n=\"1\"/><y n=\"2\"/><y n=\"3\"/></x> } return $doc //y[@n eq '2']/preceding-sibling::y")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <y><y n=\"1\"/><y n=\"2\"/><y n=\"3\"/></y> } return $doc //y[@n eq '2']/preceding-sibling::*:y")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <y><y n=\"1\"/><y n=\"2\"/><y n=\"3\"/></y> } return $doc //y[@n eq '2']/preceding-sibling::y")) {
            assertEquals(1, result.getSize());
        }
    }

    @Test
    public void followingSiblingAxis_persistent() throws XMLDBException, IOException, SAXException {
        XQueryService service = storeXMLStringAndGetQueryService("siblings.xml", siblings);
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "//a[following-sibling::*[1]/s = 'B']", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a> <s>Z</s> <n>2</n> </a>"));
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "//a[following-sibling::a[1]/s = 'B']", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a> <s>Z</s> <n>2</n> </a>"));
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "//a[following-sibling::*[2]/s = 'B']", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a> <s>A</s> <n>1</n> </a>"));
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "//a[following-sibling::a[2]/s = 'B']", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a> <s>A</s> <n>1</n> </a>"));
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "/test/following-sibling::node()", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 3 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "/node()[1]/following-sibling::node()", 3)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 2 -->", resource.getContent().toString());
            }
            try (final XMLResource resource = (XMLResource) result.getResource(1)) {
                final Node testElem = resource.getContentAsDOM();
                assertTrue(testElem instanceof Element);
                assertEquals("test", testElem.getNodeName());
            }
            try (final Resource resource = result.getResource(2)) {
                assertEquals("<!-- 3 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "/comment()[1]/following-sibling::comment()[1]", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 2 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "/comment()[1]/following-sibling::comment()[2]", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 3 -->", resource.getContent().toString());
            }
        }

        service = storeXMLStringAndGetQueryService("siblings_attr.xml", siblings_attr);
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        queryResourceV(service, "siblings_attr.xml", "/a/@b/following-sibling::*", 0);

        service = storeXMLStringAndGetQueryService("siblings_named1.xml", siblings_named1);
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        queryResourceV(service, "siblings_named1.xml", "//y[@n eq '2']/following-sibling::*:y", 1);
        queryResourceV(service, "siblings_named1.xml", "//y[@n eq '2']/following-sibling::y", 1);

        service = storeXMLStringAndGetQueryService("siblings_named2.xml", siblings_named2);
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        queryResourceV(service, "siblings_named2.xml", "//y[@n eq '2']/following-sibling::*:y", 1);
        queryResourceV(service, "siblings_named2.xml", "//y[@n eq '2']/following-sibling::y", 1);
    }

    @Test
    public void followingSiblingAxis_memtree() throws XMLDBException, IOException, SAXException {
        final XQueryService service = getQueryService();
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        try (final EXistResourceSet result = (EXistResourceSet) service.query("(<a/>, <b/>, <c/>)/following-sibling::*")) {
            assertEquals(0, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := <doc><div id='1'><div id='2'/></div><div id='3'/></doc> " +
                "return $doc/div[1]/following-sibling::div")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<div id='3'/>"));
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/test/following-sibling::node()")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 3 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/node()[1]/following-sibling::node()")) {
            assertEquals(3, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 2 -->", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<test/>"));
            }
            try (final Resource resource = result.getResource(2)) {
                assertEquals("<!-- 3 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/comment()[1]/following-sibling::comment()[1]")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 2 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <!-- 1 -->,<!-- 2 -->,<test/>,<!-- 3 --> } return $doc/comment()[1]/following-sibling::comment()[2]")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<!-- 3 -->", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $elem := <a b='c' bb='cc'/> return $elem/@b/following-sibling::*")) {
            assertEquals(0, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <x><y n=\"1\"/><y n=\"2\"/><y n=\"3\"/></x> } return $doc //y[@n eq '2']/following-sibling::*:y")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <x><y n=\"1\"/><y n=\"2\"/><y n=\"3\"/></x> } return $doc //y[@n eq '2']/following-sibling::y")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <y><y n=\"1\"/><y n=\"2\"/><y n=\"3\"/></y> } return $doc //y[@n eq '2']/following-sibling::*:y")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document { <y><y n=\"1\"/><y n=\"2\"/><y n=\"3\"/></y> } return $doc //y[@n eq '2']/following-sibling::y")) {
            assertEquals(1, result.getSize());
        }
    }

    @Test
    public void followingAxis() throws XMLDBException, IOException, SAXException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("siblings.xml", siblings);
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        queryResourceV(service, "siblings.xml", "//a/s[. = 'B']/following::s", 3);
        queryResourceV(service, "siblings.xml", "//a/s[. = 'B']/following::n", 4);
        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "//a/s[. = 'B']/following::s[1]", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<s>Z</s>"));
            }
        }

        try (final EXistResourceSet result = queryResource(service, "siblings.xml", "//a/s[. = 'B']/following::s[2]", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<s>C</s>"));
            }
        }

        String query = "declare variable $i := \n" +
            "  <root>\n" +
            "     <child/>\n" +
            "     <child/>\n" +
            "     <child>\n" +
            "        <child2>\n" +
            "           <child3>\n" +
            "              <leaf/>\n" +
            "           </child3>\n" +
            "        </child2>\n" +
            "     </child>\n" +
            "  </root>;\n" +
            "\n" +
            "root($i)//following::node()";

        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(5, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<child/>"));
            }
            try (final Resource resource = result.getResource(1)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<child><child2><child3><leaf/></child3></child2></child>"));
            }
            try (final Resource resource = result.getResource(2)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<child2><child3><leaf/></child3></child2>"));
            }
            try (final Resource resource = result.getResource(3)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<child3><leaf/></child3>"));
            }
            try (final Resource resource = result.getResource(4)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<leaf/>"));
            }
        }

        query = "declare variable $i := \n" +
            "  <root>\n" +
            "     <child/>\n" +
            "     <child/>\n" +
            "     <child>\n" +
            "        <child2>\n" +
            "           <child3>\n" +
            "              <leaf/>\n" +
            "           </child3>\n" +
            "        </child2>\n" +
            "     </child>\n" +
            "  </root>;\n" +
            "\n" +
            "root($i)//count(following::node())";

        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(7, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("0", resource.getContent());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("5", resource.getContent());
            }
            try (final Resource resource = result.getResource(2)) {
                assertEquals("4", resource.getContent());
            }
            try (final Resource resource = result.getResource(3)) {
                assertEquals("0", resource.getContent());
            }
            try (final Resource resource = result.getResource(4)) {
                assertEquals("0", resource.getContent());
            }
            try (final Resource resource = result.getResource(5)) {
                assertEquals("0", resource.getContent());
            }
            try (final Resource resource = result.getResource(6)) {
                assertEquals("0", resource.getContent());
            }
        }
    }

    @Test
    public void precedingAxis() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("siblings.xml", siblings);
        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");
        queryResourceV(service, "siblings.xml", "//a/s[. = 'B']/preceding::s", 2);
        queryResourceV(service, "siblings.xml", "//a/s[. = 'C']/preceding::s", 4);
        queryResourceV(service, "siblings.xml", "//a/n[. = '3']/preceding::s", 3);
    }

    @Test
    public void position() throws XMLDBException, IOException, SAXException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("numbers.xml", numbers);

        queryResourceV(service, "numbers.xml", "//item[position() = 3]", 1);
        queryResourceV(service, "numbers.xml", "//item[position() < 3]", 2);
        queryResourceV(service, "numbers.xml", "//item[position() <= 3]", 3);
        queryResourceV(service, "numbers.xml", "//item[position() > 3]", 1);
        queryResourceV(service, "numbers.xml", "//item[position() >= 3]", 2);
        queryResourceV(service, "numbers.xml", "//item[position() eq 3]", 1);
        queryResourceV(service, "numbers.xml", "//item[position() lt 3]", 2);
        queryResourceV(service, "numbers.xml", "//item[position() le 3]", 3);
        queryResourceV(service, "numbers.xml", "//item[position() gt 3]", 1);
        queryResourceV(service, "numbers.xml", "//item[position() ge 3]", 2);

        queryResourceV(service, "numbers.xml", "//item[last() - 1]", 1);
        queryResourceV(service, "numbers.xml", "//item[count(('a','b')) - 1]", 1);

        String query = "for $a in (<a/>, <b/>, <c/>) return $a/position()";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals("XPath: " + query, 3, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XPath: " + query, "1", resource.getContent().toString());
            }
            try (final XMLResource resource = (XMLResource) result.getResource(1)) {
                assertEquals("XPath: " + query, "1", resource.getContent().toString());
            }
            try (final XMLResource resource = (XMLResource) result.getResource(2)) {
                assertEquals("XPath: " + query, "1", resource.getContent().toString());
            }
        }

        query = "declare variable $doc := <root>" +
                "<a>1</a><a>2</a><a>3</a><a>4</a><a>5</a><a>6</a><a>7</a>" +
                "</root>; " +
                "(for $x in $doc/a return $x)[position() mod 3 = 2]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals("XPath: " + query, 2, result.getSize());
        }

        query = "declare variable $doc := <root>" +
                "<a>1</a><a>2</a><a>3</a><a>4</a><a>5</a><a>6</a><a>7</a>" +
                "</root>; " +
                "for $x in $doc/a return $x[position() mod 3 = 2]";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals("XPath: " + query, 0, result.getSize());
        }

        query = "declare variable $doc := <root>" +
                "<a>1</a><a>2</a><a>3</a><a>4</a><a>5</a><a>6</a><a>7</a>" +
                "</root>; " +
                "for $x in $doc/a[position() mod 3 = 2] return $x";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals("XPath: " + query, 2, result.getSize());
        }


        query = "let $test := <test><a> a </a><a>a</a></test>" +
                "return distinct-values($test/a/normalize-space(.))";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XPath: " + query, "a", resource.getContent().toString());
            }
        }

        query = "let $doc := document {<a><b n='1'/><b n='2'/></a>} " +
            "return $doc//b/(if (@n = '1') then position() else ())";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("1", resource.getContent().toString());
            }
        }

        //Try a second time to see if the position is reset
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("1", resource.getContent().toString());
            }
        }

        query = "let $doc := document {<a><b/></a>} " +
        "return $doc/a[1] [b[1]]";
        service.setProperty(OutputKeys.INDENT, "no");
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals("XPath: " + query, 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a><b/></a>"));
            }
        }

        //TODO : make this work ! It currently returns some content
        //query = "let $doc := document {<a><b><c>1</c></b><b><c>a</c></b></a>} " +
        //	"return $doc/a[b[position() = 2]/c[.='1']]";
        //result = service.queryResource("numbers.xml", query);
        //assertEquals("XPath: " + query, 0, result.getSize());

        // TODO: make this work ! It currently returns 1
        //query = "let $a := ('a', 'b', 'c') for $b in $a[position()] return <blah>{$b}</blah>";
        //result = service.queryResource("numbers.xml", query);
        //assertEquals("XPath: " + query, 3, result.getSize());
    }

    @Test
    public void last() throws XMLDBException {
        final XQueryService service =
            storeXMLStringAndGetQueryService("numbers.xml", numbers);

        final String query = "<a><b>test1</b><b>test2</b></a>/b/last()";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals("XPath: " + query, 2, result.getSize());
            try (final XMLResource resource = (XMLResource) result.getResource(0)) {
                assertEquals("XPath: " + query, "2", resource.getContent().toString());
            }
            try (final XMLResource resource = (XMLResource) result.getResource(1)) {
                assertEquals("XPath: " + query, "2", resource.getContent().toString());
            }
        }
    }


    @Test
    public void numbers() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("numbers.xml", numbers);

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "sum(/test/item/price)", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("96.94", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "round(sum(/test/item/price))", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("97", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "floor(sum(/test/item/stock))", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("86", resource.getContent().toString());
            }
        }

        queryResourceV(service, "numbers.xml", "/test/item[round(price + 3) > 60]", 1);

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "min(( 123456789123456789123456789, " +
                "123456789123456789123456789123456789123456789 ))", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("minimum of big integers",
                    "123456789123456789123456789",
                    resource.getContent().toString());
            }
        }

        String message = "";
        try {
            queryResourceV(service, "numbers.xml", "empty(() + (1, 2))", 1);
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XPTY0004") > -1);
    }

    @Test
    public void dates() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("numbers.xml", numbers);

        String query = "xs:untypedAtomic(\"--12-05:00\") cast as xs:gMonth";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query);
             final XMLResource resource = (XMLResource)result.getResource(0)) {
            assertEquals("XPath: " + query, "--12-05:00", resource.getContent().toString());
        }

        query = "(xs:dateTime(\"0001-01-01T01:01:01Z\") + xs:yearMonthDuration(\"-P20Y07M\"))";
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query);
             final XMLResource resource = (XMLResource)result.getResource(0)) {
            assertEquals("XPath: " + query, "-0021-06-01T01:01:01Z", resource.getContent().toString());
        }
    }
    
    @Test
    public void generalComparison() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("dates.xml", date);
        queryResourceV(service, "dates.xml", "/timestamp[@date = xs:date('2006-04-29+02:00')]", 1);
    }
    
    @Test
    public void predicates() throws XMLDBException, IOException, SAXException {
        final String numbers =
                "<test>"
                + "<item id='1' type='alphanum'><price>5.6</price><stock>22</stock></item>"
                + "<item id='2'><price>7.4</price><stock>43</stock></item>"
                + "<item id='3'><price>18.4</price><stock>5</stock></item>"
                + "<item id='4'><price>65.54</price><stock>16</stock></item>"
                + "</test>";

        final XQueryService service =
                storeXMLStringAndGetQueryService("numbers.xml", numbers);
        service.setProperty(OutputKeys.INDENT, "no");
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "/test/item[2]/price/text()", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("7.4", resource.getContent().toString());
            }
        }

        queryResourceV(service, "numbers.xml", "/test/item[5]", 0);

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "/test/item[@id='4'][1]/price[1]/text()", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("65.54",
                    resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "for $i in //item return " +
                "<item>{$i/price, $i/stock}</item>", 4)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<item><price>5.6</price><stock>22</stock></item>"));
            }
            try (final Resource resource = result.getResource(3)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<item><price>65.54</price><stock>16</stock></item>"));
            }
        }

        // test positional predicates
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "/test/node()[2]", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<item id='2'><price>7.4</price><stock>43</stock></item>"));
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "/test/element()[2]", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<item id='2'><price>7.4</price><stock>43</stock></item>"));
            }
        }

        // positional predicate on sequence of atomic values
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "('test', 'pass')[2]", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("pass", resource.getContent().toString());
            }
        }
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "let $credentials := ('test', 'pass') let $user := $credentials[1] return $user", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("test", resource.getContent().toString());
            }
        }
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "let $credentials := ('test', 'pass') let $user := $credentials[2] return $user", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("pass", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "let $els := <els><el>text1</el><el>text2</el></els> return $els/el[xs:string(.) eq 'text1'] ", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<el>text1</el>", resource.getContent().toString());
            }
        }
    }

    @Test
    public void predicates2() throws XMLDBException, IOException, SAXException {
        final String numbers =
                "<test>"
                + "<item id='1' type='alphanum'><price>5.6</price><stock>22</stock></item>"
                + "<item id='2'><price>7.4</price><stock>43</stock></item>"
                + "<item id='3'><price>18.4</price><stock>5</stock></item>"
                + "<item id='4'><price>65.54</price><stock>16</stock></item>"
                + "</test>";

        final XQueryService service =
                storeXMLStringAndGetQueryService("numbers.xml", numbers);
        service.setProperty(OutputKeys.INDENT, "no");


        String query = "let $t := " +
            "<test>" +
            "<a> <s>A</s> 1 </a>" +
            "<a> <s>Z</s> 2 </a>" +
            "<a> <s>B</s> 3 </a>" +
            "<a> <s>Z</s> 4 </a>" +
            "<a> <s>C</s> 5 </a>" +
            "<a> <s>Z</s> 6 </a>" +
            "</test>" +
            "return " +
            "$t//a[s = 'Z' and preceding-sibling::*[1]/s = 'B']";
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", query, 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a><s>Z</s> 4 </a>"));
            }
        }

        query = "let $t := <test>" + "<a> <s>A</s> 1 </a>"
                + "<a> <s>Z</s> 2 </a>" + "<a> <s>B</s> 3 </a>"
                + "<a> <s>Z</s> 4 </a>" + "<a> <s>C</s> 5 </a>"
                + "<a> <s>Z</s> 6 </a>" + "</test>"
                + "return $t//a[s='Z' and ./preceding-sibling::*[1]/s='B']";
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", query, 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a><s>Z</s> 4 </a>"));
            }
        }

        query = "let $doc := <doc><rec n='1'><a>first</a><b>second</b></rec>" +
                "<rec n='2'><a>first</a><b>third</b></rec></doc> " +
                "return $doc//rec[fn:not(b = 'second') and (./a = 'first')]";
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", query, 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<rec n=\"2\"><a>first</a><b>third</b></rec>"));
            }
        }

        query = "let $doc := <doc><a b='c' d='e'/></doc> " +
                "return $doc/a[$doc/a/@b or $doc/a/@d]";
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", query, 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a b=\"c\" d=\"e\"/>"));
            }
        }

        query = "let $x := <a><b><x/><x/></b><b><x/></b></a>" +
            "return $x//b[count(x) = 2]";
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", query, 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<b><x/><x/></b>"));
            }
        }


        //Boolean evaluation for "." (atomic sequence)
        query = "(1,2,3)[xs:decimal(.)]";
        queryResourceV(service, "numbers.xml", query, 3);

        query = "(1,2,3)[number()]";
        queryResourceV(service, "numbers.xml", query, 3);

        query = "let $c := (<a/>,<b/>), $i := 1 return $c[$i]";
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", query, 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<a/>"));
            }
        }

        query = "(1,2,3)[position() = last()]";
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", query, 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("3", resource.getContent().toString());
            }
        }

        query = "(1,2,3)[max(.)]";
        queryResourceV(service, "numbers.xml", query, 3);

        query = "(1,2,3)[max(.[. gt 1])]";
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", query, 2)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("2", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("3", resource.getContent().toString());
            }
        }

        query = "(1,2,3)[.]";
        queryResourceV(service, "numbers.xml", query, 3);

        query = "declare function local:f ($n) { " +
            "$n " +
            "}; " +
            " " +
            "declare function local:g( $n ) { " +
            "('OK','Fine','Wrong') [local:f($n) + 1 ] " +
            "} ; " +
            " " +
            "declare function local:h( $n ) { " +
            "('OK','Fine','Wrong') [local:f($n) ] " +
            "} ; " +
            " " +
            "declare function local:j( $n ) { " +
            "let $m := local:f($n) " +
            "return " +
            "('OK','Fine','Wrong') [$m + 1 ] " +
            "} ; " +
            " " +
            "declare function local:k ( $n ) { " +
            "('OK','Fine','Wrong') [ $n + 1 ] " +
            "} ; " +
            " " +
            "local:f(1),local:g(1), local:h(1), local:j(1), local:k(1) ";
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", query, 5)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("1", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("Fine", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(2)) {
                assertEquals("OK", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(3)) {
                assertEquals("Fine", resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(4)) {
                assertEquals("Fine", resource.getContent().toString());
            }
        }

        //The collection doesn't exist : let's see how the query behaves with empty sequences
        query = "let $checkDate := xs:date(adjust-date-to-timezone(current-date(), ())) " +
        "let $collection := if (xmldb:collection-available(\"/db/lease\")) then collection(\"/db/lease\") else () " +
        "for $x in " +
        "$collection//Lease/Events/Type/Event[(When/Date<=$checkDate or " +
        "When/EstimateDate<=$checkDate) and not(Status='Complete')] " +
        "return $x";
        queryResourceV(service, "numbers.xml", query, 0);

        query = "let $res := <test><element name='A'/><element name='B'/></test> " +
            "return " +
            "for $name in ('A', 'B') return " +
            "$res/element[@name=$name][1]";
        try (final EXistResourceSet result = queryResource(service, "numbers.xml", query, 2)) {
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<element name='A'/>"));
            }
            try (final Resource resource = result.getResource(1)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<element name='B'/>"));
            }
        }
    }


    /**
     * @see <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1460610&group_id=17691&atid=117691">http://sourceforge.net/tracker/index.php?func=detail&aid=1460610&group_id=17691&atid=117691</a>
     */
    @Test
    public void predicates_bug1460610() throws XMLDBException {
        final String xQuery = "(1, 2, 3)[ . lt 3]";
        
        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {

            assertEquals("SFBUG 1460610 nr of results", 2, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("SFBUG 1460610 1st result", "1",
                    resource.getContent().toString());
            }
            try (final Resource resource = result.getResource(1)) {
                assertEquals("SFBUG 1460610 2nd result", "2",
                    resource.getContent().toString());
            }
        }
    }

    /**
     * @see <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1537355&group_id=17691&atid=117691">http://sourceforge.net/tracker/index.php?func=detail&aid=1537355&group_id=17691&atid=117691</a>
     */
    @Test
    public void predicates_bug1537355() throws XMLDBException {
        final String xQuery = "let $one := 1 return (1, 2, 3)[$one + 1]";
        
        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {
            assertEquals("SFBUG 1537355 nr of results", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("SFBUG 1537355 result", "2",
                    resource.getContent().toString());
            }
        }
    }

    /**
     * @see <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1533053&group_id=17691&atid=117691">http://sourceforge.net/tracker/index.php?func=detail&aid=1533053&group_id=17691&atid=117691</a>
     */
    @Test
    public void nestedPredicates_bug1533053() throws XMLDBException, IOException, SAXException {
        final XQueryService service = getQueryService();

        String xQuery = "let $doc := <objects>" +
    	    "<detail><class/><source><dynamic>false</dynamic></source></detail>" +
    	    "<detail><class/><source><dynamic>true</dynamic></source></detail>" +
    	    "</objects> " +
    	    "let $matches := $doc/detail[source[dynamic='false'] or class] " +
    	    "return count($matches) eq 2";
        
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("true", resource.getContent().toString());
            }
        }

	    xQuery = "let $xml := <test><element>" +
	    	"<complexType><attribute name=\"design\" fixed=\"1\"/></complexType>" +
        	"</element></test> " +
        	"return $xml//element[complexType/attribute[@name eq \"design\"]/@fixed eq \"1\"]";

        service.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        service.setProperty(OutputKeys.INDENT, "no");

        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<element><complexType><attribute name=\"design\" fixed=\"1\"/></complexType></element>"));
            }
        }
    }

    
    /**
     * @see <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1488303&group_id=17691&atid=117691">http://sourceforge.net/tracker/index.php?func=detail&aid=1488303&group_id=17691&atid=117691</a>
     */
    @Test
    public void predicate_bug1488303() throws XMLDBException {
        final XQueryService service = getQueryService();
        
        // test one
        final String xQuery1 = "let $q := <q><t>eXist</t></q> return $q//t";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery1)) {
            assertEquals("nr of results", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("result", "<t>eXist</t>",
                    resource.getContent().toString());
            }
        }

        // test two
        final String xQuery2 = "let $q := <q><t>eXist</t></q> return ($q//t)[1]";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery2)) {
            assertEquals("nr of results", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("result", "<t>eXist</t>",
                    resource.getContent().toString());
            }
        }

        // This one fails http://sourceforge.net/tracker/index.php?func=detail&aid=1488303&group_id=17691&atid=117691
        final String xQuery3 = "let $q := <q><t>eXist</t></q> return $q//t[1]";
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery3)) {
            assertEquals("SFBUG 1488303 nr of results", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("SFBUG 1488303 result", "<t>eXist</t>",
                    resource.getContent().toString());
            }
        }
    }

    
    /**
     * @see <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1460791&group_id=17691&atid=117691">http://sourceforge.net/tracker/index.php?func=detail&aid=1460791&group_id=17691&atid=117691</a>
     */
    @Test
    public void descendantOrSelf_bug1460791() throws XMLDBException {
        final String xQuery = "declare option exist:serialize \"method=xml indent=no\"; let $test:=<z><a>aaa</a><z>zzz</z></z> "
                +"return ( <one>{$test//z}</one>, <two>{$test/descendant-or-self::node()/child::z}</two> )";
        
        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {

//        System.out.println("BUG1460791/1" + rs.getResource(0).getContent().toString() );
//        System.out.println("BUG1460791/2" + rs.getResource(1).getContent().toString() );

            assertEquals("SFBUG 1460791 nr of results", 2, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                assertEquals("SFBUG 1460791 result part 1", "<one><z>zzz</z></one>",
                    resource.getContent().toString());
            }

            try (final Resource resource = result.getResource(1)) {
                assertEquals("SFBUG 1460791 result part 2", "<two><z>zzz</z></two>",
                    resource.getContent().toString());
            }
        }
    }
    
    /**
     * @see <a href="http://sourceforge.net/tracker/index.php?func=detail&aid=1462120&group_id=17691&atid=1176">http://sourceforge.net/tracker/index.php?func=detail&aid=1462120&group_id=17691&atid=1176</a>
     */
    @Test
    public void xpath_bug1462120() throws XMLDBException {
        final String xQuery = "declare option exist:serialize \"method=xml indent=no\"; "
                +"let $m:=<Units><Unit name=\"g\" size=\"1\"/>"
                +"<Unit name=\"kg\" size=\"1000\"/></Units> "
                +"let $list:=(<Product aaa=\"g\"/>, <Product aaa=\"kg\"/>) "
                +"let $one:=$list[1] return ( "
                +"$m/Unit[string(data(@name)) eq string(data($list[1]/@aaa))],"
                +"<br/>,$m/Unit[string(data(@name)) eq string(data($one/@aaa))] )";
        
        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {

            assertEquals("SFBUG 1462120 nr of results", 3, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                assertEquals("SFBUG 1462120 result part 1", "<Unit name=\"g\" size=\"1\"/>",
                    resource.getContent().toString());
            }

            try (final Resource resource = result.getResource(1)) {
                assertEquals("SFBUG 1462120 result part 2", "<br/>",
                    resource.getContent().toString());
            }

            try (final Resource resource = result.getResource(2)) {
                assertEquals("SFBUG 1462120 result part 3", "<Unit name=\"g\" size=\"1\"/>",
                    resource.getContent().toString());
            }
        }
    }
    
    
    /**
     * In Predicate.java, the contextSet and the outerSequence.toNodeSet()
     * documents are different so that no match can occur.
     *
     * @see <a href="http://wiki.exist-db.org/space/XQueryBugs">http://wiki.exist-db.org/space/XQueryBugs</a>
     */
    @Test
    public void predicate_bug_wiki_1() throws XMLDBException {
        final String xQuery = "let $dum := <dummy><el>1</el><el>2</el></dummy> return $dum/el[2]";
        
        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {

            assertEquals("Predicate bug wiki_1", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("Predicate bug wiki_1", "<el>2</el>",
                    resource.getContent().toString());
            }
        }
    }

    @Test
    public void predicate_bug_andrzej() throws XMLDBException {
        final String xQuery =
            "doc('/db/test/predicates.xml')//elem1/elem2[ string-length( ./elem3 ) > 0][1]/elem3/text()";
        final XQueryService service =
            storeXMLStringAndGetQueryService("predicates.xml", predicates);
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {
            assertEquals("testPredicateBUGAndrzej", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("testPredicateBUGAndrzej", "val1", resource.getContent().toString());
            }
        }
    }

    /**
     * removing Self: makes the query work OK
     * @see <a href="http://wiki.exist-db.org/space/XQueryBugs">http://wiki.exist-db.org/space/XQueryBugs</a>
     */
    @Test
    public void cardinalitySelf_bug_wiki_2() throws XMLDBException {
        final String xQuery = "let $test := <test><works><employee>a</employee><employee>b</employee></works></test> "
                + "for $h in $test/works/employee[2] return fn:name($h/self::employee)";

        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {
            assertEquals("CardinalitySelfBUG bug wiki_2", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("CardinalitySelfBUG bug wiki_2", "employee",
                    resource.getContent().toString());
            }
        }
    }
    
    /**
     * Problem in VirtualNodeSet which return 2 attributes because it 
     * computes every level
     * @see <a href="http://wiki.exist-db.org/space/XQueryBugs">http://wiki.exist-db.org/space/XQueryBugs</a>
     */
    @Test
    public void virtualNodeset_bug_wiki_3() throws XMLDBException {
        final String xQuery = "declare option exist:serialize \"method=xml indent=no\"; "
                + "let $node := (<c id=\"OK\"><b id=\"cool\"/></c>)"
                + "/descendant::*/attribute::id return <a>{$node}</a>";

        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {

            assertEquals("VirtualNodesetBUG_wiki_3", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("VirtualNodesetBUG_wiki_3", "<a id=\"cool\"/>",
                    resource.getContent().toString());
            }
        }
    } 
    
    /**
     * Problem in VirtualNodeSet because it computes the wrong level
     *
     * @see <a href="http://wiki.exist-db.org/space/XQueryBugs">http://wiki.exist-db.org/space/XQueryBugs</a>
     */
    @Test
    public void virtualNodeset_bug_wiki_4() throws XMLDBException {
        final String xQuery = "declare option exist:serialize \"method=xml indent=no\"; "
                + "let $node := (<c id=\"OK\">"
                + "<b id=\"cool\"/></c>)/descendant-or-self::*/child::b "
                + "return <a>{$node}</a>";

        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {
            assertEquals("VirtualNodesetBUG_wiki_4", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("VirtualNodesetBUG_wiki_4", "<a><b id=\"cool\"/></a>",
                    resource.getContent().toString());
            }
        }
    } 
    
    /**
     * Problem in VirtualNodeSet because it computes the wrong level
     *
     * @see <a href="http://wiki.exist-db.org/space/XQueryBugs">http://wiki.exist-db.org/space/XQueryBugs</a>
     */
    @Test
    public void virtualNodeset_bug_wiki_5() throws XMLDBException {
        final String xQuery = "declare option exist:serialize \"method=xml indent=no\"; "
                + "let $node := (<c id=\"OK\"><b id=\"cool\"/>"
                + "</c>)/descendant-or-self::*/descendant::b return <a>{$node}</a>";

        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {
            assertEquals("VirtualNodesetBUG_wiki_5", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("VirtualNodesetBUG_wiki_5", "<a><b id=\"cool\"/></a>",
                    resource.getContent().toString());
            }
        }
    } 
    
    // It seems that the document builder receives events that are irrelevant.
    @Test
    public void documentBuilder_bug_wiki_6() throws XMLDBException {
        final String xQuery = "declare option exist:serialize \"method=xml indent=no\"; "
                + "declare function local:test() {let $results := <dummy/>"
                + "return \"id\" }; "
                + "<wrapper><string id=\"{local:test()}\"/></wrapper>";

        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {
            assertEquals("testDocumentBuilderBUG_wiki_6", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("testDocumentBuilderBUG_wiki_6", "<wrapper><string id=\"id\"/></wrapper>",
                    resource.getContent().toString());
            }
        }
    }
    
    @Test
    public void castInPredicate_bug_wiki_7() throws XMLDBException {
        final String xQuery = "let $number := 2, $list := (\"a\", \"b\", \"c\") return $list[xs:int($number * 2) - 1]";

        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {
            assertEquals("testCalculationInPredicate_wiki_7", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("testCalculationInPredicate_wiki_7", "c",
                    resource.getContent().toString());
            }
        }
     }
     
     /**
      * Miscomputation of the expression context in where clause when no 
      * wrapper expression is used. Using, e.g. where data($x/@id) eq "id" works !
      */
    @Test
    public void computation_bug_wiki_8() throws XMLDBException {
        final String xQuery = "declare option exist:serialize \"method=xml indent=no\"; "
                 + "let $a := element node1 { attribute id {'id'}, "
                 + "element node1 { '1'},element node2 { '2'} }"
                 + "for $x in $a where $x/@id eq \"id\" return $x";

        final XQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(xQuery)) {
            assertEquals("testComputationBug_wiki_8", 1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("testComputationBug_wiki_8", "<node1 id=\"id\"><node1>1</node1><node2>2</node2></node1>",
                    resource.getContent().toString());
            }
        }
    }

    @Test
    public void strings() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("strings.xml", strings);

        try (final EXistResourceSet result = queryResource(service, "strings.xml", "substring(/test/string[1], 1, 5)", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("Hello", resource.getContent().toString());
            }
        }

        queryResourceV(service, "strings.xml", "/test/string[starts-with(string(.), 'Hello')]", 2);

        try (final EXistResourceSet result = queryResource(service, "strings.xml", "count(/test/item/price)", 1,
                "Query should return an empty set (wrong document)")) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("0", resource.getContent().toString());
            }
        };
    }

    @Test
    public void quotes() throws XMLDBException {

        final XQueryService service =
                storeXMLStringAndGetQueryService("quotes.xml", quotes);

        queryResourceV(service, "quotes.xml", "/test[title = '&quot;Hello&quot;']", 1);

        service.declareVariable("content", "&quot;Hello&quot;");
        queryResourceV(service, "quotes.xml", "declare variable $content as xs:string external; /test[title = $content]", 1);
    }

    @Test
    public void booleans() throws XMLDBException {

        final XQueryService service =
                storeXMLStringAndGetQueryService("numbers.xml", numbers);

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(1.0)", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of 1.0 should be true", "true", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(0.0)", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of 0.0 should be false", "false", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(xs:double(0.0))", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of double 0.0 should be false", "false", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(xs:double(1.0))", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of double 1.0 should be true", "true", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(xs:float(1.0))", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of float 1.0 should be true", "true", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(xs:float(0.0))", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of float 0.0 should be false", "false", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(xs:integer(0))", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of integer 0 should be false", "false", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(xs:integer(1))", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of integer 1 should be true", "true", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "'true' cast as xs:boolean", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of 'true' cast to xs:boolean should be true",
                    "true", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "'false' cast as xs:boolean", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of 'false' cast to xs:boolean should be false",
                    "false", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean('Hello')", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of string 'Hello' should be true", "true", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean('')", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of empty string should be false", "false", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(())", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of empty sequence should be false", "false", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(('Hello'))", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of sequence with non-empty string should be true",
                    "true", resource.getContent().toString());
            }
        }

//			result = queryResource(service, "numbers.xml", "boolean((0.0, 0.0))", 1);
//			assertEquals("boolean value of sequence with two elements should be true", "true",
//					result.getResource(0).getContent());

        try (final EXistResourceSet result = queryResource(service, "numbers.xml", "boolean(//item[@id = '1']/price)", 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("boolean value of 5.6 should be true", "true",
                    resource.getContent().toString());
            }
        }

        String message = "";
        try {
            queryResourceV(service, "numbers.xml", "boolean(current-time())", 1);
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("FORG0006") > -1);
    }

    @Test
    public void not() throws XMLDBException {

        final XQueryService service =
                storeXMLStringAndGetQueryService("strings.xml", strings);

        queryResourceV(service, "strings.xml", "/test/string[not(@value)]", 2);

        try (final EXistResourceSet result = queryResource(service, "strings.xml",	"not(/test/abcd)", 1);
             final Resource r = result.getResource(0)) {
            assertEquals("true", r.getContent().toString());
        }

        try (final EXistResourceSet result = queryResource(service, "strings.xml",	"not(/test)", 1);
             final Resource r = result.getResource(0)) {
            assertEquals("false", r.getContent().toString());
        }

        try (final EXistResourceSet result = queryResource(service, "strings.xml", "/test/string[not(@id)]", 3);
             final Resource r = result.getResource(0)) {
            assertEquals("<string>Hello World!</string>", r.getContent().toString());
        }

        // test with non-existing items
        queryResourceV(service, "strings.xml", "/blah[not(blah)]", 0);
        queryResourceV(service, "strings.xml", "//*[string][not(@value)]", 1);
        queryResourceV(service, "strings.xml", "//*[string][not(@blah)]", 1);
        queryResourceV(service, "strings.xml", "//*[blah][not(@blah)]", 0);
    }

    @Test
    public void logicalOr() throws XMLDBException, IOException, SAXException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("strings.xml", strings);

        try (final EXistResourceSet result = queryResource(service, "strings.xml",	"<test>{() or ()}</test>", 1);
             final Resource resource = result.getResource(0)) {
            assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<test>false</test>"));
        }

        try (final EXistResourceSet result = queryResource(service, "strings.xml",	"() or ()", 1);
             final Resource resource = result.getResource(0)) {
            assertEquals("false", resource.getContent().toString());
        }
    } 
    
    @Test
    public void logicalAnd() throws XMLDBException, IOException, SAXException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("strings.xml", strings);

        try (final EXistResourceSet result = queryResource(service, "strings.xml",	"<test>{() and ()}</test>", 1);
             final Resource resource = result.getResource(0)) {
            assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<test>false</test>"));
        }

        try (final EXistResourceSet result = queryResource(service, "strings.xml",	"() and ()", 1);
             final Resource resource = result.getResource(0)) {
            assertEquals("false", resource.getContent().toString());
        }
    }     
    
    @Test
    public void ids_persistent() throws XMLDBException {
        final XQueryService service =
                storeXMLStringAndGetQueryService("ids.xml", ids);

        queryResourceV(service, "ids.xml", "//a/id(@ref)", 1);
        queryResourceV(service, "ids.xml", "/test/id(//a/@ref)", 1);

        try (final EXistResourceSet result = queryResource(service, "ids.xml", "//a/id(@ref)/name", 1);
             final Resource r = result.getResource(0)) {
            assertEquals("<name>one</name>", r.getContent().toString());
        }

        try (final EXistResourceSet result = queryResource(service, "ids.xml", "//d/id(@ref)/name", 1);
             final Resource r = result.getResource(0)) {
            assertEquals("<name>two</name>", r.getContent().toString());
        }

        String update = "update insert <t xml:id=\"id3\">Hello</t> into /test";
        queryResourceV(service, "ids.xml", update, 0);

        queryResourceV(service, "ids.xml", "/test/id('id3')", 1);

        update = "update value //t/@xml:id with 'id4'";
        queryResourceV(service, "ids.xml", update, 0);
        queryResourceV(service, "ids.xml", "id('id4', /test)", 1);
    }

    @Ignore("Not yet supported in eXist")
    @Test
    public void ids_memtree() throws XMLDBException {
        final XQueryService service = getQueryService();

        try (final EXistResourceSet result = (EXistResourceSet) service.query("document { " + ids_content + " }//a/id(@ref)")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("document { " + ids_content + " }/test/id(//a/@ref)")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("document { " + ids_content + " }//a/id(@ref)/name")) {
            assertEquals(1, result.getSize());
            try (final Resource r = result.getResource(0)) {
                assertEquals("<name>one</name>", r.getContent().toString());
            }
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("document { " + ids_content + " }//d/id(@ref)/name");
             final Resource r = result.getResource(0)) {
            assertEquals("<name>two</name>", r.getContent().toString());
        }
    }
    
    @Test
    public void idsOnEmptyCollection() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(getBaseUri(), "admin", "")) {
            final CollectionManagementService service = root.getService(CollectionManagementService.class);
            try (final Collection emptyCollection = service.createCollection("empty")) {
                final XQueryService queryService = (XQueryService) emptyCollection.getService(XPathQueryService.class);
                queryAndAssert(queryService, "/*", 0, null);
                queryAndAssert(queryService, "/id('foo')", 0, null);
            }
        }
    }
    
    @Test
    public void idRefs_persistent() throws XMLDBException {
       final XQueryService service =
          storeXMLStringAndGetQueryService("ids.xml", ids);
  
       queryResourceV(service, "ids.xml", "/idref('id2')", 1);
       queryResourceV(service, "ids.xml", "/idref('id1')", 2);
       queryResourceV(service, "ids.xml", "/idref(('id2', 'id1'))", 3);
       queryResourceV(service, "ids.xml", "<results>{/idref('id2')}</results>", 1);
    }

    @Ignore("Not yet supported in eXist")
    @Test
    public void idRefs_memtree() throws XMLDBException {
        final XQueryService service = getQueryService();

        try (final EXistResourceSet result = (EXistResourceSet) service.query("document {" + ids_content + "}/idref('id2')")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("document {" + ids_content + "}/idref('id1')")) {
            assertEquals(2, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("document {" + ids_content + "}/idref(('id2', 'id1'))")) {
            assertEquals(3, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.query("let $doc := document {" + ids_content + "} return <results>{$doc/idref('id2')}</results>")) {
            assertEquals(1, result.getSize());
        }
    }
    
    @Test
    public void externalVars() throws XMLDBException {
        final XQueryService service1 = storeXMLStringAndGetQueryService("strings.xml", strings);

        String query =
            "declare variable $x external;" +
            "$x";
        final CompiledExpression expr1 = service1.compile(query);
        //Do not declare the variable...
        assertThrows("Expected XPTY0002", XMLDBException.class, () -> {
            try (final EXistResourceSet result = (EXistResourceSet) service1.execute(expr1)) {
                // needed to ensure that reesult is closed
            }
        });

        query =
            "declare variable $local:string external;" +
            "/test/string[. = $local:string]";
        final CompiledExpression expr2 = service1.compile(query);
        service1.declareVariable("local:string", "Hello");

        try (final EXistResourceSet result = (EXistResourceSet) service1.execute(expr2);
             final XMLResource r = (XMLResource) result.getResource(0)) {
            Node node = r.getContentAsDOM();
            if (node.getNodeType() == Node.DOCUMENT_NODE) {
                node = node.getFirstChild();
            }
            assertEquals("string", node.getNodeName());
        }

        //Instanciate a new service to prevent variable reuse
        //TODO : consider auto-reset ?
        final XQueryService service2 = storeXMLStringAndGetQueryService("strings.xml", strings);

        query =
            "declare variable $local:string as xs:string external;" +
            "$local:string";
        final CompiledExpression expr3 = service2.compile(query);
        //TODO : we should virtually pass any kind of value
        service2.declareVariable("local:string", Integer.valueOf(1));

        String message = "";
        try {
            service2.execute(expr3);
        } catch (XMLDBException e) {
            //e.printStackTrace();
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XPTY0004") > -1);

        final XQueryService service3 = storeXMLStringAndGetQueryService("strings.xml", strings);

        query =
            "declare variable $x as xs:integer external; " +
            "$x";
        final CompiledExpression expr4 = service3.compile(query);
        //TODO : we should virtually pass any kind of value
        service3.declareVariable("x", "1");

        message = "";
        try {
            service3.execute(expr4);
        } catch (final XMLDBException e) {
            //e.printStackTrace();
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XPTY0004") > -1);
    }
    
    @Test
    public void externalVars2() throws ParserConfigurationException, IOException, SAXException, XMLDBException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final InputSource source = new InputSource(new StringReader(strings));
        final Document doc = builder.parse(source);

        final XQueryService service = testCollection.getService(XQueryService.class);
        final CompiledExpression expr = service.compile("declare variable $local:node external; $local:node//string");
        service.declareVariable("local:node", doc.getDocumentElement());
        try (final EXistResourceSet result = (EXistResourceSet) service.execute(expr)) {
            assertEquals(3, result.getSize());
        }
    }

    @Test
    public void queryResource() throws XMLDBException {
        try (final XMLResource doc = testCollection.createResource("strings.xml", XMLResource.class)) {
            doc.setContent(strings);
            testCollection.storeResource(doc);
        }

        try (final XMLResource doc = testCollection.createResource("strings2.xml", XMLResource.class)) {
            doc.setContent(strings);
            testCollection.storeResource(doc);
        }

        final XPathQueryService query = testCollection.getService(XPathQueryService.class);

        try (final EXistResourceSet result = (EXistResourceSet) query.queryResource("strings2.xml", "/test/string[. = 'Hello World!']")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) query.query("/test/string[. = 'Hello World!']")) {
            assertEquals(2, result.getSize());
        }
    }
    
    /**
     * test involving ancestor::
     * >>>>>>> currently this produces variable corruption :
     * 			The result is the ancestor <<<<<<<<<<
     */
    @Test
    public void ancestor() throws XMLDBException {
        final XQueryService service = storeXMLStringAndGetQueryService("numbers.xml", numbers);

        final String query =
                "let $all_items := /test/item " +

                "(: Note: variable non used but computed anyway :)" +
                "let $unused_variable :=" +
                "	for $one_item in $all_items " +
                "			/ ancestor::*	(: <<<<< if you remove this line all is normal :)" +
                "		return 'foo'" +
                "return $all_items";

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("numbers.xml", query)) {
            assertEquals(4, result.getSize());
        }
    }

    @Test
    public void namespaces() throws XMLDBException {
        final XQueryService service = storeXMLStringAndGetQueryService("namespaces.xml", namespaces);

        service.setNamespace("t", "http://www.foo.com");
        service.setNamespace("c", "http://www.other.com");

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", "//t:section")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", "/t:test//c:comment")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", "//c:*")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", "//*:comment")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("namespaces.xml", "namespace-uri(//t:test)")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("http://www.foo.com", resource.getContent().toString());
            }
        }
    }

    @Test
    public void preserveSpace() throws XMLDBException, IOException, SAXException {
        final XQueryService service = storeXMLStringAndGetQueryService("whitespace.xml", ws);

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("whitespace.xml", "//text")) {
            assertEquals(2, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final String item = resource.getContent().toString();
                assertThat(item, CompareMatcher.isIdenticalTo("<text> </text>"));
            }

            try (final Resource resource = result.getResource(1)) {
                final String item = resource.getContent().toString();
                assertThat(item, CompareMatcher.isIdenticalTo("<text xml:space=\"default\"> </text>"));
            }
        }
    }
    
    @Test
    public void nestedElements() throws XMLDBException {
        final XQueryService service = storeXMLStringAndGetQueryService("nested.xml", nested);

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("nested.xml", "//c")) {
            assertEquals(3, result.getSize());
        }
    }
    
    @Test
    public void staticVariables() throws XMLDBException {
        final XPathQueryService service = testCollection.getService(XPathQueryService.class);

        try (final XMLResource doc = testCollection.createResource("numbers.xml", XMLResource.class)) {
            doc.setContent(numbers);
            testCollection.storeResource(doc);

            final EXistXPathQueryService service2 = (EXistXPathQueryService) service;
            service2.declareVariable("name", "MONTAGUE");
            try (final EXistResourceSet result = (EXistResourceSet) service.query("declare variable $name as xs:string external; //SPEECH[SPEAKER = $name]")) {
                assertEquals(0, result.getSize());
            }

            service2.declareVariable("name", "43");
            try (final EXistResourceSet result = service2.query(doc, "declare variable $name as xs:string external; //item[stock = $name]")) {
                assertEquals(1, result.getSize());
            }

            try (final EXistResourceSet result = (EXistResourceSet) service2.query("declare variable $name as xs:string external; $name")) {
                assertEquals(1, result.getSize());
            }

            service2.clearVariables();

            try (final EXistResourceSet result = service2.query(doc, "//item[stock = 43]")) {
                assertEquals(1, result.getSize());
            }

            try (final EXistResourceSet result = service2.query(doc, "//item")) {
                assertEquals(4, result.getSize());
            }
        }
    }

    @Test
    public void membersAsResource() throws XMLDBException, IOException {
//			XPathQueryService service =
//				(XPathQueryService) testCollection.getService(
//					"XPathQueryService",
//					"1.0");
//			ResourceSet result = service.query("//SPEECH[LINE &= 'marriage']");
        final XQueryService service = storeXMLStringAndGetQueryService("numbers.xml", numbers);
        try (final EXistResourceSet result = (EXistResourceSet) service.query("//item/price");
             final Resource r = result.getMembersAsResource()) {
            final Object rawContent = r.getContent();
            final String content;
            if (rawContent instanceof File) {
                final Path p = ((File) rawContent).toPath();
                content = new String(Files.readAllBytes(p), UTF_8);
            } else {
                content = (String) r.getContent();
            }

            final Pattern p = Pattern.compile(".*(<price>.*){4}", Pattern.DOTALL);
            final Matcher m = p.matcher(content);
            assertTrue("get whole document numbers.xml", m.matches());
        }
    }

    @Test
    public void satisfies() throws XMLDBException {
        final XQueryService service = getQueryService();

        try (final EXistResourceSet result = queryAndAssert(service,
                "every $foo in (1,2,3) satisfies" +
                "   let $bar := 'baz'" +
                "       return false() ",
                1,  "")) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("satisfies + FLWR expression allways false 1", "false", resource.getContent().toString());
            }
        }

        try (final EXistResourceSet result = queryAndAssert(service,
                "declare function local:foo() { false() };" +
                "   every $bar in (1,2,3) satisfies" +
                "   local:foo()",
                1,  "")) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("satisfies + FLWR expression allways false 2", "false", resource.getContent().toString());
            }
        }

        String query = "every $x in () satisfies false()";
        try (final EXistResourceSet result = queryAndAssert(service, query, 1,  "")) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(query, "true", resource.getContent().toString());
            }
        }

        query = "some $x in () satisfies true()";
        try (final EXistResourceSet result = queryAndAssert(service, query, 1,  "")) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(query, "false", resource.getContent().toString());
            }
        }
    }
    
    @Test
    public void intersect() throws XMLDBException {
        final XQueryService service = getQueryService();

        String query = "()  intersect ()";
        queryAndAssertV(service, query, 0, "");

        query = "()  intersect  (1)";
        queryAndAssertV(service, query, 0, "");

        query = "(1)  intersect  ()";
        queryAndAssertV(service, query, 0, "");
    }
    
    @Test
    public void union() throws XMLDBException {
        final XQueryService service = getQueryService();

        String query = "()  union ()";
        queryAndAssertV( service, query, 0, "");

        String message = "";
        try {
            query = "()  union  (1)";
            queryAndAssertV( service, query, 0, "");
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XPTY0004") > -1);

        message = "";
        try {
            query = "(1)  union  ()";
            queryAndAssertV(service, query, 0, "");
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XPTY0004") > -1);

        query = "<a/>  union ()";
        queryAndAssertV(service, query, 1, "");
        query = "()  union <a/>";
        queryAndAssertV(service, query, 1, "");
        //Not the same nodes
        query = "<a/> union <a/>";
        queryAndAssertV(service, query, 2, "");
    }

    @Test
    public void except() throws XMLDBException {
        final XQueryService service = getQueryService();

        String query = "()  except ()";
        queryAndAssertV(service, query, 0, "");

        query = "()  except  (1)";
        queryAndAssertV(service, query, 0, "");

        String message = "";
        try {
            query = "(1)  except  ()";
            queryAndAssertV(service, query, 0, "");
        } catch (XMLDBException e) {
            message = e.getMessage();
        }
        assertTrue(message.indexOf("XPTY0004") > -1);

        query = "<a/>  except ()";
        queryAndAssertV(service, query, 1, "");
        query = "()  except <a/>";
        queryAndAssertV(service, query, 0, "");
        //Not the same nodes
        query = "<a/> except <a/>";
        queryAndAssertV(service, query, 1, "");
    }

    @Test
    public void convertToBoolean() throws XMLDBException {
        final XQueryService service = getQueryService();


        try (final EXistResourceSet result = queryAndAssert(
                service,
                "let $doc := <element attribute=''/>" + "return ("
                + "  <true>{boolean(($doc,2,3))}</true> ,"
                + "  <true>{boolean(($doc/@*,2,3))}</true> ,"
                + "  <true>{boolean(true())}</true> ,"
                + "  <true>{boolean('test')}</true> ,"
                + "  <true>{boolean(number(1))}</true> ,"
                + "  <false>{boolean((0))}</false> ,"
                + "  <false>{boolean(false())}</false> ,"
                + "  <false>{boolean('')}</false> ,"
                + "  <false>{boolean(number(0))}</false> ,"
                + "  <false>{boolean(number('NaN'))}</false>" + ")",
                10, "")) {

            for (int i = 0; i < 10; i++) {
                try (final Resource resource = result.getResource(i)) {
                    if (i < 5) {
                        assertEquals("true " + (i + 1), "<true>true</true>",
                            resource.getContent().toString());
                    } else {
                        assertEquals("false " + (i + 1), "<false>false</false>",
                            resource.getContent().toString());
                    }
                }
            }
        }
        
        boolean exceptionThrown = false;
        String message = "";
        try {
            queryAndAssertV(service,
                    "let $doc := <element attribute=''/>"
                    + " return boolean( (1,2,$doc) )", 1, "");
        } catch (XMLDBException e) {
            exceptionThrown = true;
            message = e.getMessage();
        }
        assertTrue("Exception wanted: " + message, exceptionThrown);
    }

    @Test
    public void compile() throws XMLDBException {
        final String invalidQuery = "for $i in (1 to 10)\n return $b";
        final String validQuery = "for $i in (1 to 10) return $i";
        final String validModule = "module namespace foo=\"urn:foo\";\n" +
                "declare function foo:test() { \"Hello World!\" };";
        final String invalidModule = "module namespace foo=\"urn:foo\";\n" +
                "declare function foo:test() { \"Hello World! };";
        
        final EXistXQueryService service = (EXistXQueryService) getQueryService();
        boolean exceptionOccurred = false;
        try {
            service.compile(invalidQuery);
        } catch (XMLDBException e) {
            assertEquals(((XPathException)e.getCause()).getLine(), 2);
            exceptionOccurred = true;
        }
        assertTrue("Expected an exception", exceptionOccurred);
        
        exceptionOccurred = false;
        try {
            service.compileAndCheck(invalidModule);
        } catch (XPathException e) {
            exceptionOccurred = true;
        }
        assertTrue("Expected an exception", exceptionOccurred);

        service.compile(validQuery);
        service.compile(validModule);
    }
    
    /**
     * Added by Geoff Shuetrim on 15 July 2006 (geoff@galexy.net).
     * This test has been added following identification of a problem running
     * XPath queries that involved the name of elements with the name 'xpointer'.
     * @throws XMLDBException
     */
    @Ignore
    @Test
    public void xpointerElementNameHandling() throws XMLDBException {
        final XQueryService service = storeXMLStringAndGetQueryService(
                "xpointer.xml", xpointerElementName);

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("xpointer.xml",
                "/test/.[local-name()='xpointer']")) {
            assertEquals(1, result.getSize());
        }

        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource("xpointer.xml", "/test/xpointer")) {
            assertEquals(1, result.getSize());
        }
    }

    @Test
    public void atomization() throws XMLDBException, IOException, SAXException {
        final String query =
                "declare namespace ex = \"http://example.org\";\n" +
                "declare function ex:elementName() as xs:QName {\n" +
                "   QName(\"http://test.org\", \"test:name\")\n" +
                "};\n" +
                "<test>{\n" +
                "   element {QName(\"http://test.org\", \"test:name\") }{ 'a' },\n" +
                "   element {ex:elementName()} { 'b' }\n" +
                "}</test>";

        final EXistXQueryService service = (EXistXQueryService) getQueryService();
        service.setProperty(OutputKeys.INDENT, "no");
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertThat(resource.getContent().toString(), CompareMatcher.isIdenticalTo("<test><test:name xmlns:test=\"http://test.org\">a</test:name><test:name xmlns:test=\"http://test.org\">b</test:name></test>"));
            }
        }
    }

    @Test
    public void substring() throws XMLDBException {
        final XQueryService service = getQueryService();

        // Test cases by MIKA
        final String validQuery = "substring(\"MK-1234\", 4,1)";
        try (final EXistResourceSet result = queryAndAssert(service, validQuery, 1, validQuery)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("1", resource.getContent().toString());
            }
        }

        String invalidQuery = "substring(\"MK-1234\", 4,4)";
        try (final EXistResourceSet result = queryAndAssert(service, invalidQuery, 1, invalidQuery)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("1234", resource.getContent().toString());
            }
        }

        // Test case by Toar
        final String toarQuery="let $num := \"2003.123\" \n return substring($num, 1, 7)";
        try (final EXistResourceSet result = queryAndAssert(service, toarQuery, 1, toarQuery)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("2003.12", resource.getContent().toString());
            }
        }
    }

    @Test
    public void cdataPersistentDom() throws XMLDBException {
        final String docName = "cdata.xml";

        final XQueryService service =
                storeXMLStringAndGetQueryService(docName, cdata_xml);

        String query = "/elem1";
        try (final EXistResourceSet result = queryResource(service, docName, query, 1)) {
            final String expected = "<elem1>" + cdata_content.replace("<", "&lt;").replace(">", "&gt;") + "</elem1>";
            try (final Resource resource = result.getResource(0)) {
                assertEquals(expected, resource.getContent().toString());
            }
        }

        query =
                "declare namespace output = \"http://www.w3.org/2010/xslt-xquery-serialization\";\n" +
                "declare option output:cdata-section-elements \"elem1\";\n" +
                "/elem1\n";
        try (final EXistResourceSet result = queryResource(service, docName, query, 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(cdata_xml, resource.getContent().toString());
            }
        }

        query =
                "fn:serialize(doc(\"/db/test/" + docName + "\"),\n" +
                    "<output:serialization-parameters xmlns:output = \"http://www.w3.org/2010/xslt-xquery-serialization\">\n" +
                    "    <output:method value=\"xml\"/>\n" +
                    "    <output:cdata-section-elements value=\"elem1\"/>\n" +
                    "</output:serialization-parameters>)";
        try (final EXistResourceSet result = queryResource(service, docName, query, 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(cdata_xml, resource.getContent().toString());
            }
        }

        query =
                "fn:serialize(doc(\"/db/test/" + docName + "\"),\n" +
                        "map {\n" +
                        "    \"method\": \"xml\",\n" +
                        "    \"cdata-section-elements\": xs:QName(\"elem1\")\n" +
                        "})";
        try (final EXistResourceSet result = queryResource(service, docName, query, 1)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(cdata_xml, resource.getContent().toString());
            }
        }
    }

    @Test
    public void cdataMemtreeDom() throws XMLDBException, IOException {
        final String docName = "cdata.xml";
        final Path tempFile = tempFolder.newFile().toPath();
        Files.write(tempFile, Arrays.asList(cdata_xml));

        final XQueryService service = getQueryService();

        String query = "doc(\"" + tempFile.toUri() + "\")";
        try (final EXistResourceSet result = queryAndAssert(service, query, 1, null)) {
            final String expected = "<elem1>" + cdata_content.replace("<", "&lt;").replace(">", "&gt;") + "</elem1>";
            try (final Resource resource = result.getResource(0)) {
                assertEquals(expected, resource.getContent().toString());
            }
        }

        query =
                "declare namespace output = \"http://www.w3.org/2010/xslt-xquery-serialization\";\n" +
                "declare option output:cdata-section-elements \"elem1\";\n" +
                "doc(\"" + tempFile.toUri().toString() + "\")\n";
        try (final EXistResourceSet result = queryAndAssert(service, query, 1, null)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(cdata_xml, resource.getContent().toString());
            }
        }

        query =
                "fn:serialize(doc(\"" + tempFile.toUri().toString() + "\"),\n" +
                        "<output:serialization-parameters xmlns:output = \"http://www.w3.org/2010/xslt-xquery-serialization\">\n" +
                        "    <output:method value=\"xml\"/>\n" +
                        "    <output:cdata-section-elements value=\"elem1\"/>\n" +
                        "</output:serialization-parameters>)";
        try (final EXistResourceSet result = queryAndAssert(service, query, 1, null)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(cdata_xml, resource.getContent().toString());
            }
        }

        query =
                "fn:serialize(doc(\"" + tempFile.toUri().toString() + "\"),\n" +
                        "map {\n" +
                        "    \"method\": \"xml\",\n" +
                        "    \"cdata-section-elements\": xs:QName(\"elem1\")\n" +
                        "})";
        try (final EXistResourceSet result = queryAndAssert(service, query, 1, null)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(cdata_xml, resource.getContent().toString());
            }
        }
    }

    @Test
    public void cdataComputedDom() throws XMLDBException {
        final XQueryService service = getQueryService();

        String query =
                "document {\n" +
                    cdata_xml + "\n" +
                "}";
        try (final EXistResourceSet result = queryAndAssert(service, query, 1, null)) {
            final String expected = "<elem1>" + cdata_content.replace("<", "&lt;").replace(">", "&gt;") + "</elem1>";
            try (final Resource resource = result.getResource(0)) {
                assertEquals(expected, resource.getContent().toString());
            }
        }

        query =
                "declare namespace output = \"http://www.w3.org/2010/xslt-xquery-serialization\";\n" +
                "declare option output:cdata-section-elements \"elem1\";\n" +
                "document {\n" +
                    cdata_xml + "\n" +
                "}";
        try (final EXistResourceSet result = queryAndAssert(service, query, 1, null)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(cdata_xml, resource.getContent().toString());
            }
        }

        query =
                "fn:serialize(document {" + cdata_xml + "},\n" +
                        "<output:serialization-parameters xmlns:output = \"http://www.w3.org/2010/xslt-xquery-serialization\">\n" +
                        "    <output:method value=\"xml\"/>\n" +
                        "    <output:cdata-section-elements value=\"elem1\"/>\n" +
                        "</output:serialization-parameters>)";
        try (final EXistResourceSet result = queryAndAssert(service, query, 1, null)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(cdata_xml, resource.getContent().toString());
            }
        }

        query =
                "fn:serialize(document {" + cdata_xml + "},\n" +
                        "map {\n" +
                        "    \"method\": \"xml\",\n" +
                        "    \"cdata-section-elements\": xs:QName(\"elem1\")\n" +
                        "})";
        try (final EXistResourceSet result = queryAndAssert(service, query, 1, null)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals(cdata_xml, resource.getContent().toString());
            }
        }
    }


    /**
     * Helper that performs an XQuery and does JUnit assertion on result size.
     *
     * @see #queryResourceV(XQueryService, String, String, int, String)
     */
    private void queryResourceV(final XQueryService service, final String resource, final String query, final int expected) throws XMLDBException {
        queryResourceV(service, resource, query, expected, null);
    }

    /**
     * Helper that performs an XQuery and does JUnit assertion on result size.
     *
     * @see #queryResource(XQueryService, String, String, int, String)
     */
    private EXistResourceSet queryResource(final XQueryService service, final String resource, final String query, final int expected) throws XMLDBException {
        return queryResource(service, resource, query, expected, null);
    }

    /**
     * Helper that performs an XQuery and does JUnit assertion on result size.
     *
     * @see #queryResource(XQueryService, String, String, int, String)
     */
    private void queryResourceV(final XQueryService service, final String resource, final String query, final int expected, @Nullable final String message) throws XMLDBException {
        try (final EXistResourceSet result = queryResource(service, resource, query, expected, message)) {
            // needed to ensure that result is closed
        }
    }

    /**
     * Helper that performs an XQuery and does JUnit assertion on result size.
     *
     * @param service XQuery service
     * @param resource database resource (collection) to query
     * @param query
     * @param expected size of result
     * @param message for JUnit
     *
     * @return a ResourceSet, allowing to do more assertions if necessary.
     * @throws XMLDBException
     */
    private EXistResourceSet queryResource(final XQueryService service, final String resource, final String query, final int expected, @Nullable final String message) throws XMLDBException {
        final EXistResourceSet result = (EXistResourceSet) service.queryResource(resource, query);
        if(message == null) {
            assertEquals(query, expected, result.getSize());
        } else {
            assertEquals(message, expected, result.getSize());
        }
        return result;
    }

    private void queryAndAssertV(final XQueryService service, final String query, final int expected, @Nullable final String message) throws XMLDBException {
        try (final EXistResourceSet result = queryAndAssert(service, query, expected, message)) {
            // needed to ensure that result is closed
        }
    }
    
    /** For queries without associated data */
    private EXistResourceSet queryAndAssert(final XQueryService service, final String query, final int expected, @Nullable final String message) throws XMLDBException {
        final EXistResourceSet result = (EXistResourceSet) service.query(query);
        if(message == null) {
            assertEquals(expected, result.getSize());
        } else {
            assertEquals(message, expected, result.getSize());
        }
        return result;
    }

    /** For queries without associated data */
    private XQueryService getQueryService() throws XMLDBException {
        final XQueryService service = (XQueryService) testCollection.getService(
            XPathQueryService.class);
        return service;
    }

    /** stores XML String and get Query Service
     * @param documentName to be stored in the DB
     * @param content to be stored in the DB
     * @return the XQuery Service
     * @throws XMLDBException
     */
    private XQueryService storeXMLStringAndGetQueryService(final String documentName, final String content) throws XMLDBException {
        try (final XMLResource doc = testCollection.createResource(documentName, XMLResource.class)) {
            doc.setContent(content);
            testCollection.storeResource(doc);
        }
        return getQueryService();
    }
}
