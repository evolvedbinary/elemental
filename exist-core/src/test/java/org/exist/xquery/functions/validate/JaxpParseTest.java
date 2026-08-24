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
import static org.junit.jupiter.api.Assertions.*;
import static org.exist.samples.Samples.SAMPLES;

import java.io.IOException;
import java.io.InputStream;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Tests for the validation:jaxp() function with Catalog (resolvers).
 * 
 * @author dizzzz@exist-db.org
 */
public class JaxpParseTest {

    private static final String[] TEST_RESOURCES = { "defaultValue.xml", "defaultValue.xsd" };

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    private static final String noValidation = "<?xml version='1.0'?>" +
            "<collection xmlns='http://exist-db.org/collection-config/1.0'>" +
            "    <validation mode='no'/>" +
            "</collection>";

    @BeforeAll
    static void prepareResources() throws XMLDBException, IOException {

        // Switch off validation
        try (final Collection conf = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "system/config/db/parse_validate")) {
            XmldbEmbeddedDatabaseExtension.storeResource(conf, DEFAULT_COLLECTION_CONFIG_FILE, noValidation.getBytes());
        }

        try (final Collection schemasCollection = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "parse_validate")) {

            for (final String testResource : TEST_RESOURCES) {
                try (final InputStream is = SAMPLES.getSample("validation/parse_validate/" + testResource)) {
                    assertNotNull(is);
                    XmldbEmbeddedDatabaseExtension.storeResource(schemasCollection, testResource, InputStreamUtil.readAll(is));
                }
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
    void parse_and_fill_defaults() throws XMLDBException, IOException, SAXException {
        String query = "validation:pre-parse-grammar(xs:anyURI('/db/parse_validate/defaultValue.xsd'))";
        String result = execute(query);
        assertEquals("defaultTest", result);

        query = "declare option exist:serialize 'indent=no'; " +
                "validation:jaxp-parse(xs:anyURI('/db/parse_validate/defaultValue.xml'), true(), ())";
        result = execute(query);

        String expected = "<ns1:root xmlns:ns1=\"defaultTest\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <color>red</color>\n" +
                "    <shoesize country=\"nl\">43</shoesize>\n" +
                "</ns1:root>";
        assertThat(result, CompareMatcher.isIdenticalTo(expected));
    }

    private String execute(final String query) throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                return (String) resource.getContent();
            }
        }
    }
}
