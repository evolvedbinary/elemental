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
package org.exist.util;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.EXistException;
import org.exist.collections.*;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xmldb.XmldbURI;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests around security exploits of the {@link org.xml.sax.XMLReader}
 */
public class XMLReaderSecurityTest extends AbstractXMLReaderSecurityTest {

    private static final String EXPECTED_EXPANSION_DISABLED_DOC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><foo/>";

    private static final Properties secureConfigProperties = new Properties();
    static {
        final Map<String, Boolean> secureProperties = new HashMap<>();
        secureProperties.put(FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
        secureProperties.put("http://xml.org/sax/features/external-parameter-entities", false);
        secureProperties.put("http://javax.xml.XMLConstants/feature/secure-processing", true);
        secureConfigProperties.put(XMLReaderPool.XmlParser.XML_PARSER_FEATURES_PROPERTY, secureProperties);
    }

    @Rule
    public final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(secureConfigProperties, true, true);

    @Override
    protected ExistEmbeddedServer getExistEmbeddedServer() {
        return existEmbeddedServer;
    }

    @Test
    public void cannotExpandExternalEntitiesWhenDisabled() throws EXistException, IOException, PermissionDeniedException, LockException, SAXException, TransformerException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();

        // create a temporary file on disk that contains secret info
        final Tuple2<String, Path> secret = createTempSecretFile();

        final XmldbURI docName = XmldbURI.create("expand-secret.xml");

        // attempt to store a document with an external entity which would be expanded to the content of the secret file
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final Collection testCollection = broker.openCollection(TEST_COLLECTION, Lock.LockMode.WRITE_LOCK)) {

                //debugReader("cannotExpandExternalEntitiesWhenDisabled", broker, testCollection);

                final String docContent = EXPANSION_DOC.replace(EXTERNAL_FILE_PLACEHOLDER, secret._2.toUri().toString());
                broker.storeDocument(transaction, docName, new StringInputSource(docContent), MimeType.XML_TYPE, testCollection);
            }

            transaction.commit();
        }

        // read back the document, to confirm that it does not contain the secret
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final Collection testCollection = broker.openCollection(TEST_COLLECTION, Lock.LockMode.READ_LOCK)) {

                try (final LockedDocument testDoc = testCollection.getDocumentWithLock(broker, docName, Lock.LockMode.READ_LOCK)) {

                    // release the collection lock early inline with asymmetrical locking
                    testCollection.close();

                    assertNotNull(testDoc);

                    final String expected = EXPECTED_EXPANSION_DISABLED_DOC;
                    final String actual = serialize(testDoc.getDocument());

                    assertEquals(expected, actual);
                }
            }

            transaction.commit();
        }
    }
}
