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
 */
package org.exist.xquery.functions.securitymanager;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.security.AXSchemaType;
import org.exist.security.PermissionDeniedException;
import org.exist.security.SecurityManager;
import org.exist.security.internal.RealmImpl;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.xquery.functions.securitymanager.SecurityManagerTestUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AccountMetadataFunctionsTest {

    private static final String USER1_NAME = "User 1";
    private static final String USER1_UID = "user1";
    private static final String USER1_PWD = USER1_UID;

    @RegisterExtension
    public final EmbeddedDatabaseExtension embeddedDatabase = new EmbeddedDatabaseExtension(true, true);

    /**
     * Creates a new user programmatically and tries to retrieve their name
     * from the user's metadata.
     *
     * See: <a href="https://github.com/eXist-db/exist/issues/5904">[BUG] Security Account Metadata is lost</a>
     */
    @Test
    void getAccountNameMetadataViaObject() throws PermissionDeniedException, EXistException, XPathException, IOException {
        final BrokerPool pool = embeddedDatabase.getBrokerPool();
        final SecurityManager sm = pool.getSecurityManager();

        // 1. Create user
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {
            createUser(broker, sm, USER1_UID, USER1_PWD, Arrays.asList(Tuple(AXSchemaType.FULLNAME, USER1_NAME)));
            transaction.commit();
        }

        // 2. Try and retrieve the user's metadata from XQuery
        final String query =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "sm:get-account-metadata('" + USER1_UID + "', xs:anyURI('http://axschema.org/namePerson'))";

        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
            final Sequence result = queryResult.result;
            assertNotNull(result);
            assertEquals(USER1_NAME, result.itemAt(0).getStringValue());
        }
    }

    /**
     * Creates a new user by storing their account document into the security collection
     * and tries to retrieve their name from the user's metadata.
     *
     * See: <a href="https://github.com/eXist-db/exist/issues/5904">[BUG] Security Account Metadata is lost</a>
     */
    @Test
    public void getAccountNameMetadataViaDocument() throws PermissionDeniedException, EXistException, XPathException, LockException, IOException, SAXException {
        final BrokerPool pool = existWebServer.getBrokerPool();
        final SecurityManager sm = pool.getSecurityManager();

        final XmldbURI accountDocumentUri = XmldbURI.create(USER1_UID + ".xml");
        final String accountDocument =
                "<account xmlns=\"http://exist-db.org/Configuration\" id=\"15\">\n" +
                "    <group name=\"nogroup\"/>\n" +
                "    <password>{RIPEMD160}q2VXP75jMi+d8E5VAsEr6pD8V5w=</password>\n" +
                "    <expired>false</expired>\n" +
                "    <enabled>true</enabled>\n" +
                "    <umask>022</umask>\n" +
                "    <metadata key=\"http://axschema.org/namePerson\">" + USER1_NAME + "</metadata>\n" +
                "    <name>" + USER1_UID + "</name>\n" +
                "</account>";

        // 1. Create user
        try (final DBBroker broker = pool.get(Optional.of(sm.getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            // /db/system/security/exist/accounts

            final XmldbURI accountsCollectionUri = SecurityManager.SECURITY_COLLECTION_URI.append(RealmImpl.ID).append(SecurityManager.ACCOUNTS_COLLECTION_URI);

            final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);

            try (final Collection accountsCollection = broker.getCollection(accountsCollectionUri)) {
                accountsCollection.storeDocument(transaction, broker, accountDocumentUri, new StringInputSource(accountDocument), xmlMediaType);
            }
            transaction.commit();
        }

        // 2. Try and retrieve the user's metadata from XQuery
        final String query =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "sm:get-account-metadata('" + USER1_UID + "', xs:anyURI('http://axschema.org/namePerson'))";

        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {
            final Sequence result = queryResult.result;
            assertNotNull(result);
            assertEquals(USER1_NAME, result.itemAt(0).getStringValue());
        }
    }
}
