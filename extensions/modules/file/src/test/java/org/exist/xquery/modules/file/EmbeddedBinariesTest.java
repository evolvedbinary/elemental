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
package org.exist.xquery.modules.file;

import org.exist.collections.Collection;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.XQueryPool;
import org.exist.storage.lock.Lock;
import org.exist.storage.lock.ManagedCollectionLock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.CompiledXQuery;
import org.exist.xquery.XQuery;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.*;
import org.junit.ClassRule;

import java.io.IOException;
import java.util.Optional;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class EmbeddedBinariesTest extends AbstractBinariesTest<Sequence, Item, IOException> {

    @ClassRule
    public static ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @Override
    protected void storeBinaryFile(final XmldbURI filePath, final byte[] content) throws Exception {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        try(final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
            final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            try(final ManagedCollectionLock collectionLock = brokerPool.getLockManager().acquireCollectionWriteLock(filePath.removeLastSegment())) {
                final Collection collection = broker.getOrCreateCollection(transaction, filePath.removeLastSegment());

                broker.storeDocument(transaction, filePath.lastSegment(), new StringInputSource(content), MimeType.BINARY_TYPE, collection);

                broker.saveCollection(transaction, collection);
            }

            transaction.commit();
        }
    }

    @Override
    protected void removeCollection(final XmldbURI collectionUri) throws Exception {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        try(final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
            final Txn transaction = brokerPool.getTransactionManager().beginTransaction();
            final Collection collection = broker.openCollection(collectionUri, Lock.LockMode.WRITE_LOCK)) {
            if(collection != null) {
                broker.removeCollection(transaction, collection);
            }

            transaction.commit();
        }
    }

    @Override
    protected QueryResultAccessor<Sequence, IOException> executeXQuery(final String query) throws Exception {
        final Source source = new StringSource(query);
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final XQueryPool pool = brokerPool.getXQueryPool();
        final XQuery xquery = brokerPool.getXQueryService();

        try(final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()))) {
            final CompiledXQuery existingCompiled = pool.borrowCompiledXQuery(broker, source);

            final XQueryContext context;
            final CompiledXQuery compiled;
            if (existingCompiled == null) {
                context = new XQueryContext(brokerPool);
                compiled = xquery.compile(context, source);
            } else {
                context = existingCompiled.getContext();
                context.prepareForReuse();
                compiled = existingCompiled;
            }

            final Sequence results = xquery.execute(broker, compiled, null);

            return consumer2E -> {
                try {
//                    context.runCleanupTasks();  //TODO(AR) shows the ordering issue with binary values (see comment below)

                    consumer2E.accept(results);
                } finally {
                    //TODO(AR) performing #runCleanupTasks causes the stream to be closed, so if we do so before we are finished with the results, serialization fails.
                    context.runCleanupTasks();
                    pool.returnCompiledXQuery(source, compiled);
                }
            };
        }
    }

    @Override
    protected long size(final Sequence results) {
        return results.getItemCount();
    }

    @Override
    protected Item item(final Sequence results, final int index) {
        return results.itemAt(index);
    }

    @Override
    protected boolean isBinaryType(final Item item) {
        return Type.BASE64_BINARY == item.getType() || Type.HEX_BINARY == item.getType();
    }

    @Override
    protected boolean isBooleanType(final Item item) throws IOException {
        return Type.BOOLEAN == item.getType();
    }

    @Override
    protected byte[] getBytes(final Item item) throws IOException {
        if (item instanceof Base64BinaryDocument) {
            final Base64BinaryDocument doc = (Base64BinaryDocument) item;
            try (final UnsynchronizedByteArrayOutputStream baos = new UnsynchronizedByteArrayOutputStream()) {
                doc.streamBinaryTo(baos);
                return baos.toByteArray();
            }
        } else {
            final BinaryValueFromFile file = (BinaryValueFromFile) item;
            try (final UnsynchronizedByteArrayOutputStream baos = new UnsynchronizedByteArrayOutputStream()) {
                file.streamBinaryTo(baos);
                return baos.toByteArray();
            }
        }
    }

    @Override
    protected boolean getBoolean(final Item item) throws IOException {
        return ((BooleanValue)item).getValue();
    }
}
