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
import java.util.Iterator;
import java.util.Optional;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.BinaryDocument;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.samples.Samples;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.test.TestConstants;
import org.exist.util.*;
import org.exist.util.io.InputStreamUtil;
import org.exist.xmldb.XmldbURI;
import org.junit.After;
import org.junit.Test;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import static org.exist.samples.Samples.SAMPLES;
import static org.junit.Assert.assertNotNull;

public class RecoverBinary2Test {

    // we don't use @ClassRule/@Rule as we want to force corruption in some tests
    private ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, false);

    @Test
    public void storeAndRead() throws SAXException, PermissionDeniedException, DatabaseConfigurationException, IOException, LockException, EXistException, URISyntaxException {
        BrokerPool.FORCE_CORRUPTION = true;
        BrokerPool pool = startDb();
        store(pool);

        stopDb();

        BrokerPool.FORCE_CORRUPTION = false;
        pool = startDb();
        read(pool);

        stopDb();

        BrokerPool.FORCE_CORRUPTION = false;
        pool = startDb();
        read2(pool);
    }

    public void store(final BrokerPool pool) throws EXistException, DatabaseConfigurationException, PermissionDeniedException, IOException, SAXException, LockException, URISyntaxException {
        final TransactionManager transact = pool.getTransactionManager();

        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final Txn transaction = transact.beginTransaction()) {

            final Collection root = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI);
            assertNotNull(root);
            broker.saveCollection(transaction, root);
            
            final Collection test2 = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI2);
            assertNotNull(test2);
            broker.saveCollection(transaction, test2);            
            
            storeFiles(broker, transaction, test2);
            transact.commit(transaction);
        }
    }

    public void read(final BrokerPool pool) throws EXistException, DatabaseConfigurationException, PermissionDeniedException, LockException, IOException, SAXException, URISyntaxException {
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            final Collection test2 = broker.getCollection(TestConstants.TEST_COLLECTION_URI2);
            for (final Iterator<DocumentImpl> i = test2.iterator(broker); i.hasNext(); ) {
                DocumentImpl doc = i.next();
            }
            
            BrokerPool.FORCE_CORRUPTION = true;
            final TransactionManager transact = pool.getTransactionManager();
            assertNotNull(transact);
            try(final Txn transaction = transact.beginTransaction()) {
                assertNotNull(transaction);

                storeFiles(broker, transaction, test2);
                transact.commit(transaction);
            }
        }
    }

    public void read2(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, TriggerException, LockException {
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            final Collection test2 = broker.getCollection(TestConstants.TEST_COLLECTION_URI2);
            for (final Iterator<DocumentImpl> i = test2.iterator(broker); i.hasNext(); ) {
                final DocumentImpl doc = i.next();
            }
            
            final TransactionManager transact = pool.getTransactionManager();
            assertNotNull(transact);
            try(final Txn transaction = transact.beginTransaction()) {
                assertNotNull(transaction);
                final Collection test1 = broker.getCollection(TestConstants.TEST_COLLECTION_URI);
                broker.removeCollection(transaction, test1);
                transact.commit(transaction);
            }
        }
    }
    
    private void storeFiles(final DBBroker broker, final Txn transaction, final Collection test2) throws IOException, EXistException, PermissionDeniedException, LockException, SAXException {
        // store some documents.
        final MediaType binMediaType = broker.getBrokerPool().getMediaTypeService().getMediaTypeResolver().forUnknown();

        for (int j = 0; j < 10; j++) {
            for (final String modsFilename : Samples.SAMPLES.getModsXmlSampleNames()) {
                final XmldbURI uri = test2.getURI().append(j + "_" + modsFilename);
                final byte[] modsContent;
                try (final InputStream is = SAMPLES.getModsSample(modsFilename)) {
                    modsContent = InputStreamUtil.readAll(is);
                }
                broker.storeDocument(transaction, uri, new StringInputSource(modsContent), binMediaType, test2);
                final BinaryDocument doc = (BinaryDocument) test2.getDocument(broker, uri);
                assertNotNull(doc);
            }
        }
    }

    private BrokerPool startDb() throws EXistException, IOException, DatabaseConfigurationException {
        existEmbeddedServer.startDb();
        return existEmbeddedServer.getBrokerPool();
    }

    @After
    public void stopDb() {
        BrokerPool.FORCE_CORRUPTION = false;
        existEmbeddedServer.stopDb();
    }
}
