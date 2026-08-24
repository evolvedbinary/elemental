package org.exist.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.TestUtils;
import org.exist.xmldb.EXistXQueryService;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XQueryService;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

public class XmldbEmbeddedDatabaseExtension extends EmbeddedDatabaseExtension {

    private static final Logger LOG = LogManager.getLogger(XmldbEmbeddedDatabaseExtension.class);

    private static final ExtensionContext.Namespace EXTENSION_CONTEXT_NAMESPACE = ExtensionContext.Namespace.create(XmldbEmbeddedDatabaseExtension.class);

    private static final String STATE_KEY_XMLDB_DATABASE = "xmldbDatabase";
    private static final String STATE_KEY_XMLDB_COLLECTION = "xmldbCollection";
    private static final String STATE_KEY_XMLDB_QUERY_SERVICE = "xmldbQueryService";

    private final boolean asGuest;

    public XmldbEmbeddedDatabaseExtension() {
        this(false, false);
    }

    /**
     * @param asGuest Use the guest account, default is the admin account
     */
    public XmldbEmbeddedDatabaseExtension(final boolean asGuest) {
        this(asGuest, false);
    }

    /**
     * @param asGuest Use the guest account, default is the admin account
     * @param disableAutoDeploy Whether auto-deployment of XARs should be disabled
     */
    public XmldbEmbeddedDatabaseExtension(final boolean asGuest, final boolean disableAutoDeploy) {
        this(asGuest, disableAutoDeploy, false);
    }

    /**
     * @param asGuest Use the guest account, default is the admin account
     * @param disableAutoDeploy Whether auto-deployment of XARs should be disabled
     * @param useTemporaryStorage Whether the data and journal folder should use temporary storage
     */
    public XmldbEmbeddedDatabaseExtension(final boolean asGuest, final boolean disableAutoDeploy, final boolean useTemporaryStorage) {
        super(disableAutoDeploy, useTemporaryStorage);
        this.asGuest = asGuest;
    }

    /**
     * @param asGuest Use the guest account, default is the admin account
     * @param disableAutoDeploy Whether auto-deployment of XARs should be disabled
     * @param useTemporaryStorage Whether the data and journal folder should use temporary storage
     * @param configFile path to conf.xml configuration file
     */
    public XmldbEmbeddedDatabaseExtension(final boolean asGuest, final boolean disableAutoDeploy, final boolean useTemporaryStorage, @Nullable final Path configFile) {
        super(null, configFile, null, disableAutoDeploy, useTemporaryStorage);
        this.asGuest = asGuest;
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        super.beforeAll(extensionContext);
        startXmlDbDriver(extensionContext);
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) {
        stopXmlDbDriver(extensionContext);
        super.afterAll(extensionContext);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        super.beforeAll(extensionContext);
        startXmlDbDriver(extensionContext);
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) {
        stopXmlDbDriver(extensionContext);
        super.afterAll(extensionContext);
    }

    private void startXmlDbDriver(final ExtensionContext extensionContext) throws ClassNotFoundException, IllegalAccessException, InstantiationException, XMLDBException, NoSuchMethodException, InvocationTargetException {
        // Check if the XML:DB driver is already started...
        @Nullable Database database = (Database) get(extensionContext, STATE_KEY_XMLDB_DATABASE);
        if (database != null) {
            // Yes, XML:DB driver is already started, therefore so we cannot start it again...
            throw new IllegalStateException("XML:DB driver is already running");
        }

        // No, so start the XML:DB driver...
        final Class<?> cl = Class.forName("org.exist.xmldb.DatabaseImpl");
        database = (Database) cl.getDeclaredConstructor().newInstance();
        database.setProperty("create-database", "true");
        DatabaseManager.registerDatabase(database);

        final Collection root;
        if (asGuest) {
            root = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, TestUtils.GUEST_DB_USER, TestUtils.GUEST_DB_PWD);
        } else {
            root = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD);
        }

        final XQueryService queryService = root.getService(EXistXQueryService.class);

        // store our state
        put(extensionContext, STATE_KEY_XMLDB_DATABASE, database);
        put(extensionContext, STATE_KEY_XMLDB_COLLECTION, root);
        put(extensionContext, STATE_KEY_XMLDB_QUERY_SERVICE, queryService);
    }

    private void stopXmlDbDriver(final ExtensionContext extensionContext) {
        // Check if the XML:DB driver is already stopped...
        @Nullable final Database database = (Database) remove(extensionContext, STATE_KEY_XMLDB_DATABASE);
        if (database == null) {
            // Yes, XML:DB driver is already stopped, therefore we cannot stop it again...
            throw new IllegalStateException("XML:DB driver is not running");
        }

        @Nullable final Collection root = (Collection) remove(extensionContext, STATE_KEY_XMLDB_COLLECTION);
        if (root != null) {
            try {
                root.close();
            } catch (final XMLDBException e) {
                LOG.warn("Unable to close root collection: {}", e.getMessage(), e);
            }
        }

        remove(extensionContext, STATE_KEY_XMLDB_QUERY_SERVICE);

        DatabaseManager.deregisterDatabase(database);
    }

    @Override
    protected ExtensionContext.Store getStore(final ExtensionContext extensionContext) {
        return extensionContext.getStore(EXTENSION_CONTEXT_NAMESPACE);
    }
}
