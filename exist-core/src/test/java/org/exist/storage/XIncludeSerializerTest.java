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
package org.exist.storage;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.exist.Namespaces;
import org.exist.test.DatabaseWebServerExtension;
import org.exist.util.io.InputStreamUtil;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.xmlunit.matchers.CompareMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jim fuller at webcomposite.com
 *
 * Test XInclude Serialiser via REST/XMLRPC/WEBDAV/SOAP interfaces
 */
public class XIncludeSerializerTest {

    @RegisterExtension
    public static final DatabaseWebServerExtension DATABASE_WEB_SERVER = new DatabaseWebServerExtension(true, true, true, true);

    private final static XmldbURI XINCLUDE_COLLECTION = XmldbURI.ROOT_COLLECTION_URI.append("xinclude_test");
    private final static XmldbURI XINCLUDE_NESTED_COLLECTION = XmldbURI.ROOT_COLLECTION_URI.append("xinclude_test/data");

    private final static String getXmlRpcApi() {
        return "http://127.0.0.1:" + DATABASE_WEB_SERVER.getPort() + "/xmlrpc";
    }

    private final static String getRestUri()  {
        return "http://admin:admin@127.0.0.1:" + DATABASE_WEB_SERVER.getPort() + "/db/xinclude_test";
    }

    private final static String XML_DATA1
            = "<test xmlns:xi='" + Namespaces.XINCLUDE_NS + "'>"
            + "<root>"
            + "<xi:include href='metatags.xml'/>"
            + "</root>"
            + "</test>";

    private final static String XML_DATA2
            = "<html>"
            + "<head>"
            + "<metatag xml:id='metatag' name='test' description='test'/>"
            + "</head>"
            + "</html>";

    private final static String XML_DATA3
            = "<test xmlns:xi='" + Namespaces.XINCLUDE_NS + "'>"
            + "<root>"
            + "<xi:include href='../xinclude_test/data/metatags.xml'/>"
            + "</root>"
            + "</test>";

    private final static String XML_DATA4
            = "<test xmlns:xi='" + Namespaces.XINCLUDE_NS + "'>"
            + "<root>"
            + "<xi:include href='data/metatags.xml'/>"
            + "</root>"
            + "</test>";

    private final static String XML_DATA5
            = "<test xmlns:xi='" + Namespaces.XINCLUDE_NS + "'>"
            + "<root>"
            + "<xi:include href='data/metatags.xml' xpointer='xpointer(//metatag)'/>"
            + "</root>"
            + "</test>";

    private final static String XML_DATA6
            = "<test xmlns:xi='" + Namespaces.XINCLUDE_NS + "'>"
            + "<root>"
            + "<xi:include href='data/metatags.xml' xpointer='metatag'/>"
            + "</root>"
            + "</test>";

    private final static String XML_DATA7
            = "<test xmlns:xi='" + Namespaces.XINCLUDE_NS + "'>"
            + "<root>"
            + "<xi:include href='data/unknown.xml'>"
            + "<xi:fallback><warning>Not found</warning></xi:fallback>"
            + "</xi:include>"
            + "</root>"
            + "</test>";

    private final static String XML_DATA8
            = "<test xmlns:xi='" + Namespaces.XINCLUDE_NS + "'>"
            + "<root>"
            + "<xi:include href='data/unknown.xml'/>"
            + "</root>"
            + "</test>";

    private final static String XML_RESULT = "<test xmlns:xi='" + Namespaces.XINCLUDE_NS + "'>"
            + "<root>"
            + "<html>"
            + "<head>"
            + "<metatag xml:id='metatag' name='test' description='test'/>"
            + "</head>"
            + "</html>"
            + "</root>"
            + "</test>";

    private final static String XML_RESULT_XPOINTER = "<test xmlns:xi='" + Namespaces.XINCLUDE_NS + "'>"
            + "<root>"
            + "<metatag xml:id='metatag' name='test' description='test'/>"
            + "</root>"
            + "</test>";

    private final static String XML_RESULT_FALLBACK1 = "<test xmlns:xi='" + Namespaces.XINCLUDE_NS + "'>"
            + "<root>"
            + "<warning>Not found</warning>"
            + "</root>"
            + "</test>";

    @Test
    void absSimpleREST() throws IOException {
        // path needs to indicate indent and wrap is off
        final String uri = getRestUri() + "/test_simple.xml?_indent=no&_wrap=no";

        final HttpResponse response = Executor.newInstance()
            .execute(Request.Get(uri))
            .returnResponse();

        final String responseBody = responseBodyToString(response);

        assertEquals(response.getStatusLine().toString() + ": " + responseBody, HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertThat(responseBody, CompareMatcher.isIdenticalTo(XML_RESULT));
    }

    @Test
    void relSimpleREST1() throws IOException {
        final String uri = getRestUri() + "/test_relative1.xml?_indent=no&_wrap=no";

        final HttpResponse response = Executor.newInstance()
            .execute(Request.Get(uri))
            .returnResponse();

        final String responseBody = responseBodyToString(response);

        assertEquals(response.getStatusLine().toString() + ": " + responseBody, HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertThat(responseBody, CompareMatcher.isIdenticalTo(XML_RESULT));
    }

    @Test
    void relSimpleREST2() throws IOException {
        // path needs to indicate indent and wrap is off
        final String uri = getRestUri() + "/test_relative2.xml?_indent=no&_wrap=no";

        final HttpResponse response = Executor.newInstance()
            .execute(Request.Get(uri))
            .returnResponse();

        final String responseBody = responseBodyToString(response);

        assertEquals(response.getStatusLine().toString() + ": " + responseBody, HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertThat(responseBody, CompareMatcher.isIdenticalTo(XML_RESULT));
    }

    @Test
    void xpointerREST3() throws IOException {
        final String uri = getRestUri() + "/test_xpointer1.xml?_indent=no&_wrap=no";

        final HttpResponse response = Executor.newInstance()
            .execute(Request.Get(uri))
            .returnResponse();

        final String responseBody = responseBodyToString(response);

        assertEquals(response.getStatusLine().toString() + ": " + responseBody, HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertThat(responseBody, CompareMatcher.isIdenticalTo(XML_RESULT_XPOINTER));
    }

    @Test
    void xpointerREST4() throws IOException {
        final String uri = getRestUri() + "/test_xpointer2.xml?_indent=no&_wrap=no";

        final HttpResponse response = Executor.newInstance()
            .execute(Request.Get(uri))
            .returnResponse();

        final String responseBody = responseBodyToString(response);

        assertEquals(response.getStatusLine().toString() + ": " + responseBody, HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertThat(responseBody, CompareMatcher.isIdenticalTo(XML_RESULT_XPOINTER));
    }

    @Test
    void fallback1() throws IOException {
        final String uri = getRestUri() + "/test_fallback1.xml?_indent=no&_wrap=no";

        final HttpResponse response = Executor.newInstance()
            .execute(Request.Get(uri))
            .returnResponse();

        final String responseBody = responseBodyToString(response);

        assertEquals(response.getStatusLine().toString() + ": " + responseBody, HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertThat(responseBody, CompareMatcher.isIdenticalTo(XML_RESULT_FALLBACK1));
    }

    @Test
    void fallback2() throws IOException {
        final String uri = getRestUri() + "/test_fallback2.xml?_indent=no&_wrap=no";

        final HttpResponse response = Executor.newInstance()
            .execute(Request.Get(uri))
            .returnResponse();

        final String responseBody = responseBodyToString(response);

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode(), response.getStatusLine().toString() + ": " + responseBody);
    }

    //TODO add full url test e.g. http://www.example.org/test.xml for xinclude
    //TODO add simple and relative url with xpointer
    //ex. <xi:include href="../javascript.xml#xpointer(html/head)"/>
    //TODO add simple and relative url with xpointer and namespaces
    // ex. <xi:include href="../javascript.xml#xmlns(x=http://www.w3.org/1999/xhtml)xpointer(/x:html/x:head)"/>
    /*
     * XML-RPC tests
     *
     */
    //TODO check serialisation via this interface, simple and relative

    /*
     * WebDAV tests
     *
     */
    //TODO check serialisation via this interface, simple and relative???
    // probably overkill
    /*
     * SOAP tests
     *
     */
    // probably overkill
    //TODO check serialisation via this interface, simple and relative???
    // probably overkill

    private static XmlRpcClient getClient() throws MalformedURLException {
        final XmlRpcClient client = new XmlRpcClient();
        final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setEnabledForExtensions(true);
        config.setServerURL(new URL(getXmlRpcApi()));
        config.setBasicUserName("admin");
        config.setBasicPassword("");
        client.setConfig(config);
        return client;
    }

    //TODO create reader for xml
    /*
     * SetUp / TearDown functions
     *
     */
    @BeforeAll
    static void startDB() throws XmlRpcException, MalformedURLException {
        final XmlRpcClient xmlrpc = getClient();
        final List<Object> params = new ArrayList<>();
        params.add(XINCLUDE_COLLECTION.toString());
        xmlrpc.execute("createCollection", params);

        params.clear();
        params.add(XINCLUDE_NESTED_COLLECTION.toString());
        xmlrpc.execute("createCollection", params);

        params.clear();
        params.add(XML_DATA1);
        params.add("/db/xinclude_test/test_simple.xml");
        params.add(1);
        xmlrpc.execute("parse", params);

        params.clear();
        params.add(XML_DATA2);
        params.add("/db/xinclude_test/metatags.xml");
        params.add(1);
        xmlrpc.execute("parse", params);

        params.clear();
        params.add(XML_DATA2);
        params.add("/db/xinclude_test/data/metatags.xml");
        params.add(1);
        xmlrpc.execute("parse", params);

        params.clear();
        params.add(XML_DATA3);
        params.add("/db/xinclude_test/test_relative1.xml");
        params.add(1);
        xmlrpc.execute("parse", params);

        params.clear();
        params.add(XML_DATA4);
        params.add("/db/xinclude_test/test_relative2.xml");
        params.add(1);
        xmlrpc.execute("parse", params);

        params.clear();
        params.add(XML_DATA5);
        params.add("/db/xinclude_test/test_xpointer1.xml");
        params.add(1);
        xmlrpc.execute("parse", params);

        params.clear();
        params.add(XML_DATA6);
        params.add("/db/xinclude_test/test_xpointer2.xml");
        params.add(1);
        xmlrpc.execute("parse", params);

        params.clear();
        params.add(XML_DATA7);
        params.add("/db/xinclude_test/test_fallback1.xml");
        params.add(1);
        xmlrpc.execute("parse", params);

        params.clear();
        params.add(XML_DATA8);
        params.add("/db/xinclude_test/test_fallback2.xml");
        params.add(1);
        xmlrpc.execute("parse", params);
    }

    private static String responseBodyToString(final HttpResponse response) throws IOException {
        try (final InputStream is = response.getEntity().getContent()) {
            return InputStreamUtil.readString(is, UTF_8);
        }
    }
}
