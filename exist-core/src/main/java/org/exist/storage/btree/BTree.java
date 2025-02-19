/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.storage.btree;

import org.exist.storage.BrokerPool;
import org.exist.storage.DefaultCacheManager;

import java.nio.file.Path;

/**
 * Concrete class for basic BTree.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class BTree extends AbstractBTree<BTreeFileHeader, BTreePageHeader> {

    public BTree(final BrokerPool pool, final byte fileId, final short fileVersion, final boolean recoveryEnabled, final DefaultCacheManager cacheManager) throws DBException {
        super(pool, fileId, fileVersion, recoveryEnabled, cacheManager);
    }

    public BTree(final BrokerPool pool, final byte fileId, final short fileVersion, final boolean recoveryEnabled, final DefaultCacheManager cacheManager, final Path file) throws DBException {
        super(pool, fileId, fileVersion, recoveryEnabled, cacheManager, file);
    }

    @Override
    public BTreeFileHeader createFileHeader(final int pageSize) {
        return new BTreeFileHeader(fileVersion, pageSize);
    }

    @Override
    public BTreePageHeader createPageHeader() {
        return new BTreePageHeader();
    }
}
