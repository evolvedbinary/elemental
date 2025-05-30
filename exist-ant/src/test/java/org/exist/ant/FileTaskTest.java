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
import org.exist.TestUtils;
import org.exist.xmldb.EXistResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

import javax.annotation.Nullable;
import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FileTaskTest extends AbstractTaskTest {

    private static final String TEST_COLLECTION_NAME = "test";
    private static final String TEST_RESOURCE_NAME = "test.xml";

    private static final String PROP_ANT_TEST_DATA_TEST_COLLECTION  = "test.data.test.collection";
    private static final String PROP_ANT_TEST_DATA_TEST_RESOURCE  = "test.data.test.resource";
    private static final String PROP_ANT_TEST_DATA_USER =  "test.data.user";
    private static final String PROP_ANT_TEST_DATA_GROUP =  "test.data.group";

    @Nullable
    @Override
    protected URL getBuildFile() {
        return getClass().getResource("file.xml");
    }

    @Before
    public void fileSetup() throws XMLDBException {
        final Collection col = existEmbeddedServer.createCollection(existEmbeddedServer.getRoot(), TEST_COLLECTION_NAME);
        final Resource res = col.createResource(TEST_RESOURCE_NAME, XMLResource.class);
        res.setContent("<test/>");
        col.storeResource(res);
    }

    @After
    public void fileCleanup() throws XMLDBException {
        final CollectionManagementService service = existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
        service.removeCollection(TEST_COLLECTION_NAME);
    }

    @Test
    public void chmod() throws XMLDBException {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE, TEST_RESOURCE_NAME);

        buildFileRule.executeTarget("chmod");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertThat(result, containsString(TEST_RESOURCE_NAME));

        final Collection col = existEmbeddedServer.getRoot().getChildCollection(TEST_COLLECTION_NAME);
        final EXistResource res = (EXistResource)col.getResource(TEST_RESOURCE_NAME);
        assertEquals("---rwxrwx", res.getPermissions().toString());
    }

    @Test
    public void chown() throws XMLDBException {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TEST_COLLECTION, TEST_COLLECTION_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_TEST_RESOURCE, TEST_RESOURCE_NAME);
        project.setProperty(PROP_ANT_TEST_DATA_USER, TestUtils.GUEST_DB_USER);
        project.setProperty(PROP_ANT_TEST_DATA_GROUP, TestUtils.GUEST_DB_USER);

        buildFileRule.executeTarget("chown");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertThat(result, containsString(TEST_RESOURCE_NAME));

        final Collection col = existEmbeddedServer.getRoot().getChildCollection(TEST_COLLECTION_NAME);
        final EXistResource res = (EXistResource)col.getResource(TEST_RESOURCE_NAME);
        assertEquals(TestUtils.GUEST_DB_USER, res.getPermissions().getOwner().getName());
        assertEquals(TestUtils.GUEST_DB_USER, res.getPermissions().getGroup().getName());
    }

    @Ignore("Would require implementing an UnlockResourceTask as well")
    @Test
    public void lockResource() {
        buildFileRule.executeTarget("lockResource");
    }
}
