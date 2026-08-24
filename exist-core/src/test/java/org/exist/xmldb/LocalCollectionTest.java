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

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.test.TestConstants;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ServiceProviderCache;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XPathQueryService;
import org.xmldb.api.modules.XQueryService;
import org.xmldb.api.modules.XUpdateQueryService;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalCollectionTest {
    static Collection testCollection;

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    @BeforeAll
    static void setup() throws XMLDBException {
        final CollectionManagementService cms = XMLDB_EMBEDDED_DATABASE
                .getRoot()
                .getService(CollectionManagementService.class);

        testCollection = cms.createCollection(TestConstants.TEST_COLLECTION_URI.lastSegment().toString());
    }

    @AfterAll
    public static void cleanup() throws XMLDBException {
        if (testCollection != null) {
            testCollection.close();
            testCollection = null;
        }
        final CollectionManagementService cms = XMLDB_EMBEDDED_DATABASE
                .getRoot()
                .getService(CollectionManagementService.class);

        cms.removeCollection(TestConstants.TEST_COLLECTION_URI.getRawCollectionPath());
    }

    @Test
    void getChildCollectionCount() throws XMLDBException {
        assertEquals(0, testCollection.getChildCollectionCount());
    }

    @Test
    void getPropertyWithDefault() throws XMLDBException {
        assertEquals("theDefault", testCollection.getProperty("myProperty", "theDefault"));
    }

    @Test
    void hasService(){
        assertTrue(testCollection.hasService(XPathQueryService.class));
    }

    @Test
    void findService(){
        assertNotNull(testCollection.findService(XPathQueryService.class).get());
    }

    @Test
    void getService() throws XMLDBException {
        assertNotNull(testCollection.getService(XPathQueryService.class));
    }

    @Test
    void registerProvders() {
        LocalCollection localCollection = (LocalCollection)testCollection;
        ServiceProviderCache.ProviderRegistry registry = createMock(ServiceProviderCache.ProviderRegistry.class);

        registry.add(eq(XPathQueryService.class), notNull());
        registry.add(eq(XQueryService.class), notNull());
        registry.add(eq(CollectionManagementService.class), notNull());
        registry.add(eq(EXistCollectionManagementService.class), notNull());
        registry.add(eq(UserManagementService.class), notNull());
        registry.add(eq(EXistUserManagementService.class), notNull());
        registry.add(eq(DatabaseInstanceManager.class), notNull());
        registry.add(eq(XUpdateQueryService.class), notNull());
        registry.add(eq(IndexQueryService.class), notNull());
        registry.add(eq(EXistRestoreService.class), notNull());

        replay(registry);
        localCollection.registerProvders(registry);
        verify(registry);
    }

    @Test
    void listChildCollections() throws XMLDBException {
        assertTrue(testCollection.listChildCollections().isEmpty());
    }

    @Test
    void getChildCollections() throws XMLDBException {
        LocalCollection localCollection = (LocalCollection)testCollection;
        assertArrayEquals(new Collection[0], localCollection.getChildCollections());
    }

    @Test
    void listResources() throws XMLDBException {
        LocalCollection localCollection = (LocalCollection)testCollection;
        assertTrue(localCollection.listResources().isEmpty());
    }

    @Test
    void getResources() throws XMLDBException {
        LocalCollection localCollection = (LocalCollection)testCollection;
        assertArrayEquals(new org.exist.Resource[0], localCollection.getResources());
    }

    @Test
    void getCreationTime() throws XMLDBException {
        assertNotNull(testCollection.getCreationTime());
    }
}
