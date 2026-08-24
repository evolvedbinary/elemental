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

import java.io.IOException;
import java.io.InputStream;

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.util.io.InputStreamUtil;
import org.exist.xmldb.EXistResourceSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.exist.collections.CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.exist.samples.Samples.SAMPLES;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

/**
 * Tests for the validation:jaxv() function with XSDs.
 *
 * @author dizzzz@exist-db.org
 */
public class JaxvTest {

    private static final String[] TEST_RESOURCES = { "personal-valid.xml", "personal-invalid.xml", "personal.xsd" };

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    @BeforeAll
    static void prepareResources() throws XMLDBException, IOException {
        final String noValidation = "<?xml version='1.0'?>" +
                "<collection xmlns=\"http://exist-db.org/collection-config/1.0" +
                "\">" +
                "<validation mode=\"no\"/>" +
                "</collection>";

        try (final Collection conf = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "system/config/db/personal")) {
            XmldbEmbeddedDatabaseExtension.storeResource(conf, DEFAULT_COLLECTION_CONFIG_FILE, noValidation.getBytes());
        }

        try (final Collection collection = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "personal")) {

            for (final String testResource : TEST_RESOURCES) {
                try (final InputStream is = SAMPLES.getSample("validation/personal/" + testResource)) {
                    assertNotNull(is);
                    XmldbEmbeddedDatabaseExtension.storeResource(collection, testResource, InputStreamUtil.readAll(is));
                }
            }
        }
    }

    @Test
    void xsd_stored_valid() throws XMLDBException {
        final String query = "validation:jaxv( " +
                "doc('/db/personal/personal-valid.xml'), " +
                "doc('/db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("true",
                    resource.getContent().toString());
            }
        }
    }

    @Test
    void xsd_stored_invalid() throws XMLDBException {
        final String query = "validation:jaxv( " +
            "doc('/db/personal/personal-invalid.xml'), " +
            "doc('/db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("false",
                    resource.getContent().toString());
            }
        }
    }

    @Test
    void xsd_stored_report_valid() throws XMLDBException {
        final String query = "validation:jaxv-report( " +
                "doc('/db/personal/personal-valid.xml'), " +
                "doc('/db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final String content = (String) resource.getContent();
                assertThat(content, hasXPath("//status/text()", equalTo("valid")));
            }
        }
    }

    @Test
    void xsd_stored_report_invalid() throws XMLDBException {
        final String query = "validation:jaxv-report( " +
                "doc('/db/personal/personal-invalid.xml'), " +
                "doc('/db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final String content = (String) resource.getContent();
                assertThat(content, hasXPath("//status/text()", equalTo("invalid")));
            }
        }
    }

    @Test
    void xsd_anyuri_valid() throws XMLDBException {
        final String query = "validation:jaxv( " +
            "xs:anyURI('xmldb:exist:///db/personal/personal-valid.xml'), " +
            "xs:anyURI('xmldb:exist:///db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("true",
                    resource.getContent().toString());
            }
        }
    }

    @Test
    void xsd_anyuri_invalid() throws XMLDBException {
        final String query = "validation:jaxv( " +
            "xs:anyURI('xmldb:exist:///db/personal/personal-invalid.xml'), " +
            "xs:anyURI('xmldb:exist:///db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("false",
                    resource.getContent().toString());
            }
        }
    }

    @Test
    void xsd_anyuri_report_valid() throws XMLDBException {
        final String query = "validation:jaxv-report( " +
                "xs:anyURI('xmldb:exist:///db/personal/personal-valid.xml'), " +
                "xs:anyURI('xmldb:exist:///db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final String content = (String) resource.getContent();
                assertThat(content, hasXPath("//status/text()", equalTo("valid")));
            }
        }
    }

    @Test
    void xsd_anyuri_report_invalid() throws XMLDBException {
        final String query = "validation:jaxv-report( " +
                "xs:anyURI('xmldb:exist:///db/personal/personal-invalid.xml'), " +
                "xs:anyURI('xmldb:exist:///db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final String content = (String) resource.getContent();
                assertThat(content, hasXPath("//status/text()", equalTo("invalid")));
            }
        }
    }

    @Test
    void xsd_stored_anyuri_valid() throws XMLDBException {
        final String query = "validation:jaxv( " +
            "doc('/db/personal/personal-valid.xml'), " +
            "xs:anyURI('xmldb:exist:///db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("true",
                    resource.getContent().toString());
            }
        }
    }

    @Test
    void xsd_stored_anyuri_invalid() throws XMLDBException {
        final String query = "validation:jaxv( " +
            "doc('/db/personal/personal-invalid.xml'), " +
            "xs:anyURI('xmldb:exist:///db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("false",
                    resource.getContent().toString());
            }
        }
    }

    @Test
    void xsd_stored_anyuri_report_valid() throws XMLDBException {
        final String query = "validation:jaxv-report( " +
            "doc('/db/personal/personal-valid.xml'), " +
            "xs:anyURI('xmldb:exist:///db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertThat(r, hasXPath("//status/text()", equalTo("valid")));
            }
        }
    }

    @Test
    void xsd_stored_anyuri_report_invalid() throws XMLDBException {
        final String query = "validation:jaxv-report( " +
            "doc('/db/personal/personal-invalid.xml'), " +
            "xs:anyURI('xmldb:exist:///db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertThat(r, hasXPath("//status/text()", equalTo("invalid")));
            }
        }
    }

    @Test
    void xsd_anyuri_stored_valid() throws XMLDBException {
        final String query = "validation:jaxv( " +
            "xs:anyURI('xmldb:exist:///db/personal/personal-valid.xml'), " +
            "doc('/db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("true",
                    resource.getContent().toString());
            }
        }
    }

    @Test
    void xsd_anyuri_stored_invalid() throws XMLDBException {
        final String query = "validation:jaxv( " +
            "xs:anyURI('xmldb:exist:///db/personal/personal-invalid.xml'), " +
            "doc('/db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("false",
                    resource.getContent().toString());
            }
        }
    }

    @Test
    void xsd_anyuri_stored_report_valid() throws XMLDBException {
        final String query = "validation:jaxv-report( " +
            "xs:anyURI('xmldb:exist:///db/personal/personal-valid.xml'), " +
            "doc('/db/personal/personal.xsd') )";

        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            assertEquals(1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertThat(r, hasXPath("//status/text()", equalTo("valid")));
            }
        }
    }

    @Test
    void xsd_anyuri_stored_report_invalid() throws XMLDBException {
        final String query = "validation:jaxv-report( " +
            "xs:anyURI('xmldb:exist:///db/personal/personal-invalid.xml'), " +
            "doc('/db/personal/personal.xsd') )";

        try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
            assertEquals(1, result.getSize());

            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertThat(r, hasXPath("//status/text()", equalTo("invalid")));
            }
        }
    }
}
