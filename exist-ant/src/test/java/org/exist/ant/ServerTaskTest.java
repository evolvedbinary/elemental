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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServerTaskTest extends AbstractTaskTest {

    private static final String PROP_ANT_TEST_DATA_BACKUP_DIR = "test.data.backup.dir";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Nullable
    @Override
    protected URL getBuildFile() {
        return getClass().getResource("server.xml");
    }

    @Test
    public void backup() throws IOException {
        final Project project = buildFileRule.getProject();
        final Path backupDir = temporaryFolder.newFolder().toPath();
        project.setProperty(PROP_ANT_TEST_DATA_BACKUP_DIR, backupDir.toAbsolutePath().toString());

        buildFileRule.executeTarget("backup");

        assertTrue(Files.exists(backupDir.resolve("db").resolve("__contents__.xml")));
    }

    @Test
    public void restore() throws URISyntaxException, XMLDBException {
        final URL backupContentsUrl = getClass().getResource("backup-test/db/__contents__.xml");
        assertNotNull(backupContentsUrl);
        final Path backupDir = Paths.get(backupContentsUrl.toURI()).getParent().getParent();

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_BACKUP_DIR, backupDir.toAbsolutePath().toString());

        buildFileRule.executeTarget("restore");

        final Resource res = existEmbeddedServer.getRoot().getResource("example.xml");
        assertNotNull(res);
    }

    @Test
    public void backupRestore() throws IOException {
        final Project project = buildFileRule.getProject();
        final Path backupDir = temporaryFolder.newFolder().toPath();
        project.setProperty(PROP_ANT_TEST_DATA_BACKUP_DIR, backupDir.toAbsolutePath().toString());

        buildFileRule.executeTarget("backup");

        buildFileRule.executeTarget("restore");
    }

    @Test
    public void shutdown() {
        buildFileRule.executeTarget("shutdown");
    }
}
