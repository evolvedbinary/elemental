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
package org.exist.xquery.functions.validate;

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.util.io.InputStreamUtil;
import org.exist.xmldb.EXistResourceSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.exist.collections.CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.exist.samples.Samples.SAMPLES;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

import java.io.IOException;
import java.io.InputStream;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

/**
 * Tests for the validation:jaxp() function with Catalog (resolvers).
 * 
 * @author dizzzz@exist-db.org
 */
public class JaxpDtdCatalogTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    private static final String noValidation = "<?xml version='1.0'?>" +
            "<collection xmlns='http://exist-db.org/collection-config/1.0'>" +
            "    <validation mode='no'/>" +
            "</collection>";

    @BeforeAll
    static void prepareResources() throws XMLDBException, IOException {

        // Switch off validation
        try (Collection conf = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "system/config/db/parse")) {
            XmldbEmbeddedDatabaseExtension.storeResource(conf, DEFAULT_COLLECTION_CONFIG_FILE, noValidation.getBytes());
        }

        try (Collection dtdsCollection = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "parse/dtds")) {

            try (final InputStream is = SAMPLES.getSample("validation/parse/dtds/MyNameSpace.dtd")) {
                assertNotNull(is);
                XmldbEmbeddedDatabaseExtension.storeResource(dtdsCollection, "MyNameSpace.dtd", InputStreamUtil.readAll(is));
            }
        }

        try (Collection parseCollection = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "parse")) {

            try (final InputStream is = SAMPLES.getSample("validation/parse/catalog.xml")) {
                assertNotNull(is);
                XmldbEmbeddedDatabaseExtension.storeResource(parseCollection, "catalog.xml", InputStreamUtil.readAll(is));
            }
        }

        try (Collection instanceCollection = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "parse/instance")) {

            try (final InputStream is = SAMPLES.getSample("validation/parse/instance/valid-dtd.xml")) {
                assertNotNull(is);
                XmldbEmbeddedDatabaseExtension.storeResource(instanceCollection, "valid-dtd.xml", InputStreamUtil.readAll(is));
            }

            try (final InputStream is = SAMPLES.getSample("validation/parse/instance/invalid-dtd.xml")) {
                assertNotNull(is);
                XmldbEmbeddedDatabaseExtension.storeResource(instanceCollection, "invalid-dtd.xml", InputStreamUtil.readAll(is));
            }
        }
    }

    @BeforeEach
    void clearGrammarCache() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("validation:clear-grammar-cache()")) {
            try (final Resource resource = result.getResource(0)) {
                resource.getContent();
            }
        }
    }

    /*
     * ***********************************************************************************
     */
    @Test
    void dtd_stored_catalog_valid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "xs:anyURI('/db/parse/instance/valid-dtd.xml'), false()," +
                "doc('/db/parse/catalog.xml') )";
        executeAndEvaluate(query,"valid");
    }

    @Test
    void dtd_stored_catalog_invalid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "xs:anyURI('/db/parse/instance/invalid-dtd.xml'), false()," +
                "doc('/db/parse/catalog.xml') )";
        executeAndEvaluate(query,"invalid");
    }

    @Test
    void dtd_anyURI_catalog_valid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "xs:anyURI('/db/parse/instance/valid-dtd.xml'), false()," +
                "xs:anyURI('/db/parse/catalog.xml') )";
        executeAndEvaluate(query,"valid");
    }

    @Test
    void dtd_anyURI_catalog_invalid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "xs:anyURI('/db/parse/instance/invalid-dtd.xml'), false()," +
                "xs:anyURI('/db/parse/catalog.xml') )";
       executeAndEvaluate(query,"invalid");
    }

    /*
     * ***********************************************************************************
     *
     * DIZZZZ: doc('/db/parse/instance/valid-dtd.xml') does not work xs:anyURI does
     *
     */
    @Test
    void dtd_searched_valid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "xs:anyURI('/db/parse/instance/valid-dtd.xml'), false()," +
                "xs:anyURI('/db/parse/') )";
        executeAndEvaluate(query,"valid");
    }

    @Test
    void dtd_searched_invalid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "xs:anyURI('/db/parse/instance/invalid-dtd.xml'), false()," +
                "xs:anyURI('/db/parse/') )";
        executeAndEvaluate(query,"invalid");
    }

    private void executeAndEvaluate(final String query, final String expectedValue) throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final String content = (String) resource.getContent();
                assertThat(content, hasXPath("//status/text()", equalTo(expectedValue)));
            }
        }
    }
}
