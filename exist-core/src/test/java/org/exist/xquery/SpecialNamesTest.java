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
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XQueryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SpecialNamesTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    private Collection testCollection;

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
    
    /** For queries without associated data */
    private EXistResourceSet queryAndAssert(final String query, final int expected, final String message) throws XMLDBException {
        final XQueryService service = testCollection.getService(XQueryService.class);
        final EXistResourceSet result = (EXistResourceSet) service.query(query);
        if (message == null) {
            assertEquals(expected, result.getSize());
        } else {
            assertEquals(expected, result.getSize(), message);
        }
        return result;
    }

    @Test
    void attributes() throws XMLDBException {
        try (final EXistResourceSet result = queryAndAssert("<foo amp='x' lt='x' gt='x' apos='x' quot='x'/>", 1,  null)) {
            // TODO: could check result
        }
    }
}
