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
package org.exist.xquery.functions.session;

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.exist.util.UUIDGenerator;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class AttributeTest extends AbstractSessionTest {

    @Test
    public void getSetAttributeExplicitSessionCreation() throws IOException {
        // explicitly create a new session
        final Request requestCreateSession = xqueryRequest("session:create()");
        final HttpResponse createSessionResponse = requestCreateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, createSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(createSessionResponse.getEntity()));

        // get the value of the attribute named "attr1", and check its value is the empty sequence
        final Request requestGetAttr = xqueryRequest("session:get-attribute('attr1')");
        final HttpResponse getResponse1 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse1.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(getResponse1.getEntity()));

        // set the value of the attribute named "attr1" to a random UUID
        final String attr1Value = UUIDGenerator.getUUIDversion4();
        final Request requestSetAttr1 = xqueryRequest("session:set-attribute('attr1', '" + attr1Value + "')");
        final HttpResponse setResponse1 = requestSetAttr1
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, setResponse1.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(setResponse1.getEntity()));

        // get the value of the attribute named "attr1", and check its value is the UUID
        final HttpResponse getResponse2 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse2.getStatusLine().getStatusCode());
        assertEquals(attr1Value, readEntityAsString(getResponse2.getEntity()));
    }

    @Test
    public void getSetAttributeImplicitSessionCreation() throws IOException {
        // get the value of the attribute named "attr1", and check its value is the empty sequence
        final Request requestGetAttr = xqueryRequest("session:get-attribute('attr1')");
        final HttpResponse getResponse1 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse1.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(getResponse1.getEntity()));

        // set the value of the attribute named "attr1" to a random UUID
        final String attr1Value = UUIDGenerator.getUUIDversion4();
        final Request requestSetAttr1 = xqueryRequest("session:set-attribute('attr1', '" + attr1Value + "')");
        final HttpResponse setResponse1 = requestSetAttr1
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, setResponse1.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(setResponse1.getEntity()));

        // get the value of the attribute named "attr1", and check its value is the UUID
        final HttpResponse getResponse2 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse2.getStatusLine().getStatusCode());
        assertEquals(attr1Value, readEntityAsString(getResponse2.getEntity()));
    }

    @Test
    public void getAttributeOnInvalidatedSessionSeparateHttpCalls() throws IOException {
        // explicitly create a new session
        final Request requestCreateSession = xqueryRequest("session:create()");
        final HttpResponse createSessionResponse = requestCreateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, createSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(createSessionResponse.getEntity()));

        // invalidate the session
        final Request requestInvalidateSession = xqueryRequest("session:invalidate()");
        final HttpResponse invalidateSessionResponse = requestInvalidateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, invalidateSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(invalidateSessionResponse.getEntity()));

        // get the value of the attribute named "attr1", and check its value is the empty sequence
        final Request requestGetAttr1 = xqueryRequest("session:get-attribute('attr1')");
        final HttpResponse getResponse1 = requestGetAttr1
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse1.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(getResponse1.getEntity()));
    }

    @Test
    public void getAttributeOnInvalidatedSessionSameHttpCall() throws IOException {
        // explicitly create a new session
        final Request requestCreateSession = xqueryRequest("session:create()");
        final HttpResponse createSessionResponse = requestCreateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, createSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(createSessionResponse.getEntity()));

        // invalidate the session and call get-attribute
        final Request requestInvalidateSession = xqueryRequest("session:invalidate(), session:get-attribute('attr1')");
        final HttpResponse invalidateSessionResponse = requestInvalidateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, invalidateSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(invalidateSessionResponse.getEntity()));
    }

    @Test
    public void setAttributeOnInvalidatedSessionSeparateHttpCalls() throws IOException {
        // explicitly create a new session
        final Request requestCreateSession = xqueryRequest("session:create()");
        final HttpResponse createSessionResponse = requestCreateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, createSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(createSessionResponse.getEntity()));

        // invalidate the session
        final Request requestInvalidateSession = xqueryRequest("session:invalidate()");
        final HttpResponse invalidateSessionResponse = requestInvalidateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, invalidateSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(invalidateSessionResponse.getEntity()));

        // set the value of the attribute named "attr1" to a random UUID
        final String attr1Value = UUIDGenerator.getUUIDversion4();
        final Request requestSetAttr1 = xqueryRequest("session:set-attribute('attr1', '" + attr1Value + "')");
        final HttpResponse setResponse1 = requestSetAttr1
                .execute()
                .returnResponse();
        final String responseBody = readEntityAsString(setResponse1.getEntity());
        assertEquals(HttpStatus.SC_OK, setResponse1.getStatusLine().getStatusCode());
        assertEquals("", responseBody);

        // get the value of the attribute named "attr1", and check its value is the UUID
        final Request requestGetAttr = xqueryRequest("session:get-attribute('attr1')");
        final HttpResponse getResponse2 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse2.getStatusLine().getStatusCode());
        assertEquals(attr1Value, readEntityAsString(getResponse2.getEntity()));
    }

    @Test
    public void setAttributeOnInvalidatedSessionSameHttpCall() throws IOException {
        // explicitly create a new session
        final Request requestCreateSession = xqueryRequest("session:create()");
        final HttpResponse createSessionResponse = requestCreateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, createSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(createSessionResponse.getEntity()));

        // invalidate the session and call set-attribute
        final String attr1Value = UUIDGenerator.getUUIDversion4();
        final Request requestInvalidateSession = xqueryRequest("session:invalidate(), session:set-attribute('attr1', '" + attr1Value + "')");
        final HttpResponse invalidateSessionResponse = requestInvalidateSession
                .execute()
                .returnResponse();
        final String responseBody = readEntityAsString(invalidateSessionResponse.getEntity());
        assertEquals(HttpStatus.SC_OK, invalidateSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", responseBody);

        // get the value of the attribute named "attr1", and check its value is the UUID
        final Request requestGetAttr = xqueryRequest("session:get-attribute('attr1')");
        final HttpResponse getResponse2 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse2.getStatusLine().getStatusCode());
        assertEquals(attr1Value, readEntityAsString(getResponse2.getEntity()));
    }

    public Request xqueryRequest(final String xquery) throws UnsupportedEncodingException {
        return Request.Get(getCollectionRootUri() + "/?_query=" + URLEncoder.encode(xquery, UTF_8.name()) + "&_wrap=no");
    }

    private static String readEntityAsString(final HttpEntity entity) throws IOException {
        return new String(readEntity(entity), UTF_8);
    }

    private static byte[] readEntity(final HttpEntity entity) throws IOException {
        try (final UnsynchronizedByteArrayOutputStream os = new UnsynchronizedByteArrayOutputStream()) {
            entity.writeTo(os);
            return os.toByteArray();
        }
    }
}
