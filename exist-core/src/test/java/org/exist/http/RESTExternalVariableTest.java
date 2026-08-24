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
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.eclipse.jetty.http.HttpStatus;
import org.exist.Namespaces;
import org.exist.TestUtils;
import org.exist.test.DatabaseWebServerExtension;
import org.exist.util.MapUtil;
import org.exist.util.io.InputStreamUtil;
import org.exist.xmldb.XmldbURI;
import org.exist.xqj.Marshaller;
import org.exist.xquery.value.Type;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.runners.Parameterized;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;
import org.xmlunit.util.Predicate;

import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.http.RESTExternalVariableTest.EntryRep.entry;
import static org.exist.http.RESTExternalVariableTest.KeyRep.key;
import static org.exist.http.RESTExternalVariableTest.SequenceRep.sequence;
import static org.exist.http.RESTExternalVariableTest.TypedArrayRep.array;
import static org.exist.http.RESTExternalVariableTest.TypedMapRep.map;
import static org.exist.http.RESTExternalVariableTest.TypedNamedValueRep.value;
import static org.exist.http.RESTExternalVariableTest.TypedValueRep.value;
import static org.exist.http.RESTExternalVariableTest.UntypedArrayRep.untypedArray;
import static org.exist.http.RESTExternalVariableTest.UntypedMapRep.untypedMap;
import static org.exist.http.RESTExternalVariableTest.UntypedNamedValueRep.value;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

/**
 * See: <a href="https://github.com/eXist-db/exist/issues/5844">[BUG] The JavaDoc comments for variable in the REST API are inconsistent with the implementation</a>.
 *
 * @author <a href="mailto:adam@evolvedbinary.com>Adam Retter</a>
 */
@Execution(ExecutionMode.CONCURRENT)
public class RESTExternalVariableTest {

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "xmlns-prefixed-ns", true },
            { "xmlns-default-ns", false }
        });
    }

    @Parameterized.Parameter
    public String testTypeName;

    @Parameterized.Parameter(value = 1)
    public boolean useXmlnsPrefixes;

    @RegisterExtension
    public static final DatabaseWebServerExtension DATABASE_WEB_SERVER = new DatabaseWebServerExtension(true, false, true, true);

    private static final String ADMIN_CREDENTIALS = Base64.encodeBase64String((TestUtils.ADMIN_DB_USER + ":" + TestUtils.ADMIN_DB_PWD).getBytes(UTF_8));

    private static final String TEST_NAMESPACE = "http://RESTExternalVariableTest";
    private static final String TEST_PREFIX = "revt";

    private static final Map<String, String> NS_CONTEXT = MapUtil.hashMap(
        Tuple("xs", XMLConstants.W3C_XML_SCHEMA_NS_URI),
        Tuple(Namespaces.EXIST_NS_PREFIX, Namespaces.EXIST_NS),
        Tuple(Marshaller.PREFIX, Marshaller.PREFIX),
        Tuple(TEST_PREFIX, TEST_NAMESPACE)
    );

    private static final Predicate<Attr> IGNORE_EXIST_TIMING_ATTRIBUTES = attr -> !("http://exist.sourceforge.net/NS/exist".equals(attr.getNamespaceURI()) && ("compilation-time".equals(attr.getLocalName()) || "execution-time".equals(attr.getLocalName())));

    private static final DefaultNodeMatcher IGNORE_MAP_ENTRY_ORDER_MATCHER = new DefaultNodeMatcher(ElementSelectors.conditionalBuilder()
        .whenElementIsNamed(new QName(Namespaces.EXIST_NS, "entry", Namespaces.EXIST_NS_PREFIX))
        .thenUse(ElementSelectors.byXPath("./exist:key/exist:value", NS_CONTEXT, ElementSelectors.byNameAndText))
        .elseUse(ElementSelectors.byName)
        .build());

    @Test
    void queryPostWithExternalVariableNotSupplied() throws IOException {
        final String query =
                "<exist:query xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" xmlns:sx=\"http://exist-db.org/xquery/types/serialized\" xmlns:" + TEST_PREFIX + "=\"" + TEST_NAMESPACE + "\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" wrap=\"yes\" typed=\"yes\">\n" +
                "\t<exist:text><![CDATA[\n" +
                "declare variable $local:my-variable as xs:string* external;\n" +
                "$local:my-variable\n" +
                "\t]]></exist:text>\n" +
                "</exist:query>\n";

        final HttpResponse response = doPostWithAuth(getResourceUri(), query);
        final int resultStatusCode = response.getStatusLine()
               .getStatusCode();

        assertEquals(HttpStatus.BAD_REQUEST_400, resultStatusCode, "Server returned response code: " + resultStatusCode);

        final String actual = readResponse(response.getEntity());
        assertThat(actual, CompareMatcher.isIdenticalTo("<exception><path>/db/test/test.xml</path><message>err:XPDY0002 The value of external variable: local:my-variable has not been set</message></exception>"));
    }

    @Test
    void queryPostWithExternalVariableUndeclared() throws IOException {
        final String query =
                "<exist:query xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" xmlns:sx=\"http://exist-db.org/xquery/types/serialized\" xmlns:" + TEST_PREFIX + "=\"" + TEST_NAMESPACE + "\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" wrap=\"yes\" typed=\"yes\">\n" +
                "\t<exist:variables>\n" +
                "\t\t<exist:variable>\n" +
                "\t\t\t<exist:qname><exist:prefix>local</exist:prefix><exist:localname>my-variable</exist:localname></exist:qname>\n" +
                "\t\t\t<sx:sequence><sx:value type=\"xs:string\">hello</sx:value></sx:sequence>\n" +
                "\t\t</exist:variable>\n" +
                "\t\t<exist:variable>\n" +
                "\t\t\t<exist:qname><exist:prefix>local</exist:prefix><exist:localname>other-variable</exist:localname></exist:qname>\n" +
                "\t\t\t<sx:sequence><sx:value type=\"xs:string\">goodbye</sx:value></sx:sequence>\n" +
                "\t\t</exist:variable>\n" +
                "\t</exist:variables>\n" +
                "\t<exist:text><![CDATA[\n" +
                "declare variable $local:my-variable as xs:string* external;\n" +
                "$local:my-variable\n" +
                "\t]]></exist:text>\n" +
                "</exist:query>\n";

        final HttpResponse response = doPostWithAuth(getResourceUri(), query);
        final int resultStatusCode = response.getStatusLine()
                .getStatusCode();

        assertEquals(HttpStatus.BAD_REQUEST_400, resultStatusCode, "Server returned response code: " + resultStatusCode);

        final String actual = readResponse(response.getEntity());

        assertThat(actual, CompareMatcher.isIdenticalTo("<exception><path>/db/test/test.xml</path><message>err:XPDY0002 External variable local:other-variable is not declared in the XQuery</message></exception>"));
    }

    @Test
    void queryPostWithExternalVariableUntypedNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, null, (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableStringSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringSuppliedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringSuppliedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[]{ value("hello"), value("goodbye") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[]{ value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptStringNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string?", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableOptStringSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptStringSuppliedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptStringSuppliedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptStringSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string+", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableStringsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringsSuppliedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringsSuppliedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "xs:string*", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableStringzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringzSuppliedString() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.STRING, "hello");
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringzSuppliedStrings() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "xs:string*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringzSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello");
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableStringzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello"), value("goodbye") };
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello"), value(Type.STRING, "goodbye") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, "xs:string*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedElementValue() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value("<hello>world</hello>");
        } else {
            externalVariable = value("<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedElement() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        } else {
            externalVariable = value(Type.ELEMENT, "<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableElementSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementSuppliedElement() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        } else {
            externalVariable = value(Type.ELEMENT, "<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementSuppliedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>"), value("<goodbye>see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value("<hello>world</hello>");
        } else {
            externalVariable = value("<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>"), value("<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello xmlns=\"\">world</hello>"), value(Type.ELEMENT, "<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptElementNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableOptElementSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptElementSuppliedElement() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        } else {
            externalVariable = value(Type.ELEMENT, "<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptElementSuppliedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello xmlns=\"\">world</hello>"), value(Type.ELEMENT, "<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptElementSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value("<hello>world</hello>");
        } else {
            externalVariable = value("<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableElementsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementsSuppliedElement() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        } else {
            externalVariable = value(Type.ELEMENT, "<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementsSuppliedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello xmlns=\"\">world</hello>"), value(Type.ELEMENT, "<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>"), value("<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "element()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableElementzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementzSuppliedElement() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value(Type.ELEMENT, "<hello>world</hello>");
        } else {
            externalVariable = value(Type.ELEMENT, "<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementzSuppliedElements() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello>world</hello>"), value(Type.ELEMENT, "<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value(Type.ELEMENT, "<hello xmlns=\"\">world</hello>"), value(Type.ELEMENT, "<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableElementzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>"), value("<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "element()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value("<hello>world</hello>");
        } else {
            externalVariable = value("<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        } else {
            externalVariable = value(Type.DOCUMENT, "<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableDocumentSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentSuppliedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        } else {
            externalVariable = value(Type.DOCUMENT, "<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentSuppliedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>"), value("<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value("<hello>world</hello>");
        } else {
            externalVariable = value("<hello xmlns=\"\">world</hello>");
        }
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>"), value("<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello xmlns=\"\">world</hello>"), value(Type.DOCUMENT, "<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptDocumentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableOptDocumentSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptDocumentSuppliedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        } else {
            externalVariable = value(Type.DOCUMENT, "<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptDocumentSuppliedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello xmlns=\"\">world</hello>"), value(Type.DOCUMENT, "<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptDocumentSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value("<hello>world</hello>");
        } else {
            externalVariable = value("<hello xmlns=\"\">world</hello>");
        }
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableDocumentsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentsSuppliedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        } else {
            externalVariable = value(Type.DOCUMENT, "<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentsSuppliedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello xmlns=\"\">world</hello>"), value(Type.DOCUMENT, "<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>") };
        }
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>"), value("<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "document-node()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableDocumentzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentzSuppliedDocument() throws IOException {
        final ExternalVariableValueRep externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = value(Type.DOCUMENT, "<hello>world</hello>");
        } else {
            externalVariable = value(Type.DOCUMENT, "<hello xmlns=\"\">world</hello>");
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentzSuppliedDocuments() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello>world</hello>"), value(Type.DOCUMENT, "<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value(Type.DOCUMENT, "<hello xmlns=\"\">world</hello>"), value(Type.DOCUMENT, "<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        queryPostWithExternalVariable(HttpStatus.OK_200, "document-node()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>") };
        }
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableDocumentzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable;
        if (useXmlnsPrefixes) {
            externalVariable = new ExternalVariableValueRep[] { value("<hello>world</hello>"), value("<goodbye>see you soon</goodbye>") };
        } else {
            externalVariable = new ExternalVariableValueRep[] { value("<hello xmlns=\"\">world</hello>"), value("<goodbye xmlns=\"\">see you soon</goodbye>") };
        }
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected document-node(), got element()</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "document-node()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableCommentSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentSuppliedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentSuppliedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptCommentNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableOptCommentSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptCommentSuppliedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptCommentSuppliedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptCommentSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableCommentsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentsSuppliedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentsSuppliedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "comment()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableCommentzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentzSuppliedComment() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.COMMENT, "<!-- hello world -->");
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentzSuppliedComments() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.COMMENT, "<!-- hello world -->"), value(Type.COMMENT, "<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableCommentzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<!-- hello world -->"), value("<!-- goodbye see you soon -->") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "comment()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionSuppliedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionSuppliedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptProcessingInstructionNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableOptProcessingInstructionSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptProcessingInstructionSuppliedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptProcessingInstructionSuppliedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptProcessingInstructionSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionsSuppliedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionsSuppliedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "processing-instruction()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionzSuppliedProcessingInstruction() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.PROCESSING_INSTRUCTION, "<?hello world?>");
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionzSuppliedProcessingInstructions() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.PROCESSING_INSTRUCTION, "<?hello world?>"), value(Type.PROCESSING_INSTRUCTION, "<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableProcessingInstructionzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("<?hello world?>"), value("<?goodbye see-you-soon?>") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "processing-instruction()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello world");
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as text()
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello world") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableTextSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextSuppliedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextSuppliedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello world");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as text()
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "hello world"), value(Type.STRING, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptTextNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableOptTextSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptTextSuppliedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptTextSuppliedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptTextSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello world");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableTextsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextsSuppliedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextsSuppliedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "text()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableTextzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextzSuppliedText() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.TEXT, "hello world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextzSuppliedTexts() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.TEXT, "hello world"), value(Type.TEXT, "goodbye see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "text()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableTextzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("hello world"), value("goodbye see you soon") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected text(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "text()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value("hello", "world");
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as attribute()
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "world") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributeNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableAttributeSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributeSuppliedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "revt:hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()", externalVariable);
    }


    @Test
    void queryPostWithExternalVariableAttributeSuppliedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world"), value("goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributeSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("revt:hello", "world");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected attribute(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "attribute()", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world"), value("goodbye", "see you soon") };
        // NOTE(AR) we expect this to return xs:string because neither the input nor the variable is actually typed as attribute()
        final ExternalVariableValueRep[] expectedResult = new ExternalVariableValueRep[] { value(Type.STRING, "world"), value(Type.STRING, "see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, expectedResult, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "revt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptAttributeNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()?", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableOptAttributeSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptAttributeSuppliedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "revt:hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptAttributeSuppliedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "revt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptAttributeSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = value("revt:hello", "world");
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected attribute(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "attribute()?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributesNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()+", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableAttributesSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributesSuppliedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "revt:hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributesSuppliedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "revt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributesSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected attribute(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "attribute()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributesSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world"), value("goodbye", "see you soon") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected attribute(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "attribute()+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributezNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "attribute()*", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableAttributezSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributezSuppliedAttribute() throws IOException {
        final ExternalVariableValueRep externalVariable = value(Type.ATTRIBUTE, "revt:hello", "world");
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributezSuppliedAttributes() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value(Type.ATTRIBUTE, "revt:hello", "world"), value(Type.ATTRIBUTE, "goodbye", "see you soon") };
        queryPostWithExternalVariable(HttpStatus.OK_200, "attribute()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributeszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected attribute(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "attribute()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableAttributezSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ExternalVariableValueRep[] { value("revt:hello", "world"), value("goodbye", "see you soon") };
        final String expectedResponseError = "<exception><path>/db/test/test.xml</path><message>err:XPTY0004 Invalid type for variable $local:my-variable. Expected attribute(), got xs:string</message></exception>";
        queryPostWithExternalVariable(Tuple(HttpStatus.BAD_REQUEST_400, expectedResponseError), "attribute()*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArrayNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableArraySuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArraySuppliedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArraySuppliedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArraySuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { untypedArray(sequence(value(Type.STRING, "hello"))), untypedArray(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptArrayNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)?", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableOptArraySuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptArraySuppliedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptArraySuppliedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptArraySuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArraysNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)+", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableArraysSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArraysSuppliedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArraysSuppliedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArraysSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArraysSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { untypedArray(sequence(value(Type.STRING, "hello"))), untypedArray(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArrayzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "array(*)*", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableArrayzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new ArrayRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArrayzSuppliedArray() throws IOException {
        final ExternalVariableValueRep externalVariable = array(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArrayzSuppliedArrays() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { array(sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))), array(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArrayszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedArray(sequence(value(Type.STRING, "hello")));
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableArrayzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = { untypedArray(sequence(value(Type.STRING, "hello"))), untypedArray(sequence(value(Type.STRING, "goodbye"))) };
        queryPostWithExternalVariable(HttpStatus.OK_200, "array(*)*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedMap() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedMap(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedMap() throws IOException {
        final ExternalVariableValueRep externalVariable = map(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "map(*)",  (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableMapSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new MapRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "map(*)", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapSuppliedMap() throws IOException {
        final ExternalVariableValueRep externalVariable = map(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapSuppliedMaps() throws IOException {
        final ExternalVariableValueRep[] externalVariable = {
            map(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            map(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, "42")))
            )
        };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "map(*)", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedMap(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedUntypedMaps() throws IOException {
        final ExternalVariableValueRep[] externalVariable = {
            untypedMap(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            untypedMap(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, "42")))
            )
        };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableUntypedSuppliedMaps() throws IOException {
        final ExternalVariableValueRep[] externalVariable = {
            map(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            map(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, "42")))
            )
        };
        queryPostWithExternalVariable(HttpStatus.OK_200, null, externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptMapNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "map(*)?", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableOptMapSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new MapRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptMapSuppliedMap() throws IOException {
        final ExternalVariableValueRep externalVariable = map(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptMapSuppliedMaps() throws IOException {
        final ExternalVariableValueRep[] externalVariable = {
            map(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            map(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, "42")))
            )
        };
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "map(*)?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableOptMapSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedMap(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)?", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapsNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "map(*)+", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableMapsSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new MapRep[0];
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "map(*)+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapsSuppliedMap() throws IOException {
        final ExternalVariableValueRep externalVariable = map(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapsSuppliedMaps() throws IOException {
        final ExternalVariableValueRep[] externalVariable = {
            map(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            map(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, "42")))
            )
        };

        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapsSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedMap(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapsSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = {
            untypedMap(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            untypedMap(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, "42")))
            )
        };
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)+", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapzNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, "map(*)*", (ExternalVariableValueRep[]) null);
    }

    @Test
    void queryPostWithExternalVariableMapzSuppliedEmpty() throws IOException {
        final ExternalVariableValueRep[] externalVariable = new MapRep[0];
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapzSuppliedMap() throws IOException {
        final ExternalVariableValueRep externalVariable = map(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapzSuppliedMaps() throws IOException {
        final ExternalVariableValueRep[] externalVariable = {
            map(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            map(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, "42")))
            )
        };
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapszSuppliedUntyped() throws IOException {
        final ExternalVariableValueRep externalVariable = untypedMap(entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"))));
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)*", externalVariable);
    }

    @Test
    void queryPostWithExternalVariableMapzSuppliedUntypeds() throws IOException {
        final ExternalVariableValueRep[] externalVariable = {
            untypedMap(
                entry(key(Type.STRING, "key1"), sequence(value(Type.STRING, "hello"), value(Type.INTEGER, "42"))),
                entry(key(Type.STRING, "key2"), sequence(value(Type.STRING, "goodbye")))
            ),
            untypedMap(
                entry(key(Type.STRING, "key3"), sequence(value(Type.STRING, "in the beginning"), value(Type.STRING, "but at the end"), value(Type.INTEGER, "42")))
            )
        };
        queryPostWithExternalVariable(HttpStatus.OK_200, "map(*)*", externalVariable);
    }

    @Test
    public void queryPostWrappedTypedWithExternalVariableStringConstructElement() throws IOException {
        queryPostWithExternalVariableStringConstructElement(true, true);
    }

    @Test
    public void queryPostWrappedNotTypedWithExternalVariableStringConstructElement() throws IOException {
        queryPostWithExternalVariableStringConstructElement(true, false);
    }

    @Test
    public void queryPostNotWrappedTypedWithExternalVariableStringConstructElement() throws IOException {
        queryPostWithExternalVariableStringConstructElement(false, true);
    }

    @Test
    public void queryPostNotWrappedNotTypedWithExternalVariableStringConstructElement() throws IOException {
        queryPostWithExternalVariableStringConstructElement(false, false);
    }

    private void queryPostWithExternalVariableStringConstructElement(final boolean wrap, final boolean typed) throws IOException {
        final String query;
        if (useXmlnsPrefixes) {
            query = "<exist:query xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" xmlns:sx=\"http://exist-db.org/xquery/types/serialized\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" wrap=\"" + (wrap ? "yes" : "no") + "\" typed=\"" + (typed ? "yes" : "no") + "\">\n" +
                "\t<exist:variables>\n" +
                "\t\t<exist:variable>\n" +
                "\t\t\t<exist:qname>\n" +
                "\t\t\t\t<exist:localname>my-variable</exist:localname>\n" +
                "\t\t\t</exist:qname>\n" +
                "\t\t\t<sx:sequence>\n" +
                "\t\t\t\t<sx:value type=\"xs:string\">greeting</sx:value>" +
                "\t\t\t</sx:sequence>\n" +
                "\t\t</exist:variable>\n" +
                "\t</exist:variables>\n" +
                "\t<exist:text><![CDATA[\n" +
                "declare variable $my-variable external;\n" +
                "element { $my-variable } { text { \"Hello, world.\" } }\n" +
                "\t]]></exist:text>\n" +
                "</exist:query>\n";
        } else {
            query = "<query xmlns=\"http://exist.sourceforge.net/NS/exist\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" wrap=\"" + (wrap ? "yes" : "no") + "\" typed=\"" + (typed ? "yes" : "no") + "\">\n" +
                "\t<variables>\n" +
                "\t\t<variable>\n" +
                "\t\t\t<qname>\n" +
                "\t\t\t\t<localname>my-variable</localname>\n" +
                "\t\t\t</qname>\n" +
                "\t\t\t<sequence xmlns=\"http://exist-db.org/xquery/types/serialized\">\n" +
                "\t\t\t\t<value type=\"xs:string\">greeting</value>" +
                "\t\t\t</sequence>\n" +
                "\t\t</variable>\n" +
                "\t</variables>\n" +
                "\t<text><![CDATA[\n" +
                "declare variable $my-variable external;\n" +
                "element { $my-variable } { text { \"Hello, world.\" } }\n" +
                "\t]]></text>\n" +
                "</query>\n";
        }

        final HttpResponse response = doPostWithAuth(getResourceUri(), query);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();

        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        //final String actual = "<xs:y xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><greeting>Hello, world.</greeting></xs:y>";
        final String actual = readResponse(response.getEntity());
        assertThat(actual, hasXPath("//greeting[namespace-uri() = ''][text() = 'Hello, world.']").withNamespaceContext(NS_CONTEXT));
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

        assertEquals((int) expectedResponse._1, resultStatusCode, "Server returned response code: " + resultStatusCode);

        String actual = readResponse(response.getEntity());
        actual = actual.replaceFirst("\\s*\\[source:[^\\]]*\\]</message></exception>$", "</message></exception>");  // NOTE(AR) remove any source information from the actual response

        @Nullable final String expected;
        if (expectedResponse._1 == HttpStatus.OK_200) {
            expected = buildExistVariableResultSequence(expectedResult);
        } else {
            expected = expectedResponse._2;
        }

        if (expected != null) {
            assertThat(actual, CompareMatcher.isSimilarTo(expected).withNamespaceContext(NS_CONTEXT).withAttributeFilter(IGNORE_EXIST_TIMING_ATTRIBUTES).withNodeMatcher(IGNORE_MAP_ENTRY_ORDER_MATCHER).ignoreWhitespace());
        }
    }

    private static String buildExistVariableResultSequence(final ExternalVariableValueRep... resultSequence) {
        final StringBuilder builder = new StringBuilder();
        builder.append('<').append(Namespaces.EXIST_NS_PREFIX).append(":result xmlns:").append(Namespaces.EXIST_NS_PREFIX).append("=\"").append(Namespaces.EXIST_NS).append("\" xmlns:").append(Marshaller.PREFIX).append("=\"").append(Marshaller.NAMESPACE).append("\" ").append(Namespaces.EXIST_NS_PREFIX).append(":count=\"").append(resultSequence.length).append("\" ").append(Namespaces.EXIST_NS_PREFIX).append(":hits=\"").append(resultSequence.length).append("\" ").append(Namespaces.EXIST_NS_PREFIX).append(":start=\"1\">\n");
        for (final ExternalVariableValueRep resultSequenceItem : resultSequence) {
            buildExistVariableResultSequenceItem(builder, resultSequenceItem);
        }
        builder.append("</").append(Namespaces.EXIST_NS_PREFIX).append(":result>");
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

        if (!Type.subTypeOf(xdmType, Type.NODE) && !isAttribute(xdmType, resultSequenceItem) && !isArray(xdmType, resultSequenceItem) && !isMap(xdmType, resultSequenceItem)) {
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

        } else if (isMap(xdmType, resultSequenceItem)) {
            builder.append("<exist:map>");
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

        } else if (resultSequenceItem instanceof MapRep) {
            // Map type
            for (final EntryRep mapEntry : ((MapRep) resultSequenceItem).getEntries()) {
                builder.append("<exist:entry>\n");
                builder.append("<exist:key>");
                buildExistVariableResultSequenceItem(builder, mapEntry.key.key);
                builder.append("</exist:key>\n");
                builder.append("<exist:sequence>\n");
                for (final ValueRep arrayEntryValue : mapEntry.value.values) {
                    buildExistVariableResultSequenceItem(builder, arrayEntryValue);
                }
                builder.append("</exist:sequence>\n");
                builder.append("</exist:entry>\n");
            }

        } else {
            builder.append(((ValueRep) resultSequenceItem).getContent());
        }

        if (!Type.subTypeOf(xdmType, Type.NODE) && !isAttribute(xdmType, resultSequenceItem) && !isArray(xdmType, resultSequenceItem) && !isMap(xdmType, resultSequenceItem)) {
            builder.append("</exist:value>\n");

        } else if (xdmType == Type.DOCUMENT) {
            builder.append("</exist:document>");

        } else if (xdmType == Type.TEXT) {
            builder.append("</exist:text>");

        } else if (xdmType == Type.ATTRIBUTE) {
            builder.append("</exist:attribute>");

        } else if (isArray(xdmType, resultSequenceItem)) {
            builder.append("</exist:array>");

        } else if (isMap(xdmType, resultSequenceItem)) {
            builder.append("</exist:map>");
        }
    }

    private static boolean isAttribute(final int xdmType, final ExternalVariableValueRep externalVariableValueRep) {
        return xdmType == Type.ATTRIBUTE || (xdmType == Type.ITEM && externalVariableValueRep instanceof NamedValueRep);
    }

    private static boolean isArray(final int xdmType, final ExternalVariableValueRep externalVariableValueRep) {
        return xdmType == Type.ARRAY_ITEM || (xdmType == Type.ITEM && externalVariableValueRep instanceof ArrayRep);
    }

    private static boolean isMap(final int xdmType, final ExternalVariableValueRep externalVariableValueRep) {
        return xdmType == Type.MAP_ITEM || (xdmType == Type.ITEM && externalVariableValueRep instanceof MapRep);
    }

    private String buildQueryExternalVariable(@Nullable final String xqExternalVariableType, @Nullable final ExternalVariableValueRep... externalVariableSequence) {
        final StringBuilder builder = new StringBuilder();

        if (useXmlnsPrefixes) {
            builder.append("<exist:query xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" xmlns:sx=\"http://exist-db.org/xquery/types/serialized\" xmlns:" + TEST_PREFIX + "=\"" + TEST_NAMESPACE + "\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" wrap=\"yes\" typed=\"yes\">\n");

            if (externalVariableSequence != null) {
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

        } else {
            builder.append("<query xmlns=\"http://exist.sourceforge.net/NS/exist\" xmlns:" + TEST_PREFIX + "=\"" + TEST_NAMESPACE + "\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" wrap=\"yes\" typed=\"yes\">\n");

            if (externalVariableSequence != null) {
                builder.append("\t<variables>\n");
                builder.append("\t\t<variable>\n");
                builder.append("\t\t\t<qname><prefix>local</prefix><localname>my-variable</localname></qname>\n");
                buildQueryExternalVariableSequence(builder, 3, externalVariableSequence);
                builder.append("\t\t</variable>\n");
                builder.append("\t</variables>\n");
            }

            builder.append("\t<text><![CDATA[\n");
            builder.append("declare variable $local:my-variable");
            if (xqExternalVariableType != null) {
                builder.append(" as ").append(xqExternalVariableType);
            }
            builder.append(" external;\n");
            builder.append("$local:my-variable\n");
            builder.append("\t]]></text>\n");
            builder.append("</query>\n");
        }

        return builder.toString();
    }

    private static final char[] INDENTS = { '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t', '\t' };

    private void buildQueryExternalVariableSequence(final StringBuilder builder, final int indentCount, final ExternalVariableValueRep... externalVariableSequence) {
        if (useXmlnsPrefixes) {
            builder.append(INDENTS, 0, indentCount).append("<sx:sequence>\n");
        } else {
            builder.append(INDENTS, 0, indentCount).append("<sequence xmlns=\"http://exist-db.org/xquery/types/serialized\">\n");
        }

        for (final ExternalVariableValueRep externalVariableSequenceItem : externalVariableSequence) {
            if (useXmlnsPrefixes) {
                builder.append(INDENTS, 0, indentCount + 1).append("<sx:value");
            } else {
                builder.append(INDENTS, 0, indentCount + 1).append("<value");
            }

            if (externalVariableSequenceItem instanceof ExternalVariableTypedValueRep) {
                builder.append(" type=\"").append(Type.getTypeName(((ExternalVariableTypedValueRep) externalVariableSequenceItem).getXdmType())).append("\"");
            }

            if (externalVariableSequenceItem instanceof TypedNamedValueRep) {
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
                builder.append(INDENTS, 0, indentCount + 1);

            } else if (externalVariableSequenceItem instanceof MapRep) {
                // Map type
                builder.append('\n');
                for (final EntryRep entryRep : ((MapRep) externalVariableSequenceItem).getEntries()) {
                    if (useXmlnsPrefixes) {
                        builder.append(INDENTS, 0, indentCount + 2).append("<sx:entry>\n");
                        builder.append(INDENTS, 0, indentCount + 3).append("<sx:key");
                    } else {
                        builder.append(INDENTS, 0, indentCount + 2).append("<entry>\n");
                        builder.append(INDENTS, 0, indentCount + 3).append("<key");
                    }

                    final ValueRep key = entryRep.key.key;
                    if (key instanceof ExternalVariableTypedValueRep) {
                        builder.append(" type=\"").append(Type.getTypeName(((ExternalVariableTypedValueRep) key).getXdmType())).append("\"");
                    }
                    if (useXmlnsPrefixes) {
                        builder.append('>').append(key.getContent()).append("</sx:key>\n");
                    } else {
                        builder.append('>').append(key.getContent()).append("</key>\n");
                    }

                    buildQueryExternalVariableSequence(builder, indentCount + 3, entryRep.value.values);

                    if (useXmlnsPrefixes) {
                        builder.append(INDENTS, 0, indentCount + 2).append("</sx:entry>\n");
                    } else {
                        builder.append(INDENTS, 0, indentCount + 2).append("</entry>\n");
                    }
                }
                builder.append(INDENTS, 0, indentCount + 1);

            } else {
                builder.append(((ValueRep) externalVariableSequenceItem).getContent());
            }

            if (useXmlnsPrefixes) {
                builder.append("</sx:value>\n");
            } else {
                builder.append("</value>\n");
            }
        }

        if (useXmlnsPrefixes) {
            builder.append(INDENTS, 0, indentCount).append("</sx:sequence>\n");
        } else {
            builder.append(INDENTS, 0, indentCount).append("</sequence>\n");
        }
    }

    private static String getServerUri() {
        return "http://localhost:" + DATABASE_WEB_SERVER.getPort() + "/rest";
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
}
