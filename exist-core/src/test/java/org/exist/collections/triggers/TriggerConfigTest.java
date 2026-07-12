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
package org.exist.collections.triggers;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.IndexQueryService;
import org.junit.*;

import static org.exist.collections.CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE;
import static org.junit.Assert.assertEquals;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;

import org.junit.runners.Parameterized.Parameters;

/**
 * Test proper configuration of triggers in collection.xconf, in particular if there's
 * only a configuration for the parent collection, but not the child. The trigger should
 * be created with the correct base collection.
 */
@RunWith(Parameterized.class)
public class TriggerConfigTest {

    private static final Logger LOG = LogManager.getLogger(TriggerConfigTest.class);

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    @Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "/db/triggers" },
            { "/db/triggers/sub1" },
            { "/db/triggers/sub1/sub2" }
        });
    }

    private static final String COLLECTION_CONFIG =
    	"<exist:collection xmlns:exist='http://exist-db.org/collection-config/1.0'>" +
	    "  <exist:triggers>" +
		"     <exist:trigger class='org.exist.collections.triggers.MessagesTrigger'/>" +
        "  </exist:triggers>" +
        "</exist:collection>";

    private static final String EMPTY_COLLECTION_CONFIG =
    	"<exist:collection xmlns:exist='http://exist-db.org/collection-config/1.0'>" +
        "</exist:collection>";

    private final static String DOCUMENT_CONTENT =
		  "<test>"
		+ "<item id='1'><price>5.6</price><stock>22</stock></item>"
		+ "<item id='2'><price>7.4</price><stock>43</stock></item>"
		+ "<item id='3'><price>18.4</price><stock>5</stock></item>"
		+ "<item id='4'><price>65.54</price><stock>16</stock></item>"
		+ "</test>";

    private final static String BASE_URI = "xmldb:exist://";

    @Parameter
    public String testCollection;


    @BeforeClass
    public static void initDB() throws XMLDBException {
        CollectionManagementService mgmt = (CollectionManagementService) existEmbeddedServer.getRoot().getService("CollectionManagementService", "1.0");
        try (final Collection testCol = mgmt.createCollection("triggers")) {
            mgmt = (CollectionManagementService) testCol.getService("CollectionManagementService", "1.0");
            try (final Collection sub1 = mgmt.createCollection("sub1")) {
                mgmt = (CollectionManagementService) sub1.getService("CollectionManagementService", "1.0");
                try (final Collection sub2 = mgmt.createCollection("sub2")) {
                }
            }
        }
    }

    @After
    public void cleanDB() throws XMLDBException {
        try (Collection config = DatabaseManager.getCollection(BASE_URI + "/db/system/config" + testCollection, "admin", "")) {
            if (config != null) {
                CollectionManagementService mgmt = (CollectionManagementService) config.getService("CollectionManagementService", "1.0");
                mgmt.removeCollection(".");
            }
        }

        try (final Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "");
             final EXistResource messages = (EXistResource) root.getResource("messages.xml")) {
            if (messages != null) {
                root.removeResource(messages);
            }

            try (final EXistResource data = (EXistResource) root.getResource("data.xml")) {
                if (data != null) {
                    root.removeResource(data);
                }
            }
        }
    }

    @Test
    public void storeDocument() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "")) {
            IndexQueryService iqs = (IndexQueryService) root.getService("IndexQueryService", "1.0");
            iqs.configureCollection(COLLECTION_CONFIG);

            try (final EXistResource resource = (EXistResource) root.createResource("data.xml", XMLResource.RESOURCE_TYPE)) {
                resource.setContent(DOCUMENT_CONTENT);
                root.storeResource(resource);
                XQueryService qs = (XQueryService) root.getService("XQueryService", "1.0");
                try (final EXistResourceSet result = (EXistResourceSet) qs.queryResource("messages.xml", "string(//event[last()]/@collection)")) {
                    assertEquals(1, result.getSize());
                    try (final EXistResource eventResource = (EXistResource) result.getResource(0)) {
                        assertEquals(testCollection, eventResource.getContent());
                    }
                }
            }
        }
    }

    @Test
    public void removeDocument() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "")) {
            IndexQueryService iqs = (IndexQueryService) root.getService("IndexQueryService", "1.0");
            iqs.configureCollection(COLLECTION_CONFIG);

            try (final EXistResource resource = (EXistResource) root.createResource("data.xml", XMLResource.RESOURCE_TYPE)) {
                resource.setContent(DOCUMENT_CONTENT);
                root.storeResource(resource);

                root.removeResource(resource);

                XQueryService qs = (XQueryService) root.getService("XQueryService", "1.0");
                try (final EXistResourceSet result = (EXistResourceSet) qs.queryResource("messages.xml", "string(//event[last()]/@collection)")) {
                    assertEquals(1, result.getSize());
                    try (final EXistResource eventResource = (EXistResource) result.getResource(0)) {
                        assertEquals(testCollection, eventResource.getContent());
                    }
                }
            }
        }
    }

    @Test
    public void removeTriggers() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "")) {
           IndexQueryService iqs = (IndexQueryService) root.getService("IndexQueryService", "1.0");
            iqs.configureCollection(EMPTY_COLLECTION_CONFIG);

            try (final EXistResource resource = (EXistResource) root.createResource("data.xml", XMLResource.RESOURCE_TYPE)) {
                resource.setContent(DOCUMENT_CONTENT);
                root.storeResource(resource);

                XQueryService qs = (XQueryService) root.getService("XQueryService", "1.0");
                try (final EXistResourceSet result = (EXistResourceSet) qs.query("if (doc-available('" + testCollection + "/messages.xml')) then doc('" + testCollection + "/messages.xml')/events/event[@id = 'STORE-DOCUMENT'] else ()")) {
                    assertEquals("No trigger should have fired. Configuration was removed", 0, result.getSize());
                }
            }
        }
    }

    @Test
    public void updateTriggers() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "")) {
            IndexQueryService iqs = (IndexQueryService) root.getService("IndexQueryService", "1.0");
            iqs.configureCollection(EMPTY_COLLECTION_CONFIG);

            try (final Collection configCol = DatabaseManager.getCollection(BASE_URI + "/db/system/config" + testCollection, "admin", "");
                 final EXistResource resource = (EXistResource) configCol.createResource(DEFAULT_COLLECTION_CONFIG_FILE, XMLResource.RESOURCE_TYPE)) {
                resource.setContent(COLLECTION_CONFIG);
                configCol.storeResource(resource);
            }

            try (final EXistResource resource = (EXistResource) root.createResource("data.xml", XMLResource.RESOURCE_TYPE)) {
                resource.setContent(DOCUMENT_CONTENT);
                root.storeResource(resource);
            }

            XQueryService qs = (XQueryService) root.getService("XQueryService", "1.0");
            try (final EXistResourceSet result = (EXistResourceSet) qs.query("if (doc-available('" + testCollection + "/messages.xml')) then doc('" + testCollection + "/messages.xml')/events/event[@id = 'STORE-DOCUMENT']/string(@collection) else ()")) {
                assertEquals(1, result.getSize());
                try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                    assertEquals(testCollection, resource.getContent());
                }
            }
        }
    }
}
