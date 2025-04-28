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
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.exist.http.AbstractHttpTest;
import org.exist.test.ExistWebServer;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.exist.xmldb.XmldbURI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.http.urlrewrite.XQueryURLRewrite.XQUERY_CONTROLLER_FILENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class URLRewritingTest extends AbstractHttpTest {

    private static final XmldbURI TEST_COLLECTION_NAME = XmldbURI.create("controller-test");
    private static final XmldbURI TEST_COLLECTION = XmldbURI.create("/db/apps").append(TEST_COLLECTION_NAME);

    private static final String TEST_CONTROLLER = "xquery version \"3.1\";\n<controller>{fn:current-dateTime()}</controller>";

    @ClassRule
    public static final ExistWebServer existWebServer = new ExistWebServer(true, false, true, true, false);

    @Test
    public void findsParentController() throws IOException {
        final XmldbURI nestedCollectionName = XmldbURI.create("nested");
        final XmldbURI docName = XmldbURI.create("test.xml");
        final String testDocument = "<hello>world</hello>";

        final String storeDocUri = getRestUri(existWebServer) + TEST_COLLECTION.append(nestedCollectionName).append(docName);
        final Request storeRequest = Request
                .Put(storeDocUri)
                .bodyString(testDocument, ContentType.APPLICATION_XML);
        final int storeResponseStatusCode = withHttpExecutor(existWebServer, executor -> executor.execute(storeRequest).returnResponse().getStatusLine().getStatusCode());
        assertEquals(HttpStatus.SC_CREATED, storeResponseStatusCode);

        final String retrieveDocUri = getAppsUri(existWebServer) + "/" + TEST_COLLECTION_NAME.append(nestedCollectionName).append(docName);
        final Request retrieveRequest = Request
                .Get(retrieveDocUri);
        final Tuple2<Integer, String> retrieveResponseStatusCodeAndBody = withHttpExecutor(existWebServer,  executor -> {
            final HttpResponse response = executor.execute(retrieveRequest).returnResponse();
            final String responseBody;
            try (final UnsynchronizedByteArrayOutputStream baos = new UnsynchronizedByteArrayOutputStream((int)response.getEntity().getContentLength())) {
                response.getEntity().writeTo(baos);
                responseBody = baos.toString(UTF_8);
            }
            return Tuple(response.getStatusLine().getStatusCode(), responseBody);
        });
        assertEquals(HttpStatus.SC_OK, retrieveResponseStatusCodeAndBody._1.intValue());
        assertTrue(retrieveResponseStatusCodeAndBody._2.matches("<controller>.+</controller>"));
    }

    @BeforeClass
    public static void setup() throws IOException {
        final Request request = Request
                .Put(getRestUri(existWebServer) + TEST_COLLECTION + "/" + XQUERY_CONTROLLER_FILENAME)
                .bodyString(TEST_CONTROLLER, ContentType.create("application/xquery"));

        final int statusCode = withHttpExecutor(existWebServer, executor ->
                executor.execute(request).returnResponse().getStatusLine().getStatusCode()
        );

        assertEquals(HttpStatus.SC_CREATED, statusCode);
    }

    @AfterClass
    public static void cleanup() throws IOException {
        final Request request = Request
                .Delete(getRestUri(existWebServer) + TEST_COLLECTION);

        final int statusCode = withHttpExecutor(existWebServer, executor ->
                executor.execute(request).returnResponse().getStatusLine().getStatusCode()
        );

        assertEquals(HttpStatus.SC_OK, statusCode);
    }
}
