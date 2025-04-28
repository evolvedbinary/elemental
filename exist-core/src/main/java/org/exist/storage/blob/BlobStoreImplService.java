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
package org.exist.storage.blob;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.storage.BrokerPool;
import org.exist.storage.BrokerPoolService;
import org.exist.storage.BrokerPoolServiceException;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.util.Configuration;
import org.exist.util.crypto.digest.DigestType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Broker Pool Service for the de-duplicating
 * Blob Store, see {@link BlobStoreImpl}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class BlobStoreImplService implements BlobStoreService, BrokerPoolService {

    private static final Logger LOG = LogManager.getLogger(BlobStoreImplService.class);

    private static final String BLOB_STORE_PERSISTENT_FILE_NAME = "blob.dbx";
    private static final String BLOB_STORE_DIR_NAME = "blob";

    private Path persistentFile;
    private Path dataDir;
    private Path blobDir;
    private BlobStore blobStore;

    @Override
    public void configure(final Configuration configuration) throws BrokerPoolServiceException {
        this.dataDir = (Path)configuration.getProperty(BrokerPool.PROPERTY_DATA_DIR);
        if (dataDir == null) {
            throw new BrokerPoolServiceException("Could not determine " + BrokerPool.PROPERTY_DATA_DIR + " from the configuration");
        }

        this.persistentFile = dataDir.resolve(BLOB_STORE_PERSISTENT_FILE_NAME);
        this.blobDir = dataDir.resolve(BLOB_STORE_DIR_NAME);
    }

    @Override
    public void prepare(final BrokerPool pool) {
        this.blobStore = new BlobStoreImpl(pool, persistentFile, blobDir, DigestType.BLAKE_256);
    }

    @Override
    public void startSystem(final DBBroker systemBroker, final Txn transaction) throws BrokerPoolServiceException {
        try {
            this.blobStore.open();
            LOG.info("Opened de-duplicating Blob Store v" + BlobStoreImpl.BLOB_STORE_VERSION + ". metadata={}, store={}/", dataDir.relativize(persistentFile), dataDir.relativize(blobDir));
        } catch (final IOException e) {
            throw new BrokerPoolServiceException(e);
        }
    }

    @Override
    public void shutdown() {
        if (this.blobStore != null) {
            try {
                this.blobStore.close();
            } catch (final IOException e) {
                LOG.error("Clean shutdown of Blob Store failed: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    @Nullable public BlobStore getBlobStore() {
        return blobStore;
    }
}
