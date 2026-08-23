/*
 * Elemental
 * Copyright (C) 2025, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 */
package org.exist.test.jupiter;

import org.exist.EXistException;
import org.exist.storage.BrokerPool;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.DatabaseConfigurationException;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.util.Properties;
import java.util.Objects;

/**
 * JUnit Jupiter extension that manages the lifecycle of an embedded eXist-db server.
 *
 * This wraps the existing JUnit 4-based {@link org.exist.test.ExistEmbeddedServer}
 * so tests can migrate from {@code @ClassRule} to {@code @RegisterExtension}
 * incrementally without changing helper calls.
 */
public class ExistEmbeddedServerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private final ExistEmbeddedServer delegate;
    private boolean started = false;
    private boolean startedByClass = false;

    /**
     * Create an extension with default settings.
     */
    public ExistEmbeddedServerExtension() {
        this(false, false);
    }

    /**
     * @param disableAutoDeploy Whether auto-deployment of XARs should be disabled
     * @param useTemporaryStorage Whether data and journal should use temporary storage
     */
    public ExistEmbeddedServerExtension(final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        this.delegate = new ExistEmbeddedServer(disableAutoDeploy, useTemporaryStorage);
    }

    /**
     * Construct with configuration properties and flags.
     * Mirrors {@link org.exist.test.ExistEmbeddedServer#ExistEmbeddedServer(java.util.Properties, boolean, boolean)}.
     *
     * @param configProperties configuration overrides for the embedded server
     * @param disableAutoDeploy whether auto-deployment of XARs should be disabled
     * @param useTemporaryStorage whether data and journal should use temporary storage
     */
    public ExistEmbeddedServerExtension(final Properties configProperties,
                                        final boolean disableAutoDeploy,
                                        final boolean useTemporaryStorage) {
        this.delegate = new ExistEmbeddedServer(configProperties, disableAutoDeploy, useTemporaryStorage);
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        // Class-level lifecycle
        if (!started) {
            delegate.startDb();
            started = true;
            startedByClass = true;
        }
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        // Class-level lifecycle
        if (started && startedByClass) {
            delegate.stopDb();
            started = false;
            startedByClass = false;
        }
    }

    // Convenience methods for tests ---------------------------------------

    public BrokerPool getBrokerPool() {
        return Objects.requireNonNull(delegate.getBrokerPool(), "BrokerPool not available");
    }

    public void restart() throws EXistException, DatabaseConfigurationException, IOException {
        delegate.restart();
    }

    public void restart(final boolean clearTemporaryStorage) throws EXistException, DatabaseConfigurationException, IOException {
        delegate.restart(clearTemporaryStorage);
    }

    // Per-test lifecycle ---------------------------------------------------

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Instance-level lifecycle (non-static @RegisterExtension)
        if (!started) {
            delegate.startDb();
            started = true;
            startedByClass = false;
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (started && !startedByClass) {
            delegate.stopDb();
            started = false;
        }
    }
}
