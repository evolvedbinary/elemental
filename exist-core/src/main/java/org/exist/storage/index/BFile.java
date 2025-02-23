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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.exist.storage.BrokerPool;
import org.exist.storage.BufferStats;
import org.exist.storage.NativeBroker;
import org.exist.storage.StorageAddress;
import org.exist.storage.btree.AbstractBTree;
import org.exist.storage.btree.BackingFile;
import org.exist.storage.btree.BTreeException;
import org.exist.storage.btree.DBException;
import org.exist.storage.btree.IndexQuery;
import org.exist.storage.btree.Page;
import org.exist.storage.btree.PageStatus;
import org.exist.storage.btree.Value;
import org.exist.storage.cache.Cache;
import org.exist.storage.cache.LRUCache;
import org.exist.storage.io.VariableByteInput;
import org.exist.storage.journal.JournalException;
import org.exist.storage.journal.LogEntryTypes;
import org.exist.storage.journal.Loggable;
import org.exist.storage.journal.Lsn;
import org.exist.storage.lock.LockManager;
import org.exist.storage.txn.Txn;
import org.exist.util.*;
import org.exist.util.sanity.SanityCheck;
import org.exist.xquery.TerminatedException;

import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Data store for variable size values.
 * 
 * This class maps keys to values of variable size. Keys are stored in the
 * b+-tree. B+-tree values are pointers to the logical storage address of the
 * value in the data section. The pointer consists of the page number and a
 * logical tuple identifier.
 * 
 * If a value is larger than the internal page size (4K), it is split into
 * overflow pages. Appending data to a overflow page is very fast. Only the
 * first and the last data page are loaded.
 * 
 * Data pages are buffered.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
public class BFile extends AbstractBTree<BFileHeader, BFilePageHeader> {

    protected final static Logger LOGSTATS = LogManager.getLogger( NativeBroker.EXIST_STATISTICS_LOGGER );
    
    public final static long UNKNOWN_ADDRESS = -1;

    public final static long DATA_SYNC_PERIOD = 15000;
    
    // minimum free space a page should have to be
    // considered for reusing
    public final static int PAGE_MIN_FREE = 64;

    /*
     * Byte ids for the records written to the log file.
     */
    public final static byte LOG_CREATE_PAGE = 0x30;
    public final static byte LOG_STORE_VALUE = 0x31;
    public final static byte LOG_REMOVE_VALUE = 0x32;
    public final static byte LOG_REMOVE_PAGE = 0x33;
    public final static byte LOG_OVERFLOW_APPEND = 0x34;
    public final static byte LOG_OVERFLOW_STORE = 0x35;
    public final static byte LOG_OVERFLOW_CREATE = 0x36;
    public final static byte LOG_OVERFLOW_MODIFIED = 0x37;
    public final static byte LOG_OVERFLOW_CREATE_PAGE = 0x38;
    public final static byte LOG_OVERFLOW_REMOVE = 0x39;

    static {
        // register log entry types for this db file
        LogEntryTypes.addEntryType(LOG_CREATE_PAGE, CreatePageLoggable::new);
        LogEntryTypes.addEntryType(LOG_STORE_VALUE, StoreValueLoggable::new);
        LogEntryTypes.addEntryType(LOG_REMOVE_VALUE, RemoveValueLoggable::new);
        LogEntryTypes.addEntryType(LOG_REMOVE_PAGE, RemoveEmptyPageLoggable::new);
        LogEntryTypes.addEntryType(LOG_OVERFLOW_APPEND, OverflowAppendLoggable::new);
        LogEntryTypes.addEntryType(LOG_OVERFLOW_STORE, OverflowStoreLoggable::new);
        LogEntryTypes.addEntryType(LOG_OVERFLOW_CREATE, OverflowCreateLoggable::new);
        LogEntryTypes.addEntryType(LOG_OVERFLOW_MODIFIED, OverflowModifiedLoggable::new);
        LogEntryTypes.addEntryType(LOG_OVERFLOW_CREATE_PAGE, OverflowCreatePageLoggable::new);
        LogEntryTypes.addEntryType(LOG_OVERFLOW_REMOVE, OverflowRemoveLoggable::new);
    }

    protected final LockManager lockManager;
    protected final int minFree;
    protected final Cache<AbstractDataPage> dataCache;
    protected final int maxValueSize;

    protected BFile(final BrokerPool pool, final byte fileId, final BackingFile backingFile, final BFileHeader fileHeader,
            final boolean enableRecovery, final double cacheGrowth, final double thresholdData) {
        super(pool, fileId, backingFile, fileHeader, enableRecovery, pool.getCacheManager());
        this.lockManager = pool.getLockManager();
        this.dataCache = new LRUCache<>(FileUtils.fileName(backingFile.path), 64, cacheGrowth, thresholdData, Cache.CacheType.DATA);
        this.cacheManager.registerCache(dataCache);
        this.minFree = PAGE_MIN_FREE;
        this.maxValueSize = fileHeader.getPageContentSize() / 2;
    }

    public static BFile open(final BrokerPool pool, final byte fileId, final short fileVersion, final Path path, final boolean enableRecovery, final double cacheGrowth, final double thresholdData) throws DBException {
        BackingFile backingFile = null;
        try {
            backingFile = openAndLockFile(path, true);
            final boolean readOnly = backingFile.fileLock.isShared();
            if (readOnly) {
                LOG.warn("BFile file was opened in read-only mode: {}", FileUtils.fileName(backingFile.path));
            }

            // create a new file header object
            final BFileHeader fileHeader = new BFileHeader(fileVersion, pool.getPageSize());
            if (backingFile.createdNewFile) {
                // write the file header data to the new file
                fileHeader.write(backingFile.randomAccessFile);
            } else {
                // load the file header data from the existing file
                fileHeader.read(backingFile.randomAccessFile);
                fileHeader.checkVersion(fileVersion, FileUtils.fileName(backingFile.path));
                LOG.info("Opened BFile file: {}", FileUtils.fileName(backingFile.path));
            }

            final BFile bfile = new BFile(pool, fileId, backingFile, fileHeader, enableRecovery, cacheGrowth, thresholdData);

            if (backingFile.createdNewFile) {
                // this is a new BTree, so create the root node and persist it
                bfile.createRootNode(null);
                fileHeader.setFixedKeyLen((short) -1);
                fileHeader.write(backingFile.randomAccessFile);

                LOG.info("Created BFile file: {}", FileUtils.fileName(backingFile.path));
            }

            return bfile;

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

    /**
     * Returns the Lock object responsible for this BFile.
     * 
     * @return Lock
     */
    @Override
    public String getLockName() {
        return FileUtils.fileName(getFile());
    }

    protected long getDataSyncPeriod() {
        return DATA_SYNC_PERIOD;
    }

    /**
     * Append the given data fragment to the value associated
     * with the key. A new entry is created if the key does not
     * yet exist in the database.
     * 
     * @param key the key
     * @param value the value
     *
     * @return the pointer to the storage address
     *
     * @throws ReadOnlyException if the BFile is read-only
     * @throws IOException if an I/O error occurs whilst writing to the BFile
     */
    public long append(final Value key, final ByteArray value)
            throws ReadOnlyException, IOException {
        return append(null, key, value);
    }

    public long append(final Txn transaction, final Value key, final ByteArray value) throws IOException {
        if (key == null) {
            LOG.debug("key is null");
            return UNKNOWN_ADDRESS;
        }

        final int maxKeySize;
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            maxKeySize = fileHeader.getMaxKeySize();
        } finally {
            fileHeaderReadLock.unlock();
        }

        if (key.getLength() > maxKeySize) {
            //TODO : throw an exception ? -pb
            LOG.warn("Key length exceeds page size! Skipping key ...");
            return UNKNOWN_ADDRESS;
        }

        try {
            // check if key exists already
            long p = findValue(key);
            if (p == KEY_NOT_FOUND) {
                // key does not exist:
                p = storeValue(transaction, value);
                addValue(transaction, key, p);
                return p;
            }
            // key exists: get old data
            final long pnum = StorageAddress.pageFromPointer(p);
            final short tid = StorageAddress.tidFromPointer(p);
            final AbstractDataPage page = getDataPage(pnum);
            if (page instanceof OverflowPage) {
                ((OverflowPage) page).append(transaction, value);
            } else {
                final int valueLen = value.size();
                final byte[] data = page.getData();
                final int offset = page.findValuePosition(tid);
                if (offset < 0) {
                    throw new IOException("tid " + tid + " not found on page " + pnum);
                }
                if (offset + 4 > data.length) {
                    LOG.error("found invalid pointer in file {} for page{} : tid = {}; offset = {}", FileUtils.fileName(getFile()), page.getPageInfo(), tid, offset);
                    return UNKNOWN_ADDRESS;
                }
                final int l = ByteConversion.byteToInt(data, offset);
                //TOUNDERSTAND : unless l can be negative, we should never get there -pb
                if (offset + 4 + l > data.length) {
                    LOG.error("found invalid data record in file {} for page{} : length = {}; required = {}", FileUtils.fileName(getFile()), page.getPageInfo(), data.length, offset + 4 + l);
                    return UNKNOWN_ADDRESS;
                }
                final byte[] newData = new byte[l + valueLen];
                System.arraycopy(data, offset + 4, newData, 0, l);
                value.copyTo(newData, l);
                p = update(transaction, p, page, key, new FixedByteArray(newData, 0, newData.length));
            }
            return p;
        } catch (final BTreeException bte) {
            LOG.warn("btree exception while appending value", bte);
        }
        return UNKNOWN_ADDRESS;
    }

    /**
     * Check, if key is contained in BFile.
     * 
     * @param key key to look for
     * @return true, if key exists
     */
    public boolean containsKey(final Value key) {
        try {
            return findValue(key) != KEY_NOT_FOUND;
        } catch (final BTreeException | IOException e) {
            LOG.warn(e.getMessage());
        }
        return false;
    }

    @Override
    public void close() throws DBException {
        super.close();
        cacheManager.deregisterCache(dataCache);
    }

    private SinglePage createDataPage() {
        try {
            final SinglePage page = new SinglePage();
            dataCache.add(page, 2);
            return page;
        } catch (final IOException ioe) {
            LOG.warn(ioe);
            return null;
        }
    }

    @Override
    public BFilePageHeader createPageHeader() {
        return new BFilePageHeader();
    }

    /**
     * Remove all entries matching the given query.
     *
     * @param transaction the database transaction
     * @param query the removal query
     *
     * @throws IOException if an I/O error occurs whilst writing to the BFile
     * @throws BTreeException if an error occurs with the tree
     */
    public void removeAll(final Txn transaction, final IndexQuery query) throws IOException, BTreeException {
        // first collect the values to remove, then sort them by their page number
        // and remove them.
        try {
            final PointerCollectorCallback cb = new PointerCollectorCallback();
            remove(transaction, query, cb);
            LOG.debug("Found {} items to remove.", cb.count);
            if (cb.count == 0) {
                return;
            }
            Arrays.sort(cb.pointers, 0, cb.count - 1);
            for (int i = 0; i < cb.count; i++) {
                remove(transaction, cb.pointers[i]);
            }
        } catch (final TerminatedException e) {
            // Should never happen during remove
            LOG.error("removeAll() - method has been terminated.", e);
        }
    }

    public List<Value> findEntries(final IndexQuery query) throws IOException,
            BTreeException, TerminatedException {
        final FindCallback cb = new FindCallback(FindCallback.Mode.BOTH);
        query(query, cb);
        return cb.getValues();
    }

    public List<Value> findKeys(final IndexQuery query)
        throws IOException, BTreeException, TerminatedException {
        final FindCallback cb = new FindCallback(FindCallback.Mode.KEYS);
        query(query, cb);
        return cb.getValues();
    }

    public void find(final IndexQuery query, final IndexCallback callback)
            throws IOException, BTreeException, TerminatedException {
        final FindCallback cb = new FindCallback(callback);
        query(query, cb);
    }

    @Override
    public boolean flush() throws DBException {
        try {
            boolean flushed = false;
            //TODO : consider log operation as a flush ?
            if (isRecoveryEnabled() && logManager != null) {
                logManager.flush(true, false);
            }
            flushed = dataCache.flush();
            flushed = flushed | super.flush();
            return flushed;
        } catch (final IOException e) {
            throw new DBException(e.getMessage(), e);
        }
    }

    public BufferStats getDataBufferStats() {
        if (dataCache == null) {
            return null;
        }
        return new BufferStats(dataCache.getBuffers(), dataCache.getUsedBuffers(), 
            dataCache.getHits(), dataCache.getFails());
    }

    @Override
    public void printStatistics() {
        super.printStatistics();
        final NumberFormat nf = NumberFormat.getPercentInstance();
        final StringBuilder buf = new StringBuilder();
        buf.append(FileUtils.fileName(getFile())).append(" DATA ");
        buf.append("Buffers occupation : ");
        if (dataCache.getBuffers() == 0 && dataCache.getUsedBuffers() == 0) {
            buf.append("N/A");
        } else {
            buf.append(nf.format(dataCache.getUsedBuffers()/(float)dataCache.getBuffers()));
        }
        buf.append(" (").append(dataCache.getUsedBuffers()).append(" out of ").append(dataCache.getBuffers()).append(")");
        //buf.append(dataCache.getBuffers()).append(" / ");
        //buf.append(dataCache.getUsedBuffers()).append(" / ");
        buf.append(" Cache efficiency : ");
        if (dataCache.getHits() == 0 && dataCache.getFails() == 0) {
            buf.append("N/A");
        } else {
            buf.append(nf.format(dataCache.getHits()/(float)(dataCache.getHits() + dataCache.getFails())));
        }
        //buf.append(dataCache.getHits()).append(" / ");
        //buf.append(dataCache.getFails());
        LOGSTATS.info(buf.toString());
    }

    /**
     * Get the value data associated with the specified key
     * or null if the key could not be found.
     * 
     * @param key the key
     *
     * @return the value associated with the key, or null if there is no association.
     */
    public Value get(final Value key) {
        try {
            final long p = findValue(key);
            if (p == KEY_NOT_FOUND) {
                return null;
            }
            final long pnum = StorageAddress.pageFromPointer(p);
            final AbstractDataPage page = getDataPage(pnum);
            return get(page, p);
        } catch (final BTreeException e) {
            LOG.error("An exception occurred while trying to retrieve key {}: {}", key, e.getMessage(), e);
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Get the value data for the given key as a variable byte
     * encoded input stream.
     * 
     * @param key the key
     * @return the stream
     * @throws IOException if an I/O error occurs
     */
    public VariableByteInput getAsStream(final Value key) throws IOException {
        try {
            final long p = findValue(key);
            if (p == KEY_NOT_FOUND) {return null;}           
            final long pnum = StorageAddress.pageFromPointer(p);
            final AbstractDataPage page = getDataPage(pnum);
            switch (page.getPageHeader().getStatus()) {
                case MULTI_PAGE:
                    return ((OverflowPage) page).getDataStream(p);
                default:
                    return getAsStream(page, p);
            }
        } catch (final BTreeException e) {
            LOG.error("An exception occurred while trying to retrieve key {}: {}", key, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Get the value located at the specified address as a
     * variable byte encoded input stream.
     * 
     * @param pointer the pointer to the value
     * @return the stream
     * @throws IOException if an I/O error occurs
     */
    public VariableByteInput getAsStream(final long pointer) throws IOException {
        final AbstractDataPage page = getDataPage(StorageAddress.pageFromPointer(pointer));
        switch (page.getPageHeader().getStatus()) {
            case MULTI_PAGE:
                return ((OverflowPage) page).getDataStream(pointer);
            default:
                return getAsStream(page, pointer);
        }
    }

    private VariableByteInput getAsStream(final AbstractDataPage page, final long pointer) throws IOException {
        dataCache.add(page.getFirstPage(), 2);
        final short tid = StorageAddress.tidFromPointer(pointer);
        final int offset = page.findValuePosition(tid);
        if (offset < 0) {
            throw new IOException("no data found at tid " + tid + "; page " + page.getPageNum());
        }
        final byte[] data = page.getData();
        final int l = ByteConversion.byteToInt(data, offset);
        final SimplePageInput input = new SimplePageInput(data, offset + 4, l, pointer);
        return input;
    }

    /**
     * Returns the value located at the specified address.
     * 
     * @param pointer the pointer to the value
     * @return value located at the specified address
     */
    public Value get(final long pointer) {
        try {
            final long pnum = StorageAddress.pageFromPointer(pointer);
            final AbstractDataPage page = getDataPage(pnum);
            return get(page, pointer);
        } catch (final IOException e) {
            LOG.error(e);
        }
        return null;
    }

    /**
     * Retrieve value at logical address pointer from page
     *
     * @param page the data page
     * @param pointer the pointer to the value
     *
     * @return the value or null if there is no value
     *
     * @throws IOException if an I/O error occurs
     */
    protected Value get(final AbstractDataPage page, final long pointer) throws IOException {
        final short tid = StorageAddress.tidFromPointer(pointer);
        final int offset = page.findValuePosition(tid);
        final byte[] data = page.getData();
        if (offset < 0 || offset > data.length) {
            LOG.error("wrong pointer (tid: {}{}) in file {}; offset = {}", tid, page.getPageInfo(), FileUtils.fileName(getFile()), offset);
            return null;
        }
        final int l = ByteConversion.byteToInt(data, offset);
        if (l + 6 > data.length) {
            LOG.error("{} wrong data length in page {}: expected={}; found={}", FileUtils.fileName(getFile()), page.getPageNum(), l + 6, data.length);
            return null;
        }
        dataCache.add(page.getFirstPage());
        final Value v = new Value(data, offset + 4, l);
        v.setAddress(pointer);
        return v;
    }

    private AbstractDataPage getDataPage(final long pos) throws IOException {
    	return getDataPage(pos, true);
    }

    private AbstractDataPage getDataPage(final long pos, final boolean initialize) throws IOException {
        final AbstractDataPage wp = dataCache.get(pos);
        if (wp == null) {
            final Page<BFilePageHeader> page = getPage(pos);
            if (page == null) {
                LOG.debug("page {} not found!", pos);
                return null;
            }
            final byte[] data = page.read(backingFile.randomAccessFile);
            if (page.getPageHeader().getStatus() == PageStatus.MULTI_PAGE) {
                return new OverflowPage(page, data);
            }
            return new SinglePage(page, data, initialize);
        } else if (wp.getPageHeader().getStatus() == PageStatus.MULTI_PAGE) {
            return new OverflowPage(wp);
        } else {
            return wp;
        }
    }

    private SinglePage getSinglePage(final long pos) throws IOException {
        return getSinglePage(pos, false);
    }

    private SinglePage getSinglePage(final long pos, final boolean initialize) throws IOException {
        final SinglePage wp = (SinglePage) dataCache.get(pos);
        if (wp == null) {
            final Page<BFilePageHeader> page = getPage(pos);
            if (page == null) {
                LOG.debug("page {} not found!", pos);
                return null;
            }
            final byte[] data = page.read(backingFile.randomAccessFile);
            return new SinglePage(page, data, initialize);
        }
        return wp;
    }

    public List<Value> getEntries() throws IOException, BTreeException, TerminatedException {
        final IndexQuery query = new IndexQuery(IndexQuery.ANY, "");
        final FindCallback cb = new FindCallback(FindCallback.Mode.BOTH);
        query(query, cb);
        return cb.getValues();
    }

    public List<Value> getKeys() throws IOException, BTreeException, TerminatedException {
        final IndexQuery query = new IndexQuery(IndexQuery.ANY, "");
        final FindCallback cb = new FindCallback(FindCallback.Mode.KEYS);
        query(query, cb);
        return cb.getValues();
    }

    public List<Value> getValues() throws IOException, BTreeException, TerminatedException {
        final IndexQuery query = new IndexQuery(IndexQuery.ANY, "");
        final FindCallback cb = new FindCallback(FindCallback.Mode.VALUES);
        query(query, cb);
        return cb.getValues();
    }

    /**
     * Put data under given key.
     *
     * @param key the key
     * @param data the data (value) to update
     * @param overwrite overwrite if set to true, value will be overwritten if it already exists
     *
     * @return on success the address of the stored value, else UNKNOWN_ADDRESS
     * @throws ReadOnlyException if the BFile is read-only
     */
    public long put(final Value key, final byte[] data, final boolean overwrite) throws ReadOnlyException {
        return put(null, key, data, overwrite);
    }

    public long put(final Txn transaction, final Value key, final byte[] data, final boolean overwrite) {
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        final int workSize;
        try {
            workSize = fileHeader.getPageContentSize();
        } finally {
            fileHeaderReadLock.unlock();
        }
        SanityCheck.THROW_ASSERT(key.getLength() <= workSize, "Key length exceeds page size!");
        final FixedByteArray buf = new FixedByteArray(data, 0, data.length);
        return put(transaction, key, buf, overwrite);
    }

    /**
     * Convenience method for {@link BFile#put(Value, byte[], boolean)}, overwrite is true.
     * 
     * @param key with which the data is updated
     * @param value value to update
     *
     * @return on success the address of the stored value, else UNKNOWN_ADDRESS
     */
    public long put(final Value key, final ByteArray value) {
        return put(key, value, true);
    }

    /**
     * Put a value under given key. The difference of this
     * method and {@link BFile#append(Value, ByteArray)} is,
     * that the value gets updated and not stored.
     * 
     * @param key with which the data is updated
     * @param value value to update
     * @param overwrite if set to true, value will be overwritten if it already exists
     *
     * @return on success the address of the stored value, else UNKNOWN_ADDRESS
     */
    public long put(final Value key, final ByteArray value, final boolean overwrite) {
        return put(null, key, value, overwrite);
    }

    public long put(final Txn transaction, final Value key, final ByteArray value, final boolean overwrite) {
        if (key == null) {
            LOG.debug("key is null");
            return UNKNOWN_ADDRESS;
        }

        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        final int workSize;
        try {
            workSize = fileHeader.getPageContentSize();
        } finally {
            fileHeaderReadLock.unlock();
        }

        if (key.getLength() > workSize) {
            //TODO : exception ? -pb
            LOG.warn("Key length exceeds page size! Skipping key ...");
            return UNKNOWN_ADDRESS;
        }

        try {
            try {
                // check if key exists already
                //TODO : rely on a KEY_NOT_FOUND (or maybe VALUE_NOT_FOUND) result ! -pb
                long p = findValue(key);
                if (p == KEY_NOT_FOUND) {
                    // key does not exist:
                    p = storeValue(transaction, value);
                    addValue(transaction, key, p);
                    return p;
                }

                // if exists, update value
                if (overwrite) {
                    return update(transaction, p, key, value);
                }

                //TODO : throw an exception ? -pb
                return UNKNOWN_ADDRESS;
            //TODO : why catch an exception here ??? It costs too much ! -pb
            } catch (final BTreeException bte) {
                // key does not exist:
                final long p = storeValue(transaction, value);
                addValue(transaction, key, p);
                return p;
            } catch (final IOException ioe) {
                ioe.printStackTrace();
                LOG.warn(ioe);
                return UNKNOWN_ADDRESS;
            }
        } catch (final BTreeException | IOException e) {
            e.printStackTrace();
            LOG.warn(e);
            return UNKNOWN_ADDRESS;
        }
    }

    public void remove(final Value key) {
        remove(null, key);
    }

    public void remove(final Txn transaction, final Value key) {
        try {
            final long p = findValue(key);
            if (p == KEY_NOT_FOUND) {
                return;
            }
            final long pos = StorageAddress.pageFromPointer(p);
            final AbstractDataPage page = getDataPage(pos);
            remove(transaction, page, p);
            removeValue(transaction, key);
        } catch (final BTreeException | IOException e) {
            LOG.error(e);
        }
    }

    public void remove(final Txn transaction, final long p) {
        try {
            final long pos = StorageAddress.pageFromPointer(p);
            final AbstractDataPage page = getDataPage(pos);
            remove(transaction, page, p);
        } catch (final IOException e) {
            LOG.error(e);
        }
    }

    private void remove(final Txn transaction, final AbstractDataPage page, final long p) throws IOException {
        if (page.getPageHeader().getStatus() == PageStatus.MULTI_PAGE) {
            // overflow page: simply delete the whole page
            ((OverflowPage)page).delete(transaction);
            return;
        }
        final short tid = StorageAddress.tidFromPointer(p);
        final int offset = page.findValuePosition(tid);
        final byte[] data = page.getData();
        if (offset > data.length || offset < 0) {
            LOG.error("wrong pointer (tid: {}, {})", tid, page.getPageInfo());
            return;
        }
        final int l = ByteConversion.byteToInt(data, offset);
        if (transaction != null && isRecoveryEnabled()) {
            final Loggable loggable = new RemoveValueLoggable(transaction, fileId, page.getPageNum(), tid, data, offset + 4, l);
            writeToLog(loggable, page);
        }
        final BFilePageHeader ph = page.getPageHeader();
        final int end = offset + 4 + l;
        int len = ph.getDataLength();
        // remove old value
        System.arraycopy(data, end, data, offset - 2, len - end);
        ph.setDirty(true);
        ph.decRecordCount();
        len = len - l - 6;
        ph.setDataLength(len);
        page.setDirty(true);

        // if this page is empty, remove it
        final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
        try {
            if (len == 0) {
                if (transaction != null && isRecoveryEnabled()) {
                    final Loggable loggable = new RemoveEmptyPageLoggable(transaction, fileId, page.getPageNum());
                    writeToLog(loggable, page);
                }
                fileHeader.removeFreeSpace(fileHeader.getFreeSpace(page.getPageNum()));
                dataCache.remove(page);
                page.delete();
            } else {
                page.removeTID(tid, l + 6);
                // adjust free space data
                final int newFree = fileHeader.getPageContentSize() - len;
                if (newFree > minFree) {
                    FreeSpace free = fileHeader.getFreeSpace(page.getPageNum());
                    if (free == null) {
                        free = new FreeSpace(page.getPageNum(), newFree);
                        fileHeader.addFreeSpace(free);
                    } else {
                        free.setFree(newFree);
                    }
                }
                dataCache.add(page, 2);
            }
        } finally {
            fileHeaderWriteLock.unlock();
        }
    }

    private final void saveFreeSpace(final FreeSpace space, final AbstractDataPage page) {
        final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
        try {
            final int free = fileHeader.getPageContentSize() - page.getPageHeader().getDataLength();
            space.setFree(free);
            if (free < minFree) {
                fileHeader.removeFreeSpace(space);
            }
        } finally {
            fileHeaderWriteLock.unlock();
        }
    }

    public long storeValue(final Txn transaction, final ByteArray value) throws IOException {
        final int vlen = value.size();
        // does value fit into a single page?
        if (6 + vlen > maxValueSize) {
            final OverflowPage page = new OverflowPage(transaction);
            final byte[] data = new byte[vlen + 6];
            page.getPageHeader().setDataLength(vlen + 6);
            ByteConversion.shortToByte((short) 1, data, 0);
            ByteConversion.intToByte(vlen, data, 2);
            //System.arraycopy(value, 0, data, 6, vlen);
            value.copyTo(data, 6);
            page.setData(transaction, data);
            page.setDirty(true);
            //dataCache.add(page);
            return StorageAddress.createPointer((int) page.getPageNum(), (short)1);
        }
        AbstractDataPage page = null;
        short tid = -1;
        FreeSpace free = null;

        final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
        try {
            // check for available tid
            while (tid < 0) {
                free = fileHeader.findFreeSpace(vlen + 6);
                if (free == null) {
                    page = createDataPage();
                    if (transaction != null && isRecoveryEnabled()) {
                        final Loggable loggable = new CreatePageLoggable(transaction, fileId, page.getPageNum());
                        writeToLog(loggable, page);
                    }
                    page.setData(new byte[fileHeader.getPageContentSize()]);
                    free = new FreeSpace(page.getPageNum(),
                        fileHeader.getPageContentSize() - page.getPageHeader().getDataLength());
                    fileHeader.addFreeSpace(free);
                } else {
                    page = getDataPage(free.getPage());
                    // check if this is really a data page
                    if (page.getPageHeader().getStatus() != PageStatus.RECORD) {
                        LOG.warn("page {} is not a data page; removing it", page.getPageNum());
                        fileHeader.removeFreeSpace(free);
                        continue;
                    }
                    // check if the information about free space is really correct
                    final int realSpace = fileHeader.getPageContentSize() - page.getPageHeader().getDataLength();
                    if (realSpace < 6 + vlen) {
                        // not correct: adjust and continue
                        LOG.warn("Wrong data length in list of free pages: adjusting to {}", realSpace);
                        free.setFree(realSpace);
                        continue;
                    }
                }
                tid = page.getNextTID();
                if (tid < 0) {
                    LOG.info("removing page {} from free pages", page.getPageNum());
                    fileHeader.removeFreeSpace(free);
                }
            }
        } finally {
            fileHeaderWriteLock.unlock();
        }

        if (transaction != null && isRecoveryEnabled()) {
            final Loggable loggable = new StoreValueLoggable(transaction, fileId, page.getPageNum(), tid, value);
            writeToLog(loggable, page);
        }
        int len = page.getPageHeader().getDataLength();
        final byte[] data = page.getData();
        // save tid
        ByteConversion.shortToByte(tid, data, len);
        len += 2;
        page.setOffset(tid, len);
        // save data length
        ByteConversion.intToByte(vlen, data, len);
        len += 4;
        // save data
        value.copyTo(data, len);
        len += vlen;
        page.getPageHeader().setDataLength(len);
        page.getPageHeader().incRecordCount();
        saveFreeSpace(free, page);
        page.setDirty(true);
        dataCache.add(page);
        // return pointer from pageNum and offset into page
        return StorageAddress.createPointer((int) page.getPageNum(), tid);
    }

    /**
     * Update a key/value pair.
     * 
     * @param key
     *                   Description of the Parameter
     * @param value
     *                   Description of the Parameter
     * @return Description of the Return Value
     */
    public long update(final Value key, final ByteArray value) {
        try {
            final long p = findValue(key);
            if (p == KEY_NOT_FOUND) {return UNKNOWN_ADDRESS;}
            return update(p, key, value);
        } catch (final BTreeException | IOException bte) {
            LOG.debug(bte);
        }
        return UNKNOWN_ADDRESS;
    }

    /**
     * Update the key/value pair found at the logical address p.
     * 
     * @param p
     *                   Description of the Parameter
     * @param key
     *                   Description of the Parameter
     * @param value
     *                   Description of the Parameter
     * @return Description of the Return Value
     */
    public long update(final long p, final Value key, final ByteArray value) {
        return update(null, p, key, value);
    }
    
    public long update(final Txn transaction, final long p, final Value key, final ByteArray value) {
        try {
            return update(transaction, p, getDataPage(StorageAddress.pageFromPointer(p)),
                    key, value);
        } catch (final BTreeException | IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);
            return UNKNOWN_ADDRESS;
        }
    }

    /**
     * Update the key/value pair with logical address p and stored in page.
     *
     * @param transaction the database transaction
     * @param p the pointer address
     * @param page the data page
     * @param key the key
     * @param value the value
     *
     * @return the new pointer
     *
     * @throws BTreeException if an error occurs updating the tree
     * @throws IOException if an I/O error occurs
     */
    protected long update(final Txn transaction, final long p, final AbstractDataPage page, final Value key, final ByteArray value)
            throws BTreeException, IOException {
        if (page.getPageHeader().getStatus() == PageStatus.MULTI_PAGE) {
            final int valueLen = value.size();
            // does value fit into a single page?
            if (valueLen + 6 < maxValueSize) {
                // yes: remove the overflow page
                remove(transaction, page, p);
                final long np = storeValue(transaction, value);
                addValue(transaction, key, np);
                return np;
            }
            // this is an overflow page: simply replace the value
            final byte[] data = new byte[valueLen + 6];
            // save tid
            ByteConversion.shortToByte((short) 1, data, 0);
            // save length
            ByteConversion.intToByte(valueLen, data, 2);
            // save data
            value.copyTo(data, 6);
            ((OverflowPage)page).setData(transaction, data);
            return p;
        }
        remove(transaction, page, p);
        final long np = storeValue(transaction, value);
        addValue(transaction, key, np);
        return np;
    }

//    public void debugFreeList() {
//        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
//        try {
//            fileHeader.debugFreeList();
//        } finally {
//            fileHeaderReadLock.unlock();
//        }
//    }

    /* ---------------------------------------------------------------------------------
     * Methods used by recovery and transaction management
     * --------------------------------------------------------------------------------- */
    
    /**
     * Write loggable to the journal and update the LSN in the page header.
     *
     * @param loggable the log entry
     * @param page the data page
     */
    private void writeToLog(final Loggable loggable, final AbstractDataPage page) {
        if (logManager != null) {
            try {
                logManager.journal(loggable);
                page.getPageHeader().setLsn(loggable.getLsn());
            } catch (final JournalException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    private SinglePage getSinglePageForRedo(final Loggable loggable, final long pos) throws IOException {
        final SinglePage wp = (SinglePage) dataCache.get(pos);
        if (wp == null) {
            final Page<BFilePageHeader> page = getPage(pos);
            final byte[] data = page.read(backingFile.randomAccessFile);
            if (!PageStatus.isRecordType(page.getPageHeader().getStatus())) {
                return null;
            }
            if (loggable != null && isUptodate(page, loggable)) {
                return null;
            }
            return new SinglePage(page, data, true);
        }
        return wp;
    }

    private boolean isUptodate(final Page<BFilePageHeader> page, final Loggable loggable) {
        return page.getPageHeader().getLsn().compareTo(loggable.getLsn()) >= 0;
    }

    private boolean requiresRedo(final Loggable loggable, final AbstractDataPage page) {
        return loggable.getLsn().compareTo(page.getPageHeader().getLsn()) > 0;
    }

    protected void redoStoreValue(final StoreValueLoggable loggable) {
        try {
            final SinglePage page = getSinglePageForRedo(loggable, loggable.page);
            if (page != null && requiresRedo(loggable, page)) {
                storeValueHelper(loggable, loggable.tid, loggable.value, page);
            }
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage());
        }
    }

    protected void undoStoreValue(final StoreValueLoggable loggable) {
        try {
            final SinglePage page = (SinglePage) getDataPage(loggable.page, true);
            removeValueHelper(null, loggable.tid, page);
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void redoCreatePage(final CreatePageLoggable loggable) {
        createPageHelper(loggable, loggable.newPage, false);
    }

    protected void undoCreatePage(final CreatePageLoggable loggable) {
        try {
            final SinglePage page = (SinglePage) getDataPage(loggable.newPage);
            final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
            try {
                fileHeader.removeFreeSpace(fileHeader.getFreeSpace(page.getPageNum()));
            } finally {
                fileHeaderWriteLock.unlock();
            }
            dataCache.remove(page);
            page.delete();
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void redoRemoveValue(final RemoveValueLoggable loggable) {
        try {
            SinglePage wp = (SinglePage) dataCache.get(loggable.page);
            if (wp == null) {
                final Page<BFilePageHeader> page = getPage(loggable.page);
                if (page == null) {
                    LOG.warn("page {} not found!", loggable.page);
                    return;
                }
                final byte[] data = page.read(backingFile.randomAccessFile);
                if ((!PageStatus.isRecordType(page.getPageHeader().getStatus())) || isUptodate(page, loggable)) {
                	// page is obviously deleted later
                	return;
                }
                wp = new SinglePage(page, data, true);
            }
            if (!wp.ph.getLsn().equals(Lsn.LSN_INVALID) && requiresRedo(loggable, wp)) {
                removeValueHelper(loggable, loggable.tid, wp);
            }
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void undoRemoveValue(final RemoveValueLoggable loggable) {
        try {
            final SinglePage page = getSinglePage(loggable.page, true);
            final FixedByteArray data = new FixedByteArray(loggable.oldData);
            storeValueHelper(null, loggable.tid, data, page);
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during undo: {}", e.getMessage(), e);
        }
    }
    
    protected void redoRemovePage(final RemoveEmptyPageLoggable loggable) {
        try {
            SinglePage wp = (SinglePage) dataCache.get(loggable.page);
            if (wp == null) {
                final Page<BFilePageHeader> page = getPage(loggable.page);
                if (page == null) {
                    LOG.warn("page {} not found!", loggable.page);
                    return;
                }
                final byte[] data = page.read(backingFile.randomAccessFile);
                if ((!PageStatus.isRecordType(page.getPageHeader().getStatus())) || isUptodate(page, loggable)) {
                    return;
                }
                wp = new SinglePage(page, data, false);
            }
            if (wp.getPageHeader().getLsn().equals(Lsn.LSN_INVALID) || requiresRedo(loggable, wp)) {
                final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
                try {
                    fileHeader.removeFreeSpace(fileHeader.getFreeSpace(wp.getPageNum()));
                } finally {
                    fileHeaderWriteLock.unlock();
                }
                dataCache.remove(wp);
                wp.delete();
            }
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void undoRemovePage(final RemoveEmptyPageLoggable loggable) {
        createPageHelper(loggable, loggable.page, false);
    }

    protected void redoCreateOverflow(final OverflowCreateLoggable loggable) {
        try {
            AbstractDataPage firstPage = dataCache.get(loggable.pageNum);
            if (firstPage == null) {
                final Page<BFilePageHeader> page = getPage(loggable.pageNum);
                byte[] data = page.read(backingFile.randomAccessFile);
                if (page.getPageHeader().getLsn().equals(Lsn.LSN_INVALID) || requiresRedo(loggable, page)) {
                    dropFreePageList();
                    final BFilePageHeader ph = page.getPageHeader();
                    ph.updateStatus(PageStatus.MULTI_PAGE);
                    ph.setNextInChain(0L);
                    ph.setLastInChain(0L);
                    ph.setDataLength(0);
                    ph.setNextTID((short) 32);
                    final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
                    try {
                        data = new byte[fileHeader.getPageContentSize()];
                    } finally {
                        fileHeaderReadLock.unlock();
                    }
                    firstPage = new SinglePage(page, data, true);
                    firstPage.setDirty(true);
                } else {
                    firstPage = new SinglePage(page, data, false);
                }
            }
            if (!firstPage.getPageHeader().getLsn().equals(Lsn.LSN_INVALID) && requiresRedo(loggable, firstPage)) {
                firstPage.getPageHeader().setLsn(loggable.getLsn());
                firstPage.setDirty(true);
            }
            dataCache.add(firstPage);
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void undoCreateOverflow(final OverflowCreateLoggable loggable) {
        try {
            final SinglePage page = getSinglePage(loggable.pageNum);
            dataCache.remove(page);
            page.delete();
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void redoCreateOverflowPage(final OverflowCreatePageLoggable loggable) {
        createPageHelper(loggable, loggable.newPage, false);
        if (loggable.prevPage != Page.NO_PAGE) {
            try {
                final SinglePage page = getSinglePageForRedo(null, loggable.prevPage);
                SanityCheck.ASSERT(page != null, "Previous page is null");
                page.getPageHeader().setNextInChain(loggable.newPage);
                page.setDirty(true);
                dataCache.add(page);
            } catch (final IOException e) {
                LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
            }
        }
    }

    protected void undoCreateOverflowPage(final OverflowCreatePageLoggable loggable) {
        try {
            SinglePage page = getSinglePage(loggable.newPage);
            dataCache.remove(page);
            page.delete();
            
            if (loggable.prevPage != Page.NO_PAGE) {
                page = getSinglePage(loggable.prevPage);
                SanityCheck.ASSERT(page != null, "Previous page is null");
                page.getPageHeader().setNextInChain(0);
                page.setDirty(true);
                dataCache.add(page);
            }
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void redoAppendOverflow(final OverflowAppendLoggable loggable) {
        try {
            final SinglePage page = getSinglePageForRedo(loggable, loggable.pageNum);
            if (page != null && requiresRedo(loggable, page)) {
                final BFilePageHeader ph = page.getPageHeader();
                loggable.data.copyTo(0, page.getData(), ph.getDataLength(), loggable.chunkSize);
                ph.setDataLength(ph.getDataLength() + loggable.chunkSize);
                ph.setLsn(loggable.getLsn());
                page.setDirty(true);
                dataCache.add(page);
            }
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void undoAppendOverflow(final OverflowAppendLoggable loggable) {
        try {
            final SinglePage page = getSinglePage(loggable.pageNum);
            final BFilePageHeader ph = page.getPageHeader();
            ph.setDataLength(ph.getDataLength() - loggable.chunkSize);
            page.setDirty(true);
            dataCache.add(page);
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void redoStoreOverflow(final OverflowStoreLoggable loggable) {
        try {
            SinglePage page = getSinglePageForRedo(loggable, loggable.pageNum);
            if (page != null && requiresRedo(loggable, page)) {
                final BFilePageHeader ph = page.getPageHeader();
                try {
                    System.arraycopy(loggable.data, 0, page.getData(), 0, loggable.size);
                } catch (final ArrayIndexOutOfBoundsException e) {
                    LOG.warn("{}; {}; {}; {}", loggable.data.length, page.getData().length, ph.getDataLength(), loggable.size);
                    throw e;
                }
                ph.setDataLength(loggable.size);
                ph.setNextInChain(0);
                ph.setLsn(loggable.getLsn());
                page.setDirty(true);
                dataCache.add(page);
                
                if (loggable.prevPage != Page.NO_PAGE) {
                    page = getSinglePage(loggable.prevPage);
                    SanityCheck.ASSERT(page != null, "Previous page is null");
                    page.getPageHeader().setNextInChain(loggable.pageNum);
                    page.setDirty(true);
                    dataCache.add(page);
                }
            }
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void redoModifiedOverflow(final OverflowModifiedLoggable loggable) {
        try {
            final SinglePage page = getSinglePageForRedo(loggable, loggable.pageNum);
            if (page != null && requiresRedo(loggable, page)) {
                final BFilePageHeader ph = page.getPageHeader();
                ph.setDataLength(loggable.length);
                ph.setLastInChain(loggable.lastInChain);
                // adjust length field in first page
                ByteConversion.intToByte(ph.getDataLength() - 6, page.getData(), 2);
                page.setDirty(true);
                // keep the first page in cache
                dataCache.add(page, 2);
            }
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void undoModifiedOverflow(final OverflowModifiedLoggable loggable) {
        try {
            final SinglePage page = getSinglePage(loggable.pageNum);
            final BFilePageHeader ph = page.getPageHeader();
            ph.setDataLength(loggable.oldLength);
            // adjust length field in first page
            ByteConversion.intToByte(ph.getDataLength() - 6, page.getData(), 2);
            page.setDirty(true);
            dataCache.add(page);
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during undo: {}", e.getMessage(), e);
        }
    }

    protected void redoRemoveOverflow(final OverflowRemoveLoggable loggable) {
        try {
            SinglePage wp = (SinglePage) dataCache.get(loggable.getPageNum());
            if (wp == null) {
                final Page<BFilePageHeader> page = getPage(loggable.getPageNum());
                if (page == null) {
                    LOG.warn("page {} not found!", loggable.getPageNum());
                    return;
                }
                final byte[] data = page.read(backingFile.randomAccessFile);
                if ((!PageStatus.isRecordType(page.getPageHeader().getStatus())) || isUptodate(page, loggable)) {
                    return;
                }
                wp = new SinglePage(page, data, true);
            }
            if (requiresRedo(loggable, wp)) {
                wp.setDirty(true);
                dataCache.remove(wp);
                wp.delete();
            }
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
    }

    protected void undoRemoveOverflow(final OverflowRemoveLoggable loggable) {
        final AbstractDataPage page = createPageHelper(loggable, loggable.getPageNum(), false);
        final BFilePageHeader ph = page.getPageHeader();
        ph.updateStatus(loggable.getPageStatus());
        ph.setDataLength(loggable.getLength());
        ph.setNextInChain(loggable.getNextInChain());
        page.setData(loggable.getData());
        page.setDirty(true);
        dataCache.add(page);
    }

    private void storeValueHelper(final Loggable loggable, final short tid, final ByteArray value, final SinglePage page) {
        int len = page.ph.getDataLength();
        // save tid
        ByteConversion.shortToByte(tid, page.data, len);
        len += 2;
        page.adjustTID(tid);
        page.setOffset(tid, len);
        // save data length
        ByteConversion.intToByte(value.size(), page.data, len);
        len += 4;
        // save data
        try {
            value.copyTo(page.data, len);
        } catch (final RuntimeException e) {
            LOG.error("{}: storage error in page: {}; len: {} ; value: {}; max: {}; status: {}", FileUtils.fileName(getFile()), page.getPageNum(), len, value.size(), fileHeader.getPageContentSize(), page.ph.getStatus());
            LOG.debug(page.printContents());
            throw e;
        }
        len += value.size();
        page.ph.setDataLength(len);
        page.ph.incRecordCount();
        if (loggable != null) {
            page.ph.setLsn(loggable.getLsn());
        }
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            FreeSpace free = fileHeader.getFreeSpace(page.getPageNum());
            if (free == null) {
                free = new FreeSpace(page.getPageNum(), fileHeader.getPageContentSize() - len);
            }
            saveFreeSpace(free, page);
        } finally {
            fileHeaderReadLock.unlock();
        }
        page.setDirty(true);
        dataCache.add(page);
    }

    private void removeValueHelper(final Loggable loggable, final short tid, final SinglePage page) throws IOException {
        final int offset = page.findValuePosition(tid);
        if (offset < 0) {
            LOG.warn("TID: {} not found on page: {}", tid, page.getPageNum());
            return;
        }
        final int l = ByteConversion.byteToInt(page.data, offset);
        final int end = offset + 4 + l;
        int len = page.ph.getDataLength();
        // remove old value
        System.arraycopy(page.data, end, page.data, offset - 2, len - end);
        page.ph.setDirty(true);
        page.ph.decRecordCount();
        len = len - l - 6;
        page.ph.setDataLength(len);
        if (loggable != null) {
            page.ph.setLsn(loggable.getLsn());
        }
        page.setDirty(true);
        if (len > 0) {
            page.removeTID(tid, l + 6);
            // adjust free space data
            final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
            try {
                final int newFree = fileHeader.getPageContentSize() - len;
                if (newFree > minFree) {
                    FreeSpace free = fileHeader.getFreeSpace(page.getPageNum());
                    if (free == null) {
                        free = new FreeSpace(page.getPageNum(), newFree);
                        fileHeader.addFreeSpace(free);
                    } else {
                        free.setFree(newFree);
                    }
                }
            } finally {
                fileHeaderWriteLock.unlock();
            }
            dataCache.add(page, 2);
        }
    }

    private AbstractDataPage createPageHelper(final Loggable loggable, final long newPage, final boolean reuseDeleted) {
        try {
            AbstractDataPage dp = dataCache.get(newPage);
            if (dp == null) {
                final Page<BFilePageHeader> page = getPage(newPage);
                byte[] data = page.read(backingFile.randomAccessFile);
                if (page.getPageHeader().getLsn().equals(Lsn.LSN_INVALID) || (loggable != null && requiresRedo(loggable, page)) ) {
                    if (reuseDeleted) {
                        reuseDeleted(page);
                    } else {
                        dropFreePageList();
                    }
                    final BFilePageHeader ph = page.getPageHeader();
                    ph.updateStatus(PageStatus.RECORD);
                    ph.setDataLength(0);
                    final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
                    try {
                        ph.updateDataLen(fileHeader.getPageContentSize());
                        data = new byte[fileHeader.getPageContentSize()];
                    } finally {
                        fileHeaderReadLock.unlock();
                    }
                    ph.setNextTID((short) 32);
                    dp = new SinglePage(page, data, true);
                } else {
                    dp = new SinglePage(page, data, true);
                }
            }
            if (loggable != null && loggable.getLsn().compareTo(dp.getPageHeader().getLsn()) > 0) {
                dp.getPageHeader().setLsn(loggable.getLsn());
            }
            dp.setDirty(true);
            dataCache.add(dp);
            return dp;
        } catch (final IOException e) {
            LOG.warn("An IOException occurred during redo: {}", e.getMessage(), e);
        }
        return null;
    }
}
