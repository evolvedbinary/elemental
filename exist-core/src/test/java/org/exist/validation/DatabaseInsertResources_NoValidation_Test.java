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
package org.exist.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.security.AuthenticationException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.TransactionException;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.util.Configuration;
import org.exist.util.XMLReaderObjectFactory;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.exist.TestUtils.*;
import static org.exist.util.PropertiesBuilder.propertiesBuilder;
import static org.exist.samples.Samples.SAMPLES;

/**
 *  Insert documents for validation tests.
 *
 * @author Dannes Wessels (dizzzz@exist-db.org)
 */
public class DatabaseInsertResources_NoValidation_Test {

    private final static String TEST_COLLECTION = "testNoValidationInsert";

    private final static String VALIDATION_HOME_COLLECTION_URI = "/db/" + TEST_COLLECTION + "/" + TestTools.VALIDATION_HOME_COLLECTION;


    /**
     * Insert all documents into database, switch of validation.
     */
    @Test
    void insertValidationResources_xsd() throws IOException {
        final Configuration config = EMBEDDED_DATABASE.getBrokerPool().getConfiguration();
        config.setProperty(XMLReaderObjectFactory.PROPERTY_VALIDATION_MODE, "no");

        try (final InputStream is = SAMPLES.getSample("validation/addressbook/addressbook.xsd")) {
            TestTools.insertDocumentToURL(is,
                    "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/" + TestTools.VALIDATION_XSD_COLLECTION + "/addressbook.xsd");
        }

        try (final InputStream is = SAMPLES.getSample("validation/addressbook/catalog.xml")) {
            TestTools.insertDocumentToURL(is,
                    "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/" + TestTools.VALIDATION_XSD_COLLECTION + "/catalog.xml");
        }

        try (final InputStream is = SAMPLES.getSample("validation/addressbook/addressbook_valid.xml")) {
            TestTools.insertDocumentToURL(is,
                    "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/addressbook_valid.xml");
        }

        try (final InputStream is = SAMPLES.getSample("validation/addressbook/addressbook_invalid.xml")) {
            TestTools.insertDocumentToURL(is,
                    "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/addressbook_invalid.xml");
        }
    }

    @Test
    void insertValidationResources_dtd() throws IOException {
        final Configuration config = EMBEDDED_DATABASE.getBrokerPool().getConfiguration();
        config.setProperty(XMLReaderObjectFactory.PROPERTY_VALIDATION_MODE, "no");

        try (final InputStream is = SAMPLES.getSample("validation/dtd/hamlet.dtd")) {
            TestTools.insertDocumentToURL(is,
                    "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/" + TestTools.VALIDATION_DTD_COLLECTION + "/hamlet.dtd");
        }

        try (final InputStream is = SAMPLES.getSample("validation/dtd/catalog.xml")) {
            TestTools.insertDocumentToURL(is,
                    "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/" + TestTools.VALIDATION_DTD_COLLECTION + "/catalog.xml");
        }

        try (final InputStream is = SAMPLES.getSample("validation/dtd/hamlet_valid.xml")) {
            TestTools.insertDocumentToURL(is,
                    "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/hamlet_valid.xml");
        }

        try (final InputStream is = SAMPLES.getSample("validation/dtd/hamlet_invalid.xml")) {
            TestTools.insertDocumentToURL(is,
                    "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/hamlet_invalid.xml");
        }
    }

    @Test
    void insertValidationResource_dtd_badDocType() throws IOException {
        final Configuration config = EMBEDDED_DATABASE.getBrokerPool().getConfiguration();
        config.setProperty(XMLReaderObjectFactory.PROPERTY_VALIDATION_MODE, "no");

        try (final InputStream is = SAMPLES.getSample("validation/dtd/hamlet_nodoctype.xml")) {
            TestTools.insertDocumentToURL(is,
                    "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/hamlet_nodoctype.xml");
        }

        try (final InputStream is = SAMPLES.getSample("validation/dtd/hamlet_wrongdoctype.xml")) {
            TestTools.insertDocumentToURL(is,
                    "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/hamlet_wrongdoctype.xml");
        }
    }

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(
            propertiesBuilder()
                .set(XMLReaderObjectFactory.PROPERTY_VALIDATION_MODE, "auto")
                .build(),
            true,
            true);

    @BeforeAll
    static void startup() throws TransactionException, AuthenticationException, PermissionDeniedException, IOException, TriggerException {
        //create the collections we need for these tests
        createTestCollections();
    }

    @AfterAll
    static void shutdown() throws AuthenticationException, PermissionDeniedException, IOException, EXistException, TriggerException {
        removeTestCollections();
    }

    private static void createTestCollections() throws AuthenticationException, PermissionDeniedException, IOException, TriggerException, TransactionException {
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();

        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().authenticate(ADMIN_DB_USER, ADMIN_DB_PWD)));
                final Txn txn = transact.beginTransaction()) {

            /** create necessary collections if they dont exist */
            Collection testCollection = broker.getOrCreateCollection(txn, XmldbURI.create(VALIDATION_HOME_COLLECTION_URI));
            testCollection.getPermissions().setOwner(GUEST_DB_USER);
            broker.saveCollection(txn, testCollection);

            Collection col = broker.getOrCreateCollection(txn, XmldbURI.create(VALIDATION_HOME_COLLECTION_URI + "/" + TestTools.VALIDATION_DTD_COLLECTION));
            col.getPermissions().setOwner(GUEST_DB_USER);
            broker.saveCollection(txn, col);

            col = broker.getOrCreateCollection(txn, XmldbURI.create(VALIDATION_HOME_COLLECTION_URI + "/" + TestTools.VALIDATION_XSD_COLLECTION));
            col.getPermissions().setOwner(GUEST_DB_USER);
            broker.saveCollection(txn, col);

            col = broker.getOrCreateCollection(txn, XmldbURI.create(VALIDATION_HOME_COLLECTION_URI + "/" + TestTools.VALIDATION_TMP_COLLECTION));
            col.getPermissions().setOwner(GUEST_DB_USER);
            broker.saveCollection(txn, col);

            transact.commit(txn);
        }
    }

    private static void removeTestCollections() throws AuthenticationException, PermissionDeniedException, IOException, TriggerException, EXistException {
        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();

        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().authenticate(ADMIN_DB_USER, ADMIN_DB_PWD)));
            final Txn txn = transact.beginTransaction()) {

            /** create necessary collections if they dont exist */
            Collection testCollection = broker.getOrCreateCollection(txn, XmldbURI.create(VALIDATION_HOME_COLLECTION_URI));
            broker.removeCollection(txn, testCollection);

            transact.commit(txn);
        }
    }
}
