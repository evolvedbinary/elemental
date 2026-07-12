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
package org.exist.xmldb;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.After;
import org.exist.security.Account;
import org.exist.security.Permission;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.exist.TestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XPathQueryService;


public class CopyMoveTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    private final static String TEST_COLLECTION = "testCopyMove";

    @Test
    public void copyResourceChangeName() throws XMLDBException {
        try (final Collection testCollection = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + TEST_COLLECTION)) {
            try (final EXistResource original = (EXistResource) testCollection.createResource("original", "XMLResource")) {
                original.setContent("<sample/>");
                testCollection.storeResource(original);
            }
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) testCollection.getService("CollectionManagementService", "1.0");
            cms.copyResource("original", "", "duplicate");
            assertEquals(2, testCollection.getResourceCount());
            try (final EXistResource duplicate = (EXistResource) testCollection.getResource("duplicate")) {
                assertNotNull(duplicate);
            }
        }
    }

    @Test
    public void queryCopiedResource() throws XMLDBException {
        try (final Collection testCollection = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + TEST_COLLECTION)) {
            try (final EXistResource original = (EXistResource)testCollection.createResource("original", "XMLResource")) {
                original.setContent("<sample/>");
                testCollection.storeResource(original);
            }
            final EXistCollectionManagementService cms = (EXistCollectionManagementService) testCollection.getService("CollectionManagementService", "1.0");
            cms.copyResource("original", "", "duplicate");
            try (final EXistResource duplicate = (EXistResource) testCollection.getResource("duplicate")) {
                assertNotNull(duplicate);
            }
            final XPathQueryService xq = (XPathQueryService) testCollection.getService("XPathQueryService", "1.0");
            try (final EXistResourceSet rs = (EXistResourceSet) xq.queryResource("duplicate", "/sample")) {
                assertEquals(1, rs.getSize());
            }
        }
    }
    
    @Test
    public void changePermissionsAfterCopy() throws XMLDBException {
        final String collectionURL = XmldbURI.LOCAL_DB + "/" + TEST_COLLECTION;
        final String originalResource = "original.xml";
        final String copyResource = "copy.xml";
        
        final String resourceURL = collectionURL + "/" + originalResource;
        
        //get collection & services
        try (final EXistCollection col = (EXistCollection)DatabaseManager.getCollection(collectionURL)) {
            final EXistCollectionManagementService service = (EXistCollectionManagementService) col.getService("CollectionManagementService", "1.0");
            final UserManagementService ums = (UserManagementService) DatabaseManager.getCollection(collectionURL, ADMIN_DB_USER, ADMIN_DB_PWD).getService("UserManagementService", "1.0");

            //store xml document
            try (final EXistResource original = (EXistResource)col.createResource(originalResource, "XMLResource")) {
                original.setContent("<sample/>");
                col.storeResource(original);
            }

            //get original resource
            try (final EXistResource orgnRes = (EXistResource) col.getResource(originalResource)) {

                //check permission before copy
                Permission prm = ums.getPermissions(orgnRes);
                assertEquals("rw-r--r--", prm.toString());


                //copy
                service.copyResource(XmldbURI.create(resourceURL), col.getPathURI(), XmldbURI.create(copyResource));

                //check permission after copy
                prm = ums.getPermissions(orgnRes);
                assertEquals("rw-r--r--", prm.toString());

                //get copy resource
                try (final EXistResource copyRes = (EXistResource) col.getResource(copyResource)) {

                    //change permission on copy
                    final Account admin = ums.getAccount(ADMIN_DB_USER);
                    ums.chown(copyRes, admin, admin.getPrimaryGroup());
                    ums.chmod(copyRes, "rwx--x---");

                    //check permission of copy
                    prm = ums.getPermissions(copyRes);
                    assertEquals("rwx--x---", prm.toString());
                }

                //check permission of original
                prm = ums.getPermissions(orgnRes);
                assertEquals("rw-r--r--", prm.toString());
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        final CollectionManagementService cms = (CollectionManagementService) existEmbeddedServer.getRoot().getService("CollectionManagementService", "1.0");
        try (final Collection testCollection = cms.createCollection(TEST_COLLECTION)) {
            final UserManagementService ums = (UserManagementService) testCollection.getService("UserManagementService", "1.0");
            // change ownership to guest
            final Account guest = ums.getAccount(GUEST_DB_USER);
            ums.chown(guest, guest.getPrimaryGroup());
            ums.chmod("rwxr-xr-x");
        }
    }

    @After
    public void tearDown() throws XMLDBException {
        //delete the test collection
        final CollectionManagementService cms = (CollectionManagementService)existEmbeddedServer.getRoot().getService("CollectionManagementService", "1.0");
        cms.removeCollection(TEST_COLLECTION);
    }
}
