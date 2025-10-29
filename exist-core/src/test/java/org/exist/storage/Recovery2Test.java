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
package org.exist.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.output.StringBuilderWriter;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.persistent.LockedDocument;
import org.exist.samples.Samples;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.btree.BTreeException;
import org.exist.storage.dom.DOMFile;
import org.exist.storage.lock.Lock.LockMode;
import org.exist.storage.serializers.Serializer;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.test.TestConstants;
import org.exist.util.*;
import org.exist.util.io.InputStreamUtil;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.samples.Samples.SAMPLES;
import static org.junit.Assert.assertNotNull;

/**
 * Test recovery after a forced database corruption.
 * 
 * @author wolf
 *
 */
public class Recovery2Test {

    @Rule
    public ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @Test
    public void storeRead() throws PermissionDeniedException, DatabaseConfigurationException, IOException, LockException, SAXException, EXistException, BTreeException, XPathException, URISyntaxException {

        BrokerPool.FORCE_CORRUPTION = true;
        store(existEmbeddedServer.getBrokerPool());

        // flush journal
        existEmbeddedServer.getBrokerPool().getJournalManager().get().flush(true, false);

        existEmbeddedServer.restart();
        BrokerPool.FORCE_CORRUPTION = false;

        read(existEmbeddedServer.getBrokerPool());
    }

    private void store(final BrokerPool pool) throws DatabaseConfigurationException, EXistException, PermissionDeniedException, IOException, SAXException, BTreeException, LockException, URISyntaxException {
        final TransactionManager transact = pool.getTransactionManager();

        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final Txn transaction = transact.beginTransaction();) {

            Collection root = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI);
            assertNotNull(root);
            broker.saveCollection(transaction, root);

            Collection test2 = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI2);
            assertNotNull(test2);
            broker.saveCollection(transaction, test2);

            DOMFile domDb = ((NativeBroker) broker).getDOMFile();
            assertNotNull(domDb);
            try (final StringBuilderWriter writer = new StringBuilderWriter()) {
                domDb.dump(writer);
            }

            // store some documents. Will be replaced below
            for (final String modsFilename : Samples.SAMPLES.getModsXmlSampleNames()) {
                final String modsContent;
                try (final InputStream is = SAMPLES.getModsSample(modsFilename)) {
                    modsContent = InputStreamUtil.readString(is, UTF_8);
                }
                broker.storeDocument(transaction, XmldbURI.create(modsFilename), new StringInputSource(modsContent), MimeType.XML_TYPE, test2);
            }

            transact.commit(transaction);
        }
    }

    private void read(final BrokerPool pool) throws EXistException, DatabaseConfigurationException, PermissionDeniedException, SAXException, IOException {
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            assertNotNull(broker);
            final Serializer serializer = broker.borrowSerializer();
            
            try(final LockedDocument lockedDoc = broker.getXMLResource(TestConstants.TEST_COLLECTION_URI2.append("0d569a0b-2738-4865-8b47-a9f8b821a653.xml"), LockMode.READ_LOCK)) {
                assertNotNull("Document should not be null", lockedDoc);
                String data = serializer.serialize(lockedDoc.getDocument());
                assertNotNull(data);
            } finally {
                broker.returnSerializer(serializer);
            }
        }
    }

    @AfterClass
    public static void cleanup() {
        // restore the flag in-case of a test failure
        BrokerPool.FORCE_CORRUPTION = false;
    }
}
