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
package org.exist.xquery.modules.persistentlogin;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.exist.TestUtils;
import org.exist.test.ExistWebServer;
import org.exist.test.TestConstants;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.UserManagementService;
import org.exist.xmldb.XmldbURI;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import xyz.elemental.mediatype.MediaType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class LoginModuleTest {

    @Parameters
    public static Iterable<? extends Object> loginDomains() {
        return Arrays.asList("xyz.elemental.login", "org.exist.login");
    }

    @ClassRule
    public static final ExistWebServer EXIST_WEB_SERVER = new ExistWebServer(true, false, true);

    private final static String XQUERY_FILENAME = "test-login.xql";

    private Collection root;
    private HttpClient client;

    @Parameter
    public String loginDomain;

    @Before
    public void setup() throws XMLDBException {
        final String xquery = "import module namespace login=\"http://exist-db.org/xquery/login\" " +
            "at \"resource:org/exist/xquery/modules/persistentlogin/login.xql\";" +
            "login:set-user('" + loginDomain + "', (), false())," +
            "sm:id()/(descendant::sm:effective,descendant::sm:real)[1]/sm:username/string()";

        root = DatabaseManager.getCollection("xmldb:exist://localhost:" + EXIST_WEB_SERVER.getPort() + "/xmlrpc" + XmldbURI.ROOT_COLLECTION, TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        final BinaryResource res = (BinaryResource)root.createResource(XQUERY_FILENAME, "BinaryResource");
        ((EXistResource) res).setMediaType(MediaType.APPLICATION_XQUERY);
        res.setContent(xquery);
        root.storeResource(res);
        final UserManagementService ums = (UserManagementService)root.getService("UserManagementService", "1.0");
        ums.chmod(res, 0777);

        final BasicCookieStore store = new BasicCookieStore();
        client = HttpClientBuilder.create().setDefaultCookieStore(store).build();
    }

    @After
    public void cleanup() throws XMLDBException {
        final BinaryResource res = (BinaryResource)root.getResource(XQUERY_FILENAME);
        root.removeResource(res);
    }

    @Test
    public void loginAndLogout() throws IOException {
        // not logged in
        doGet(null, TestUtils.GUEST_DB_USER);

        // log in as admin
        doGet("user=" + TestUtils.ADMIN_DB_USER + "&password=" + TestUtils.ADMIN_DB_PWD + "&duration=P1D", TestUtils.ADMIN_DB_USER);

        // second request should stay logged in
        doGet(null, TestUtils.ADMIN_DB_USER);

        // log off returns to guest user
        doGet("logout=true", TestUtils.GUEST_DB_USER);
    }

    private void doGet(@Nullable final String params, final String expected) throws IOException {
        final HttpGet httpGet = new HttpGet("http://localhost:" + EXIST_WEB_SERVER.getPort() + "/rest" + XmldbURI.ROOT_COLLECTION + '/' + XQUERY_FILENAME +
                (params == null ? "" : "?" + params));
        final HttpResponse response = client.execute(httpGet);
        final HttpEntity entity = response.getEntity();
        final String responseBody = EntityUtils.toString(entity);
        assertEquals(responseBody, SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(expected, responseBody);
    }
}
