/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.storage.btree;

import org.exist.storage.BrokerPool;
import org.exist.util.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Concrete class for basic BTree.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class BTree extends AbstractBTree<BTreeFileHeader, BTreePageHeader> {

    private BTree(final BrokerPool pool, final byte fileId, final BackingFile backingFile,
            final BTreeFileHeader fileHeader, final boolean enableRecovery) {
        super(pool, fileId, backingFile, fileHeader, enableRecovery, pool.getCacheManager());
    }

    public static BTree open(final BrokerPool pool, final byte fileId, final short fileVersion, final Path path, final boolean enableRecovery) throws DBException {
        BackingFile backingFile = null;
        try {
            backingFile = openAndLockFile(path, true);
            final boolean readOnly = backingFile.fileLock.isShared();
            if (readOnly) {
                LOG.warn("BTree file was opened in read-only mode: {}", FileUtils.fileName(backingFile.path));
            }

            // create a new file header object
            final BTreeFileHeader fileHeader = new BTreeFileHeader(fileVersion, 0, pool.getPageSize());
            if (backingFile.createdNewFile) {
                // write the file header data to the new file
                fileHeader.write(backingFile.randomAccessFile);
            } else {
                // load the file header data from the existing file
                fileHeader.read(backingFile.randomAccessFile);
                fileHeader.checkVersion(fileVersion, FileUtils.fileName(backingFile.path));
                LOG.info("Opened BTree file: {}", FileUtils.fileName(backingFile.path));
            }

            final BTree btree = new BTree(pool, fileId, backingFile, fileHeader, enableRecovery);

            if (backingFile.createdNewFile) {
                // this is a new BTree, so create the root node and persist it
                btree.createRootNode(null);
                fileHeader.setFixedKeyLen((short) -1);
                fileHeader.write(backingFile.randomAccessFile);

                LOG.info("Created BTree file: {}", FileUtils.fileName(backingFile.path));
            }

            return btree;

        } catch (final IOException e) {
            // release the resources we opened before re-throwing exception
            if (backingFile != null) {
                try {
                    backingFile.randomAccessFile.close();
                    backingFile.fileLock.release();
                } catch (final IOException e2) {
                    LOG.error(e2.getMessage(), e2);
                }
            }
            throw new DBException(e);
        }
    }

    @Override
    public BTreePageHeader createPageHeader() {
        return new BTreePageHeader();
    }
}
