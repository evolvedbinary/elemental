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
package org.exist.security;

import java.util.Arrays;

import org.exist.TestUtils;
import org.exist.security.internal.aider.GroupAider;
import org.exist.security.internal.aider.UserAider;
import org.exist.test.DatabaseWebServerExtension;
import org.exist.xmldb.EXistCollectionManagementService;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.EXistXPathQueryService;
import org.exist.xmldb.UserManagementService;
import org.exist.xmldb.XmldbURI;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class XMLDBSecurityTest {

    @RegisterExtension
    public static final DatabaseWebServerExtension DATABASE_WEB_SERVER = new DatabaseWebServerExtension(true, false, true, true);
    private static final String PORT_PLACEHOLDER = "${PORT}";

    @Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "local", "xmldb:exist://" },
            { "remote", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc" }
        });
    }
    
    @Parameter
    public String apiName;
    
    @Parameter(value = 1)
    public String baseUri;

    private final String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(DATABASE_WEB_SERVER.getPort()));
    }

    @Test(expected=XMLDBException.class) // fails since guest has no write permissions
    public void worldCreateCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "guest", "guest")) {
            final CollectionManagementService cms = test.getService(CollectionManagementService.class);
            try (final Collection createdByGuest = cms.createCollection("createdByGuest")) { }
        }
    }

    @Test(expected=XMLDBException.class) // fails since guest has no write permissions
    public void worldAddResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "guest", "guest");
            final Resource resource = test.createResource("createdByGuest", XMLResource.class)) {
            resource.setContent("<testMe/>");
            test.storeResource(resource);
        }
    }

    @Test(expected=XMLDBException.class) // fails since guest has no write permissions
    public void worldRemoveCollection() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", "guest", "guest")) {
            final CollectionManagementService cms = root.getService(CollectionManagementService.class);
            cms.removeCollection("securityTest1");
        }
    }

    @Test(expected=XMLDBException.class) // fails since guest has no write permissions
    public void worldChmodCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "guest", "guest")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            // grant myself all rights ;-)
            ums.chmod(0777);
        }
    }

    @Test(expected=XMLDBException.class) // fails since guest has no write permissions
    public void worldChmodResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "guest", "guest");
             final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            // grant myself all rights ;-)
            ums.chmod(resource, 0777);
        }
    }

    @Test(expected=XMLDBException.class) // fails since guest has no write permissions
    public void worldChownCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "guest", "guest")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            final Account guest = ums.getAccount("guest");
            // make myself the owner ;-)
            ums.chown(guest, "guest");
        }
    }

    /**
     * only the owner or dba can chown a collection or resource
     */
    @Test (expected=XMLDBException.class)
    public void worldChownResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "guest", "guest");
             final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            // grant myself all rights ;-)
            final Account test2 = ums.getAccount("guest");
            ums.chown(resource, test2, "guest");
        }
    }

    @Test
    public void groupCreateSubColl() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2")) {
            final CollectionManagementService cms = test.getService(CollectionManagementService.class);
            try (final Collection newCol = cms.createCollection("createdByTest2")) {
                assertNotNull(newCol);
            }
        }
    }

    @Test
    public void groupCreateResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2")) {
            try (final Resource resource = test.createResource("createdByTest2.xml", XMLResource.class)) {
                resource.setContent("<testMe/>");
                test.storeResource(resource);
            }

            try (final Resource resource = test.getResource("createdByTest2.xml")) {
                assertNotNull(resource);
                assertEquals("<testMe/>", resource.getContent().toString());
            }
        }
    }

    @Test(expected=XMLDBException.class)
    public void groupRemoveCollection_canNotWriteParent() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", "test2", "test2")) {
            final CollectionManagementService cms = root.getService(CollectionManagementService.class);
            cms.removeCollection("securityTest1");
        }
    }

    @Test
    public void groupRemoveCollection_canWriteParent() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", "admin", "")) {
            final CollectionManagementService cms = root.getService(CollectionManagementService.class);
            cms.removeCollection("securityTest1");
        }
    }

    @Test(expected=XMLDBException.class)
    public void groupChmodCollection_asNotOwnerAndNotDBA() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // grant myself all rights ;-)
            ums.chmod(07777);
        }
    }

    @Test
    public void groupChmodCollection_asOwner() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            // grant myself all rights ;-)
            ums.chmod(07777);

            assertEquals("rwsrwsrwt", ums.getPermissions(test).toString());
        }
    }

    @Test(expected=XMLDBException.class)
    public void groupChmodResource_asNotOwnerAndNotDBA() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2");
             final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            // grant myself all rights ;-)
            ums.chmod(resource, 0777);
        }
    }

    @Test
    public void groupChmodResource_asOwner() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            // grant myself all rights ;-)
            ums.chmod(resource, 0777);
        }
    }

    /**
     * DBA can change the owner uid of a collection
     *
     * As the user 'admin' (who is a DBA) attempt to change the
     * ownership uid of /db/securityTest1
     * to 'test2' user
     */
    @Test
    public void dbaChownUidCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "admin", "")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to change uid ownership of /db/securityTest1 to the test2 user
            final Account test2 = ums.getAccount("test2");
            ums.chown(test2);
        }
    }

    /**
     * DBA can change the owner gid of a collection
     *
     * As the user 'admin' (who is a DBA) attempt to change the
     * ownership gid of /db/securityTest1
     * to 'guest' group
     */
    @Test
    public void dbaChownGidCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "admin", "")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to change uid ownership of /db/securityTest1 to the guest group
            ums.chgrp("guest");
        }
    }

    /**
     * Owner can NOT change the owner uid of a collection
     *
     * As the user 'test1' attempt to change the
     * ownership uid of /db/securityTest1
     * to 'test2' user
     */
    @Test(expected=XMLDBException.class)
    public void ownerChownUidCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to change uid ownership of /db/securityTest1 to the test2 user
            final Account test2 = ums.getAccount("test2");
            ums.chown(test2);
        }
    }

    /**
     * Owner can NOT change the owner gid of a collection
     * to a group of which they are not a member
     *
     * As the user 'test1' attempt to change the
     * ownership gid of /db/securityTest1
     * to 'guest' group
     */
    @Test(expected=XMLDBException.class)
    public void ownerChownGidCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to change gid ownership of /db/securityTest1 to the guest group
            ums.chgrp("guest");
        }
    }

    /**
     * Group member can NOT change the owner uid of a collection
     *
     * As the user 'test2' attempt to change the
     * ownership uid of /db/securityTest1
     * to ourselves
     */
    @Test(expected=XMLDBException.class)
    public void groupMemberChownUidCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to take uid ownership of /db/securityTest1
            final Account test2 = ums.getAccount("test2");
            ums.chown(test2);
        }
    }

    /**
     * Owner can change the owner gid of a collection
     * to a group of which they are a member
     *
     * As the user 'test1' (who is the owner and
     * who is in the group 'extusers')
     * attempt to change ownership gid of /db/securityTest1
     * to the group 'extusers'
     */
    @Test
    public void ownerAndGroupMemberChownGidCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to take gid ownership of /db/securityTest1
            ums.chgrp("extusers");

            final Permission perms = ums.getPermissions(test);
            assertEquals("extusers", perms.getGroup().getName());
        }
    }
    
    /**
     * Group Member can NOT change the owner gid of a resource
     * to a group of which they are a member
     *
     * As the user 'test2' (who is in the group users)
     * attempt to change ownership gid of /db/securityTest1 (which has uid 'test1' and gid 'users')
     * to the group 'test2-only' (of which they are a member)
     */
    @Test(expected=XMLDBException.class)
    public void groupMemberChownGidCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to have user 'test2' take gid ownership of /db/securityTest1 (which is owner by test1:users)
            ums.chgrp("test2-only");
        }
    }

    /**
     * Group Member can NOT change owner gid of a collection
     * to a group of which we are NOT a member
     *
     * As the user 'test2' (who is in the group users)
     * attempt to change ownership gid of /db/securityTest1
     * to the group 'guest' (of which they are NOT a member)
     */
    @Test(expected=XMLDBException.class)
    public void groupNonMemberChownGidCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to take gid ownership of /db/securityTest1
            ums.chgrp("guest");
        }
    }

    /**
     * DBA can change the owner uid of a resource
     *
     * As the user 'admin' (who is a DBA) attempt to change the
     * ownership uid of /db/securityTest1/test.xml
     * to 'test2' user
     */
    @Test
    public void dbaChownUidResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "admin", "");
             final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to change uid ownership of /db/securityTest1/test.xml to the test2 user
            final Account test2 = ums.getAccount("test2");
            ums.chown(resource, test2);
        }
    }

    /**
     * DBA can change the owner gid of a resource
     *
     * As the user 'admin' (who is a DBA) attempt to change the
     * ownership gid of /db/securityTest1/test1.xml
     * to 'guest' group
     */
    @Test
    public void dbaChownGidResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "admin", "");
             final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to change uid ownership of /db/securityTest1/test.xml to the guest group
            ums.chgrp(resource, "guest");
        }
    }

    /**
     * Owner can NOT change the owner uid of a resource
     *
     * As the user 'test1' attempt to change the
     * ownership uid of /db/securityTest1/test.xml
     * to 'test2' user
     */
    @Test(expected=XMLDBException.class)
    public void ownerChownUidResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to change uid ownership of /db/securityTest1/test.xml to the test2 user
            final Account test2 = ums.getAccount("test2");
            ums.chown(resource, test2);
        }
    }

    /**
     * Owner can NOT change the owner gid of a resource
     * to a group of which they are not a member
     *
     * As the user 'test1' attempt to change the
     * ownership gid of /db/securityTest1/test.xml
     * to 'guest' group
     */
    @Test(expected=XMLDBException.class)
    public void ownerChownGidResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
        final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to change gid ownership of /db/securityTest1/test.xml to the guest group
            ums.chgrp(resource, "guest");
        }
    }

    
    /**
     * Group member can NOT change the owner uid of a resource
     *
     * As the user 'test2' attempt to change the
     * ownership uid of /db/securityTest1/test.xml
     * to ourselves
     */
    @Test(expected=XMLDBException.class)
    public void groupMemberChownUidResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2");
             final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to take uid ownership of /db/securityTest1/test.xml
            final Account test2 = ums.getAccount("test2");
            ums.chown(resource, test2);
        }
    }

    /**
     * Owner can change the owner gid of a resource
     * to a group of which they are a member
     *
     * As the user 'test1' (who is the owner and
     * who is in the group 'extusers')
     * attempt to change ownership gid of /db/securityTest1/test.xml
     * to the group 'extusers'
     */
    @Test
    public void ownerAndGroupMemberChownGidResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
        final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to take gid ownership of /db/securityTest1
            ums.chgrp(resource, "extusers");

            final Permission perms = ums.getPermissions(resource);
            assertEquals("extusers", perms.getGroup().getName());
        }
    }
    
    /**
     * Group Member can NOT change the owner gid of a resource
     * to a group of which they are a member
     *
     * As the user 'test2' (who is in the group users)
     * attempt to change ownership gid of /db/securityTest1/test.xml (which has uid 'test1' and gid 'users')
     * to the group 'test2-only' (of which they are a member)
     */
    @Test(expected=XMLDBException.class)
    public void groupMemberChownGidResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2");
            final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to have user 'test2' take gid ownership of /db/securityTest1/test.xml (which is owned by test1:users)
            ums.chgrp(resource, "test2-only");
        }
    }

    /**
     * Group Member can NOT change owner gid of a resource
     * to a group of which we are NOT a member
     *
     * As the user 'test2' (who is in the group users)
     * attempt to change ownership gid of /db/securityTest1/test.xml
     * to the group 'guest' (of which they are NOT a member)
     */
    @Test(expected=XMLDBException.class)
    public void groupNonMemberChownGidResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2");
        final Resource resource = test.getResource("test.xml")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            // attempt to take gid ownership of /db/securityTest1/test.xml
            ums.chgrp(resource, "guest");
        }
    }

    @Test
    public void onlyExecuteRequiredToOpenCollectionContent() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            ums.chmod("--x------");
            test.close();

            try (final Collection reopened = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) { }
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotOpenCollectionWithoutExecute() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            ums.chmod("rw-rw-rw-");
            test.close();

            try (final Collection reopened = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) { }
        }
    }

    @Test
    public void canOpenCollectionWithExecute() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            ums.chmod("--x--x--x");
            test.close();

            try (final Collection reopened = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) { }
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotOpenRootCollectionWithoutExecute() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db", "admin", "")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            ums.chmod("rw-rw-rw-");
            test.close();

            try (final Collection reopened = DatabaseManager.getCollection(getBaseUri() + "/db", "test1", "test1")) { }
        }
    }

    @Test
    public void canOpenRootCollectionWithExecute() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db", "admin", "")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            ums.chmod("--x--x--x");
            test.close();

            try (final Collection reopened = DatabaseManager.getCollection(getBaseUri() + "/db", "test1", "test1")) { }
        }
    }

    @Test
    public void onlyReadAndExecuteRequiredToListCollectionResources() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            ums.chmod("r-x------");

            test.listResources();
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotListCollectionResourcesWithoutRead() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            ums.chmod("-wx-wx-wx");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            test.listResources();
        }
    }

    @Test
    public void onlyReadAndExecuteRequiredToListCollectionSubCollections() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            ums.chmod("r-x------");

            test.listChildCollections();
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotListCollectionSubCollectionsWithoutRead() throws XMLDBException {
        try (Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            ums.chmod("-wx-wx-wx");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            test.listChildCollections();
        }
    }

    @Test
    public void canReadXmlResourceWithOnlyExecutePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("--x------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.xml")) {
            assertEquals("<test/>", resource.getContent());
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotReadXmlResourceWithoutExecutePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("rw-------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.xml")) {
            assertEquals("<test/>", resource.getContent());
        }
    }

    @Test
    public void canReadBinaryResourceWithOnlyExecutePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("--x------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.bin")) {
            assertArrayEquals("binary-test".getBytes(), (byte[]) resource.getContent());
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotReadBinaryResourceWithoutExecutePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("rw-------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")){
            final Resource resource = test.getResource("test.bin");
            assertArrayEquals("binary-test".getBytes(), (byte[]) resource.getContent());
        }
    }

    @Test
    public void canReadXmlResourceWithOnlyReadPermission() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            try (final Resource resource = test.getResource("test.xml")) {
                ums.chmod(resource, "r--------");
            }
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.xml")) {
            assertEquals("<test/>", resource.getContent());
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotReadXmlResourceWithoutReadPermission() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            try (final Resource resource = test.getResource("test.xml")) {
                ums.chmod(resource, "-wx------");
            }
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.xml")) {
            assertEquals("<test/>", resource.getContent());
        }
    }

    @Test
    public void canReadBinaryResourceWithOnlyReadPermission() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            try (final Resource resource = test.getResource("test.bin")) {
                ums.chmod(resource, "r--------");
            }
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.bin")) {
            assertArrayEquals("binary-test".getBytes(), (byte[]) resource.getContent());
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotReadBinaryResourceWithoutReadPermission() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            try (final Resource resource = test.getResource("test.bin")) {
                ums.chmod(resource, "-wx------");
            }
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.bin")) {
            assertArrayEquals("binary-test".getBytes(), (byte[]) resource.getContent());
        }
    }

    @Test
    public void canCreateXmlResourceWithOnlyExecuteAndWritePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("-wx------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.createResource("other.xml", XMLResource.class)) {
            resource.setContent("<other/>");
            test.storeResource(resource);
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotCreateXmlResourceWithoutWritePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("--x------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
            final Resource resource = test.createResource("other.xml", XMLResource.class)) {
            resource.setContent("<other/>");
            test.storeResource(resource);
        }
    }

    @Test
    public void canCreateBinaryResourceWithOnlyExecuteAndWritePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("-wx------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.createResource("other.bin", BinaryResource.class)) {
            resource.setContent("binary".getBytes());
            test.storeResource(resource);
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotCreateBinaryResourceWithoutWritePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("--x------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.createResource("other.bin", BinaryResource.class)) {
                resource.setContent("binary".getBytes());
                test.storeResource(resource);
        }
    }

    @Test
    public void canUpdateXmlResourceWithOnlyExecutePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("--x------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {

            try (final Resource resource = test.getResource("test.xml")) {
                assertEquals("<test/>", resource.getContent());

                //update the resource
                resource.setContent("<testing/>");
                test.storeResource(resource);
            }

            try (final Resource resource = test.getResource("test.xml")) {
                assertEquals("<testing/>", resource.getContent());
            }
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotUpdateXmlResourceWithoutExecutePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("rw-------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.xml")) {
            assertEquals("<test/>", resource.getContent());

            // attempt to pdate the resource
            resource.setContent("<testing/>");
            test.storeResource(resource);
        }
    }

    @Test
    public void canUpdateBinaryResourceWithOnlyExecutePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("--x------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {

            try (final Resource resource = test.getResource("test.bin")) {
                assertArrayEquals("binary-test".getBytes(), (byte[]) resource.getContent());

                //update the resource
                resource.setContent("testing".getBytes());
                test.storeResource(resource);
            }

            try (final Resource resource = test.getResource("test.bin")) {
                assertArrayEquals("testing".getBytes(), (byte[]) resource.getContent());
            }
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotUpdateBinaryResourceWithoutExecutePermissionOnParentCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);
            ums.chmod("rw-------");
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource resource = test.getResource("test.bin")) {
            assertArrayEquals("binary-test".getBytes(), (byte[]) resource.getContent());

            //attempt to update the resource
            resource.setContent("testing".getBytes());
            test.storeResource(resource);
        }
    }

    @Test
    public void canExecuteXQueryWithOnlyExecutePermissionOnParentCollection() throws XMLDBException {
        final String xquery = "<xquery>{ 1 + 1 }</xquery>";

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            try (final Resource xqueryResource = test.createResource("test.xquery", BinaryResource.class)) {
                xqueryResource.setContent(xquery);
                test.storeResource(xqueryResource);

                ums.chmod("--x------");
                ums.chmod(xqueryResource, "rwx------"); //set execute bit on xquery (it's off by default!)
            }
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource xqueryResource = test.getResource("test.xquery")) {
            assertEquals(xquery, new String((byte[]) xqueryResource.getContent()));

            //execute the stored XQuery
            final EXistXPathQueryService queryService = test.getService(EXistXPathQueryService.class);
            try (final EXistResourceSet result = queryService.executeStoredQuery("/db/securityTest1/test.xquery")) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("<xquery>2</xquery>", resource.getContent());
                }
            }
        }
    }

    /**
     * Note the eventual goal is for XQuery to be executeable in eXist
     * with just the EXECUTE flag set, this however will require some
     * serious refactoring. See my (Adam) posts to exist-open thread entitled
     * '[HEADS-UP] Merge in of Security Branch', most significant
     * messages from 08/02/2012
     */
    @Test
    public void canExecuteXQueryWithOnlyExecuteAndReadPermission() throws XMLDBException {
        final String xquery = "<xquery>{ 1 + 2 }</xquery>";
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            try (final Resource xqueryResource = test.createResource("test.xquery", BinaryResource.class)) {
                xqueryResource.setContent(xquery);
                test.storeResource(xqueryResource);

                ums.chmod(xqueryResource, "r-x------"); //execute only on xquery
            }
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource xqueryResource = test.getResource("test.xquery")) {
            assertEquals(xquery, new String((byte[]) xqueryResource.getContent()));

            //execute the stored XQuery
            final EXistXPathQueryService queryService = test.getService(EXistXPathQueryService.class);
            try (final EXistResourceSet result = queryService.executeStoredQuery("/db/securityTest1/test.xquery")) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("<xquery>3</xquery>", resource.getContent());
                }
            }
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotExecuteXQueryWithoutExecutePermission() throws XMLDBException {
        final String xquery = "<xquery>{ 1 + 2 }</xquery>";

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final UserManagementService ums = test.getService(UserManagementService.class);

            try (final Resource xqueryResource = test.createResource("test.xquery", BinaryResource.class)) {
                xqueryResource.setContent(xquery);
                test.storeResource(xqueryResource);
                ums.chmod(xqueryResource, "rw-------"); //execute only on xquery
            }
        }

        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1");
             final Resource xqueryResource = test.getResource("test.xquery")) {
            assertEquals(xquery, new String((byte[]) xqueryResource.getContent()));

            //execute the stored XQuery
            final EXistXPathQueryService queryService = test.getService(EXistXPathQueryService.class);
            try (final EXistResourceSet result = queryService.executeStoredQuery("/db/securityTest1/test.xquery")) {
                try (final Resource resource = result.getResource(0)) {
                    assertEquals("<xquery>3</xquery>", resource.getContent());
                }
            }
        }
    }

    @Test(expected=XMLDBException.class)
    public void cannotOpenCollection() throws XMLDBException {
        //check that a user not in the users group (i.e. test3) cannot open the collection /db/securityTest1
        try (final Collection collection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test3", "test3")) {
            // needed to ensure that collection is closed
        }
    }

    @Test
    public void canOpenCollection() throws XMLDBException {
        //check that a user in the users group (i.e. test2) can open the collection /db/securityTest1
        try (final Collection collection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            // needed to ensure that collection is closed
        }
        try (final Collection collection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2")) {
            // needed to ensure that collection is closed
        }

        //check that any user can open the collection /db/securityTest3
        try (final Collection collection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            // needed to ensure that collection is closed
        }
        try (final Collection collection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test2", "test2")) {
            // needed to ensure that collection is closed
        }
        try (final Collection collection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            // needed to ensure that collection is closed
        }
    }

    @Test
    public void copyCollectionWithResources() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create collection owned by "test1", and group "users" in /db/securityTest3
            try (final Collection source = cms.createCollection("source")) {

                //create resource owned by "test1", and group "users" in /db/securityTest3/source
                try (final Resource resSource = source.createResource("source1.xml", XMLResource.class)) {
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }

                try (final Resource resSource = source.createResource("source2.xml", XMLResource.class)) {
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }
            }
        }

        //as the 'test3' user copy the collection
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("/db/securityTest3/source", "/db/securityTest3", "copy-of-source");

            try (final Collection copyOfSource = test.getChildCollection("copy-of-source")) {
                assertNotNull(copyOfSource);
                assertEquals(2, copyOfSource.listResources().size());
            }
        }
    }
    
    /**
     * As the 'test1' user, creates the collection and resource:
     *
     *  test1:users /db/securityTest3/source
     *  test1:users /db/securityTest3/source/source1.xml
     *  test1:users /db/securityTest3/source/source2.xml
     *
     * We then also create the Collection
     *  test1:users /db/securityTest3/copy-of-source (0777)
     * so that the destination (for the copy we are about
     * to do) already exists and is writable...
     * 
     * As the 'test3' user, copy the collection:
     *
     *  /db/securityTest3/source
     *      -> /db/securityTest3/copy-of-source
     */
    @Test
    public void copyCollectionWithResources_destExists_destIsWritable() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create collection owned by "test1", and group "users" in /db/securityTest3
            try (final Collection source = cms.createCollection("source")) {

                //create resource owned by "test1", and group "users" in /db/securityTest3/source
                try (final Resource resSource = source.createResource("source1.xml", XMLResource.class)) {
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }

                try (final Resource resSource = source.createResource("source2.xml", XMLResource.class)) {
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }
            }

            //pre-create the destination and set writable by all
            try (final Collection dest = cms.createCollection("copy-of-source")) {
                final UserManagementService ums = dest.getService(UserManagementService.class);
                ums.chmod(0777);
            }
        }

        //as the 'test3' user copy the collection
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("/db/securityTest3/source", "/db/securityTest3", "copy-of-source");

            try  (final Collection copyOfSource = test.getChildCollection("copy-of-source")) {
                assertNotNull(copyOfSource);
                assertEquals(2, copyOfSource.listResources().size());
            }
        }
    }
    
    /**
     * As the 'test1' user, creates the collection and resource:
     *
     *  test1:users /db/securityTest3/source
     *  test1:users /db/securityTest3/source/source1.xml
     *  test1:users /db/securityTest3/source/source2.xml
     *
     * We then also create the Collection
     *  test1:users /db/securityTest3/copy-of-source (0755)
     * so that the destination (for the copy we are about
     * to do) already exists and is NOT writable...
     * 
     * As the 'test3' user, copy the collection:
     *
     *  /db/securityTest3/source
     *      -> /db/securityTest3/copy-of-source
     */
    @Test(expected=XMLDBException.class)
    public void copyCollectionWithResources_destExists_destIsNotWritable() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create collection owned by "test1", and group "users" in /db/securityTest3
            try (final Collection source = cms.createCollection("source")) {

                //create resource owned by "test1", and group "users" in /db/securityTest3/source
                try (final Resource resSource = source.createResource("source1.xml", XMLResource.class)) {
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }

                try (final Resource resSource = source.createResource("source2.xml", XMLResource.class)) {
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }
            }

            //pre-create the destination with default mode (0755)
            //so that it is not writable by 'test3' user
            try (final Collection dest = cms.createCollection("copy-of-source")) {
                // needed to make sure that dest is closed
            }
        }
        
        //as the 'test3' user copy the collection
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("/db/securityTest3/source", "/db/securityTest3", "copy-of-source");
        }
    }
    
    /**
     * As the 'test1' user, creates the collection and resource:
     *
     *  test1:users /db/securityTest3/source
     *  test1:users /db/securityTest3/source/source1.xml
     *  test1:users /db/securityTest3/source/source2.xml
     *
     * We then also create the Collection
     *  test1:users /db/securityTest3/copy-of-source (0777)
     * so that the destination (for the copy we are about
     * to do) already exists and is writable.
     * We then create the resource
     *  test1:users /db/securityTest/copy-of-source/source1.xml
     * and set it so that it is not accessible by anyone
     * apart from 'test1' user...
     * 
     * As the 'test3' user, copy the collection:
     *
     *  /db/securityTest3/source
     *      -> /db/securityTest3/copy-of-source
     * 
     * The test should prove that during a copy, existing
     * documents in the dest are replaced as long as the
     * dest collection has write permission and that the
     * permissions on the dest resource must also be writable
     */
    @Test(expected=XMLDBException.class)
    public void copyCollectionWithResources_destResourceExists_destResourceIsNotWritable() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create collection owned by "test1", and group "users" in /db/securityTest3
            try (final Collection source = cms.createCollection("source")){

                //create resource owned by "test1", and group "users" in /db/securityTest3/source
                try (final Resource resSource = source.createResource("source1.xml", XMLResource.class)) {
                    resSource.setContent("<test1/>");
                    source.storeResource(resSource);
                }

                try (final Resource resSource = source.createResource("source2.xml", XMLResource.class)) {
                    resSource.setContent("<test2/>");
                    source.storeResource(resSource);
                }
            }

            //pre-create the destination and set writable by all
            try (final Collection dest = cms.createCollection("copy-of-source")) {
                final UserManagementService ums = dest.getService(UserManagementService.class);
                ums.chmod(0777);

                //pre-create a destination resource and set no access to group and others
                try (final Resource resDestSource1 = dest.createResource("source1.xml", XMLResource.class)) {
                    resDestSource1.setContent("<old/>");
                    dest.storeResource(resDestSource1);
                    ums.chmod(resDestSource1, 0700);
                }
            }
        }
        
        //as the 'test3' user copy the collection
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("/db/securityTest3/source", "/db/securityTest3", "copy-of-source");

            try (final Collection copyOfSource = test.getChildCollection("copy-of-source")) {
                assertNotNull(copyOfSource);
                assertEquals(2, copyOfSource.listResources().size());

                try (final Resource resCopyOfSource1 = copyOfSource.getResource("source1.xml")) {
                    assertEquals("<test1/>", resCopyOfSource1.getContent().toString());
                }

                try (final Resource resCopyOfSource2 = copyOfSource.getResource("source2.xml")) {
                    assertEquals("<test2/>", resCopyOfSource2.getContent().toString());
                }
            }

            //TODO check perms are/areNot preserved? on the replaced resource
        }
    }
    
    @Test
    public void copyCollectionWithResources_withSubCollectionWithResource() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create collection owned by "test1", and group "users" in /db/securityTest3
            try (final Collection source = cms.createCollection("source")) {

                //create resource owned by "test1", and group "users" in /db/securityTest3/source
                try (final Resource resSource = source.createResource("source1.xml", XMLResource.class)) {
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }

                try (final Resource resSource = source.createResource("source2.xml", XMLResource.class)){
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }

                //create sub-collection "sub" owned by "test1", and group "users" in /db/securityTest3/source
                final CollectionManagementService cms1 = source.getService(CollectionManagementService.class);
                try (final Collection sub = cms1.createCollection("sub")) {

                    //create resource owned by "test1", and group "users" in /db/securityTest3/source/sub1
                    try (final Resource resSub = sub.createResource("sub1.xml", XMLResource.class)) {
                        resSub.setContent("<test-sub/>");
                        sub.storeResource(resSub);
                    }
                }
            }
        }

        //as the 'test3' user copy the collection
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("/db/securityTest3/source", "/db/securityTest3", "copy-of-source");

            try (final Collection copyOfSource = test.getChildCollection("copy-of-source")) {
                assertNotNull(copyOfSource);
                assertEquals(2, copyOfSource.listResources().size());

                try (final Collection copyOfSub = copyOfSource.getChildCollection("sub")) {
                    assertNotNull(copyOfSub);
                    assertEquals(1, copyOfSub.listResources().size());
                }
            }
        }
    }

    @Test
    public void copyDocument_doesNotPreservePermissions() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create resource owned by "test1", and group "users" in /db/securityTest3
            try (final Resource resSource = test.createResource("source.xml", XMLResource.class)) {
                resSource.setContent("<test/>");
                test.storeResource(resSource);
            }
        }

        //as the 'test3' user copy the resource
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copyResource("/db/securityTest3/source.xml", "/db/securityTest3", "copy-of-source.xml");

            final UserManagementService ums = test.getService(UserManagementService.class);
            final Permission permissions = ums.getPermissions(test.getResource("copy-of-source.xml"));

            //resource should be owned by test3:guest, i.e. permissions were not preserved from the test1 users doc /db/securityTest3/source.xml
            assertEquals("test3", permissions.getOwner().getName());
            assertEquals("guest", permissions.getGroup().getName());
        }
    }
    
    @Test
    public void copyDocument_doesPreservePermissions_whenDestResourceExists() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create resource owned by "test1", and group "users" in /db/securityTest3
            try (final Resource resSource = test.createResource("source.xml", XMLResource.class)) {
                resSource.setContent("<test/>");
                test.storeResource(resSource);
            }

            //pre-create the dest resource (before the copy) and set writable by all
            try (final Resource resDest = test.createResource("copy-of-source.xml", XMLResource.class)) {
                resDest.setContent("<old/>");
                test.storeResource(resDest);

                final UserManagementService ums = test.getService(UserManagementService.class);
                ums.chmod(resDest, 0777);
            }
        }
        
        //as the 'test3' user copy the resource
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copyResource("/db/securityTest3/source.xml", "/db/securityTest3", "copy-of-source.xml");

            //as test3 user!
            final UserManagementService ums = test.getService(UserManagementService.class);
            final Permission permissions = ums.getPermissions(test.getResource("copy-of-source.xml"));

            //resource should be owned by test3:guest, i.e. permissions were not preserved from the test1 users doc /db/securityTest3/source.xml
            assertEquals("test1", permissions.getOwner().getName());
            assertEquals("users", permissions.getGroup().getName());

            //TODO copy collection should do the same??!?
        }
    }

    @Test
    public void copyCollection_doesNotPreservePermissions() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create collection owned by "test1", and group "users" in /db/securityTest3
            try (final Collection source = cms.createCollection("source")) {
                // needed to ensure that source is closed
            }
        }

        //as the 'test3' user copy the collection
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("/db/securityTest3/source", "/db/securityTest3", "copy-of-source");


            final UserManagementService ums = test.getService(UserManagementService.class);
            try (final Collection copyOfSource = test.getChildCollection("copy-of-source")) {
                final Permission permissions = ums.getPermissions(copyOfSource);

                //collection should be owned by test3:guest, i.e. permissions were not preserved from the test1 users collection /db/securityTest3/source
                assertEquals("test3", permissions.getOwner().getName());
                assertEquals("guest", permissions.getGroup().getName());
            }
        }
    }

    @Test
    public void copyCollection_doesPreservePermissions_whenDestCollectionExists() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create collection owned by "test1", and group "users" in /db/securityTest3
            try (final Collection source = cms.createCollection("source")) {

                //pre-create the dest collection and grant access to all (0777)
                try (final Collection dest = cms.createCollection("copy-of-source")) {
                    final UserManagementService ums = dest.getService(UserManagementService.class);
                    ums.chmod(0777);
                }
            }
        }

        //as the 'test3' user copy the collection
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("/db/securityTest3/source", "/db/securityTest3", "copy-of-source");

            //re-get ums as 'test3' user
            final UserManagementService ums = test.getService(UserManagementService.class);
            try (final Collection copyOfSource = test.getChildCollection("copy-of-source")) {
                final Permission permissions = ums.getPermissions(copyOfSource);

                //collection should STILL be owned by test1:users, i.e. permissions were preserved from the test1 users collection /db/securityTest3/copy-of-source
                assertEquals("test1", permissions.getOwner().getName());
                assertEquals("users", permissions.getGroup().getName());
            }
        }
    }

    @Test
    public void copyCollection_doesPreservePermissionsOfSubDocuments() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            cms.copy(XmldbURI.create("/db/securityTest1"), XmldbURI.create("/db/securityTest3"), XmldbURI.create("copy-of-securityTest1"));

            try (final Collection testCopy = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3/copy-of-securityTest1", "test1", "test1")) {

                final UserManagementService ums = testCopy.getService(UserManagementService.class);
                try (final Resource resource = testCopy.getResource("test.xml")) {
                    final Permission permissions = ums.getPermissions(resource);

                    assertEquals("test1", permissions.getOwner().getName());
                    assertEquals("users", permissions.getGroup().getName());
                    assertEquals(0770, permissions.getMode());
                }
            }
        }
    }
    
    @Test
    public void copyCollection_doesPreservePermissionsOfSubCollections() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            cms.copy(XmldbURI.create("/db/securityTest1"), XmldbURI.create("/db/securityTest3"), XmldbURI.create("copy-of-securityTest1"));

            try (final Collection testCopy = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3/copy-of-securityTest1", "test1", "test1");
                 final Collection sub1 = testCopy.getChildCollection("sub1")) {
                final UserManagementService ums = sub1.getService(UserManagementService.class);
                final Permission permissions = ums.getPermissions(sub1);

                assertEquals("test1", permissions.getOwner().getName());
                assertEquals("users", permissions.getGroup().getName());
                assertEquals(0777, permissions.getMode());
            }
        }
    }
    
    /**
     * As the 'test1' user, creates the collection and resource:
     *
     *  test1:users /db/securityTest3/source
     *  test1:users /db/securityTest3/source/source.xml
     *
     *
     * As the 'test3' user, copy the collection:
     *
     *  /db/securityTest3/source
     *      -> /db/securityTest3/copy-of-source
     */
    @Test
    public void copyCollectionWithResource_doesNotPreservePermissions() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create collection owned by "test1", and group "users" in /db/securityTest3
            try (final Collection source = cms.createCollection("source")) {

                //create resource owned by "test1", and group "users" in /db/securityTest3/source
                try (final Resource resSource = source.createResource("source.xml", XMLResource.class)) {
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }
            }
        }

        //as the 'test3' user copy the collection
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("/db/securityTest3/source", "/db/securityTest3", "copy-of-source");

            UserManagementService ums = test.getService(UserManagementService.class);
            try (final Collection copyOfSource = test.getChildCollection("copy-of-source")) {
                Permission permissions = ums.getPermissions(copyOfSource);

                //collection should be owned by test3:guest, i.e. permissions were not preserved from the test1 users doc /db/securityTest3/source
                assertEquals("test3", permissions.getOwner().getName());
                assertEquals("guest", permissions.getGroup().getName());

                ums = copyOfSource.getService(UserManagementService.class);
                try (final Resource resCopyOfSource = copyOfSource.getResource("source.xml")) {
                    permissions = ums.getPermissions(resCopyOfSource);

                    //resource in collection should be owned by test3:guest, i.e. permissions were not preserved from the test1 users doc /db/securityTest3/source.xml
                    assertEquals("test3", permissions.getOwner().getName());
                    assertEquals("guest", permissions.getGroup().getName());
                }
            }
        }
    }
    
    
    /**
     * As the 'test1' user, creates the collection and resource:
     *
     *  test1:users /db/securityTest3/source
     *  test1:users /db/securityTest3/source/source1.xml
     *  test1:users /db/securityTest3/source/source2.xml
     *  test1:users /db/securityTest3/source/sub
     *  test1:users /db/securityTest3/source/sub/sub1.xml
     *
     *
     * As the 'test3' user, copy the collection:
     *
     *  /db/securityTest3/source
     *      -> /db/securityTest3/copy-of-source
     */
    @Test
    public void copyCollectionWithResources_withSubCollectionWithResource_doesNotPreservePermissions() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create collection owned by "test1", and group "users" in /db/securityTest3
            try (final Collection source = cms.createCollection("source")) {

                //create resource owned by "test1", and group "users" in /db/securityTest3/source
                try (final Resource resSource = source.createResource("source1.xml", XMLResource.class)) {
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }

                try (final Resource resSource = source.createResource("source2.xml", XMLResource.class)) {
                    resSource.setContent("<test/>");
                    source.storeResource(resSource);
                }

                //create sub-collection "sub" owned by "test1", and group "users" in /db/securityTest3/source
                final CollectionManagementService cms1 = source.getService(CollectionManagementService.class);
                try (final Collection sub = cms1.createCollection("sub")) {

                    //create resource owned by "test1", and group "users" in /db/securityTest3/source/sub1
                    try (final Resource resSub = sub.createResource("sub1.xml", XMLResource.class)) {
                        resSub.setContent("<test-sub/>");
                        sub.storeResource(resSub);
                    }
                }
            }
        }

        //as the 'test3' user copy the collection
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("/db/securityTest3/source", "/db/securityTest3", "copy-of-source");

            try (final Collection copyOfSource = test.getChildCollection("copy-of-source")) {
                assertNotNull(copyOfSource);
                assertEquals(2, copyOfSource.listResources().size());

                try (final Collection copyOfSub = copyOfSource.getChildCollection("sub")) {
                    assertNotNull(copyOfSub);
                    assertEquals(1, copyOfSub.listResources().size());

                    //collection should be owned by test3:guest, i.e. permissions were not preserved from the test1 users doc /db/securityTest3/source
                    UserManagementService ums = test.getService(UserManagementService.class);
                    Permission permissions = ums.getPermissions(copyOfSource);
                    assertEquals("test3", permissions.getOwner().getName());
                    assertEquals("guest", permissions.getGroup().getName());

                    //resource in collection should be owned by test3:guest, i.e. permissions were not preserved from the test1 users doc /db/securityTest3/source/source1.xml
                    ums = copyOfSource.getService(UserManagementService.class);
                    try (final Resource resCopyOfSource1 = copyOfSource.getResource("source1.xml")) {
                        permissions = ums.getPermissions(resCopyOfSource1);
                        assertEquals("test3", permissions.getOwner().getName());
                        assertEquals("guest", permissions.getGroup().getName());
                    }

                    //resource in collection should be owned by test3:guest, i.e. permissions were not preserved from the test1 users doc /db/securityTest3/source/source2.xml
                    try (final Resource resCopyOfSource2 = copyOfSource.getResource("source2.xml")) {
                        permissions = ums.getPermissions(resCopyOfSource2);
                        assertEquals("test3", permissions.getOwner().getName());
                        assertEquals("guest", permissions.getGroup().getName());

                        //sub-collection should be owned by test3:guest, i.e. permissions were not preserved from the test1 users doc /db/securityTest3/source/sub
                        ums = copyOfSub.getService(UserManagementService.class);
                        permissions = ums.getPermissions(copyOfSub);
                        assertEquals("test3", permissions.getOwner().getName());
                        assertEquals("guest", permissions.getGroup().getName());
                    }

                    //sub-collection/resource should be owned by test3:guest, i.e. permissions were not preserved from the test1 users doc /db/securityTest3/source/sub/sub1.xml
                    try (final Resource resCopyOfSub1 = copyOfSub.getResource("sub1.xml")) {
                        permissions = ums.getPermissions(resCopyOfSub1);
                        assertEquals("test3", permissions.getOwner().getName());
                        assertEquals("guest", permissions.getGroup().getName());
                    }
                }
            }
        }
    }
    
    /**
     * As the 'test1' user, creates the collection and resource:
     *
     *  test1:users /db/securityTest3/source
     *  test1:users /db/securityTest3/source/source1.xml
     *  test1:users /db/securityTest3/source/source2.xml
     *
     * We then also create the Collection
     *  test1:users /db/securityTest3/copy-of-source (0777)
     * so that the destination (for the copy we are about
     * to do) already exists and is writable.
     * We then create the resource
     *  test1:users /db/securityTest/copy-of-source/source1.xml
     * and set it so that it is writable by all (0777)...
     * 
     * As the 'test3' user, copy the collection:
     *
     *  /db/securityTest3/source
     *      -> /db/securityTest3/copy-of-source
     * 
     * The test should prove that during a copy, existing
     * documents in the dest are replaced as long as the
     * dest collection has write permission and that the
     * permissions on the dest resource must also be writable
     * and that the existing permissions on the dest
     * resource will be preserved
     */
    @Test
    public void copyCollectionWithResources_destResourceExists_destResourceIsWritable_preservePermissions() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create collection owned by "test1", and group "users" in /db/securityTest3
            try (final Collection source = cms.createCollection("source")) {

                //create resource owned by "test1", and group "users" in /db/securityTest3/source
                try (final Resource resSource = source.createResource("source1.xml", XMLResource.class)) {
                    resSource.setContent("<test1/>");
                    source.storeResource(resSource);
                }

                try (final Resource resSource = source.createResource("source2.xml", XMLResource.class)) {
                    resSource.setContent("<test2/>");
                    source.storeResource(resSource);
                }
            }

            //pre-create the destination and set writable by all
            try (final Collection dest = cms.createCollection("copy-of-source")) {
                final UserManagementService ums = dest.getService(UserManagementService.class);
                ums.chmod(0777);

                //pre-create a destination resource and set access for all
                try (final Resource resDestSource1 = dest.createResource("source1.xml", XMLResource.class)) {
                    resDestSource1.setContent("<old/>");
                    dest.storeResource(resDestSource1);
                    ums.chmod(resDestSource1, 0777);
                }
            }
        }

        //as the 'test3' user copy the collection
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest3", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("/db/securityTest3/source", "/db/securityTest3", "copy-of-source");

            try (final Collection copyOfSource = test.getChildCollection("copy-of-source")) {
                assertNotNull(copyOfSource);
                assertEquals(2, copyOfSource.listResources().size());
                final UserManagementService ums = copyOfSource.getService(UserManagementService.class);

                //permissions should NOT have changed as the dest already existed!
                Permission permissions = ums.getPermissions(copyOfSource);
                assertEquals("test1", permissions.getOwner().getName());
                assertEquals("users", permissions.getGroup().getName());

                try (final Resource resCopyOfSource1 = copyOfSource.getResource("source1.xml")) {
                    assertEquals("<test1/>", resCopyOfSource1.getContent().toString());

                    //permissions should NOT have changed as the dest resource already existed!
                    permissions = ums.getPermissions(resCopyOfSource1);
                    assertEquals("test1", permissions.getOwner().getName());
                    assertEquals("users", permissions.getGroup().getName());
                }

                try (final Resource resCopyOfSource2 = copyOfSource.getResource("source2.xml")) {
                    assertEquals("<test2/>", resCopyOfSource2.getContent().toString());

                    //permissions SHOULD have changed as the dest resource is did NOT exist
                    permissions = ums.getPermissions(resCopyOfSource2);
                    assertEquals("test3", permissions.getOwner().getName());
                    assertEquals("guest", permissions.getGroup().getName());
                }
            }
        }
    }

    @Test
    public void setUidXQueryCanWriteRestrictedCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {

            final long timestamp = System.currentTimeMillis();
            final String content = "<setuid>" + timestamp + "</setuid>";

            //create an XQuery /db/securityTest1/setuid.xquery
            final String xquery = "xmldb:store('/db/securityTest1/forSetUidWrite', 'setuid.xml', " + content + ")";
            try (final Resource xqueryResource = test.createResource("setuid.xquery", BinaryResource.class)) {
                xqueryResource.setContent(xquery);
                test.storeResource(xqueryResource);
            }

            //set the xquery to be owned by 'test1' and set it 'setuid', and set it 'rx' by 'users' group so 'test2' can execute it!
            UserManagementService ums = test.getService(UserManagementService.class);
            try (final Resource xqueryResource = test.getResource("setuid.xquery")) {
                ums.chmod(xqueryResource, 04750);
            }

            //create a collection for the XQuery to write into
            final CollectionManagementService cms = test.getService(CollectionManagementService.class);
            try (final Collection colForSetUid = cms.createCollection("forSetUidWrite")) {
                //only allow the user 'test1' to write into the collection
                ums = colForSetUid.getService(UserManagementService.class);
                ums.chmod(0700);

                //execute the XQuery as the 'test2' user... it should become 'setuid' of 'test1' and succeed.
                try (final Collection test2 = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2")) {
                    final EXistXPathQueryService queryService = test2.getService(EXistXPathQueryService.class);
                    try (final EXistResourceSet result = queryService.executeStoredQuery("/db/securityTest1/setuid.xquery")) {
                        try (final Resource resource = result.getResource(0)) {
                            assertEquals("/db/securityTest1/forSetUidWrite/setuid.xml", resource.getContent());
                        }
                    }
                }

                //check the written content
                try (final Resource writtenXmlResource = colForSetUid.getResource("setuid.xml")) {
                    assertEquals(content, writtenXmlResource.getContent());
                }
            }
        }
    }

    @Test(expected=XMLDBException.class)
    public void nonSetUidXQueryCannotWriteRestrictedCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {

            final long timestamp = System.currentTimeMillis();
            final String content = "<not_setuid>" + timestamp + "</not_setuid>";

            //create an XQuery /db/securityTest1/not_setuid.xquery
            final String xquery = "xmldb:store('/db/securityTest1/forSetUidWrite', 'not_setuid.xml', " + content + ")";
            try (final Resource xqueryResource = test.createResource("not_setuid.xquery", BinaryResource.class)) {
                xqueryResource.setContent(xquery);
                test.storeResource(xqueryResource);
            }

            //set the xquery to be owned by 'test1' and do NOT set it 'setuid', and do set it 'rx' by 'users' group so 'test2' can execute it!
            UserManagementService ums = test.getService(UserManagementService.class);
            try (final Resource xqueryResource = test.getResource("not_setuid.xquery")) {
                ums.chmod(xqueryResource, 00750); //NOT SETUID
            }

            //create a collection for the XQuery to write into
            final CollectionManagementService cms = test.getService(CollectionManagementService.class);
            try (final Collection colForSetUid = cms.createCollection("forSetUidWrite")) {

                //only allow the user 'test1' to write into the collection
                ums = colForSetUid.getService(UserManagementService.class);
                ums.chmod(0700);

                //execute the XQuery as the 'test2' user... it should become 'setuid' of 'test1' and succeed.
                try (final Collection test2 = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test2", "test2")) {
                    final EXistXPathQueryService queryService = test2.getService(EXistXPathQueryService.class);
                    try (final EXistResourceSet result = queryService.executeStoredQuery("/db/securityTest1/not_setuid.xquery")) {
                        try (final Resource resource = result.getResource(0)) {
                            assertFalse("/db/securityTest1/forSetUidWrite/not_setuid.xml".equals(resource.getContent()));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void setGidXQueryCanWriteRestrictedCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test1", "test1")) {

            final long timestamp = System.currentTimeMillis();
            final String content = "<setgid>" + timestamp + "</setgid>";

            //create an XQuery /db/securityTest1/setuid.xquery
            final String xquery = "xmldb:store('/db/securityTest2/forSetGidWrite', 'setgid.xml', " + content + ")";
            try (final Resource xqueryResource = test.createResource("setgid.xquery", BinaryResource.class)) {
                xqueryResource.setContent(xquery);
                test.storeResource(xqueryResource);
            }

            //set the xquery to be owned by 'test1':'users' and set it 'setgid', and set it 'rx' by ohers, so 'test3' can execute it!
            UserManagementService ums = test.getService(UserManagementService.class);
            try (final Resource xqueryResource = test.getResource("setgid.xquery")) {
                ums.chown(xqueryResource, ums.getAccount("test1"), "users");
                ums.chmod(xqueryResource, 02705); //setgid
            }

            //create a collection for the XQuery to write into
            final CollectionManagementService cms = test.getService(CollectionManagementService.class);
            try (final Collection colForSetUid = cms.createCollection("forSetGidWrite")) {

                //only allow the group 'users' to write into the collection
                ums = colForSetUid.getService(UserManagementService.class);
                ums.chmod(0570);

                //execute the XQuery as the 'test3' user... it should become 'setgid' of 'users' and succeed.
                try (final Collection test3 = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test3", "test3")) {
                    final EXistXPathQueryService queryService = test3.getService(EXistXPathQueryService.class);
                    try (final EXistResourceSet result = queryService.executeStoredQuery("/db/securityTest2/setgid.xquery")) {
                        try (final Resource resource = result.getResource(0)) {
                            assertEquals("/db/securityTest2/forSetGidWrite/setgid.xml", resource.getContent());
                        }
                    }

                    //check the written content
                    try (final Resource writtenXmlResource = colForSetUid.getResource("setgid.xml")) {
                        assertEquals(content, writtenXmlResource.getContent());
                    }
                }
            }
        }
    }
    
    @Test(expected=XMLDBException.class)
    public void nonSetGidXQueryCannotWriteRestrictedCollection() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test1", "test1")) {

            final long timestamp = System.currentTimeMillis();
            final String content = "<not_setgid>" + timestamp + "</not_setgid>";

            //create an XQuery /db/securityTest1/not_setgid.xquery
            final String xquery = "xmldb:store('/db/securityTest2/forSetGidWrite', 'not_setgid.xml', " + content + ")";
            try (final Resource xqueryResource = test.createResource("not_setgid.xquery", BinaryResource.class)) {
                xqueryResource.setContent(xquery);
                test.storeResource(xqueryResource);
            }

            //set the xquery to be owned by 'test1':'users' and set it 'setgid', and set it 'rx' by ohers, so 'test3' can execute it!
            UserManagementService ums = test.getService(UserManagementService.class);
            try (final Resource xqueryResource = test.getResource("not_setgid.xquery")) {
                ums.chmod(xqueryResource, 00705); //NOT setgid
            }

            //create a collection for the XQuery to write into
            final CollectionManagementService cms = test.getService(CollectionManagementService.class);
            try (final Collection colForSetUid = cms.createCollection("forSetGidWrite")) {

                //only allow the group 'users' to write into the collection
                ums = colForSetUid.getService(UserManagementService.class);
                ums.chmod(0070);

                //execute the XQuery as the 'test3' user... it should become 'setgid' of 'users' and succeed.
                try (final Collection test3 = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test3", "test3")) {
                    final EXistXPathQueryService queryService = test3.getService(EXistXPathQueryService.class);
                    try (final EXistResourceSet result = queryService.executeStoredQuery("/db/securityTest2/not_setgid.xquery")) {
                        try (final Resource resource = result.getResource(0)) {
                            assertFalse("/db/securityTest2/forSetGidWrite/not_setgid.xml".equals(resource.getContent()));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void noSetGid_createSubCollection_subCollectionGroupIsUsersPrimaryGroup() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test1", "test1")) {
            CollectionManagementService cms = test.getService(CollectionManagementService.class);

            UserManagementService ums;
            //create /db/securityTest2/parentCollection with owner "test1:users" and mode "rwxr--rwx"
            try (final Collection parentCollection = cms.createCollection("parentCollection")) {
                ums = parentCollection.getService(UserManagementService.class);
                ums.chmod("rwxr--rwx");
            }

            //now create the sub-collection /db/securityTest2/parentCollection/subCollection1
            //as "user3:guest", it should have it's group set to the primary group of user3 i.e. 'guest'
            //as the collection is NOT setUid and it should NOT have the setGid bit set
            try (final Collection parentCollection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2/parentCollection", "test3", "test3")) {
                ums = parentCollection.getService(UserManagementService.class);
                cms = parentCollection.getService(CollectionManagementService.class);
                try (final Collection subCollection = cms.createCollection("subCollection1")) {

                    final Permission permissions = ums.getPermissions(subCollection);
                    assertEquals("guest", permissions.getGroup().getName());
                    assertFalse(permissions.isSetGid());
                }
            }
        }
    }

    @Test
    public void setGid_createSubCollection_subCollectionGroupInheritedFromParent() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test1", "test1")) {
            CollectionManagementService cms = test.getService(CollectionManagementService.class);

            //create /db/securityTest2/parentCollection with owner "test1:users" and mode "rwxrwsrwx"
            UserManagementService ums;
            try (final Collection parentCollection = cms.createCollection("parentCollection")) {
                ums = parentCollection.getService(UserManagementService.class);
                ums.chmod("rwxrwsrwx");
            }

            //now create the sub-collection /db/securityTest2/parentCollection/subCollection1
            //it should inherit the group ownership 'users' from the parent collection which is setGid
            //and it should inherit the setGid bit
            try (final Collection parentCollection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2/parentCollection", "test3", "test3")) {
                ums = parentCollection.getService(UserManagementService.class);
                cms = parentCollection.getService(CollectionManagementService.class);
                try (final Collection subCollection = cms.createCollection("subCollection1")) {

                    final Permission permissions = ums.getPermissions(subCollection);
                    assertEquals("users", permissions.getGroup().getName());
                    assertTrue(permissions.isSetGid());
                }
            }
        }
    }

    @Test
    public void noSetGid_createResource_resourceGroupIsUsersPrimaryGroup() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test1", "test1")) {
            CollectionManagementService cms = test.getService(CollectionManagementService.class);

            UserManagementService ums;
            //create /db/securityTest2/parentCollection with owner "test1:users" and mode "rwxrwxrwx"
            try (final Collection parentCollection = cms.createCollection("parentCollection")) {
                ums = parentCollection.getService(UserManagementService.class);
                ums.chmod("rwxrwxrwx");
            }

            //now create the sub-resource /db/securityTest2/parentCollection/test.xml
            //as "user3:guest", it should have it's group set to the primary group of user3 i.e. 'guest'
            //as the collection is NOT setGid, the file should NOT have the setGid bit set
            try (final Collection parentCollection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2/parentCollection", "test3", "test3")) {
                ums = parentCollection.getService(UserManagementService.class);
                try (final Resource resource = parentCollection.createResource("test.xml", XMLResource.class)) {
                    resource.setContent("<test/>");
                    parentCollection.storeResource(resource);

                    final Permission permissions = ums.getPermissions(resource);
                    assertEquals("guest", permissions.getGroup().getName());
                    assertFalse(permissions.isSetGid());
                }
            }
        }
    }

    @Test
    public void setGid_createResource_resourceGroupInheritedFromParent() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test1", "test1")) {
            CollectionManagementService cms = test.getService(CollectionManagementService.class);

            UserManagementService ums;
            //create /db/securityTest2/parentCollection with owner "test1:users" and mode "rwxrwsrwx"
            try (final Collection parentCollection = cms.createCollection("parentCollection")) {
                ums = parentCollection.getService(UserManagementService.class);
                ums.chmod("rwxrwsrwx");
            }

            //now as "test3:guest" create the sub-resource /db/securityTest2/parentCollection/test.xml
            //it should inherit the group ownership 'users' from the parent which is setGid
            //but it should not inherit the setGid bit as it is a resource
            try (final Collection parentCollection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2/parentCollection", "test3", "test3")) {
                ums = parentCollection.getService(UserManagementService.class);
                try (final Resource resource = parentCollection.createResource("test.xml", XMLResource.class)) {
                    resource.setContent("<test/>");
                    parentCollection.storeResource(resource);

                    final Permission permissions = ums.getPermissions(resource);
                    assertEquals("users", permissions.getGroup().getName());
                    assertFalse(permissions.isSetGid());
                }
            }
        }
    }

    @Test
    public void noSetGid_copyCollection_collectionGroupIsUsersPrimaryGroup() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create the /db/securityTest2/src collection
            try (final Collection srcCollection = cms.createCollection("src")) {
                // needed to ensure that srcCollection is closed
            }

            //create /db/securityTest2/parentCollection with owner "test1:users" and mode "rwxrwxrwx"
            try (final Collection parentCollection = cms.createCollection("parentCollection")) {
                final UserManagementService ums = parentCollection.getService(UserManagementService.class);
                ums.chmod("rwxrwxrwx");
            }
        }

        //now copy /db/securityTest2/src to /db/securityTest2/parentCollection/src
        //as "user3:guest", it should have it's group set to the primary group of "user3" i.e. 'guest'
        //as the collection is NOT setGid and it should NOT have it's setGid bit set
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("src", "/db/securityTest2/parentCollection", "src");
            try (final Collection parentCollection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2/parentCollection", "test3", "test3")) {
                final UserManagementService ums = parentCollection.getService(UserManagementService.class);

                try (final Collection srcCollection = test.getChildCollection("src");
                     final Collection destCollection = parentCollection.getChildCollection("src")) {

                    final Permission permissions = ums.getPermissions(destCollection);
                    assertEquals("guest", permissions.getGroup().getName());
                    assertFalse(permissions.isSetGid());
                }
            }
        }
    }
    
    @Test
    public void setGid_copyCollection_collectionGroupInheritedFromParent() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create the /db/securityTest2/src collection with owner "test1:extusers" and default mode
            try (final Collection srcCollection = cms.createCollection("src")) {
                final UserManagementService ums = srcCollection.getService(UserManagementService.class);
                ums.chgrp("extusers");
            }

            //create /db/securityTest2/parentCollection with owner "test1:users" and mode "rwxrwsrwx"
            try (final Collection parentCollection = cms.createCollection("parentCollection")) {
                final UserManagementService ums = parentCollection.getService(UserManagementService.class);
                ums.chmod("rwxrwsrwx");
            }
        }

        //now copy /db/securityTest2/src to /db/securityTest2/parentCollection/src
        //as "user3:guest", it should inherit the group ownership 'users' from the parent
        //collection which is setGid and it should have its setGid bit set
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copy("src", "/db/securityTest2/parentCollection", "src");
            try (final Collection parentCollection = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2/parentCollection", "test3", "test3")) {
                final UserManagementService ums = parentCollection.getService(UserManagementService.class);

                try (final Collection destCollection = parentCollection.getChildCollection("src")) {
                    final Permission permissions = ums.getPermissions(destCollection);
                    assertEquals("users", permissions.getGroup().getName());
                    assertTrue(permissions.isSetGid());
                }
            }
        }
    }

    @Test
    public void noSetGid_copyResource_resourceGroupIsUsersPrimaryGroup() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);

            //create the /db/securityTest2/test.xml resource
            try (final Resource resource = test.createResource("test.xml", XMLResource.class)) {
                resource.setContent("<test/>");
                test.storeResource(resource);
            }

            //create /db/securityTest2/parentCollection with owner "test1:users" and mode "rwxrwxrwx"
            try (final Collection parentCollection = cms.createCollection("parentCollection")) {
                final UserManagementService ums = parentCollection.getService(UserManagementService.class);
                ums.chmod("rwxrwxrwx");
            }
        }

        //now copy /db/securityTest2/test.xml to /db/securityTest2/parentCollection/test.xml
        //as user3, it should have it's group set to the primary group of user3 i.e. 'guest'
        //as the collection is NOT setGid and it should not have the setGid bit
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copyResource("test.xml", "/db/securityTest2/parentCollection", "test.xml");

            try (final Collection parentCollection = test.getChildCollection("parentCollection");
                 final Resource resource = parentCollection.getResource("test.xml")) {
                final UserManagementService ums = parentCollection.getService(UserManagementService.class);
                final Permission permissions = ums.getPermissions(resource);
                assertEquals("guest", permissions.getGroup().getName());
                assertFalse(permissions.isSetGid());
            }
        }
    }

    @Test
    public void setGid_copyResource_resourceGroupInheritedFromParent() throws XMLDBException {
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test1", "test1")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            UserManagementService ums = test.getService(UserManagementService.class);

            //create the /db/securityTest2/test.xml resource
            try (final Resource resource = test.createResource("test.xml", XMLResource.class)) {
                resource.setContent("<test/>");
                test.storeResource(resource);
                ums.chgrp(resource, "extusers");
            }

            //create /db/securityTest2/parentCollection with owner "test1:users" and mode "rwxrwsrwx"
            try (final Collection parentCollection = cms.createCollection("parentCollection")) {
                ums = parentCollection.getService(UserManagementService.class);
                ums.chmod("rwxrwsrwx");
            }
        }

        //now copy /db/securityTest2/test.xml to /db/securityTest2/parentCollection/test.xml
        //as "user3:guest", it should inherit the group ownership 'users' from the parent collection which is setGid
        //and it should NOT have its setGid bit set as it is a resource
        try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest2", "test3", "test3")) {
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) test.getService(CollectionManagementService.class);
            cms.copyResource("test.xml", "/db/securityTest2/parentCollection", "test.xml");

            try (final Collection parentCollection = test.getChildCollection("parentCollection");
                 final Resource resource = parentCollection.getResource("test.xml")) {

                final UserManagementService ums = parentCollection.getService(UserManagementService.class);
                final Permission permissions = ums.getPermissions(resource);
                assertEquals("users", permissions.getGroup().getName());
                assertFalse(permissions.isSetGid());
            }
        }
    }

    
    //TODO need tests for
    //4) CopyingCollections to dests where permission is denied!
    //5) What about move Document, move Collection?
    
    /**
     * 1) Sets '/db' to rwxr-xr-x (0755)
     * 2) Adds the Group 'users'
     * 3) Adds the User 'test1' with password 'test1' and set's their primary group to 'users'
     * 4) Creates the group 'extusers' and adds 'test1' to it
     * 5) Adds the User 'test2' with password 'test2' and set's their primary group to 'users'
     * 6) Creates the group 'test2-only` and adds 'test2' to it
     * 7) Adds the User 'test3' with password 'test3' and set's their primary group to 'guest'
     * 8) Creates the Collection '/db/securityTest1' owned by 'test1':'users' with permissions rwxrwx--- (0770)
     * 9) Creates the XML resource '/db/securityTest1/test.xml' owned by 'test1':'users' with permissions rwxrwx--- (0770)
     * 10) Creates the Binary resource '/db/securityTest1/test.bin' owned by 'test1':'users' with permissions rwxrwx--- (0770)
     * 11) Creates the Collection '/db/securityTest2' owned by 'test1':'users' with permissions rwxrwxr-x (0775)
     * 12) Creates the Collection '/db/securityTest3' owned by 'test3':'guest' with permissions rwxrwxrwx (0777)
     */
    @Before
    public void setup() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
            final UserManagementService rootUms = root.getService(UserManagementService.class);

            rootUms.chmod("rwxr-xr-x"); //ensure /db is always 755

            //remove accounts 'test1', 'test2' and 'test3'
            removeAccounts(rootUms, new String[]{"test1", "test2", "test3"});

            //remove group 'users'
            removeGroups(rootUms, new String[]{"users"});

            final Group group = new GroupAider("exist", "users");
            rootUms.addGroup(group);

            UserAider user = new UserAider("test1", group);
            user.setPassword("test1");
            rootUms.addAccount(user);

            final Group extGroup = new GroupAider("exist", "extusers");
            rootUms.addGroup(extGroup);
            rootUms.addAccountToGroup("test1", "extusers");

            user = new UserAider("test2", group);
            user.setPassword("test2");
            rootUms.addAccount(user);

            final Group test2OnlyGroup = new GroupAider("exist", "test2-only");
            rootUms.addGroup(test2OnlyGroup);
            rootUms.addAccountToGroup("test2", "test2-only");

            final Group guestGroup = rootUms.getGroup("guest");
            user = new UserAider("test3", guestGroup);
            user.setPassword("test3");
            rootUms.addAccount(user);

            // create a collection /db/securityTest1 as owned by "test1:users" and mode 0770
            CollectionManagementService cms = root.getService(CollectionManagementService.class);
            try (final Collection test = cms.createCollection("securityTest1")) {
                final UserManagementService securityTest1Ums = test.getService(UserManagementService.class);
                //change ownership to test1
                final Account test1 = securityTest1Ums.getAccount("test1");
                securityTest1Ums.chown(test1, "users");
                // full permissions for user and group, none for world
                securityTest1Ums.chmod(0770);
            }

            try (final Collection test = DatabaseManager.getCollection(getBaseUri() + "/db/securityTest1", "test1", "test1")) {
                final UserManagementService testUms = test.getService(UserManagementService.class);

                // create a resource /db/securityTest1/test.xml owned by "test1:users" and mode 0770
                try (final Resource resource = test.createResource("test.xml", XMLResource.class)) {
                    resource.setContent("<test/>");
                    test.storeResource(resource);
                    testUms.chmod(resource, 0770);
                }

                try (final Resource resource = test.createResource("test.bin", BinaryResource.class)) {
                    resource.setContent("binary-test".getBytes());
                    test.storeResource(resource);
                    testUms.chmod(resource, 0770);
                }

                // create a collection /db/securityTest2 as user "test1"
                cms = root.getService(CollectionManagementService.class);
                try (final Collection testCol2 = cms.createCollection("securityTest2")) {
                    final UserManagementService testCol2Ums = testCol2.getService(UserManagementService.class);
                    //change ownership to test1
                    final Account test1 = testCol2Ums.getAccount("test1");
                    testCol2Ums.chown(test1, "users");
                    // full permissions for user and group, none for world
                    testCol2Ums.chmod(0775);
                }

                // create a collection /db/securityTest3 as user "test3"
                cms = root.getService(CollectionManagementService.class);
                try (final Collection testCol3 = cms.createCollection("securityTest3")) {
                    final UserManagementService testCol3Ums = testCol3.getService(UserManagementService.class);
                    //change ownership to test3
                    final Account test3 = testCol3Ums.getAccount("test3");
                    testCol3Ums.chown(test3, "users");
                    // full permissions for all
                    testCol3Ums.chmod(0777);
                }

                // create a sub-collection /db/securityTest1/sub1 as user "test1"
                cms = test.getService(CollectionManagementService.class);
                try (final Collection sub1 = cms.createCollection("sub1")) {
                    final UserManagementService sub1Ums = sub1.getService(UserManagementService.class);
                    //change ownership to test1
                    final Account test1 = sub1Ums.getAccount("test1");
                    sub1Ums.chown(test1, "users");
                    // full permissions for all
                    sub1Ums.chmod(0777);
                }
            }
        }
    }

    @After
    public void cleanup() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
            final CollectionManagementService cms = root.getService(CollectionManagementService.class);

            try (final Collection secTest1 = root.getChildCollection("securityTest1")) {
                if (secTest1 != null) {
                    secTest1.close();
                    cms.removeCollection("securityTest1");
                }
            }

            try (final Collection secTest2 = root.getChildCollection("securityTest2")) {
                if (secTest2 != null) {
                    secTest2.close();
                    cms.removeCollection("securityTest2");
                }
            }

            try (final Collection secTest3 = root.getChildCollection("securityTest3")) {
                if (secTest3 != null) {
                    secTest3.close();
                    cms.removeCollection("securityTest3");
                }
            }

            final UserManagementService ums = root.getService(UserManagementService.class);

            //remove accounts 'test1', 'test2' and 'test3'
            removeAccounts(ums, new String[]{"test1", "test2", "test3"});

            //remove group 'users', 'extusers', 'test2-only'
            removeGroups(ums, new String[]{"users", "extusers", "test2-only"});
        }
    }

    private void removeAccounts(final UserManagementService ums, final String[] accountNames) throws XMLDBException {
        final Account[] accounts = ums.getAccounts();
        for(final Account account: accounts) {
            for(final String accountName: accountNames) {
                if(account.getName().equals(accountName)) {
                    ums.removeAccount(account);
                    break;
                }
            }
        }
    }

    private void removeGroups(final UserManagementService ums, final String[] groupNames) throws XMLDBException {
        final String[] groups = ums.getGroups();
        for(final String group: groups) {
            for(final String groupName : groupNames) {
                if(group.equals(groupName)) {
                    ums.removeGroup(ums.getGroup(group));
                    break;
                }
            }
        }
    }
}
