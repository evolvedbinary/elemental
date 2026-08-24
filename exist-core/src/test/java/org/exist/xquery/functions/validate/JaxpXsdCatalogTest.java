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
public class JaxpXsdCatalogTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    private static final String noValidation = "<?xml version='1.0'?>" +
            "<collection xmlns='http://exist-db.org/collection-config/1.0'>" +
            "    <validation mode='no'/>" +
            "</collection>";

    @BeforeAll
    static void prepareResources() throws XMLDBException, IOException {

        // Switch off validation
        try (final Collection conf = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "system/config/db/parse")) {
            XmldbEmbeddedDatabaseExtension.storeResource(conf, DEFAULT_COLLECTION_CONFIG_FILE, noValidation.getBytes());
        }

        try (final Collection schemasCollection = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "parse/schemas")) {

            try (final InputStream is = SAMPLES.getSample("validation/parse/schemas/MyNameSpace.xsd")) {
                assertNotNull(is);
                XmldbEmbeddedDatabaseExtension.storeResource(schemasCollection, "MyNameSpace.xsd", InputStreamUtil.readAll(is));
            }

            try (final InputStream is = SAMPLES.getSample("validation/parse/schemas/AnotherNamespace.xsd")) {
                assertNotNull(is);
                XmldbEmbeddedDatabaseExtension.storeResource(schemasCollection, "AnotherNamespace.xsd", InputStreamUtil.readAll(is));
            }

        }

        try (final Collection parseCollection = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "parse")) {
            try (final InputStream is = SAMPLES.getSample("validation/parse/catalog.xml")) {
                assertNotNull(is);
                XmldbEmbeddedDatabaseExtension.storeResource(parseCollection, "catalog.xml", InputStreamUtil.readAll(is));
            }
        }

        try (final Collection instanceCollection = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "parse/instance")) {

            try (final InputStream is = SAMPLES.getSample("validation/parse/instance/valid.xml")) {
                assertNotNull(is);
                XmldbEmbeddedDatabaseExtension.storeResource(instanceCollection, "valid.xml", InputStreamUtil.readAll(is));
            }

            try (final InputStream is = SAMPLES.getSample("validation/parse/instance/invalid.xml")) {
                assertNotNull(is);
                XmldbEmbeddedDatabaseExtension.storeResource(instanceCollection, "invalid.xml", InputStreamUtil.readAll(is));
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

    @Test
    void xsd_stored_catalog_valid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "doc('/db/parse/instance/valid.xml'), false()," +
                "doc('/db/parse/catalog.xml') )";
        executeAndEvaluate(query,"valid");
    }

    @Test
    void xsd_stored_catalog_invalid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "doc('/db/parse/instance/invalid.xml'), false()," +
                "doc('/db/parse/catalog.xml') )";
        executeAndEvaluate(query,"invalid");
    }

    @Test
    void xsd_anyURI_catalog_valid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "xs:anyURI('/db/parse/instance/valid.xml'), false()," +
                "xs:anyURI('/db/parse/catalog.xml') )";
        executeAndEvaluate(query,"valid");
    }

    @Test
    void xsd_anyURI_catalog_invalid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "xs:anyURI('/db/parse/instance/invalid.xml'), false()," +
                "xs:anyURI('/db/parse/catalog.xml') )";
        executeAndEvaluate(query,"invalid");
    }

    @Test
    void xsd_searched_valid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "doc('/db/parse/instance/valid.xml'), false()," +
                "xs:anyURI('/db/parse/') )";
        executeAndEvaluate(query,"valid");
    }

    @Test
    void xsd_searched_invalid() throws XMLDBException {
        final String query = "validation:jaxp-report( " +
                "doc('/db/parse/instance/invalid.xml'), false()," +
                "xs:anyURI('/db/parse/') )";
        executeAndEvaluate(query,"invalid");
    }

    // test boolean function
    @Test
    void xsd_searched_valid_boolean() throws XMLDBException {
        final String query = "validation:jaxp( " +
                "doc('/db/parse/instance/valid.xml'), false()," +
                "xs:anyURI('/db/parse/') )";
        assertEquals("true", XMLDB_EMBEDDED_DATABASE.executeOneValue(query));
    }

    // test boolean function
    @Test
    void xsd_searched_invalid_boolean() throws XMLDBException {
        final String query = "validation:jaxp( " +
                "doc('/db/parse/instance/invalid.xml'), false()," +
                "xs:anyURI('/db/parse/') )";
        assertEquals("false", XMLDB_EMBEDDED_DATABASE.executeOneValue(query));
    }

    // test parse function
    @Test
    void xsd_searched_parse_valid() throws XMLDBException {
        final String query = "validation:jaxp-parse( " +
                "doc('/db/parse/instance/valid.xml'), false()," +
                "xs:anyURI('/db/parse/') )";
        final String result = XMLDB_EMBEDDED_DATABASE.executeOneValue(query);
        assertThat(result, hasXPath("//Y", equalTo("2006-05-04T18:13:51.0Z")));
    }

    // test parse function
    @Test
    void xsd_searched_parse_invalid() throws XMLDBException {
        final String query = "validation:jaxp-parse( " +
                "doc('/db/parse/instance/invalid.xml'), false()," +
                "xs:anyURI('/db/parse/') )";
        final String result = XMLDB_EMBEDDED_DATABASE.executeOneValue(query);
        assertThat(result, hasXPath("//Y", equalTo("2006-05-04T18:13:51.0Z")));
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
