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
 */
package org.exist.xmldb;

import com.evolvedbinary.j8fu.Either;
import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.Namespaces;
import org.exist.TestUtils;
import org.exist.dom.memtree.DocumentBuilderReceiver;
import org.exist.test.DatabaseWebServerExtension;
import org.exist.util.MapUtil;
import org.exist.util.StringInputSource;
import org.exist.xqj.Marshaller;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.ArrayWrapper;
import org.exist.xquery.value.Type;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XQueryService;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.xmldb.XMLDBExternalVariableTest.EntryRep.entry;
import static org.exist.xmldb.XMLDBExternalVariableTest.KeyRep.key;
import static org.exist.xmldb.XMLDBExternalVariableTest.TypedArrayRep.array;
import static org.exist.xmldb.XMLDBExternalVariableTest.TypedMapRep.map;
import static org.exist.xmldb.XMLDBExternalVariableTest.UntypedArrayRep.untypedArray;
import static org.exist.xmldb.XMLDBExternalVariableTest.SequenceRep.sequence;
import static org.exist.xmldb.XMLDBExternalVariableTest.TypedValueRep.value;
import static org.exist.xmldb.XMLDBExternalVariableTest.TypedNamedValueRep.value;
import static org.exist.xmldb.XMLDBExternalVariableTest.UntypedMapRep.untypedMap;
import static org.exist.xmldb.XMLDBExternalVariableTest.UntypedNamedValueRep.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class XMLDBExternalVariableTest {

    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "local", "xmldb:exist://" },
            { "remote", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc" }
        });
    }
    public String apiName;
    public String baseUri;

    @RegisterExtension
    public static final DatabaseWebServerExtension DATABASE_WEB_SERVER = new DatabaseWebServerExtension(true, false, true, true);
    private static final String PORT_PLACEHOLDER = "${PORT}";

    private static final String TEST_NAMESPACE = "http://XMLDBExternalVariableTest";
    private static final String TEST_PREFIX = "xevt";

    private static final Map<String, String> NS_CONTEXT = MapUtil.hashMap(
        Tuple("xs", XMLConstants.W3C_XML_SCHEMA_NS_URI),
        Tuple(Namespaces.EXIST_NS_PREFIX, Namespaces.EXIST_NS),
        Tuple(Marshaller.PREFIX, Marshaller.PREFIX),
        Tuple(TEST_PREFIX, TEST_NAMESPACE)
    );

    private String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(DATABASE_WEB_SERVER.getPort()));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        try (final Collection dbCollection = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
            final XQueryService xqueryService = dbCollection.getService(XQueryService.class);

            xqueryService.declareVariable("local:my-variable", "hello");
            xqueryService.declareVariable("local:other-variable", "goodbye");

            final CompiledExpression compiled = xqueryService.compile("declare variable $local:my-variable as xs:string* external;\n$local:my-variable");

            try (final EXistResourceSet result = (EXistResourceSet) xqueryService.execute(compiled)) {
                fail("Expected XMLDBException with cause XPathException: XPDY0002 External variable local:other-variable is not declared in the XQuery");
            } catch (final XMLDBException e) {
                final Throwable cause = e.getCause();
                assertInstanceOf(XPathException.class, cause);
            }
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUndeclared(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "xs:string*", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), null, (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedString(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedString(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "xs:string", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "xs:string", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringSuppliedString(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringSuppliedStrings(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[]{ value("hello"), value("goodbye") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "xs:string", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedStrings(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[]{ value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(expectedResult, null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedStrings(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptStringNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "xs:string?", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptStringSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("xs:string?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptStringSuppliedString(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptStringSuppliedStrings(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "xs:string?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptStringSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringsNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "xs:string+", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringsSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "xs:string+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringsSuppliedString(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringsSuppliedStrings(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable("xs:string+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringsSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringsSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(expectedResult, "xs:string+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringzNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "xs:string*", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringzSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("xs:string*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringzSuppliedString(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringzSuppliedStrings(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable("xs:string*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringzSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableStringzSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(expectedResult, "xs:string*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedElementValue(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedElement(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "element()", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "element()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementSuppliedElement(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("element()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementSuppliedElements(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "element()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableElementSuppliedUntyped} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "element()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedElements(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedElements(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptElementNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "element()?", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptElementSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("element()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptElementSuppliedElement(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("element()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptElementSuppliedElements(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "element()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptElementSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableOptElementSuppliedUntyped} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()?", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "element()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementsNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "element()+", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementsSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "element()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementsSuppliedElement(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("element()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementsSuppliedElements(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable("element()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementsSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableElementsSuppliedUntyped} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()+", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "element()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementsSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableElementsSuppliedUntypeds} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()+", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "element()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementzNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "element()*", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementzSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("element()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementzSuppliedElement(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("element()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementzSuppliedElements(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable("element()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementszSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableElementszSuppliedUntyped} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()*", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "element()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableElementzSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableElementzSuppliedUntypeds} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()*", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "element()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedDocument(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedDocument(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "document-node()", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "document-node()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentSuppliedDocument(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("document-node()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentSuppliedDocuments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "document-node()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "document-node()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedDocuments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedDocuments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptDocumentNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "document-node()?", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptDocumentSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("document-node()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptDocumentSuppliedDocument(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("document-node()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptDocumentSuppliedDocuments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "document-node()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptDocumentSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "document-node()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentsNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "document-node()+", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentsSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "document-node()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentsSuppliedDocument(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("document-node()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentsSuppliedDocuments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable("document-node()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentsSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "document-node()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentsSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "document-node()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentzNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "document-node()*", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentzSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("document-node()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentzSuppliedDocument(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("document-node()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentzSuppliedDocuments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable("document-node()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentszSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "document-node()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableDocumentzSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "document-node()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedComment(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<!-- hello world -->");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedComment(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "comment()", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "comment()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentSuppliedComment(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable("comment()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentSuppliedComments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "comment()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<!-- hello world -->");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "comment()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedComments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedComments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptCommentNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "comment()?", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptCommentSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("comment()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptCommentSuppliedComment(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable("comment()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptCommentSuppliedComments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "comment()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptCommentSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<!-- hello world -->");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "comment()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentsNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "comment()+", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentsSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "comment()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentsSuppliedComment(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable("comment()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentsSuppliedComments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable("comment()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentsSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "comment()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentsSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "comment()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentzNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "comment()*", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentzSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("comment()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentzSuppliedComment(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable("comment()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentzSuppliedComments(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable("comment()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentszSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "comment()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableCommentzSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "comment()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstruction(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<?hello world?>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedProcessingInstruction(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "processing-instruction()", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "processing-instruction()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionSuppliedProcessingInstruction(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable("processing-instruction()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionSuppliedProcessingInstructions(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "processing-instruction()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<?hello world?>");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "processing-instruction()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstructions(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedProcessingInstructions(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptProcessingInstructionNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "processing-instruction()?", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("processing-instruction()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedProcessingInstruction(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable("processing-instruction()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedProcessingInstructions(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "processing-instruction()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("<?hello world?>");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "processing-instruction()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionsNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "processing-instruction()+", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "processing-instruction()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedProcessingInstruction(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable("processing-instruction()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedProcessingInstructions(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable("processing-instruction()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "processing-instruction()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "processing-instruction()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionzNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "processing-instruction()*", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("processing-instruction()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedProcessingInstruction(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable("processing-instruction()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedProcessingInstructions(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable("processing-instruction()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionszSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "processing-instruction()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "processing-instruction()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedText(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("hello world");
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as text()
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello world") };
        queryPostWithExternalVariable(expectedResult, null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedText(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "text()", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "text()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextSuppliedText(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable("text()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextSuppliedTexts(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "text()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("hello world");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "text()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedTexts(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as text()
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello world"), value(Type.STRING, "goodbye see you soon") };
        queryPostWithExternalVariable(expectedResult, null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedTexts(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptTextNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "text()?", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptTextSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("text()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptTextSuppliedText(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable("text()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptTextSuppliedTexts(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "text()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptTextSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("hello world");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "text()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextsNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "text()+", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextsSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "text()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextsSuppliedText(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable("text()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextsSuppliedTexts(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable("text()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextsSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "text()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextsSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "text()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextzNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "text()*", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextzSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("text()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextzSuppliedText(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable("text()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextzSuppliedTexts(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable("text()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextszSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "text()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableTextzSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "text()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedAttribute(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("hello", "world");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedAttribute(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "hello", "world");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributeNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "attribute()", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributeSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "attribute()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributeSuppliedAttribute(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "xevt:hello", "world");
        queryPostWithExternalVariable("attribute()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributeSuppliedAttributes(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world"), value("goodbye", "see you soon") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "attribute()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributeSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("xevt:hello", "world");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "attribute()", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedAttributes(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world"), value("goodbye", "see you soon") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedAttributes(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "xevt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptAttributeNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "attribute()?", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptAttributeSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("attribute()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptAttributeSuppliedAttribute(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "xevt:hello", "world");
        queryPostWithExternalVariable("attribute()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptAttributeSuppliedAttributes(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "xevt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "attribute()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptAttributeSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value("xevt:hello", "world");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "attribute()?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributesNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "attribute()+", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributesSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "attribute()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributesSuppliedAttribute(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "xevt:hello", "world");
        queryPostWithExternalVariable("attribute()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributesSuppliedAttributes(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "xevt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable("attribute()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributesSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "attribute()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributesSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world"), value("goodbye", "see you soon") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "attribute()+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributezNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "attribute()*", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributezSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("attribute()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributezSuppliedAttribute(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "xevt:hello", "world");
        queryPostWithExternalVariable("attribute()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributezSuppliedAttributes(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "xevt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable("attribute()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributeszSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "attribute()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableAttributezSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world"), value("goodbye", "see you soon") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "attribute()*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedArray(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedArray(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArrayNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "array(*)", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArraySuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "array(*)", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArraySuppliedArray(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable("array(*)", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArraySuppliedArrays(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "array(*)", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArraySuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected array(*), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "array(*)", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedArrays(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = { untypedArray(sequence(value(Type.STRING, "hello"))), untypedArray(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedArrays(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptArrayNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "array(*)?", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptArraySuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable("array(*)?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptArraySuppliedArray(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable("array(*)?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptArraySuppliedArrays(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "array(*)?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptArraySuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected array(*), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "array(*)?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArraysNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "array(*)+", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArraysSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "array(*)+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArraysSuppliedArray(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable("array(*)+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArraysSuppliedArrays(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable("array(*)+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArraysSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected array(*), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "array(*)+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArraysSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = { untypedArray(sequence(value(Type.STRING, "hello"))), untypedArray(sequence(value(Type.STRING, "goodbye"))) };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected array(*), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "array(*)+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArrayzNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "array(*)*", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArrayzSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable("array(*)*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArrayzSuppliedArray(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable("array(*)*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArrayzSuppliedArrays(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable("array(*)*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArrayszSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected array(*), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "array(*)*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableArrayzSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = { untypedArray(sequence(value(Type.STRING, "hello"))), untypedArray(sequence(value(Type.STRING, "goodbye"))) };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected array(*), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), expectedResponseError), "array(*)*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedMap(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = untypedMap(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedMap(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = map(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "map(*)", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new MapRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "map(*)", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapSuppliedMap(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = map(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable("map(*)", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapSuppliedMaps(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = {
            map(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            map(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, 42)))
            )
        };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "map(*)", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = untypedMap(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable("map(*)", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedUntypedMaps(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = {
            untypedMap(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            untypedMap(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, 42)))
            )
        };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableUntypedSuppliedMaps(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = {
            map(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            map(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, 42)))
            )
        };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptMapNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "map(*)?", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptMapSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new MapRep[0];
        queryPostWithExternalVariable("map(*)?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptMapSuppliedMap(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = map(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable("map(*)?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptMapSuppliedMaps(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = {
            map(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            map(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, 42)))
            )
        };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "map(*)?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableOptMapSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = untypedMap(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable("map(*)?", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapsNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "map(*)+", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapsSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new MapRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004.getErrorCode(), "map(*)+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapsSuppliedMap(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = map(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable("map(*)+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapsSuppliedMaps(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = {
            map(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            map(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, 42)))
            )
        };

        queryPostWithExternalVariable("map(*)+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapsSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = untypedMap(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable("map(*)+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapsSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = {
            untypedMap(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            untypedMap(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, 42)))
            )
        };
        queryPostWithExternalVariable("map(*)+", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapzNotSupplied(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002.getErrorCode(), "map(*)*", (ExternalVariableValueRep[]) null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapzSuppliedEmpty(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = new MapRep[0];
        queryPostWithExternalVariable("map(*)*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapzSuppliedMap(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = map(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable("map(*)*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapzSuppliedMaps(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = {
            map(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            map(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, 42)))
            )
        };
        queryPostWithExternalVariable("map(*)*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapszSuppliedUntyped(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep externalVariable = untypedMap(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable("map(*)*", externalVariable);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void queryPostWithExternalVariableMapzSuppliedUntypeds(String apiName, String baseUri) throws XMLDBException {
        initXMLDBExternalVariableTest(apiName, baseUri);
        final ExternalVariableValueRep[] externalVariable = {
            untypedMap(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, 42))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            untypedMap(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, 42)))
            )
        };
        queryPostWithExternalVariable("map(*)*", externalVariable);
    }

    private void queryPostWithExternalVariable(@Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        queryPostWithExternalVariable(Tuple(null, null), externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(@Nullable final ErrorCodes.ErrorCode expectedResponseCode, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        queryPostWithExternalVariable(Tuple(expectedResponseCode, null), externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final Tuple2<ErrorCodes.ErrorCode, String> expectedResponse, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        queryPostWithExternalVariable(expectedResponse, externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final ExternalVariableValueRep[] expectedResult, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        queryPostWithExternalVariable((ErrorCodes.ErrorCode) null, expectedResult, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(@Nullable final ErrorCodes.ErrorCode expectedResponseCode, final ExternalVariableValueRep[] expectedResult, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        queryPostWithExternalVariable(Tuple(expectedResponseCode, null), expectedResult, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final Tuple2<ErrorCodes.ErrorCode, String> expectedResponse, final ExternalVariableValueRep[] expectedResult, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws XMLDBException {
        @Nullable final Object[] externalVariableValue = buildExternalVariableValue(externalVariableSequence);
        final String query = buildQueryExternalVariable(xqExternalVariableType);
        final Either<XMLDBException, Tuple2<Collection, EXistResourceSet>> response = doPostWithAuth(externalVariableValue, query);
        @Nullable final Collection dbCollection = response.map(r -> r._1).getOrElse((Collection) null);

        try (@Nullable final EXistResourceSet actualResultSet = response.map(r -> r._2).getOrElse((EXistResourceSet) null);) {
            if (expectedResponse._1 == null) {
                // We expect success
                assertTrue(response.isRight());
                final List<Tuple2<String, Object>> expectedResults = buildExistVariableResultSequence(expectedResult);

                assertEquals(expectedResults.size(), actualResultSet.getSize());
                for (int i = 0; i < expectedResults.size(); i++) {
                    final Tuple2<String, Object> expected = expectedResults.get(i);
                    try (final EXistResource actual = (EXistResource) actualResultSet.getResource(i)) {

                        if (expected._1 != null) {
                            assertEquals(expected._1, actual.getTypeName());
                        }
                        final Object actualValue = actual.getContent();
                        assertInstanceOf(String.class, actualValue);
                        final String actualString = actualValue.toString();

                        try {
                            if (expected._1 != null && Type.getType(expected._1) == Type.MAP_ITEM || expectedResult[i] instanceof MapRep) {
                                // NOTE(AR) XDM Map type does not have order, so we cannot compare as strings
                                final Map<Object, Object> expectedMap = MapUtil.parseXdmMapStringToJavaMap(expected._2.toString());
                                final Map<Object, Object> actualMap = MapUtil.parseXdmMapStringToJavaMap(actualString);
                                assertThat(actualMap).containsExactlyInAnyOrderEntriesOf(expectedMap);
                            } else {
                                assertEquals(expected._2, actualString);
                            }
                        } catch (final XPathException e) {
                            fail(e.getMessage());
                        }
                    }
                }

            } else {
                // We expect an error, so check the error is the expected one
                assertTrue(response.isLeft());
                final XMLDBException errorResponse = response.left().get();
                assertInstanceOf(XPathException.class, errorResponse.getCause());
                final XPathException errorResponseXPathException = (XPathException) errorResponse.getCause();
                assertEquals(expectedResponse._1, errorResponseXPathException.getErrorCode());

                if (expectedResponse._2 != null) {
                    String expectedResponseMessage = expectedResponse._2;
                    assertEquals(expectedResponseMessage, errorResponseXPathException.getDetailMessage());
                }
            }
        } finally {
            if (dbCollection != null) {
                dbCollection.close();
            }
        }
    }

    private static List<Tuple2<String, Object>> buildExistVariableResultSequence(final ExternalVariableValueRep... resultSequence) {
        final List<Tuple2<String, Object>> results = new ArrayList<>();
        for (final ExternalVariableValueRep resultItem : resultSequence) {

            @Nullable String type = null;
            Object value = null;

            if (resultItem instanceof ExternalVariableTypedValueRep) {
                type = Type.getTypeName(((ExternalVariableTypedValueRep) resultItem).getXdmType());
            }

            if (resultItem instanceof ValueRep) {
                value = ((ValueRep) resultItem).getContent();

            } else if (resultItem instanceof TypedArrayRep) {
                final StringBuilder builder = new StringBuilder();
                builder.append("[ ");
                final SequenceRep[] arrayItems = ((ArrayRep) resultItem).getValues();
                for (final SequenceRep arrayItem : arrayItems) {
                    final ValueRep[] arrayItemValues = arrayItem.values;
                    if (arrayItemValues.length == 0) {
                        builder.append("()");
                    } else if (arrayItemValues.length > 1) {
                        builder.append("(");
                    }
                    final List<Tuple2<String, Object>> arrayItemValueSeq = buildExistVariableResultSequence(arrayItemValues);
                    for (int i = 0; i < arrayItemValueSeq.size(); i++) {
                        if (i > 0) {
                            builder.append(", ");
                        }
                        final boolean isStringType;
                        try {
                            isStringType = arrayItemValueSeq.get(i)._1 != null && Type.subTypeOf(Type.getType(arrayItemValueSeq.get(i)._1), Type.STRING);
                        } catch (final XPathException e) {
                            fail(e.getMessage());
                            return null;
                        }
                        if (isStringType) {
                            builder.append('"');
                        }
                        builder.append(arrayItemValueSeq.get(i)._2);
                        if (isStringType) {
                            builder.append('"');
                        }
                    }
                    if (arrayItemValues.length > 1) {
                        builder.append(")");
                    }
                }
                builder.append(" ]");
                value = builder.toString();

            } else if (resultItem instanceof UntypedArrayRep) {
                final StringBuilder builder = new StringBuilder();
                final SequenceRep[] arrayItems = ((ArrayRep) resultItem).getValues();
                for (final SequenceRep arrayItem : arrayItems) {
                    for (final ValueRep arrayItemValue : arrayItem.values) {
                        builder.append(arrayItemValue.getContent());
                    }
                }
                value = builder.toString();

            } else if (resultItem instanceof MapRep) {
                final StringBuilder builder = new StringBuilder();
                builder.append("map {");
                final EntryRep[] mapEntries = ((MapRep) resultItem).getEntries();
                for (int i = 0; i < mapEntries.length; i++) {

                    if (i > 0) {
                        builder.append(", ");
                    }

                    final EntryRep mapEntry = mapEntries[i];
                    final ValueRep key = mapEntry.key.key;
                    final int keyType;
                    if (key instanceof TypedValueRep) {
                        keyType = ((TypedValueRep) key).getXdmType();
                    } else {
                        keyType = Type.STRING;
                    }

                    if (Type.subTypeOf(keyType, Type.STRING)) {
                        builder.append('"');
                    }
                    builder.append(key.getContent());
                    if (Type.subTypeOf(keyType, Type.STRING)) {
                        builder.append('"');
                    }
                    builder.append(": ");
                    if (mapEntry.value.values.length != 1) {
                        builder.append('(');
                    }
                    for (int j = 0; j < mapEntry.value.values.length; j++) {
                        final ValueRep valueRep = mapEntry.value.values[j];
                        final int valueType;
                        if (valueRep instanceof TypedValueRep) {
                            valueType = ((TypedValueRep) valueRep).getXdmType();
                        } else {
                            valueType = Type.STRING;
                        }
                        if (j > 0) {
                            builder.append(", ");
                        }
                        if (Type.subTypeOf(valueType, Type.STRING)) {
                            builder.append('"');
                        }
                        builder.append(valueRep.getContent());
                        if (Type.subTypeOf(valueType, Type.STRING)) {
                            builder.append('"');
                        }
                    }
                    if (mapEntry.value.values.length != 1) {
                        builder.append(')');
                    }
                }
                builder.append('}');
                value = builder.toString();
            }

            results.add(Tuple(type, value));
        }
        return results;
    }

    private Either<XMLDBException, Tuple2<Collection, EXistResourceSet>> doPostWithAuth(@Nullable final Object[] externalVariableValue, final String query) throws XMLDBException {
        // NOTE(AR) dbCollection will be closed either when XMLDBException is captured, or the ResourceSet is closed
        final Collection dbCollection = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        try {
            final XQueryService xqueryService = dbCollection.getService(XQueryService.class);

            if (externalVariableValue != null) {
                if (externalVariableValue.length == 1) {
                    // unbox an array of 1 item
                    xqueryService.declareVariable("local:my-variable", externalVariableValue[0]);
                } else {
                    xqueryService.declareVariable("local:my-variable", externalVariableValue);
                }
            }

            final CompiledExpression compiled = xqueryService.compile(query);
            try {
                return Either.Right(Tuple(dbCollection, (EXistResourceSet) xqueryService.execute(compiled)));
            } catch (final XMLDBException e) {
                dbCollection.close();
                return Either.Left(e);
            }
        } catch (final XMLDBException e) {
            dbCollection.close();
            throw e;
        }
    }

    private static @Nullable Object[] buildExternalVariableValue(@Nullable final ExternalVariableValueRep... externalVariableSequence) {
        if (externalVariableSequence == null) {
            return null;
        }

        final Object[] values = new Object[externalVariableSequence.length];
        for (int i = 0; i < externalVariableSequence.length; i++) {
            final ExternalVariableValueRep externalVariableItem = externalVariableSequence[i];

            int type = Type.ITEM;
            if (externalVariableItem instanceof TypedValueRep) {
                type = ((TypedValueRep) externalVariableItem).getXdmType();
            }

            if (Type.subTypeOf(type, Type.NODE)) {

                if (type == Type.DOCUMENT) {
                    final Document document = parse(((ValueRep) externalVariableItem).getContent().toString());
                    values[i] = document;

                } else if (type == Type.ELEMENT) {
                    final Element element = parse(((ValueRep) externalVariableItem).getContent().toString()).getDocumentElement();
                    values[i] = element;

                } else if (type == Type.COMMENT) {
                    final Comment comment = (Comment) parse("<stub-element>" + ((ValueRep) externalVariableItem).getContent() + "</stub-element>").getDocumentElement().getFirstChild();
                    values[i] = comment;

                } else if (type == Type.PROCESSING_INSTRUCTION) {
                    final ProcessingInstruction processingInstruction = (ProcessingInstruction) parse("<stub-element>" + ((ValueRep) externalVariableItem).getContent() + "</stub-element>").getDocumentElement().getFirstChild();
                    values[i] = processingInstruction;

                } else if (type == Type.TEXT) {
                    final Text text = (Text) parse("<stub-element>" + ((ValueRep) externalVariableItem).getContent() + "</stub-element>").getDocumentElement().getFirstChild();
                    values[i] = text;

                } else if (type == Type.ATTRIBUTE) {
                    final String attrNameString = ((NamedValueRep) externalVariableItem).getName();
                    final String attrValue = ((ValueRep) externalVariableItem).getContent().toString();
                    @Nullable final String attrPrefix;
                    @Nullable final String attrNamespace;
                    final String attrLocalName;
                    final int colonSep = attrNameString.indexOf(':');
                    if (colonSep > -1) {
                        attrPrefix = attrNameString.substring(0, colonSep);
                        attrNamespace = NS_CONTEXT.get(attrPrefix);
                        attrLocalName = attrNameString.substring(colonSep + 1);
                    } else {
                        attrPrefix = null;
                        attrNamespace = null;
                        attrLocalName = attrNameString;
                    }

                    final Attr attr;
                    if (attrNamespace == null) {
                        final String elementWithAttr = "<stub-element " + attrNameString + "=\"" + attrValue + "\"/>";
                        attr = parse(elementWithAttr).getDocumentElement().getAttributeNode(attrNameString);
                    } else {
                        final String elementWithAttr = "<stub-element xmlns:" + attrPrefix + "=\"" + attrNamespace + "\" " + attrNameString + "=\"" + attrValue + "\"/>";
                        attr = parse(elementWithAttr).getDocumentElement().getAttributeNodeNS(attrNamespace, attrLocalName);
                    }
                    values[i] = attr;

                } else {
                    throw new UnsupportedOperationException("TODO(AR) implement type conversion");
                }

            } else if (externalVariableItem instanceof ArrayRep) {
                final SequenceRep[] arrayItems = ((ArrayRep) externalVariableItem).getValues();
                final Object[] result = new Object[arrayItems.length];
                for (int j = 0; j < arrayItems.length; j++) {
                    final SequenceRep arrayItem = arrayItems[j];
                    result[j] = buildExternalVariableValue(arrayItem.values);
                }

                if (externalVariableItem instanceof TypedArrayRep) {
                    values[i] = new ArrayWrapper<>(result);
                } else {
                    values[i] = result;
                }

            } else if (externalVariableItem instanceof ValueRep) {
                values[i] = ((ValueRep) externalVariableItem).getContent();

            } else if (externalVariableItem instanceof MapRep) {
                final Map<Object, Object> map = new HashMap<>();
                for (final EntryRep entryRep : ((MapRep) externalVariableItem).getEntries()) {
                    map.put(entryRep.key.key.getContent(), buildExternalVariableValue(entryRep.value.values));
                }
                values[i] = map;

            } else {
                throw new UnsupportedOperationException("TODO(AR) implement type conversion");
            }
        }
        return values;
    }

    private static Document parse(final String xml) {
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        try {
            final SAXParser saxParser = saxParserFactory.newSAXParser();
            final XMLReader xmlReader = saxParser.getXMLReader();
            final DocumentBuilderReceiver handler = new DocumentBuilderReceiver();
            xmlReader.setContentHandler(handler);
            xmlReader.setProperty(Namespaces.SAX_LEXICAL_HANDLER, handler);
            xmlReader.parse(new StringInputSource(xml));
            return handler.getDocument();
        } catch (final SAXException | ParserConfigurationException | IOException e) {
            fail(e.getMessage());
            return null;
        }
    }

    private static String buildQueryExternalVariable(@Nullable final String xqExternalVariableType) {
        final StringBuilder builder = new StringBuilder();

        builder.append("declare variable $local:my-variable");
        if (xqExternalVariableType != null) {
            builder.append(" as ").append(xqExternalVariableType);
        }
        builder.append(" external;\n");
        builder.append("$local:my-variable\n");

        return builder.toString();
    }

    interface ExternalVariableValueRep {};
    interface ExternalVariableUntypedValueRep extends ExternalVariableValueRep {}
    interface ExternalVariableTypedValueRep extends ExternalVariableUntypedValueRep {
        int getXdmType();
    }

    static class SequenceRep {
        private final ValueRep[] values;

        public static SequenceRep sequence(final ValueRep... values) {
            return new SequenceRep(values);
        }

        private SequenceRep(final ValueRep[] values) {
            this.values = values;
        }
    }

    interface ValueRep extends ExternalVariableValueRep {
        Object getContent();
    }

    private interface NamedValueRep extends ValueRep {
        String getName();
    }

    static class UntypedValueRep implements ValueRep, ExternalVariableUntypedValueRep {
        private final Object content;

        public static UntypedValueRep value(final Object content) {
            return new UntypedValueRep(content);
        }

        private UntypedValueRep(final Object content) {
            this.content = content;
        }

        @Override
        public Object getContent() {
            return content;
        }
    }

    static class UntypedNamedValueRep extends UntypedValueRep implements NamedValueRep {
        private final String name;

        public static UntypedNamedValueRep value(final String name, final Object content) {
            return new UntypedNamedValueRep(name, content);
        }

        private UntypedNamedValueRep(final String name, final Object content) {
            super(content);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    static class TypedValueRep extends UntypedValueRep implements ExternalVariableTypedValueRep {
        final int xdmType;

        public static TypedValueRep value(final int xdmType, final Object content) {
            return new TypedValueRep(xdmType, content);
        }

        private TypedValueRep(final int xdmType, final Object content) {
            super(content);
            this.xdmType = xdmType;
        }

        @Override
        public int getXdmType() {
            return xdmType;
        }
    }

    static class TypedNamedValueRep extends TypedValueRep implements NamedValueRep {
        final String name;

        public static TypedNamedValueRep value(final int xdmType, final String name, final Object content) {
            return new TypedNamedValueRep(xdmType, name, content);
        }

        private TypedNamedValueRep(final int xdmType, final String name, final Object content) {
            super(xdmType, content);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    interface ArrayRep extends ExternalVariableValueRep {
        SequenceRep[] getValues();
    }

    static class UntypedArrayRep implements ArrayRep, ExternalVariableUntypedValueRep {
        private final SequenceRep[] values;

        public static UntypedArrayRep untypedArray(final SequenceRep... values) {
            return new UntypedArrayRep(values);
        }

        private UntypedArrayRep(final SequenceRep[] values) {
            this.values = values;
        }

        @Override
        public SequenceRep[] getValues() {
            return values;
        }
    }

    static class TypedArrayRep extends UntypedArrayRep implements ArrayRep, ExternalVariableTypedValueRep {

        public static TypedArrayRep array(final SequenceRep... values) {
            return new TypedArrayRep(values);
        }

        private TypedArrayRep(final SequenceRep[] values) {
            super(values);
        }

        @Override
        public int getXdmType() {
            return Type.ARRAY_ITEM;
        }
    }

    interface MapRep extends ExternalVariableValueRep {
        EntryRep[] getEntries();
    }

    static class UntypedMapRep implements MapRep, ExternalVariableUntypedValueRep {
        private final EntryRep[] entries;

        public static UntypedMapRep untypedMap(final EntryRep... entries) {
            return new UntypedMapRep(entries);
        }

        private UntypedMapRep(final EntryRep[] entries) {
            this.entries = entries;
        }

        @Override
        public EntryRep[] getEntries() {
            return entries;
        }
    }

    static class TypedMapRep extends UntypedMapRep implements MapRep, ExternalVariableTypedValueRep {

        public static TypedMapRep map(final EntryRep... entries) {
            return new TypedMapRep(entries);
        }

        private TypedMapRep(final EntryRep[] entries) {
            super(entries);
        }

        @Override
        public int getXdmType() {
            return Type.MAP_ITEM;
        }
    }

    static class EntryRep {
        private final KeyRep key;
        private final SequenceRep value;

        public static EntryRep entry(final KeyRep key, final SequenceRep value) {
            return new EntryRep(key, value);
        }

        private EntryRep(final KeyRep key, final SequenceRep value) {
            this.key = key;
            this.value = value;
        }
    }

    static class KeyRep {
        private final ValueRep key;

        public static KeyRep key(final int xdmType, final String content) {
            return new KeyRep(value(xdmType, content));
        }

        private KeyRep(final ValueRep key) {
            this.key = key;
        }
    }

    public void initXMLDBExternalVariableTest(String apiName, String baseUri) {
        this.apiName = apiName;
        this.baseUri = baseUri;
    }
}
