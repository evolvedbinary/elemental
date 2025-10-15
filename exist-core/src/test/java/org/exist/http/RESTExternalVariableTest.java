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
package org.exist.http;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import com.googlecode.junittoolbox.ParallelRunner;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.eclipse.jetty.http.HttpStatus;
import org.exist.TestUtils;
import org.exist.test.ExistWebServer;
import org.exist.util.MapUtil;
import org.exist.util.io.InputStreamUtil;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.value.Type;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Attr;
import org.xmlunit.matchers.CompareMatcher;
import org.xmlunit.util.Predicate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.http.RESTExternalVariableTest.SequenceRep.sequence;
import static org.exist.http.RESTExternalVariableTest.TypedArrayRep.array;
import static org.exist.http.RESTExternalVariableTest.TypedNamedValueRep.value;
import static org.exist.http.RESTExternalVariableTest.TypedValueRep.value;
import static org.exist.http.RESTExternalVariableTest.UntypedArrayRep.untypedArray;
import static org.exist.http.RESTExternalVariableTest.UntypedNamedValueRep.value;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * See: <a href="https://github.com/eXist-db/exist/issues/5844">[BUG] The JavaDoc comments for variable in the REST API are inconsistent with the implementation</a>.
 *
 * @author <a href="mailto:adam@evolvedbinary.com>Adam Retter</a>
 */
@RunWith(ParallelRunner.class)
public class RESTExternalVariableTest {

    @ClassRule
    public static final ExistWebServer existWebServer = new ExistWebServer(true, false, true, true);

    private static final String ADMIN_CREDENTIALS = Base64.encodeBase64String((TestUtils.ADMIN_DB_USER + ":" + TestUtils.ADMIN_DB_PWD).getBytes(UTF_8));

    private static final String TEST_NAMESPACE = "http://RESTExternalVariableTest";
    private static final String TEST_PREFIX = "revt";

    private static final Map<String, String> NS_CONTEXT = MapUtil.hashMap(
        Tuple("xs", "http://www.w3.org/2001/XMLSchema"),
        Tuple("exist", "http://exist.sourceforge.net/NS/exist"),
        Tuple("sx", "http://exist-db.org/xquery/types/serialized"),
        Tuple(TEST_PREFIX, TEST_NAMESPACE)
    );

    private static final Predicate<Attr> IGNORE_EXIST_TIMING_ATTRIBUTES = attr -> !("http://exist.sourceforge.net/NS/exist".equals(attr.getNamespaceURI()) && ("compilation-time".equals(attr.getLocalName()) || "execution-time".equals(attr.getLocalName())));

    @Test
    public void queryPostWithExternalVariableUntypedNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, null, null);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string", null);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[]{ value("hello"), value("goodbye") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[]{ value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string+", null);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string*", null);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedElementValue() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedElement() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()", null);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedElement() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedElement() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()+", null);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedElement() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()*", null);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedElement() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()", null);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<hello>world</hello>");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()+", null);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()*", null);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()", null);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()+", null);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()*", null);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()", null);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()+", null);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()*", null);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello world");
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as text()
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello world") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()", null);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello world");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as text()
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello world"), value(Type.STRING, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello world");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()+", null);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()*", null);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()", null);
    }

    @Test
    public void queryPostWithExternalVariableAttributeSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeSuppliedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "revt:hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()", externalVariable);
    }


    @Test
    public void queryPostWithExternalVariableAttributeSuppliedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world"), value("goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("revt:hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world"), value("goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "revt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "revt:hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "revt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("revt:hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()+", null);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "revt:hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "revt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world"), value("goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()*", null);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "revt:hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "revt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world"), value("goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArrayNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)", null);
    }

    @Test
    public void queryPostWithExternalVariableArraySuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArraySuppliedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArraySuppliedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArraySuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { untypedArray(sequence(value(Type.STRING, "hello"))), untypedArray(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptArrayNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptArraySuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptArraySuppliedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptArraySuppliedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptArraySuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArraysNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)+", null);
    }

    @Test
    public void queryPostWithExternalVariableArraysSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArraysSuppliedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArraysSuppliedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArraysSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArraysSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { untypedArray(sequence(value(Type.STRING, "hello"))), untypedArray(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArrayzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)*", null);
    }

    @Test
    public void queryPostWithExternalVariableArrayzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArrayzSuppliedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArrayzSuppliedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArrayszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableArrayzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { untypedArray(sequence(value(Type.STRING, "hello"))), untypedArray(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)*", externalVariable);
    }

    private void queryPostWithExternalVariable(final int expectedResponseCode, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws IOException {
        queryPostWithExternalVariable(Tuple(expectedResponseCode, null), externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final Tuple2<Integer, String> expectedResponse, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws IOException {
        queryPostWithExternalVariable(expectedResponse, externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final int expectedResponseCode, final ExternalVariableValueRep[] expectedResult, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws IOException {
        queryPostWithExternalVariable(Tuple(expectedResponseCode, null), expectedResult, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final Tuple2<Integer, String> expectedResponse, final ExternalVariableValueRep[] expectedResult, @Nullable final String xqExternalVariableType, final ExternalVariableValueRep... externalVariableSequence) throws IOException {
        final String query = buildQueryExternalVariable(xqExternalVariableType, externalVariableSequence);

        final HttpResponse response = doPostWithAuth(getResourceUri(), query);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();

        assertEquals("Server returned response code: " + resultStatusCode, (int) expectedResponse._1, resultStatusCode);

        if (expectedResponse._1 == HttpStatus.OK_200) {
            final String expected = buildExistVariableResultSequence(expectedResult);

            final String data = readResponse(response.getEntity());
            assertThat(data, CompareMatcher.isIdenticalTo(expected).withNamespaceContext(NS_CONTEXT).withAttributeFilter(IGNORE_EXIST_TIMING_ATTRIBUTES).ignoreWhitespace());
        } else if (expectedResponse._2 != null) {
            final String data = readResponse(response.getEntity());
            assertThat(data, CompareMatcher.isIdenticalTo(expectedResponse._2).withNamespaceContext(NS_CONTEXT).withAttributeFilter(IGNORE_EXIST_TIMING_ATTRIBUTES).ignoreWhitespace());
        }
    }

    private static String buildExistVariableResultSequence(final ExternalVariableValueRep... resultSequence) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<exist:result xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" xmlns:sx=\"http://exist-db.org/xquery/types/serialized\" exist:count=\"").append(resultSequence.length).append("\" exist:hits=\"").append(resultSequence.length).append("\" exist:start=\"1\">\n");
        for (final ExternalVariableValueRep resultSequenceItem : resultSequence) {
            buildExistVariableResultSequenceItem(builder, resultSequenceItem);
        }
        builder.append("</exist:result>");
        return builder.toString();
    }

    private static void buildExistVariableResultSequenceItem(final StringBuilder builder, final ExternalVariableValueRep resultSequenceItem) {
        // TODO(AR) When wrap="yes" is set for the eXist-db REST API it does not wrap Node Types in <exist:value> - that is probably a bug in eXist-db that should be fixed - https://github.com/eXist-db/exist/issues/5909
        final int xdmType;
        if (resultSequenceItem instanceof ExternalVariableTypedValueRep) {
            xdmType = ((ExternalVariableTypedValueRep) resultSequenceItem).getXdmType();
        } else {
            if (resultSequenceItem instanceof NamedValueRep) {
                xdmType = Type.ATTRIBUTE;
            } else if (resultSequenceItem instanceof ValueRep && ((ValueRep) resultSequenceItem).getContent().startsWith("<")) {
                xdmType = Type.ELEMENT;
            } else {
                xdmType = Type.ITEM;
            }
        }

        if (!Type.subTypeOf(xdmType, Type.NODE) && !isAttribute(xdmType, resultSequenceItem) && !isArray(xdmType, resultSequenceItem)) {
            builder.append("\t<exist:value");
            if (resultSequenceItem instanceof ExternalVariableTypedValueRep) {
                builder.append(" exist:type=\"").append(Type.getTypeName(((ExternalVariableTypedValueRep) resultSequenceItem).getXdmType())).append("\"");
            }
            builder.append('>');

        } else if (xdmType == Type.DOCUMENT) {
            builder.append("<exist:document>");

        } else if (xdmType == Type.TEXT) {
            builder.append("<exist:text>");

        } else if (xdmType == Type.ATTRIBUTE) {
            builder.append("<exist:attribute");
            final String attrNameString = ((NamedValueRep) resultSequenceItem).getName();

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

            builder.append(" exist:local=\"").append(attrLocalName).append('"');
            if (attrPrefix != null) {
                builder.append(" exist:prefix=\"").append(attrPrefix).append('"');
            }
            if (attrPrefix != null) {
                builder.append(" exist:target-namespace=\"").append(attrNamespace).append('"');
            }
            builder.append('>');

        } else if (isArray(xdmType, resultSequenceItem)) {
            builder.append("<exist:array>");
        }

        if (resultSequenceItem instanceof ArrayRep) {
            // Array type
            for (final SequenceRep arrayEntry : ((ArrayRep) resultSequenceItem).getValues()) {
                builder.append("<exist:sequence>\n");
                for (final ValueRep arrayEntryValue : arrayEntry.values) {
                    buildExistVariableResultSequenceItem(builder, arrayEntryValue);
                }
                builder.append("</exist:sequence>\n");
            }

        } else {
            builder.append(((ValueRep) resultSequenceItem).getContent());
        }

        if (!Type.subTypeOf(xdmType, Type.NODE) && !isAttribute(xdmType, resultSequenceItem) && !isArray(xdmType, resultSequenceItem)) {
            builder.append("</exist:value>\n");

        } else if (xdmType == Type.DOCUMENT) {
            builder.append("</exist:document>");

        } else if (xdmType == Type.TEXT) {
            builder.append("</exist:text>");

        } else if (xdmType == Type.ATTRIBUTE) {
            builder.append("</exist:attribute>");

        } else if (isArray(xdmType, resultSequenceItem)) {
            builder.append("</exist:array>");
        }
    }

    private static boolean isAttribute(final int xdmType, final ExternalVariableValueRep externalVariableValueRep) {
        return xdmType == Type.ATTRIBUTE || (xdmType == Type.ITEM && externalVariableValueRep instanceof NamedValueRep);
    }

    private static boolean isArray(final int xdmType, final ExternalVariableValueRep externalVariableValueRep) {
        return xdmType == Type.ARRAY_ITEM || (xdmType == Type.ITEM && externalVariableValueRep instanceof ArrayRep);
    }

    private static String buildQueryExternalVariable(@Nullable final String xqExternalVariableType, @Nullable final ExternalVariableValueRep... externalVariableSequence) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<exist:query xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" xmlns:sx=\"http://exist-db.org/xquery/types/serialized\" xmlns:" + TEST_PREFIX + "=\"" + TEST_NAMESPACE + "\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" wrap=\"yes\" typed=\"yes\">\n");

        if (externalVariableSequence!= null) {
            builder.append("\t<exist:variables>\n");
            builder.append("\t\t<exist:variable>\n");
            builder.append("\t\t\t<exist:qname><exist:prefix>local</exist:prefix><exist:localname>my-variable</exist:localname></exist:qname>\n");
            buildQueryExternalVariableSequence(builder, 3, externalVariableSequence);
            builder.append("\t\t</exist:variable>\n");
            builder.append("\t</exist:variables>\n");
        }

        builder.append("\t<exist:text><![CDATA[\n");
        builder.append("declare variable $local:my-variable");
        if (xqExternalVariableType != null) {
            builder.append(" as ").append(xqExternalVariableType);
        }
        builder.append(" external;\n");
        builder.append("$local:my-variable\n");
        builder.append("\t]]></exist:text>\n");
        builder.append("</exist:query>\n");

        return builder.toString();
    }

    private static final char[] INDENTS = { '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t' };

    private static void buildQueryExternalVariableSequence(final StringBuilder builder, final int indentCount, final ExternalVariableValueRep... externalVariableSequence) {
        builder.append(INDENTS, 0, indentCount).append("<sx:sequence>\n");
        for (final ExternalVariableValueRep externalVariableSequenceItem : externalVariableSequence) {
            builder.append(INDENTS, 0, indentCount + 1).append("<sx:value");
            if (externalVariableSequenceItem instanceof ExternalVariableTypedValueRep) {
                builder.append(" type=\"").append(Type.getTypeName(((ExternalVariableTypedValueRep) externalVariableSequenceItem).getXdmType())).append("\"");
            }

            if (externalVariableSequenceItem instanceof NamedValueRep) {
                // Attribute Node type
                final String name = ((NamedValueRep) externalVariableSequenceItem).getName();
                builder.append(" name=\"").append(name).append("\"");
            }
            builder.append('>');

            if (externalVariableSequenceItem instanceof ArrayRep) {
                // Array type
                builder.append('\n');
                for (final SequenceRep sequenceRep : ((ArrayRep) externalVariableSequenceItem).getValues()) {
                    buildQueryExternalVariableSequence(builder, indentCount + 2, sequenceRep.values);
                }
                builder.append(INDENTS, 0, indentCount + 1).append("</sx:value>\n");

            } else {
                builder.append(((ValueRep) externalVariableSequenceItem).getContent());
                builder.append("</sx:value>\n");
            }
        }
        builder.append(INDENTS, 0, indentCount).append("</sx:sequence>\n");
    }

    private static String getServerUri() {
        return "http://localhost:" + existWebServer.getPort() + "/rest";
    }

    private static String getResourceUri() {
        return getServerUri() + XmldbURI.ROOT_COLLECTION + "/test/test.xml";
    }

    private HttpResponse doPostWithAuth(final String uri, final String content) throws IOException {
        return Request.Post(uri)
            .setHeader("Authorization", "Basic " + ADMIN_CREDENTIALS)
            .bodyString(content, ContentType.APPLICATION_XML)
            .execute()
            .returnResponse();
    }

    private String readResponse(final HttpEntity response) throws IOException {
        try (final InputStream is = response.getContent()) {
            return InputStreamUtil.readString(is, UTF_8);
        }
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
}
