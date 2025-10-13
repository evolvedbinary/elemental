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
package org.exist.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.eclipse.jetty.http.HttpStatus;
import org.exist.EXistException;
import org.exist.Namespaces;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.memtree.SAXAdapter;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.test.ExistWebServer;
import org.exist.util.ExistSAXParserFactory;
import org.exist.util.LockException;
import org.exist.util.MapUtil;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Type;
import org.w3c.dom.Attr;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Source;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.matchers.CompareMatcher;
import org.xmlunit.util.Predicate;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

/**
 * A test case for accessing a remote server via REST-Style Web API.
 * @author <a href="mailto:pierrick.brihaye@free.fr">wolf
 * @author Pierrick Brihaye</a>
 */
//@RunWith(ParallelRunner.class)    // TODO(AR) when running in parallel a deadlock is encountered... this needs to be resolved!
public class RESTServiceTest {

    @ClassRule
    public static final ExistWebServer existWebServer = new ExistWebServer(true, false, true, true);

    private static final String XML_DATA = "<test>"
            + "<para>\u00E4\u00E4\u00FC\u00FC\u00F6\u00F6\u00C4\u00C4\u00D6\u00D6\u00DC\u00DC</para>"
            + "</test>";

    private static final String XUPDATE = "<xu:modifications xmlns:xu=\"http://www.xmldb.org/xupdate\" version=\"1.0\">"
            + "<xu:append select=\"/test\" child=\"1\">"
            + "<para>Inserted paragraph.</para>"
            + "</xu:append>" + "</xu:modifications>";

    private static final String QUERY_REQUEST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<query xmlns=\"" + Namespaces.EXIST_NS + "\">"
            + "<properties>"
            + "<property name=\"indent\" value=\"yes\"/>"
            + "<property name=\"encoding\" value=\"UTF-8\"/>"
            + "</properties>"
            + "<text>"
            + "xquery version \"1.0\";"
            + "(::pragma exist:serialize indent=no ::)"
            + "//para"
            + "</text>" + "</query>";

    private static final String QUERY_REQUEST_ERROR = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<query xmlns=\"" + Namespaces.EXIST_NS + "\">"
            + "<properties>"
            + "<property name=\"indent\" value=\"yes\"/>"
            + "<property name=\"encoding\" value=\"UTF-8\"/>"
            + "</properties>"
            + "<text>"
            + "xquery version \"1.0\";"
            + "//undeclared:para"
            + "</text>" + "</query>";

    private static final String TEST_MODULE =
            "module namespace t=\"http://test.foo\";\n" +
                    "declare variable $t:VAR { 'World!' };";

    private static final String TEST_XQUERY =
            "xquery version \"1.0\";\n" +
                    "declare option exist:serialize \"method=text media-type=text/text\";\n" +
                    "import module namespace request=\"http://exist-db.org/xquery/request\";\n" +
                    "import module namespace t=\"http://test.foo\" at \"module.xq\";\n" +
                    "let $param := request:get-parameter('p', ())\n" +
                    "return\n" +
                    "	($param, ' ', $t:VAR)";

    private static final String TEST_XQUERY_PARAMETER =
            "xquery version \"1.0\";\n" +
                    "declare namespace request=\"http://exist-db.org/xquery/request\";\n" +
                    "import module namespace requestparametermod=\"http://exist-db.org/xquery/requestparametermod\" at \"requestparametermod.xqm\";\n" +
                    "concat(\"xql=\", request:get-parameter(\"doc\",())),\n" +
                    "concat(\"xqm=\", $requestparametermod:request)";

    private static final String TEST_XQUERY_PARAMETER_MODULE =
            "module namespace requestparametermod = \"http://exist-db.org/xquery/requestparametermod\";\n" +
                    "declare namespace request=\"http://exist-db.org/xquery/request\";\n" +
                    "declare variable $requestparametermod:request { request:get-parameter(\"doc\",())};\n";

    private static final String TEST_XQUERY_WITH_PATH_PARAMETER =
            "xquery version \"1.0\";\n" +
                    "declare namespace request=\"http://exist-db.org/xquery/request\";\n" +
                    "declare option exist:serialize \"method=text media-type=text/text\";\n" +
                    "(\"pathInfo=\", request:get-path-info(), \"\n\"," +
                    "\"servletPath=\", request:get-servlet-path(), \"\n\")";

    private static final String TEST_XQUERY_WITH_PATH_AND_CONTENT =
            "xquery version \"3.0\";\n" +
                    "declare namespace request=\"http://exist-db.org/xquery/request\";\n" +
                    "declare option exist:serialize \"method=text media-type=text/text\";\n" +
                    "request:get-data()//data/text() || ' ' || request:get-path-info()";

    private static final String AUTH_QUERY =
            "import module namespace request=\"http://exist-db.org/xquery/request\";\n" +
                    "import module namespace sm=\"http://exist-db.org/xquery/securitymanager\";\n" +
                    "<authorization>\n" +
                    "    {sm:id()}\n" +
                    "    <header>{request:get-header('Authorization')}</header>\n" +
                    "</authorization>\n";

    private static final String XML_WITH_DOCTYPE =
            "<!DOCTYPE bookmap PUBLIC \"-//OASIS//DTD DITA BookMap//EN\" \"bookmap.dtd\">\n" +
            "<bookmap id=\"bookmap-1\"/>";


    private static final XmldbURI TEST_DOCTYPE_COLLECTION_URI = XmldbURI.ROOT_COLLECTION_URI.append("rest-test-doctype");
    private static final XmldbURI TEST_XML_DOC_WITH_DOCTYPE_URI = XmldbURI.create("test-with-doctype.xml");

    private static final XmldbURI TEST_XSLPI_COLLECTION_URI = XmldbURI.ROOT_COLLECTION_URI.append("rest-test-xslpi");

    private static final String XSLT_WITH_XSLPI =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" exclude-result-prefixes=\"xs\" version=\"2.0\">\n" +
            "  <xsl:output method=\"xml\" indent=\"no\" media-type=\"application/xml\" omit-xml-declaration=\"yes\"/>\n" +
            "  <xsl:template match=\"processing-instruction()\" priority=\"2\"/>\n" +
            "  <xsl:template match=\"bookmap\">\n" +
            "    <copied>\n" +
            "      <xsl:copy>\n" +
            "        <xsl:apply-templates select=\"node()|@*\"/>\n" +
            "      </xsl:copy>\n" +
            "    </copied>\n" +
            "  </xsl:template>\n" +
            "  <xsl:template match=\"node()|@*\">\n" +
            "    <xsl:copy>\n" +
            "      <xsl:apply-templates select=\"node()|@*\"/>\n" +
            "    </xsl:copy>\n" +
            "  </xsl:template>\n" +
            "</xsl:stylesheet>";

    private static final XmldbURI TEST_XSLT_DOC_WITH_XSLPI_URI = XmldbURI.create("test-with-xslpi.xslt");

    private static final String XML_WITH_XSLPI =
            "<?xml-stylesheet type=\"text/xsl\" href=\"" + TEST_XSLT_DOC_WITH_XSLPI_URI.lastSegmentString() + "\"?>\n" +
            "<bookmap id=\"bookmap-1\"/>";

    private static final XmldbURI TEST_XML_DOC_WITH_XSLPI_URI = XmldbURI.create("test-with-xslpi.xml");


    private static String credentials;
    private static String badCredentials;

    private static final Map<String, String> NS_CONTEXT = MapUtil.HashMap(
        Tuple("xs", "http://www.w3.org/2001/XMLSchema"),
        Tuple("exist", "http://exist.sourceforge.net/NS/exist"),
        Tuple("sx", "http://exist-db.org/xquery/types/serialized")
    );

    private final Predicate<Attr> ignoreExistTimingAttributes = attr -> !("http://exist.sourceforge.net/NS/exist".equals(attr.getNamespaceURI()) && ("compilation-time".equals(attr.getLocalName()) || "execution-time".equals(attr.getLocalName())));

    private static String getServerUri() {
        return "http://localhost:" + existWebServer.getPort() + "/rest";
    }

    private static String getServerUriRedirected() {
        return "http://localhost:" + existWebServer.getPort();
    }

    private static String getCollectionUri() {
        return getServerUri() + XmldbURI.ROOT_COLLECTION + "/test";
    }

    private static String getCollectionUriRedirected() {
        return getServerUriRedirected() + XmldbURI.ROOT_COLLECTION + "/test";
    }

    private static String getResourceUri() {
        return getServerUri() + XmldbURI.ROOT_COLLECTION + "/test/test.xml";
    }

    private static String getResourceWithDocTypeUri() {
        return getServerUri() + TEST_DOCTYPE_COLLECTION_URI.append(TEST_XML_DOC_WITH_DOCTYPE_URI);
    }

    /* About path components of URIs:

     ** reserved characters # http://tools.ietf.org/html/rfc3986#section-2.2
     *
     *  gen-delims  = ":" / "/" / "?" / "#" / "[" / "]" / "@"
     *  sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
     *              / "*" / "+" / "," / ";" / "="
     *  reserved    = gen-delims / sub-delims
        RCHAR=": / ? # [ ] @ ! $ & ' ( ) *  + , ; ="

     ** path-segment # http://tools.ietf.org/html/rfc3986#section-3.3
     *
     *  unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
     *  pct-encoded = "%" HEXDIG HEXDIG
     *  sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
     *              / "*" / "+" / "," / ";" / "="
     *  pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"

     ** So, characters literally allowed in a path-segment are:
        PCHAR="A-Z a-z 0-9 - . _ ~ ! $ & ' ( ) *  + , ; = : @"

     ** All the rest has to be percent-encoded
     *  the percent sign itself MUST start a code
     *  reserved+ chars in need of encoding - in a path-segment - are:
     *       /   ?   #   [   ]   %
     *  %20 %2F %3F %23 %5B %5D %25

     ** Interoperability /rest/ space:
     *  most webbrowsers act mostly correct
     *  curl does _no_ encoding on its own
     *  all browsers send a bare / as is (user error? will separate path-segments)
     *  all browsers send a bare ? as is (user error? will start the query-string)
     *  no browser sends a bare # at all (user error? will start the fragment-identifier)
     *  chrome and msie send [] verbatim (wrong? apache can accomodate…)
     *  all browsers send a bare % as is (user error? will start an escape, apache returns Bad Request)

     ** Interoperability /webdav/ space:
     *  the GET and PUT methods mirror /rest/ space
     *  These characters are not allowed in an NFTS filename
        INTFS='/ \ : *   ? " < > |'
     *  of those, Macintosh HFS only prohibits the colon
     *  most other UN*X FSs only prohibit the slash
     *  Quick test with bash on Linux extfs:
        TWDAV="$PCHAR $RCHAR $INTFS %"
     *  set -f; for fn in $TWDAV; do echo T__${fn}__ > /tmp/T__${fn}__; done
     *  only the slash will error out (twice)
     *  anything in this set can be thrown at webdav!

     ** Beware, some chars valid in a path-segment must not be in a filename (mostly NTFS)
     */
    // Below String mostly contains the PCHAR set literally; the colon fails though, so its omitted…
    // Also in the mix: some (mandatory except %27) escapes, some multibyte UTF-8 characters
    // and a superficial directory traversal and a superficial double slash too
    private static String getResourceUriPlus() {
        return getServerUri() + XmldbURI.ROOT_COLLECTION + "/test//../test/A-Za-z0-9_~!$&'()*+,;=@%20%23%25%27%2F%3F%5B%5Däöü.xml";
    }

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @BeforeClass
    public static void setup() throws PermissionDeniedException, IOException, TriggerException {
        credentials = Base64.encodeBase64String("admin:".getBytes(UTF_8));
        badCredentials = Base64.encodeBase64String("johndoe:this pw should fail".getBytes(UTF_8));

        final XmldbURI TEST_XML_DOC_URI = XmldbURI.create("AéB.xml");
        final XmldbURI TEST_COLLECTION_URI = XmldbURI.create("/db/AéB");
        final String TEST_XML_DOC = "<foo/>";

        final BrokerPool pool =  existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            try (final Collection col = broker.getOrCreateCollection(transaction, TEST_COLLECTION_URI)) {
                broker.storeDocument(transaction, TEST_XML_DOC_URI, new StringInputSource(TEST_XML_DOC), MimeType.XML_TYPE, col);
                broker.saveCollection(transaction, col);
            }

            try (final Collection col = broker.getOrCreateCollection(transaction, TEST_DOCTYPE_COLLECTION_URI)) {
                broker.storeDocument(transaction, TEST_XML_DOC_WITH_DOCTYPE_URI, new StringInputSource(XML_WITH_DOCTYPE), MimeType.XML_TYPE, col);
                broker.saveCollection(transaction, col);
            }

            try (final Collection col = broker.getOrCreateCollection(transaction, TEST_XSLPI_COLLECTION_URI)) {
                broker.storeDocument(transaction, TEST_XSLT_DOC_WITH_XSLPI_URI, new StringInputSource(XSLT_WITH_XSLPI), MimeType.XML_TYPE, col);
                broker.storeDocument(transaction, TEST_XML_DOC_WITH_XSLPI_URI, new StringInputSource(XML_WITH_XSLPI), MimeType.XML_TYPE, col);
                broker.saveCollection(transaction, col);
            }

            transaction.commit();
        } catch (EXistException | SAXException | LockException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getFailNoSuchDocument() throws IOException {
        final String uri = getCollectionUri() + "/nosuchdocument.xml";
        final HttpResponse response = doGet(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.NOT_FOUND_404, resultStatusCode);
    }

    @Test
    public void xqueryGetWithEmptyPath() throws IOException {
        /* store the documents that we need for this test */
        doPut(TEST_XQUERY_WITH_PATH_PARAMETER, "requestwithpath.xq", HttpStatus.CREATED_201);

        final String uri = getCollectionUri() + "/requestwithpath.xq";

        final HttpResponse response = doGetWithAuth(uri);

        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();

        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        final String responseBody = readResponse(response.getEntity());
        final String pathInfo = responseBody.substring("pathInfo=".length(), responseBody.indexOf("servletPath=") - 2);
        final String servletPath = responseBody.substring(responseBody.indexOf("servletPath=") + "servletPath=".length(), responseBody.lastIndexOf("\r\n"));

        //check the responses
        assertEquals("XQuery servletPath is: \"" + servletPath + "\" expected: \"/db/test/requestwithpath.xq\"", "/db/test/requestwithpath.xq", servletPath);
        assertEquals("XQuery pathInfo is: \"" + pathInfo + "\" expected: \"\"", "", pathInfo);
    }

    @Test
    public void xqueryPOSTWithEmptyPath() throws IOException {
        /* store the documents that we need for this test */
        doPut(TEST_XQUERY_WITH_PATH_PARAMETER, "requestwithpath.xq", HttpStatus.CREATED_201);

        final String uri = getCollectionUri() + "/requestwithpath.xq";
        final HttpResponse response = doPostWithAuth(uri, "boo");
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        final String responseBody = readResponse(response.getEntity());
        final String pathInfo = responseBody.substring("pathInfo=".length(), responseBody.indexOf("servletPath=")-2);
        final String servletPath = responseBody.substring(responseBody.indexOf("servletPath=") + "servletPath=".length(), responseBody.lastIndexOf("\r\n"));

        //check the responses
        assertEquals("XQuery servletPath is: \"" + servletPath + "\" expected: \"/db/test/requestwithpath.xq\"", "/db/test/requestwithpath.xq", servletPath);
        assertEquals("XQuery pathInfo is: \"" + pathInfo + "\" expected: \"\"", "", pathInfo);
    }

    @Test
    public void xqueryGetWithNonEmptyPath() throws IOException {
        /* store the documents that we need for this test */
        doPut(TEST_XQUERY_WITH_PATH_PARAMETER, "requestwithpath.xq", HttpStatus.CREATED_201);

        final String uri = getCollectionUri() + "/requestwithpath.xq/some/path";
        final HttpResponse response = doGetWithAuth(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        final String responseBody = readResponse(response.getEntity());
        final String pathInfo = responseBody.substring("pathInfo=".length(), responseBody.indexOf("servletPath=") - 2);
        final String servletPath = responseBody.substring(responseBody.indexOf("servletPath=") + "servletPath=".length(), responseBody.lastIndexOf("\r\n"));

        //check the responses
        assertEquals("XQuery servletPath is: \"" + servletPath + "\" expected: \"/db/test/requestwithpath.xq\"", "/db/test/requestwithpath.xq", servletPath);
        assertEquals("XQuery pathInfo is: \"" + pathInfo + "\" expected: \"/some/path\"", "/some/path", pathInfo);
    }

    @Test
    public void xqueryPOSTWithNonEmptyPath() throws IOException {
        /* store the documents that we need for this test */
        doPut(TEST_XQUERY_WITH_PATH_PARAMETER, "requestwithpath.xq", HttpStatus.CREATED_201);

        final String uri = getCollectionUri() + "/requestwithpath.xq/some/path";
        final HttpResponse response = doPostWithAuth(uri, "boo");
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        final String responseBody = readResponse(response.getEntity());
        final String pathInfo = responseBody.substring("pathInfo=".length(), responseBody.indexOf("servletPath=") - 2);
        final String servletPath = responseBody.substring(responseBody.indexOf("servletPath=") + "servletPath=".length(), responseBody.lastIndexOf("\r\n"));

        //check the responses
        assertEquals("XQuery servletPath is: \"" + servletPath + "\" expected: \"/db/test/requestwithpath.xq\"", "/db/test/requestwithpath.xq", servletPath);
        assertEquals("XQuery pathInfo is: \"" + pathInfo + "\" expected: \"/some/path\"", "/some/path", pathInfo);
    }


    @Test
    public void xqueryGetFailWithNonEmptyPath() throws IOException {
        /* store the documents that we need for this test */
        HttpResponse response = doPutWithAuth(getResourceUri(), "application/xml", XML_DATA);

        final String uri = getResourceUri() + "/some/path";    // should not be able to get this path
        response = doGet(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.NOT_FOUND_404, resultStatusCode);
    }

    @Test
    public void testPut() throws IOException {
        final int r = uploadData();
        assertEquals("Server returned response code: " + r, HttpStatus.CREATED_201, r);

        doGet();
    }

    @Test
    public void testPutPlus() throws IOException {
        assumeThat("Requires non-Windows platform", System.getProperty("os.name").toLowerCase(), not(containsString("win")));

        HttpResponse response = doPutWithAuth(getResourceUriPlus(), ContentType.APPLICATION_XML.getMimeType(), XML_DATA);
        int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.CREATED_201, resultStatusCode);

        response = doGet(getResourceUriPlus());
        resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        assertResponseMediaType("application/xml", response);
        assertNotNull(readResponse(response.getEntity()));
    }

    @Test
    public void putFailAgainstCollection() throws IOException {
        final HttpResponse response = doPutWithAuth(getCollectionUri(), "application/xml", XML_DATA);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.BAD_REQUEST_400, resultStatusCode);
    }

    @Test
    public void putWithCharset() throws IOException {
        final HttpResponse response = doPutWithAuth(getResourceUri(), "application/xml; charset=UTF-8", XML_DATA);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.CREATED_201, resultStatusCode);
    }

    @Test
    public void putFailAndRechallengeAuthorization() throws IOException {
        final HttpResponse response = Request.Put(getResourceUri())
            .setHeader("Authorization", "Basic " + badCredentials)
            .execute()
            .returnResponse();
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.UNAUTHORIZED_401, resultStatusCode);

        final String auth = response.getFirstHeader("WWW-Authenticate").getValue();
        assertEquals("WWW-Authenticate = " + auth, "Basic realm=\"exist\"", auth);
    }

    @Test
    public void putAgainstXQuery() throws IOException {
        doPut(TEST_XQUERY_WITH_PATH_AND_CONTENT, "requestwithcontent.xq", HttpStatus.CREATED_201);

        final String uri = getCollectionUriRedirected() + "/requestwithcontent.xq/a/b/c";
        final HttpResponse response = doPutWithAuth(uri, "application/xml", "<data>test data</data>");
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        //get the response of the query
        final String responseBody = readResponse(response.getEntity());
        assertEquals("test data /a/b/c", responseBody.trim());
    }

    @Test
    public void deleteAgainstXQuery() throws IOException {
        doPut(TEST_XQUERY_WITH_PATH_PARAMETER, "requestwithcontent.xq", HttpStatus.CREATED_201);

        final String uri = getCollectionUriRedirected() + "/requestwithcontent.xq/a/b/c";
        final HttpResponse response = doDeleteWithAuth(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        //get the response of the query
        final String responseBody = readResponse(response.getEntity());
        final String pathInfo = responseBody.substring("pathInfo=".length(), responseBody.indexOf("servletPath=")-2);
        assertEquals("/a/b/c", pathInfo);
    }

    @Test
    public void headAgainstXQuery() throws IOException {
        doPut(TEST_XQUERY_WITH_PATH_PARAMETER, "requestwithcontent.xq", HttpStatus.CREATED_201);

        final String uri = getCollectionUriRedirected() + "/requestwithcontent.xq/a/b/c";
        final HttpResponse response = doHeadWithAuth(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
    }

    @Test
    public void xUpdate() throws IOException {
        final HttpResponse response = doPostWithAuth(getResourceUri(), XUPDATE);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();

        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        doGet();
    }

    @Test
    public void queryPost() throws IOException, SAXException, ParserConfigurationException {
        uploadData();

        final HttpResponse response = doPostWithAuth(getResourceUri(), QUERY_REQUEST);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();

        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        final String data = readResponse(response.getEntity());
        final int hits = parseResponse(data);
        assertEquals(1, hits);
    }

    @Test
    public void queryPostXQueryError() throws IOException {
        final HttpResponse response = doPostWithAuth(getResourceUri(), QUERY_REQUEST_ERROR);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.BAD_REQUEST_400, resultStatusCode);
    }

    /**
     * See: <a href="https://github.com/eXist-db/exist/issues/5845">[BUG] Spurious namespace declarations in REST API results</a>
     */
    @Test
    public void queryPostWithEnclosedExpressionResponseNamespaces() throws IOException {
        String query =
                "<query xmlns=\"http://exist.sourceforge.net/NS/exist\" wrap=\"no\" typed=\"no\">\n" +
                "   <text>&lt;doc&gt;{3+4}&lt;/doc&gt;</text>\n" +
                "</query>";

        HttpResponse response = doPostWithAuth(getResourceUri(), query);
        int resultStatusCode = response.getStatusLine()
            .getStatusCode();

        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        String data = readResponse(response.getEntity());
        assertEquals("<doc>7</doc>", data.trim());

        query =
                "<query xmlns=\"http://exist.sourceforge.net/NS/exist\" wrap=\"no\" typed=\"yes\">\n" +
                "   <text>&lt;doc&gt;{3+4}&lt;/doc&gt;</text>\n" +
                "</query>";

        response = doPostWithAuth(getResourceUri(), query);
        resultStatusCode = response.getStatusLine()
            .getStatusCode();

        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        data = readResponse(response.getEntity());
        assertEquals("<doc>7</doc>", data.trim());
    }

    /**
     * See: <a href="https://github.com/eXist-db/exist/issues/5845">[BUG] Spurious namespace declarations in REST API results</a>
     */
    @Test
    public void queryPostWithoutEnclosedExpressionResponseNamespaces() throws IOException {
        String query =
                "<query xmlns=\"http://exist.sourceforge.net/NS/exist\" wrap=\"no\" typed=\"no\">\n" +
                "   <text>&lt;doc&gt;7&lt;/doc&gt;</text>\n" +
                "</query>";

        HttpResponse response = doPostWithAuth(getResourceUri(), query);
        int resultStatusCode = response.getStatusLine()
            .getStatusCode();

        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        String data = readResponse(response.getEntity());
        assertEquals("<doc>7</doc>", data.trim());

        query =
                "<query xmlns=\"http://exist.sourceforge.net/NS/exist\" wrap=\"no\" typed=\"yes\">\n" +
                "   <text>&lt;doc&gt;7&lt;/doc&gt;</text>\n" +
                "</query>";

        response = doPostWithAuth(getResourceUri(), query);
        resultStatusCode = response.getStatusLine()
            .getStatusCode();

        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        data = readResponse(response.getEntity());
        assertEquals("<doc>7</doc>", data.trim());
    }

    @Test
    public void queryPostWithExternalVariableUntypedNotSupplied() throws IOException {
        queryPostWithExternalVariable(HttpStatus.BAD_REQUEST_400, null, null);
    }

    /**
     * See: <a href="https://github.com/eXist-db/exist/issues/5844">[BUG] The JavaDoc comments for variable in the REST API are inconsistent with the implementation</a>
     */
    @Test
    public void queryPostWithExternalVariableUntypedSuppliedUntypedStringValue() throws IOException {
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
    public void queryPostWithExternalVariableElementsString() throws IOException {
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
    public void queryPostWithExternalVariableUntypedSuppliedUntypedDocumentValue() throws IOException {
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
    public void queryPostWithExternalVariableDocumentsString() throws IOException {
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
    public void queryPostWithExternalVariableUntypedSuppliedUntypedCommentValue() throws IOException {
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
    public void queryPostWithExternalVariableCommentsString() throws IOException {
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
    public void queryPostWithExternalVariableUntypedSuppliedUntypedProcessingInstructionValue() throws IOException {
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
    public void queryPostWithExternalVariableProcessingInstructionsString() throws IOException {
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
    public void queryPostWithExternalVariableUntypedSuppliedUntypedTextValue() throws IOException {
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
    public void queryPostWithExternalVariableTextsString() throws IOException {
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

    private void queryPostWithExternalVariable(final int expectedResponseCode, @Nullable final String xqExternalVariableType, final Tuple2<String, String>... externalVariableSequence) throws IOException {
        queryPostWithExternalVariable(Tuple(expectedResponseCode, null), externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final Tuple2<Integer, String> expectedResponse, @Nullable final String xqExternalVariableType, final Tuple2<String, String>... externalVariableSequence) throws IOException {
        queryPostWithExternalVariable(expectedResponse, externalVariableSequence, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final int expectedResponseCode, final Tuple2<String, String>[] expectedResult, @Nullable final String xqExternalVariableType, final Tuple2<String, String>... externalVariableSequence) throws IOException {
        queryPostWithExternalVariable(Tuple(expectedResponseCode, null), expectedResult, xqExternalVariableType, externalVariableSequence);
    }

    private void queryPostWithExternalVariable(final Tuple2<Integer, String> expectedResponse, final Tuple2<String, String>[] expectedResult, @Nullable final String xqExternalVariableType, final Tuple2<String, String>... externalVariableSequence) throws IOException {
        final String query = buildQueryExternalVariable(xqExternalVariableType, externalVariableSequence);

        final HttpResponse response = doPostWithAuth(getResourceUri(), query);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();

        assertEquals("Server returned response code: " + resultStatusCode, (int) expectedResponse._1, resultStatusCode);

        if (expectedResponse._1 == HttpStatus.OK_200) {
            final String expected = buildExistVariableResultSequence(expectedResult);

            final String data = readResponse(response.getEntity());
            assertThat(data, CompareMatcher.isIdenticalTo(expected).withNamespaceContext(NS_CONTEXT).withAttributeFilter(ignoreExistTimingAttributes).ignoreWhitespace());
        } else if (expectedResponse._2 != null) {
            final String data = readResponse(response.getEntity());
            assertThat(data, CompareMatcher.isIdenticalTo(expectedResponse._2).withNamespaceContext(NS_CONTEXT).withAttributeFilter(ignoreExistTimingAttributes).ignoreWhitespace());
        }
    }

    private static String buildExistVariableResultSequence(final Tuple2<String, String>... resultSequence) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<exist:result xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" xmlns:sx=\"http://exist-db.org/xquery/types/serialized\" exist:count=\"").append(resultSequence.length).append("\" exist:hits=\"").append(resultSequence.length).append("\" exist:start=\"1\">\n");
        for (final Tuple2<String, String> resultSequenceItem : resultSequence) {

            // TODO(AR) When wrap="yes" is set for the eXist-db REST API it does not wrap Node Types in <exist:value> - that is probably a bug in eXist-db that should be fixed - https://github.com/eXist-db/exist/issues/5909
            final int xdmType;
            if (resultSequenceItem._1 == null) {
                xdmType = resultSequenceItem._2.startsWith("<") ? Type.ELEMENT : Type.ITEM;
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
                builder.append("<exist:attribute>");
            }

            builder.append(resultSequenceItem._2);

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

    private static String buildQueryExternalVariable(@Nullable final String xqExternalVariableType, @Nullable final Tuple2<String, String>... externalVariableSequence) {
        final StringBuilder builder = new StringBuilder();
        builder.append("<exist:query xmlns:exist=\"http://exist.sourceforge.net/NS/exist\" xmlns:sx=\"http://exist-db.org/xquery/types/serialized\" wrap=\"yes\" typed=\"yes\">\n");

        if (externalVariableSequence!= null) {
            builder.append("\t<exist:variables>\n");
            builder.append("\t\t<exist:variable>\n");
            builder.append("\t\t\t<exist:qname><exist:prefix>local</exist:prefix><exist:localname>my-variable</exist:localname></exist:qname>");
            builder.append("\t\t\t<sx:sequence>\n");
            for (final Tuple2<String, String> externalVariableSequenceItem : externalVariableSequence) {
                builder.append("\t\t\t\t<sx:value");
                if (externalVariableSequenceItem._1 != null) {
                    builder.append(" type=\"").append(externalVariableSequenceItem._1).append("\"");
                }
                builder.append('>');
                builder.append(externalVariableSequenceItem._2);
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

    @Test
    public void queryGet() throws IOException {
        final String uri = getCollectionUri()
                + "?_query="
                + URLEncoder
                        .encode(
                                "doc('"
                                        + XmldbURI.ROOT_COLLECTION
                                        + "/test/test.xml')//para[. = '\u00E4\u00E4\u00FC\u00FC\u00F6\u00F6\u00C4\u00C4\u00D6\u00D6\u00DC\u00DC']/text()",
                                UTF_8.displayName());

        final HttpResponse response = doGet(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        assertNotNull(readResponse(response.getEntity()));
    }

    @Test
    public void queryGetXQueryError() throws IOException {
        final String uri = getCollectionUri()
                + "?_query="
                + URLEncoder
                .encode(
                        "not-$a:-function()",
                        UTF_8.displayName());
        final HttpResponse response = doGet(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.BAD_REQUEST_400, resultStatusCode);
    }

    @Test
    public void requestModule() throws IOException {
        String uri = getCollectionUri() + "?_query=request:get-uri()&_wrap=no";
        HttpResponse response = doGet(uri);
        int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        String responseBody = readResponse(response.getEntity()).trim();
        assertTrue(responseBody.endsWith(XmldbURI.ROOT_COLLECTION + "/test"));


        uri = getCollectionUri() + "?_query=request:get-url()&_wrap=no";
        response = doGet(uri);
        resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        responseBody = readResponse(response.getEntity()).trim();
        assertTrue(responseBody.endsWith(XmldbURI.ROOT_COLLECTION + "/test"));
    }

    @Test
    public void requestGetParameterFromModule() throws IOException {
        /* store the documents that we need for this test */
        doPut(TEST_XQUERY_PARAMETER, "requestparameter.xql", HttpStatus.CREATED_201);
        doPut(TEST_XQUERY_PARAMETER_MODULE, "requestparametermod.xqm", HttpStatus.CREATED_201);

        /* execute the stored xquery a few times */
        for (int i = 0; i < 5; i++) {
            final String uri = getCollectionUri() + "/requestparameter.xql?doc=somedoc" + i;
            final HttpResponse response = doGetWithAuth(uri);
            final int resultStatusCode = response.getStatusLine()
                .getStatusCode();
            assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
            assertResponseMediaType("application/xml", response);

            //get the response of the query
            final String responseBody = readResponse(response.getEntity());

            final String strXQLRequestParameter = responseBody.substring("xql=".length(), responseBody.indexOf("xqm="));
            final String strXQMRequestParameter = responseBody.substring(responseBody.indexOf("xqm=") + "xqm=".length(), responseBody.lastIndexOf("\r\n"));

            //check the responses
            assertEquals("XQuery Request Parameter is: \"" + strXQLRequestParameter + "\" expected: \"somedoc" + i + "\"", "somedoc" + i, strXQLRequestParameter);
            assertEquals("XQuery Module Request Parameter is: \"" + strXQMRequestParameter + "\" expected: \"somedoc" + i + "\"", "somedoc" + i, strXQMRequestParameter);
        }
    }

    @Test
    public void storedQuery() throws IOException {
        doPut(TEST_MODULE, "module.xq", HttpStatus.CREATED_201);
        doPut(TEST_XQUERY, "test.xq", HttpStatus.CREATED_201);

        doStoredQuery(false, false);

        // cached:
        doStoredQuery(true, false);

        // cached and wrapped:
        doStoredQuery(true, true);
    }

    @Test
    public void execQueryWithNoAuth() throws IOException {
        doPut(AUTH_QUERY, "auth.xq", HttpStatus.CREATED_201);

        // allow query to be executed only by owner
        chmod(XmldbURI.ROOT_COLLECTION + "/test/auth.xq", "rwxrw-r--");

        // call the auth.xq
        final String uri = getCollectionUri() + "/auth.xq";
        final HttpResponse response = doGet(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.UNAUTHORIZED_401, resultStatusCode);
    }

    @Test
    public void execGuestQueryWithNoAuth() throws IOException {
        doPut(AUTH_QUERY, "auth.xq", HttpStatus.CREATED_201);

        // allow query to be executed by guest
        chmod(XmldbURI.ROOT_COLLECTION + "/test/auth.xq", "rwxrw-r-x");

        // call the auth.xq
        final String uri = getCollectionUri() + "/auth.xq";
        final HttpResponse response = doGet(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        final String responseBody = readResponse(response.getEntity());

        final Source expectedSource = Input.from(
                "<authorization>\n" +
                        "    <sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "        <sm:real>\n" +
                        "            <sm:username>guest</sm:username>\n" +
                        "            <sm:groups>\n" +
                        "                <sm:group>guest</sm:group>\n" +
                        "            </sm:groups>\n" +
                        "        </sm:real>\n" +
                        "    </sm:id>\n" +
                        "    <header/>\n" +
                        "</authorization>").build();
        final Source actualSource = Input.from(responseBody).build();

        final Diff diff = DiffBuilder.compare(expectedSource)
                .withTest(actualSource)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void execQueryWithBasicAuth() throws IOException {
        doPut(AUTH_QUERY, "auth.xq", HttpStatus.CREATED_201);

        // allow query to be executed only by owner
        chmod(XmldbURI.ROOT_COLLECTION + "/test/auth.xq", "rwxrw-r--");

        // call the auth.xq
        final String uri = getCollectionUri() + "/auth.xq";
        final HttpResponse response = doGetWithAuth(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        final String responseBody = readResponse(response.getEntity());

        final Source expectedSource = Input.from(
                "<authorization>\n" +
                        "    <sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "        <sm:real>\n" +
                        "            <sm:username>admin</sm:username>\n" +
                        "            <sm:groups>\n" +
                        "                <sm:group>dba</sm:group>\n" +
                        "            </sm:groups>\n" +
                        "        </sm:real>\n" +
                        "    </sm:id>\n" +
                        "    <header>Basic YWRtaW46</header>\n" +
                        "</authorization>").build();
        final Source actualSource = Input.from(responseBody).build();

        final Diff diff = DiffBuilder.compare(expectedSource)
                .withTest(actualSource)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void execQueryWithBasicAuthCaseInsensitive() throws IOException {
        doPut(AUTH_QUERY, "auth.xq", HttpStatus.CREATED_201);

        // allow query to be executed only by owner
        chmod(XmldbURI.ROOT_COLLECTION + "/test/auth.xq", "rwxrw-r--");

        // call the auth.xq
        final String uri = getCollectionUri() + "/auth.xq";
        final HttpResponse response = Request.Get(uri)
            .setHeader("Authorization", "bAsiC " + credentials)  // NOTE(AR): Intentional use of 'bAsiC' to test case-insensitive scheme matching
            .execute()
            .returnResponse();
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        final String responseBody = readResponse(response.getEntity());

        final Source expectedSource = Input.from(
                "<authorization>\n" +
                        "    <sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "        <sm:real>\n" +
                        "            <sm:username>admin</sm:username>\n" +
                        "            <sm:groups>\n" +
                        "                <sm:group>dba</sm:group>\n" +
                        "            </sm:groups>\n" +
                        "        </sm:real>\n" +
                        "    </sm:id>\n" +
                        "    <header>Basic YWRtaW46</header>\n" +
                        "</authorization>").build();
        final Source actualSource = Input.from(responseBody).build();

        final Diff diff = DiffBuilder.compare(expectedSource)
                .withTest(actualSource)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void execSetUidQueryWithNoAuth() throws IOException {
        doPut(AUTH_QUERY, "auth.xq", HttpStatus.CREATED_201);

        // allow query to be executed setUid as admin by guest
        chmod(XmldbURI.ROOT_COLLECTION + "/test/auth.xq", "rwsr--r-x");

        // call the auth.xq
        final String uri = getCollectionUri() + "/auth.xq";
        final HttpResponse response = doGet(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        final String responseBody = readResponse(response.getEntity());

        final Source expectedSource = Input.from(
                "<authorization>\n" +
                        "    <sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "        <sm:real>\n" +
                        "            <sm:username>guest</sm:username>\n" +
                        "            <sm:groups>\n" +
                        "                <sm:group>guest</sm:group>\n" +
                        "            </sm:groups>\n" +
                        "        </sm:real>\n" +
                        "        <sm:effective>\n" +
                        "            <sm:username>admin</sm:username>\n" +
                        "            <sm:groups>\n" +
                        "                <sm:group>dba</sm:group>\n" +
                        "            </sm:groups>\n" +
                        "        </sm:effective>\n" +
                        "    </sm:id>\n" +
                        "    <header/>\n" +
                        "</authorization>").build();
        final Source actualSource = Input.from(responseBody).build();

        final Diff diff = DiffBuilder.compare(expectedSource)
                .withTest(actualSource)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void execSetUidQueryWithBasicAuth() throws IOException {
        doPut(AUTH_QUERY, "auth.xq", HttpStatus.CREATED_201);

        // allow query to be executed setUid as admin by guest
        chmod(XmldbURI.ROOT_COLLECTION + "/test/auth.xq", "rwsr--r-x");

        // call the auth.xq
        final String uri = getCollectionUri() + "/auth.xq";
        final HttpResponse response = doGetWithAuth(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        final String responseBody = readResponse(response.getEntity());

        final Source expectedSource = Input.from(
                "<authorization>\n" +
                        "    <sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "        <sm:real>\n" +
                        "            <sm:username>admin</sm:username>\n" +
                        "            <sm:groups>\n" +
                        "                <sm:group>dba</sm:group>\n" +
                        "            </sm:groups>\n" +
                        "        </sm:real>\n" +
                        "    </sm:id>\n" +
                        "    <header>Basic YWRtaW46</header>\n" +
                        "</authorization>").build();
        final Source actualSource = Input.from(responseBody).build();

        final Diff diff = DiffBuilder.compare(expectedSource)
                .withTest(actualSource)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void execQueryWithBearerAuth() throws IOException {
        doPut(AUTH_QUERY, "auth.xq", HttpStatus.CREATED_201);

        // allow query to be executed only by owner
        chmod(XmldbURI.ROOT_COLLECTION + "/test/auth.xq", "rwxrw-r--");

        // call the auth.xq
        final String uri = getCollectionUri() + "/auth.xq";

        final HttpResponse response = Request.Get(uri)
            .setHeader("Authorization", "Bearer some-token")
            .execute()
            .returnResponse();

        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.UNAUTHORIZED_401, resultStatusCode);
    }

    @Test
    public void execSetUidQueryWithBearerAuth() throws IOException {
        doPut(AUTH_QUERY, "auth.xq", HttpStatus.CREATED_201);

        // allow query to be executed setUid as admin by guest
        chmod(XmldbURI.ROOT_COLLECTION + "/test/auth.xq", "rwsr--r-x");

        // call the auth.xq
        final String uri = getCollectionUri() + "/auth.xq";
        final HttpResponse response = Request.Get(uri)
            .setHeader("Authorization", "Bearer some-token")
            .execute()
            .returnResponse();

        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        final String responseBody = readResponse(response.getEntity());

        final Source expectedSource = Input.from(
                "<authorization>\n" +
                        "    <sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "        <sm:real>\n" +
                        "            <sm:username>guest</sm:username>\n" +
                        "            <sm:groups>\n" +
                        "                <sm:group>guest</sm:group>\n" +
                        "            </sm:groups>\n" +
                        "        </sm:real>\n" +
                        "        <sm:effective>\n" +
                        "            <sm:username>admin</sm:username>\n" +
                        "            <sm:groups>\n" +
                        "                <sm:group>dba</sm:group>\n" +
                        "            </sm:groups>\n" +
                        "        </sm:effective>\n" +
                        "    </sm:id>\n" +
                        "    <header>Bearer some-token</header>\n" +
                        "</authorization>").build();
        final Source actualSource = Input.from(responseBody).build();

        final Diff diff = DiffBuilder.compare(expectedSource)
                .withTest(actualSource)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    //test rest server ability to handle encoded characters
    // all the tests with EncodedPath in function declaration aim to test rest server ability to handle special characters
    @Test
    public void doGetEncodedPath() throws IOException {
        String uri = getServerUri() + XmldbURI.ROOT_COLLECTION + "/AéB/AéB.xml";
        final HttpResponse response = doGet(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        assertResponseMediaType("application/xml", response);

        final String responseBody = readResponse(response.getEntity());

        //readResponse is appending \r\n to each line that's why its added the expected content
        assertEquals("Server returned document content " + responseBody, "<foobar/>\r\n", responseBody);
    }

    @Test
    public void doHeadEncodedPath() throws IOException {
        final String uri = getServerUri() + XmldbURI.ROOT_COLLECTION + "/AéB/AéB.xml";
        final HttpResponse response = doHead(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
    }

    @Test
    public void doPutEncodedPath() throws IOException {
        final String uri = getServerUri() + XmldbURI.ROOT_COLLECTION + "/AéB/AéB.xml";
        final String data = "<foobar/>";

        HttpResponse response = doPutWithAuth(uri, "application/xml", data);
        int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.CREATED_201, resultStatusCode);

        // assert file content updated
        response = doGet(uri);
        resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        final String responseBody = readResponse(response.getEntity());
        //readResponse is appending \r\n to each line that's why its added the expected content
        assertEquals("Server returned document content " + responseBody, data + "\r\n", responseBody);
    }

    @Test
    public void doPostEncodedPath() throws IOException {
        final String uri = getServerUri() + XmldbURI.ROOT_COLLECTION + "/AéB/AéB.xml";

        final String data = "<query xmlns=\"http://exist.sourceforge.net/NS/exist\">\n" +
                "    <text>\n" +
                "        //foo\n" +
                "    </text>\n" +
                "</query>";

        final HttpResponse response = doPostWithAuth(uri, data);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        final String responseBody = readResponse(response.getEntity());

        //readResponse is appending \r\n to each line that's why its added the expected content
        assertTrue("Server returned " + responseBody, responseBody.contains("exist:hits=\"1\""));
    }

    @Test
    public void doDeleteEncodedPath() throws IOException {
        final String docUri = getServerUri() + XmldbURI.ROOT_COLLECTION + "/AéB/AéB.xml";
        HttpResponse response = doDeleteWithAuth(docUri);
        int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        // assert file content updated
        response = doGet(docUri);
        resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.NOT_FOUND_404, resultStatusCode);
    }

    /**
     * By default there should be NO doctype serialized.
     */
    @Test
    public void getDocTypeDefault() throws IOException {
        final HttpResponse response = doGet(getResourceWithDocTypeUri());
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        assertResponseMediaType("application/xml", response);

        final String responseBody = readResponse(response.getEntity());
        assertEquals("<!DOCTYPE bookmap PUBLIC \"-//OASIS//DTD DITA BookMap//EN\" \"bookmap.dtd\">\r\n" +
                "<bookmap id=\"bookmap-1\"/>\r\n", responseBody);
    }

    @Test
    public void getDocTypeNo() throws IOException {
        final HttpResponse response = doGet(getResourceWithDocTypeUri() + "?_output-doctype=no");
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        assertResponseMediaType("application/xml", response);

        final String responseBody = readResponse(response.getEntity());
        assertEquals("<bookmap id=\"bookmap-1\"/>\r\n", responseBody);
    }

    @Test
    public void getDocTypeYes() throws IOException {
        final HttpResponse response = doGet(getResourceWithDocTypeUri() + "?_output-doctype=yes");
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        assertResponseMediaType("application/xml", response);

        final String responseBody = readResponse(response.getEntity());
        assertEquals(
                "<!DOCTYPE bookmap PUBLIC \"-//OASIS//DTD DITA BookMap//EN\" \"bookmap.dtd\">\r\n" +
                "<bookmap id=\"bookmap-1\"/>\r\n", responseBody);
    }

    @Test
    public void getDocWithXslPi() throws IOException {
        final String uri = getServerUri() + TEST_XSLPI_COLLECTION_URI.append(TEST_XML_DOC_WITH_XSLPI_URI);
        final HttpResponse response = doGet(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        // NOTE(AR) At present the RESTServer will force XHTML with text/html mimetype and indenting if an xsl-pi is used... this should probably be improved in future!
        assertResponseMediaType("text/html", response);

        final String responseBody = readResponse(response.getEntity());

        final Source expectedSource = Input.from("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<copied>\n" +
                "    <bookmap id=\"bookmap-1\"></bookmap>\n" +
                "</copied>\n").build();
        final Source actualSource = Input.from(responseBody).build();

        final Diff diff = DiffBuilder.compare(expectedSource)
                .withTest(actualSource)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void getDocWithXslPi_twice() throws IOException {
        // NOTE(AR) doing this twice revealed an issue with the Serializer not being correctly reset
        getDocWithXslPi();
        getDocWithXslPi();
    }

    private void chmod(final String resourcePath, final String mode) throws IOException {
        final String uri = getCollectionUri() +"?_query=" + URLEncoder.encode(
                "sm:chmod(xs:anyURI('" + resourcePath + "'), '" + mode + "')",
                UTF_8.displayName());
        final HttpResponse response = doGetWithAuth(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
    }

    private void doStoredQuery(final boolean cacheHeader, final boolean wrap) throws IOException {
        String uri = getCollectionUri() + "/test.xq?p=Hello";
        if (wrap) {
            uri += "&_wrap=yes";
        }

        final HttpResponse response = doGetWithAuth(uri);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);

        final String cached = response.getFirstHeader("X-XQuery-Cached").getValue();
        assertNotNull(cached);
        assertEquals(cacheHeader, Boolean.valueOf(cached).booleanValue());
        if (wrap) {
            assertResponseMediaType("application/xml", response);
        } else {
            assertResponseMediaType("text/text", response);
        }

        final String responseBody = readResponse(response.getEntity());
        if (wrap) {
            assertTrue("Server returned response: " + responseBody, responseBody.startsWith("<exist:result "));
        } else {
            assertTrue("Server returned response: " + responseBody, responseBody.startsWith("Hello World!"));
        }
    }

    private void doPut(final String data, final String path, final int expectedResponseCode) throws IOException {
        final String uri = getCollectionUri() + '/' + path;
        final HttpResponse response = doPutWithAuth(uri, "application/xquery", data);
        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, expectedResponseCode, resultStatusCode);
    }

    private int uploadData() throws IOException {
        final HttpResponse response = doPutWithAuth(getResourceUri(), "application/xml", XML_DATA);
        return response.getStatusLine().getStatusCode();
    }

    private HttpResponse doPutWithAuth(final String uri, final String contentType, final String data) throws IOException {
        return Request.Put(uri)
            .setHeader("Authorization", "Basic " + credentials)
            .bodyString(data, ContentType.parse(contentType))
            .execute()
            .returnResponse();
    }

    private void doGet() throws IOException {
        final HttpResponse response = doGet(getResourceUri());

        final int resultStatusCode = response.getStatusLine()
            .getStatusCode();
        assertEquals("Server returned response code: " + resultStatusCode, HttpStatus.OK_200, resultStatusCode);
        assertResponseMediaType("application/xml", response);

        assertNotNull(readResponse(response.getEntity()));
    }

    private HttpResponse doGet(final String uri) throws IOException {
        return Request.Get(uri)
            .execute()
            .returnResponse();
    }

    private HttpResponse doGetWithAuth(final String uri) throws IOException {
        return Request.Get(uri)
            .setHeader("Authorization", "Basic " + credentials)
            .execute()
            .returnResponse();
    }

    private HttpResponse doHead(final String uri) throws IOException {
        return Request.Head(uri)
            .execute()
            .returnResponse();
    }

    private HttpResponse doHeadWithAuth(final String uri) throws IOException {
        return Request.Head(uri)
            .setHeader("Authorization", "Basic " + credentials)
            .execute()
            .returnResponse();
    }

    private HttpResponse doPostWithAuth(final String uri, final String content) throws IOException {
        return Request.Post(uri)
            .setHeader("Authorization", "Basic " + credentials)
            .bodyString(content, ContentType.APPLICATION_XML)
            .execute()
            .returnResponse();
    }

    private HttpResponse doDeleteWithAuth(final String uri) throws IOException {
        return Request.Delete(uri)
            .setHeader("Authorization", "Basic " + credentials)
            .execute()
            .returnResponse();
    }

    private String readResponse(final HttpEntity response) throws IOException {
        try (final InputStream is = response.getContent();
             final BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8))) {
            String line;
            final StringBuilder out = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                out.append(line);
                out.append("\r\n");
            }
            return out.toString();
        }
    }

    private int parseResponse(final String data) throws IOException, SAXException, ParserConfigurationException {
        final SAXParserFactory factory = ExistSAXParserFactory.getSAXParserFactory();
        factory.setNamespaceAware(true);
        try (final Reader reader = new StringReader(data)) {
            final InputSource src = new InputSource(reader);
            final SAXParser parser = factory.newSAXParser();
            final XMLReader xmlReader = parser.getXMLReader();
            final SAXAdapter adapter = new SAXAdapter();
            xmlReader.setContentHandler(adapter);
            xmlReader.parse(src);

            final Document doc = adapter.getDocument();

            final Element root = doc.getDocumentElement();
            final String hits = root.getAttributeNS(Namespaces.EXIST_NS, "hits");
            return Integer.parseInt(hits);
        }
    }

    private static void assertResponseMediaType(final String expectedContentType, final HttpResponse response) {
        String contentType = response.getEntity().getContentType().getValue();
        final int semicolon = contentType.indexOf(';');
        if (semicolon > 0) {
            contentType = contentType.substring(0, semicolon).trim();
        }
        assertEquals("Server returned content type: " + contentType, expectedContentType, contentType);
    }
}
