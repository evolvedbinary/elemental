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
package org.exist.dom.persistent;

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertFalse;

public class CommentTest {

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @Test
    public void commentContentNotEscaped() throws EXistException, PermissionDeniedException, LockException, SAXException, IOException {
        final XmldbURI docUri = XmldbURI.create("comments.xml");
        final String xml = "<root><!-- text <a> &lt;b&gt;  --></root>";

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final Txn transaction = pool.getTransactionManager().beginTransaction();
             final Collection collection = broker.openCollection(XmldbURI.ROOT_COLLECTION_URI, Lock.LockMode.WRITE_LOCK)) {

            broker.storeDocument(transaction, docUri, new StringInputSource(xml), MimeType.XML_TYPE, collection);

            transaction.commit();
        }


        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final Txn transaction = pool.getTransactionManager().beginTransaction()) {

            try (final LockedDocument lockedDocument = broker.getXMLResource(XmldbURI.ROOT_COLLECTION_URI.append(docUri), Lock.LockMode.READ_LOCK)) {
                final Document doc = lockedDocument.getDocument();

                final Diff diff = DiffBuilder.compare(xml)
                        .withTest(doc)
                        .checkForSimilar()
                        .build();

                assertFalse(diff.toString(), diff.hasDifferences());
            }

            transaction.commit();
        }
    }
}
