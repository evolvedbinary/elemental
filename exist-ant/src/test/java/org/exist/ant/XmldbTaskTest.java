/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.ant;

import org.apache.tools.ant.Project;
import org.exist.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.*;

public class XmldbTaskTest extends AbstractTaskTest {

    private static final String TEST_COLLECTION_NAME = "test";
    private static final String TEST_COLLECTION2_NAME = "test2";
    private static final String OTHER_TEST_COLLECTION_NAME = "other-test";
    private static final String TEST_RESOURCE_NAME = "test.xml";
    private static final String TEST_RESOURCE2_NAME = "test2.xml";
    private static final String OTHER_TEST_RESOURCE_NAME = "other-test.xml";
    private static final String BIN_TEST_RESOURCE_NAME = "bin-test.bin";

    private static final String PROP_ANT_TEST_DATA_TEST_COLLECTION  = "test.data.test.collection";
    private static final String PROP_ANT_TEST_DATA_TEST_COLLECTION2  = "test.data.test.collection2";
    private static final String PROP_ANT_TEST_DATA_TEST_RESOURCE  = "test.data.test.resource";
    private static final String PROP_ANT_TEST_DATA_TEST_RESOURCE2  = "test.data.test.resource2";
    private static final String PROP_ANT_TEST_DATA_BIN_TEST_RESOURCE  = "test.data.bin.test.resource";

    private static final String PROP_ANT_TEST_DATA_TMP_DIR  = "test.data.tmp.dir";
    private static final String PROP_ANT_TEST_DATA_TMP_FILE  = "test.data.tmp.file";
    private static final String PROP_ANT_TEST_DATA_TMP_FILE_NAME  = "test.data.tmp.file.name";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Nullable
    @Override
    protected URL getBuildFile() {
        return getClass().getResource("xmldb.xml");
    }

    @Before
    public void fileSetup() throws XMLDBException {
        final Collection col = existEmbeddedServer.createCollection(existEmbeddedServer.getRoot(), TEST_COLLECTION_NAME);

        final Resource res = col.createResource(TEST_RESOURCE_NAME, XMLResource.class);
        res.setContent("<test>hello <subject>world</subject></test>");
        col.storeResource(res);

        final Resource binResource = col.createResource(BIN_TEST_RESOURCE_NAME, BinaryResource.class);
        binResource.setContent("blah blah");
        col.storeResource(binResource);

        final CollectionManagementService service = col.getService(CollectionManagementService.class);
        final Collection otherCol = service.createCollection(OTHER_TEST_COLLECTION_NAME);
        final Resource otherRes = otherCol.createResource(OTHER_TEST_RESOURCE_NAME, XMLResource.class);
        otherRes.setContent("<test>other</test>");
        otherCol.storeResource(otherRes);
        otherCol.close();

        col.close();
    }

    @After
    public void fileCleanup() throws XMLDBException {
        final CollectionManagementService service = existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
        service.removeCollection(TEST_COLLECTION_NAME);
    }

    @Test
    public void copy() {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE, TEST_RESOURCE_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE2, TEST_RESOURCE2_NAME);

        buildFileRule.executeTarget("copy");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertThat(result, allOf(containsString(TEST_RESOURCE_NAME), containsString(TEST_RESOURCE2_NAME)));
    }

    @Test
    public void create() {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION2, TEST_COLLECTION2_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE, TEST_RESOURCE_NAME);

        buildFileRule.executeTarget("create");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertThat(result, containsString(TEST_COLLECTION2_NAME));
    }

    @Test
    public void exists() {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE, TEST_RESOURCE_NAME);

        buildFileRule.executeTarget("exists");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertTrue(Boolean.parseBoolean(result));
    }

    @Test
    public void extract() throws IOException {
        final Path tmpFile = temporaryFolder.newFile().toPath();

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE, TEST_RESOURCE_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TMP_FILE, tmpFile.toAbsolutePath().toString());

        buildFileRule.executeTarget("extract");

        assertTrue(Files.exists(tmpFile));
    }

    @Test
    public void extractCreateDirectories() throws IOException {
        final Path tmpDir = temporaryFolder.newFolder().toPath();
        Files.createDirectories(tmpDir);

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE, TEST_RESOURCE_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TMP_DIR, tmpDir.toAbsolutePath().toString());

        buildFileRule.executeTarget("extractCreateDirectories");

        assertTrue(Files.exists(tmpDir));
        assertTrue(Files.exists(tmpDir.resolve(TEST_RESOURCE_NAME)));
        assertTrue(Files.exists(tmpDir.resolve(OTHER_TEST_COLLECTION_NAME)));
        assertTrue(Files.exists(tmpDir.resolve(OTHER_TEST_COLLECTION_NAME).resolve(OTHER_TEST_RESOURCE_NAME)));
    }

    @Test
    public void extractCreateDirectoriesDestDir() throws IOException {
        final Path tmpFile = temporaryFolder.newFolder().toPath().resolve("new-sub-dir").resolve(TEST_RESOURCE_NAME);

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE, TEST_RESOURCE_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TMP_FILE, tmpFile.toAbsolutePath().toString());

        buildFileRule.executeTarget("extractCreateDirectoriesDestDir");

        assertTrue(Files.exists(tmpFile));
    }

    @Test
    public void extractCreateDirectoriesOverwriteFile() throws IOException {
        final Path tmpFile = temporaryFolder.newFile(TEST_RESOURCE_NAME).toPath();
        assertTrue(Files.exists(tmpFile)); // to ensure we can overwrite from Ant task

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE, TEST_RESOURCE_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TMP_FILE, tmpFile.toAbsolutePath().toString());

        buildFileRule.executeTarget("extractCreateDirectoriesOverwriteFile");

        assertTrue(Files.exists(tmpFile));
    }

    @Test
    public void extractCreateDirectoriesOverwriteDir() throws IOException {
        final Path tmpDir = temporaryFolder.newFolder().toPath();
        assertTrue(Files.exists(tmpDir)); // to ensure we can overwrite from Ant task

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TMP_DIR, tmpDir.toAbsolutePath().toString());

        buildFileRule.executeTarget("extractCreateDirectoriesOverwriteDir");

        assertTrue(Files.exists(tmpDir));
    }

    @Test
    public void extractBinary() throws IOException {
        final Path tmpFile = temporaryFolder.newFile().toPath();

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_BIN_TEST_RESOURCE, BIN_TEST_RESOURCE_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TMP_FILE, tmpFile.toAbsolutePath().toString());

        buildFileRule.executeTarget("extractBinary");

        assertTrue(Files.exists(tmpFile));
    }

    @Test
    public void list() {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);

        buildFileRule.executeTarget("list");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertThat(result, allOf(containsString(TEST_RESOURCE_NAME), containsString(BIN_TEST_RESOURCE_NAME)));
    }

    @Test
    public void move() {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE, TEST_RESOURCE_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE2, TEST_RESOURCE2_NAME);

        buildFileRule.executeTarget("move");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertTrue(Boolean.parseBoolean(result));
    }

    @Test
    public void store() throws IOException {
        final Path tmpFile = temporaryFolder.newFile().toPath();
        Files.write(tmpFile, "<hello/>".getBytes(UTF_8));

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TMP_FILE, tmpFile.toAbsolutePath().toString());
        project.setProperty(PROP_ANT_TEST_DATA_TMP_FILE_NAME, FileUtils.fileName(tmpFile));

        buildFileRule.executeTarget("store");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertTrue(Boolean.parseBoolean(result));
    }

    @Test
    public void storeEmptyFile() throws IOException {
        final Path tmpFile = temporaryFolder.newFile().toPath();

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TMP_FILE, tmpFile.toAbsolutePath().toString());
        project.setProperty(PROP_ANT_TEST_DATA_TMP_FILE_NAME, FileUtils.fileName(tmpFile));

        buildFileRule.executeTarget("store");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertFalse(Boolean.parseBoolean(result));
    }

    @Test
    public void xpath() {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);

        buildFileRule.executeTarget("xpath");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertEquals("world", result.trim());
    }

    @Test
    public void xpathXml() {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);

        buildFileRule.executeTarget("xpathXml");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertThat(result.replaceAll("\\s", ""), allOf(containsString("<test>hello<subject>world</subject></test>"), containsString("<test>other</test>")));
    }

    @Test
    public void xquery() {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);

        buildFileRule.executeTarget("xquery");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertEquals("<subject>world</subject>", result);
    }

    @Test
    public void xupdate() {
        buildFileRule.executeTarget("xupdate");
    }
}
