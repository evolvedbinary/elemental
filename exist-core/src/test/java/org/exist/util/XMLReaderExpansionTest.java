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
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
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
package org.exist.util;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.collections.Collection;
import org.exist.dom.persistent.LockedDocument;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.xmldb.XmldbURI;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import xyz.elemental.mediatype.MediaType;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class XMLReaderExpansionTest extends AbstractXMLReaderSecurityTest {

    private static final String EXPECTED_EXPANDED_DOC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><foo>" + EXTERNAL_FILE_PLACEHOLDER + "</foo>";

    @RegisterExtension
    public final EmbeddedDatabaseExtension embeddedDatabase = new EmbeddedDatabaseExtension(true, true);

    @Override
    protected EmbeddedDatabaseExtension getEmbeddedDatabaseExtension() {
        return embeddedDatabase;
    }

    @Test
    void expandExternalEntities() throws EXistException, IOException, PermissionDeniedException, LockException, SAXException, TransformerException {
        final BrokerPool brokerPool = embeddedDatabase.getBrokerPool();
        final Map<String, Boolean> parserConfig = new HashMap<>();
        parserConfig.put(FEATURE_EXTERNAL_GENERAL_ENTITIES, true);
        brokerPool.getConfiguration().setProperty(XMLReaderPool.XmlParser.XML_PARSER_FEATURES_PROPERTY, parserConfig);

        // create a temporary file on disk that contains secret info
        final Tuple2<String, Path> secret = createTempSecretFile();

        final XmldbURI docName = XmldbURI.create("expand-secret.xml");

        // attempt to store a document with an external entity which would be expanded to the content of the secret file
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final Collection testCollection = broker.openCollection(TEST_COLLECTION, Lock.LockMode.WRITE_LOCK)) {

                //debugReader("expandExternalEntities", broker, testCollection);

                final String docContent = EXPANSION_DOC.replace(EXTERNAL_FILE_PLACEHOLDER, secret._2.toUri().toString());
                final MediaType xmlMediaType = brokerPool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
                broker.storeDocument(transaction, docName, new StringInputSource(docContent), xmlMediaType, testCollection);
            }

            transaction.commit();
        }

        // read back the document, to confirm that it does contain the secret
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try (final Collection testCollection = broker.openCollection(TEST_COLLECTION, Lock.LockMode.READ_LOCK)) {

                try (final LockedDocument testDoc = testCollection.getDocumentWithLock(broker, docName, Lock.LockMode.READ_LOCK)) {

                    // release the collection lock early inline with asymmetrical locking
                    testCollection.close();

                    assertNotNull(testDoc);
                    final String expected = EXPECTED_EXPANDED_DOC.replace(EXTERNAL_FILE_PLACEHOLDER, secret._1);
                    final String actual = serialize(testDoc.getDocument());

                    assertEquals(expected, actual);
                }
            }

            transaction.commit();
        }
    }
}
