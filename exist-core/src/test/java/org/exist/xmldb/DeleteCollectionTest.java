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

import org.exist.TestUtils;
import org.exist.test.DatabaseWebServerExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeleteCollectionTest {

    @RegisterExtension
    public static final DatabaseWebServerExtension DATABASE_WEB_SERVER = new DatabaseWebServerExtension(true, false, true, true);
    private static final String PORT_PLACEHOLDER = "${PORT}";

    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "local", "xmldb:exist://" },
                { "remote", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc" }
        });
    }
    public String apiName;
    public String baseUri;

    private static final String TEST_COLLECTION_NAME = "testDelete";
    private static final String ZERO_COLLECTION_NAME = "00";
    private static final String ONE_COLLECTION_NAME = "11";
    private static final String THREE_COLLECTION_NAME = "33";
    private Collection testCollection;

    private final String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(DATABASE_WEB_SERVER.getPort()));
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void delete(String apiName, String baseUri) throws XMLDBException {
        initDeleteCollectionTest(apiName, baseUri);
        /*
         * Create the collections:
         *
         * /db/testDelete/00
         * /db/testDelete/11
         * /db/testDelete/00/33
         */
        EXistCollectionManagementService service = testCollection.getService(EXistCollectionManagementService.class);
        try (final Collection zeroCollection = service.createCollection(ZERO_COLLECTION_NAME)) {
            assertNotNull(zeroCollection);

            try (final Collection oneCollection = service.createCollection(ONE_COLLECTION_NAME)) {
                assertNotNull(oneCollection);

                service = zeroCollection.getService(EXistCollectionManagementService.class);
                try (final Collection threeCollection = service.createCollection(THREE_COLLECTION_NAME)) {
                    assertNotNull(threeCollection);

                    // delete the collection /db/test/00
                    service = testCollection.getService(EXistCollectionManagementService.class);
                    service.removeCollection(ZERO_COLLECTION_NAME);
                }
            }
        }
    }

    @BeforeEach
    void setUp() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
            final CollectionManagementService service = root.getService(CollectionManagementService.class);
            testCollection = service.createCollection(TEST_COLLECTION_NAME);
            assertNotNull(testCollection);
        }
    }

    @AfterEach
    void tearDown() throws XMLDBException {
        if (testCollection != null) {
            testCollection.close();
            testCollection = null;
        }
        try (final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
            final CollectionManagementService service = root.getService(CollectionManagementService.class);
            service.removeCollection(TEST_COLLECTION_NAME);
        }
    }

    public void initDeleteCollectionTest(String apiName, String baseUri) {
        this.apiName = apiName;
        this.baseUri = baseUri;
    }
}
