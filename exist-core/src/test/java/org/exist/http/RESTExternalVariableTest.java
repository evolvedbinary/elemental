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
import org.exist.xquery.XPathException;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

    private static final Map<String, String> NS_CONTEXT = MapUtil.HashMap(
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
        final Tuple2<String, String> externalVariable = Tuple(null, "hello");
        final Tuple2<String, String>[] expectedResult = new Tuple2[] { Tuple("xs:string", "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedString() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("xs:string", "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string", null);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedString() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("xs:string", "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedStrings() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "hello"), Tuple(null, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "hello");
        final Tuple2<String, String>[] expectedResult = new Tuple2[] { Tuple("xs:string", "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedStrings() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "hello"), Tuple(null, "goodbye") };
        final Tuple2<String, String>[] expectedResult = new Tuple2[] { Tuple("xs:string", "hello"), Tuple("xs:string", "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedStrings() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("xs:string", "hello"), Tuple("xs:string", "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedString() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("xs:string", "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedStrings() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("xs:string", "hello"), Tuple("xs:string", "goodbye") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptStringSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "hello");
        final Tuple2<String, String>[] expectedResult = new Tuple2[] { Tuple("xs:string", "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string+", null);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedString() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("xs:string", "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedStrings() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("xs:string", "hello"), Tuple("xs:string", "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "hello");
        final Tuple2<String, String>[] expectedResult = new Tuple2[] { Tuple("xs:string", "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringsSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "hello"), Tuple(null, "goodbye") };
        final Tuple2<String, String>[] expectedResult = new Tuple2[]{ Tuple("xs:string", "hello"), Tuple("xs:string", "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string*", null);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedString() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("xs:string", "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedStrings() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("xs:string", "hello"), Tuple("xs:string", "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "hello");
        final Tuple2<String, String>[] expectedResult = new Tuple2[]{ Tuple("xs:string", "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableStringzSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "hello"), Tuple(null, "goodbye") };
        final Tuple2<String, String>[] expectedResult = new Tuple2[]{ Tuple("xs:string", "hello"), Tuple("xs:string", "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedElementValue() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedElement() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("element()", "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()", null);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedElement() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("element()", "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedElements() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>"), Tuple(null, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedElements() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>"), Tuple(null, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedElements() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("element()", "<hello>world</hello>"), Tuple("element()", "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedElement() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("element()", "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedElements() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("element()", "<hello>world</hello>"), Tuple("element()", "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptElementSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()+", null);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedElement() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("element()", "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedElements() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("element()", "<hello>world</hello>"), Tuple("element()", "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedUntyped() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementsSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>"), Tuple(null, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()*", null);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedElement() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("element()", "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedElements() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("element()", "<hello>world</hello>"), Tuple("element()", "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementszSuppliedUntyped() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableElementzSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>"), Tuple(null, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedDocument() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedDocument() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("document-node()", "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()", null);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedDocument() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("document-node()", "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedDocuments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>"), Tuple(null, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<hello>world</hello>");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedDocuments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>"), Tuple(null, "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedDocuments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("document-node()", "<hello>world</hello>"), Tuple("document-node()", "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedDocument() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("document-node()", "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedDocuments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("document-node()", "<hello>world</hello>"), Tuple("document-node()", "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptDocumentSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<hello>world</hello>");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()+", null);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedDocument() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("document-node()", "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedDocuments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("document-node()", "<hello>world</hello>"), Tuple("document-node()", "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedUntyped() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentsSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>"), Tuple(null, "<goodbye>see you soon</goodbye>") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()*", null);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedDocument() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("document-node()", "<hello>world</hello>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedDocuments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("document-node()", "<hello>world</hello>"), Tuple("document-node()", "<goodbye>see you soon</goodbye>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentszSuppliedUntyped() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableDocumentzSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<hello>world</hello>"), Tuple(null, "<goodbye>see you soon</goodbye>") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedComment() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedComment() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("comment()", "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()", null);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedComment() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("comment()", "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedComments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<!-- hello world -->"), Tuple(null, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedComments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<!-- hello world -->"), Tuple(null, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedComments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("comment()", "<!-- hello world -->"), Tuple("comment()", "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedComment() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("comment()", "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedComments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("comment()", "<!-- hello world -->"), Tuple("comment()", "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptCommentSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()+", null);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedComment() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("comment()", "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedComments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("comment()", "<!-- hello world -->"), Tuple("comment()", "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedUntyped() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<!-- hello world -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentsSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<!-- hello world -->"), Tuple(null, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()*", null);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedComment() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("comment()", "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedComments() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("comment()", "<!-- hello world -->"), Tuple("comment()", "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentszSuppliedUntyped() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<!-- hello world -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableCommentzSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<!-- hello world -->"), Tuple(null, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstruction() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedProcessingInstruction() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("processing-instruction()", "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()", null);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedProcessingInstruction() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("processing-instruction()", "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedProcessingInstructions() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<?hello world?>"), Tuple(null, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstructions() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<?hello world?>"), Tuple(null, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedProcessingInstructions() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("processing-instruction()", "<?hello world?>"), Tuple("processing-instruction()", "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedProcessingInstruction() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("processing-instruction()", "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedProcessingInstructions() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("processing-instruction()", "<?hello world?>"), Tuple("processing-instruction()", "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptProcessingInstructionSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()+", null);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedProcessingInstruction() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("processing-instruction()", "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedProcessingInstructions() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("processing-instruction()", "<?hello world?>"), Tuple("processing-instruction()", "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedUntyped() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<?hello world?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionsSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<?hello world?>"), Tuple(null, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()*", null);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedProcessingInstruction() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("processing-instruction()", "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedProcessingInstructions() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("processing-instruction()", "<?hello world?>"), Tuple("processing-instruction()", "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionszSuppliedUntyped() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<?hello world?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableProcessingInstructionzSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "<?hello world?>"), Tuple(null, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedText() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "hello world");
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as text()
        final Tuple2<String, String>[] expectedResult = new Tuple2[] { Tuple("xs:string", "hello world") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedText() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("text()", "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()", null);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedText() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("text()", "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedTexts() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "hello world"), Tuple(null, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "hello world");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedTexts() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "hello world"), Tuple(null, "goodbye see you soon") };
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as text()
        final Tuple2<String, String>[] expectedResult = new Tuple2[] { Tuple("xs:string", "hello world"), Tuple("xs:string", "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedTexts() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("text()", "hello world"), Tuple("text()", "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedText() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("text()", "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedTexts() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("text()", "hello world"), Tuple("text()", "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptTextSuppliedUntyped() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple(null, "hello world");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()+", null);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedText() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("text()", "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedTexts() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("text()", "hello world"), Tuple("text()", "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedUntyped() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "hello world") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextsSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "hello world"), Tuple(null, "goodbye see you soon") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()*", null);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedEmpty() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedText() throws IOException {
        final Tuple2<String, String> externalVariable = Tuple("text()", "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedTexts() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple("text()", "hello world"), Tuple("text()", "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextszSuppliedUntyped() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "hello world") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableTextzSuppliedUntypeds() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, "hello world"), Tuple(null, "goodbye see you soon") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>exerr:ERROR XPTY0004: Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedAttribute() throws IOException {
        final Tuple2<String, Tuple2<String, String>> externalVariable = Tuple(null, Tuple("hello", "world"));
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedAttribute() throws IOException {
        final Tuple2<String, Tuple2<String, String>> externalVariable = Tuple("attribute()", Tuple("hello", "world"));
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()", null);
    }

    @Test
    public void queryPostWithExternalVariableAttributeSuppliedEmpty() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeSuppliedAttribute() throws IOException {
        final Tuple2<String, Tuple2<String, String>> externalVariable = Tuple("attribute()", Tuple("revt:hello", "world"));
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()", externalVariable);
    }


    @Test
    public void queryPostWithExternalVariableAttributeSuppliedAttributes() throws IOException {
        final Tuple2<String, String>[] externalVariable = new Tuple2[]{ Tuple(null, Tuple("revt:hello", "world")), Tuple(null, Tuple("goodbye", "see you soon")) };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeSuppliedUntyped() throws IOException {
        final Tuple2<String, Tuple2<String, String>> externalVariable = Tuple(null, Tuple("revt:hello", "world"));
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedAttributes() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[]{ Tuple(null, Tuple("revt:hello", "world")), Tuple(null, Tuple("goodbye", "see you soon")) };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableUntypedSuppliedAttributes() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[]{ Tuple("attribute()", Tuple("revt:hello", "world")), Tuple("attribute()", Tuple("goodbye", "see you soon")) };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()?", null);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedEmpty() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedAttribute() throws IOException {
        final Tuple2<String, Tuple2<String, String>> externalVariable = Tuple("attribute()", Tuple("revt:hello", "world"));
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedAttributes() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[]{ Tuple("attribute()", Tuple("revt:hello", "world")), Tuple("attribute()", Tuple("goodbye", "see you soon")) };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableOptAttributeSuppliedUntyped() throws IOException {
        final Tuple2<String, Tuple2<String, String>> externalVariable = Tuple(null, Tuple("revt:hello", "world"));
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()?", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()+", null);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedEmpty() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedAttribute() throws IOException {
        final Tuple2<String, Tuple2<String, String>> externalVariable = Tuple("attribute()", Tuple("revt:hello", "world"));
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedAttributes() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[]{ Tuple("attribute()", Tuple("revt:hello", "world")), Tuple("attribute()", Tuple("goodbye", "see you soon")) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedUntyped() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[]{ Tuple(null, Tuple("revt:hello", "world")) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributesSuppliedUntypeds() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[]{ Tuple(null, Tuple("revt:hello", "world")), Tuple(null, Tuple("goodbye", "see you soon")) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()+", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()*", null);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedEmpty() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedAttribute() throws IOException {
        final Tuple2<String, Tuple2<String, String>> externalVariable = Tuple("attribute()", Tuple("revt:hello", "world"));
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedAttributes() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[]{ Tuple("attribute()", Tuple("revt:hello", "world")), Tuple("attribute()", Tuple("goodbye", "see you soon")) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributeszSuppliedUntyped() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[]{ Tuple(null, Tuple("revt:hello", "world")) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    public void queryPostWithExternalVariableAttributezSuppliedUntypeds() throws IOException {
        final Tuple2<String, Tuple2<String, String>>[] externalVariable = new Tuple2[]{ Tuple(null, Tuple("revt:hello", "world")), Tuple(null, Tuple("goodbye", "see you soon")) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    private void queryPostWithExternalVariable(final int expectedResponseCode, @Nullable final String xqExternalVariableType, final Tuple2<String, ?>... externalVariableSequence) throws IOException {
        queryPostWithExternalVariable(Tuple(expectedResponseCode, null), externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final Tuple2<Integer, String> expectedResponse, @Nullable final String xqExternalVariableType, final Tuple2<String, ?>... externalVariableSequence) throws IOException {
        queryPostWithExternalVariable(expectedResponse, externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final int expectedResponseCode, final Tuple2<String, ?>[] expectedResult, @Nullable final String xqExternalVariableType, final Tuple2<String, ?>... externalVariableSequence) throws IOException {
        queryPostWithExternalVariable(Tuple(expectedResponseCode, null), expectedResult, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final Tuple2<Integer, String> expectedResponse, final Tuple2<String, ?>[] expectedResult, @Nullable final String xqExternalVariableType, final Tuple2<String, ?>... externalVariableSequence) throws IOException {
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

    private static String buildExistVariableResultSequence(final Tuple2<String, ?>... resultSequence) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<exist:result xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" xmlns:sx=\"http://exist-db.org/xquery/types/serialized\" exist:count=\"").append(resultSequence.length).append("\" exist:hits=\"").append(resultSequence.length).append("\" exist:start=\"1\">\n");
        for (final Tuple2<String, ?> resultSequenceItem : resultSequence) {

            // TODO(AR) When wrap="yes" is set for the eXist-db REST API it does not wrap Node Types in <exist:value> - that is probably a bug in eXist-db that should be fixed - https://github.com/eXist-db/exist/issues/5909
            final int xdmType;
            if (resultSequenceItem._1 == null) {
                if (resultSequenceItem._2 instanceof Tuple2) {
                    xdmType = Type.ATTRIBUTE;
                } else {
                    xdmType = resultSequenceItem._2.toString().startsWith("<") ? Type.ELEMENT : Type.ITEM;
                }
            } else {
                try {
                    xdmType = Type.getType(resultSequenceItem._1);
                } catch (final XPathException e) {
                    fail("Unable to find XDM type value for: " + resultSequenceItem._1);
                    return null;
                }
            }

            if (!Type.subTypeOf(xdmType, Type.NODE)) {
                builder.append("\t<exist:value");
                if (resultSequenceItem._1 != null) {
                    builder.append(" exist:type=\"").append(resultSequenceItem._1).append("\"");
                }
                builder.append('>');

            } else if (xdmType == Type.DOCUMENT) {
                builder.append("<exist:document>");

            } else if (xdmType == Type.TEXT) {
                builder.append("<exist:text>");

            } else if (xdmType == Type.ATTRIBUTE) {
                builder.append("<exist:attribute");
                if (resultSequenceItem._2 instanceof Tuple2) {
                    final String attrNameString = ((Tuple2<String, String>) resultSequenceItem._2)._1;

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
                }
                builder.append('>');
            }

            if (resultSequenceItem._2 instanceof Tuple2) {
                final String value = ((Tuple2<String, String>) resultSequenceItem._2)._2;
                builder.append(value);
            } else {
                builder.append(resultSequenceItem._2.toString());
            }

            if (!Type.subTypeOf(xdmType, Type.NODE)) {
                builder.append("</exist:value>\n");

            } else if (xdmType == Type.DOCUMENT) {
                builder.append("</exist:document>");

            } else if (xdmType == Type.TEXT) {
                builder.append("</exist:text>");

            } else if (xdmType == Type.ATTRIBUTE) {
                builder.append("</exist:attribute>");
            }
        }
        builder.append("</exist:result>");
        return builder.toString();
    }

    private static String buildQueryExternalVariable(@Nullable final String xqExternalVariableType, @Nullable final Tuple2<String, ?>... externalVariableSequence) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<exist:query xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" xmlns:sx=\"http://exist-db.org/xquery/types/serialized\" xmlns:" + TEST_PREFIX + "=\"" + TEST_NAMESPACE + "\" wrap=\"yes\" typed=\"yes\">\n");

        if (externalVariableSequence!= null) {
            builder.append("\t<exist:variables>\n");
            builder.append("\t\t<exist:variable>\n");
            builder.append("\t\t\t<exist:qname><exist:prefix>local</exist:prefix><exist:localname>my-variable</exist:localname></exist:qname>");
            builder.append("\t\t\t<sx:sequence>\n");
            for (final Tuple2<String, ?> externalVariableSequenceItem : externalVariableSequence) {
                builder.append("\t\t\t\t<sx:value");
                if (externalVariableSequenceItem._1 != null) {
                    builder.append(" type=\"").append(externalVariableSequenceItem._1).append("\"");
                }
                if (externalVariableSequenceItem._2 instanceof Tuple2) {
                    final String name = ((Tuple2<String, String>) externalVariableSequenceItem._2)._1;
                    builder.append(" name=\"").append(name).append("\"");
                }
                builder.append('>');
                if (externalVariableSequenceItem._2 instanceof Tuple2) {
                    final String value = ((Tuple2<String, String>) externalVariableSequenceItem._2)._2;
                    builder.append(value);
                } else {
                    builder.append(externalVariableSequenceItem._2);
                }
                builder.append("</sx:value>\n");
            }
            builder.append("\t\t\t</sx:sequence>\n");
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
}
