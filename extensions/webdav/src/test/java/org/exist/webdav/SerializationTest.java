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
package org.exist.webdav;

import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.NotFoundException;
import com.ettrema.httpclient.*;
import org.apache.http.impl.client.AbstractHttpClient;
import org.exist.TestUtils;
import org.exist.test.ExistWebServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class SerializationTest {

    private static final String XML_WITH_DOCTYPE =
            "<!DOCTYPE bookmap PUBLIC \"-//OASIS//DTD DITA BookMap//EN\" \"bookmap.dtd\">\n" +
            "<bookmap id=\"bookmap-1\"/>";

    private static final String XML_WITH_XMLDECL =
            "<?xml version=\"1.1\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n" +
            "<bookmap id=\"bookmap-2\"/>";

    private static String PREV_PROPFIND_METHOD_XML_SIZE = null;

    @ClassRule
    public static final ExistWebServer EXIST_WEB_SERVER = new ExistWebServer(true, false, true, true);

    @ClassRule
    public static final TemporaryFolder TEMP_FOLDER = new TemporaryFolder();

    @BeforeClass
    public static void setup() {
        PREV_PROPFIND_METHOD_XML_SIZE = System.setProperty(MiltonDocument.PROPFIND_METHOD_XML_SIZE, "exact");
    }

    @AfterClass
    public static void cleanup() {
        if (PREV_PROPFIND_METHOD_XML_SIZE == null) {
            System.clearProperty(MiltonDocument.PROPFIND_METHOD_XML_SIZE);
        } else {
            System.setProperty(MiltonDocument.PROPFIND_METHOD_XML_SIZE, PREV_PROPFIND_METHOD_XML_SIZE);
        }
    }

    @Test
    public void getDocTypeDefault() throws IOException, NotAuthorizedException, BadRequestException, HttpException, ConflictException, NotFoundException {
        final String docName = "test-with-doctype.xml";
        final HostBuilder builder = new HostBuilder();
        builder.setServer("localhost");
        final int port = EXIST_WEB_SERVER.getPort();
        builder.setPort(port);
        builder.setRootPath("webdav/db");
        final Host host = builder.buildHost();

        // workaround pre-emptive auth issues of Milton Client
        try (final AbstractHttpClient httpClient = (AbstractHttpClient)host.getClient()) {
            httpClient.addRequestInterceptor(new AlwaysBasicPreAuth(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD));

            final Folder folder = host.getFolder("/");
            assertNotNull(folder);

            // store document
            final byte data[] = XML_WITH_DOCTYPE.getBytes(UTF_8);
            final java.io.File tmpStoreFile = TEMP_FOLDER.newFile();
            Files.write(tmpStoreFile.toPath(), data);
            assertNotNull(folder.uploadFile(docName, tmpStoreFile, null));

            // retrieve document
            final Resource resource = folder.child(docName);
            assertNotNull(resource);
            assertTrue(resource instanceof File);
            assertEquals("application/xml", ((File) resource).contentType);
            final java.io.File tempRetrieveFile = TEMP_FOLDER.newFile();
            resource.downloadTo(tempRetrieveFile, null);
            assertEquals(XML_WITH_DOCTYPE, new String(Files.readAllBytes(tempRetrieveFile.toPath()), UTF_8));
        }
    }

    @Test
    public void getXmlDeclDefault() throws IOException, NotAuthorizedException, BadRequestException, HttpException, ConflictException, NotFoundException {
        final String docName = "test-with-xmldecl.xml";
        final HostBuilder builder = new HostBuilder();
        builder.setServer("localhost");
        final int port = EXIST_WEB_SERVER.getPort();
        builder.setPort(port);
        builder.setRootPath("webdav/db");
        final Host host = builder.buildHost();

        // workaround pre-emptive auth issues of Milton Client
        try (final AbstractHttpClient httpClient = (AbstractHttpClient)host.getClient()) {
            httpClient.addRequestInterceptor(new AlwaysBasicPreAuth(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD));

            final Folder folder = host.getFolder("/");
            assertNotNull(folder);

            // store document
            final byte data[] = XML_WITH_XMLDECL.getBytes(UTF_8);
            final java.io.File tmpStoreFile = TEMP_FOLDER.newFile();
            Files.write(tmpStoreFile.toPath(), data);
            assertNotNull(folder.uploadFile(docName, tmpStoreFile, null));

            // retrieve document
            final Resource resource = folder.child(docName);
            assertNotNull(resource);
            assertTrue(resource instanceof File);
            assertEquals("application/xml", ((File) resource).contentType);
            final java.io.File tempRetrieveFile = TEMP_FOLDER.newFile();
            resource.downloadTo(tempRetrieveFile, null);
            assertEquals(XML_WITH_XMLDECL, new String(Files.readAllBytes(tempRetrieveFile.toPath()), UTF_8));
        }
    }
}
