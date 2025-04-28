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

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.exist.TestUtils;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.Before;
import org.junit.Rule;

import javax.annotation.Nullable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

public abstract class AbstractTaskTest {

    protected static final String PROP_ANT_ADMIN_USER = "admin.user";
    protected static final String PROP_ANT_ADMIN_PASSWORD = "admin.password";

    protected static final String PROP_ANT_TEST_DATA_RESULT = "test.data.result";

    @Rule
    public BuildFileRule buildFileRule = new BuildFileRule();

    @Rule
    public final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    @Before
    public void setup() throws URISyntaxException {
        final URL buildFileUrl = getBuildFile();
        assertNotNull(buildFileUrl);

        final Path buildFile = Paths.get(buildFileUrl.toURI());
        buildFileRule.configureProject(buildFile.toAbsolutePath().toString());
        final Path path = Paths.get(getClass().getResource("user.xml").toURI());
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_ADMIN_USER, TestUtils.ADMIN_DB_USER);
        project.setProperty(PROP_ANT_ADMIN_PASSWORD, TestUtils.ADMIN_DB_PWD);
    }

    protected abstract @Nullable URL getBuildFile();
}
