/*
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
package org.exist.storage;

import org.exist.security.SecurityManager;
import org.exist.security.internal.aider.UserAider;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.util.Configuration;
import org.junit.ClassRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for startup triggers.
 */
public class StartupTriggerTest {

    private final static String USER = "testuser1";
    private final static String PASSWORD = "testpass";

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(null, null, properties(), true, true);

    public static Properties properties() {
        final List<Configuration.StartupTriggerConfig> startupTriggers = new ArrayList<>();
        startupTriggers.add(new Configuration.StartupTriggerConfig(TestStartupTrigger.class.getName(), null));

        final Properties properties = new Properties();
        properties.put(BrokerPool.PROPERTY_STARTUP_TRIGGERS, startupTriggers);
        return properties;
    }

    /**
     * Check if startup trigger has access to security manager.
     */
    @Test
    void createUser() throws DatabaseConfigurationException, EXistException, IOException {
        assertTrue(TestStartupTrigger.completed);
    }

    public static class TestStartupTrigger implements StartupTrigger {

        static volatile boolean completed = false;

        @Override
        public void execute(final DBBroker sysBroker, final Txn transaction, final Map<String, List<? extends Object>> params) {
            final SecurityManager secman = sysBroker.getBrokerPool().getSecurityManager();
            if (!secman.hasAccount(USER)) {
                final UserAider aider = new UserAider(USER);
                aider.setPassword(PASSWORD);

                Assertions.assertDoesNotThrow(() -> {
                    secman.addAccount(sysBroker, aider);
                });

                assertTrue(secman.hasAccount(USER));

                Assertions.assertDoesNotThrow(() -> {
                    secman.deleteAccount(USER);
                });
            }

            completed = true;
        }
    }
}
