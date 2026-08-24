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
package org.exist.xmldb;

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.util.io.InputStreamUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Disabled;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XPathQueryService;

import java.io.InputStream;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.exist.samples.Samples.SAMPLES;

public class ResourceSetTest {

	@RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

	private final static String TEST_COLLECTION = "testResourceSet";

	private Collection testCollection;

    @BeforeEach
    void setUp() throws XMLDBException, IOException {
		final CollectionManagementService service = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
		testCollection = service.createCollection(TEST_COLLECTION);
		assertNotNull(testCollection);

		try (final InputStream is = SAMPLES.getSample("shakespeare/shakes.xsl")) {
			try (final Resource shakesRes = testCollection.createResource("shakes.xsl", XMLResource.class)) {
				shakesRes.setContent(InputStreamUtil.readAll(is));
				testCollection.storeResource(shakesRes);
			}
		}

		try (final InputStream is = SAMPLES.getHamletSample()) {
			try (final Resource hamletRes = testCollection.createResource("hamlet.xml", XMLResource.class)) {
				hamletRes.setContent(InputStreamUtil.readAll(is));
				testCollection.storeResource(hamletRes);
			}
		}
	}

    @AfterEach
    void tearDown() throws XMLDBException {
		//delete the test collection
		testCollection.close();
		try (final Collection parent = testCollection.getParentCollection()) {
			final CollectionManagementService service = parent.getService(CollectionManagementService.class);
			service.removeCollection(TEST_COLLECTION);
		}
	}

    @Disabled
    @Test
    void intersection1() throws XMLDBException {
		final String xpathPrefix = "doc('/db/" + TEST_COLLECTION + "/shakes.xsl')/*/*";
		final String query1 = xpathPrefix + "[position() >= 5 ]";
		final String query2 = xpathPrefix + "[position() <= 10]";
		final int expected = 87;

        final XPathQueryService service = testCollection.getService(XPathQueryService.class);

        try (final EXistResourceSet result1 = (EXistResourceSet) service.query(query1);
			 final EXistResourceSet result2 = (EXistResourceSet) service.query(query2)) {
			assertEquals(expected, ResourceSetHelper.intersection(result1, result2).getSize(), "size of intersection of " + query1 + " and " + query2 + " yields ");
		}
	}

    @Test
    void intersection2() throws XMLDBException {
	   	final String xpathPrefix = "doc('/db/" + TEST_COLLECTION + "/hamlet.xml')//LINE";
		final String query1 = xpathPrefix + "[fn:contains(. , 'funeral')]";		// count=4
		final String query2 = xpathPrefix + "[fn:contains(. , 'dirge')]";		// count=1, intersection=1
		final int expected = 1;

		final XPathQueryService service = testCollection.getService(XPathQueryService.class);

		try (final EXistResourceSet result1 = (EXistResourceSet) service.query(query1);
			 final EXistResourceSet result2 = (EXistResourceSet) service.query(query2)) {
			assertEquals(expected, ResourceSetHelper.intersection(result1, result2).getSize(), "size of intersection of " + query1 + " and " + query2 + " yields ");
		}
	}
}
