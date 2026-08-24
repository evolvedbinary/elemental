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
 */
package org.exist.xquery.functions.securitymanager;

import com.evolvedbinary.j8fu.function.Runnable4E;
import com.evolvedbinary.j8fu.function.Runnable5E;
import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.*;
import org.exist.security.SecurityManager;
import org.exist.security.internal.aider.GroupAider;
import org.exist.security.internal.aider.UserAider;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.util.LockException;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SecurityManagerTestUtil {

    static Account createUser(final DBBroker broker, final org.exist.security.SecurityManager sm, final String username, final String password) throws PermissionDeniedException, EXistException {
        return createUser(broker, sm, username, password, null);
    }

    static Account createUser(final DBBroker broker, final org.exist.security.SecurityManager sm, final String username, final String password, @Nullable final List<Tuple2<SchemaType, String>> metadata) throws PermissionDeniedException, EXistException {
        Group userGroup = new GroupAider(username);
        sm.addGroup(broker, userGroup);
        final Account user = new UserAider(username);
        user.setPassword(password);
        user.setPrimaryGroup(userGroup);

        if (metadata != null) {
            for (final Tuple2<SchemaType, String> metadataEntry : metadata) {
                user.setMetadataValue(metadataEntry._1, metadataEntry._2);
            }
        }

        sm.addAccount(user);

        userGroup = sm.getGroup(username);
        userGroup.addManager(sm.getAccount(username));
        sm.updateGroup(userGroup);

        return user;
    }

    static Group createGroup(final DBBroker broker, final org.exist.security.SecurityManager sm, final String groupName) throws PermissionDeniedException, EXistException {
        final Group otherGroup = new GroupAider(groupName);
        return sm.addGroup(broker, otherGroup);
    }

    static void addUserToGroup(final org.exist.security.SecurityManager sm, final Account user, final Group group) throws PermissionDeniedException, EXistException {
        user.addGroup(group.getName());
        sm.updateAccount(user);
    }

    static void setPrimaryGroup(final org.exist.security.SecurityManager sm, final Account user, final Group group) throws PermissionDeniedException, EXistException {
        user.setPrimaryGroup(group);
        sm.updateAccount(user);
    }

    static void removeUser(final org.exist.security.SecurityManager sm, final String username) throws PermissionDeniedException, EXistException {
        sm.deleteAccount(username);
        removeGroup(sm, username);
    }

    static void removeGroup(final SecurityManager sm, final String groupname) throws PermissionDeniedException, EXistException {
        sm.deleteGroup(groupname);
    }

    static XQueryUtil.QueryResult xqueryAddUserAsGroupManager(final BrokerPool pool, final String username, final String groupname) throws EXistException, PermissionDeniedException, XPathException, IOException {
        final String query =
            "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "sm:add-group-manager('" + groupname + "', '" + username + "')";

        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            return XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null);
        }
    }

    static XQueryUtil.QueryResult xqueryRemoveUserFromGroup(final BrokerPool pool, final String username, final String groupname) throws XPathException, PermissionDeniedException, EXistException, IOException {
        final Optional<Subject> asUser = Optional.of(pool.getSecurityManager().getSystemSubject());
        return xqueryRemoveUserFromGroup(pool, username, groupname, asUser);
    }

    static XQueryUtil.QueryResult xqueryRemoveUserFromGroup(final BrokerPool pool, final String username, final String groupname, final Optional<Subject> asUser) throws EXistException, PermissionDeniedException, XPathException, IOException {
        final String query =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "sm:remove-group-member('" + groupname + "', '" + username + "')";

        try (final DBBroker broker = pool.get(asUser)) {
            return XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null);
        }
    }

    static XQueryUtil.QueryResult xqueryRemoveGroup(final BrokerPool pool, final String groupname) throws EXistException, PermissionDeniedException, XPathException, IOException {
        final String query =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "sm:remove-group('" + groupname + "')";

        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            return XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null);
        }
    }

    static void xqueryChangeMode(final BrokerPool pool, final Subject execAsUser, final XmldbURI uri, final String newMode) throws EXistException, PermissionDeniedException, XPathException, IOException {
        final String query =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "sm:chmod(xs:anyURI('" + uri.getRawCollectionPath() + "'), '" + newMode + "'),\n" +
                "sm:get-permissions(xs:anyURI('" + uri.getRawCollectionPath() + "'))/sm:permission/string(@mode)";

        try (final DBBroker broker = pool.get(Optional.of(execAsUser));
                final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {

            final Sequence result = queryResult.result;

            assertEquals(1, result.getItemCount());
            assertEquals(newMode, result.itemAt(0).getStringValue());
        }
    }

    static void removeDocument(final DBBroker broker, final Txn transaction, final XmldbURI documentUri) throws PermissionDeniedException, LockException, IOException, TriggerException {
        try (final Collection collection = broker.openCollection(documentUri.removeLastSegment(), Lock.LockMode.WRITE_LOCK)) {
            collection.removeXMLResource(transaction, broker, documentUri.lastSegment());
            broker.saveCollection(transaction, collection);
        }
    }

    static void removeCollection(final DBBroker broker, final Txn transaction, final XmldbURI collectionUri) throws PermissionDeniedException, IOException, TriggerException {
        try (final Collection collection = broker.openCollection(collectionUri, Lock.LockMode.WRITE_LOCK)) {
            broker.removeCollection(transaction, collection);
        }
    }

    static void extractPermissionDenied(final Runnable4E<XPathException, PermissionDeniedException, EXistException, IOException> runnable) throws XPathException, PermissionDeniedException, EXistException, IOException {
        try {
            runnable.run();
        } catch (final XPathException | IOException e) {
            if (e.getCause() != null && e.getCause() instanceof PermissionDeniedException) {
                throw (PermissionDeniedException)e.getCause();
            } else {
                throw e;
            }
        }
    }

    static void extractPermissionDeniedWithAuth(final Runnable5E<XPathException, AuthenticationException, PermissionDeniedException, EXistException, IOException> runnable) throws XPathException, AuthenticationException, PermissionDeniedException, EXistException, IOException {
        try {
            runnable.run();
        } catch (final XPathException e) {
            if (e.getCause() != null && e.getCause() instanceof PermissionDeniedException) {
                throw (PermissionDeniedException)e.getCause();
            } else {
                throw e;
            }
        }
    }

    static void assertDocumentSetGid(final BrokerPool pool, final Subject execAsUser, final XmldbURI uri, final boolean isSet) throws EXistException, PermissionDeniedException {
        try (final DBBroker broker = pool.get(Optional.of(execAsUser));
             final LockedDocument lockedDoc = broker.getXMLResource(uri, Lock.LockMode.READ_LOCK)) {

            final DocumentImpl doc = lockedDoc.getDocument();
            if (isSet) {
                assertTrue(doc.getPermissions().isSetGid());
            } else {
                assertFalse(doc.getPermissions().isSetGid());
            }
        }
    }

    static void assertCollectionSetGid(final BrokerPool pool, final Subject execAsUser, final XmldbURI uri, final boolean isSet) throws EXistException, PermissionDeniedException {
        try (final DBBroker broker = pool.get(Optional.of(execAsUser))) {
            try (final Collection col = broker.openCollection(uri, Lock.LockMode.READ_LOCK)) {
                if (isSet) {
                    assertTrue(col.getPermissions().isSetGid());
                } else {
                    assertFalse(col.getPermissions().isSetGid());
                }
            }
        }
    }
}
