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
import org.exist.test.ExistWebServer;
import org.exist.util.MapUtil;
import org.exist.util.StringInputSource;
import org.exist.xqj.Marshaller;
import org.exist.xquery.ErrorCodes;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Type;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
import org.xmldb.api.base.ResourceSet;
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
import java.util.List;
import java.util.Map;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.xmldb.XMLDBExternalVariableTest.TypedValueRep.value;
import static org.exist.xmldb.XMLDBExternalVariableTest.TypedNamedValueRep.value;
import static org.exist.xmldb.XMLDBExternalVariableTest.UntypedNamedValueRep.value;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class XMLDBExternalVariableTest {

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "local", "xmldb:exist://" },
            { "remote", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc" }
        });
    }

    @Parameterized.Parameter
    public String apiName;

    @Parameterized.Parameter(value = 1)
    public String baseUri;

    @ClassRule
    public static final ExistWebServer existWebServer = new ExistWebServer(true, false, true, true);
    private static final String PORT_PLACEHOLDER = "${PORT}";

    private static final String TEST_NAMESPACE = "http://XMLDBExternalVariableTest";
    private static final String TEST_PREFIX = "xevt";

    private static final Map<String, String> NS_CONTEXT = MapUtil.HashMap(
        Tuple("xs", XMLConstants.W3C_XML_SCHEMA_NS_URI),
        Tuple(Namespaces.EXIST_NS_PREFIX, Namespaces.EXIST_NS),
        Tuple(Marshaller.PREFIX, Marshaller.PREFIX),
        Tuple(TEST_PREFIX, TEST_NAMESPACE)
    );

    private String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(existWebServer.getPort()));
    }

    @Test
    public void queryPostWithExternalVariableUntypedNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, null, (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, null, externalVariable);
    }
    
    @Test
    public void queryPostWithExternalVariableUntypedSuppliedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "xs:string", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[]{ UntypedValueRep.value("hello"), UntypedValueRep.value("goodbye") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[]{ UntypedValueRep.value("hello"), UntypedValueRep.value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "xs:string?", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "xs:string+", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable("xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("hello"), UntypedValueRep.value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(expectedResult, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "xs:string*", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedString() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable("xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedStrings() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable("xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(expectedResult, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("hello"), UntypedValueRep.value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(expectedResult, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedElementValue() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<hello>world</hello>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedElement() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "element()", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedElement() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedElements() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>"), UntypedValueRep.value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<hello>world</hello>");
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableElementSuppliedUntyped} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedElements() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>"), UntypedValueRep.value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedElements() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "element()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedElement() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedElements() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<hello>world</hello>");
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableOptElementSuppliedUntyped} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()?", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "element()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedElement() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedElements() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable("element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>") };
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableElementsSuppliedUntyped} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()+", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>"), UntypedValueRep.value("<goodbye>see you soon</goodbye>") };
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableElementsSuppliedUntypeds} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()+", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "element()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedElement() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedElements() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable("element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementszSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>") };
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableElementszSuppliedUntyped} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()*", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>"), UntypedValueRep.value("<goodbye>see you soon</goodbye>") };
        // NOTE(AR) unlike {@link RESTExternalVariableTest#queryPostWithExternalVariableElementzSuppliedUntypeds} a value supplied as an 'untyped' Element cannot be inferred by the XML:DB API as it can be via the REST API
//        queryPostWithExternalVariable("element()*", externalVariable);
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected element(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedDocument() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<hello>world</hello>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedDocument() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "document-node()", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedDocument() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedDocuments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>"), UntypedValueRep.value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<hello>world</hello>");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedDocuments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>"), UntypedValueRep.value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedDocuments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "document-node()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedDocument() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedDocuments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<hello>world</hello>");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "document-node()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedDocument() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedDocuments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable("document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>"), UntypedValueRep.value("<goodbye>see you soon</goodbye>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "document-node()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedDocument() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable("document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedDocuments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable("document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentszSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<hello>world</hello>"), UntypedValueRep.value("<goodbye>see you soon</goodbye>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected document-node(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedComment() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<!-- hello world -->");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedComment() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "comment()", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedComment() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable("comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedComments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<!-- hello world -->"), UntypedValueRep.value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<!-- hello world -->");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedComments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<!-- hello world -->"), UntypedValueRep.value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedComments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "comment()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedComment() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable("comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedComments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<!-- hello world -->");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "comment()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedComment() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable("comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedComments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable("comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<!-- hello world -->") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<!-- hello world -->"), UntypedValueRep.value("<!-- goodbye see you soon -->") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "comment()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedComment() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable("comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedComments() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable("comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentszSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<!-- hello world -->") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<!-- hello world -->"), UntypedValueRep.value("<!-- goodbye see you soon -->") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected comment(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstruction() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<?hello world?>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedProcessingInstruction() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "processing-instruction()", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedProcessingInstruction() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable("processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedProcessingInstructions() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<?hello world?>"), UntypedValueRep.value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<?hello world?>");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstructions() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<?hello world?>"), UntypedValueRep.value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedProcessingInstructions() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "processing-instruction()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedProcessingInstruction() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable("processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedProcessingInstructions() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("<?hello world?>");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "processing-instruction()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedProcessingInstruction() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable("processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedProcessingInstructions() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable("processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<?hello world?>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<?hello world?>"), UntypedValueRep.value("<?goodbye see-you-soon?>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "processing-instruction()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedProcessingInstruction() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable("processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedProcessingInstructions() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable("processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionszSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<?hello world?>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("<?hello world?>"), UntypedValueRep.value("<?goodbye see-you-soon?>") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected processing-instruction(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedText() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("hello world");
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as text()
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello world") };
        queryPostWithExternalVariable(expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedText() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "text()", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedText() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable("text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedTexts() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("hello world"), UntypedValueRep.value("goodbye see you soon") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("hello world");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedTexts() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("hello world"), UntypedValueRep.value("goodbye see you soon") };
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as text()
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello world"), value(Type.STRING, "goodbye see you soon") };
        queryPostWithExternalVariable(expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedTexts() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "text()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedText() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable("text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedTexts() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = UntypedValueRep.value("hello world");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "text()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedText() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable("text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedTexts() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable("text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("hello world") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("hello world"), UntypedValueRep.value("goodbye see you soon") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "text()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedText() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable("text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedTexts() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable("text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextszSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("hello world") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { UntypedValueRep.value("hello world"), UntypedValueRep.value("goodbye see you soon") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected text(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedAttribute() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value("hello", "world");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedAttribute() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "hello", "world");
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "attribute()", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableAttributeSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "attribute()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeSuppliedAttribute() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "xevt:hello", "world");
        queryPostWithExternalVariable("attribute()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeSuppliedAttributes() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world"), value("goodbye", "see you soon") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "attribute()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value("xevt:hello", "world");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "attribute()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedAttributes() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world"), value("goodbye", "see you soon") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedAttributes() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "xevt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "attribute()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedAttribute() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "xevt:hello", "world");
        queryPostWithExternalVariable("attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedAttributes() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "xevt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value("xevt:hello", "world");
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "attribute()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPTY0004, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedAttribute() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "xevt:hello", "world");
        queryPostWithExternalVariable("attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedAttributes() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "xevt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable("attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world"), value("goodbye", "see you soon") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezNotSupplied() throws XMLDBException {
        queryPostWithExternalVariable(ErrorCodes.W3CErrorCode.XPDY0002, "attribute()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedEmpty() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable("attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedAttribute() throws XMLDBException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "xevt:hello", "world");
        queryPostWithExternalVariable("attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedAttributes() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "xevt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable("attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeszSuppliedUntyped() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedUntypeds() throws XMLDBException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("xevt:hello", "world"), value("goodbye", "see you soon") };
        final String expectedResponseError = "Invalid type for variable $local:my-variable. Expected attribute(), got xs:string";
        queryPostWithExternalVariable(Tuple(ErrorCodes.W3CErrorCode.XPTY0004, expectedResponseError), "attribute()*", externalVariable);
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
        final Either<XMLDBException, ResourceSet> response = doPostWithAuth(externalVariableValue, query);

        if (expectedResponse._1 == null) {
            // We expect success
            assertTrue(response.isRight());
            final ResourceSet actualResultSet = response.right().get();

            final List<Tuple2<String, Object>> expectedResults = buildExistVariableResultSequence(expectedResult);

            assertEquals(expectedResults.size(), actualResultSet.getSize());
            for (int i = 0; i < expectedResults.size(); i++) {
                final Tuple2<String, Object> expected = expectedResults.get(i);
                final EXistResource actual = (EXistResource) actualResultSet.getResource(i);

                if (expected._1 != null) {
                    assertEquals(expected._1, actual.getTypeName());
                }
                assertEquals(expected._2, actual.getContent());
            }

        } else {
            // We expect an error, so check the error is the expected one
            assertTrue(response.isLeft());
            final XMLDBException errorResponse = response.left().get();
            assertTrue(errorResponse.getCause() instanceof XPathException);
            final XPathException errorResponseXPathException = (XPathException) errorResponse.getCause();
            assertEquals(expectedResponse._1, errorResponseXPathException.getErrorCode());

            if (expectedResponse._2 != null) {
                String expectedResponseMessage = expectedResponse._2;
                assertEquals(expectedResponseMessage, errorResponseXPathException.getDetailMessage());
            }
        }
    }

    private static List<Tuple2<String, Object>> buildExistVariableResultSequence(final ExternalVariableValueRep... resultSequence) {
        final List<Tuple2<String, Object>> results = new ArrayList<>();
        for (final ExternalVariableValueRep resultItem : resultSequence) {

            @Nullable String type = null;
            Object value = null;

            if (resultItem instanceof TypedValueRep) {
                type = Type.getTypeName(((TypedValueRep) resultItem).getXdmType());
            }

            if (resultItem instanceof ValueRep) {
                value = ((ValueRep) resultItem).getContent();
            }

            results.add(Tuple(type, value));
        }
        return results;
    }

    private Either<XMLDBException, ResourceSet> doPostWithAuth(@Nullable final Object[] externalVariableValue, final String query) throws XMLDBException {
        try (final Collection dbCollection = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
            final XQueryService xqueryService = (XQueryService) dbCollection.getService("XQueryService", "1.0");

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
                return Either.Right(xqueryService.execute(compiled));
            } catch (final XMLDBException e) {
                return Either.Left(e);
            }
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
                    final Document document = parse(((ValueRep) externalVariableItem).getContent());
                    values[i] = document;

                } else if (type == Type.ELEMENT) {
                    final Element element = parse(((ValueRep) externalVariableItem).getContent()).getDocumentElement();
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
                    final String attrValue = ((ValueRep) externalVariableItem).getContent();
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

            } else if (externalVariableItem instanceof ValueRep) {
                values[i] = ((ValueRep) externalVariableItem).getContent();

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

    private interface ValueRep extends ExternalVariableValueRep {
        String getContent();
    }

    private interface NamedValueRep extends ValueRep {
        String getName();
    }

    static class UntypedValueRep implements ValueRep, ExternalVariableUntypedValueRep {
        private final String content;

        public static UntypedValueRep value(final String content) {
            return new UntypedValueRep(content);
        }

        private UntypedValueRep(final String content) {
            this.content = content;
        }

        @Override
        public String getContent() {
            return content;
        }
    }

    static class UntypedNamedValueRep extends UntypedValueRep implements NamedValueRep {
        private final String name;

        public static UntypedNamedValueRep value(final String name, final String content) {
            return new UntypedNamedValueRep(name, content);
        }

        private UntypedNamedValueRep(final String name, final String content) {
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

        public static TypedValueRep value(final int xdmType, final String content) {
            return new TypedValueRep(xdmType, content);
        }

        private TypedValueRep(final int xdmType, final String content) {
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

        public static TypedNamedValueRep value(final int xdmType, final String name, final String content) {
            return new TypedNamedValueRep(xdmType, name, content);
        }

        private TypedNamedValueRep(final int xdmType, final String name, final String content) {
            super(xdmType, content);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
