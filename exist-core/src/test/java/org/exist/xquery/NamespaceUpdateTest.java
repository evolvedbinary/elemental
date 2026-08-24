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
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NamespaceUpdateTest {

	@RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

	private final static String namespaces =
			"<test xmlns='http://www.foo.com'>"
					+ "<section>"
					+ "<title>Test Document</title>"
					+ "<c:comment xmlns:c='http://www.other.com'>This is my comment</c:comment>"
					+ "</section>"
					+ "</test>";

	private Collection testCollection;

    @Test
    void updateAttribute() throws XMLDBException {
		final XQueryService service = testCollection.getService(XQueryService.class);
		String query =
				"declare namespace t='http://www.foo.com';\n" +
						"<test xmlns='http://www.foo.com'>\n" +
						"{\n" +
						"	update insert attribute { 'ID' } { 'myid' } into /t:test\n" +
						"}\n" +
						"</test>";
		service.query(query);

		query =
				"declare namespace t='http://www.foo.com';\n" +
						"/t:test/@ID/string(.)";
		try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
			assertEquals(1, result.getSize());
			try (final Resource resource = result.getResource(0)) {
				assertEquals("myid", resource.getContent().toString());
			}
		}
	}

    @BeforeEach
    void setUp() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XMLDBException {
		// initialize driver
		final CollectionManagementService service =
			XMLDB_EMBEDDED_DATABASE.getRoot().getService(
				CollectionManagementService.class);
		testCollection = service.createCollection("test");
		assertNotNull(testCollection);

		try (final XMLResource doc = testCollection.createResource("namespace-updates.xml", XMLResource.class)) {
			doc.setContent(namespaces);
			testCollection.storeResource(doc);
		}
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
}
