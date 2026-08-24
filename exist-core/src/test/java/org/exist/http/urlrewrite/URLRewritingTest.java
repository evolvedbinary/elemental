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
package org.exist.http.urlrewrite;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.exist.http.AbstractHttpTest;
import org.exist.test.DatabaseWebServerExtension;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xyz.elemental.mediatype.MediaType;

import java.io.IOException;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.http.urlrewrite.XQueryURLRewrite.XQUERY_CONTROLLER_FILENAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class URLRewritingTest extends AbstractHttpTest {

    private static final XmldbURI TEST_COLLECTION_NAME = XmldbURI.create("controller-test");
    private static final XmldbURI TEST_COLLECTION = XmldbURI.create("/db/apps").append(TEST_COLLECTION_NAME);

    private static final String TEST_CONTROLLER = "xquery version \"3.1\";\n<controller>{fn:current-dateTime()}</controller>";

    @RegisterExtension
    public static final DatabaseWebServerExtension DATABASE_WEB_SERVER = new DatabaseWebServerExtension(true, false, true, true, false);

    @Test
    void findsParentController() throws IOException {
        final XmldbURI nestedCollectionName = XmldbURI.create("nested");
        final XmldbURI docName = XmldbURI.create("test.xml");
        final String testDocument = "<hello>world</hello>";

        final String storeDocUri = getRestUri(DATABASE_WEB_SERVER) + TEST_COLLECTION.append(nestedCollectionName).append(docName);
        final Request storeRequest = Request
                .Put(storeDocUri)
                .bodyString(testDocument, ContentType.APPLICATION_XML);
        final int storeResponseStatusCode = withHttpExecutor(DATABASE_WEB_SERVER, executor -> executor.execute(storeRequest).returnResponse().getStatusLine().getStatusCode());
        assertEquals(HttpStatus.SC_CREATED, storeResponseStatusCode);

        final String retrieveDocUri = getAppsUri(DATABASE_WEB_SERVER) + "/" + TEST_COLLECTION_NAME.append(nestedCollectionName).append(docName);
        final Request retrieveRequest = Request
                .Get(retrieveDocUri);
        final Tuple2<Integer, String> retrieveResponseStatusCodeAndBody = withHttpExecutor(DATABASE_WEB_SERVER,  executor -> {
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

    @BeforeAll
    static void setup() throws IOException {
        final Request request = Request
                .Put(getRestUri(DATABASE_WEB_SERVER) + TEST_COLLECTION + "/" + XQUERY_CONTROLLER_FILENAME)
                .bodyString(TEST_CONTROLLER, ContentType.create(MediaType.APPLICATION_XQUERY));

        final int statusCode = withHttpExecutor(DATABASE_WEB_SERVER, executor ->
                executor.execute(request).returnResponse().getStatusLine().getStatusCode()
        );

        assertEquals(HttpStatus.SC_CREATED, statusCode);
    }

    @AfterAll
    static void cleanup() throws IOException {
        final Request request = Request
                .Delete(getRestUri(DATABASE_WEB_SERVER) + TEST_COLLECTION);

        final int statusCode = withHttpExecutor(DATABASE_WEB_SERVER, executor ->
                executor.execute(request).returnResponse().getStatusLine().getStatusCode()
        );

        assertEquals(HttpStatus.SC_OK, statusCode);
    }
}
