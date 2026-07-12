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
import org.exist.test.TestConstants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LocalXMLResourceDOMTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    private static String TEST_RESOURCE_NAME = "doc1.xml";

    @BeforeClass
    public static void setup() throws XMLDBException {
        final CollectionManagementService cms = (CollectionManagementService) existEmbeddedServer
                .getRoot()
                .getService("CollectionManagementService", "1.0");

        try (final Collection coll = cms.createCollection(TestConstants.TEST_COLLECTION_URI.lastSegment().toString())) {

            try (final EXistResource er = (EXistResource) coll.createResource(
                    TEST_RESOURCE_NAME,
                    XMLResource.RESOURCE_TYPE
            )) {
                final XMLResource r = (XMLResource) er;
                r.setContent("<properties><property key=\"type\">Table</property><test/></properties><!-- comment -->");
                coll.storeResource(r);
            }
        }
    }

    @AfterClass
    public static void cleanup() throws XMLDBException {
        final CollectionManagementService cms = (CollectionManagementService) existEmbeddedServer
                .getRoot()
                .getService("CollectionManagementService", "1.0");

        cms.removeCollection(TestConstants.TEST_COLLECTION_URI.getRawCollectionPath());
    }

    @Test
    public void testEnhancer01() throws XMLDBException {
        final String query = "doc('" + TestConstants.TEST_COLLECTION_URI.getRawCollectionPath() + "/" + TEST_RESOURCE_NAME + "')//properties[property[@key eq 'type'][text() eq 'Table']]";

        try (final EXistResourceSet rs1 = existEmbeddedServer.executeQuery(query);
             final EXistResourceSet rs2 = existEmbeddedServer.executeQuery(query)) {

            final ResourceIterator i1 = rs1.getIterator();
            final ResourceIterator i2 = rs2.getIterator();

            for (; i1.hasMoreResources() && i1.hasMoreResources(); ) {

                try (final EXistResource er1 = (EXistResource) i1.nextResource();
                     final EXistResource er2 = (EXistResource) i2.nextResource()) {
                    final XMLResource r1 = (XMLResource) er1;
                    final XMLResource r2 = (XMLResource) er2;

                    assertEquals(r1.getContentAsDOM(), r2.getContentAsDOM());
                }
            }
        }
    }

    @Test
    public void testEnhancer02() throws XMLDBException {
        try (final EXistResourceSet rs1 = existEmbeddedServer.executeQuery(
            "doc('" + TestConstants.TEST_COLLECTION_URI.getRawCollectionPath() + "/" + TEST_RESOURCE_NAME + "')//properties/property[@key='type' and text()='Table']"
        )) {
            for (final ResourceIterator i1 = rs1.getIterator(); i1.hasMoreResources(); ) {
                try (final EXistResource er1 = (EXistResource) i1.nextResource()) {
                    final XMLResource r1 = (XMLResource) er1;

                    final Map<String, Object> variables = new HashMap<>();
                    variables.put("local:document", r1.getContentAsDOM());

                    final String query = "xquery version \"1.0\";"
                        + "declare namespace xmldb=\"http://exist-db.org/xquery/xmldb\";"
                        + "declare variable $local:document external;"
                        + "$local:document";
                    try (final EXistResourceSet rs2 = existEmbeddedServer.executeQuery(query, variables)) {

                        for (final ResourceIterator i2 = rs2.getIterator(); i2.hasMoreResources(); ) {
                            try (final EXistResource er2 = (EXistResource) i2.nextResource()) {
                                final XMLResource r2 = (XMLResource) er2;
                                final Node content2 = r2.getContentAsDOM();
                                assertNotNull(content2);
                            }
                        }
                    }
                }
            }
        }
    }
}
