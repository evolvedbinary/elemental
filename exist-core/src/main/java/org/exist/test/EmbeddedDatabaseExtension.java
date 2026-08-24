package org.exist.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.storage.BrokerPool;
import org.exist.storage.journal.Journal;
import org.exist.util.Configuration;
import org.exist.util.ConfigurationHelper;
import org.exist.util.DatabaseConfigurationException;
import org.exist.util.FileUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.exist.repo.AutoDeploymentTrigger.AUTODEPLOY_PROPERTY;

public class EmbeddedDatabaseExtension extends AbstractExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final Logger LOG = LogManager.getLogger(EmbeddedDatabaseExtension.class);

    private static final ExtensionContext.Namespace EXTENSION_CONTEXT_NAMESPACE = ExtensionContext.Namespace.create(EmbeddedDatabaseExtension.class);

    private static final String STATE_KEY_BROKER_POOL = "brokerPool";
    private static final String STATE_KEY_PREV_AUTO_DEPLOY = "prevAutoDeploy";
    private static final String STATE_KEY_TEMPORARY_STORAGE = "temporaryStorage";

    @Nullable private final String instanceName;
    @Nullable private final Path configFile;
    @Nullable private final Properties configProperties;
    private final boolean useTemporaryStorage;
    private final boolean disableAutoDeploy;

    public EmbeddedDatabaseExtension() {
        this(null, null, null, false, false);
    }

    public EmbeddedDatabaseExtension(final boolean useTemporaryStorage) {
        this(null, null, null, false, useTemporaryStorage);
    }

    public EmbeddedDatabaseExtension(final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        this(null, null, null, disableAutoDeploy, useTemporaryStorage);
    }

    public EmbeddedDatabaseExtension(final Properties configProperties) {
        this(null, null, configProperties, false, false);
    }

    public EmbeddedDatabaseExtension(final Properties configProperties, final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        this(null, null, configProperties, disableAutoDeploy, useTemporaryStorage);
    }

    public EmbeddedDatabaseExtension(final String instanceName, final Path configFile) {
        this(instanceName, configFile, null, false, false);
    }

    public EmbeddedDatabaseExtension(final String instanceName, final Path configFile, final Properties configProperties) {
        this(instanceName, configFile, configProperties, false, false);
    }

    public EmbeddedDatabaseExtension(final String instanceName, final Path configFile, final Properties configProperties, final boolean disableAutoDeploy) {
        this(instanceName, configFile, configProperties, false, false);
    }

    public EmbeddedDatabaseExtension(@Nullable final String instanceName, @Nullable final Path configFile, @Nullable final Properties configProperties, @Nullable final boolean disableAutoDeploy, @Nullable final boolean useTemporaryStorage) {
        this.instanceName = instanceName;
        this.configFile = configFile;
        this.configProperties = configProperties;
        this.disableAutoDeploy = disableAutoDeploy;
        this.useTemporaryStorage = useTemporaryStorage;

        // TODO(AR) do we still need this? Where do we put it?
        // setup classloader
//        final Classpath _classpath = new Classpath();
//        final EXistClassLoader cl = _classpath.getClassLoader(null);
//        Thread.currentThread().setContextClassLoader(cl);
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        startDatabase(extensionContext);
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) {
        stopDatabase(extensionContext, true);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        startDatabase(extensionContext);
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) {
        stopDatabase(extensionContext, true);
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType() == BrokerPool.class;
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        if (parameterContext.getParameter().getType() == BrokerPool.class) {
            return get(extensionContext, STATE_KEY_BROKER_POOL);
        }
        return null;
    }

    private void startDatabase(final ExtensionContext extensionContext) throws DatabaseConfigurationException, EXistException, IOException {
        // Check if the database is already running...
        @Nullable BrokerPool pool = (BrokerPool) get(extensionContext, STATE_KEY_BROKER_POOL);
        if (pool != null) {
            // Yes, database is already running, therefore so we cannot start it again...
            throw new IllegalStateException("Database is already running");
        }

        // No, so start the database...
        @Nullable String prevAutoDeploy = null;
        if (disableAutoDeploy) {
            prevAutoDeploy = System.setProperty(AUTODEPLOY_PROPERTY, "off");
        }

        final String name = instanceName != null ? instanceName : BrokerPool.DEFAULT_INSTANCE_NAME;

        final Optional<Path> home = Optional.ofNullable(System.getProperty("exist.home", System.getProperty("user.dir"))).map(Paths::get);
        final Path confFile = configFile != null ? configFile : ConfigurationHelper.lookup("conf.xml", home);

        final Configuration config;
        if (confFile.isAbsolute() && Files.exists(confFile)) {
            config = new Configuration(confFile.toAbsolutePath().toString());
        } else {
            config = new Configuration(FileUtils.fileName(confFile), home);
        }

        // override any specified config properties
        if (configProperties != null) {
            for (final Map.Entry<Object, Object> configProperty : configProperties.entrySet()) {
                config.setProperty(configProperty.getKey().toString(), configProperty.getValue());
            }
        }

        @Nullable Path temporaryStorage = (Path) get(extensionContext, STATE_KEY_TEMPORARY_STORAGE);
        if (useTemporaryStorage) {
            if (temporaryStorage == null) {
                temporaryStorage = Files.createTempDirectory(getClass().getName());
            }

            config.setProperty(BrokerPool.PROPERTY_DATA_DIR, temporaryStorage);
            config.setProperty(Journal.PROPERTY_RECOVERY_JOURNAL_DIR, temporaryStorage);
            LOG.info("Using temporary storage location: {}", temporaryStorage.toAbsolutePath().toString());
        }

        BrokerPool.configure(name, 1, 5, config, Optional.empty());
        pool = BrokerPool.getInstance(name);

        // store our state
        put(extensionContext, STATE_KEY_BROKER_POOL, pool);
        put(extensionContext, STATE_KEY_PREV_AUTO_DEPLOY, prevAutoDeploy);
        if (temporaryStorage != null) {
            put(extensionContext, STATE_KEY_TEMPORARY_STORAGE, temporaryStorage);
        }
    }

    private void stopDatabase(final ExtensionContext extensionContext, final boolean clearTemporaryStorage) {
        // Check if the database is already running...
        @Nullable final BrokerPool pool = (BrokerPool) remove(extensionContext, STATE_KEY_BROKER_POOL);
        if (pool == null) {
            // Yes, database is already stopped, therefore we cannot stop it again...
            throw new IllegalStateException("Database is not running");
        }

        // No, so shutdown the database...
        pool.shutdown();

        @Nullable final Path temporaryStorage = (Path) remove(extensionContext, STATE_KEY_TEMPORARY_STORAGE);
        if (useTemporaryStorage && temporaryStorage != null && clearTemporaryStorage) {
            FileUtils.deleteQuietly(temporaryStorage);
        }

        @Nullable final String prevAutoDeploy = (String) remove(extensionContext, STATE_KEY_PREV_AUTO_DEPLOY);
        if (disableAutoDeploy) {
            // Set the autodeploy trigger enablement back to how it was before this test class
            if (prevAutoDeploy != null) {
                System.setProperty(AUTODEPLOY_PROPERTY, prevAutoDeploy);
            } else {
                System.clearProperty(AUTODEPLOY_PROPERTY);
            }
        }
    }

    @Override
    protected ExtensionContext.Store getStore(final ExtensionContext extensionContext) {
        return extensionContext.getStore(EXTENSION_CONTEXT_NAMESPACE);
    }
}
