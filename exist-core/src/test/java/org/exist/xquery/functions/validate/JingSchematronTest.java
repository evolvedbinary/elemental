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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.exist.collections.CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE;
import static org.exist.samples.Samples.SAMPLES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

import java.io.IOException;
import java.io.InputStream;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

/**
 * Tests for the validation:jing() function with SCHs.
 * 
 * @author dizzzz@exist-db.org
 */
public class JingSchematronTest {

    private static final String[] TEST_RESOURCES = { "Tournament-valid.xml", "Tournament-invalid.xml", "tournament-schema.sch" };

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    private static final String noValidation = "<?xml version='1.0'?>" +
            "<collection xmlns='http://exist-db.org/collection-config/1.0'>" +
            "    <validation mode='no'/>" +
            "</collection>";

    @BeforeAll
    static void prepareResources() throws XMLDBException, IOException {

        // Switch off validation
        try (final Collection conf = XMLDB_EMBEDDED_DATABASE.createCollection(existEmbeddedServer.getRoot(), "system/config/db/tournament")) {
            XmldbEmbeddedDatabaseExtension.storeResource(conf, DEFAULT_COLLECTION_CONFIG_FILE, noValidation.getBytes());
        }

        // Store schematron 1.5 test files
        try (final Collection col15 = XMLDB_EMBEDDED_DATABASE.createCollection(existEmbeddedServer.getRoot(), "tournament/1.5")) {

            for (final String testResource : TEST_RESOURCES) {
                try (final InputStream is = SAMPLES.getSample("validation/tournament/1.5/" + testResource)) {
                    assertNotNull(is);
                    XmldbEmbeddedDatabaseExtension.storeResource(col15, testResource, InputStreamUtil.readAll(is));
                }
            }
        }
    }

    @Test
    void sch_15_stored_valid() throws XMLDBException {
        String query = "validation:jing-report( " +
                "doc('/db/tournament/1.5/Tournament-valid.xml'), " +
                "doc('/db/tournament/1.5/tournament-schema.sch') )";

        executeAndEvaluate(query,"valid");
    }

    @Test
    void sch_15_stored_valid_boolean() throws XMLDBException {
        final String query = "validation:jing( " +
                "doc('/db/tournament/1.5/Tournament-valid.xml'), " +
                "doc('/db/tournament/1.5/tournament-schema.sch') )";

        try (final EXistResourceSet results = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, results.getSize());

            try (final Resource resource = results.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("true", r);
            }
        }
    }

    @Test
    void sch_15_stored_invalid() throws XMLDBException {
        final String query = "validation:jing-report( " +
                "doc('/db/tournament/1.5/Tournament-invalid.xml'), " +
                "doc('/db/tournament/1.5/tournament-schema.sch') )";
        executeAndEvaluate(query,"invalid");
    }

    @Test
    void sch_15_anyuri_valid() throws XMLDBException {
        final String query = "validation:jing-report( " +
                "xs:anyURI('xmldb:exist:///db/tournament/1.5/Tournament-valid.xml'), " +
                "xs:anyURI('xmldb:exist:///db/tournament/1.5/tournament-schema.sch') )";
        executeAndEvaluate(query,"valid");
    }

    @Test
    void sch_15_anyuri_invalid() throws XMLDBException {
        final String query = "validation:jing-report( " +
                "xs:anyURI('xmldb:exist:///db/tournament/1.5/Tournament-invalid.xml'), " +
                "xs:anyURI('xmldb:exist:///db/tournament/1.5/tournament-schema.sch') )";
        executeAndEvaluate(query,"invalid");
    }

    private void executeAndEvaluate(final String query, final String expectedValue) throws XMLDBException {
        try (final EXistResourceSet results = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, results.getSize());

            try (final Resource resource = results.getResource(0)) {
                final String result = (String) resource.getContent();
                assertThat(result, hasXPath("//status/text()", equalTo(expectedValue)));
            }
        }
    }
}
