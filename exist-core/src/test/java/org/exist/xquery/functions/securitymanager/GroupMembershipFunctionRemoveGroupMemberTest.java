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
import org.exist.security.*;
import org.exist.security.SecurityManager;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.XPathException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

import static org.exist.xquery.functions.securitymanager.SecurityManagerTestUtil.*;
import static org.junit.Assert.*;

public class GroupMembershipFunctionRemoveGroupMemberTest {

    private static final String USER1_NAME = "user1";
    private static final String USER1_PWD = USER1_NAME;

    private static final String OTHER_GROUP1_NAME = "otherGroup";
    private static final String OTHER_GROUP2_NAME = "otherGroup2";

    @Rule
    public final ExistEmbeddedServer existWebServer = new ExistEmbeddedServer(true, true);

    @Test(expected = PermissionDeniedException.class)
    public void cannotRemoveAllGroupsFromUserAsOwner() throws XPathException, PermissionDeniedException, EXistException, AuthenticationException, IOException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject owner = pool.getSecurityManager().authenticate(USER1_NAME, USER1_NAME);
        extractPermissionDenied(() -> {
            xqueryRemoveUserFromGroup(pool, USER1_NAME, OTHER_GROUP2_NAME, Optional.of(owner)).close();
            xqueryRemoveUserFromGroup(pool, USER1_NAME, OTHER_GROUP1_NAME, Optional.of(owner)).close();
            xqueryRemoveUserFromGroup(pool, USER1_NAME, USER1_NAME, Optional.of(owner)).close();
        });
    }

    @Test(expected = PermissionDeniedException.class)
    public void cannotRemoveAllGroupsFromUserAsDBA() throws XPathException, PermissionDeniedException, EXistException, AuthenticationException, IOException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject admin = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        extractPermissionDenied(() -> {
            xqueryRemoveUserFromGroup(pool, USER1_NAME, OTHER_GROUP2_NAME, Optional.of(admin)).close();
            xqueryRemoveUserFromGroup(pool, USER1_NAME, OTHER_GROUP1_NAME, Optional.of(admin)).close();
            xqueryRemoveUserFromGroup(pool, USER1_NAME, USER1_NAME, Optional.of(admin)).close();
        });
    }

    @Before
    public void setup() throws EXistException, PermissionDeniedException, XPathException, IOException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final SecurityManager sm = pool.getSecurityManager();

        // create user with personal group as primary group
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            final Account user1 = createUser(broker, sm, USER1_NAME, USER1_PWD);

            final Group otherGroup1 = createGroup(broker, sm, OTHER_GROUP1_NAME);
            addUserToGroup(sm, user1, otherGroup1);
            xqueryAddUserAsGroupManager(pool, USER1_NAME, OTHER_GROUP1_NAME).close();

            final Group otherGroup2 = createGroup(broker, sm, OTHER_GROUP2_NAME);
            addUserToGroup(sm, user1, otherGroup2);
            xqueryAddUserAsGroupManager(pool, USER1_NAME, OTHER_GROUP2_NAME).close();

            transaction.commit();
        }

        // check that the user is as we expect
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            final Account user1 = sm.getAccount(USER1_NAME);
            assertEquals(USER1_NAME, user1.getPrimaryGroup());
            final String[] user1Groups = user1.getGroups();
            assertArrayEquals(new String[] { USER1_NAME, OTHER_GROUP1_NAME, OTHER_GROUP2_NAME }, user1Groups);
            for (final String user1Group : user1Groups) {
                assertNotNull(sm.getGroup(user1Group));
            }
            transaction.commit();
        }
    }
}
