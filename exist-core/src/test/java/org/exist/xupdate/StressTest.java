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
package org.exist.xupdate;

import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

import org.exist.EXistException;
import org.exist.TestUtils;

import org.exist.collections.triggers.TriggerException;
import org.exist.security.PermissionDeniedException;
import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.util.LockException;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.XmldbURI;
import org.exist.xmldb.concurrent.DBUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XPathQueryService;
import org.xmldb.api.modules.XUpdateQueryService;

/**
 * @author wolf
 */
public class StressTest {

    private static final String XML = "<root><a/><b/><c/></root>";

    private final static int RUNS = 1000;

    private Collection testCol;
    private final Random rand = new Random();

    private String[] tags;

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    @Test
    void stressTest() throws XMLDBException {
        insertTags();
        removeTags();
        fetchDb();
    }

    private void insertTags() throws XMLDBException {
        final XUpdateQueryService service = testCol.getService(XUpdateQueryService.class);
        final XPathQueryService xquery = testCol.getService(XPathQueryService.class);

        final String[] tagsWritten = new String[RUNS];
        for (int i = 0; i < RUNS; i++) {
            final String tag = tags[i];
            final String parent;
            if (i > 0 && rand.nextInt(100) < 70) {
                parent = "//" + tagsWritten[rand.nextInt(i) / 2];
            } else {
                parent = "/root";
            }
            final String xupdate =
                    "<xupdate:modifications version=\"1.0\" xmlns:xupdate=\"http://www.xmldb.org/xupdate\">" +
                            "<xupdate:append select=\"" + parent + "\">" +
                            "<xupdate:element name=\"" + tag + "\"/>" +
                            "</xupdate:append>" +
                            "</xupdate:modifications>";

            final long mods = service.updateResource("test.xml", xupdate);
            assertEquals(1, mods);

            tagsWritten[i] = tag;

            final String query = "//" + tagsWritten[rand.nextInt(i + 1)];
            try (final EXistResourceSet result = (EXistResourceSet) xquery.query(query)) {
                assertEquals(1, result.getSize());
            }
        }

        try (final XMLResource res = (XMLResource) testCol.getResource("test.xml")) {
            assertNotNull(res);
        }
    }

    private void removeTags() throws XMLDBException {
        final XUpdateQueryService service = testCol.getService(XUpdateQueryService.class);
        final int start = rand.nextInt(RUNS / 4);
        for (int i = start; i < RUNS; i++) {
            final String xupdate =
                    "<xupdate:modifications version=\"1.0\" xmlns:xupdate=\"http://www.xmldb.org/xupdate\">" +
                            "<xupdate:remove select=\"//" + tags[i] + "\"/>" +
                            "</xupdate:modifications>";

            @SuppressWarnings("unused")
            long mods = service.updateResource("test.xml", xupdate);

            i += rand.nextInt(3);
        }
    }

    private void fetchDb() throws XMLDBException {
        final XPathQueryService xquery = testCol.getService(XPathQueryService.class);
        try (final EXistResourceSet result = (EXistResourceSet) xquery.query("for $n in collection('" + XmldbURI.ROOT_COLLECTION + "/test')//* return local-name($n)")) {

            for (int i = 0; i < result.getSize(); i++) {
                try (final Resource r = result.getResource(i)) {
                    final String tag = r.getContent().toString();

                    try (final EXistResourceSet result2 = (EXistResourceSet) xquery.query("//" + tag)) {
                        assertEquals(1, result2.getSize());
                    }
                }
            }
        }
    }

    @BeforeEach
    void setUp() throws XMLDBException {
        final Collection rootCol = XMLDB_EMBEDDED_DATABASE.getRoot();
        testCol = rootCol.getChildCollection(XmldbURI.ROOT_COLLECTION + "/test");
        if (testCol != null) {
            final CollectionManagementService mgr = DBUtils.getCollectionManagementService(rootCol);
            mgr.removeCollection(XmldbURI.ROOT_COLLECTION + "/test");
        }

        testCol = DBUtils.addCollection(rootCol, "test");
        assertNotNull(testCol);

        tags = IntStream
                .range(0, RUNS)
                .mapToObj(i -> "TAG" + i)
                .toArray(String[]::new);

        DBUtils.addXMLResource(testCol, "test.xml", XML);
    }

    @AfterEach
    void tearDown() throws XMLDBException, LockException, TriggerException, PermissionDeniedException, EXistException, IOException {
        testCol.close();

        TestUtils.cleanupDB();
    }
}
