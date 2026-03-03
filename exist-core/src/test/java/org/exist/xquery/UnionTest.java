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
package org.exist.xquery;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResourceSet;
import org.junit.*;

import static org.exist.collections.CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;

/**
 *
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
public class UnionTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    private final static String TEST_COLLECTION_NAME = "test-pubmed";
    
    private final static String PUBMED_DOC_NAME = "pubmed.xml";
    
    private final static String PUBMED =
        "<PubmedArticleSet>"
        + "<PubmedArticle>"
            + "<MedlineCitation Owner=\"NLM\" Status=\"In-Process\">"
                + "<Article PubModel=\"Print\">"
                    + "<AuthorList CompleteYN=\"Y\">"
                        + "<Author ValidYN=\"Y\">"
                            + "<LastName>Castellano</LastName>"
                            + "<ForeName>Christopher R</ForeName>"
                            + "<Initials>CR</Initials>"
                        + "</Author>"
                        + "<Author ValidYN=\"Y\">"
                            + "<LastName>Rizzolo</LastName>"
                            + "<ForeName>Denise</ForeName>"
                            + "<Initials>D</Initials>"
                        + "</Author>"
                    + "</AuthorList>"
                    + "<Language>eng</Language>"
                    + "<PublicationTypeList>"
                        + "<PublicationType>Journal Article</PublicationType>"
                    + "</PublicationTypeList>"
                + "</Article>"
            + "</MedlineCitation>"
        + "</PubmedArticle>"
    + "</PubmedArticleSet>";
    
    private final static String INDEX_CONFIG =
        "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">"
            + "<index>"
                + "<create qname=\"ForeName\" type=\"xs:string\"/>"
                + "<create qname=\"LastName\" type=\"xs:string\"/>"
            + "</index>"
        +" </collection>";
    
    private final static String XQUERY = "/PubmedArticleSet/PubmedArticle[MedlineCitation/Article/AuthorList/Author/(ForeName|LastName) = \"Castellano\"]";

    private static Collection testCollection;    
    
    @Test
    public void unionInPredicate_withoutIndex() throws XMLDBException {
         final XQueryService service = storeXMLStringAndGetQueryService(PUBMED_DOC_NAME, PUBMED);
         try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(PUBMED_DOC_NAME, XQUERY)) {
             assertEquals(1, result.getSize());
         }
    }
    
    @Test
    public void unionInPredicate_withIndex() throws XMLDBException {
        storeCollectionConfig();
        
        final XQueryService service = storeXMLStringAndGetQueryService(PUBMED_DOC_NAME, PUBMED);
        try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(PUBMED_DOC_NAME, XQUERY)) {
            assertEquals(1, result.getSize());
        }
    }

    @Test
    public void unionPersistentAndConstructedNodes() throws XMLDBException {
        final XQueryService service = storeXMLStringAndGetQueryService(PUBMED_DOC_NAME, PUBMED);
        final String xquery = "doc('" + testCollection.getName() + "/" + PUBMED_DOC_NAME + "')//Language | <a/> | <b/>";

        try (final EXistResourceSet result = (EXistResourceSet) service.query(xquery)) {
            assertEquals(3, result.getSize());
        }
    }
    
    private void storeCollectionConfig() throws XMLDBException {
        try (final Collection colConfig = getOrCreateCollection("/db/system/config/db/" + TEST_COLLECTION_NAME);
             final XMLResource docConfig = colConfig.createResource(DEFAULT_COLLECTION_CONFIG_FILE, XMLResource.class)) {
            docConfig.setContent(INDEX_CONFIG);
            colConfig.storeResource(docConfig);
        }
    }
    
    private Collection getOrCreateCollection(final String collectionPath) throws XMLDBException {
        return getOrCreateCollection(testCollection.getParentCollection(), collectionPath.replaceFirst("/db/", ""));
    }
    
    private Collection getOrCreateCollection(final Collection currentCollection, final String collectionPath) throws XMLDBException {
        final int offset = collectionPath.indexOf("/") > -1 ? collectionPath.indexOf("/") : collectionPath.length();
        final String colName = collectionPath.substring(0, offset);
        
        if(colName.length() == 0) {
            return currentCollection;
        }
        
        Collection child = currentCollection.getChildCollection(colName);
        if(child == null) {
            final CollectionManagementService service = currentCollection.getService(CollectionManagementService.class);
            child = service.createCollection(colName);
        }
        
        if (collectionPath.indexOf("/") == -1) {
            return child;
        } else {
            final String subPath = collectionPath.substring(collectionPath.indexOf("/") + 1);
            try {
                return getOrCreateCollection(child, subPath);
            } finally {
                child.close();
            }
        }
    }
    
    private XQueryService storeXMLStringAndGetQueryService(String documentName, String content) throws XMLDBException {
       try (final XMLResource doc = testCollection.createResource(documentName, XMLResource.class)) {
           doc.setContent(content);
           testCollection.storeResource(doc);
       }
       return testCollection.getService(XQueryService.class);
    }
    
    @Before
    public void clearCollectionConfig() throws XMLDBException {
        try (final Collection colDb = testCollection.getParentCollection();
             final Collection colSystem = colDb.getChildCollection("system")) {

            if (colSystem == null) {
                return;
            }

            try (final Collection colConfig = colSystem.getChildCollection("config")) {
                if (colConfig == null) {
                    return;
                }

                try (final Collection colConfigDb = colConfig.getChildCollection("db")) {
                    if (colConfigDb == null) {
                        return;
                    }

                    boolean foundPubmedConfig = false;
                    for (final String configCol : colConfigDb.listChildCollections()) {
                        if (configCol.equals(TEST_COLLECTION_NAME)) {
                            foundPubmedConfig = true;
                            break;
                        }
                    }

                    if (foundPubmedConfig) {
                        final CollectionManagementService service = colConfigDb.getService(CollectionManagementService.class);
                        service.removeCollection(TEST_COLLECTION_NAME);
                    }
                }
            }
        }
    }

    @BeforeClass
    public static void createTestCollection() throws Exception {
        final CollectionManagementService service = existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
        testCollection = service.createCollection(TEST_COLLECTION_NAME);
        assertNotNull(testCollection);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        testCollection.close();
        final CollectionManagementService service =
                existEmbeddedServer.getRoot().getService(
                        CollectionManagementService.class);
        service.removeCollection(TEST_COLLECTION_NAME);
        testCollection = null;
    }
}
