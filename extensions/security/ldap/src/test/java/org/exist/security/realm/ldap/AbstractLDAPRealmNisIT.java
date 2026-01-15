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
package org.exist.security.realm.ldap;

import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.security.*;
import org.exist.security.SecurityManager;
import org.exist.security.internal.RealmImpl;
import org.exist.security.internal.aider.GroupAider;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.DatabaseConfigurationException;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for the LDAP Realm.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractLDAPRealmNisIT extends AbstractLdapTestUnit {
    private static ExistEmbeddedServer existEmbeddedServer;

    private static final Set<String> EXPECTED_DATABASE_USER_NAMES = new HashSet<>(Arrays.asList(
        "nobody",
        "guest",
        "admin",
        "SYSTEM"
    ));

    private static final Set<String> EXPECTED_LDAP_USER_NAMES = new HashSet<>(Arrays.asList(
        "user1@gb.myorg.com",
        "user2@gb.myorg.com",
        "user3@gb.myorg.com"
    ));

    private static final Set<String> EXPECTED_DATABASE_GROUP_NAMES = new HashSet<>(Arrays.asList(
        "nogroup",
        "guest",
        "dba",
        "ldap-users"
    ));

    private static final Set<String> EXPECTED_LDAP_GROUP_NAMES = new HashSet<>(Arrays.asList(
        "group1@gb.myorg.com",
        "group2@gb.myorg.com"
    ));

    private final PrincipalPattern principalPattern;
    private final DefaultUser defaultUser;

    public AbstractLDAPRealmNisIT(final PrincipalPattern principalPattern, final DefaultUser defaultUser) {
        this.principalPattern = principalPattern;
        this.defaultUser = defaultUser;
    }

    private boolean directLdapAuthShouldFail() {
        return principalPattern == PrincipalPattern.ABSENT
            || principalPattern == PrincipalPattern.PRESENT_INVALID;
    }

    private boolean defaultLdapAuthShouldFail() {
        return defaultUser == DefaultUser.ABSENT
            || defaultUser == DefaultUser.PRESENT_INVALID_UNQUALIFIED
            || defaultUser == DefaultUser.PRESENT_INVALID_QUALIFIED
            || (defaultUser == DefaultUser.PRESENT_VALID_UNQUALIFIED && (principalPattern == PrincipalPattern.ABSENT || principalPattern == PrincipalPattern.PRESENT_INVALID));
    }

    @BeforeEach
    public void setup() throws DatabaseConfigurationException, EXistException, IOException, PermissionDeniedException, LockException, URISyntaxException, SAXException {
        existEmbeddedServer = new ExistEmbeddedServer(true, true);
        existEmbeddedServer.startDb();

        // Start eXist-db with an LDAP Security config
        createLocalLdapUsersGroup(existEmbeddedServer);
        configureLdapRealm(existEmbeddedServer);
    }

    @AfterEach
    public void cleanup() {
        existEmbeddedServer.stopDb();
    }

    @Test
    public void authenticateUnqualified() throws AuthenticationException {
        // Attempt to authenticate as user-2 via LDAP Realm
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final org.exist.security.SecurityManager securityManager = brokerPool.getSecurityManager();

        if (directLdapAuthShouldFail()) {
            // direct authentication should fail
            assertThrows(AuthenticationException.class, () ->
                securityManager.authenticate("user2", "user2")
            );

        } else {
            // direct authentication should succeed
            final Subject ldapUser2Subject = securityManager.authenticate("user2", "user2");
            assertNotNull(ldapUser2Subject);
        }
    }

    @Test
    public void authenticateQualified() throws AuthenticationException {
        // Attempt to authenticate as user-2 via LDAP Realm
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final org.exist.security.SecurityManager securityManager = brokerPool.getSecurityManager();

        // direct authentication should succeed
        final Subject ldapUser2Subject = securityManager.authenticate("uid=user2,ou=Users,dc=gb,dc=myorg,dc=com", "user2");
        assertNotNull(ldapUser2Subject);
    }

    @Test
    public void findAllUserNames() throws EXistException {
        final Set<String> expectedUserNames = new HashSet<>(EXPECTED_DATABASE_USER_NAMES);
        if (!defaultLdapAuthShouldFail()) {
            // default authentication should succeed, so we should see users from both the database and LDAP
            expectedUserNames.addAll(EXPECTED_LDAP_USER_NAMES);
        }

        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Set<String> actualUserNames = new HashSet(securityManager.findAllUserNames());
            assertEquals(expectedUserNames, actualUserNames);
        }
    }

    @Test
    public void findAllGroupNames() throws EXistException {
        final Set<String> expectedGroupNames = new HashSet<>(EXPECTED_DATABASE_GROUP_NAMES);
        if (!defaultLdapAuthShouldFail()) {
            // default authentication should succeed, so we should see users from both the database and LDAP
            expectedGroupNames.addAll(EXPECTED_LDAP_GROUP_NAMES);
        }

        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Set<String> actualGroupNames = new HashSet(securityManager.findAllGroupNames());
            assertEquals(expectedGroupNames, actualGroupNames);
        }
    }

    @Test
    public void findAllGroupMembers() throws EXistException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();

        Set<String> expectedGroupMembers = new HashSet<>();
        if (!defaultLdapAuthShouldFail()) {
            // default authentication should succeed, so we should see users from both the database and LDAP
            expectedGroupMembers.add("user1@gb.myorg.com");
            expectedGroupMembers.add("user2@gb.myorg.com");
        }

        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Set<String> actualGroupMembers = new HashSet(securityManager.findAllGroupMembers("group1"));
            assertEquals(expectedGroupMembers, actualGroupMembers);
        }

        expectedGroupMembers.clear();
        if (!defaultLdapAuthShouldFail()) {
            // default authentication should succeed, so we should see users from both the database and LDAP
            expectedGroupMembers.add("user2@gb.myorg.com");
        }

        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Set<String> actualGroupMembers = new HashSet(securityManager.findAllGroupMembers("group2"));
            assertEquals(expectedGroupMembers, actualGroupMembers);
        }

        // group 3 should never appear as it is not in the LDAP Realm security config's white-list
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Set<String> actualGroupMembers = new HashSet(securityManager.findAllGroupMembers("group3"));
            assertTrue(actualGroupMembers.isEmpty());
        }
    }

    @Test
    public void findUsernamesWhereNameStarts() throws EXistException {
        final Set<String> expectedUserNames = new HashSet<>();
        if (!defaultLdapAuthShouldFail()) {
            // default authentication should succeed, so we should see users from LDAP
            expectedUserNames.addAll(EXPECTED_LDAP_USER_NAMES);
        }

        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Set<String> actualUserNames = new HashSet(securityManager.findUsernamesWhereNameStarts("First"));
            assertEquals(expectedUserNames, actualUserNames);
        }
    }

    @Test
    public void findUsernamesWhereNamePartStarts() throws EXistException {
        final Set<String> expectedUserNames = new HashSet<>();
        if (!defaultLdapAuthShouldFail()) {
            // default authentication should succeed, so we should see users from LDAP
            expectedUserNames.addAll(EXPECTED_LDAP_USER_NAMES);
        }

        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Set<String> actualUserNames = new HashSet(securityManager.findUsernamesWhereNamePartStarts("First"));
            assertEquals(expectedUserNames, actualUserNames);
        }
    }

    @Test
    public void findUsernamesWhereUsernameStarts() throws EXistException {
        final Set<String> expectedUserNames = new HashSet<>();
        if (!defaultLdapAuthShouldFail()) {
            // default authentication should succeed, so we should see users from LDAP
            expectedUserNames.addAll(EXPECTED_LDAP_USER_NAMES);
        }

        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Set<String> actualUserNames = new HashSet(securityManager.findUsernamesWhereUsernameStarts("user"));
            assertEquals(expectedUserNames, actualUserNames);
        }
    }

    @Test
    public void findGroupnamesWhereGroupnameStarts() throws EXistException {
        final Set<String> expectedGroupNames = new HashSet<>();
        if (!defaultLdapAuthShouldFail()) {
            // default authentication should succeed, so we should see users from LDAP
            expectedGroupNames.addAll(EXPECTED_LDAP_GROUP_NAMES);
        }

        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Set<String> actualGroupNames = new HashSet(securityManager.findGroupnamesWhereGroupnameStarts("group"));
            assertEquals(expectedGroupNames, actualGroupNames);
        }
    }

    @Test
    public void findGroupnamesWhereGroupnameContains() throws EXistException {
        final Set<String> expectedGroupNames = new HashSet<>();
        expectedGroupNames.add("nogroup");
        if (!defaultLdapAuthShouldFail()) {
            // default authentication should succeed, so we should see users from LDAP
            expectedGroupNames.addAll(EXPECTED_LDAP_GROUP_NAMES);
        }

        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Set<String> actualGroupNames = new HashSet(securityManager.findGroupnamesWhereGroupnameContains("oup"));
            assertEquals(expectedGroupNames, actualGroupNames);
        }
    }

    @Test
    public void getAccount() throws EXistException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {

            @Nullable final Account account = securityManager.getAccount("user2");
            if (defaultLdapAuthShouldFail()) {
                // default authentication should fail, so we should not be able to retrieve the account details
                assertNull(account);

            } else {
                // default authentication should succeed, so we should now have the account details
                assertEquals("user2", account.getUsername());
                assertEquals("user2", account.getName());
                assertEquals("group1@gb.myorg.com", account.getPrimaryGroup());
                assertArrayEquals(new String[]{ "group1@gb.myorg.com", "group2@gb.myorg.com", "ldap-users" }, account.getGroups());

                assertEquals("First name 2", account.getMetadataValue(AXSchemaType.FIRSTNAME));
                assertEquals("Last name 2", account.getMetadataValue(AXSchemaType.LASTNAME));
                assertEquals("First Last 2", account.getMetadataValue(AXSchemaType.FULLNAME));
                assertEquals("es", account.getMetadataValue(AXSchemaType.LANGUAGE));
                assertEquals("user2@mail.com", account.getMetadataValue(AXSchemaType.EMAIL));
            }
        }
    }

    @Test
    public void hasAccount() throws EXistException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {

            if (defaultLdapAuthShouldFail()) {
                // default authentication should fail, so we should not be able to retrieve the account details
                assertFalse(securityManager.hasAccount("user2"));

            } else {
                // default authentication should succeed, so we should now have the account details
                assertTrue(securityManager.hasAccount("user2"));
            }

            assertFalse(securityManager.hasAccount("no-such-account-exists"));
        }
    }

    @Test
    public void getGroup() throws EXistException, PermissionDeniedException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {

            @Nullable final Group group = securityManager.getGroup("group2");
            if (defaultLdapAuthShouldFail()) {
                // default authentication should fail, so we should not be able to retrieve the group details
                assertNull(group);

            } else {
                // default authentication should succeed, so we should now have the group details
                assertEquals("group2", group.getName());
                assertEquals(Collections.emptyList(), group.getManagers());

                assertEquals("Group 2", group.getMetadataValue(EXistSchemaType.DESCRIPTION));
            }
        }
    }

    @Test
    public void hasGroup() throws EXistException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {

            if (defaultLdapAuthShouldFail()) {
                // default authentication should fail, so we should not be able to retrieve the group details
                assertFalse(securityManager.hasGroup("group2"));

            } else {
                // default authentication should succeed, so we should now have the group details
                assertTrue(securityManager.hasGroup("group2"));
            }

            assertFalse(securityManager.hasGroup("no-such-group-exists"));
        }
    }

    private void createLocalLdapUsersGroup(final ExistEmbeddedServer existEmbeddedServer) throws EXistException, PermissionDeniedException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final SecurityManager securityManager = brokerPool.getSecurityManager();
        try (final DBBroker broker = brokerPool.get(Optional.of(securityManager.getSystemSubject()))) {
            final Group localLdapUsersGroup = new GroupAider(RealmImpl.ID, "ldap-users");
            securityManager.addGroup(broker, localLdapUsersGroup);
        }
    }

    private void configureLdapRealm(final ExistEmbeddedServer existEmbeddedServer) throws EXistException, PermissionDeniedException, IOException, SAXException, URISyntaxException, LockException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final XmldbURI securityCollectionUri = XmldbURI.SYSTEM.append("security");

            try (final Collection securityCollection = broker.getOrCreateCollection(transaction, securityCollectionUri)) {

                final StringInputSource securityConfigInputSource = new StringInputSource(loadSecurityConfigXml());
                final MediaType xmlMediaType = broker.getBrokerPool().getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
                broker.storeDocument(transaction, XmldbURI.create("config.xml"), securityConfigInputSource, xmlMediaType, securityCollection);

                // NOTE: early release of Collection lock inline with Asymmetrical Locking scheme
                securityCollection.close();
            }
        }
    }

    private String loadSecurityConfigXml() throws URISyntaxException, IOException {
        final ElementalDbSecurityConfig securityConfigAnnotation = getClass().getAnnotation(ElementalDbSecurityConfig.class);
        final String securityConfigFileName = securityConfigAnnotation.fileName();
        final URL configUrl = getClass().getResource("/" + securityConfigFileName);
        final byte[] data = Files.readAllBytes(Paths.get(configUrl.toURI()));
        final String str = new String(data, StandardCharsets.UTF_8);
        return str.replace("<url>ldap://localhost:389</url>", "<url>ldap://localhost:" + ldapServer.getPort() + "</url>");
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ElementalDbSecurityConfig {
        String fileName();
    }

    public enum PrincipalPattern {
        ABSENT,
        PRESENT_VALID,
        PRESENT_INVALID
    }

    public enum DefaultUser {
        ABSENT,
        PRESENT_VALID_QUALIFIED,
        PRESENT_VALID_UNQUALIFIED,
        PRESENT_INVALID_QUALIFIED,
        PRESENT_INVALID_UNQUALIFIED,
    }
}
