/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.exist.security.Account;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.util.io.InputStreamUtil;
import org.junit.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.TestUtils.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.exist.samples.Samples.SAMPLES;

import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.Service;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.*;

public class CreateCollectionsTest  {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    private final static String TEST_COLLECTION = "testCreateCollection";

    @Before
    public void setUp() throws XMLDBException {
        //create a test collection
        final CollectionManagementService cms = existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
        final Collection test = cms.createCollection(TEST_COLLECTION);
        final UserManagementService ums = test.getService(UserManagementService.class);
        // change ownership to guest
        Account guest = ums.getAccount(GUEST_DB_USER);
        ums.chown(guest, guest.getPrimaryGroup());
        ums.chmod("rwxrwxrwx");
    }

    @After
    public void tearDown() throws XMLDBException {
        //delete the test collection
        final CollectionManagementService cms = existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
        cms.removeCollection(TEST_COLLECTION);
    }

    @Test
    public void rootCollectionHasNoParent() throws XMLDBException {
        final Collection root = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, ADMIN_DB_USER, ADMIN_DB_PWD);
        assertNull("root collection has no parent", root.getParentCollection());
    }

    @Test
    public void collectionMustProvideAtLeastOneService() throws XMLDBException {
        final Collection colTest = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + TEST_COLLECTION);
        final List<Class<? extends Service>> expectedServiceTypes = Arrays.asList(CollectionManagementService.class,
                DatabaseInstanceManager.class, EXistCollectionManagementService.class, EXistRestoreService.class,
                EXistUserManagementService.class, IndexQueryService.class, UserManagementService.class,
                XPathQueryService.class, XQueryService.class, XUpdateQueryService.class);
        for (Class<? extends Service> expectedServiceType : expectedServiceTypes) {
            assertTrue(colTest.hasService(expectedServiceType));
            assertNotNull(colTest.getService(expectedServiceType));
        }
    }

    @Test
    public void createCollection_hasNoSubCollections_andIsOpen() throws XMLDBException {
        final Collection colTest = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + TEST_COLLECTION);
        final CollectionManagementService service = colTest.getService(CollectionManagementService.class);
        final Collection testCollection = service.createCollection("test");
        assertNotNull(testCollection);

        assertEquals("Created Collection has zero child collections", 0, testCollection.getChildCollectionCount());
        assertTrue("Created Collection state should be Open after creation", testCollection.isOpen());
    }

    @Test
    public void storeSamplesShakespeare() throws XMLDBException, IOException, URISyntaxException {
        final Collection colTest = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + TEST_COLLECTION);
        final CollectionManagementService service = colTest.getService(CollectionManagementService.class);
        final Collection testCollection = service.createCollection("test");
        UserManagementService ums = testCollection.getService(UserManagementService.class);
        ums.chmod("rwxr-xr-x");

        final List<String> storedResourceNames = new ArrayList<>();
        final List<String> filenames = new ArrayList<>();

        //store the samples
        for (final String sampleName : SAMPLES.getShakespeareXmlSampleNames()) {
            final Resource res = storeResourceFromFile(SAMPLES.getShakespeareSample(sampleName), testCollection, sampleName);
            storedResourceNames.add(res.getId());
            filenames.add(sampleName);
        }

        assertEquals(filenames, storedResourceNames);

        //get a list from the database of stored resource names
        final List<String> retrievedStoredResourceNames = testCollection.listResources();

        //order of names from database may not be the order in which the files were loaded!
        Collections.sort(filenames);
        Collections.sort(retrievedStoredResourceNames);

        assertEquals(filenames, retrievedStoredResourceNames);
    }

    @Test
    public void storeRemoveStoreResource() throws XMLDBException, IOException, URISyntaxException {
        final Collection colTest = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + TEST_COLLECTION);
        final CollectionManagementService service = colTest.getService(CollectionManagementService.class);
        final Collection testCollection = service.createCollection("test");
        UserManagementService ums = testCollection.getService(UserManagementService.class);
        ums.chmod("rwxr-xr-x");

        final String testFile = "macbeth.xml";
        try (final InputStream is = SAMPLES.getMacbethSample()) {
            storeResourceFromFile(is, testCollection, testFile);
        }
        Resource resMacbeth = testCollection.getResource(testFile);
        assertNotNull("getResource(" + testFile + "\")", resMacbeth);

        final int resourceCount = testCollection.getResourceCount();

        testCollection.removeResource(resMacbeth);
        assertEquals("After removal resource count must decrease", resourceCount - 1, testCollection.getResourceCount());
        resMacbeth = testCollection.getResource(testFile);
        assertNull(resMacbeth);

        // restore the resource just removed
        try (final InputStream is = SAMPLES.getMacbethSample()) {
            storeResourceFromFile(is, testCollection, testFile);
        }
        assertEquals("After re-store resource count must increase", resourceCount, testCollection.getResourceCount());
        resMacbeth = testCollection.getResource(testFile);
        assertNotNull("getResource(" + testFile + "\")", resMacbeth);
    }

    @Test
    public void storeBinaryResource() throws XMLDBException, IOException, URISyntaxException {
        Collection colTest = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + TEST_COLLECTION);
        CollectionManagementService service = colTest.getService(CollectionManagementService.class);
        Collection testCollection = service.createCollection("test");
        UserManagementService ums = testCollection.getService(UserManagementService.class);
        ums.chmod("rwxr-xr-x");

        final Path fLogo = Paths.get(getClass().getClassLoader().getResource("org/exist/xquery/value/logo.png").toURI());
        byte[] data = storeBinaryResourceFromFile(fLogo, testCollection);
        Object content = testCollection.getResource("logo.png").getContent();
        byte[] dataStored = (byte[])content;
        assertArrayEquals("After storing binary resource, data out==data in", data, dataStored);
    }

    private XMLResource storeResourceFromFile(final InputStream is, final Collection testCollection, final String fileName) throws XMLDBException, IOException {
        XMLResource res = testCollection.createResource(fileName, XMLResource.class);
        assertNotNull("storeResourceFromFile", res);
        res.setContent(InputStreamUtil.readString(is, UTF_8));
        testCollection.storeResource(res);
        return res;
    }

    private byte[] storeBinaryResourceFromFile(Path file, Collection testCollection) throws XMLDBException, IOException {
        final Resource res = testCollection.createResource(file.getFileName().toString(), BinaryResource.class);
        assertNotNull("store binary Resource From File", res);
        // Get an array of bytes from the file:
        final byte[] data = Files.readAllBytes(file);
        res.setContent(data);
        testCollection.storeResource(res);
        return data;
    }

    @Test
    public void testMultipleCreates() throws XMLDBException {
        
        Collection testCol = DatabaseManager.getCollection(XmldbURI.LOCAL_DB + "/" + TEST_COLLECTION);
        CollectionManagementService cms = testCol.getService(CollectionManagementService.class);
        assertNotNull(cms);

        cms.createCollection("dummy1");
        Collection c1 = testCol.getChildCollection("dummy1");
        assertNotNull(c1);

        cms.setCollection(c1);
        cms.createCollection("dummy2");
        Collection c2 = c1.getChildCollection("dummy2");
        assertNotNull(c2);

        cms.setCollection(c2);
        cms.createCollection("dummy3");
        Collection c3 = c2.getChildCollection("dummy3");
        assertNotNull(c3);

        cms.setCollection(testCol);
        cms.removeCollection("dummy1");
        c1 = testCol.getChildCollection("dummy1");
        assertNull(c1);
    }
}