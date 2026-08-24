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
import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.EXistException;
import org.exist.TestUtils;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.*;
import org.exist.security.SecurityManager;
import org.exist.security.internal.aider.GroupAider;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.test.TestConstants;
import org.exist.util.Configuration;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;
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

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.xquery.functions.securitymanager.SecurityManagerTestUtil.*;
import static org.junit.jupiter.api.Assertions.*;

public class PermissionsFunctionChownTest {

    private static final boolean RESTRICTED = true;
    private static final boolean NOT_RESTRICTED = false;

    private static final boolean IS_SET = true;
    private static final boolean NOT_SET = false;

    private static final String USER1_NAME = "user1";
    private static final String USER1_PWD = USER1_NAME;
    private static final String USER2_NAME = "user2";
    private static final String USER2_PWD = USER2_NAME;
    private static final String USERRM_NAME = "userrm";
    private static final String USERRM_PWD = USERRM_NAME;

    private static final XmldbURI USER1_COL1 = XmldbURI.create("u1c1");
    private static final XmldbURI USER1_COL2 = XmldbURI.create("u1c2");
    private static final XmldbURI USER1_DOC1 = XmldbURI.create("u1d1.xml");
    private static final XmldbURI USER1_XQUERY1 = XmldbURI.create("u1xq1.xq");

    private static final String OTHER_GROUP_NAME = "otherGroup";

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, true);

    /**
     * With {@code posix-chown-restricted="true"},
     * as a DBA user change the owner of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentOwnerToSelfAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as a DBA user change the owner of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentOwnerToSelfAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as a DBA user change the owner of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionOwnerToSelfAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as a DBA user change the owner of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionOwnerToSelfAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the owner of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentOwnerToSelfAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the document owner user change the owner of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentOwnerToSelfAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the owner of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionOwnerToSelfAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the collection owner user change the owner of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionOwnerToSelfAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the user "user2" (not the document's owner) change the owner of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentOwnerToSelfAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the user "user2" (not the document's owner) change the owner of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentOwnerToSelfAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the user "user2" (not the collection's owner) change the owner of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionOwnerToSelfAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the user "user2" (not the collection's owner) change the owner of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionOwnerToSelfAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as a DBA user change the owner of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentOwnerAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as a DBA user change the owner of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentOwnerAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as a DBA user change the owner of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionOwnerAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as a DBA user change the owner of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionOwnerAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the owner of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentOwnerAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
                changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the document owner user change the owner of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentOwnerAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the owner of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionOwnerAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
                changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the collection owner user change the owner of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionOwnerAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the user "user2" (not the document's owner) change the owner of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentOwnerAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the user "user2" (not the document's owner) change the owner of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentOwnerAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the user "user2" (not the collection's owner) change the owner of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionOwnerAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the user "user2" (not the collection's owner) change the owner of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionOwnerAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the owner of {@link #USER1_DOC1} from "user1" to "user1".
     * Finally make sure that chown has cleared the setUid and setGid bits.
     */
    @Test
    void changeDocumentOwnerToSelfAsNonDBAOwner_clearsSetUidAndSetGid_restricted(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setUid and setGid bits are set before we begin
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the owner
        changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), USER1_NAME);

        // check the setUid and setGid bits are now cleared
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), NOT_SET);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the document owner user change the owner of {@link #USER1_DOC1} from "user1" to "user1".
     * Finally make sure that chown has cleared the setUid and setGid bits.
     */
    @Test
    void changeDocumentOwnerToSelfAsNonDBAOwner_clearsSetUidAndSetGid(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setUid and setGid bits are set before we begin
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the owner
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), USER1_NAME);

        // check the setUid and setGid bits are now cleared
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), NOT_SET);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the owner of {@link #USER1_COL2} from "user1" to "user1".
     * Finally make sure that chown has cleared the setUid and setGid bits.
     */
    @Test
    void changeCollectionOwnerToSelfAsNonDBAOwner_clearsSetUidAndSetGid_restricted(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setUid and setGid bits are set before we begin
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the owner
        changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), USER1_NAME);

        // check the setUid and setGid bits are now cleared
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), NOT_SET);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the collection owner user change the owner of {@link #USER1_COL2} from "user1" to "user1".
     * Finally make sure that chown has cleared the setUid and setGid bits.
     */
    @Test
    void changeCollectionOwnerToSelfAsNonDBAOwner_clearsSetUidAndSetGid(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setUid and setGid bits are set before we begin
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the owner
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), USER1_NAME);

        // check the setUid and setGid bits are now cleared
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), NOT_SET);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the DBA user change the owner of {@link #USER1_DOC1} from "user1" to "user1".
     * Finally make sure that chown has preserved the setUid and setGid bits.
     */
    @Test
    void changeDocumentOwnerToSelfAsDBA_preservesSetUidAndSetGid_restricted(final BrokerPool pool) throws EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().getSystemSubject();

        // check the setUid and setGid bits are set before we begin
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the owner
        changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), USER1_NAME);

        // check the setUid and setGid bits are still set
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the DBA user change the owner of {@link #USER1_DOC1} from "user1" to "user1".
     * Finally make sure that chown has preserved the setUid and setGid bits.
     */
    @Test
    void changeDocumentOwnerToSelfAsDBA_preservesSetUidAndSetGid(final BrokerPool pool) throws EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().getSystemSubject();

        // check the setUid and setGid bits are set before we begin
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the owner
        changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), USER1_NAME);

        // check the setUid and setGid bits are still set
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the DBA user change the owner of {@link #USER1_COL2} from "user1" to "user1".
     * Finally make sure that chown has preserved the setUid and setGid bits.
     */
    @Test
    void changeCollectionOwnerToSelfAsDBA_preservesSetUidAndSetGid_restricted(final BrokerPool pool) throws EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().getSystemSubject();

        // check the setUid and setGid bits are set before we begin
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the owner
        changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), USER1_NAME);

        // check the setUid and setGid bits are still set
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the DBA user change the owner of {@link #USER1_COL2} from "user1" to "user1".
     * Finally make sure that chown has preserved the setUid and setGid bits.
     */
    @Test
    void changeCollectionOwnerToSelfAsDBA_preservesSetUidAndSetGid(final BrokerPool pool) throws EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().getSystemSubject();

        // check the setUid and setGid bits are set before we begin
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the owner
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), USER1_NAME);

        // check the setUid and setGid bits are still set
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as a DBA user change the group of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentGroupToSelfAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as a DBA user change the group of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentGroupToSelfAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as a DBA user change the group of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionGroupToSelfAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as a DBA user change the group of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionGroupToSelfAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the group of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentGroupToSelfAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the document owner user change the group of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentGroupToSelfAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the group of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionGroupToSelfAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the collection owner user change the group of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionGroupToSelfAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the user "user2" (not the document's owner) change the group of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentGroupToSelfAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        changeGroup(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the user "user2" (not the document's owner) change the group of {@link #USER1_DOC1} from "user1" to "user1".
     */
    @Test
    void changeDocumentGroupToSelfAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        changeGroup(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the user "user2" (not the collection's owner) change the group of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionGroupToSelfAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        changeGroup(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the user "user2" (not the collection's owner) change the group of {@link #USER1_COL1} from "user1" to "user1".
     */
    @Test
    void changeCollectionGroupToSelfAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        changeGroup(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as a DBA user change the group of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentGroupAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as a DBA user change the group of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentGroupAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as a DBA user change the group of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionGroupAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as a DBA user change the group of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionGroupAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the group of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentGroupAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
                changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the document owner user change the group of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentGroupAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the group of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionGroupAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
                changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the collection owner user change the group of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionGroupAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the user "user2" (not the document's owner) change the group of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentGroupAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the user "user2" (not the document's owner) change the group of {@link #USER1_DOC1} from "user1" to "user2".
     */
    @Test
    void changeDocumentGroupAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the user "user2" (not the collection's owner) change the group of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionGroupAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the user "user2" (not the collection's owner) change the group of {@link #USER1_COL1} from "user1" to "user2".
     */
    @Test
    void changeCollectionGroupAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USER2_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the group of {@link #USER1_DOC1} from "user1" to "otherGroup" (of which user1 is a member).
     */
    @Test
    void changeDocumentGroupToMemberGroupAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), OTHER_GROUP_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the document owner user change the group of {@link #USER1_DOC1} from "user1" to "otherGroup" (of which user1 is a member).
     */
    @Test
    void changeDocumentGroupToMemberGroupAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), OTHER_GROUP_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the group of {@link #USER1_COL1} from "user1" to "otherGroup" (of which user1 is a member).
     */
    @Test
    void changeCollectionGroupToMemberGroupAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), OTHER_GROUP_NAME);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the collection owner user change the group of {@link #USER1_COL1} from "user1" to "otherGroup" (of which user1 is a member).
     */
    @Test
    void changeCollectionGroupToMemberGroupAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), OTHER_GROUP_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the user "user2" (not the document's owner) change the group of {@link #USER1_DOC1} from "user1" to "otherGroup" (of which user2 is a member).
     */
    @Test
    void changeDocumentGroupToMemberGroupAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), OTHER_GROUP_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the user "user2" (not the document's owner) change the group of {@link #USER1_DOC1} from "user1" to "otherGroup" (of which user2 is a member).
     */
    @Test
    void changeDocumentGroupToMemberGroupAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        Runnable3E x = () ->
                    changeGroup(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), OTHER_GROUP_NAME);
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDenied(x));
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the user "user2" (not the collection's owner) change the group of {@link #USER1_COL1} from "user1" to "otherGroup" (of which user2 is a member).
     */
    @Test
    void changeCollectionGroupToMemberGroupAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), OTHER_GROUP_NAME);
            }));
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the user "user2" (not the collection's owner) change the group of {@link #USER1_COL1} from "user1" to "otherGroup" (of which user2 is a member).
     */
    @Test
    void changeCollectionGroupToMemberGroupAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
        Runnable3E x = () ->
                    changeGroup(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), OTHER_GROUP_NAME);
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDenied(x));
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the group of {@link #USER1_DOC1} from "user1" to "user1".
     * Finally make sure that chown has cleared the setUid and setGid bits.
     */
    @Test
    void changeDocumentGroupToSelfAsNonDBAOwner_clearsSetUidAndSetGid_restricted(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setUid and setGid bits are set before we begin
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the owner
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), USER1_NAME);

        // check the setUid and setGid bits are now cleared
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), NOT_SET);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the document owner user change the group of {@link #USER1_DOC1} from "user1" to "user1".
     * Finally make sure that chown has cleared the setUid and setGid bits.
     */
    @Test
    void changeDocumentGroupToSelfAsNonDBAOwner_clearsSetUidAndSetGid(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setUid and setGid bits are set before we begin
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the owner
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), USER1_NAME);

        // check the setUid and setGid bits are now cleared
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), NOT_SET);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the group of {@link #USER1_COL2} from "user1" to "user1".
     * Finally make sure that chown has cleared the setUid and setGid bits.
     */
    @Test
    void changeCollectionGroupToSelfAsNonDBAOwner_clearsSetUidAndSetGid_restricted(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setUid and setGid bits are set before we begin
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the owner
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), USER1_NAME);

        // check the setUid and setGid bits are now cleared
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), NOT_SET);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the collection owner user change the group of {@link #USER1_COL2} from "user1" to "user1".
     * Finally make sure that chown has cleared the setUid and setGid bits.
     */
    @Test
    void changeCollectionGroupToSelfAsNonDBAOwner_clearsSetUidAndSetGid(final BrokerPool pool) throws AuthenticationException, EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);

        // check the setUid and setGid bits are set before we begin
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the owner
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), USER1_NAME);

        // check the setUid and setGid bits are now cleared
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), NOT_SET);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the DBA user change the group of {@link #USER1_DOC1} from "user1" to "user1".
     * Finally make sure that chown has preserved the setUid and setGid bits.
     */
    @Test
    void changeDocumentGroupToSelfAsDBA_preservesSetUidAndSetGid_restricted(final BrokerPool pool) throws EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().getSystemSubject();

        // check the setUid and setGid bits are set before we begin
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the owner
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), USER1_NAME);

        // check the setUid and setGid bits are still set
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the DBA user change the group of {@link #USER1_DOC1} from "user1" to "user1".
     * Finally make sure that chown has preserved the setUid and setGid bits.
     */
    @Test
    void changeDocumentGroupToSelfAsDBA_preservesSetUidAndSetGid(final BrokerPool pool) throws EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().getSystemSubject();

        // check the setUid and setGid bits are set before we begin
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);

        // change the owner
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), USER1_NAME);

        // check the setUid and setGid bits are still set
        assertDocumentSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_XQUERY1), IS_SET);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the DBA user change the group of {@link #USER1_COL2} from "user1" to "user1".
     * Finally make sure that chown has preserved the setUid and setGid bits.
     */
    @Test
    void changeCollectionGroupToSelfAsDBA_preservesSetUidAndSetGid_restricted(final BrokerPool pool) throws EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().getSystemSubject();

        // check the setUid and setGid bits are set before we begin
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the owner
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), USER1_NAME);

        // check the setUid and setGid bits are still set
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);
    }

    /**
     * With {@code posix-chown-restricted="false"},
     * as the DBA user change the owner of {@link #USER1_COL2} from "user1" to "user1".
     * Finally make sure that chown has preserved the setUid and setGid bits.
     */
    @Test
    void changeCollectionGroupToSelfAsDBA_preservesSetUidAndSetGid(final BrokerPool pool) throws EXistException, PermissionDeniedException, XPathException {
        final Subject user1 = pool.getSecurityManager().getSystemSubject();

        // check the setUid and setGid bits are set before we begin
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);

        // change the owner
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), USER1_NAME);

        // check the setUid and setGid bits are still set
        assertCollectionSetUidSetGid(pool, user1, TestConstants.TEST_COLLECTION_URI.append(USER1_COL2), IS_SET);
    }

    @Test
    void changeCollectionOwnerToNonExistentAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-user", USER1_NAME);
    }

    @Test
    void changeCollectionOwnerToNonExistentAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-user", USER1_NAME);
    }

    @Test
    void changeCollectionOwnerToRemovedUserAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeCollectionOwnerToRemovedUserAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeCollectionOwnerToNonExistentAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-user", USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the owner of {@link #USER1_COL1} from "user1" to "no-such-user".
     */
    @Test
    public void changeCollectionOwnerToNonExistentAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-user", USER1_NAME);
    }

    @Test
    void changeCollectionOwnerToRemovedUserAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the owner of {@link #USER1_COL1} from "user1" to "userrm".
     */
    @Test
    void changeCollectionOwnerToRemovedUserAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
                changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
            }));
    }

    @Test
    void changeCollectionOwnerToNonExistentAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-user", USER1_NAME);
            }));
    }

    @Test
    void changeCollectionOwnerToNonExistentAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-user", USER1_NAME);
            }));
    }

    @Test
    void changeCollectionOwnerToRemovedUserAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
            }));
    }

    @Test
    void changeCollectionOwnerToRemovedUserAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
            }));
    }

    @Test
    void changeCollectionGroupToNonExistentAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-group", USER1_NAME);
    }

    @Test
    void changeCollectionGroupToNonExistentAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-group", USER1_NAME);
    }

    @Test
    void changeCollectionGroupToRemovedGroupAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeCollectionGroupToRemovedGroupAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeCollectionGroupToNonExistentAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-group", USER1_NAME);
    }

    @Test
    void changeCollectionGroupToNonExistentAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-group", USER1_NAME);
    }

    @Test
    void changeCollectionGroupToRemovedGroupAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeCollectionGroupToRemovedGroupAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeCollectionGroupToNonExistentAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-group", USER1_NAME);
            }));
    }

    @Test
    void changeCollectionGroupToNonExistentAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), "no-such-group", USER1_NAME);
            }));
    }

    @Test
    void changeCollectionGroupToRemovedGroupAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
            }));
    }

    @Test
    void changeCollectionGroupToRemovedGroupAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), USERRM_NAME, USER1_NAME);
            }));
    }

    @Test
    void changeDocumentOwnerToNonExistentAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-user", USER1_NAME);
    }

    @Test
    void changeDocumentOwnerToNonExistentAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-user", USER1_NAME);
    }

    @Test
    void changeDocumentOwnerToRemovedUserAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeDocumentOwnerToRemovedUserAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeDocumentOwnerToNonExistentAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-user", USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the owner of {@link #USER1_DOC1} from "user1" to "no-such-user".
     */
    @Test
    void changeDocumentOwnerToNonExistentAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
                changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-user", USER1_NAME);
            }));
    }

    @Test
    void changeDocumentOwnerToRemovedUserAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the owner of {@link #USER1_DOC1} from "user1" to "no-such-user".
     */
    @Test
    void changeDocumentOwnerToRemovedUserAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
                changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
            }));
    }

    @Test
    void changeDocumentOwnerToNonExistentAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-user", USER1_NAME);
            }));
    }

    @Test
    void changeDocumentOwnerToNonExistentAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-user", USER1_NAME);
            }));
    }

    @Test
    void changeDocumentOwnerToRemovedUserAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
            }));
    }

    @Test
    void changeDocumentOwnerToRemovedUserAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
            }));
    }

    @Test
    void changeDocumentGroupToNonExistentAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-group", USER1_NAME);
    }

    @Test
    void changeDocumentGroupToNonExistentAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-group", USER1_NAME);
    }

    @Test
    void changeDocumentGroupToRemovedGroupAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeDocumentGroupToRemovedGroupAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeGroup(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeDocumentGroupToNonExistentAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-group", USER1_NAME);
    }

    @Test
    void changeDocumentGroupToNonExistentAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-group", USER1_NAME);
    }

    @Test
    void changeDocumentGroupToRemovedGroupAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeDocumentGroupToRemovedGroupAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeGroup(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
    }

    @Test
    void changeDocumentGroupToNonExistentAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-group", USER1_NAME);
            }));
    }

    @Test
    void changeDocumentGroupToNonExistentAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), "no-such-group", USER1_NAME);
            }));
    }

    @Test
    void changeDocumentGroupToRemovedGroupAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
            }));
    }

    @Test
    void changeDocumentGroupToRemovedGroupAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeGroup(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), USERRM_NAME, USER1_NAME);
            }));
    }

    //TODO need tests for changing owner like "user:group" and checking both resultant group and owner

    @Test
    void ChangeCollectionOwnerAndGroupToNonExistentAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
    }

    @Test
    void ChangeCollectionOwnerAndGroupToNonExistentAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
    }

    @Test
    void ChangeCollectionOwnerAndGroupToRemovedAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
    }

    @Test
    void ChangeCollectionOwnerAndGroupToRemovedAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
    }

    @Test
    void ChangeCollectionOwnerAndGroupToNonExistentAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the owner of {@link #USER1_COL1} from "user1" to "no-such-user".
     */
    public void ChangeCollectionOwnerAndGroupToNonExistentAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
    }

    @Test
    void ChangeCollectionOwnerAndGroupToRemovedAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the collection owner user change the owner of {@link #USER1_COL1} from "user1" to "userrm".
     */
    @Test
    void ChangeCollectionOwnerAndGroupToRemovedAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
                changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    @Test
    void ChangeCollectionOwnerAndGroupToNonExistentAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    @Test
    void ChangeCollectionOwnerAndGroupToNonExistentAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    @Test
    void ChangeCollectionOwnerAndGroupToRemovedAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    @Test
    void ChangeCollectionOwnerAndGroupToRemovedAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_COL1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    @Test
    void ChangeDocumentOwnerAndGroupToNonExistentAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
    }

    @Test
    void ChangeDocumentOwnerAndGroupToNonExistentAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
    }

    @Test
    void ChangeDocumentOwnerAndGroupToRemovedAsDBA(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
    }

    @Test
    void ChangeDocumentOwnerAndGroupToRemovedAsDBA_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject adminUser = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        changeOwner(pool, adminUser, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
    }

    @Test
    void ChangeDocumentOwnerAndGroupToNonExistentAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the owner of {@link #USER1_DOC1} from "user1" to "no-such-user".
     */
    @Test
    void ChangeDocumentOwnerAndGroupToNonExistentAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
                changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    @Test
    void ChangeDocumentOwnerAndGroupToRemovedAsNonDBAOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
        changeOwner(pool, user1, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
    }

    /**
     * With {@code posix-chown-restricted="true"},
     * as the document owner user change the owner of {@link #USER1_DOC1} from "user1" to "no-such-user".
     */
    @Test
    void ChangeDocumentOwnerAndGroupToRemovedAsNonDBAOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user1 = pool.getSecurityManager().authenticate(USER1_NAME, USER1_PWD);
                changeOwner(pool, user1, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    @Test
    void ChangeDocumentOwnerAndGroupToNonExistentAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    @Test
    void ChangeDocumentOwnerAndGroupToNonExistentAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple("no-such-user", "no-such-group"), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    @Test
    void ChangeDocumentOwnerAndGroupToRemovedAsNonOwner(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, NOT_RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    @Test
    void ChangeDocumentOwnerAndGroupToRemovedAsNonOwner_restricted(final BrokerPool pool) throws AuthenticationException, XPathException, PermissionDeniedException, EXistException {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDeniedWithAuth(() -> {
                final Subject user2 = pool.getSecurityManager().authenticate(USER2_NAME, USER2_PWD);
                changeOwner(pool, user2, RESTRICTED, TestConstants.TEST_COLLECTION_URI.append(USER1_DOC1), Tuple(USERRM_NAME, USERRM_NAME), Tuple(USER1_NAME, USER1_NAME));
            }));
    }

    private void changeOwner(final BrokerPool pool, final Subject execAsUser, final boolean restricted, final XmldbURI uri, final String newOwner) throws EXistException, PermissionDeniedException, XPathException {
        changeOwner(pool, execAsUser, restricted, uri, newOwner, newOwner);
    }

    private void changeOwner(final BrokerPool pool, final Subject execAsUser, final boolean restricted, final XmldbURI uri, final Tuple2<String, String> newOwnerGroup, final Tuple2<String, String> expectedOwnerGroup) throws EXistException, PermissionDeniedException, XPathException {
        changeOwner(pool, execAsUser, restricted, uri, newOwnerGroup.<String>fold(og -> og._1 + ":" + og._2), expectedOwnerGroup.<String>fold(og -> og._1 + ":" + og._2));
    }

    private void changeOwner(final BrokerPool pool, final Subject execAsUser, final boolean restricted, final XmldbURI uri, final String newOwnerGroup, final String expectedOwnerGroup) throws EXistException, PermissionDeniedException, XPathException {
        final boolean prevRestricted = setPosixChownRestricted(pool, restricted);
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "sm:chown(xs:anyURI('" + uri.getRawCollectionPath() + "'), '" + newOwnerGroup + "'),\n" +
                "sm:get-permissions(xs:anyURI('" + uri.getRawCollectionPath() + "'))/sm:permission/(string(@owner), string(@group))";

        try (final DBBroker broker = pool.get(Optional.of(execAsUser));
                final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {

            final Sequence result = queryResult.result;

            assertEquals(2, result.getItemCount());

            final String expectedOwnerGroupParts[] = expectedOwnerGroup.split(":");
            assertEquals(expectedOwnerGroupParts[0], result.itemAt(0).getStringValue());
            if (expectedOwnerGroupParts.length == 2) {
                assertEquals(expectedOwnerGroupParts[1], result.itemAt(1).getStringValue());
            }

        } finally {
            setPosixChownRestricted(pool, prevRestricted);
        }
    }

    private void changeGroup(final BrokerPool pool, final Subject execAsUser, final boolean restricted, final XmldbURI uri, final String newGroup) throws EXistException, PermissionDeniedException, XPathException {
        changeGroup(pool, execAsUser, restricted, uri, newGroup, newGroup);
    }

    private void changeGroup(final BrokerPool pool, final Subject execAsUser, final boolean restricted, final XmldbURI uri, final String newGroup, final String expectedGroup) throws EXistException, PermissionDeniedException, XPathException {
        final boolean prevRestricted = setPosixChownRestricted(pool, restricted);

        final String query =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                        "sm:chgrp(xs:anyURI('" + uri.getRawCollectionPath() + "'), '" + newGroup + "'),\n" +
                        "sm:get-permissions(xs:anyURI('" + uri.getRawCollectionPath() + "'))/sm:permission/string(@group)";

        try (final DBBroker broker = pool.get(Optional.of(execAsUser));
                final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {

            final Sequence result = queryResult.result;

            assertEquals(1, result.getItemCount());
            assertEquals(expectedGroup, result.itemAt(0).getStringValue());
        } finally {
            setPosixChownRestricted(pool, prevRestricted);
        }
    }

    private static void assertDocumentSetUidSetGid(final BrokerPool pool, final Subject execAsUser, final XmldbURI uri, final boolean isSet) throws EXistException, PermissionDeniedException {
        try (final DBBroker broker = pool.get(Optional.of(execAsUser));
                final LockedDocument lockedDoc = broker.getXMLResource(uri, Lock.LockMode.READ_LOCK)) {

            final DocumentImpl doc = lockedDoc.getDocument();
            if (isSet) {
                assertTrue(doc.getPermissions().isSetUid());
                assertTrue(doc.getPermissions().isSetGid());
            } else {
                assertFalse(doc.getPermissions().isSetUid());
                assertFalse(doc.getPermissions().isSetGid());
            }
        }
    }

    private static void assertCollectionSetUidSetGid(final BrokerPool pool, final Subject execAsUser, final XmldbURI uri, final boolean isSet) throws EXistException, PermissionDeniedException {
        try (final DBBroker broker = pool.get(Optional.of(execAsUser))) {
            try (final Collection col = broker.openCollection(uri, Lock.LockMode.READ_LOCK)) {

                if (isSet) {
                    assertTrue(col.getPermissions().isSetUid());
                    assertTrue(col.getPermissions().isSetGid());
                } else {
                    assertFalse(col.getPermissions().isSetUid());
                    assertFalse(col.getPermissions().isSetGid());
                }
            }
        }
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
            createUser(broker, sm, USERRM_NAME, USERRM_PWD);

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

        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            removeUser(sm, USERRM_NAME);

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
            removeGroup(sm, OTHER_GROUP_NAME);

            if (sm.hasAccount(USERRM_NAME)) {
                removeUser(sm, USERRM_NAME);
            }

            removeCollection(broker, transaction, TestConstants.TEST_COLLECTION_URI);

            transaction.commit();
        }
    }

    /**
     * Set the posix-chown-restricted flag.
     *
     * @param restricted true if the restriction is enforced, false otherwise.
     *
     * @return the previous value of the flag.
     */
    private boolean setPosixChownRestricted(final BrokerPool pool, final boolean restricted) {
        final Configuration config = pool.getConfiguration();
        final boolean prevPosixChownRestricted = config.getProperty(DBBroker.POSIX_CHOWN_RESTRICTED_PROPERTY, true);
        config.setProperty(DBBroker.POSIX_CHOWN_RESTRICTED_PROPERTY, restricted);
        return prevPosixChownRestricted;
    }
}
