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

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.IndexQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;

import static org.exist.collections.CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.provider.ValueSource;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;

/**
 * Test proper configuration of triggers in collection.xconf, in particular if there's
 * only a configuration for the parent collection, but not the child. The trigger should
 * be created with the correct base collection.
 */
@ParameterizedClass
@ValueSource(strings = {
    "/db/triggers",
    "/db/triggers/sub1",
    "/db/triggers/sub1/sub2"
})
public class TriggerConfigTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

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


    @BeforeAll
    static void initDB() throws XMLDBException {
        CollectionManagementService mgmt = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
        try (final Collection testCol = mgmt.createCollection("triggers")) {
            mgmt = testCol.getService(CollectionManagementService.class);
            try (final Collection sub1 = mgmt.createCollection("sub1")) {
                mgmt = sub1.getService(CollectionManagementService.class);
                try (final Collection sub2 = mgmt.createCollection("sub2")) {
                }
            }
        }
    }

    @AfterEach
    void cleanDB() throws XMLDBException {
        try (Collection config = DatabaseManager.getCollection(BASE_URI + "/db/system/config" + testCollection, "admin", "")) {
            if (config != null) {
                CollectionManagementService mgmt = config.getService(CollectionManagementService.class);
                mgmt.removeCollection(".");
            }
        }

        try (final Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "");
             final Resource messages = root.getResource("messages.xml")) {
            if (messages != null) {
                root.removeResource(messages);
            }

            try (final Resource data = root.getResource("data.xml")) {
                if (data != null) {
                    root.removeResource(data);
                }
            }
        }
    }

    @Test
    public void storeDocument() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "")) {
            IndexQueryService iqs = root.getService(IndexQueryService.class);
            iqs.configureCollection(COLLECTION_CONFIG);

            try (final Resource resource = root.createResource("data.xml", XMLResource.class)) {
                resource.setContent(DOCUMENT_CONTENT);
                root.storeResource(resource);
                XQueryService qs = root.getService(XQueryService.class);
                try (final EXistResourceSet result = (EXistResourceSet) qs.queryResource("messages.xml", "string(//event[last()]/@collection)")) {
                    assertEquals(1, result.getSize());
                    try (final Resource eventResource = result.getResource(0)) {
                        assertEquals(testCollection, eventResource.getContent());
                    }
                }
            }
        }
    }

    @Test
    public void removeDocument() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "")) {
            IndexQueryService iqs = root.getService(IndexQueryService.class);
            iqs.configureCollection(COLLECTION_CONFIG);

            try (final Resource resource = root.createResource("data.xml", XMLResource.class)) {
                resource.setContent(DOCUMENT_CONTENT);
                root.storeResource(resource);

                root.removeResource(resource);

                XQueryService qs = root.getService(XQueryService.class);
                try (final EXistResourceSet result = (EXistResourceSet) qs.queryResource("messages.xml", "string(//event[last()]/@collection)")) {
                    assertEquals(1, result.getSize());
                    try (final Resource eventResource = result.getResource(0)) {
                        assertEquals(testCollection, eventResource.getContent());
                    }
                }
            }
        }
    }

    @Test
    public void removeTriggers() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "")) {
            IndexQueryService iqs = root.getService(IndexQueryService.class);
            iqs.configureCollection(EMPTY_COLLECTION_CONFIG);

            try (final Resource resource = root.createResource("data.xml", XMLResource.class)) {
                resource.setContent(DOCUMENT_CONTENT);
                root.storeResource(resource);

                XQueryService qs = root.getService(XQueryService.class);
                try (final EXistResourceSet result = (EXistResourceSet) qs.query("if (doc-available('" + testCollection + "/messages.xml')) then doc('" + testCollection + "/messages.xml')/events/event[@id = 'STORE-DOCUMENT'] else ()")) {
                    assertEquals(0, result.getSize(), "No trigger should have fired. Configuration was removed");
                }
            }
        }
    }

    @Test
    public void updateTriggers() throws XMLDBException {
        try (final Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "")) {
            IndexQueryService iqs = root.getService(IndexQueryService.class);
            iqs.configureCollection(EMPTY_COLLECTION_CONFIG);

            try (final Collection configCol = DatabaseManager.getCollection(BASE_URI + "/db/system/config" + testCollection, "admin", "");
                 final Resource resource = configCol.createResource(DEFAULT_COLLECTION_CONFIG_FILE, XMLResource.class)) {
                resource.setContent(COLLECTION_CONFIG);
                configCol.storeResource(resource);
            }

            try (final Resource resource = root.createResource("data.xml", XMLResource.class)) {
                resource.setContent(DOCUMENT_CONTENT);
                root.storeResource(resource);
            }

            XQueryService qs = root.getService(XQueryService.class);
            try (final EXistResourceSet result = (EXistResourceSet) qs.query("if (doc-available('" + testCollection + "/messages.xml')) then doc('" + testCollection + "/messages.xml')/events/event[@id = 'STORE-DOCUMENT']/string(@collection) else ()")) {
                assertEquals(1, result.getSize());
                try (final Resource resource = result.getResource(0)) {
                    assertEquals(testCollection, resource.getContent());
                }
            }
        }
    }
}
