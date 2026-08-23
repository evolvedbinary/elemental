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
import org.exist.repo.AutoDeploymentTrigger;
import org.exist.start.Classpath;
import org.exist.start.EXistClassLoader;
import org.exist.storage.BrokerPool;
import org.exist.storage.BrokerPoolConstants;
import org.exist.storage.journal.Journal;
import org.exist.util.Configuration;
import org.exist.util.ConfigurationHelper;
import org.exist.util.DatabaseConfigurationException;
import org.exist.util.FileUtils;
import org.junit.rules.ExternalResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Exist embedded Server Rule for JUnit.
 */
public class ExistEmbeddedServer extends ExternalResource {

    private static final Logger LOG =  LogManager.getLogger(ExistEmbeddedServer.class);

    public static final String USE_TEMPORARY_STORAGE_PROPERTY = "exist.use-temporary-storage";

    private final String instanceName;
    private final Path home;
    private @Nullable final Path configFile;
    private final Properties configProperties;
    private final boolean useTemporaryStorage;
    private final boolean disableAutoDeploy;
    private @Nullable Path temporaryStorage = null;

    private BrokerPool pool = null;

    public ExistEmbeddedServer() {
        this(null, null, null, false, false);
    }

    public ExistEmbeddedServer(final boolean useTemporaryStorage) {
        this(null, null, null, false, useTemporaryStorage);
    }

    public ExistEmbeddedServer(final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        this(null, null, null, disableAutoDeploy, useTemporaryStorage);
    }

    public ExistEmbeddedServer(final Properties configProperties) {
        this(null, null, configProperties, false, false);
    }

    public ExistEmbeddedServer(final Properties configProperties, final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        this(null, null, configProperties, disableAutoDeploy, useTemporaryStorage);
    }

    public ExistEmbeddedServer(final String instanceName, final Path configFile) {
        this(instanceName, configFile, null, false, false);
    }

    public ExistEmbeddedServer(final String instanceName, final Path configFile, final Properties configProperties) {
        this(instanceName, configFile, configProperties, false, false);
    }

    public ExistEmbeddedServer(final String instanceName, final Path configFile, final Properties configProperties, final boolean disableAutoDeploy) {
        this(instanceName, configFile, configProperties, false, false);
    }

    public ExistEmbeddedServer(@Nullable final String instanceName, @Nullable final Path configFile, @Nullable final Properties configProperties, final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        this.instanceName = instanceName != null ? instanceName : BrokerPool.DEFAULT_INSTANCE_NAME;
        this.home = Paths.get(System.getProperty("elemental.home", System.getProperty("exist.home", System.getProperty("user.dir"))));
        this.configFile = configFile != null ? configFile : ConfigurationHelper.lookup("conf.xml", Optional.of(home));
        this.configProperties = configProperties != null ? configProperties : new Properties();
        this.disableAutoDeploy = disableAutoDeploy;
        this.useTemporaryStorage = useTemporaryStorage;

        // setup classloader
        final Classpath _classpath = new Classpath();
        final EXistClassLoader cl = _classpath.getClassLoader(null);
        Thread.currentThread().setContextClassLoader(cl);
    }

    @Override
    protected void before() throws Throwable {
        startDb();
        super.before();
    }

    public void startDb() throws DatabaseConfigurationException, EXistException, IOException {
        if(pool == null) {

            final Configuration config;
            if(configFile.isAbsolute() && Files.exists(configFile)) {
                config = new Configuration(configFile.toAbsolutePath().toString());
            } else {
                config = new Configuration(FileUtils.fileName(configFile), Optional.of(home));
            }

            final boolean propUseTemporaryStorage = Boolean.parseBoolean(System.getProperty(USE_TEMPORARY_STORAGE_PROPERTY, "false"));
            if (useTemporaryStorage || propUseTemporaryStorage) {
                if (temporaryStorage == null) {
                    this.temporaryStorage = Files.createTempDirectory("org.exist.test.ExistEmbeddedServer");
                }
                configProperties.put(BrokerPool.PROPERTY_DATA_DIR, temporaryStorage);
                configProperties.put(Journal.PROPERTY_RECOVERY_JOURNAL_DIR, temporaryStorage);
                LOG.info("Using temporary storage location: {}", temporaryStorage.toAbsolutePath().toString());
            }

            // override any specified config properties
            for (final Map.Entry<Object, Object> configProperty : configProperties.entrySet()) {
                config.setProperty(configProperty.getKey().toString(), configProperty.getValue());
            }

            if (disableAutoDeploy) {
                // remove auto deploy from config if present
                final List<Configuration.StartupTriggerConfig> configuredStartupTriggers = (List<Configuration.StartupTriggerConfig>) config.getProperty(BrokerPoolConstants.PROPERTY_STARTUP_TRIGGERS);
                for (final Configuration.StartupTriggerConfig configuredStartupTrigger : configuredStartupTriggers) {
                    if (AutoDeploymentTrigger.class.getName().equals(configuredStartupTrigger.clazz())) {
                        configuredStartupTriggers.remove(configuredStartupTrigger);
                        break;
                    }
                }
            }

            BrokerPool.configure(instanceName, 1, 5, config, Optional.empty());
            this.pool = BrokerPool.getInstance(instanceName);
        } else {
            throw new IllegalStateException("ExistEmbeddedServer already running");
        }
    }

    public BrokerPool getBrokerPool() {
        return pool;
    }

    public @Nullable Path getTemporaryStorage() {
        return temporaryStorage;
    }

    public void restart() throws EXistException, DatabaseConfigurationException, IOException {
        restart(false);
    }

    public void restart(final boolean clearTemporaryStorage) throws EXistException, DatabaseConfigurationException, IOException {
        if(pool != null) {
            stopDb(clearTemporaryStorage);
            startDb();
        } else {
            throw new IllegalStateException("ExistEmbeddedServer already stopped");
        }
    }

    @Override
    protected void after() {
        stopDb();

        super.after();
    }

    public void stopDb() {
        stopDb(true);
    }

    public void stopDb(final boolean clearTemporaryStorage) {
        if(pool != null) {
            pool.shutdown();

            // clear instance variables
            pool = null;

            final boolean propUseTemporaryStorage = Boolean.parseBoolean(System.getProperty(USE_TEMPORARY_STORAGE_PROPERTY, "false"));
            if((useTemporaryStorage || propUseTemporaryStorage) && temporaryStorage != null && clearTemporaryStorage) {
                FileUtils.deleteQuietly(temporaryStorage);
                temporaryStorage = null;
            }

        } else {
            throw new IllegalStateException("ExistEmbeddedServer already stopped");
        }
    }
}
