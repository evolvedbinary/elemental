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
package org.exist.http.urlrewrite;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.exist.http.AbstractHttpTest;
import org.exist.test.ExistWebServer;
import org.exist.xmldb.XmldbURI;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Function;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.http.urlrewrite.XQueryURLRewrite.XQUERY_CONTROLLER_FILENAME;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class RedirectTest extends AbstractHttpTest {

    private static final XmldbURI TEST_COLLECTION_NAME = XmldbURI.create("redirect-test");
    private static final XmldbURI TEST_COLLECTION = XmldbURI.create("/db/apps").append(TEST_COLLECTION_NAME);

    private static final String TEST_CONTROLLER =
        "xquery version \"1.0\";\n" +
        "declare namespace exist = \"http://exist.sourceforge.net/NS/exist\";\n" +
        "declare namespace request = \"http://exist-db.org/xquery/request\";\n" +
        "let $redirect-type := request:get-parameter(\"redirect-type\", ())\n" +
        "return\n" +
        "  element exist:dispatch {\n" +
        "    element exist:redirect {\n" +
        "      attribute url { \"http://elsewhere.dom\" },\n" +
        "      $redirect-type ! attribute type { . }\n" +
        "    }\n" +
        "  }\n";

    @ClassRule
    public static final ExistWebServer EXIST_WEB_SERVER = new ExistWebServer(true, false, true, true, false);

    @Test
    public void defaultForGetIsFound() throws IOException {
        testRedirect(Redirect.RedirectType.Found, null, Request::Get);
    }

    @Test
    public void defaultForHeadIsFound() throws IOException {
        testRedirect(Redirect.RedirectType.Found, null, Request::Head);
    }

    @Test
    public void defaultForPostIsSeeOther() throws IOException {
        testRedirect(Redirect.RedirectType.SeeOther, null, Request::Post);
    }

    @Test
    public void defaultForPatchIsSeeOther() throws IOException {
        testRedirect(Redirect.RedirectType.SeeOther, null, Request::Patch);
    }

    @Test
    public void defaultForPutIsSeeOther() throws IOException {
        testRedirect(Redirect.RedirectType.SeeOther, null, Request::Put);
    }

    @Test
    public void defaultForDeleteIsSeeOther() throws IOException {
        testRedirect(Redirect.RedirectType.SeeOther, null, Request::Delete);
    }

    @Test
    public void movedPermanently() throws IOException {
        testGetSendRedirect(Redirect.RedirectType.MovedPermanently);
    }

    @Test
    public void found() throws IOException {
        testGetSendRedirect(Redirect.RedirectType.Found);
    }

    @Test
    public void seeOther() throws IOException {
        testGetSendRedirect(Redirect.RedirectType.SeeOther);
    }

    @Test
    public void temporaryRedirect() throws IOException {
        testGetSendRedirect(Redirect.RedirectType.TemporaryRedirect);
    }

    @Test
    public void permanentRedirect() throws IOException {
        testGetSendRedirect(Redirect.RedirectType.TemporaryRedirect);
    }

    private void testGetSendRedirect(final Redirect.RedirectType redirectType) throws IOException {
        testGetRedirect(redirectType, redirectType);
    }

    private void testGetRedirect(final Redirect.RedirectType expectedRedirectTypeResponse, @Nullable final Redirect.RedirectType sendRedirectType) throws IOException {
        testRedirect(expectedRedirectTypeResponse, sendRedirectType, Request::Get);
    }

    private void testRedirect(final Redirect.RedirectType expectedRedirectTypeResponse, @Nullable final Redirect.RedirectType sendRedirectType, final Function<String, Request> requestFn) throws IOException {
        final String uri = getAppsUri(EXIST_WEB_SERVER) + "/" + TEST_COLLECTION_NAME.append("anything") + (sendRedirectType != null ? "?redirect-type=" + sendRedirectType.httpStatusCode : "");
        final Request request = requestFn.apply(uri);
        final Tuple2<Integer, String> redirectStatusCodeAndLocation = withHttpExecutor(EXIST_WEB_SERVER, true, executor -> {
            final HttpResponse response = executor.execute(request).returnResponse();
            return Tuple(response.getStatusLine().getStatusCode(), response.getFirstHeader("Location").getValue());
        });
        assertEquals(expectedRedirectTypeResponse.httpStatusCode, redirectStatusCodeAndLocation._1.intValue());
        assertEquals("http://elsewhere.dom", redirectStatusCodeAndLocation._2);
    }


    @BeforeClass
    public static void setup() throws IOException {
        final Request request = Request
            .Put(getRestUri(EXIST_WEB_SERVER) + TEST_COLLECTION + "/" + XQUERY_CONTROLLER_FILENAME)
            .bodyString(TEST_CONTROLLER, ContentType.create("application/xquery"));

        final int statusCode = withHttpExecutor(EXIST_WEB_SERVER, executor ->
            executor.execute(request).returnResponse().getStatusLine().getStatusCode()
        );

        assertEquals(HttpStatus.SC_CREATED, statusCode);
    }
}
