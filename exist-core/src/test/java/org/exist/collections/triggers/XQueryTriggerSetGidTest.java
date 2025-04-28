/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.collections.triggers;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.PermissionDeniedException;
import org.exist.security.PermissionFactory;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.Sequence;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.collections.CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE_URI;
import static org.exist.collections.CollectionConfigurationManager.CONFIG_COLLECTION_URI;
import static org.exist.security.SecurityManager.*;
import static org.exist.test.Util.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class XQueryTriggerSetGidTest {

    @ClassRule
    public static final ExistEmbeddedServer EXIST_EMBEDDED_SERVER = new ExistEmbeddedServer(true, true);

	private final static XmldbURI TEST_COLLECTION_URI = XmldbURI.create("/db/testXQueryTriggerSetGid");
    private final static XmldbURI TEST_TRIGGER_COLLECTION_URI = TEST_COLLECTION_URI.append("triggered");
    private final static XmldbURI TEST_OUTPUT_COLLECTION_URI = TEST_COLLECTION_URI.append("output");

    private final static XmldbURI TEST_OUTPUT_BEFORE_DOC_URI = TEST_OUTPUT_COLLECTION_URI.append("before.xml");
    private final static XmldbURI TEST_OUTPUT_AFTER_DOC_URI = TEST_OUTPUT_COLLECTION_URI.append("after.xml");

    private final static XmldbURI TRIGGER_MODULE_URI = TEST_COLLECTION_URI.append("XQueryTriggerSetGid.xqm");
    private final static String TRIGGER_MODULE =
            "module namespace trigger = 'http://exist-db.org/xquery/trigger';\n" +
            "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
            "import module namespace xmldb = 'http://exist-db.org/xquery/xmldb';\n" +
            "\n" +
            "declare function trigger:before-create-document($uri as xs:anyURI) {\n" +
            "  xmldb:store('" +  TEST_OUTPUT_COLLECTION_URI + "', '" + TEST_OUTPUT_BEFORE_DOC_URI.lastSegment() + "', sm:id())\n" +
            "};\n" +
            "\n" +
            "declare function trigger:after-create-document($uri as xs:anyURI) {\n" +
            "  xmldb:store('" + TEST_OUTPUT_COLLECTION_URI + "', '" + TEST_OUTPUT_AFTER_DOC_URI.lastSegment() + "', sm:id())\n" +
            "};";

    private final static String TRIGGER_COLLECTION_CONFIG =
    	"<exist:collection xmlns:exist='http://exist-db.org/collection-config/1.0'>" +
	    "  <exist:triggers>" +
		"     <exist:trigger class='org.exist.collections.triggers.XQueryTrigger'>" +
		"	     <exist:parameter " +
		"			name='url' " +
		"			value='" + TRIGGER_MODULE_URI + "' " +
		"        />" +
		"     </exist:trigger>" +
		"  </exist:triggers>" +
        "</exist:collection>";    

    private final static XmldbURI TRIGGERING_DOCUMENT_URI = TEST_TRIGGER_COLLECTION_URI.append("test.xml");
    
    private final static String TRIGGERING_DOCUMENT_CONTENT =
		  "<test/>";

    @BeforeClass
    public static void setup() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final BrokerPool pool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final Txn transaction = pool.getTransactionManager().beginTransaction();
                final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            // store the trigger module
            try (final Collection collection = broker.getOrCreateCollection(transaction, TEST_COLLECTION_URI)) {
                assertNotNull(collection);
                final XmldbURI triggerModuleUri = storeQuery(broker, transaction, new StringInputSource(TRIGGER_MODULE.getBytes(UTF_8)), collection, TRIGGER_MODULE_URI);
                assertEquals(TRIGGER_MODULE_URI, triggerModuleUri);

                try (final LockedDocument lockedDocument = collection.getDocumentWithLock(broker, TRIGGER_MODULE_URI.lastSegment(), Lock.LockMode.WRITE_LOCK)) {
                    assertNotNull(lockedDocument);
                    final DocumentImpl document = lockedDocument.getDocument();

                    // set the trigger module as setGid for DBA_GROUP
                    PermissionFactory.chown(broker, document, Optional.of(DBA_USER), Optional.of(DBA_GROUP));
                    PermissionFactory.chmod_str(broker, document, Optional.of("rw-r-sr-x"), Optional.empty());
                }
            }

            // create the collection we will trigger on
            try (final Collection collection = broker.getOrCreateCollection(transaction, TEST_TRIGGER_COLLECTION_URI)) {
                assertNotNull(collection);

                // allow any user to write to this collection
                PermissionFactory.chmod_str(broker, collection, Optional.of("rwxrwxrwx"), Optional.empty());
            }

            // create the collection for the output of the trigger
            try (final Collection collection = broker.getOrCreateCollection(transaction, TEST_OUTPUT_COLLECTION_URI)) {
                assertNotNull(collection);
            }

            // install the collection.xconf for the collection we will trigger on
            final XmldbURI configCollectionUri = CONFIG_COLLECTION_URI.append(TEST_TRIGGER_COLLECTION_URI);
            try (final Collection collection = broker.getOrCreateCollection(transaction, configCollectionUri)) {
                assertNotNull(collection);
                broker.storeDocument(transaction, configCollectionUri.append(DEFAULT_COLLECTION_CONFIG_FILE_URI), new StringInputSource(TRIGGER_COLLECTION_CONFIG), MimeType.XML_TYPE, collection);
            }

            transaction.commit();
        }
    }

    @Test
    public void triggerSetGid() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException, XPathException {
        final BrokerPool pool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final Txn transaction = pool.getTransactionManager().beginTransaction();
             final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getGuestSubject()))) {  // NOTE: "guest" user

            // store a document into the "triggered" collection as the guest user, should cause the trigger to fire
            try (final Collection collection = broker.getOrCreateCollection(transaction, TEST_TRIGGER_COLLECTION_URI)) {
                assertNotNull(collection);
                broker.storeDocument(transaction, TRIGGERING_DOCUMENT_URI, new StringInputSource(TRIGGERING_DOCUMENT_CONTENT), MimeType.XML_TYPE, collection);
            }

            transaction.commit();
        }

        // trigger should have completed by this stage... so now check the content of the documents produced by the trigger

        // trigger group for "before" phase should be real=guest, effective=guest,dba...
        final String queryBeforeRealGroup =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "doc('" + TEST_OUTPUT_BEFORE_DOC_URI + "')/sm:id/sm:real/sm:groups/sm:group/string(.)";
        final String queryBeforeEffectiveGroup =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "doc('" + TEST_OUTPUT_BEFORE_DOC_URI + "')/sm:id/sm:effective/sm:groups/sm:group/string(.)";

        try (final Txn transaction = pool.getTransactionManager().beginTransaction();
             final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            final String beforeRealGroup = withCompiledQuery(broker, new StringSource(queryBeforeRealGroup), compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertNotNull(result);
                assertEquals(1, result.getItemCount());

                return result.itemAt(0).getStringValue();
            });
            assertEquals(GUEST_GROUP, beforeRealGroup);

            final String beforeEffectiveGroup = withCompiledQuery(broker, new StringSource(queryBeforeEffectiveGroup), compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertNotNull(result);
                assertEquals(2, result.getItemCount());
                return result.itemAt(0).getStringValue() + "," + result.itemAt(1).getStringValue();
            });
            assertEquals(DBA_GROUP + "," + GUEST_GROUP, beforeEffectiveGroup);

            transaction.commit();
        }

        // trigger group for "after" phase should be real=guest, effective=guest,dba...
        final String queryAfterRealGroup =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "doc('" + TEST_OUTPUT_AFTER_DOC_URI + "')/sm:id/sm:real/sm:groups/sm:group/string(.)";
        final String queryAfterEffectiveGroup =
                "import module namespace sm = 'http://exist-db.org/xquery/securitymanager';\n" +
                "doc('" + TEST_OUTPUT_AFTER_DOC_URI + "')/sm:id/sm:effective/sm:groups/sm:group/string(.)";

        try (final Txn transaction = pool.getTransactionManager().beginTransaction();
             final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            final String afterRealGroup = withCompiledQuery(broker, new StringSource(queryAfterRealGroup), compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertNotNull(result);
                assertEquals(1, result.getItemCount());

                return result.itemAt(0).getStringValue();
            });
            assertEquals(GUEST_GROUP, afterRealGroup);

            final String afterEffectiveGroup = withCompiledQuery(broker, new StringSource(queryAfterEffectiveGroup), compiledQuery -> {
                final Sequence result = executeQuery(broker, compiledQuery);
                assertNotNull(result);
                assertEquals(2, result.getItemCount());
                return result.itemAt(0).getStringValue() + "," + result.itemAt(1).getStringValue();
            });
            assertEquals(DBA_GROUP + "," + GUEST_GROUP, afterEffectiveGroup);

            transaction.commit();
        }
    }
}