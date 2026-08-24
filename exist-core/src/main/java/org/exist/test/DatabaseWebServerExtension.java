package org.exist.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.TestUtils;
import org.exist.jetty.JettyStart;
import org.exist.util.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.exist.repo.AutoDeploymentTrigger.AUTODEPLOY_PROPERTY;
import static org.exist.util.IPUtil.nextFreePort;

public class DatabaseWebServerExtension extends AbstractExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final Logger LOG = LogManager.getLogger(DatabaseWebServerExtension.class);

    private static final ExtensionContext.Namespace EXTENSION_CONTEXT_NAMESPACE = ExtensionContext.Namespace.create(DatabaseWebServerExtension.class);

    private static final String STATE_KEY_JETTY_START = "jettyStart";
    private static final String STATE_KEY_PREV_AUTO_DEPLOY = "prevAutoDeploy";
    private static final String STATE_KEY_TEMPORARY_STORAGE = "temporaryStorage";
    private static final String STATE_KEY_PREV_DATA_DIR = "prevDbConnectionDataDir";
    private static final String STATE_KEY_PREV_JOURNAL_DIR = "prevDbConnectionJournalDir";
    private static final String STATE_KEY_PREV_JETTY_PORT = "prevJettyPort";
    private static final String STATE_KEY_PREV_JETTY_SECURE_PORT = "prevJettySecurePort";
    private static final String STATE_KEY_PREV_JETTY_SSL_PORT = "prevJettySslPort";

    private static final String CONFIG_PROP_FILES = "org.exist.db-connection.files";
    private static final String CONFIG_PROP_JOURNAL_DIR = "org.exist.db-connection.recovery.journal-dir";

    private static final String PROP_JETTY_PORT = "jetty.port";
    private static final String PROP_JETTY_SECURE_PORT = "jetty.secure.port";
    private static final String PROP_JETTY_SSL_PORT = "jetty.ssl.port";

    private static final int MIN_RANDOM_PORT = 49152;
    private static final int MAX_RANDOM_PORT = 65535;
    private static final int MAX_RANDOM_PORT_ATTEMPTS = 10;

    private final boolean useRandomPort;
    private final boolean cleanupDbOnShutdown;
    private final boolean disableAutoDeploy;
    private final boolean useTemporaryStorage;
    private final boolean jettyStandaloneMode;

    public DatabaseWebServerExtension() {
        this(false);
    }

    public DatabaseWebServerExtension(final boolean useRandomPort) {
        this(useRandomPort, false);
    }

    public DatabaseWebServerExtension(final boolean useRandomPort, final boolean cleanupDbOnShutdown) {
        this(useRandomPort, cleanupDbOnShutdown, false);
    }

    public DatabaseWebServerExtension(final boolean useRandomPort, final boolean cleanupDbOnShutdown, final boolean disableAutoDeploy) {
        this(useRandomPort, cleanupDbOnShutdown, disableAutoDeploy, false);
    }

    public DatabaseWebServerExtension(final boolean useRandomPort, final boolean cleanupDbOnShutdown, final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        this(useRandomPort, cleanupDbOnShutdown, disableAutoDeploy, useTemporaryStorage, true);
    }

    public DatabaseWebServerExtension(final boolean useRandomPort, final boolean cleanupDbOnShutdown, final boolean disableAutoDeploy, final boolean useTemporaryStorage, final boolean jettyStandaloneMode) {
        this.useRandomPort = useRandomPort;
        this.cleanupDbOnShutdown = cleanupDbOnShutdown;
        this.disableAutoDeploy = disableAutoDeploy;
        this.useTemporaryStorage = useTemporaryStorage;
        this.jettyStandaloneMode = jettyStandaloneMode;
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws IOException {
        startDatabaseWebServer(extensionContext);
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) {
        stopDatabaseWebServer(extensionContext);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws IOException {
        startDatabaseWebServer(extensionContext);
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) {
        stopDatabaseWebServer(extensionContext);
    }

    private void startDatabaseWebServer(final ExtensionContext extensionContext) throws IOException {
        // Check if the server is already running...
        @Nullable JettyStart jettyStart = (JettyStart) get(extensionContext, STATE_KEY_JETTY_START);
        if (jettyStart != null) {
            // Yes, server is already running, therefore so we cannot start it again...
            throw new IllegalStateException("Database Web Server is already running");
        }

        @Nullable String prevAutoDeploy = null;
        if (disableAutoDeploy) {
            prevAutoDeploy = System.setProperty(AUTODEPLOY_PROPERTY, "off");
        }

        @Nullable String prevDbConnectionDataDir = null;
        @Nullable String prevDbConnectionJournalDir = null;
        @Nullable Path temporaryStorage = (Path) get(extensionContext, STATE_KEY_TEMPORARY_STORAGE);
        if (useTemporaryStorage) {
            if (temporaryStorage == null) {
                temporaryStorage = Files.createTempDirectory(getClass().getName());
            }

            final String absTemporaryStorage = temporaryStorage.toAbsolutePath().toString();
            prevDbConnectionDataDir = System.setProperty(CONFIG_PROP_FILES, absTemporaryStorage);
            prevDbConnectionJournalDir = System.setProperty(CONFIG_PROP_JOURNAL_DIR, absTemporaryStorage);
            LOG.info("Using temporary storage location: {}", absTemporaryStorage);
        }

        @Nullable String prevJettyPort = null;
        @Nullable String prevJettySecurePort = null;
        @Nullable String prevJettySslPort = null;
        if (useRandomPort) {
            synchronized (DatabaseWebServerExtension.class) {
                prevJettyPort = System.setProperty(PROP_JETTY_PORT, Integer.toString(nextFreePort(MIN_RANDOM_PORT, MAX_RANDOM_PORT, MAX_RANDOM_PORT_ATTEMPTS)));
                prevJettySecurePort = System.setProperty(PROP_JETTY_SECURE_PORT, Integer.toString(nextFreePort(MIN_RANDOM_PORT, MAX_RANDOM_PORT, MAX_RANDOM_PORT_ATTEMPTS)));
                prevJettySslPort = System.setProperty(PROP_JETTY_SSL_PORT, Integer.toString(nextFreePort(MIN_RANDOM_PORT, MAX_RANDOM_PORT, MAX_RANDOM_PORT_ATTEMPTS)));

                jettyStart = new JettyStart();
                jettyStart.run(jettyStandaloneMode);
            }
        } else {
            jettyStart = new JettyStart();
            jettyStart.run();
        }

        // store our state
        put(extensionContext, STATE_KEY_JETTY_START, jettyStart);
        put(extensionContext, STATE_KEY_PREV_AUTO_DEPLOY, prevAutoDeploy);
        if (temporaryStorage != null) {
            put(extensionContext, STATE_KEY_TEMPORARY_STORAGE, temporaryStorage);
        }
        put(extensionContext, STATE_KEY_PREV_DATA_DIR, prevDbConnectionDataDir);
        put(extensionContext, STATE_KEY_PREV_JOURNAL_DIR, prevDbConnectionJournalDir);
        put(extensionContext, STATE_KEY_PREV_JETTY_PORT, prevJettyPort);
        put(extensionContext, STATE_KEY_PREV_JETTY_SECURE_PORT, prevJettySecurePort);
        put(extensionContext, STATE_KEY_PREV_JETTY_SSL_PORT, prevJettySslPort);
    }

    private void stopDatabaseWebServer(final ExtensionContext extensionContext) {
        // Check if the server is already running...
        @Nullable final JettyStart jettyStart = (JettyStart) remove(extensionContext, STATE_KEY_JETTY_START);
        if (jettyStart == null) {
            // Yes, server is already stopped, therefore we cannot stop it again...
            throw new IllegalStateException("Database Web Server is not running");
        }

        if (cleanupDbOnShutdown) {
            Assertions.assertDoesNotThrow(TestUtils::cleanupDB);
        }

        // Stop the server
        jettyStart.shutdown();

        @Nullable final Path temporaryStorage = (Path) remove(extensionContext, STATE_KEY_TEMPORARY_STORAGE);
        if (useTemporaryStorage && temporaryStorage != null) {
            FileUtils.deleteQuietly(temporaryStorage);

            @Nullable final String prevDbConnectionDataDir = (String) remove(extensionContext, STATE_KEY_PREV_DATA_DIR);
            if (prevDbConnectionDataDir != null) {
                // Set the data dir back to how it was before this test class
                System.setProperty(CONFIG_PROP_FILES, prevDbConnectionDataDir);
            } else {
                System.clearProperty(CONFIG_PROP_FILES);
            }

            @Nullable final String prevDbConnectionJournalDir = (String) remove(extensionContext, STATE_KEY_PREV_JOURNAL_DIR);
            if (prevDbConnectionJournalDir != null) {
                // Set the journal dir back to how it was before this test class
                System.setProperty(CONFIG_PROP_JOURNAL_DIR, prevDbConnectionJournalDir);
            } else {
                System.clearProperty(CONFIG_PROP_JOURNAL_DIR);
            }
        }

        if (useRandomPort) {
            synchronized (DatabaseWebServerExtension.class) {
                @Nullable final String prevJettyPort = (String) remove(extensionContext, STATE_KEY_PREV_JETTY_PORT);
                if (prevJettyPort != null) {
                    // Set the Jetty port back to how it was before this test class
                    System.setProperty(PROP_JETTY_PORT, prevJettyPort);
                } else {
                    System.clearProperty(PROP_JETTY_PORT);
                }

                @Nullable final String prevJettySecurePort = (String) remove(extensionContext, STATE_KEY_PREV_JETTY_SECURE_PORT);
                if (prevJettySecurePort != null) {
                    // Set the Jetty secure port back to how it was before this test class
                    System.setProperty(PROP_JETTY_SECURE_PORT, prevJettySecurePort);
                } else {
                    System.clearProperty(PROP_JETTY_SECURE_PORT);
                }

                @Nullable final String prevJettySslPort = (String) remove(extensionContext, STATE_KEY_PREV_JETTY_SSL_PORT);
                if (prevJettySslPort != null) {
                    // Set the Jetty SSL port back to how it was before this test class
                    System.setProperty(PROP_JETTY_SSL_PORT, prevJettySslPort);
                } else {
                    System.clearProperty(PROP_JETTY_SSL_PORT);
                }
            }
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
