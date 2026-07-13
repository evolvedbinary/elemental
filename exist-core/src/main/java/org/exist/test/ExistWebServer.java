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
package org.exist.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.TestUtils;
import org.exist.collections.triggers.TriggerException;
import org.exist.jetty.JettyStart;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.journal.Journal;
import org.exist.util.FileUtils;
import org.exist.util.LockException;
import org.junit.rules.ExternalResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.exist.util.IPUtil.nextFreePort;
import static org.junit.Assert.fail;
import static org.exist.repo.AutoDeploymentTrigger.AUTODEPLOY_PROPERTY;

/**
 * Exist Jetty Web Server Rule for JUnit
 */
public class ExistWebServer extends ExternalResource {

    private static final Logger LOG =  LogManager.getLogger(ExistWebServer.class);

    public static final String USE_TEMPORARY_STORAGE_PROPERTY = "exist.use-temporary-storage";

    private static final String PROP_JETTY_PORT = "jetty.port";
    private static final String PROP_JETTY_SECURE_PORT = "jetty.httpConfig.securePort";
    private static final String PROP_JETTY_SSL_PORT = "jetty.ssl.port";

    private static final int MIN_RANDOM_PORT = 49152;
    private static final int MAX_RANDOM_PORT = 65535;
    private static final int MAX_RANDOM_PORT_ATTEMPTS = 10;

    private JettyStart server = null;

    private final boolean useRandomPort;
    private final boolean cleanupDbOnShutdown;
    private final Properties configProperties;
    private final boolean disableAutoDeploy;
    private final boolean useTemporaryStorage;
    private @Nullable Path temporaryStorage = null;
    private final boolean jettyStandaloneMode;

    public ExistWebServer() {
        this(false);
    }

    public ExistWebServer(final boolean useRandomPort) {
        this(useRandomPort, false);
    }

    public ExistWebServer(final boolean useRandomPort, final boolean cleanupDbOnShutdown) {
        this(useRandomPort, cleanupDbOnShutdown, false);
    }

    public ExistWebServer(final boolean useRandomPort, final boolean cleanupDbOnShutdown, final boolean disableAutoDeploy) {
        this(useRandomPort, cleanupDbOnShutdown, disableAutoDeploy, false);
    }

    public ExistWebServer(final boolean useRandomPort, final boolean cleanupDbOnShutdown, final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        this(useRandomPort, cleanupDbOnShutdown, disableAutoDeploy, useTemporaryStorage, true);
    }

    public ExistWebServer(final boolean useRandomPort, final boolean cleanupDbOnShutdown, final boolean disableAutoDeploy, final boolean useTemporaryStorage, final boolean jettyStandaloneMode) {
        this(useRandomPort, cleanupDbOnShutdown, null, disableAutoDeploy, useTemporaryStorage, jettyStandaloneMode);
    }

    public ExistWebServer(final boolean useRandomPort, final boolean cleanupDbOnShutdown, @Nullable final Properties configProperties, final boolean disableAutoDeploy, final boolean useTemporaryStorage, final boolean jettyStandaloneMode) {
        this.useRandomPort = useRandomPort;
        this.cleanupDbOnShutdown = cleanupDbOnShutdown;
        this.configProperties = configProperties != null ? configProperties : new Properties();
        this.disableAutoDeploy = disableAutoDeploy;
        this.useTemporaryStorage = useTemporaryStorage;
        this.jettyStandaloneMode = jettyStandaloneMode;
    }

    public final int getPort() {
        if(server != null) {
            return server.getPrimaryPort();
        } else {
            throw new IllegalStateException("ExistWebServer is not running");
        }
    }

    @Override
    protected void before() throws Throwable {
        if (server == null) {
            final boolean propUseTemporaryStorage = Boolean.parseBoolean(System.getProperty(USE_TEMPORARY_STORAGE_PROPERTY, "false"));
            if (useTemporaryStorage || propUseTemporaryStorage) {
                if (temporaryStorage == null) {
                    this.temporaryStorage = Files.createTempDirectory("org.exist.test.ExistWebServer");
                }
                configProperties.put(BrokerPool.PROPERTY_DATA_DIR, temporaryStorage);
                configProperties.put(Journal.PROPERTY_RECOVERY_JOURNAL_DIR, temporaryStorage);
                LOG.info("Using temporary storage location: {}", temporaryStorage.toAbsolutePath().toString());
            }

            final Properties jettyConfigProperties = new Properties();
            if(useRandomPort) {
                jettyConfigProperties.setProperty(PROP_JETTY_PORT, Integer.toString(nextFreePort(MIN_RANDOM_PORT, MAX_RANDOM_PORT, MAX_RANDOM_PORT_ATTEMPTS)));
                jettyConfigProperties.setProperty(PROP_JETTY_SECURE_PORT, Integer.toString(nextFreePort(MIN_RANDOM_PORT, MAX_RANDOM_PORT, MAX_RANDOM_PORT_ATTEMPTS)));
                jettyConfigProperties.setProperty(PROP_JETTY_SSL_PORT, Integer.toString(nextFreePort(MIN_RANDOM_PORT, MAX_RANDOM_PORT, MAX_RANDOM_PORT_ATTEMPTS)));
            }

            if (disableAutoDeploy) {
                // NOTE(AR) will be processed in JettyStart
                configProperties.setProperty(AUTODEPLOY_PROPERTY, "off");
            }

            server = new JettyStart(configProperties, jettyConfigProperties);
            server.run(jettyStandaloneMode);

        } else {
            throw new IllegalStateException("ExistWebServer already running");
        }

        super.before();
    }

    public void restart() {
        if(server != null) {
            try {
                server.shutdown();
                server.run();
            } catch (final Throwable t) {
                throw new RuntimeException(t);
            }
        } else {
            throw new IllegalStateException("ExistWebServer already stopped");
        }
    }

    @Override
    protected void after() {
        if(server != null) {
            if(cleanupDbOnShutdown) {
                try {
                    TestUtils.cleanupDB();
                } catch (final EXistException | PermissionDeniedException | LockException | IOException | TriggerException e) {
                    fail(e.getMessage());
                }
            }
            server.shutdown();
            server = null;

            final boolean propUseTemporaryStorage = Boolean.parseBoolean(System.getProperty(USE_TEMPORARY_STORAGE_PROPERTY, "false"));
            if((useTemporaryStorage || propUseTemporaryStorage) && temporaryStorage != null) {
                FileUtils.deleteQuietly(temporaryStorage);
                temporaryStorage = null;
            }
        } else {
            throw new IllegalStateException("ExistWebServer already stopped");
        }

        super.after();
    }
}
