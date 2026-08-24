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
package org.exist.xquery.functions.util;

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.xmldb.*;
import org.exist.xquery.ErrorCodes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

import org.exist.xquery.XPathException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

import org.xmldb.api.base.Collection;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import xyz.elemental.mediatype.MediaType;

/**
 *
 * @author jim.fuller@webcomposite.com
 */
//@Execution(ExecutionMode.CONCURRENT)
public class EvalTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    private static Resource invokableQuery;

    private final static String INVOKABLE_QUERY_FILENAME = "invokable.xql";
    private final static String INVOKABLE_QUERY_EXTERNAL_VAR_NAME = "some-value";

    @BeforeAll
    static void setUp() throws XMLDBException {
        invokableQuery = XMLDB_EMBEDDED_DATABASE.getRoot().createResource(INVOKABLE_QUERY_FILENAME, BinaryResource.class);
        invokableQuery.setContent(
            "declare variable $" + INVOKABLE_QUERY_EXTERNAL_VAR_NAME + " external;\n" + "<hello>{$" + INVOKABLE_QUERY_EXTERNAL_VAR_NAME + "}</hello>"
        );
        ((EXistResource) invokableQuery).setMediaType(MediaType.APPLICATION_XQUERY);
        XMLDB_EMBEDDED_DATABASE.getRoot().storeResource(invokableQuery);
    }

    @AfterAll
    static void tearDown() throws XMLDBException {
        XMLDB_EMBEDDED_DATABASE.getRoot().removeResource(invokableQuery);
    }

    @Test
    void eval() throws XMLDBException {
        final String query = "let $query := 'let $a := 1 return $a'\n" +
                "return\n" +
                "util:eval($query)";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("1", r);
            }
        }
    }

    @Test
    void evalWithExternalVars() throws XMLDBException {
        final String query = "let $value := 'world' return\n" +
                "\tutil:eval(xs:anyURI('/db/" + INVOKABLE_QUERY_FILENAME + "'), false(), (xs:QName('" + INVOKABLE_QUERY_EXTERNAL_VAR_NAME + "'), $value))";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {

            final LocalXMLResource res = (LocalXMLResource) result.getResource(0);
            final Node n = res.getContentAsDOM();
            assertEquals("hello", n.getLocalName());
            assertEquals("world", n.getFirstChild().getNodeValue());
        }
    }

    @Test
    void evalwithPI() throws XMLDBException {
        final String query = "let $query := 'let $a := <test><?pi test?></test> return count($a//processing-instruction())'\n" +
                "return\n" +
                "util:eval($query)";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("1", r);
            }
        }
    }

    @Test
    void evalInline() throws XMLDBException {
        final String query = "let $xml := document{<test><a><b/></a></test>}\n" +
                "let $query := 'count(.//*)'\n" +
                "return\n" +
                "util:eval-inline($xml,$query)";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }
    }

    @Test
    void evalWithContextVariable() throws XMLDBException {
        final String query = "let $xml := <test><a/><b/></test>\n" +
                "let $context := <static-context>\n" +
                "<variable name='xml'>{$xml}</variable>\n" +
                "</static-context>\n" +
                "let $query := 'count($xml//*) mod 2 = 0'\n" +
                "return\n" +
                "util:eval-with-context($query, $context, false())";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("true", r);
            }
        }
    }

    @Test
    void evalSupplyingContext() throws XMLDBException {
        final String query = "let $xml := <test><a/></test>\n" +
                "let $context := <static-context>\n" +
                "<default-context>{$xml}</default-context>\n" +
                "</static-context>\n" +
                "let $query := 'count(.//*) mod 2 = 0'\n" +
                "return\n" +
                "util:eval-with-context($query, $context, false())";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("true", r);
            }
        }
    }

    @Test
    void evalSupplyingContextAndVariable() throws XMLDBException {
        final String query = "let $xml := <test><a/></test>\n" +
                "let $context := <static-context>\n" +
                "<variable name='xml'>{$xml}</variable>\n" +
                "<default-context>{$xml}</default-context>\n" +
                "</static-context>\n" +
                "let $query := 'count($xml//*) + count(.//*)'\n" +
                "return\n" +
                "util:eval-with-context($query, $context, false())";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("3", r);
            }
        }
    }

    @Test
    void evalSupplyingContextItem() throws XMLDBException {
        final String query = "let $context := 'London'\n" +
                "let $query := '.'\n" +
                "return\n" +
                "util:eval-with-context($query, (), false(), $context)";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("London", r);
            }
        }
    }

    @Test
    void evalInContextWithPreDeclaredNamespace() throws XMLDBException {
        createCollection("testEvalInContextWithPreDeclaredNamespace");
        final String query =
            "xquery version \"1.0\";\r\n" +
            "declare namespace db = \"http://docbook.org/ns/docbook\";\r\n" +
            "import module namespace util = \"http://exist-db.org/xquery/util\";\r\n" +
            "let $q := \"/db:article\" return\r\n" +
            "util:eval($q)";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            // needed to ensure that result is closed
        }
    }

    @Test
    void evalInContextWithPreDeclaredNamespaceAcrossLocalFunctionBoundary() throws XMLDBException {
        createCollection("testEvalInContextWithPreDeclaredNamespace");
        final String query =
            "xquery version \"1.0\";\r\n" +
            "import module namespace util = \"http://exist-db.org/xquery/util\";\r\n" +
            "declare namespace db = \"http://docbook.org/ns/docbook\";\r\n" +
            "declare function local:process($q as xs:string) {\r\n" +
            "\tutil:eval($q)\r\n" +
            "};\r\n" +
            "let $q := \"/db:article\" return\r\n" +
            "local:process($q)";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            // needed to ensure that result is closed
        }
    }

    //should fail with - Error while evaluating expression: /db:article. XPST0081: No namespace defined for prefix db [at line 5, column 9]
    @Test
    void evalInContextWithPreDeclaredNamespaceAcrossModuleBoundary() throws XMLDBException {
        try (final Collection testHome = createCollection("testEvalInContextWithPreDeclaredNamespace")) {
            final String processorModule =
                "xquery version \"1.0\";\r\n" +
                    "module namespace processor = \"http://processor\";\r\n" +
                    "import module namespace util = \"http://exist-db.org/xquery/util\";\r\n" +
                    "declare function processor:process($q as xs:string) {\r\n" +
                    "\tutil:eval($q)\r\n" +
                    "};";

            writeModule(testHome, "processor.xqm", processorModule);

            final String query =
                "xquery version \"1.0\";\r\n" +
                    "import module namespace processor = \"http://processor\" at \"xmldb:exist://" + testHome.getName() + "/processor.xqm\";\r\n" +
                    "declare namespace db = \"http://docbook.org/ns/docbook\";\r\n" +
                    "let $q := \"/db:article\" return\r\n" +
                    "processor:process($q)";

            assertThrows(XMLDBException.class, () ->
                try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
                    // needed to ensure that result is closed
                }
            });
        }
    }

    /**
     * The original issue was caused by VariableReference inside util:eval
     * not calling XQueryContext#popNamespaceContext when a variable
     * reference could not be resolved, which led to the wrong
     * namespaces being present in the XQueryContext the next time
     * the same query was executed
     */
    @Test
    void evalWithMissingVariableReferenceShouldReportTheSameErrorEachTime() throws XMLDBException {
        final String testHomeName = "testEvalWithMissingVariableReferenceShouldReportTheSameErrorEachTime";
        try (final Collection testHome = createCollection(testHomeName)) {

            final String configModuleName = "config-test.xqm";
            final String configModule = "xquery version \"1.0\";\r\n" +
                "module namespace ct = \"http://config/test\";\r\n" +
                "declare variable $ct:var1 := request:get-parameter(\"var1\", ());";

            writeModule(testHome, configModuleName, configModule);

            final String testModuleName = "test.xqy";
            final String testModule = "import module namespace ct = \"http://config/test\" at \"xmldb:exist:///db/" + testHomeName + "/" + configModuleName + "\";\r\n" +
                "declare namespace x = \"http://x\";\r\n" +
                "declare function local:hello() {\r\n" +
                " (\r\n" +
                "<x:hello>hello</x:hello>,\r\n" +
                "util:eval(\"$ct:var1\")\r\n" +
                ")\r\n" +
                "};\r\n" +
                "local:hello()";

            writeModule(testHome, testModuleName, testModule);

            //run the 1st time
            try (final EXistResourceSet result = executeModule(testHome, testModuleName)) {
                // needed to ensure that result is closed
            } catch (final XMLDBException e) {
                final Throwable cause = e.getCause();
                assertInstanceOf(XPathException.class, cause);
                assertEquals(ErrorCodes.XPDY0002, ((XPathException) cause).getErrorCode());
            }

            //run a 2nd time, error code should be the same!
            try (final EXistResourceSet result = executeModule(testHome, testModuleName)) {
                // needed to ensure that result is closed
            } catch (final XMLDBException e) {
                final Throwable cause = e.getCause();
                assertInstanceOf(XPathException.class, cause);
                assertEquals(ErrorCodes.XPDY0002, ((XPathException) cause).getErrorCode());
            }
        }
    }
    
    private Collection createCollection(String collectionName) throws XMLDBException {
        Collection collection = XMLDB_EMBEDDED_DATABASE.getRoot().getChildCollection(collectionName);
        if (collection == null) {
            CollectionManagementService cmService = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
            try (final Collection created = cmService.createCollection(collectionName)) { }
        }

        collection = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + collectionName, "admin", "");
        assertNotNull(collection);
        return collection;
    }

    @Test
    void evalAndSerialize() throws XMLDBException {
        final String query = "let $query := \"<elem1>hello</elem1>\"\n" +
                "return\n" +
                "util:eval-and-serialize($query, ())";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            final Resource r = result.getResource(0);
            assertEquals("<elem1>hello</elem1>", r.getContent());
        }
    }

    @Test
    void evalAndSerializeDefaultOptions() throws XMLDBException {
        String query = "let $query := \"<elem1>hello</elem1>\"\n" +
                "return\n" +
                "util:eval-and-serialize($query, map { \"method\": \"adaptive\" })";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            final Resource r = result.getResource(0);
            assertEquals("<elem1>hello</elem1>", r.getContent());
        }

        // test that XQuery Prolog output options override the default provided options
        query = "xquery version \"3.1\";\n" +
                "declare namespace output = \"http://www.w3.org/2010/xslt-xquery-serialization\";\n" +
                "declare option output:method \"text\";\n" +
                "let $query := \"<elem1>hello</elem1>\"\n" +
                "return\n" +
                "util:eval-and-serialize($query, map { \"method\": \"adaptive\" })";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            final Resource r = result.getResource(0);
            assertEquals("hello", r.getContent());
        }
    }

    @Test
    void evalAndSerializeJson() throws XMLDBException {
        String query = "let $query := \"<outer><elem1>hello</elem1></outer>\"\n" +
                "return\n" +
                "util:eval-and-serialize($query, map { \"method\": \"json\" })";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            final Resource r = result.getResource(0);
            assertEquals("{\"elem1\":\"hello\"}", r.getContent());
        }
    }

    @Test
    void evalAndSerializeAdaptive() throws XMLDBException {
        String query = "let $query := 'map { \"key\": \"value\"}'\n" +
                "return\n" +
                "util:eval-and-serialize($query, map { \"method\": \"adaptive\" })";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            final Resource r = result.getResource(0);
            assertEquals("map{\"key\":\"value\"}", r.getContent());
        }
    }

    @Test
    void evalAndSerializeSubsequence() throws XMLDBException {
        final String query = "let $query := \"for $i in (1 to 10) return <i>{$i}</i>\"\n" +
                "return\n" +
                "util:eval-and-serialize($query, (), 1, 4)";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            final Resource r = result.getResource(0);
            assertEquals("<i>1</i><i>2</i><i>3</i><i>4</i>", r.getContent());
        }
    }

    @Test
    void evalErrorInfo() {
        final String query = "let $query := \"let $msg := 'some error message'\n" +
                "let $code := xs:QName('some-error')\n" +
                "return\n" +
                "    fn:error($code, $msg)\"\n" +
                "return\n" +
                "    util:eval($query, false(), (), false())";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {

            fail("Expected XPathException");

        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().contains("line 6"));
            assertTrue(e.getMessage().contains("column 5"));
        }
    }

    @Test
    void evalPassErrorInfo() {
        final String query = "let $query := \"let $msg := 'some error message'\n" +
                "let $code := xs:QName('some-error')\n" +
                "return\n" +
                "    fn:error($code, $msg)\"\n" +
                "return\n" +
                "    util:eval($query, false(), (), true())";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {

            fail("Expected XPathException");

        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().contains("line 4"));
            assertTrue(e.getMessage().contains("column 5"));
        }
    }
    
    private void writeModule(Collection collection, String modulename, String module) throws XMLDBException {
        try (final BinaryResource res = collection.createResource(modulename, BinaryResource.class)) {
            ((EXistResource) res).setMediaType(MediaType.APPLICATION_XQUERY);
            res.setContent(module.getBytes());
            collection.storeResource(res);
        }
    }

    private EXistResourceSet executeModule(final Collection collection, final String moduleName) throws XMLDBException {
        final EXistXPathQueryService service = collection.getService(EXistXPathQueryService.class);
        final XmldbURI moduleUri = ((EXistCollection)collection).getPathURI().append(moduleName);
        return service.executeStoredQuery(moduleUri.toString());
    }
}
