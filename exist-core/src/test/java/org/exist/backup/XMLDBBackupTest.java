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
package org.exist.backup;

import org.exist.TestUtils;
import org.exist.http.RESTTest;
import org.exist.xmldb.AbstractRestoreServiceTaskListener;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.EXistRestoreService;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class XMLDBBackupTest extends RESTTest {
    private static final String PORT_PLACEHOLDER = "${PORT}";

    private static final String COLLECTION_NAME = "test-xmldb-backup-restore";

    @TempDir
    Path tempDir;

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("local (classic)", XmldbURI.EMBEDDED_SERVER_URI.toString(), false),
                Arguments.of("remote (classic)", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc", false),
                Arguments.of("local (dedup)", XmldbURI.EMBEDDED_SERVER_URI.toString(), false),
                Arguments.of("remote (dedup)", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc", true)
        );
    }

    private String apiName;
    private String baseUri;
    private boolean deduplicateBlobs;

    private static final String DOC1_NAME = "doc1.xml";
    private final String doc1Content = "<timestamp>" + System.nanoTime() + "</timestamp>";

    private static final String BIN_DOC1_NAME = "doc1.bin";
    private final String binDoc1Content = Long.toString(System.nanoTime());
    private static final String BIN_DOC2_NAME = "doc2.bin";
    private final String binDoc2Content = Long.toString(System.nanoTime());

    private String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(existWebServer.getPort()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void backupRestore(final String apiName, final String baseUri, final boolean deduplicateBlobs) throws XMLDBException, SAXException, IOException, URISyntaxException, ParserConfigurationException {
        // assign parameters to fields for helper methods
        this.apiName = apiName;
        this.baseUri = baseUri;
        this.deduplicateBlobs = deduplicateBlobs;

        // prepare test collection
        beforeEachSetup();

        final XmldbURI collectionUri = XmldbURI.create(getBaseUri()).append("/db").append(COLLECTION_NAME);

        // backup the collection
        final Path backupFile = backup("test-xmldb-backup-" + System.currentTimeMillis() + ".zip", collectionUri, tempDir, deduplicateBlobs);

        // delete the collection
        deleteCollection(collectionUri);

        // restore the collection
        restore(backupFile, XmldbURI.create(getBaseUri()).append("/db"));

        // check restore has restored the collection
        final Collection testCollection = DatabaseManager.getCollection(collectionUri.toString(), TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        assertNotNull(testCollection);

        try (final EXistResource doc1 = (EXistResource) testCollection.getResource(DOC1_NAME)) {
            assertNotNull(doc1);
            final Source expected = Input.fromString(doc1Content).build();
            final Source actual = Input.fromString(doc1.getContent().toString()).build();
            final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForIdentical()
                .build();
            assertFalse(diff.hasDifferences(), diff.toString());
        }

        try (final EXistResource binDoc1 = (EXistResource) testCollection.getResource(BIN_DOC1_NAME)) {
            assertEquals(binDoc1Content, new String((byte[]) binDoc1.getContent(), UTF_8));
        }

        try (final EXistResource binDoc2 = (EXistResource) testCollection.getResource(BIN_DOC2_NAME)) {
            assertEquals(binDoc2Content, new String((byte[]) binDoc2.getContent(), UTF_8));
        }
    }

    private Path backup(final String filename, final XmldbURI collectionUri, final Path tempDir, final boolean deduplicateBlobs) throws IOException, XMLDBException, SAXException {
        final Path backupFile = Files.createFile(tempDir.resolve(filename));
        final Backup backup = new Backup(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD,
                backupFile,
                collectionUri,
                null,
                deduplicateBlobs);
        backup.backup(false, null);
        return backupFile;
    }

    private void restore(final Path backupFile, final XmldbURI collectionUri) throws XMLDBException, SAXException, URISyntaxException, ParserConfigurationException, IOException {
        final Collection collection = DatabaseManager.getCollection(collectionUri.toString(), TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        final EXistRestoreService restoreService = (EXistRestoreService) collection.getService("RestoreService", "1.0");
        final TestRestoreListener listener = new TestRestoreListener();
        restoreService.restore(backupFile.normalize().toAbsolutePath().toString(), null, listener, false);
    }

    private void deleteCollection(final XmldbURI collectionUri) throws XMLDBException {
        final Collection parent = DatabaseManager.getCollection(collectionUri.removeLastSegment().toString(), TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        final CollectionManagementService colService = (CollectionManagementService) parent.getService("CollectionManagementService", "1.0");
        colService.removeCollection(collectionUri.lastSegment().toString());
    }

    // not annotated: invoked manually from the parameterized test with params initialized
    void beforeEachSetup() throws XMLDBException {
        final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        final CollectionManagementService colService = (CollectionManagementService) root.getService("CollectionManagementService", "1.0");
        final Collection testCollection = colService.createCollection(COLLECTION_NAME);
        assertNotNull(testCollection);

        try (final EXistResource doc1 = (EXistResource) testCollection.createResource(DOC1_NAME, XMLResource.RESOURCE_TYPE)) {
            doc1.setContent(doc1Content);
            testCollection.storeResource(doc1);
        }

        try (final EXistResource binDoc1 = (EXistResource) testCollection.createResource(BIN_DOC1_NAME, BinaryResource.RESOURCE_TYPE)) {
            binDoc1.setContent(binDoc1Content);
            testCollection.storeResource(binDoc1);
        }

        try (final EXistResource binDoc2 = (EXistResource) testCollection.createResource(BIN_DOC2_NAME, BinaryResource.RESOURCE_TYPE)) {
            binDoc2.setContent(binDoc2Content);
            testCollection.storeResource(binDoc2);
        }
    }

    private static class TestRestoreListener extends AbstractRestoreServiceTaskListener {
        final List<String> restored = new ArrayList<>();

        @Override
        public void createdCollection(final String collection) {
            restored.add(collection);
        }

        @Override
        public void restoredResource(final String resource) {
            restored.add(resource);
        }

        @Override
        public void info(final String message) {
        }

        @Override
        public void warn(final String message) {
        }

        @Override
        public void error(final String message) {
        }
    }
}
