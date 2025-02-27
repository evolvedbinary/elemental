/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 *
 * NOTE: Parts of this file contain code from The eXist-db Authors.
 *       The original license header is included below.
 *
 * ----------------------------------------------------------------------------
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
package org.exist.storage.index;

import org.exist.collections.Collection;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.storage.BrokerPool;
import org.exist.storage.btree.BackingFile;
import org.exist.storage.btree.DBException;
import org.exist.storage.btree.Value;
import org.exist.util.*;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Handles access to the central collection storage file (collections.dbx). 
 * 
 * @author wolf
 */
public class CollectionStore extends BFile {

    public static final short FILE_FORMAT_VERSION_ID = 16;

    public static final String FILE_NAME = "collections.dbx";
    public static final String  FILE_KEY_IN_CONFIG = "db-connection.collections";
    
    public final static String FREE_DOC_ID_KEY = "__free_doc_id";
    public final static String NEXT_DOC_ID_KEY = "__next_doc_id";  
    public final static String FREE_COLLECTION_ID_KEY = "__free_collection_id";
    public final static String NEXT_COLLECTION_ID_KEY = "__next_collection_id";  

    public final static byte KEY_TYPE_COLLECTION = 0;
    public final static byte KEY_TYPE_DOCUMENT = 1;

    private final Deque<Integer> freeResourceIds = new ConcurrentLinkedDeque<>();
    private final Deque<Integer> freeCollectionIds = new ConcurrentLinkedDeque<>();

    /**
     * @param pool the broker pool
     * @param fileId the id of the collection store
     * @param backingFile the backing file
     * @param fileHeader the file header
     */
    private CollectionStore(final BrokerPool pool, final byte fileId, final BackingFile backingFile, final BFileHeader fileHeader) {
        super(pool, fileId, backingFile, fileHeader, true, 1.25, 0.03);
    }

    public static CollectionStore open(final BrokerPool pool, final byte fileId, final Path path) throws DBException {
        BackingFile backingFile = null;
        try {
            backingFile = openAndLockFile(path, true);
            final boolean readOnly = backingFile.fileLock.isShared();
            if (readOnly) {
                LOG.warn("CollectionStore file was opened in read-only mode: {}", FileUtils.fileName(backingFile.path));
            }

            // create a new file header object
            final BFileHeader fileHeader;
            if (backingFile.createdNewFile) {
                // write the file header data to the new file
                fileHeader = BFileHeader.createNew(FILE_FORMAT_VERSION_ID, pool.getPageSize());
                fileHeader.write(backingFile.randomAccessFile);
            } else {
                // load the file header data from the existing file
                fileHeader = BFileHeader.load(backingFile.randomAccessFile);
                fileHeader.checkVersion(FILE_FORMAT_VERSION_ID, FileUtils.fileName(backingFile.path));
                LOG.info("Opened CollectionStore file: {}", FileUtils.fileName(backingFile.path));
            }

            final CollectionStore collectionStore = new CollectionStore(pool, fileId, backingFile, fileHeader);

            if (backingFile.createdNewFile) {
                // this is a new BTree, so create the root node and persist it
                collectionStore.createRootNode(null);
                fileHeader.write(backingFile.randomAccessFile);
                LOG.info("Created CollectionStore file: {}", FileUtils.fileName(backingFile.path));
            }

            return collectionStore;

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

    public static String getFileName() {
        return FILE_NAME;
    }
    
    public static String getConfigKeyForFile() {
        return FILE_KEY_IN_CONFIG;
    }

    @Override
    protected long getDataSyncPeriod() {
        return 1000;
    }

    @Override
    public boolean flush() throws DBException {
        try {
            boolean flushed = false;
            if (!BrokerPool.FORCE_CORRUPTION) {
                flushed = dataCache.flush();
                flushed = flushed | super.flush();
            }
            return flushed;
        } catch (final IOException e) {
            throw new DBException(e);
        }
    }

    public void freeResourceId(final int id) {
        freeResourceIds.addFirst(id);
    }

    public int getFreeResourceId() {
        Integer freeDocId = freeResourceIds.pollFirst();
        if (freeDocId == null) {
            freeDocId = DocumentImpl.UNKNOWN_DOCUMENT_ID;
        }
        return freeDocId;
    }

    public void freeCollectionId(final int id) {
        freeCollectionIds.addFirst(id);
    }

    public int getFreeCollectionId() {
        Integer freeCollectionId = freeCollectionIds.pollFirst();
        if (freeCollectionId == null) {
            freeCollectionId = Collection.UNKNOWN_COLLECTION_ID;
        }
        return freeCollectionId;
    }

    protected void dumpValue(Writer writer, Value value) throws IOException {
        //TODO : what does this 5 stand for ?
        if (value.getLength() == 5 + Collection.LENGTH_COLLECTION_ID) {
            final short collectionId = ByteConversion.byteToShort(value.data(), value.start());
            //TODO : what does this 1 stand for ?
            final int docId = ByteConversion.byteToInt(value.data(), value.start() + 1 + Collection.LENGTH_COLLECTION_ID);
            writer.write('[');
            writer.write("Document: collection = ");
            writer.write(collectionId);
            writer.write(", docId = ");
            writer.write(docId);
            writer.write(']');
        } else {
            writer.write('[');
            writer.write("Collection: ");
            writer.write(new String(value.data(), value.start(), value.getLength(), StandardCharsets.UTF_8));
            writer.write(']');
        }
    }

    public static class DocumentKey extends Value {
        
        public static final int OFFSET_TYPE = 0;
        public static final int LENGTH_TYPE = 1; //sizeof byte
        public static final int OFFSET_COLLECTION_ID = OFFSET_TYPE + LENGTH_TYPE; //1
        public static final int LENGTH_TYPE_DOCUMENT = 2; //sizeof short
        public static final int OFFSET_DOCUMENT_TYPE = OFFSET_COLLECTION_ID + Collection.LENGTH_COLLECTION_ID; //3
        public static final int LENGTH_DOCUMENT_TYPE = 1; //sizeof byte
        public static final int OFFSET_DOCUMENT_ID = OFFSET_DOCUMENT_TYPE + LENGTH_DOCUMENT_TYPE; //4

        public DocumentKey() {
            data = new byte[LENGTH_TYPE];
            data[OFFSET_TYPE] = KEY_TYPE_DOCUMENT;
            len = LENGTH_TYPE;
        }

        public DocumentKey(int collectionId) {
            data = new byte[LENGTH_TYPE + Collection.LENGTH_COLLECTION_ID];
            data[OFFSET_TYPE] = KEY_TYPE_DOCUMENT;
            ByteConversion.intToByte(collectionId, data, OFFSET_COLLECTION_ID);
            len = LENGTH_TYPE + Collection.LENGTH_COLLECTION_ID;
            pos = OFFSET_TYPE;
        }

        public DocumentKey(int collectionId, byte type, int docId) {
            data = new byte[LENGTH_TYPE + Collection.LENGTH_COLLECTION_ID + LENGTH_DOCUMENT_TYPE + 
                            DocumentImpl.LENGTH_DOCUMENT_ID];
            data[OFFSET_TYPE] = KEY_TYPE_DOCUMENT;
            ByteConversion.intToByte(collectionId, data, OFFSET_COLLECTION_ID);
            data[OFFSET_DOCUMENT_TYPE] = type;
            ByteConversion.intToByte(docId, data, OFFSET_DOCUMENT_ID);
            len = LENGTH_TYPE + Collection.LENGTH_COLLECTION_ID + LENGTH_DOCUMENT_TYPE + 
            	DocumentImpl.LENGTH_DOCUMENT_ID;
            pos = OFFSET_TYPE;
        }

        public static int getCollectionId(Value key) {
            return ByteConversion.byteToInt(key.data(), key.start() + OFFSET_COLLECTION_ID);
        }

        public static int getDocumentId(Value key) {
            return ByteConversion.byteToInt(key.data(), key.start() + OFFSET_DOCUMENT_ID);
        }
    }

    public static class CollectionKey extends Value {
        
        public static final int OFFSET_TYPE = 0;
        public static final int LENGTH_TYPE = 1; //sizeof byte
        public static final int OFFSET_VALUE = OFFSET_TYPE + LENGTH_TYPE; //1

        public CollectionKey() {
            data = new byte[LENGTH_TYPE];
            data[OFFSET_TYPE] = KEY_TYPE_COLLECTION;
            len = LENGTH_TYPE;
        }

        public CollectionKey(String name) {
            len = LENGTH_TYPE + UTF8.encoded(name);
            data = new byte[len];
            data[OFFSET_TYPE] = KEY_TYPE_COLLECTION;
            UTF8.encode(name, data, OFFSET_VALUE);
            pos = OFFSET_TYPE;
        }
    }
}
