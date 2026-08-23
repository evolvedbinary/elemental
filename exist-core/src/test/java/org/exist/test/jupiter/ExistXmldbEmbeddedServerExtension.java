/*
 * Elemental
 * Copyright (C) 2025, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 */
package org.exist.test.jupiter;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.jupiter.api.extension.*;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;

/**
 * JUnit Jupiter extension that manages the lifecycle of an XML:DB-enabled embedded eXist-db server.
 *
 * This wraps the existing JUnit 4-based {@link org.exist.test.ExistXmldbEmbeddedServer}
 * so tests can migrate from {@code @ClassRule} to {@code @RegisterExtension}
 * incrementally without changing helper calls.
 *
 * Usage:
 * - Class-level (Jupiter {@code @BeforeAll/@AfterAll}) — declare the field {@code static}.
 * - Per-test (Jupiter {@code @BeforeEach/@AfterEach}) — declare the field as an instance field.
 */
public class ExistXmldbEmbeddedServerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private final ExistXmldbEmbeddedServer delegate;
    private boolean started = false;
    private boolean startedByClass = false;

    public ExistXmldbEmbeddedServerExtension() {
        this(false, false, false);
    }

    /**
     * @param asGuest use the guest account (default is admin)
     * @param disableAutoDeploy disable auto-deployment of XARs
     * @param useTemporaryStorage use temp dirs for data and journal
     */
    public ExistXmldbEmbeddedServerExtension(final boolean asGuest, final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        this.delegate = new ExistXmldbEmbeddedServer(asGuest, disableAutoDeploy, useTemporaryStorage);
    }

    /**
     * Overload that allows specifying a custom conf.xml file.
     */
    public ExistXmldbEmbeddedServerExtension(final boolean asGuest, final boolean disableAutoDeploy, final boolean useTemporaryStorage, @Nullable final Path configFile) {
        this.delegate = new ExistXmldbEmbeddedServer(asGuest, disableAutoDeploy, useTemporaryStorage, configFile);
    }

    // Lifecycle -----------------------------------------------------------------

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        startIfNeeded(true);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        stopIfStarted(true);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        startIfNeeded(false);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        stopIfStarted(false);
    }

    private void startIfNeeded(final boolean byClass) throws Exception {
        if (!started) {
            invokeProtected(delegate, "before");
            started = true;
            startedByClass = byClass;
        }
    }

    private void stopIfStarted(final boolean byClass) {
        if (started && startedByClass == byClass) {
            try {
                invokeProtected(delegate, "after");
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                started = false;
                startedByClass = false;
            }
        }
    }

    private static void invokeProtected(final Object target, final String method) throws Exception {
        final Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(target);
    }

    // Convenience delegate methods ---------------------------------------------

    public ResourceSet executeQuery(final String query) throws XMLDBException {
        return delegate.executeQuery(query);
    }

    public ResourceSet executeQuery(final String query, final Map<String, Object> externalVariables) throws XMLDBException {
        return delegate.executeQuery(query, externalVariables);
    }

    public String executeOneValue(final String query) throws XMLDBException {
        return delegate.executeOneValue(query);
    }

    public Collection createCollection(final Collection collection, final String collectionName) throws XMLDBException {
        return delegate.createCollection(collection, collectionName);
    }

    public void storeResource(final Collection collection, final String documentName, final byte[] content) throws XMLDBException {
        delegate.storeResource(collection, documentName, content);
    }

    public String getXMLResource(final Collection collection, final String resource) throws XMLDBException {
        return delegate.getXMLResource(collection, resource);
    }

    public Collection getRoot() {
        return delegate.getRoot();
    }

    public void restart() {
        try {
            delegate.restart();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void restart(final boolean clearTemporaryStorage) {
        try {
            delegate.restart(clearTemporaryStorage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
