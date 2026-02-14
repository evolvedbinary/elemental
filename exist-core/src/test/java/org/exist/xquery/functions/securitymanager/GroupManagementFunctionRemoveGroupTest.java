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
import org.exist.security.*;
import org.exist.security.SecurityManager;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.XPathException;
import org.junit.*;

import java.io.IOException;
import java.util.Optional;

import static org.exist.xquery.functions.securitymanager.SecurityManagerTestUtil.*;
import static org.junit.Assert.*;

public class GroupManagementFunctionRemoveGroupTest {

    private static final String USER1_NAME = "user1";
    private static final String USER1_PWD = USER1_NAME;
    private static final String USER2_NAME = "user2";
    private static final String USER2_PWD = USER2_NAME;

    private static final String OTHER_GROUP1_NAME = "otherGroup";
    private static final String OTHER_GROUP2_NAME = "otherGroup2";

    @Rule
    public final ExistEmbeddedServer existWebServer = new ExistEmbeddedServer(true, true);

    @Test(expected = PermissionDeniedException.class)
    public void cannotDeleteDbaGroup() throws XPathException, PermissionDeniedException, EXistException, IOException {
        extractPermissionDenied(() -> {
            xqueryRemoveGroup(existWebServer.getBrokerPool(), SecurityManager.DBA_GROUP).close();
        });
    }

    @Test(expected = PermissionDeniedException.class)
    public void cannotDeleteGuestGroup() throws XPathException, PermissionDeniedException, EXistException, IOException {
        extractPermissionDenied(() -> {
            xqueryRemoveGroup(existWebServer.getBrokerPool(), SecurityManager.GUEST_GROUP).close();
        });
    }

    @Test(expected = PermissionDeniedException.class)
    public void cannotDeleteUnknownGroup() throws XPathException, PermissionDeniedException, EXistException, IOException {
        extractPermissionDenied(() -> {
            xqueryRemoveGroup(existWebServer.getBrokerPool(), SecurityManager.UNKNOWN_GROUP).close();
        });
    }

    @Test
    public void deleteUsersSupplementalGroups() throws PermissionDeniedException, EXistException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final SecurityManager sm = pool.getSecurityManager();

        // create user with personal group as primary group
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            final Account user1 = createUser(broker, sm, USER1_NAME, USER1_PWD);

            final Group otherGroup1 = createGroup(broker, sm, OTHER_GROUP1_NAME);
            addUserToGroup(sm, user1, otherGroup1);

            final Group otherGroup2 = createGroup(broker, sm, OTHER_GROUP2_NAME);
            addUserToGroup(sm, user1, otherGroup2);

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

        // attempt to remove the supplemental groups of the user
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            assertTrue(sm.deleteGroup(OTHER_GROUP1_NAME));
            assertTrue(sm.deleteGroup(OTHER_GROUP2_NAME));

            transaction.commit();
        }

        // check that the user no longer has the supplemental groups
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            final Account user1 = sm.getAccount(USER1_NAME);
            final String user1PrimaryGroup = user1.getPrimaryGroup();
            assertEquals(USER1_NAME, user1PrimaryGroup);
            final String[] user1Groups = user1.getGroups();
            assertArrayEquals(new String[] { USER1_NAME, OTHER_GROUP1_NAME, OTHER_GROUP2_NAME }, user1Groups);
            for (final String user1Group : user1Groups) {
                if (user1PrimaryGroup.equals(user1Group)) {
                    assertNotNull(sm.getGroup(user1Group));
                } else {
                    // cannot retrieve groups which have been deleted!
                    assertNull(sm.getGroup(user1Group));
                }
            }

            transaction.commit();
        }
    }

    @Test(expected = PermissionDeniedException.class)
    public void deleteUsersPersonalPrimaryGroup() throws PermissionDeniedException, EXistException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final SecurityManager sm = pool.getSecurityManager();

        // create user with personal group as primary group
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            createUser(broker, sm, USER1_NAME, USER1_PWD);
            transaction.commit();
        }

        // check that the user is as we expect
        String user1PrimaryGroup = null;
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            final Account user1 = sm.getAccount(USER1_NAME);
            user1PrimaryGroup = user1.getPrimaryGroup();
            assertEquals(USER1_NAME, user1PrimaryGroup);
            assertArrayEquals(new String[] { USER1_NAME }, user1.getGroups());

            transaction.commit();
        }

        // attempt to remove the primary group of the user
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            sm.deleteGroup(user1PrimaryGroup);
            fail("Should have received: PermissionDeniedException: Account 'user1' still has 'user1' as their primary group!");

            transaction.commit();
        }
    }

    @Test
    public void deleteUsersSharingPersonalPrimaryGroup() throws PermissionDeniedException, EXistException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final SecurityManager sm = pool.getSecurityManager();

        // create two users which share a primary group
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            final Group otherGroup1 = createGroup(broker, sm, OTHER_GROUP1_NAME);

            Account user1 = createUser(broker, sm, USER1_NAME, USER1_PWD);
            addUserToGroup(sm, user1, otherGroup1);
            setPrimaryGroup(sm, user1, otherGroup1);

            final Account user2 = createUser(broker, sm, USER2_NAME, USER2_PWD);
            addUserToGroup(sm, user2, otherGroup1);
            setPrimaryGroup(sm, user2, otherGroup1);

            transaction.commit();
        }

        // check that the users are as we expect
        String primaryGroup = null;
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            final Account user1 = sm.getAccount(USER1_NAME);
            primaryGroup = user1.getPrimaryGroup();
            assertEquals(OTHER_GROUP1_NAME, primaryGroup);
            final String[] user1Groups = user1.getGroups();
            assertArrayEquals(new String[] { OTHER_GROUP1_NAME, USER1_NAME}, user1Groups);
            for (final String user1Group : user1Groups) {
                assertNotNull(sm.getGroup(user1Group));
            }

            final Account user2 = sm.getAccount(USER2_NAME);
            assertEquals(OTHER_GROUP1_NAME, user2.getPrimaryGroup());
            final String[] user2Groups = user2.getGroups();
            assertArrayEquals(new String[] { OTHER_GROUP1_NAME, USER2_NAME}, user2Groups);
            for (final String user2Group : user2Groups) {
                assertNotNull(sm.getGroup(user2Group));
            }

            transaction.commit();
        }

        // attempt to remove the primary group of the first user
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            try {
                sm.deleteGroup(primaryGroup);
                fail("Should have received: PermissionDeniedException: Account 'user1' still has 'otherGroup1' as their primary group!");
            } catch (final PermissionDeniedException e) {
                // expected
            }

            transaction.commit();
        }

        // delete the first user
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            removeUser(sm, USER1_NAME);
            transaction.commit();
        }

        // attempt to remove the primary group of the second user
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            try {
                sm.deleteGroup(primaryGroup);
                fail("Should have received: PermissionDeniedException: Account 'user2' still has 'otherGroup1' as their primary group!");
            } catch (final PermissionDeniedException e) {
                // expected
            }

            transaction.commit();
        }

        // delete the second user
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            removeUser(sm, USER2_NAME);
            transaction.commit();
        }

        // no users have the group as primary group, so now should be able to delete the group
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            sm.deleteGroup(primaryGroup);

            transaction.commit();
        }
    }
}
