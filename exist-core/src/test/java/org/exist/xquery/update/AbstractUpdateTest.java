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
package org.exist.xquery.update;

import org.exist.TestUtils;
import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.IndexQueryService;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author <a href="mailto:adam@exist-db.org">Adam Retter</a>
 */
public abstract class AbstractUpdateTest {
    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    // required for updateAttributeInNamespacedElement
    private static final String XCONF =
        "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">" +
        "   <index xmlns:t=\"http://test.com\">" +
        "       <lucene>" +
        "           <text qname=\"t:test\"/>" +
        "       </lucene>" +
        "   </index>" +
        "</collection>";

    protected Collection testCollection;

    @BeforeEach
    void setUp() throws XMLDBException {
        final CollectionManagementService service = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
        testCollection = service.createCollection("test");

        final IndexQueryService idx = testCollection.getService(IndexQueryService.class);
        idx.configureCollection(XCONF);
    }

    @AfterEach
    public void tearDown() throws XMLDBException {
        testCollection.close();
        CollectionManagementService service = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
        service.removeCollection("test");

        try (final Collection confColl = DatabaseManager.getCollection("xmldb:exist:///db/system/config/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
            service = confColl.getService(CollectionManagementService.class);
            service.removeCollection("test");
        }

        testCollection = null;
    }

    /** stores XML String and get Query Service
     * @param documentName to be stored in the DB
     * @param content to be stored in the DB
     * @return the XQuery Service
     * @throws XMLDBException
     */
    protected void storeXMLString(final String documentName, final String content) throws XMLDBException {
        try (final XMLResource doc = testCollection.createResource(documentName, XMLResource.class)) {
            doc.setContent(content);
            testCollection.storeResource(doc);
        }
    }

    protected void queryResourceV(final String resource, final String query, final int expected) throws XMLDBException {
        try (final EXistResourceSet result = queryResource(resource, query, expected, null)) {
            // needed to ensure that result is closed
        }
    }

    /** Helper that performs an XQuery and does JUnit assertion on result size.
     * @see #queryResource(String, String, int, String)
     */
    protected EXistResourceSet queryResource(final String resource, final String query, final int expected) throws XMLDBException {
        return queryResource(resource, query, expected, null);
    }

    /** Helper that performs an XQuery and does JUnit assertion on result size.
     * @param resource database resource (collection) to query
     * @param query
     * @param expected size of result
     * @param message for JUnit
     * @return a ResourceSet, allowing to do more assertions if necessary.
     * @throws XMLDBException
     */
    protected EXistResourceSet queryResource(final String resource, final String query, final int expected, @Nullable final String message) throws XMLDBException {
        final XQueryService service = testCollection.getService(XQueryService.class);
        final EXistResourceSet result = (EXistResourceSet) service.queryResource(resource, query);
        if (message == null) {
            assertEquals(expected, result.getSize(), query);
        } else {
            assertEquals(expected, result.getSize(), message);
        }
        return result;
    }
}
