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

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.xmldb.EXistResourceSet;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XQueryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntitiesTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    private Collection testCollection;
    @SuppressWarnings("unused")
	private String query;

    @BeforeEach
    void setUp() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XMLDBException {
        final CollectionManagementService service =
                XMLDB_EMBEDDED_DATABASE.getRoot().getService(
                CollectionManagementService.class);
        testCollection = service.createCollection("test");
        assertNotNull(testCollection);
    }

    @AfterEach
    void tearDown() throws XMLDBException {
        testCollection.close();

        final CollectionManagementService service =
                XMLDB_EMBEDDED_DATABASE.getRoot().getService(
                        CollectionManagementService.class);
        service.removeCollection("test");
        testCollection = null;
    }
    
    /** Helper that performs an XQuery and does JUnit assertion on result size.
     * @see #queryResource(XQueryService, String, String, int, String)
     */
    @SuppressWarnings("unused")
	private EXistResourceSet queryResource(final XQueryService service, final String resource, final String query, final int expected) throws XMLDBException {
        return queryResource(service, resource, query, expected, null);
    }
    
    /** Helper that performs an XQuery and does JUnit assertion on result size.
     * @param service XQuery service
     * @param resource database resource (collection) to query
     * @param query
     * @param expected size of result
     * @param message for JUnit
     * @return a ResourceSet, allowing to do more assertions if necessary.
     * @throws XMLDBException
     */
    private EXistResourceSet queryResource(final XQueryService service, final String resource, final String query, final int expected, final String message) throws XMLDBException {
        final EXistResourceSet result = (EXistResourceSet) service.queryResource(resource, query);
        if(message == null) {
            assertEquals(expected, result.getSize(), query);
        } else {
            assertEquals(expected, result.getSize(), message);
        }
        return result;
    }
    
    /** For queries without associated data */
    private EXistResourceSet queryAndAssert(final String query, final int expected, final String message) throws XMLDBException {
        final XQueryService xqueryService = testCollection.getService(XQueryService.class);
        final EXistResourceSet result = (EXistResourceSet) xqueryService.query(query);
        if(message == null) {
            assertEquals(expected, result.getSize());
        } else {
            assertEquals(expected, result.getSize(), message);
        }
        return result;
    }

    @Test
    void attributeConstructor() throws XMLDBException {
        try (final EXistResourceSet result = queryAndAssert(
                "<foo "+
                " ampEntity=\"{('&amp;')}\"" +
                " string=\"{(string('&amp;'))}\"" +
                " ltEntity=\"{('&lt;')}\"" +
                " gtEntity=\"{('&gt;')}\"" +
                " aposEntity=\"{('&apos;')}\"" +
                " quotEntity=\"{('&quot;')}\"" +
                "/>",
                1,  null )) {
            // TODO: could check result
        }
    }

    @Test
    void stringConstructor() throws XMLDBException {
        try (final EXistResourceSet result = queryAndAssert("'&amp;'", 1, null)) {
            // TODO: could check result
        }

        try (final EXistResourceSet result = queryAndAssert("'&lt;'", 1, null)) {
            // TODO: could check result
        }

        try (final EXistResourceSet result = queryAndAssert("'&gt;'", 1, null)) {
            // TODO: could check result
        }

        try (final EXistResourceSet result = queryAndAssert("'&apos;'", 1, null)) {
            // TODO: could check result
        }

        try (final EXistResourceSet result = queryAndAssert("'&quot;'", 1, null)) {
            // TODO: could check result
        }
    }

    @Test
    void uriConstructor() throws XMLDBException {
        try (final EXistResourceSet result = queryAndAssert("xs:anyURI(\"index.xql?a=1&amp;b=2\")", 1, null)) {
            // TODO: could check result
        }

        try (final EXistResourceSet result = queryAndAssert("xs:anyURI('a') le xs:anyURI('b')", 1, null)) {
            try (final Resource resource = result.getResource(0)) {
                assertEquals("true", resource.getContent());
            }
        }
    }
}
