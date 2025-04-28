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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BaseTaskTest extends AbstractTaskTest {

    @Parameters
    public static Iterable<? extends Object> data() {
        return Arrays.asList(
                UserTask.class.getSimpleName(),
                UserPasswordTask.class.getSimpleName(),
                AddUserTask.class.getSimpleName(),
                RemoveUserTask.class.getSimpleName(),
                AddGroupTask.class.getSimpleName(),
                RemoveGroupTask.class.getSimpleName(),
                ListUsersTask.class.getSimpleName(),
                ListGroupsTask.class.getSimpleName(),
                XMLDBCreateTask.class.getSimpleName(),
                XMLDBListTask.class.getSimpleName(),
                XMLDBExistTask.class.getSimpleName(),
                XMLDBStoreTask.class.getSimpleName(),
                XMLDBCopyTask.class.getSimpleName(),
                XMLDBMoveTask.class.getSimpleName(),
                XMLDBExtractTask.class.getSimpleName(),
                XMLDBRemoveTask.class.getSimpleName(),
                BackupTask.class.getSimpleName(),
                RestoreTask.class.getSimpleName(),
                ChmodTask.class.getSimpleName(),
                ChownTask.class.getSimpleName(),
                LockResourceTask.class.getSimpleName(),
                XMLDBXPathTask.class.getSimpleName(),
                XMLDBXQueryTask.class.getSimpleName(),
                XMLDBXUpdateTask.class.getSimpleName(),
                XMLDBShutdownTask.class.getSimpleName()
        );
    }

    @Parameter
    public String taskName;

    private static final String PROP_ANT_TEST_DATA_TASK_NAME  = "test.data.task.name";

    @Nullable
    @Override
    protected URL getBuildFile() {
        return getClass().getResource("base.xml");
    }

    @Test
    public void taskAvailable() {
        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_TASK_NAME, taskName);

        buildFileRule.executeTarget("taskAvailable");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertTrue(Boolean.parseBoolean(result));
    }
}
