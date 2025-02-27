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

import org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.exist.storage.BrokerPool;
import org.exist.storage.BufferStats;
import org.exist.storage.NativeBroker;
import org.exist.storage.StorageAddress;
import org.exist.storage.btree.AbstractBTree;
import org.exist.storage.btree.BackingFile;
import org.exist.storage.btree.BTreeCallback;
import org.exist.storage.btree.BTreeException;
import org.exist.storage.btree.DBException;
import org.exist.storage.btree.IndexQuery;
import org.exist.storage.btree.PageType;
import org.exist.storage.btree.Value;
import org.exist.storage.cache.AbstractCacheable;
import org.exist.storage.cache.Cache;
import org.exist.storage.cache.Cacheable;
import org.exist.storage.cache.LRUCache;
import org.exist.storage.io.VariableByteInput;
import org.exist.storage.io.VariableByteOutput;
import org.exist.storage.journal.JournalException;
import org.exist.storage.journal.LogEntryTypes;
import org.exist.storage.journal.Loggable;
import org.exist.storage.journal.Lsn;
import org.exist.storage.lock.LockManager;
import org.exist.storage.lock.ManagedLock;
import org.exist.storage.txn.Txn;
import org.exist.util.*;
import org.exist.util.sanity.SanityCheck;
import org.exist.xquery.Constants;
import org.exist.xquery.TerminatedException;

import javax.annotation.Nullable;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.nio.charset.StandardCharsets.UTF_8;


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
        this.maxValueSize = getPageContentSize() / 2;
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
            final BFileHeader fileHeader;
            if (backingFile.createdNewFile) {
                // write the file header data to the new file
                fileHeader = BFileHeader.createNew(fileVersion, pool.getPageSize());
                fileHeader.write(backingFile.randomAccessFile);
            } else {
                // load the file header data from the existing file
                fileHeader = BFileHeader.load(backingFile.randomAccessFile);
                fileHeader.checkVersion(fileVersion, FileUtils.fileName(backingFile.path));
                LOG.info("Opened BFile file: {}", FileUtils.fileName(backingFile.path));
            }

            final BFile bfile = new BFile(pool, fileId, backingFile, fileHeader, enableRecovery, cacheGrowth, thresholdData);

            if (backingFile.createdNewFile) {
                // this is a new BTree, so create the root node and persist it
                bfile.createRootNode(null);
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

        if (key.getLength() > fileHeader.getMaxKeySize()) {
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
        final FindCallback cb = new FindCallback(FindCallbackMode.BOTH);
        query(query, cb);
        return cb.getValues();
    }

    public List<Value> findKeys(final IndexQuery query)
        throws IOException, BTreeException, TerminatedException {
        final FindCallback cb = new FindCallback(FindCallbackMode.KEYS);
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
            switch (page.getPageHeader().getType()) {
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
        switch (page.getPageHeader().getType()) {
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
        final SinglePageInput input = new SinglePageInput(data, offset + 4, l, pointer);
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
            final Page<BFilePageHeader> page = createPage(pos);
            if (page == null) {
                LOG.debug("page {} not found!", pos);
                return null;
            }
            final byte[] data = page.read(backingFile.randomAccessFile);
            if (page.getPageHeader().getType() == PageType.MULTI_PAGE) {
                return new OverflowPage(page, data);
            }
            return new SinglePage( page, data, initialize);
        } else if (wp.getPageHeader().getType() == PageType.MULTI_PAGE) {
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
            final Page<BFilePageHeader> page = createPage(pos);
            final byte[] data = page.read(backingFile.randomAccessFile);
            return new SinglePage(page, data, initialize);
        }
        return wp;
    }

    public List<Value> getEntries() throws IOException, BTreeException, TerminatedException {
        final IndexQuery query = new IndexQuery(IndexQuery.ANY, "");
        final FindCallback cb = new FindCallback(FindCallbackMode.BOTH);
        query(query, cb);
        return cb.getValues();
    }

    public List<Value> getKeys() throws IOException, BTreeException, TerminatedException {
        final IndexQuery query = new IndexQuery(IndexQuery.ANY, "");
        final FindCallback cb = new FindCallback(FindCallbackMode.KEYS);
        query(query, cb);
        return cb.getValues();
    }

    public List<Value> getValues() throws IOException, BTreeException, TerminatedException {
        final IndexQuery query = new IndexQuery(IndexQuery.ANY, "");
        final FindCallback cb = new FindCallback(FindCallbackMode.VALUES);
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
        SanityCheck.THROW_ASSERT(key.getLength() <= getPageContentSize(), "Key length exceeds page size!");
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

        if (key.getLength() > getPageContentSize()) {
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
        if (page.getPageHeader().getType() == PageType.MULTI_PAGE) {
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
            final int newFree = getPageContentSize() - len;
            if (newFree > minFree) {
                FreeSpace freeSpace = fileHeader.getFreeSpace(page.getPageNum());
                if (freeSpace == null) {
                    freeSpace = new FreeSpace(page.getPageNum(), newFree);
                    fileHeader.addFreeSpace(freeSpace);
                } else {
                    freeSpace.free = newFree;
                }
            }
            dataCache.add(page, 2);
        }
    }

    private final void saveFreeSpace(final FreeSpace freeSpace, final AbstractDataPage page) {
        final int free = getPageContentSize() - page.getPageHeader().getDataLength();
        freeSpace.free = free;
        if (free < minFree) {
            fileHeader.removeFreeSpace(freeSpace);
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
        FreeSpace freeSpace = null;
        // check for available tid
        while (tid < 0) {
            freeSpace = fileHeader.findFreeSpace(vlen + 6);
            if (freeSpace == null) {
                page = createDataPage();
                if (transaction != null && isRecoveryEnabled()) {
                    final Loggable loggable = new CreatePageLoggable(transaction, fileId, page.getPageNum());
                    writeToLog(loggable, page);
                }
                page.setData(new byte[getPageContentSize()]);
                freeSpace = new FreeSpace(page.getPageNum(),
                    getPageContentSize() - page.getPageHeader().getDataLength());
                fileHeader.addFreeSpace(freeSpace);
            } else {
                page = getDataPage(freeSpace.page);
                // check if this is really a data page
                if (page.getPageHeader().getType() != PageType.RECORD) {
                    LOG.warn("page {} is not a data page; removing it", page.getPageNum());
                    fileHeader.removeFreeSpace(freeSpace);
                    continue;
                }
                // check if the information about free space is really correct
                final int realSpace = getPageContentSize() - page.getPageHeader().getDataLength();
                if (realSpace < 6 + vlen) {
                    // not correct: adjust and continue
                    LOG.warn("Wrong data length in list of free pages: adjusting to {}", realSpace);
                    freeSpace.free = realSpace;
                    continue;
                }
            }
            tid = page.getNextTID();
            if (tid < 0) {
                LOG.info("removing page {} from free pages", page.getPageNum());
                fileHeader.removeFreeSpace(freeSpace);
            }
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
        saveFreeSpace(freeSpace, page);
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
        if (page.getPageHeader().getType() == PageType.MULTI_PAGE) {
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
            final Page<BFilePageHeader> page = createPage(pos);
            final byte[] data = page.read(backingFile.randomAccessFile);
            if (!PageType.isRecordType(page.getPageHeader().getType())) {
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
                final Page<BFilePageHeader> page = createPage(loggable.page);
                if (page == null) {
                    LOG.warn("page {} not found!", loggable.page);
                    return;
                }
                final byte[] data = page.read(backingFile.randomAccessFile);
                if ((!PageType.isRecordType(page.getPageHeader().getType())) || isUptodate(page, loggable)) {
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
                final Page<BFilePageHeader> page = createPage(loggable.page);
                final byte[] data = page.read(backingFile.randomAccessFile);
                if ((!PageType.isRecordType(page.getPageHeader().getType())) || isUptodate(page, loggable)) {
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
                final Page<BFilePageHeader> page = createPage(loggable.pageNum);
                byte[] data = page.read(backingFile.randomAccessFile);
                if (page.getPageHeader().getLsn().equals(Lsn.LSN_INVALID) || requiresRedo(loggable, page)) {
                    dropFreePageList();
                    final BFilePageHeader ph = page.getPageHeader();
                    ph.updateType(PageType.MULTI_PAGE);
                    ph.setNextInChain(0L);
                    ph.setLastInChain(0L);
                    ph.setDataLength(0);
                    ph.setNextTID((short) 32);
                    data = new byte[getPageContentSize()];
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
                final Page<BFilePageHeader> page = createPage(loggable.getPageNum());
                if (page == null) {
                    LOG.warn("page {} not found!", loggable.getPageNum());
                    return;
                }
                final byte[] data = page.read(backingFile.randomAccessFile);
                if ((!PageType.isRecordType(page.getPageHeader().getType())) || isUptodate(page, loggable)) {
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
        ph.updateType(loggable.getPageType());
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
            LOG.error("{}: storage error in page: {}; len: {} ; value: {}; max: {}; status: {}", FileUtils.fileName(getFile()), page.getPageNum(), len, value.size(), getPageContentSize(), page.ph.getType());
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
                free = new FreeSpace(page.getPageNum(), getPageContentSize() - len);
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
                final int newFree = getPageContentSize() - len;
                if (newFree > minFree) {
                    FreeSpace freeSpace = fileHeader.getFreeSpace(page.getPageNum());
                    if (freeSpace == null) {
                        freeSpace = new FreeSpace(page.getPageNum(), newFree);
                        fileHeader.addFreeSpace(freeSpace);
                    } else {
                        freeSpace.free = newFree;
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
                final Page<BFilePageHeader> page = createPage(newPage);
                byte[] data = page.read(backingFile.randomAccessFile);
                if (page.getPageHeader().getLsn().equals(Lsn.LSN_INVALID) || (loggable != null && requiresRedo(loggable, page)) ) {
                    if (reuseDeleted) {
                        reuseDeleted(page);
                    } else {
                        dropFreePageList();
                    }
                    final BFilePageHeader ph = page.getPageHeader();
                    ph.updateType(PageType.RECORD);
                    ph.setDataLength(0);
                    ph.updateDataLen(getPageContentSize());
                    data = new byte[getPageContentSize()];
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

    /**
     * Base class for a data page in a {@link BFile}.
     *
     * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
     * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
     */
    public abstract class AbstractDataPage extends AbstractCacheable implements Comparable<AbstractDataPage>, Cacheable {

        public abstract void delete() throws IOException;

        public abstract byte[] getData() throws IOException;

        public abstract BFilePageHeader getPageHeader();

        public abstract String getPageInfo();

        public abstract long getPageNum();

        public abstract int findValuePosition(short tid) throws IOException;

        public abstract short getNextTID();

        public abstract void removeTID(short tid, int length);

        public abstract void setOffset(short tid, int offset);

        @Override
        public long getKey() {
            return getPageNum();
        }

        @Override
        public boolean sync(final boolean syncJournal) throws IOException {
            if (isDirty()) {
                write();
                if (isRecoveryEnabled() && syncJournal && logManager != null && logManager.lastWrittenLsn().compareTo(getPageHeader().getLsn()) < 0) {
                    logManager.flush(true, false);
                }
                return true;
            }
            return false;
        }

        public abstract void setData(byte[] buf);

        public abstract SinglePage getFirstPage();

        @Override
        public void setDirty(final boolean dirty) {
            super.setDirty(dirty);

            getPageHeader().setDirty(dirty);
        }

        public abstract void write() throws IOException;

        @Override
        public int compareTo(final AbstractDataPage other) {
            if (getPageNum() == other.getPageNum()) {
                return Constants.EQUAL;
            } else if (getPageNum() > other.getPageNum()) {
                return Constants.SUPERIOR;
            } else {
                return Constants.INFERIOR;
            }
        }
    }

    /**
     * Represents an overflow page (as opposed to a single data page, see: {@link SinglePage}).
     *
     * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
     * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
     */
    private class OverflowPage extends AbstractDataPage {
        private final SinglePage firstPage;
        private byte[] data = null;

        public OverflowPage(final Txn transaction) throws IOException {
            firstPage = new SinglePage();
            if (transaction != null && isRecoveryEnabled()) {
                final Loggable loggable = new OverflowCreateLoggable(fileId, transaction, firstPage.getPageNum());
                writeToLog(loggable, firstPage);
            }
            final BFilePageHeader ph = firstPage.getPageHeader();
            ph.updateType(PageType.MULTI_PAGE);
            ph.setNextInChain(0L);
            ph.setLastInChain(0L);
            ph.setDataLength(0);
            firstPage.setData(new byte[getPageContentSize()]);
            dataCache.add(firstPage, 3);
        }

        public OverflowPage(final AbstractDataPage page) {
            firstPage = (SinglePage) page;
        }

        public OverflowPage(final Page<BFilePageHeader> p, final byte[] data) throws IOException {
            firstPage = new SinglePage(p, data, false);
            firstPage.getPageHeader().updateType(PageType.MULTI_PAGE);
        }

        /**
         * Append a new chunk of data to the page
         *
         * @param transaction the database transaction
         * @param chunk chunk of data to append
         */
        public void append(final Txn transaction, final ByteArray chunk) throws IOException {
            SinglePage nextPage;
            BFilePageHeader ph = firstPage.getPageHeader();
            final int newLen = ph.getDataLength() + chunk.size();
            // get the last page and fill it
            final long next = ph.getLastInChain();
            AbstractDataPage page;
            if (next > 0) {
                page = getDataPage(next, false);
            } else {
                page = firstPage;
            }
            ph = page.getPageHeader();
            final int chunkLen = chunk.size();
            int chunkSize = getPageContentSize() - ph.getDataLength();
            if (chunkLen < chunkSize) {
                chunkSize = chunkLen;
            }
            // fill last page
            if (transaction != null && isRecoveryEnabled()) {
                final Loggable loggable =
                    new OverflowAppendLoggable(fileId, transaction, page.getPageNum(), chunk, 0, chunkSize);
                writeToLog(loggable, page);
            }
            chunk.copyTo(0, page.getData(), ph.getDataLength(), chunkSize);
            if (page != firstPage) {
                ph.setDataLength(ph.getDataLength() + chunkSize);
            }
            page.setDirty(true);
            // write the remaining chunks to new pages
            int remaining = chunkLen - chunkSize;
            int current = chunkSize;
            chunkSize = getPageContentSize();
            if (remaining > 0) {
                // walk through chain of pages
                while (remaining > 0) {
                    if (remaining < chunkSize) {
                        chunkSize = remaining;
                    }

                    // add a new page to the chain
                    nextPage = createDataPage();
                    if (transaction != null && isRecoveryEnabled()) {
                        Loggable loggable = new OverflowCreatePageLoggable(transaction, fileId, nextPage.getPageNum(),
                            page.getPageNum());
                        writeToLog(loggable, nextPage);

                        loggable = new OverflowAppendLoggable(fileId, transaction, nextPage.getPageNum(),
                            chunk, current, chunkSize);
                        writeToLog(loggable, page);
                    }
                    nextPage.setData(new byte[getPageContentSize()]);
                    page.getPageHeader().setNextInChain(nextPage.getPageNum());
                    page.setDirty(true);
                    dataCache.add(page);
                    page = nextPage;
                    // copy next chunk of data to the page
                    chunk.copyTo(current, page.getData(), 0, chunkSize);
                    page.setDirty(true);
                    if (page != firstPage) {
                        page.getPageHeader().setDataLength(chunkSize);
                    }
                    remaining = remaining - chunkSize;
                    current += chunkSize;
                }
            }
            ph = firstPage.getPageHeader();
            if (transaction != null && isRecoveryEnabled()) {
                final Loggable loggable = new OverflowModifiedLoggable(fileId, transaction, firstPage.getPageNum(),
                    ph.getDataLength() + chunkLen, ph.getDataLength(), page == firstPage ? 0 : page.getPageNum());
                writeToLog(loggable, page);
            }
            if (page != firstPage) {
                // add link to last page
                dataCache.add(page);
                ph.setLastInChain(page.getPageNum());

            } else {
                ph.setLastInChain(0L);
            }
            // adjust length field in first page
            ph.setDataLength(newLen);
            ByteConversion.intToByte(firstPage.getPageHeader().getDataLength() - 6, firstPage.getData(), 2);
            firstPage.setDirty(true);
            // keep the first page in cache
            dataCache.add(firstPage, 2);
        }

        @Override
        public void delete() throws IOException {
            delete(null);
        }

        public void delete(final Txn transaction) throws IOException {
            long next = firstPage.getPageNum();
            SinglePage page = firstPage;
            do {
                next = page.ph.getNextInChain();
                if (transaction != null && isRecoveryEnabled()) {
                    int dataLen = page.ph.getDataLength();
                    if (dataLen > getPageContentSize()) {
                        dataLen = getPageContentSize();
                    }
                    final Loggable loggable = new OverflowRemoveLoggable(fileId, transaction,
                        page.ph.getType(), page.getPageNum(),
                        page.getData(), dataLen,
                        page.ph.getNextInChain());
                    writeToLog(loggable, page);
                }

                page.getPageHeader().setNextInChain(-1L);
                page.setDirty(true);
                dataCache.remove(page);
                page.delete();
                if (next > 0) {
                    page = getSinglePage(next);
                }
            } while (next > 0);
        }

        public VariableByteInput getDataStream(final long pointer) {
            final BFile.MultiPageInput input = new BFile.MultiPageInput(firstPage, pointer);
            return input;
        }

        @Override
        public byte[] getData() throws IOException {
            if (data != null) {
                return data;
            }

            SinglePage page = firstPage;
            long next;
            byte[] temp;
            int len;
            try(final UnsynchronizedByteArrayOutputStream os = new UnsynchronizedByteArrayOutputStream(page.getPageHeader().getDataLength())) {
                do {
                    temp = page.getData();
                    next = page.getPageHeader().getNextInChain();
                    len = next > 0 ? getPageContentSize() : page
                        .getPageHeader().getDataLength();
                    os.write(temp, 0, len);

                    if (next > 0) {
                        page = (SinglePage) getDataPage(next, false);
                        dataCache.add(page);
                    }
                } while (next > 0);
                data = os.toByteArray();
                if (data.length != firstPage.getPageHeader().getDataLength()) {
                    LOG.warn("{} read={}; expected={}", FileUtils.fileName(getFile()), data.length, firstPage.getPageHeader().getDataLength());
                }
                return data;
            }
        }

        @Override
        public SinglePage getFirstPage() {
            return firstPage;
        }

        @Override
        public BFilePageHeader getPageHeader() {
            return firstPage.getPageHeader();
        }

        @Override
        public String getPageInfo() {
            return "MULTI_PAGE: " + firstPage.getPageInfo();
        }

        @Override
        public long getPageNum() {
            return firstPage.getPageNum();
        }

        @Override
        public void setData(final byte[] buf) {
            setData(null, buf);
        }

        public void setData(final Txn transaction, final byte[] data) {
            this.data = data;
            try {
                write(transaction);
            } catch (final IOException e) {
                LOG.error(e);
            }
        }

        @Override
        public void write() throws IOException {
            write(null);
        }

        public void write(final Txn transaction) throws IOException {
            if (data == null) {
                return;
            }

            int chunkSize = getPageContentSize();
            int remaining = data.length;
            int current = 0;
            long next = 0L;
            SinglePage page = firstPage;
            page.getPageHeader().setDataLength(remaining);
            SinglePage nextPage;
            long prevPageNum = Page.NO_PAGE;
            // walk through chain of pages
            while (remaining > 0) {
                if (remaining < chunkSize) {
                    chunkSize = remaining;
                }
                page.clear();
                // copy next chunk of data to the page
                if (transaction != null && isRecoveryEnabled()) {
                    final Loggable loggable = new OverflowStoreLoggable(fileId, transaction, page.getPageNum(), prevPageNum,
                        data, current, chunkSize);
                    writeToLog(loggable, page);
                }
                System.arraycopy(data, current, page.getData(), 0, chunkSize);
                if (page != firstPage) {
                    page.getPageHeader().setDataLength(chunkSize);
                }
                page.setDirty(true);
                remaining -= chunkSize;
                current += chunkSize;
                next = page.getPageHeader().getNextInChain();
                if (remaining > 0) {
                    if (next > 0) {
                        // load next page in chain
                        nextPage = (SinglePage) getDataPage(next, false);
                        dataCache.add(page);
                        prevPageNum = page.getPageNum();
                        page = nextPage;
                    } else {
                        // add a new page to the chain
                        nextPage = createDataPage();
                        if (transaction != null && isRecoveryEnabled()) {
                            final Loggable loggable = new CreatePageLoggable(transaction, fileId, nextPage.getPageNum());
                            writeToLog(loggable, nextPage);
                        }
                        nextPage.setData(new byte[getPageContentSize()]);
                        nextPage.getPageHeader().setNextInChain(0L);
                        page.getPageHeader().setNextInChain(
                            nextPage.getPageNum());
                        dataCache.add(page);
                        prevPageNum = page.getPageNum();
                        page = nextPage;
                    }
                } else {
                    page.getPageHeader().setNextInChain(0L);
                    if (page != firstPage) {
                        page.setDirty(true);
                        dataCache.add(page);
                        firstPage.getPageHeader().setLastInChain(
                            page.getPageNum());
                    } else {
                        firstPage.getPageHeader().setLastInChain(0L);
                    }
                    firstPage.setDirty(true);
                    dataCache.add(firstPage, 3);
                }
            }


            if (next > 0) {
                // there are more pages in the chain:
                // remove them
                while (next > 0) {
                    nextPage = (SinglePage) getDataPage(next, false);

                    next = nextPage.getPageHeader().getNextInChain();

                    if (transaction != null && isRecoveryEnabled()) {
                        final Loggable loggable = new OverflowRemoveLoggable(fileId, transaction,
                            nextPage.getPageHeader().getType(), nextPage.getPageNum(),
                            nextPage.getData(), nextPage.getPageHeader().getDataLength(),
                            nextPage.getPageHeader().getNextInChain());
                        writeToLog(loggable, nextPage);
                    }

                    nextPage.setDirty(true);
                    nextPage.delete();
                    dataCache.remove(nextPage);
                }
            }
            firstPage.getPageHeader().setDataLength(data.length);
            firstPage.setDirty(true);
            dataCache.add(firstPage, 3);
            //            LOG.debug(firstPage.getPageNum() + " data length: " + firstPage.ph.getDataLength());
        }

        @Override
        public int findValuePosition(final short tid) {
            return 2;
        }

        @Override
        public short getNextTID() {
            return 1;
        }

        @Override
        public void removeTID(final short tid, final int length) {
            //
        }

        @Override
        public void setOffset(final short tid, final int offset) {
            //
        }
    }

    /**
     * Represents a single data page (as opposed to an overflow page, see: {@link OverflowPage}).
     *
     * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
     * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
     */
    private class SinglePage extends AbstractDataPage {

        // the raw working data of this page (without page header)
        byte[] data = null;

        // the low-level page
        final Page<BFilePageHeader> page;

        // the page header
        final BFilePageHeader ph;

        // table mapping record ids (tids) to offsets
        short[] offsets = null;

        public SinglePage() throws IOException {
            page = getFreePage();
            ph = page.getPageHeader();
            ph.updateType(PageType.RECORD);
            ph.setDirty(true);
            ph.setDataLength(0);
            //ph.setNextChunk( -1 );
            final ReentrantReadWriteLock.ReadLock fileHeaderReadlock = fileHeader.readLock();
            try {
                data = new byte[getPageContentSize()];
            } finally {
                fileHeaderReadlock.unlock();
            }
            offsets = new short[32];
            ph.setNextTID((short) 32);
            Arrays.fill(offsets, (short)-1);
        }

        public SinglePage(final Page<BFilePageHeader> p, final byte[] data, final boolean initialize) throws IOException {
            if (p == null) {
                throw new IOException("illegal page");
            }

            if (!(p.getPageHeader().getType() == PageType.RECORD || p.getPageHeader()
                .getType() == PageType.MULTI_PAGE)) {
                final IOException e = new IOException("not a data-page: "
                    + p.getPageHeader().getType());
                LOG.debug("not a data-page: {}", p.getPageInfo(), e);
                throw e;
            }
            this.data = data;
            page = p;
            ph = page.getPageHeader();
            if(initialize) {
                offsets = new short[ph.getCurrentTID()];
                if (ph.getType() != PageType.MULTI_PAGE) {
                    readOffsets();
                }
            }
        }

        @Override
        public final int findValuePosition(final short tid) throws IOException {
            return offsets[tid];
        }

        private void readOffsets() {
            //if(offsets.length > 256)
            //LOG.warn("TID size: " + ph.nextTID);
            Arrays.fill(offsets, (short)-1);
            final int dlen = ph.getDataLength();
            for(short pos = 0; pos < dlen; ) {
                final short tid = ByteConversion.byteToShort(data, pos);
                if (tid < 0) {
                    LOG.error("Invalid tid found: {}; ignoring rest of page ...", tid);
                    ph.setDataLength(pos);
                    return;
                }
                if(tid >= offsets.length) {
                    LOG.error("Problematic tid found: {}; trying to recover ...", tid);
                    final short[] t = new short[tid + 1];
                    Arrays.fill(t, (short)-1);
                    System.arraycopy(offsets, 0, t, 0, offsets.length);
                    offsets = t;
                    ph.setNextTID((short)(tid + 1));
                }
                offsets[tid] = (short)(pos + 2);
                pos += ByteConversion.byteToInt(data, pos + 2) + 6;
            }
        }

        @Override
        public short getNextTID() {
            for(short i = 0; i < offsets.length; i++) {
                if(offsets[i] == -1) {
                    return i;
                }
            }
            final short tid = (short)offsets.length;
            final short next = (short)(ph.getCurrentTID() * 2);
            if (next < 0 || next < ph.getCurrentTID()) {
                return -1;
            }
            final short[] t = new short[next];
            Arrays.fill(t, (short)-1);
            System.arraycopy(offsets, 0, t, 0, offsets.length);
            offsets = t;
            ph.setNextTID(next);
            return tid;
        }

        public void adjustTID(final short tid) {
            if (tid >= ph.getCurrentTID()) {
                final short next = (short)(tid * 2);
                final short[] t = new short[next];
                Arrays.fill(t, (short)-1);
                System.arraycopy(offsets, 0, t, 0, offsets.length);
                offsets = t;
                ph.setNextTID(next);
            }
        }

        public void clear() {
            Arrays.fill(data, (byte) 0);
        }

        String printContents() {
            final StringBuilder buf = new StringBuilder();
            for (short i = 0; i < offsets.length; i++) {
                if (offsets[i] > -1) {
                    buf.append('[').append(i).append(", ").append(offsets[i]);
                    final short len = ByteConversion.byteToShort(data, offsets[i]);
                    buf.append(", ").append(len).append(']');
                }
            }
            return buf.toString();
        }

        @Override
        public void setOffset(final short tid, final int offset) {
            if (offsets == null) {
                LOG.warn("page: {} file: {} status: {}", page.getPageNum(), FileUtils.fileName(getFile()), getPageHeader().getType());
                throw new RuntimeException("page offsets not initialized");
            }
            offsets[tid] = (short)offset;
        }

        @Override
        public void removeTID(final short tid, final int length) {
            final int offset = offsets[tid] - 2;
            offsets[tid] = -1;
            for(short i = 0; i < offsets.length; i++) {
                if(offsets[i] > offset) {
                    offsets[i] -= length;
                }
            }
            //readOffsets(start);
        }

        @Override
        public void delete() throws IOException {
            // reset page header fields
            ph.setDataLength(0);
            ph.setNextInChain(-1L);
            ph.setLastInChain(-1L);
            ph.setNextTID((short) -1);
            ph.setRecordCount((short) 0);
            setReferenceCount(0);
            ph.setDirty(true);
            unlinkPages(page);
        }

        @Override
        public SinglePage getFirstPage() {
            return this;
        }

        @Override
        public byte[] getData() {
            return data;
        }

        @Override
        public BFilePageHeader getPageHeader() {
            return ph;
        }

        @Override
        public String getPageInfo() {
            return page.getPageInfo();
        }

        @Override
        public long getPageNum() {
            return page.getPageNum();
        }

        @Override
        public void setData(final byte[] buf) {
            data = buf;
        }

        @Override
        public void write() throws IOException {
            //LOG.debug(getPath().getName() + " writing page " + getPageNum());
            writeValue(page, new Value(data));
            setDirty(false);
        }
    }

    /**
     * Variable byte input stream to read a multi-page sequences.
     *
     * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
     * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
     */
    private class MultiPageInput implements VariableByteInput, PageInput {
        private SinglePage nextPage;
        private int pageLen;
        private short offset = 0;
        private final long address;

        public MultiPageInput(final SinglePage first, final long address) {
            this.nextPage = first;
            this.offset = 6;
            this.pageLen = first.ph.getDataLength();
            if (this.pageLen > getPageContentSize()) {
                this.pageLen = getPageContentSize();
            }
            dataCache.add(first, 3);
            this.address = address;
        }

        @Override
        public long getAddress() {
            return address;
        }

        @Override
        public int read() throws IOException {
            if (offset == pageLen) {
                advance();
            }
            return nextPage.data[offset++] & 0xFF;
        }

        @Override
        public byte readByte() throws IOException {
            if (offset == pageLen) {
                advance();
            }
            return nextPage.data[offset++];
        }

        @Override
        public short readShort() throws IOException {
            if (offset == pageLen) {
                advance();
            }
            byte b = nextPage.data[offset++];
            short i = (short) (b & 0177);
            for (int shift = 7; (b & 0200) != 0; shift += 7) {
                if (offset == pageLen) {
                    advance();
                }
                b = nextPage.data[offset++];
                i |= (b & 0177) << shift;
            }
            return i;
        }

        @Override
        public short readFixedShort() throws IOException {
            if (offset == pageLen) {
                advance();
            }
            // do we have to read across a page boundary?
            if (offset + 2 < pageLen) {
                return (short) ((nextPage.data[offset++] & 0xff) |
                    ((nextPage.data[offset++] & 0xff) << 8));
            }

            short r = (short) (nextPage.data[offset++] & 0xff);
            if (offset == pageLen) {
                advance();
            }
            r |= (nextPage.data[offset++] & 0xff) << 8;

            return r;
        }

        @Override
        public int readInt() throws IOException {
            if (offset == pageLen) {
                advance();
            }
            byte b = nextPage.data[offset++];
            int i = b & 0177;
            for (int shift = 7; (b & 0200) != 0; shift += 7) {
                if (offset == pageLen) {
                    advance();
                }
                b = nextPage.data[offset++];
                i |= (b & 0177) << shift;
            }
            return i;
        }

        @Override
        public int readFixedInt() throws IOException {
            if (offset == pageLen) {
                advance();
            }
            // do we have to read across a page boundary?
            if (offset + 4 < pageLen) {
                return ( nextPage.data[offset++] & 0xff ) |
                    ( (nextPage.data[offset++] & 0xff) << 8 ) |
                    ( (nextPage.data[offset++] & 0xff) << 16 ) |
                    ( (nextPage.data[offset++] & 0xff) << 24 );
            }

            int r = nextPage.data[offset++] & 0xff;
            int shift = 8;
            for (int i = 0; i < 3; i++) {
                if (offset == pageLen) {
                    advance();
                }
                r |= (nextPage.data[offset++] & 0xff) << shift;
                shift += 8;
            }
            return r;
        }

        @Override
        public long readLong() throws IOException {
            if (offset == pageLen) {
                advance();
            }
            byte b = nextPage.data[offset++];
            long i = b & 0177;
            for (int shift = 7; (b & 0200) != 0; shift += 7) {
                if (offset == pageLen) {
                    advance();
                }
                b = nextPage.data[offset++];
                i |= (b & 0177L) << shift;
            }
            return i;
        }

        @Override
        public long readFixedLong() throws IOException {
            if (offset == pageLen) {
                advance();
            }
            // do we have to read across a page boundary?
            if (offset + 8 < pageLen) {
                return ((nextPage.data[offset++] & 0xff) << 56) |
                    ((nextPage.data[offset++] & 0xff) << 48) |
                    ((nextPage.data[offset++] & 0xff) << 40) |
                    ((nextPage.data[offset++] & 0xff) << 32) |
                    ((nextPage.data[offset++] & 0xff) << 24) |
                    ((nextPage.data[offset++] & 0xff) << 16) |
                    ((nextPage.data[offset++] & 0xff) << 8) |
                    (nextPage.data[offset++] & 0xff);
            }

            int r = (nextPage.data[offset++] & 0xff) << 56;
            int shift = 48;
            for (int i = 0; i < 7; i++) {
                if (offset == pageLen) {
                    advance();
                }
                r |= (nextPage.data[offset++] & 0xff) << shift;
                shift -= 8;
            }
            return r;
        }

        @Override
        public BigInteger readBigInteger() throws IOException {
            final int dataLength = readInt();
            final byte[] data = new byte[dataLength];
            read(data);

            return new BigInteger(data);
        }

        @Override
        public BigInteger readFixedBigInteger() throws IOException {
            final int dataLength = readFixedInt();
            final byte[] data = new byte[dataLength];
            read(data);

            return new BigInteger(data);
        }

        @Override
        public BigDecimal readBigDecimal() throws IOException {
            final int scale = readInt();
            final int precision = readInt();
            final int dataLength = readInt();
            final byte[] data = new byte[dataLength];
            read(data);

            final MathContext mathContext = new java.math.MathContext(precision);
            return new BigDecimal(new BigInteger(data), scale, mathContext);
        }

        @Override
        public BigDecimal readFixedBigDecimal() throws IOException {
            final int scale = readFixedInt();
            final int precision = readFixedInt();
            final int dataLength = readFixedInt();
            final byte[] data = new byte[dataLength];
            read(data);

            final MathContext mathContext = new java.math.MathContext(precision);
            return new BigDecimal(new BigInteger(data), scale, mathContext);
        }

        @Override
        public void skip(final int count) throws IOException {
            for (int i = 0; i < count; i++) {
                do {
                    if (offset == pageLen) {
                        advance();
                    }
                } while ((nextPage.data[offset++] & 0200) > 0);
            }
        }

        @Override
        public void skipBytes(final long count) throws IOException {
            for(long i = 0; i < count; i++) {
                if (offset == pageLen) {
                    advance();
                }
                offset++;
            }
        }

        private void advance() throws IOException {
            final long next = nextPage.getPageHeader().getNextInChain();
            if (next < 1) {
                pageLen = -1;
                offset = 0;
                throw new EOFException();
            }


            try (final ManagedLock<ReentrantLock> bfileLock = lockManager.acquireBtreeReadLock(getLockName())) {
                nextPage = (SinglePage) getDataPage(next, false);
                pageLen = nextPage.ph.getDataLength();
                offset = 0;
                dataCache.add(nextPage);
            } catch (final LockException e) {
                throw new IOException("failed to acquire a read lock on "
                    + FileUtils.fileName(getFile()));
            }
        }

        @Override
        public int available() throws IOException {
            if (pageLen < 0) {
                return 0;
            }
            int inPage = pageLen - offset;
            if (inPage == 0) {
                inPage = nextPage.getPageHeader().getNextInChain() > 0 ? 1 : 0;
            }
            return inPage;
        }

        @Override
        public int read(final byte[] data) throws IOException {
            return read(data, 0, data.length);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            if (pageLen < 0) {
                return -1;
            }

            for (int i = 0; i < len; i++) {
                if (offset == pageLen) {
                    final long next = nextPage.getPageHeader().getNextInChain();
                    if (next < 1) {
                        pageLen = -1;
                        offset = 0;
                        return i;
                    }
                    nextPage = (SinglePage) getDataPage(next, false);
                    pageLen = nextPage.ph.getDataLength();
                    offset = 0;
                    dataCache.add(nextPage);
                }
                b[off + i] = nextPage.data[offset++];
            }
            return len;
        }

        @Override
        public String readUTF() throws IOException {
            final int len = readInt();
            final byte data[] = new byte[len];

            read(data);

            return new String(data, UTF_8);
        }

        @Override
        public void copyTo(final VariableByteOutput os) throws IOException {
            byte more;
            do {
                if (offset == pageLen) {
                    advance();
                }
                more = nextPage.data[offset++];
                os.writeByte(more);
                more &= 0200;
            } while (more > 0);
        }

        @Override
        public void copyTo(final VariableByteOutput os, final int count) throws IOException {
            byte more;
            for (int i = 0; i < count; i++) {
                do {
                    if (offset == pageLen) {
                        advance();
                    }
                    more = nextPage.data[offset++];
                    os.writeByte(more);
                } while ((more & 0x200) > 0);
            }
        }

        @Override
        public void copyRaw(final VariableByteOutput os, final int count) throws IOException {
            for (int i = count; i != 0; ) {
                if (offset == pageLen) {
                    advance();
                }
                int avail = pageLen - offset;
                if (i >= avail) {
                    os.write(nextPage.data, offset, avail);
                    i -= avail;
                    offset = (short) pageLen;
                } else {
                    os.write(nextPage.data, offset, i);
                    offset += i;
                    break;
                }
                //os.writeByte(nextPage.data[offset++]);
            }
        }

        @Override
        public long position() {
            return StorageAddress.createPointer((int) nextPage.getPageNum(), offset);
        }

        @Override
        public void seek(final long position) throws IOException {
            final int newPage = StorageAddress.pageFromPointer(position);
            final short newOffset = StorageAddress.tidFromPointer(position);
            try (final ManagedLock<ReentrantLock> bfileLock =  lockManager.acquireBtreeReadLock(getLockName())) {
                nextPage = getSinglePage(newPage);
                pageLen = nextPage.ph.getDataLength();
                if (pageLen > getPageContentSize()) {
                    pageLen = getPageContentSize();
                }
                offset = newOffset;
                dataCache.add(nextPage);
            } catch (final LockException e) {
                throw new IOException("Failed to acquire a read lock on " + FileUtils.fileName(getFile()));
            }
        }
    }

    private enum FindCallbackMode {
        BOTH,
        KEYS,
        VALUES
    }

    /**
     * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
     * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
     */
    private class FilterCallback implements BTreeCallback {
        private final BFileCallback callback;

        public FilterCallback(final BFileCallback callback) {
            this.callback = callback;
        }

        @Override
        public boolean indexInfo(final Value value, final long pointer) throws TerminatedException {
            try {
                final long pos = StorageAddress.pageFromPointer(pointer);
                final short tid = StorageAddress.tidFromPointer(pointer);
                final AbstractDataPage page = getDataPage(pos);
                final int offset = page.findValuePosition(tid);
                final byte[] data = page.getData();
                final int l = ByteConversion.byteToInt(data, offset);
                final Value v = new Value(data, offset + 4, l);
                callback.info(value, v);
                return true;
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                return true;
            }
        }
    }

    /**
     * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
     * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
     */
    private class FindCallback implements BTreeCallback {

        private final FindCallbackMode mode;
        private @Nullable
        final IndexCallback callback;
        private @Nullable final ArrayList<Value> values;

        public FindCallback(final FindCallbackMode mode) {
            this.mode = mode;
            this.callback = null;
            this.values = new ArrayList<>();
        }

        public FindCallback(final IndexCallback callback) {
            this.mode = FindCallbackMode.BOTH;
            this.callback = callback;
            this.values = null;
        }

        public List<Value> getValues() {
            return values;
        }

        @Override
        public boolean indexInfo(final Value value, final long pointer) throws TerminatedException {
            // TODO(AR) can we cleanup and unify the code below... there seems to be a lot of duplication
            final long pos;
            final short tid;
            final AbstractDataPage page;
            final int offset;
            final int l;
            final Value v;
            byte[] data;
            try {
                switch (mode) {
                    case VALUES:
                        pos = StorageAddress.pageFromPointer(pointer);
                        tid = StorageAddress.tidFromPointer(pointer);
                        page = getDataPage(pos);
                        dataCache.add(page.getFirstPage());
                        offset = page.findValuePosition(tid);
                        data = page.getData();
                        l = ByteConversion.byteToInt(data, offset);
                        v = new Value(data, offset + 4, l);
                        v.setAddress(pointer);

                        if (callback == null) {
                            values.add(v);
                        } else {
                            return callback.indexInfo(value, v);
                        }
                        return true;

                    case KEYS:
                        value.setAddress(pointer);
                        if (callback == null) {
                            values.add(value);
                        } else {
                            return callback.indexInfo(value, null);
                        }
                        return true;

                    case BOTH:
                        pos = StorageAddress.pageFromPointer(pointer);
                        tid = StorageAddress.tidFromPointer(pointer);
                        page = getDataPage(pos);
                        // TODO(AR) is this bit below superfluous as `data` is then overwritten just below
                        if (page.getPageHeader().getType() == PageType.MULTI_PAGE) {
                            data = page.getData();
                        }
                        dataCache.add(page.getFirstPage());
                        offset = page.findValuePosition(tid);
                        data = page.getData();
                        l = ByteConversion.byteToInt(data, offset);
                        v = new Value(data, offset + 4, l);
                        v.setAddress(pointer);
                        if (callback == null) {
                            values.add(value);
                            values.add(v);
                        } else {
                            return callback.indexInfo(value, v);
                        }

                        return true;
                }
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
            }

            return false;
        }
    }
}
