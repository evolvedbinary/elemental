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
package org.exist.xquery.functions.securitymanager;

import org.exist.EXistException;
import org.exist.TestUtils;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.security.*;
import org.exist.security.SecurityManager;
import org.exist.security.internal.aider.GroupAider;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.test.TestConstants;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.junit.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.xquery.functions.securitymanager.SecurityManagerTestUtil.*;

public class PermissionsFunctionChmodTest {

    private static final boolean IS_SET = true;
    private static final boolean NOT_SET = false;

    private static final String USER1_NAME = "user1";
    private static final String USER1_PWD = USER1_NAME;
    private static final String USER2_NAME = "user2";
    private static final String USER2_PWD = USER2_NAME;

    private static final XmldbURI USER1_COL1 = XmldbURI.create("u1c1");
    private static final XmldbURI USER1_COL2 = XmldbURI.create("u1c2");
    private static final XmldbURI USER1_DOC1 = XmldbURI.create("u1d1.xml");
    private static final XmldbURI USER1_XQUERY1 = XmldbURI.create("u1xq1.xq");

    private static final String OTHER_GROUP_NAME = "otherGroup";

    private static final String RWXRWXRWX = "rwxrwxrwx";
    private static final String RWXRWS__ = "rwxrws---";
    private static final String RWXRWSRWX = "rwxrwsrwx";

    @ClassRule
    public static final ExistEmbeddedServer existWebServer = new ExistEmbeddedServer(true, true);

    @Test
    public void changeDocumentModeAsDBA() throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        xqueryChangeMode(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), RWXRWXRWX);
    }

    @Test
    public void changeCollectionModeAsDBA() throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        xqueryChangeMode(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), RWXRWXRWX);
    }

    @Test
    public void changeDocumentModeAsNonDBAOwner() throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        xqueryChangeMode(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), RWXRWXRWX);
    }

    @Test
    public void changeCollectionModeAsNonDBAOwner() throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        xqueryChangeMode(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), RWXRWXRWX);
    }

    @Test(expected=PermissionDeniedException.class)
    public void changeDocumentModeAsNonOwner() throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        extractPermissionDenied(() ->
            xqueryChangeMode(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), RWXRWXRWX)
        );
    }

    @Test(expected=PermissionDeniedException.class)
    public void changeCollectionModeAsNonOwner() throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        extractPermissionDenied(() ->
            xqueryChangeMode(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), RWXRWXRWX)
        );
    }

    @Test
    public void changeDocumentModeAsDBA_preservesSetGid() throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);

        // check the setGid bit is set before we begin
        assertDocumentSetGid(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the mode
        xqueryChangeMode(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), RWXRWS__);

        // check the setGid bit still set
        assertDocumentSetGid(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);
    }

    @Test
    public void changeCollectionModeAsDBA_preservesSetGid() throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);

        // check the setGid bit is set before we begin
        assertCollectionSetGid(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the mode
        xqueryChangeMode(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), RWXRWS__);

        // check the setGid bit still set
        assertCollectionSetGid(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);
    }

    @Test
    public void changeDocumentModeAsNonDBAOwner_preservesSetGid() throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setGid bit is set before we begin
        assertDocumentSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the mode
        xqueryChangeMode(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), RWXRWS__);

        // check the setGid bit still set
        assertDocumentSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);
    }

    @Test
    public void changeCollectionModeAsNonDBAOwner_preservesSetGid() throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setGid bit is set before we begin
        assertCollectionSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the mode
        xqueryChangeMode(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), RWXRWS__);

        // check the setGid bit still set
        assertCollectionSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);
    }

    @Test(expected=PermissionDeniedException.class)
    public void changeDocumentModeAsNonOwner_clearsSetGid() throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);

        // check the setGid bit is set before we begin
        assertDocumentSetGid(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the mode
        extractPermissionDenied(() ->
            xqueryChangeMode(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), RWXRWSRWX)
        );

        // check the setGid bit still set
        assertDocumentSetGid(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), NOT_SET);
    }

    @Test(expected=PermissionDeniedException.class)
    public void changeCollectionModeAsNonOwner_clearsSetGid() throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);

        // check the setGid bit is set before we begin
        assertCollectionSetGid(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the mode
        extractPermissionDenied(() ->
            xqueryChangeMode(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), RWXRWSRWX)
        );

        // check the setGid bit still set
        assertCollectionSetGid(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), NOT_SET);
    }

    @BeforeClass
    public static void prepareDb() throws EXistException, PermissionDeniedException, IOException, TriggerException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final SecurityManager sm = pool.getSecurityManager();
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
                final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            final Collection collection = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI);
            PermissionFactory.chmod(broker, collection, Optional.of(511), Optional.empty());
            broker.saveCollection(transaction, collection);

            createUser(broker, sm, USER1_NAME, USER1_PWD);
            createUser(broker, sm, USER2_NAME, USER2_PWD);

            final Group otherGroup = new GroupAider(OTHER_GROUP_NAME);
            sm.addGroup(broker, otherGroup);
            final Account user1 = sm.getAccount(USER1_NAME);
            user1.addGroup(OTHER_GROUP_NAME);
            sm.updateAccount(user1);
            final Account user2 = sm.getAccount(USER2_NAME);
            user2.addGroup(OTHER_GROUP_NAME);
            sm.updateAccount(user2);

            transaction.commit();
        }
    }

    @Before
    public void setup() throws EXistException, PermissionDeniedException, LockException, SAXException, IOException, AuthenticationException {
        final BrokerPool pool = existWebServer.getBrokerPool();

        // create user1 resources
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        try (final DBBroker broker = pool.get(Optional.of(user1));
                final Txn transaction = pool.getTransactionManager().beginTransaction();
                final Collection collection = broker.openCollection(TestConstants.TEST_COLLECTION_URI, Lock.LockMode.WRITE_LOCK)) {

            final Collection u1c1 = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1));
            broker.saveCollection(transaction, u1c1);

            final Collection u1c2 = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2));
            PermissionFactory.chmod_str(broker, u1c2, Optional.of("u+s,g+s"), Optional.empty());
            broker.saveCollection(transaction, u1c2);

            final String xml1 = "<empty1/>";
            broker.storeDocument(transaction, USER1_DOC1, new StringInputSource(xml1), MimeType.XML_TYPE, collection);

            final String xquery1 =
                    "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                    "sm:id()";
            broker.storeDocument(transaction, USER1_XQUERY1, new StringInputSource(xquery1.getBytes(UTF_8)), MimeType.XQUERY_TYPE, collection);
            PermissionFactory.chmod_str(broker, transaction, collection.getURI().append(USER1_XQUERY1), Optional.of("u+s,g+s"), Optional.empty());

            transaction.commit();
        }
    }

    @After
    public void teardown() throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            removeDocument(broker, transaction, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1));
            removeCollection(broker, transaction, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2));
            removeCollection(broker, transaction, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1));

            transaction.commit();
        }
    }

    @AfterClass
    public static void cleanupDb() throws EXistException, PermissionDeniedException, IOException, TriggerException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final SecurityManager sm = pool.getSecurityManager();
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            removeUser(sm, USER2_NAME);
            removeUser(sm, USER1_NAME);

            removeCollection(broker, transaction, TestConstants.TEST_COLLECTION_URI);

            transaction.commit();
        }
    }
}
