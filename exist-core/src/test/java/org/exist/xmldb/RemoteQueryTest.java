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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.exist.test.TestConstants;
import org.exist.util.io.InputStreamUtil;
import org.exist.xmlrpc.XmlRpcTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;
import xyz.elemental.mediatype.MediaType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.exist.samples.Samples.SAMPLES;

public class RemoteQueryTest extends RemoteDBTest {
	private Collection testCollection;
	private Collection xmlrpcCollection;

	@Test
	public void resourceSet() throws XMLDBException {
		String query = "//SPEECH[SPEAKER = 'HAMLET']";
		XQueryService service = (XQueryService) testCollection.getService("XQueryService", "1.0");
		service.setProperty("highlight-matches", "none");
		final CompiledExpression compiled = service.compile(query);
		try (final EXistResourceSet result = (EXistResourceSet) service.execute(compiled)) {

            assertEquals(result.getSize(), 359);

            for (int i = 0; i < result.getSize(); i++) {
                try (final EXistResource r = (EXistResource) result.getResource(i)) {
                    final Node node = ((XMLResource) r).getContentAsDOM().getFirstChild();
                }
            }
        }
	}

	@Test
	public void externalVar() throws XMLDBException {
        String query = XmlRpcTest.QUERY_MODULE_DATA;
        XQueryService service = (XQueryService) testCollection.getService("XQueryService", "1.0");
        service.setProperty("highlight-matches", "none");

        service.setNamespace("tm", "http://exist-db.org/test/module");
        service.setNamespace("tm-query", "http://exist-db.org/test/module/query");

        service.declareVariable("tm:imported-external-string", "imported-string-value");
        service.declareVariable("tm-query:local-external-string", "local-string-value");

        final CompiledExpression compiled = service.compile(query);
        try (final EXistResourceSet result = (EXistResourceSet) service.execute(compiled)) {

            assertEquals(result.getSize(), 2);

            for (int i = 0; i < result.getSize(); i++) {
                try (final EXistResource r = (EXistResource) result.getResource(i)) {
                    // needed to ensure that r is closed
                }
            }
        }
	}

	@Before
	public void setUp() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XMLDBException, URISyntaxException, IOException {
        // initialize driver
        final Class<?> cl = Class.forName("org.exist.xmldb.DatabaseImpl");
        final Database database = (Database) cl.newInstance();
        database.setProperty("create-database", "true");
        DatabaseManager.registerDatabase(database);

        try (final Collection root = DatabaseManager.getCollection(getUri() + XmldbURI.ROOT_COLLECTION, "admin", null)) {
            final CollectionManagementService service = (CollectionManagementService) root.getService("CollectionManagementService", "1.0");

            testCollection = service.createCollection("test");
            assertNotNull(testCollection);

            try (final EXistResource xr = (EXistResource) testCollection.createResource("hamlet.xml", "XMLResource");
                 final InputStream is = SAMPLES.getHamletSample()) {
                xr.setContent(InputStreamUtil.readString(is, UTF_8));
                testCollection.storeResource(xr);
            }

            xmlrpcCollection = service.createCollection("xmlrpc");
            assertNotNull(xmlrpcCollection);

            try (final EXistResource br = (EXistResource) xmlrpcCollection.createResource(TestConstants.TEST_MODULE_URI.toString(), "BinaryResource")) {
                br.setMediaType(MediaType.APPLICATION_XQUERY);
                br.setContent(XmlRpcTest.MODULE_DATA);
                xmlrpcCollection.storeResource(br);
            }
        }
	}

	@After
	public void tearDown() throws Exception {
        xmlrpcCollection.close();
        testCollection.close();

        if (!((EXistCollection) testCollection).isRemoteCollection()) {
            DatabaseInstanceManager dim =
                (DatabaseInstanceManager) testCollection.getService(
                        "DatabaseInstanceManager", "1.0");
            dim.shutdown();
        }

        testCollection = null;
        xmlrpcCollection = null;
	}
}
