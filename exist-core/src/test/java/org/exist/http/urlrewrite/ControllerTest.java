/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.http.urlrewrite;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.exist.http.AbstractHttpTest;
import org.exist.test.ExistWebServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.http.urlrewrite.XQueryURLRewrite.LEGACY_XQUERY_CONTROLLER_FILENAME;
import static org.exist.http.urlrewrite.XQueryURLRewrite.XQUERY_CONTROLLER_FILENAME;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ControllerTest extends AbstractHttpTest {

    private static final String CONTROLLER_XQUERY = "<controller>xq</controller>";
    private static final String LEGACY_CONTROLLER_XQUERY = "<controller>xql</controller>";
    private static final String TEST_DOCUMENT_NAME = "test.xml";

    @Rule
    public final ExistWebServer existWebServer = new ExistWebServer(true, false, true, true, false);

    @Test
    public void findsLegacyController() throws IOException {
        final String testCollectionName = "test-finds-legacy-controller";

        // store the legacy controller
        store(testCollectionName, "application/xquery", LEGACY_XQUERY_CONTROLLER_FILENAME, LEGACY_CONTROLLER_XQUERY);

        // make a request and see if the legacy controller responds
        final Tuple2<Integer, String> responseCodeAndBody = get(testCollectionName, TEST_DOCUMENT_NAME);
        assertEquals(HttpStatus.SC_OK, (int)responseCodeAndBody._1);
        assertEquals(LEGACY_CONTROLLER_XQUERY, responseCodeAndBody._2);
    }

    @Test
    public void findsController() throws IOException {
        final String testCollectionName = "test-finds-controller";

        // store the controller
        store(testCollectionName, "application/xquery", XQUERY_CONTROLLER_FILENAME, CONTROLLER_XQUERY);

        // make a request and see if the controller responds
        final Tuple2<Integer, String> responseCodeAndBody = get(testCollectionName, TEST_DOCUMENT_NAME);
        assertEquals(HttpStatus.SC_OK, (int)responseCodeAndBody._1);
        assertEquals(CONTROLLER_XQUERY, responseCodeAndBody._2);
    }

    @Test
    public void prefersNonLegacyController() throws IOException {
        final String testCollectionName = "test-prefers-non-legacy-controller";

        // store the controller and the legacy controller
        store(testCollectionName, "application/xquery", XQUERY_CONTROLLER_FILENAME, CONTROLLER_XQUERY);
        store(testCollectionName, "application/xquery", LEGACY_XQUERY_CONTROLLER_FILENAME, LEGACY_CONTROLLER_XQUERY);

        // make a request and see if the (non-legacy) controller responds
        final Tuple2<Integer, String> responseCodeAndBody = get(testCollectionName, TEST_DOCUMENT_NAME);
        assertEquals(HttpStatus.SC_OK, (int)responseCodeAndBody._1);
        assertEquals(CONTROLLER_XQUERY, responseCodeAndBody._2);
    }

    private void store(final String testCollectionName, final String documentMediaType, final String documentName, final String documentContent) throws IOException {
        final Request request = Request
                .Put(getRestUri(existWebServer) + "/db/apps/" + testCollectionName + "/" + documentName)
                .bodyString(documentContent, ContentType.create(documentMediaType));
        int statusCode = withHttpExecutor(existWebServer, executor ->
                executor.execute(request).returnResponse().getStatusLine().getStatusCode()
        );
        assertEquals(HttpStatus.SC_CREATED, statusCode);
    }

    private Tuple2<Integer, String> get(final String testCollectionName, final String documentName) throws IOException {
        final Request request = Request
                .Get(getAppsUri(existWebServer) + "/" + testCollectionName + "/" + documentName);
        final Tuple2<Integer, String> responseCodeAndBody = withHttpExecutor(existWebServer, executor -> {
            final HttpResponse response = executor.execute(request).returnResponse();
            final int sc = response.getStatusLine().getStatusCode();
            try (final UnsynchronizedByteArrayOutputStream baos = new UnsynchronizedByteArrayOutputStream()) {
                response.getEntity().writeTo(baos);
                return Tuple(sc, baos.toString(UTF_8));
            }
        });
        return responseCodeAndBody;
    }
}
