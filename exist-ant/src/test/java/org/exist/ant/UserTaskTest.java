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
import org.junit.Test;

import javax.annotation.Nullable;
import java.net.URL;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class UserTaskTest extends AbstractTaskTest {

    private static final String PROP_ANT_TEST_DATA_USER =  "test.data.user";
    private static final String PROP_ANT_TEST_DATA_PASSWORD = "test.data.password";
    private static final String PROP_ANT_TEST_DATA_PASSWORD_CHANGED = "test.data.password.changed";

    @Nullable
    @Override
    protected URL getBuildFile() {
        return getClass().getResource("user.xml");
    }

    @Test
    public void addUser() {
        final String testUsername = "test-user-1";
        final String testPassword = "test-user-1-password";

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_USER, testUsername);
        project.setProperty(PROP_ANT_TEST_DATA_PASSWORD, testPassword);

        buildFileRule.executeTarget("addUser");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertThat(result, containsString(testUsername));
    }

    @Test
    public void listUser() {
        final Project project = buildFileRule.getProject();

        buildFileRule.executeTarget("listUser");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertThat(result, containsString(TestUtils.ADMIN_DB_USER));
        assertThat(result, containsString(TestUtils.GUEST_DB_USER));
    }

    @Test
    public void changePassword() {
        final String testUsername = "test-user-1";
        final String testPassword = "test-user-1-password";
        final String testPasswordChanged = "test-user-1-password-changed";

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_USER, testUsername);
        project.setProperty(PROP_ANT_TEST_DATA_PASSWORD, testPassword);
        project.setProperty(PROP_ANT_TEST_DATA_PASSWORD_CHANGED, testPasswordChanged);

        buildFileRule.executeTarget("changePassword");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertEquals("<changed>" + testPasswordChanged + "</changed>", result.trim());
    }

    @Test
    public void removeUser() {
        final String testUsername = "test-user-1";
        final String testPassword = "test-user-1-password";

        final Project project = buildFileRule.getProject();
        project.setProperty(PROP_ANT_TEST_DATA_USER, testUsername);
        project.setProperty(PROP_ANT_TEST_DATA_PASSWORD, testPassword);

        buildFileRule.executeTarget("removeUser");

        final String result = project.getProperty(PROP_ANT_TEST_DATA_RESULT);
        assertThat(result, not(containsString(testUsername)));
    }
}
