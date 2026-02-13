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
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

public class AccountManagementFunctionRemoveAccountTest {

    @Rule
    public final ExistEmbeddedServer existWebServer = new ExistEmbeddedServer(true, true);

    @Test(expected = PermissionDeniedException.class)
    public void cannotDeleteSystemAccount() throws XPathException, PermissionDeniedException, EXistException, AuthenticationException, IOException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Subject admin = pool.getSecurityManager().authenticate(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        extractPermissionDenied(() -> {
            xqueryRemoveAccount(SecurityManager.SYSTEM, Optional.of(admin));
        });
    }

    @Test(expected = PermissionDeniedException.class)
    public void cannotDeleteDbaAccount() throws XPathException, PermissionDeniedException, EXistException, IOException {
        extractPermissionDenied(() -> {
            xqueryRemoveAccount(SecurityManager.DBA_USER);
        });
    }

    @Test(expected = PermissionDeniedException.class)
    public void cannotDeleteGuestAccount() throws XPathException, PermissionDeniedException, EXistException, IOException {
        extractPermissionDenied(() -> {
            xqueryRemoveAccount(SecurityManager.GUEST_USER);
        });
    }

    @Test(expected = PermissionDeniedException.class)
    public void cannotDeleteUnknownAccount() throws XPathException, PermissionDeniedException, EXistException, IOException {
        extractPermissionDenied(() -> {
            xqueryRemoveAccount(SecurityManager.UNKNOWN_USER);
        });
    }

    private Sequence xqueryRemoveAccount(final String username) throws XPathException, PermissionDeniedException, EXistException, IOException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final Optional<Subject> asUser = Optional.of(pool.getSecurityManager().getSystemSubject());
        return xqueryRemoveAccount(username, asUser);
    }

    private Sequence xqueryRemoveAccount(final String username, final Optional<Subject> asUser) throws EXistException, PermissionDeniedException, XPathException, IOException {
        final BrokerPool pool = existWebServer.getBrokerPool();

        final String query =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                        "sm:remove-account('" + username + "')";

        try (final DBBroker broker = pool.get(asUser)) {
            return XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
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
