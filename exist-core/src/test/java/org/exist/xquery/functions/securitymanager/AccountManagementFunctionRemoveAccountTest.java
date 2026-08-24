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

import com.evolvedbinary.j8fu.function.Runnable4E;
import org.exist.EXistException;
import org.exist.TestUtils;
import org.exist.security.AuthenticationException;
import org.exist.security.PermissionDeniedException;
import org.exist.security.SecurityManager;
import org.exist.security.Subject;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccountManagementFunctionRemoveAccountTest {

    @RegisterExtension
    public final EmbeddedDatabaseExtension embeddedDatabase = new EmbeddedDatabaseExtension(true, true);

    @Test
    void cannotDeleteSystemAccount() throws AuthenticationException {
        final Subject admin = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        Runnable3E x = () -> xqueryRemoveAccount(SecurityManager.SYSTEM, Optional.of(admin).close());
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDenied(x));
    }

    @Test
    void cannotDeleteDbaAccount() {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDenied(() -> xqueryRemoveAccount(SecurityManager.DBA_USER).close()));
    }

    @Test
    void cannotDeleteGuestAccount() {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDenied(() -> xqueryRemoveAccount(SecurityManager.GUEST_USER).close()));
    }

    @Test
    void cannotDeleteUnknownAccount() {
        assertThrows(PermissionDeniedException.class, () ->
            extractPermissionDenied(() -> xqueryRemoveAccount(SecurityManager.UNKNOWN_USER).close()));
    }

    private XQueryUtil.QueryResult xqueryRemoveAccount(final String username) throws XPathException, PermissionDeniedException, EXistException, IOException {
        final BrokerPool pool = embeddedDatabase.getBrokerPool();
        final Optional<Subject> asUser = Optional.of(pool.getSecurityManager().getSystemSubject());
        return xqueryRemoveAccount(username, asUser);
    }

    private XQueryUtil.QueryResult xqueryRemoveAccount(final String username, final Optional<Subject> asUser) throws EXistException, PermissionDeniedException, XPathException, IOException {
        final BrokerPool pool = embeddedDatabase.getBrokerPool();

        final String query =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                        "sm:remove-account('" + username + "')";

        try (final DBBroker broker = pool.get(asUser)) {
             return XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null);
        }
    }

    private static void extractPermissionDenied(final Runnable4E<XPathException, PermissionDeniedException, EXistException, IOException> runnable) throws XPathException, PermissionDeniedException, EXistException, IOException {
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
}
