/*
 * Elemental
 * Copyright (C) 2025, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 */
package org.exist.test.jupiter;

import org.exist.test.ExistWebServer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;

/**
 * JUnit Jupiter extension that manages the lifecycle of the embedded Jetty web server for eXist-db.
 *
 * Wraps {@link org.exist.test.ExistWebServer} so tests can migrate from {@code @ClassRule}
 * to {@code @RegisterExtension} with minimal changes.
 *
 * Usage:
 * - Class-level lifecycle: declare the field {@code static}.
 * - Per-test lifecycle: declare the field as an instance field.
 */
public class ExistWebServerExtension implements BeforeEachCallback, AfterEachCallback {

    private final ExistWebServer delegate;
    private boolean started = false;

    public ExistWebServerExtension() {
        this(true, false, true, true);
    }

    public ExistWebServerExtension(final boolean useRandomPort, final boolean cleanupDbOnShutdown, final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        this.delegate = new ExistWebServer(useRandomPort, cleanupDbOnShutdown, disableAutoDeploy, useTemporaryStorage);
    }

    public ExistWebServerExtension(final boolean useRandomPort, final boolean cleanupDbOnShutdown, final boolean disableAutoDeploy, final boolean useTemporaryStorage, final boolean jettyStandaloneMode) {
        this.delegate = new ExistWebServer(useRandomPort, cleanupDbOnShutdown, disableAutoDeploy, useTemporaryStorage, jettyStandaloneMode);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        startIfNeeded();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        stopIfStarted();
    }

    private void startIfNeeded() throws Exception {
        if (!started) {
            invokeProtected(delegate, "before");
            // expose port for helpers that read from system properties
            System.setProperty("exist.test.webserver.port", Integer.toString(delegate.getPort()));
            started = true;
        }
    }

    private void stopIfStarted() {
        if (started) {
            try {
                invokeProtected(delegate, "after");
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                System.clearProperty("exist.test.webserver.port");
                started = false;
            }
        }
    }

    private static void invokeProtected(final Object target, final String method) throws Exception {
        final Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(target);
    }

    // Convenience methods --------------------------------------------------

    public int getPort() {
        return delegate.getPort();
    }

    public void restart() {
        delegate.restart();
    }
}
