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
package org.exist.xquery.functions.xmldb;

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.exist.TestUtils;
import org.exist.security.internal.aider.GroupAider;
import org.exist.security.internal.aider.UserAider;
import org.exist.xmldb.UserManagementService;
import org.junit.Before;
import org.junit.Test;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class XMLDBAuthenticateTest extends AbstractXMLDBTest{

    private static final String USER1_UID = "user1";
    private static final String USER1_PWD = "user1";

    @Before
    public void beforeClass() throws XMLDBException {
        final Collection root = DatabaseManager.getCollection("xmldb:exist://localhost:" + existWebServer.getPort() + "/xmlrpc/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        final UserManagementService ums = root.getService(UserManagementService.class);

        final GroupAider group1 = new GroupAider(USER1_UID);
        ums.addGroup(group1);

        final UserAider user1 = new UserAider(USER1_UID, group1);
        user1.setPassword(USER1_PWD);
        ums.addAccount(user1);
    }

    @Test
    public void loginExplicitSessionCreation() throws IOException {
        // explicitly create a new session
        final Request requestCreateSession = xqueryRequest("session:create()");
        final HttpResponse createSessionResponse = requestCreateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, createSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(createSessionResponse.getEntity()));

        // login to the database
        final Request requestGetAttr = xqueryRequest("xmldb:login('/db', '" + USER1_UID + "', '" + USER1_PWD + "')");
        final HttpResponse getResponse1 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse1.getStatusLine().getStatusCode());
        assertEquals("true", readEntityAsString(getResponse1.getEntity()));

        // get the identity of the current user
        final Request requestSetAttr1 = xqueryRequest("sm:id()");
        final HttpResponse setResponse1 = requestSetAttr1
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, setResponse1.getStatusLine().getStatusCode());

        final Source expected = Input.fromString(
                "<sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                "    <sm:real>\n" +
                "        <sm:username>user1</sm:username>\n" +
                "        <sm:groups>\n" +
                "            <sm:group>user1</sm:group>\n" +
                "        </sm:groups>\n" +
                "    </sm:real>\n" +
                "</sm:id>").build();
        final Source actual = Input.fromString(readEntityAsString(setResponse1.getEntity())).build();
        final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();
        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void loginImplicitSessionCreateSessionFalse() throws IOException {
        // login to the database
        final Request requestGetAttr = xqueryRequest("xmldb:login('/db', '" + USER1_UID + "', '" + USER1_PWD + "', false())");
        final HttpResponse getResponse1 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse1.getStatusLine().getStatusCode());
        assertEquals("true", readEntityAsString(getResponse1.getEntity()));

        // get the identity of the current user
        final Request requestSetAttr1 = xqueryRequest("sm:id()");
        final HttpResponse setResponse1 = requestSetAttr1
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, setResponse1.getStatusLine().getStatusCode());

        final Source expected = Input.fromString(
                "<sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "    <sm:real>\n" +
                        "        <sm:username>guest</sm:username>\n" +
                        "        <sm:groups>\n" +
                        "            <sm:group>guest</sm:group>\n" +
                        "        </sm:groups>\n" +
                        "    </sm:real>\n" +
                        "</sm:id>").build();
        final Source actual = Input.fromString(readEntityAsString(setResponse1.getEntity())).build();
        final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();
        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void loginImplicitSessionCreateSessionTrue() throws IOException {
        // login to the database
        final Request requestGetAttr = xqueryRequest("xmldb:login('/db', '" + USER1_UID + "', '" + USER1_PWD + "', true())");
        final HttpResponse getResponse1 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse1.getStatusLine().getStatusCode());
        assertEquals("true", readEntityAsString(getResponse1.getEntity()));

        // get the identity of the current user
        final Request requestSetAttr1 = xqueryRequest("sm:id()");
        final HttpResponse setResponse1 = requestSetAttr1
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, setResponse1.getStatusLine().getStatusCode());

        final Source expected = Input.fromString(
                "<sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "    <sm:real>\n" +
                        "        <sm:username>user1</sm:username>\n" +
                        "        <sm:groups>\n" +
                        "            <sm:group>user1</sm:group>\n" +
                        "        </sm:groups>\n" +
                        "    </sm:real>\n" +
                        "</sm:id>").build();
        final Source actual = Input.fromString(readEntityAsString(setResponse1.getEntity())).build();
        final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();
        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void loginOnInvalidatedSessionCreateSessionFalseSeparateHttpCalls() throws IOException {
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

        // login to the database
        final Request requestGetAttr = xqueryRequest("xmldb:login('/db', '" + USER1_UID + "', '" + USER1_PWD + "', false())");
        final HttpResponse getResponse1 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse1.getStatusLine().getStatusCode());
        assertEquals("true", readEntityAsString(getResponse1.getEntity()));

        // get the identity of the current user
        final Request requestSetAttr1 = xqueryRequest("sm:id()");
        final HttpResponse setResponse1 = requestSetAttr1
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, setResponse1.getStatusLine().getStatusCode());

        final Source expected = Input.fromString(
                "<sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "    <sm:real>\n" +
                        "        <sm:username>guest</sm:username>\n" +
                        "        <sm:groups>\n" +
                        "            <sm:group>guest</sm:group>\n" +
                        "        </sm:groups>\n" +
                        "    </sm:real>\n" +
                        "</sm:id>").build();
        final Source actual = Input.fromString(readEntityAsString(setResponse1.getEntity())).build();
        final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();
        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void loginOnInvalidatedSessionCreateSessionTrueSeparateHttpCalls() throws IOException {
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

        // login to the database
        final Request requestGetAttr = xqueryRequest("xmldb:login('/db', '" + USER1_UID + "', '" + USER1_PWD + "', true())");
        final HttpResponse getResponse1 = requestGetAttr
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, getResponse1.getStatusLine().getStatusCode());
        assertEquals("true", readEntityAsString(getResponse1.getEntity()));

        // get the identity of the current user
        final Request requestSetAttr1 = xqueryRequest("sm:id()");
        final HttpResponse setResponse1 = requestSetAttr1
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, setResponse1.getStatusLine().getStatusCode());

        final Source expected = Input.fromString(
                "<sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "    <sm:real>\n" +
                        "        <sm:username>user1</sm:username>\n" +
                        "        <sm:groups>\n" +
                        "            <sm:group>user1</sm:group>\n" +
                        "        </sm:groups>\n" +
                        "    </sm:real>\n" +
                        "</sm:id>").build();
        final Source actual = Input.fromString(readEntityAsString(setResponse1.getEntity())).build();
        final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();
        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void loginOnInvalidatedSessionCreateSessionFalseSameHttpCall() throws IOException {
        // explicitly create a new session
        final Request requestCreateSession = xqueryRequest("session:create()");
        final HttpResponse createSessionResponse = requestCreateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, createSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(createSessionResponse.getEntity()));

        // invalidate the session and login to the database
        final Request requestInvalidateSession = xqueryRequest("session:invalidate(), xmldb:login('/db', '" + USER1_UID + "', '" + USER1_PWD + "', false())");
        final HttpResponse invalidateSessionResponse = requestInvalidateSession
                .execute()
                .returnResponse();
        final String responseBody = readEntityAsString(invalidateSessionResponse.getEntity());
        assertEquals(responseBody, HttpStatus.SC_OK, invalidateSessionResponse.getStatusLine().getStatusCode());
        assertEquals("true", responseBody);

        // get the identity of the current user
        final Request requestSetAttr1 = xqueryRequest("sm:id()");
        final HttpResponse setResponse1 = requestSetAttr1
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, setResponse1.getStatusLine().getStatusCode());

        final Source expected = Input.fromString(
                "<sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "    <sm:real>\n" +
                        "        <sm:username>guest</sm:username>\n" +
                        "        <sm:groups>\n" +
                        "            <sm:group>guest</sm:group>\n" +
                        "        </sm:groups>\n" +
                        "    </sm:real>\n" +
                        "</sm:id>").build();
        final Source actual = Input.fromString(readEntityAsString(setResponse1.getEntity())).build();
        final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();
        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void loginOnInvalidatedSessionCreateSessionTrueSameHttpCall() throws IOException {
        // explicitly create a new session
        final Request requestCreateSession = xqueryRequest("session:create()");
        final HttpResponse createSessionResponse = requestCreateSession
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, createSessionResponse.getStatusLine().getStatusCode());
        assertEquals("", readEntityAsString(createSessionResponse.getEntity()));

        // invalidate the session and login to the database
        final Request requestInvalidateSession = xqueryRequest("session:invalidate(), xmldb:login('/db', '" + USER1_UID + "', '" + USER1_PWD + "', true())");
        final HttpResponse invalidateSessionResponse = requestInvalidateSession
                .execute()
                .returnResponse();
        final String responseBody = readEntityAsString(invalidateSessionResponse.getEntity());
        assertEquals(responseBody, HttpStatus.SC_OK, invalidateSessionResponse.getStatusLine().getStatusCode());
        assertEquals("true", responseBody);

        // get the identity of the current user
        final Request requestSetAttr1 = xqueryRequest("sm:id()");
        final HttpResponse setResponse1 = requestSetAttr1
                .execute()
                .returnResponse();
        assertEquals(HttpStatus.SC_OK, setResponse1.getStatusLine().getStatusCode());

        final Source expected = Input.fromString(
                "<sm:id xmlns:sm=\"http://exist-db.org/xquery/securitymanager\">\n" +
                        "    <sm:real>\n" +
                        "        <sm:username>user1</sm:username>\n" +
                        "        <sm:groups>\n" +
                        "            <sm:group>user1</sm:group>\n" +
                        "        </sm:groups>\n" +
                        "    </sm:real>\n" +
                        "</sm:id>").build();
        final Source actual = Input.fromString(readEntityAsString(setResponse1.getEntity())).build();
        final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();
        assertFalse(diff.toString(), diff.hasDifferences());
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
