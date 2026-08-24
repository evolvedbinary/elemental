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

import com.evolvedbinary.j8fu.function.Runnable3E;
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
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.test.TestConstants;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import java.io.IOException;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.xquery.functions.securitymanager.SecurityManagerTestUtil.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, true);

    @Test
    void changeDocumentModeAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        xqueryChangeMode(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), RWXRWXRWX);
    }

    @Test
    void changeCollectionModeAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        xqueryChangeMode(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), RWXRWXRWX);
    }

    @Test
    void changeDocumentModeAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        xqueryChangeMode(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), RWXRWXRWX);
    }

    @Test
    void changeCollectionModeAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        xqueryChangeMode(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), RWXRWXRWX);
    }

    @Test
    void changeDocumentModeAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        Runnable3E x = () ->
                    xqueryChangeMode(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), RWXRWXRWX);
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDenied(x));
    }

    @Test
    void changeCollectionModeAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        Runnable3E x = () ->
                    xqueryChangeMode(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), RWXRWXRWX);
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDenied(x));
    }

    @Test
    void changeDocumentModeAsDBA_preservesSetGid(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);

        // check the setGid bit is set before we begin
        assertDocumentSetGid(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the mode
        xqueryChangeMode(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), RWXRWS__);

        // check the setGid bit still set
        assertDocumentSetGid(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);
    }

    @Test
    void changeCollectionModeAsDBA_preservesSetGid(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);

        // check the setGid bit is set before we begin
        assertCollectionSetGid(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the mode
        xqueryChangeMode(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), RWXRWS__);

        // check the setGid bit still set
        assertCollectionSetGid(pool, adminUser, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);
    }

    @Test
    void changeDocumentModeAsNonDBAOwner_preservesSetGid(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setGid bit is set before we begin
        assertDocumentSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the mode
        xqueryChangeMode(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), RWXRWS__);

        // check the setGid bit still set
        assertDocumentSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);
    }

    @Test
    void changeCollectionModeAsNonDBAOwner_preservesSetGid(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setGid bit is set before we begin
        assertCollectionSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the mode
        xqueryChangeMode(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), RWXRWS__);

        // check the setGid bit still set
        assertCollectionSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);
    }

    @Test
    void changeDocumentModeAsNonOwner_clearsSetGid(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        assertDocumentSetGid(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);
        extractPermissionDenied(() ->
                    xqueryChangeMode(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), RWXRWSRWX)
            );
        XmldbURI append = TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1);
        assertThrows(PermissionDeniedException.class, () ->

            // check the setGid bit still set
            assertDocumentSetGid(pool, user2, append, NOT_SET));
    }

    @Test
    void changeCollectionModeAsNonOwner_clearsSetGid(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        assertCollectionSetGid(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);
        extractPermissionDenied(() ->
                    xqueryChangeMode(pool, user2, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), RWXRWSRWX)
            );
        XmldbURI append = TestConstants.TEST_COLLECTION_URI.append(USER1_COL2);
        assertThrows(PermissionDeniedException.class, () ->

            // check the setGid bit still set
            assertCollectionSetGid(pool, user2, append, NOT_SET));
    }

    @BeforeAll
    static void prepareDb(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, TriggerException {
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

    @BeforeEach
    void setup(final BrokerPool pool) throws EXistException, PermissionDeniedException, LockException, SAXException, IOException, AuthenticationException {
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
            final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
            broker.storeDocument(transaction, USER1_DOC1, new StringInputSource(xml1), xmlMediaType, collection);

            final String xquery1 =
                    "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                    "sm:id()";
            final MediaType xqueryMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XQUERY);
            broker.storeDocument(transaction, USER1_XQUERY1, new StringInputSource(xquery1.getBytes(UTF_8)), xqueryMediaType, collection);
            PermissionFactory.chmod_str(broker, transaction, collection.getURI().append(USER1_XQUERY1), Optional.of("u+s,g+s"), Optional.empty());

            transaction.commit();
        }
    }

    @AfterEach
    void teardown(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            removeDocument(broker, transaction, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1));
            removeCollection(broker, transaction, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2));
            removeCollection(broker, transaction, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1));

            transaction.commit();
        }
    }

    @AfterAll
    static void cleanupDb(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, TriggerException {
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
