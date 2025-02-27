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
 * NOTE: This file is in part based on code from The dbXML Group.
 * The original license statement is also included below.
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
 *
 * ---------------------------------------------------------------------
 *
 * dbXML License, Version 1.0
 *
 * Copyright (c) 1999-2001 The dbXML Group, L.L.C.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        The dbXML Group (http://www.dbxml.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "dbXML" and "The dbXML Group" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact info@dbxml.com.
 *
 * 5. Products derived from this software may not be called "dbXML",
 *    nor may "dbXML" appear in their name, without prior written
 *    permission of The dbXML Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE DBXML GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.exist.storage.btree;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.exist.storage.*;

import org.exist.storage.cache.*;
import org.exist.storage.journal.*;
import org.exist.storage.txn.Txn;
import org.exist.util.ByteConversion;
import org.exist.util.FileUtils;
import org.exist.util.HexEncoder;
import org.exist.util.Lockable;
import org.exist.xquery.TerminatedException;

import javax.annotation.Nullable;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 *  A general purpose B+-tree which stores binary keys as instances of
 *  {@link org.exist.storage.btree.Value}. The actual value data is not
 *  stored in the B+tree itself. Instead, we use long pointers to record the
 *  storage address of the value. This class has no methods to locate or
 *  modify data records. Data handling is in the responsibility of the
 *  proper subclasses: {@link org.exist.storage.index.BFile} and
 *  {@link org.exist.storage.dom.DOMFile}.
 *  
 *  Both, branch and leaf nodes are represented by the inner class 
 *  {@link org.exist.storage.btree.AbstractBTree.BTreeNode}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
public abstract class AbstractBTree<HEADER extends BTreeFileHeader, PAGE_HEADER extends BTreePageHeader> extends AbstractPagedFile<HEADER, PAGE_HEADER> implements Lockable {

    protected final static Logger LOGSTATS = LogManager.getLogger(NativeBroker.EXIST_STATISTICS_LOGGER);
    
    /** Used as return value, if a value was not found */
    public final static long KEY_NOT_FOUND = -1;

    /** Log entry type for an insert value operation */
    public final static byte LOG_INSERT_VALUE = 0x20;
    /** Log entry type for creation of a new BTree node */
    public final static byte LOG_CREATE_BNODE = 0x21;
    /** Log entry type for a page update resulting from a page split */
    public final static byte LOG_UPDATE_PAGE = 0x22;
    /** Log entry type for a parent page change resulting from a page split */
    public final static byte LOG_SET_PARENT = 0x23;
    /** Log entry type for a value update */
    public final static byte LOG_UPDATE_VALUE = 0x24;
    /** Log entry type for removing a value */
    public final static byte LOG_REMOVE_VALUE = 0x25;
    public final static byte LOG_SET_LINK = 0x26;

    static {
        // register the log entry types used for the BTree
        LogEntryTypes.addEntryType(LOG_INSERT_VALUE, InsertValueLoggable::new);
        LogEntryTypes.addEntryType(LOG_UPDATE_VALUE, UpdateValueLoggable::new);
        LogEntryTypes.addEntryType(LOG_REMOVE_VALUE, RemoveValueLoggable::new);
        LogEntryTypes.addEntryType(LOG_CREATE_BNODE, CreateBTNodeLoggable::new);
        LogEntryTypes.addEntryType(LOG_UPDATE_PAGE, UpdatePageLoggable::new);
        LogEntryTypes.addEntryType(LOG_SET_PARENT, SetParentLoggable::new);
        LogEntryTypes.addEntryType(LOG_SET_LINK, SetPageLinkLoggable::new);
    }

    private final BrokerPool pool;

    protected final CacheManager cacheManager;

    /** Cache of BTreeNode(s) */
    private final Cache<BTreeNode> cache;

    /** The LogManager for writing the transaction log */
    protected final @Nullable JournalManager logManager;

    protected final byte fileId;

    private double splitFactor = -1;

    protected AbstractBTree(final BrokerPool pool, final byte fileId, final BackingFile backingFile,
            final HEADER fileHeader, final boolean enableRecovery, final CacheManager cacheManager) {
        super(backingFile, fileHeader);
        this.pool = pool;
        this.cacheManager = cacheManager;
        this.fileId = fileId;
        if (enableRecovery && pool.isRecoveryEnabled()) {
            this.logManager = pool.getJournalManager().orElse(null);
        } else {
            this.logManager = null;
        }
        this.cache = new BTreeCache<>(FileUtils.fileName(getFile()), cacheManager.getDefaultInitialSize(), 1.5, 0, Cache.CacheType.BTREE);
        this.cacheManager.registerCache(cache);
    }

    protected boolean isRecoveryEnabled() {
        return logManager != null && pool.isRecoveryEnabled();
    }

    @Override
    public String getLockName() {
        return null;
    }

    public void setSplitFactor(final double factor) {
        if (factor > 1.0) {
            throw new IllegalArgumentException("splitFactor should be <= 1 > 0");
        }
        this.splitFactor = factor;
    }

    /**
     * addValue adds a Value to the BTree and associates a pointer with it. The
     * pointer can be used for referencing any type of data, it just so happens
     * that dbXML uses it for referencing pages of associated data in the BTree
     * file or other files.
     *
     * @param  value               The Value to add
     * @param  pointer             The pointer to associate with it
     * @return                     The previous value for the pointer (or -1)
     * @throws  IOException     Description of the Exception
     * @throws  BTreeException  Description of the Exception
     */
    public long addValue(final Value value, final long pointer) throws IOException, BTreeException {
        return addValue(null, value, pointer);
    }

    public long addValue(final Txn transaction, final Value value, final long pointer) throws IOException, BTreeException {
        return getRootNode().addValue(transaction, value, pointer);
    }

    /**
     * removeValue removes a Value from the BTree and returns the associated
     * pointer for it.
     *
     * @param  value               The Value to remove
     * @return                     The pointer that was associated with it
     * @throws  IOException     Description of the Exception
     * @throws  BTreeException  Description of the Exception
     */
    public long removeValue(final Value value) throws IOException, BTreeException {
        return removeValue(null, value);
    }

    public long removeValue(final Txn transaction, final Value value) throws IOException, BTreeException {
        return getRootNode().removeValue(transaction, value);
    }


    public void remove(final IndexQuery query, final BTreeCallback callback) throws IOException,
            BTreeException, TerminatedException {
        remove(null, query, callback);
    }

    /**
     * Search for keys matching the given {@link IndexQuery} and
     * remove them from the node. Every match is reported 
     * to the specified {@link BTreeCallback}.
     *
     * @param transaction the database transaction.
     * @param query the query
     * @param callback the callback
     *
     * @throws IOException if an I/O error occurs
     * @throws BTreeException if an error occurss with the tree
     * @throws TerminatedException if the callback is terminated
     */
    public void remove(final Txn transaction, IndexQuery query, final BTreeCallback callback)
            throws IOException, BTreeException, TerminatedException {
        if (query != null && query.getOperator() == IndexQuery.TRUNC_RIGHT) {
            final Value val1 = query.getValue(0);
            final byte data1[] = val1.getData();
            final byte data2[] = new byte[data1.length];
            System.arraycopy(data1, 0, data2, 0, data1.length);
            data2[data2.length - 1] += 1;
            query = new IndexQuery(query.getOperator(), val1, new Value(data2));
        }
        getRootNode().remove(transaction, query, callback);
    }

    protected void removeSequential(final Txn transaction, final BTreeNode page, final IndexQuery query,
            final BTreeCallback callback) throws TerminatedException {
        long next = page.pageHeader.getNextPage();
        while (next != Page.NO_PAGE) {
            final BTreeNode nextPage = getBTreeNode(next);
            for (int i = 0; i < nextPage.nKeys; i++) {
                final boolean test = query.testValue(nextPage.keys[i]);
                if (query.getOperator() != IndexQuery.NEQ && !test) {
                    return;
                }
                if (test) {
                    if (transaction != null && isRecoveryEnabled() && nextPage.pageHeader.getType() == PageType.LEAF) {
                        final RemoveValueLoggable log = new RemoveValueLoggable(transaction, 
                            fileId, nextPage.page.getPageNum(), i, nextPage.keys[i], nextPage.ptrs[i]);
                        writeToLog(log, nextPage);
                    }
                    if (callback != null) {
                        callback.indexInfo(nextPage.keys[i], nextPage.ptrs[i]);
                    }
                    nextPage.removeKey(i);
                    nextPage.removePointer(i);
                    nextPage.recalculateDataLen();
                    --i;
                }
            }
            next = nextPage.pageHeader.getNextPage();
        }
    }

    /**
     * findValue finds a Value in the BTree and returns the associated pointer
     * for it.
     *
     * @param  value               The Value to find
     * @return                     The pointer that was associated with it
     * @throws IOException if an I/O error occurs
     * @throws BTreeException if an error occurss with the tree
     */
    public long findValue(final Value value) throws IOException, BTreeException {
        return getRootNode().findValue(value);
    }

    /**
     * query performs a query against the BTree and performs callback
     * operations to report the search results.
     *
     * @param  query               The IndexQuery to use (or null for everything)
     * @param  callback            The callback instance
     * @throws IOException if an I/O error occurs
     * @throws BTreeException if an error occurss with the tree
     * @throws TerminatedException if the callback is terminated
     */
    public void query(IndexQuery query, final BTreeCallback callback)
            throws IOException, BTreeException, TerminatedException {
        if (query != null && query.getOperator() == IndexQuery.TRUNC_RIGHT) {
            final Value val1 = query.getValue(0);
            final byte data1[] = val1.getData();
            final byte data2[] = new byte[data1.length];
            System.arraycopy(data1, 0, data2, 0, data1.length);
            data2[data2.length - 1] += 1;
            query = new IndexQuery(query.getOperator(), val1, new Value(data2));
        }
        getRootNode().query(query, callback);
    }

    /**
     * Executes a query against the BTree and performs callback
     * operations to report the search results. This method takes an
     * additional prefix value. Only BTree keys starting with the specified
     * prefix are considered. Search through the tree is thus restricted to
     * a given key range.
     *
     * @param  query The IndexQuery to use (or null for everything)
     * @param prefix a prefix value
     * @param  callback The callback instance
     * @throws IOException if an I/O error occurs
     * @throws BTreeException if an error occurss with the tree
     * @throws TerminatedException if the callback is terminated
     */
    public void query(final IndexQuery query, final Value prefix, final BTreeCallback callback)
            throws IOException, BTreeException, TerminatedException {
        getRootNode().query(query, prefix, callback);
    }

    protected void scanSequential(BTreeNode page, final IndexQuery query, final Value keyPrefix, final BTreeCallback callback) throws TerminatedException {
        while (page != null) {
            for (int i = 0; i < page.nKeys; i++) {
                if (keyPrefix != null && page.keys[i].comparePrefix(keyPrefix) > 0) {
                    return;
                }
                final boolean test = query.testValue(page.keys[i]);
                if (query.getOperator() != IndexQuery.NEQ && !test) {
                    return;
                }
                if (test) {
                    callback.indexInfo(page.keys[i], page.ptrs[i]);
                }
            }
            final long next = page.pageHeader.getNextPage();
            if (next != Page.NO_PAGE) {
                page = getBTreeNode(next);
            } else {
                page = null;
            }
        }
    }

    /**
     * Create a new node with the given status and parent.
     * 
     * @param transaction the database transaction
     * @param pageType the status
     * @param parent the parent
     * @param reuseDeleted true if deleted pages should be reused
     * @return The BTree node
     */
    private BTreeNode createBTreeNode(final Txn transaction, final PageType pageType, final BTreeNode parent, final boolean reuseDeleted) {
        try {
            final Page<PAGE_HEADER> page = getFreePage(reuseDeleted);
            final BTreeNode node = createBTreeNode(page, true);
            if (transaction != null && isRecoveryEnabled() && pageType == PageType.LEAF) {
                final Loggable loggable = new CreateBTNodeLoggable(transaction, fileId,
                    pageType, page.getPageNum(), parent != null ? parent.page.getPageNum() : Page.NO_PAGE);
                writeToLog(loggable, node);
            }
            node.pageHeader.updateType(pageType);
            node.setPointers(new long[0]);
            node.setParent(parent);
            node.write();
            return node;
        } catch (final IOException e) {
            LOG.error("Failed to create a BTree node", e);
            return null;
        }
    }

    private BTreeNode createBTreeNode(final Page<PAGE_HEADER> page, final boolean newPage) {
        return new BTreeNode(page, newPage);
    }

    /**
     * Read a node from the given page.
     * 
     * @param pageNum the page number
     * @return The BTree node
     */
    private BTreeNode getBTreeNode(final long pageNum) {
        try {
            BTreeNode node = cache.get(pageNum);
            if (node == null) {
                final Page<PAGE_HEADER> page = createPage(pageNum);
                node = createBTreeNode(page, false);
                node.read();
            }
            final int increment = node.pageHeader.getType() == PageType.BRANCH ? 2 : 1;
            cache.add(node, increment);
            return node;
        } catch (final IOException e) {
            LOG.error("Failed to get BTree node on page {}", pageNum, e);
            return null;
        }
    }

    /**
     * Set the root node of the tree.
     * 
     * @param rootNode the root node
     * @throws IOException if an I/O error occurs
     */
    protected void setRootNode(final BTreeNode rootNode) throws IOException {
        final ReentrantReadWriteLock.WriteLock fileHeaderWriteLock = fileHeader.writeLock();
        try {
            fileHeader.setRootPage(rootNode.page.getPageNum());
            fileHeader.write(backingFile.randomAccessFile);
        } finally {
            fileHeaderWriteLock.unlock();
        }
        cache.add(rootNode, 2);
    }

    /**
     * Create the root node.
     * 
     * @param transaction the database transaction
     * @return The root node
     * @throws IOException if an I/O error occurs
     */
    protected long createRootNode(final Txn transaction) throws IOException {
        final BTreeNode root = createBTreeNode(transaction, PageType.LEAF, null, true);
        setRootNode(root);
        return root.page.getPageNum();
    }

    /**
     * @return the root node.
     */
    protected BTreeNode getRootNode() {
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            BTreeNode node = cache.get(fileHeader.getRootPage());
            if (node == null) {
                final Page<PAGE_HEADER> page = createPage(fileHeader.getRootPage());
                node = createBTreeNode(page, false);
                node.read();
            }
            cache.add(node, 2);
            return node;
        } catch (final IOException e) {
            LOG.warn("Failed to get root btree node", e);
            return null;
        } finally {
            fileHeaderReadLock.unlock();
        }
    }

    /**
     * Print a dump of the tree to the given writer. For debug only!
     *
     * @param writer the writer
     *
     * @throws IOException if an I/O error occurs
     * @throws BTreeException if an error occurss with the tree
     */
    public void dump(final Writer writer) throws IOException, BTreeException {
        final BTreeNode root = getRootNode();
        LOG.debug("ROOT = {}", root.page.getPageNum());
        root.dump(writer);
    }

    public TreeMetrics treeStatistics() throws IOException {
        final TreeMetrics metrics = new TreeMetrics(FileUtils.fileName(getFile()));
        final BTreeNode root = getRootNode();
        root.treeStatistics(metrics);
        return metrics;
    }

    @Override
    public boolean flush() throws DBException {
        try {
            boolean flushed = cache.flush();
            flushed = flushed | super.flush();
            return flushed;
        } catch (final IOException e) {
            throw new DBException(e);
        }
    }

    @Override
	public void close() throws DBException {
        if (!isReadOnly()) {
            flush();
        }
        super.close();
        cacheManager.deregisterCache(cache);
    }

    protected void dumpValue(final Writer writer, final Value value, final PageType pageType) throws IOException {
        final byte[] data = value.getData();
        writer.write('[');
        writer.write(HexEncoder.bytesToHex(data));
//        for (int i = 0; i < data.length; i++) {
//            writer.write(Integer.toHexString(data[i]));
//        }
        writer.write(']');
    }

    public void rawScan(final IndexQuery query, final BTreeCallback callback) throws IOException,
            TerminatedException {
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            final long pages = fileHeader.getTotalCount();
            for (int i = 1; i < pages; i++) {
                final Page<PAGE_HEADER> page = createPage(i);
                page.read(backingFile.randomAccessFile);
                if (page.getPageHeader().getType() == PageType.LEAF) {
                    final BTreeNode node = createBTreeNode(page, false);
                    node.read();
                    node.scanRaw(query, callback);
                }
            }
        } finally {
            fileHeaderReadLock.unlock();
        }
    }

    private static class TreeInfo {
        final long firstPage;
        final int leafPages;

        TreeInfo(final long firstPage, final int leafs) {
            this.firstPage = firstPage;
            this.leafPages = leafs;
        }
    }

    /**
     * Scan pages by walking through the file sequentially.
     * Optionally remove all inner (branch) pages and return the first leaf page (in order).
     * This method is used to rebuild the btree from the leaf pages.
     *
     * @param removeBranches, true if branches should be removed
     * @return the tree info
     * @throws IOException if an I/O error occurs
     * @throws DBException if an error occurss with the tree
     * @throws TerminatedException if the callback is terminated
     */
    private TreeInfo scanTree(final boolean removeBranches) throws IOException, TerminatedException, DBException {
        final Set<Long> pagePointers = new HashSet<>();
        final Set<Long> nextPages = new HashSet<>();
        final List<Long> branchPages = new ArrayList<>();

        int pageCount = 0;
        final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
        try {
            final long pages = fileHeader.getTotalCount();
            for (long i = 0; i < pages; i++) {
                // first check if page is in cache. if yes, use it.
                BTreeNode node = cache.get(i);

                final Page<PAGE_HEADER> page;
                if (node != null) {
                    page = node.page;
                } else {
                    page = createPage(i);
                    page.read(backingFile.randomAccessFile);
                }
                if (page.getPageHeader().getType() == PageType.LEAF) {
                    pageCount++;
                    if (node == null) {
                        node = createBTreeNode(page, false);
                        node.read();
                    }
                    cache.add(node);
                    pagePointers.add(node.page.getPageNum());
                    if (node.pageHeader.getNextPage() != Page.NO_PAGE) {
                        nextPages.add(node.pageHeader.getNextPage());
                    }
                } else if (page.getPageHeader().getType() == PageType.BRANCH) {
                    branchPages.add(page.getPageNum());
                }
            }
        } finally {
            fileHeaderReadLock.unlock();
        }
        pagePointers.removeAll(nextPages);
        if (pagePointers.size() > 1) {
            LOG.error("Found multiple start pages: [{}]", pagePointers.stream().map(l -> Long.toString(l)).collect(Collectors.joining(", ")));
            throw new DBException("More than one start page found for btree: " + FileUtils.fileName(getFile()));
        }
        if (removeBranches) {
            for (final long p : branchPages) {
                final Page<PAGE_HEADER> page = createPage(p);
                page.read(backingFile.randomAccessFile);
                final BTreeNode node = createBTreeNode(page, false);
                node.read();
                cache.remove(node);
                unlinkPages(page);
                page.getPageHeader().setDirty(true);
            }
        }
        return new TreeInfo(pagePointers.iterator().next(), pageCount);
    }

    public void scanSequential(final PrintStream out, long pageNum, final BTreeCallback callback) throws IOException, TerminatedException {
        while (pageNum != Page.NO_PAGE) {
            out.print(pageNum + " ");
            final BTreeNode node = getBTreeNode(pageNum);
            node.scanRaw(null, callback);
            pageNum = node.pageHeader.getNextPage();
        }
        out.println();
    }

    public void scanSequential(final PrintStream out) throws TerminatedException, IOException, DBException {
        final TreeInfo info = scanTree(false);
        out.println("Sequential scan...");
        scanSequential(out, info.firstPage, (value, pointer) -> true);
    }

    /**
     * Rebuild the btree: removes all branches and rebuilds the tree by scanning
     * through leaf pages.
     *
     * @throws IOException if an I/O error occurs
     * @throws DBException if an error occurss with the tree
     * @throws TerminatedException if the callback is terminated
     */
    public void rebuild() throws TerminatedException, IOException, DBException {
        final TreeInfo info  = scanTree(true);
        if (info.leafPages == 1) {
            final BTreeNode root = getBTreeNode(info.firstPage);
            setRootNode(root);
            cache.add(root);
        } else {
            // create a new root node
            final BTreeNode root = createBTreeNode(null, PageType.BRANCH, null, false);
            setRootNode(root);
            // insert a pointer to the first page into the root
            BTreeNode node = getBTreeNode(info.firstPage);
            root.insertPointer(info.firstPage, 0);
            cache.add(root);
            node.setParent(root);
            node.setDirty(true);
            cache.add(node);

            // scan through chain of pages and add them to the tree
            long rightPageNum = node.pageHeader.getNextPage();
            while (rightPageNum != Page.NO_PAGE) {

                node = getBTreeNode(rightPageNum);

                rightPageNum = node.pageHeader.getNextPage();

                // promote first key of page to parent
                if (node.nKeys < 1) {
    //                throw new IOException("No keys found in page " + node.page.getPageNum());
                    continue;
                }
                final Value key = node.keys[0];
                final BTreeNode parent = findParent(key);
                if (parent == null) {
                    throw new IOException("Parent is null for page " + node.page.getPageNum());
                }
                if (parent.pageHeader.getType() != PageType.BRANCH) {
                    throw new IOException("Not a branch page: " + parent.page.getPageNum());
                }

                parent.promoteValue(null, key, node);
            }
        }
    }

    /**
     * Walk the tree to find the parent page to which key should
     * be promoted.
     *
     * @param key the key
     * @return the parent node
     * @throws IOException if an I/O error occurs
     */
    private BTreeNode findParent(final Value key) throws IOException {
        BTreeNode node = getRootNode();
        BTreeNode last = node;
        while (node.pageHeader.getType() != PageType.LEAF) {
            last = node;
            try {
                int idx = node.searchKey(key);
                idx = idx < 0 ? - (idx + 1) : idx + 1;
                node = node.getChildNode(idx);
            } catch (final Exception e) {
                e.printStackTrace();
                throw new IOException("Error while scanning page " + node.page.getPageNum());
            }
        }
        return last;
    }

    /* -------------------------------------------------------------------------
     * Methods used by recovery and transaction management
     * ---------------------------------------------------------------------- */

    private void writeToLog(final Loggable loggable, final BTreeNode node) {
        if(logManager != null) {
            try {
                logManager.journal(loggable);
                node.page.getPageHeader().setLsn(loggable.getLsn());
            } catch (final JournalException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    protected boolean requiresRedo(final Loggable loggable, final Page<PAGE_HEADER> page) {
        return loggable.getLsn().compareTo(page.getPageHeader().getLsn()) > 0;
    }

    protected void redoCreateBTNode(final CreateBTNodeLoggable loggable) throws LogException {
        BTreeNode node = cache.get(loggable.getPageNum());
        if (node == null) {
            // node is not yet loaded. Load it
            try {
                final Page<PAGE_HEADER> page = createPage(loggable.getPageNum());
                page.read(backingFile.randomAccessFile);
                if ((page.getPageHeader().getType() == PageType.BRANCH ||
                        page.getPageHeader().getType() == PageType.LEAF) &&
                        (!page.getPageHeader().getLsn().equals(Lsn.LSN_INVALID)) &&
                        !requiresRedo(loggable, page)) {
                    // node already found on disk: read it
                    node = createBTreeNode(page, false);
                    node.read();
                    return;
                } else {
                    // create a new node
                    node = createBTreeNode(page, true);
                    node.pageHeader.updateType(loggable.getPageType());
                    node.setPointers(new long[0]);
                    node.write();
                }
                node.pageHeader.setLsn(loggable.getLsn());
                node.pageHeader.setParentPage(loggable.getParentNum());
                final int increment = node.pageHeader.getType() == PageType.BRANCH ? 2 : 1;
                cache.add(node, increment);
            } catch (final IOException e) {
                throw new LogException(e.getMessage(), e);
            }
        }
    }

    protected void redoInsertValue(final InsertValueLoggable loggable) throws LogException {
        final BTreeNode node = getBTreeNode(loggable.pageNum);
        if (requiresRedo(loggable, node.page)) {
            node.insertKey(loggable.key, loggable.idx);
            node.insertPointer(loggable.pointer, loggable.pointerIdx);
            node.adjustDataLen(loggable.idx);
            node.pageHeader.setLsn(loggable.getLsn());
        }
    }

    protected void undoInsertValue(final InsertValueLoggable loggable) throws LogException {
        try {
            removeValue(null, loggable.key);
        } catch (final BTreeException | IOException e) {
            LOG.error("Failed to undo: {}", loggable.dump(), e);
        }
    }

    protected void redoUpdateValue(final UpdateValueLoggable loggable) throws LogException {
        final BTreeNode node = getBTreeNode(loggable.pageNum);
        if (!node.page.getPageHeader().getLsn().equals(Lsn.LSN_INVALID) && requiresRedo(loggable, node.page)) {
            if (loggable.idx > node.ptrs.length) {
                LOG.warn("{}; loggable.idx = {}; node.ptrs.length = {}", node.page.getPageInfo(), loggable.idx, node.ptrs.length);
                try (final StringBuilderWriter writer = new StringBuilderWriter()) {
                    dump(writer);
                    LOG.warn(writer.toString());
                } catch (final Exception e) {
                    LOG.warn(e);
                    e.printStackTrace();
                }
                throw new LogException("Critical error during recovery");
            }
            node.ptrs[loggable.idx] = loggable.pointer;
            node.pageHeader.setLsn(loggable.getLsn());
            node.setDirty(true);
        }
    }

    protected void undoUpdateValue(final UpdateValueLoggable loggable) throws LogException {
        try {
            addValue(null, loggable.key, loggable.oldPointer);
        } catch (final BTreeException | IOException e) {
            LOG.error("Failed to undo: {}", loggable.dump(), e);
        }
    }

    protected void redoRemoveValue(final RemoveValueLoggable loggable) throws LogException {
        final BTreeNode node = getBTreeNode(loggable.pageNum);
        if (!node.page.getPageHeader().getLsn().equals(Lsn.LSN_INVALID) && requiresRedo(loggable, node.page)) {
            node.removeKey(loggable.idx);
            node.removePointer(loggable.idx);
            node.recalculateDataLen();
            node.pageHeader.setLsn(loggable.getLsn());
        }
    }

    protected void undoRemoveValue(final RemoveValueLoggable loggable) throws LogException {
        try {
            addValue(null, loggable.oldValue, loggable.oldPointer);
        } catch (final BTreeException | IOException e) {
            LOG.error("Failed to undo: {}", loggable.dump(), e);
        }
    }

    protected void redoUpdatePage(final UpdatePageLoggable loggable) throws LogException {
        final BTreeNode node = getBTreeNode(loggable.pageNum);
        if (requiresRedo(loggable, node.page)) {
            node.prefix = loggable.prefix;
            node.keys = loggable.values;
            node.nKeys = loggable.values.length;
            node.pageHeader.updateValueCount((short) node.nKeys);
            node.setPointers(loggable.pointers);
            node.recalculateDataLen();
            node.pageHeader.setLsn(loggable.getLsn());
        }
    }

    protected void redoSetParent(final SetParentLoggable loggable) throws LogException {
            final BTreeNode node = getBTreeNode(loggable.pageNum);
        if (requiresRedo(loggable, node.page)) {
            node.pageHeader.setParentPage(loggable.parentNum);
            node.pageHeader.setLsn(loggable.getLsn());
            node.setDirty(true);
        }
    }

    protected void redoSetPageLink(final SetPageLinkLoggable loggable) throws LogException {
        final BTreeNode node = getBTreeNode(loggable.pageNum);
        if (requiresRedo(loggable, node.page)) {
            node.pageHeader.updateNextPage(loggable.nextPage);
            node.pageHeader.setLsn(loggable.getLsn());
            node.setDirty(true);
        }
    }

    /**
     * A node in the B+-tree. Every node is backed by a Page for
     * storing the node's data. Both, branch and leaf nodes are represented
     * by this class. Each node stores its keys as instances of {@link Value}
     * and its values as pointers of type long.
     * 
     *  If the node is a branch, the long pointers point to the child nodes
     *  of the branch. If it is a leaf, the pointers contain the virtual storage
     *  of the data section associated to the key.
     *  
     * @author wolf
     *
     */
    protected final class BTreeNode extends AbstractCacheable implements BTreeCacheable {

        /** defines the default size for the keys array */
        private final static int DEFAULT_INITIAL_ENTRIES = 32;
        /** the underlying Page object that stores the node's data */
        private final Page<PAGE_HEADER> page;
        private final PAGE_HEADER pageHeader;

        /** stores the keys in this page */
        private Value[] keys;

        private Value prefix = Value.EMPTY_VALUE;

        /** the number of keys currently stored */
        private int nKeys = 0;

        /** 
         * stores the page pointers to child nodes (for branches)
         * or the storage address (for leaf nodes).
         */
        private long[] ptrs;

        /** the number of pointers currently used */
        private int nPtrs = 0;

        /** the computed raw data size required by this node */
        private int currentDataLen = -1;

        private boolean allowUnload = true;

        public BTreeNode(final Page<PAGE_HEADER> page, final boolean newPage) {
            this.page = page;
            this.pageHeader = page.getPageHeader();
            if (newPage) {
                keys = new Value[DEFAULT_INITIAL_ENTRIES];
                ptrs = new long[DEFAULT_INITIAL_ENTRIES + 1];
                pageHeader.updateValueCount((short) 0);
                setDirty(true);
            }
        }

        /**
         * Set the link to the parent of this node.
         * 
         * @param parent the parent
         */
        public void setParent(final BTreeNode parent) {
            if (parent != null) {
                pageHeader.setParentPage(parent.page.getPageNum());
            } else {
                pageHeader.setParentPage(Page.NO_PAGE);
            }
            setDirty(true);
        }
		
        /**
         * Get the parent.
         *
         * @return the parent of this node.
         */
        public BTreeNode getParent() {
            if (pageHeader.getParentPage() != Page.NO_PAGE) {
                return getBTreeNode(pageHeader.getParentPage());
            } else {
                return null;
            }
        }

        @Override
        public boolean allowUnload() {
            return allowUnload;
        }

        @Override
        public boolean isInnerPage() {
            return pageHeader.getType() == PageType.BRANCH;
        }

        @Override
        public boolean sync(final boolean syncJournal) throws IOException {
            if (isDirty()) {
                write();
                if (isRecoveryEnabled() && syncJournal && logManager != null) {
                    logManager.flush(true, false);
                }
                return true;
            }
            return false;
        }

        @Override
        public long getKey() {
            return page.getPageNum();
        }

        /**
         * Set the keys of this node.
         * 
         * @param vals the values
         */
        private void setValues(final Value[] vals) {
            keys = vals;
            nKeys = vals.length;
            pageHeader.updateValueCount((short) nKeys);
            setDirty(true);
        }

        /**
         * Set the array of pointers of this node.
         * 
         * @param pointers the pointers
         */
        private void setPointers(final long[] pointers) {
            ptrs = pointers;
            nPtrs = pointers.length;
            setDirty(true);
        }

        /**
         * Returns the raw data size (in bytes) required by this node.
         * 
         * @return The data length
         */
        private int getDataLen() {
            return currentDataLen < 0 ?
                recalculateDataLen() : currentDataLen;
        }

        /**
         * Recalculates the raw data size (in bytes) required by this node.
         * 
         * @return the data length
         */
        private int recalculateDataLen() {
            currentDataLen = ptrs == null ? 0 : nPtrs * 8;
            if (fileHeader.getFixedKeyLen() < 0) {
                currentDataLen += 2 * nKeys;
            }
            if (pageHeader.getType() == PageType.BRANCH) {
                currentDataLen += prefix.getLength() + 2;
            }
            if (pageHeader.getType() == PageType.LEAF) {
                currentDataLen += nKeys - 1;
            }
            for (int i = 0; i < nKeys; i++) {
                if (pageHeader.getType() == PageType.LEAF && i > 0) {
                    // if this is a leaf page, we use prefix compression to store the keys,
                    // so subtract the size of the prefix
                    int prefix = keys[i].commonPrefix(keys[i - 1]);
                    if (prefix < 0 || prefix > Byte.MAX_VALUE) {
                        prefix = 0;
                    }
                    currentDataLen += keys[i].getLength() - prefix;
                } else {
                    currentDataLen += keys[i].getLength();
                }
            }
            return currentDataLen;
        }

        /**
         * Add the raw data size required to store the value to the internal
         * data size of this node.
         *
         * @param idx the index
         */
        private void adjustDataLen(final int idx) {
            if(currentDataLen < 0) {
                recalculateDataLen();
                return;
            }
            if (pageHeader.getType() == PageType.LEAF && idx > 0) {
                // if this is a leaf page, we use prefix compression to store the keys,
                // so subtract the size of the prefix
                int prefix;
                if (idx + 1< nKeys) {
                    // recalculate the prefix length for the following value
                    prefix = calculatePrefixLen(idx + 1, idx - 1);
                    currentDataLen -= keys[idx + 1].getLength() - prefix;
                    prefix = calculatePrefixLen(idx + 1, idx);
                    currentDataLen += keys[idx + 1].getLength() - prefix;
                }
                // calculate the prefix length for the new value
                prefix = calculatePrefixLen(idx, idx - 1);
                currentDataLen += keys[idx].getLength() - prefix;
                currentDataLen++; // add one byte for the prefix length
            } else {
                currentDataLen += keys[idx].getLength();
                if (pageHeader.getType() == PageType.LEAF) {
                    currentDataLen++;
                }
            }
            currentDataLen += 8;
            if (fileHeader.getFixedKeyLen() < 0) {
                currentDataLen += 2;
            }
        }

        private int calculatePrefixLen(final int idx0, final int idx1) {
            int prefix = keys[idx0].commonPrefix(keys[idx1]);
            if (prefix < 0 || prefix > Byte.MAX_VALUE) {
                prefix = 0;
            }
            return prefix;
        }

        /**
         * Compute where to split a page: tries to split at half the data size
         *
         * @param preferred preferred
         *
         * @return the pivot
         */
        private int getPivot(final int preferred) {
            if (nKeys == 2) {
                return 1;
            }
            final int totalLen = getKeyDataLen();
            int currentLen = 0;
            int pivot = nKeys - 1;
            for (int i = 0; i < nKeys - 1; i++) {
                if (pageHeader.getType() == PageType.LEAF && i > 0) {
                    // if this is a leaf page, we use prefix compression to store the keys,
                    // so subtract the size of the prefix
                    int prefix = keys[i].commonPrefix(keys[i - 1]);
                    if (prefix < 0 || prefix > Byte.MAX_VALUE) {
                        prefix = 0;
                    }
                    currentLen += keys[i].getLength() - prefix;
                } else {
                    currentLen += keys[i].getLength();
                }
                if (currentLen > totalLen / 2 || i + 1 == preferred) {
                    pivot = currentLen > getPageContentSize() ? i : i + 1;
                    break;
                }
            }
            return pivot;
        }

        private int getKeyDataLen() {
            int totalLen = 0;
            for (int i = 0; i < nKeys; i++) {
                if (pageHeader.getType() == PageType.LEAF && i > 0) {
                    // if this is a leaf page, we use prefix compression to store the keys,
                    // so subtract the size of the prefix
                    int prefix = keys[i].commonPrefix(keys[i - 1]);
                    if (prefix < 0 || prefix > Byte.MAX_VALUE) {
                        prefix = 0;
                    }
                    totalLen += keys[i].getLength() - prefix;
                } else {
                    totalLen += keys[i].getLength();
                }
            }
            return totalLen;
        }

        private boolean mustSplit() {
            if (pageHeader.getValueCount() != nKeys) {
                throw new RuntimeException("Wrong value count");
            }
            return getDataLen() > getPageContentSize();
        }

        /**
         * Read the node from the underlying page.
         *
         * @throws IOException if an I/O error occurs
         */
        private void read() throws IOException {
            final byte[] data = page.read(backingFile.randomAccessFile);
            final short keyLen = fileHeader.getFixedKeyLen();
            short valSize = keyLen;
            int p = 0;
            // it this is a branch node, read the common prefix
            if (pageHeader.getType() == PageType.BRANCH) {
                final short prefixSize = ByteConversion.byteToShort(data, p);
                p += 2;
                if (prefixSize == 0) {
                    prefix = Value.EMPTY_VALUE;
                } else {
                    prefix = new Value(data, p, prefixSize);
                    p += prefixSize;
                }
            }
            nKeys = pageHeader.getValueCount();
            keys = new Value[(nKeys * 3) / 2 + 1];
            for (int i = 0; i < nKeys; i++) {
                if (keyLen < 0) {
                    valSize = ByteConversion.byteToShort(data, p);
                    p += 2;
                }
                if (pageHeader.getType() == PageType.LEAF && i > 0) {
                    // for leaf pages, we use prefix compression to increase the number of
                    // keys that can be stored on one page. Each key is stored as follows:
                    // [valSize, prefixLen, value], where prefixLen specifies the number of
                    // leading bytes the key has in common with the previous key.
                    final int prefixLen = (data[p++] & 0xFF);
                    try {
                        final byte[] t = new byte[valSize];
                        if (prefixLen > 0) {
                            // copy prefixLen leading bytes from the previous key
                            System.arraycopy(keys[i - 1].data(), keys[i - 1].start(), t, 0, prefixLen);
                        }
                        // read the remaining bytes
                        System.arraycopy(data, p, t, prefixLen, valSize - prefixLen);
                        p += valSize - prefixLen;
                        keys[i] = new Value(t);
                    } catch (final Exception e) {
                        e.printStackTrace();
                        LOG.error("prefixLen = {}; i = {}; nKeys = {}", prefixLen, i, nKeys);
                        throw new IOException(e.getMessage());
                    }
                } else {
                    keys[i] = new Value(data, p, valSize);
                    p += valSize;
                }
            }
            //	Read in the pointers
            nPtrs = pageHeader.getPointerCount();
            ptrs = new long[(nPtrs * 3) / 2 + 1];
            for (int i = 0; i < nPtrs; i++) {
                ptrs[i] = ByteConversion.byteToLong(data, p);
                p += 8;
            }
        }

        /**
         * Write the node to the underlying page.
         *
         * @throws IOException if an I/O error occurs
         */
        private void write() throws IOException {
            if (nKeys != pageHeader.getValueCount()) {
                throw new IOException("nkeys: " + nKeys + " valueCount: " + pageHeader.getValueCount());
            }

            final byte[] temp = new byte[getPageContentSize()];
            int p = 0;

            // if this is a branch node, write out the common prefix
            if (pageHeader.getType() == PageType.BRANCH) {
                ByteConversion.shortToByte((short) prefix.getLength(), temp, p);
                p += 2;
                if (prefix.getLength() > 0) {
                    System.arraycopy(prefix.data(), prefix.start(), temp, p, prefix.getLength());
                    p += prefix.getLength();
                }
            }
            final int keyLen = fileHeader.getFixedKeyLen();
            for (int i = 0; i < nKeys; i++) {
                if (keyLen < 0) {
                    ByteConversion.shortToByte((short) keys[i].getLength(), temp, p);
                    p += 2;
                }
                if (pageHeader.getType() == PageType.LEAF && i > 0) {
                    // for leaf pages, we use prefix compression to increase the number of
                    // keys that can be stored on one page. Each key is stored as follows:
                    // [valSize, prefixLen, value], where prefixLen specifies the number of
                    // leading bytes the key has in common with the previous key.
                    int prefixLen = keys[i].commonPrefix(keys[i - 1]); // determine the common prefix
                    if (prefixLen < 0 || prefixLen > Byte.MAX_VALUE)
                        {prefixLen = 0;}
                    // store the length of the prefix
                    temp[p++] = (byte) prefixLen;
                    // copy the remaining bytes, starting at prefixLen
                    System.arraycopy(keys[i].data(), keys[i].start() + prefixLen, 
                            temp, p, keys[i].getLength() - prefixLen);
                    p += keys[i].getLength() - prefixLen;
                } else {
                    final byte[] data = keys[i].getData();
                    if(p + data.length > temp.length) {
                        throw new IOException("calculated: " + getDataLen() + "; required: " + (p + data.length));
                    }
                    System.arraycopy(data, 0, temp, p, data.length);
                    p += data.length;
                }
            }
            for (int i = 0; i < nPtrs; i++) {
                ByteConversion.longToByte(ptrs[i], temp, p);
                p += 8;
            }
            writeValue(page, new Value(temp));
            setDirty(false);
        }

        /**
         * Retrieve the child node at guven index.
         * 
         * @param idx The index
         * @return The BTree node
         * @throws IOException if an I/O error occurs
         */
        private BTreeNode getChildNode(final int idx) throws IOException {
            if (pageHeader.getType() == PageType.BRANCH && idx >= 0 && idx < nPtrs) {
                return getBTreeNode(ptrs[idx]);
            } else {
                return null;
            }
        }

        /**
         * Remove a key.
         *
         * @param transaction the database transaction
         * @param key the key
         *
         * @throws IOException if an I/O error occurs
         * @throws DBException if an error occurs with the tree
         */
        private long removeValue(final Txn transaction, final Value key) throws IOException, BTreeException {
            int idx = searchKey(key);
            switch (pageHeader.getType()) {
                case BRANCH :
                    idx = idx < 0 ? - (idx + 1) : idx + 1;
                    return getChildNode(idx).removeValue(transaction, key);

                case LEAF :
                    if (idx < 0) {
                        return KEY_NOT_FOUND;
                    }
                    else {
                        try {
                            allowUnload = false;
                            if (transaction != null && isRecoveryEnabled()) {
                                final RemoveValueLoggable log = new RemoveValueLoggable(transaction,
                                    fileId, page.getPageNum(), idx, keys[idx], ptrs[idx]);
                                writeToLog(log, this);
                            }
                            final long oldPtr = ptrs[idx];
                            removeKey(idx);
                            removePointer(idx);
                            recalculateDataLen();
                            return oldPtr;
                        } finally {
                            allowUnload = true;
                        }
                    }

                default :
                    throw new BTreeException("Invalid Page Type In removeValue");
            }
        }

        /**
         * Add a key and the corresponding pointer to the node.
         *
         * @param transaction the database transaction
         * @param value the value
         * @param pointer the pointer to the node
         *
         * @return the new storage address
         */
        private long addValue(final Txn transaction, final Value value, final long pointer)
                throws IOException, BTreeException {
            if (value == null) {
                return -1;
            }

            int idx = searchKey(value);
            switch (pageHeader.getType()) {
                case BRANCH :
                    idx = idx < 0 ? - (idx + 1) : idx + 1;
                    return getChildNode(idx).addValue(transaction, value, pointer);

                case LEAF :
                    try {
                        allowUnload = false;
                        if (idx >= 0) {
                            // Value was found... Overwrite
                            final long oldPtr = ptrs[idx];
                            if (transaction != null && isRecoveryEnabled()) {
                                final UpdateValueLoggable loggable = new UpdateValueLoggable(transaction,
                                    fileId, page.getPageNum(), idx, value, pointer, oldPtr);
                                    writeToLog(loggable, this);
                            }
                            ptrs[idx] = pointer;
                            setDirty(true);
                            return oldPtr;
                        } else {
                            // Value was not found
                            idx = - (idx + 1);
                            if (transaction != null && isRecoveryEnabled()) {
                                final InsertValueLoggable loggable = new InsertValueLoggable(transaction,
                                    fileId, page.getPageNum(), idx, value, idx, pointer);
                                    writeToLog(loggable, this);
                                }
                                insertKey(value, idx);
                                insertPointer(pointer, idx);
                                adjustDataLen(idx);
                                if (mustSplit()) {
                                    // we normally split a node at its median value.
                                    // however, if the inserted key is in the upper or lower
                                    // section of the node, we split directly at the key. this
                                    // has advantages if keys are inserted in ascending order
                                    if (splitFactor > 0 && idx > (nKeys * splitFactor) && value.getLength() < getPageContentSize() / 4) {
                                        split(transaction, idx == 0 ? 1 : idx);
                                    } else {
                                        split(transaction);
                                    }
                                }
                        }
                    } finally {
                        allowUnload = true;
                    }
                    return -1;

                default :
                    throw new BTreeException("Invalid Page Type In addValue: " +
                        pageHeader.getType() + "; " + page.getPageInfo());
            }
        }

        /**
         * Promote a key to the parent node. Called by {@link #split(Txn)}.
         *
         * @param transaction the database transaction
         * @param value the value
         * @param rightNode the right-most node
         */
        private void promoteValue(final Txn transaction, final Value value, final BTreeNode rightNode)
                throws IOException, BTreeException {
            int idx = searchKey(value);
            idx = idx < 0 ? -( idx + 1) : idx + 1;
            insertKey(value, idx);
            insertPointer(rightNode.page.getPageNum(), idx + 1);
            rightNode.setParent(this);
            rightNode.setDirty(true);
            cache.add(rightNode);
            setDirty(true);
            cache.add(this);
            final boolean split = recalculateDataLen() > getPageContentSize();
            if (split) {
                split(transaction);
            }
        }

        private void split(final Txn transaction) throws IOException, BTreeException {
            split(transaction, -1);
        }

        /**
         * Split the node.
         *
         * @param transaction the current transaction
         * @param pivot the pivot value
         *
         * @throws IOException if an I/O error occurs
         * @throws BTreeException if an error occurss with the tree
         */
        private void split(final Txn transaction, int pivot) throws IOException, BTreeException {
            final Value[] leftVals;
            final Value[] rightVals;
            final long[] leftPtrs;
            final long[] rightPtrs;
            Value separator;
            final short vc = pageHeader.getValueCount();
            pivot = getPivot(pivot);

            // Split the node into two nodes
            switch (pageHeader.getType()) {
                case BRANCH :
                    leftVals = new Value[pivot];
                    leftPtrs = new long[leftVals.length + 1];
                    rightVals = new Value[vc - (pivot + 1)];
                    rightPtrs = new long[rightVals.length + 1];
                    System.arraycopy(keys, 0, leftVals, 0, leftVals.length);
                    System.arraycopy(ptrs, 0, leftPtrs, 0, leftPtrs.length);
                    System.arraycopy(keys, leftVals.length + 1, rightVals, 0, rightVals.length);
                    System.arraycopy(ptrs, leftPtrs.length, rightPtrs, 0, rightPtrs.length);
                    separator = keys[leftVals.length];
                    if (prefix != null && prefix.getLength() > 0) {
                        final byte[] t = new byte[prefix.getLength() + separator.getLength()];
                        System.arraycopy(prefix.data(), prefix.start(), t, 0, prefix.getLength());
                        System.arraycopy(separator.data(), separator.start(), t, prefix.getLength(), separator.getLength());
                        separator = new Value(t);
                    }
                    break;

                case LEAF :
                    leftVals = new Value[pivot];
                    leftPtrs = new long[leftVals.length];
                    rightVals = new Value[vc - pivot];
                    rightPtrs = new long[rightVals.length];
                    System.arraycopy(keys, 0, leftVals, 0, leftVals.length);
                    System.arraycopy(ptrs, 0, leftPtrs, 0, leftPtrs.length);
                    System.arraycopy(keys, leftVals.length, rightVals, 0, rightVals.length);
                    System.arraycopy(ptrs, leftPtrs.length, rightPtrs, 0, rightPtrs.length);
                    separator = keys[leftVals.length];
                    break;

                default :
                    throw new BTreeException("Invalid Page Type In split");
            }

            // Log the update of the current page
            if (transaction != null && isRecoveryEnabled() && pageHeader.getType() == PageType.LEAF) {
                final Loggable log = new UpdatePageLoggable(transaction, fileId,
                    page.getPageNum(), prefix, leftVals, leftVals.length, leftPtrs, leftPtrs.length);
                writeToLog(log, this);
            }
            setValues(leftVals);
            setPointers(leftPtrs);
            recalculateDataLen();
            // Promote the pivot to the parent branch
            BTreeNode parent = getParent();
            if (parent == null) {
                // This can only happen if this is the root
                parent = createBTreeNode(transaction, PageType.BRANCH, null, false);
                // Log change of the parent page
                if (transaction != null && isRecoveryEnabled() && pageHeader.getType() == PageType.LEAF) {
                    final Loggable log = new SetParentLoggable(transaction, fileId, page.getPageNum(), 
                        parent.page.getPageNum());
                    writeToLog(log, this);
                }
                setParent(parent);
                final BTreeNode rNode = createBTreeNode(transaction, pageHeader.getType(), parent, false);
                rNode.setValues(rightVals);
                rNode.setPointers(rightPtrs);
                rNode.setAsParent();
                if (pageHeader.getType() == PageType.BRANCH) {
                    rNode.prefix = prefix;
                    rNode.growPrefix();
                } else {
                    if (transaction != null && isRecoveryEnabled()) {
                        final Loggable log = new SetPageLinkLoggable(transaction, 
                            fileId, page.getPageNum(), rNode.page.getPageNum());
                        writeToLog(log, this);
                    }
                    pageHeader.updateNextPage(rNode.page.getPageNum());
                }
                // Log update of the right node
                if (transaction != null && isRecoveryEnabled() && pageHeader.getType() == PageType.LEAF) {
                    final Loggable log = new UpdatePageLoggable(transaction, fileId,
                        rNode.page.getPageNum(), rNode.prefix, rNode.keys, rNode.nKeys, rightPtrs, rightPtrs.length);
                    writeToLog(log, rNode);
                }
                rNode.recalculateDataLen();
                parent.prefix = separator;
                parent.setValues(new Value[] { Value.EMPTY_VALUE });
                parent.setPointers(new long[] { page.getPageNum(), rNode.page.getPageNum()});
                parent.recalculateDataLen();
                cache.add(parent);
                setRootNode(parent);
                if(rNode.mustSplit()) {
                    LOG.debug("{} right node requires second split: {}", FileUtils.fileName(getFile()), rNode.getDataLen());
                    rNode.split(transaction);
                }
                cache.add(rNode);
            } else {
                final BTreeNode rNode = createBTreeNode(transaction, pageHeader.getType(), parent, false);
                rNode.setValues(rightVals);
                rNode.setPointers(rightPtrs);
                rNode.setAsParent();
                if (pageHeader.getType() == PageType.BRANCH) {
                    rNode.prefix = prefix;
                    rNode.growPrefix();
                } else {
                    if (transaction != null && isRecoveryEnabled()) {
                        Loggable log = new SetPageLinkLoggable(transaction, fileId, 
                            rNode.page.getPageNum(), pageHeader.getNextPage());
                        writeToLog(log, this);
                        log = new SetPageLinkLoggable(transaction, fileId, 
                            page.getPageNum(), rNode.page.getPageNum());
                        writeToLog(log, this);
                    }
                    rNode.pageHeader.updateNextPage(pageHeader.getNextPage());
                    pageHeader.updateNextPage(rNode.page.getPageNum());
                }
                // Log update of the right node
                if (transaction != null && isRecoveryEnabled() && pageHeader.getType() == PageType.LEAF) {
                    final Loggable log = new UpdatePageLoggable(transaction, fileId, 
                        rNode.page.getPageNum(), rNode.prefix, rNode.keys,
                        rNode.nKeys, rightPtrs, rightPtrs.length);
                    writeToLog(log, rNode);
                }
                rNode.recalculateDataLen();
                if(rNode.mustSplit()) {
                    LOG.debug("{} right node requires second split: {}", FileUtils.fileName(getFile()), rNode.getDataLen());
                    rNode.split(transaction);
                }
                cache.add(rNode);
                parent.promoteValue(transaction, separator, rNode);
            }
            cache.add(this);
            if (mustSplit()) {
                LOG.debug("{}left node requires second split: {}", FileUtils.fileName(getFile()), getDataLen());
                split(transaction);
            }
        }

        /**
         * Set the parent-link in all child nodes to point to this node
         */
        private void setAsParent() throws IOException {
            if (pageHeader.getType() == PageType.BRANCH) {
                for (int i = 0; i < nPtrs; i++) {
                    final BTreeNode node = getBTreeNode(ptrs[i]);
                    node.setParent(this);
                    cache.add(node);
                }
            }
        }

        /**
         * Locate the given value in the keys and return the
         * associated pointer.
         *
         * @param value the value
         *
         * @return the address of the value
         *
         * @throws IOException if an I/O error occurs
         * @throws BTreeException if an error occurs with the tree
         */
        private long findValue(final Value value) throws IOException, BTreeException {
            int idx = searchKey(value);
            switch (pageHeader.getType()) {

                case BRANCH :
                    idx = idx < 0 ? - (idx + 1) : idx + 1;
                    final BTreeNode child = getChildNode(idx);
                    if (child == null) {throw new BTreeException("Unexpected " + idx + ", " +
                            page.getPageNum() + ": value '" + value.toString() + "' doesn't exist");
                    }
                    return child.findValue(value);

                case LEAF :
                    if (idx < 0) {
                        return KEY_NOT_FOUND;
                    } else {
                        return ptrs[idx];
                    }

                default :
                    throw new BTreeException("Invalid Page Type In findValue");
            }
        }

        @Override
        public String toString() {
            final StringBuilderWriter writer = new StringBuilderWriter();
            try {
                dump(writer);
            } catch (final Exception e) {
                LOG.error(e);
                //TODO : add something here ! -pb
            }
            return writer.toString();
        }

        private void treeStatistics(final TreeMetrics metrics) throws IOException {
            metrics.addPage(pageHeader.getType());
            if (pageHeader.getType() == PageType.BRANCH) {
                for (int i = 0; i < nPtrs; i++) {
                    final BTreeNode child = getChildNode(i);
                    child.treeStatistics(metrics);
                }
            }
        }

        /**
         * Prints out a debug view of the node to the given writer.
         *
         * @param writer the writer
         *
         * @throws IOException if an I/O error occurs
         * @throws BTreeException if an error occurs with the tree
         */
        private void dump(final Writer writer) throws IOException, BTreeException {
            //if (pageHeader.getStatus() == LEAF)
            //    return;
            final int workSize;
            final int keyLen;
            final ReentrantReadWriteLock.ReadLock fileHeaderReadLock = fileHeader.readLock();
            try {
                if (page.getPageNum() == fileHeader.getRootPage()) {
                    writer.write("ROOT: ");
                }
            } finally {
                fileHeaderReadLock.unlock();
            }
            writer.write(page.getPageNum() + ": ");
            writer.write(pageHeader.getType() == PageType.BRANCH ? "BRANCH: " : "LEAF: ");
            writer.write(isDirty() ? "DIRTY: " : "SAVED: ");
            if (pageHeader.getType() == PageType.BRANCH) {
                writer.write("PREFIX: ");
                dumpValue(writer, prefix, pageHeader.getType());
                writer.write(": ");
            }
            writer.write("NEXT: ");
            writer.write(Long.toString(pageHeader.getNextPage()));
            writer.write(": ");
            for (int i = 0; i < nKeys; i++) {
                if (i > 0) {
                    writer.write(' ');
                }
                dumpValue(writer, keys[i], pageHeader.getType());
            }
            writer.write('\n');
            if (pageHeader.getType() == PageType.BRANCH) {
                writer.write("-----------------------------------------------------------------------------------------\n");
                writer.write(page.getPageNum() + " POINTERS: ");
                for (int i = 0; i < nPtrs; i++) {
                    writer.write(ptrs[i] + " ");
                }
                writer.write('\n');
            }
            writer.write("-----------------------------------------------------------------------------------------\n");
            if (pageHeader.getType() == PageType.BRANCH) {
                for (int i = 0; i < nPtrs; i++) {
                    final BTreeNode child = getChildNode(i);
                    child.dump(writer);
                }
            }
        }

        /**
         * Search for keys matching the given {@link IndexQuery} and
         * report the to the specified {@link BTreeCallback}.
         * 
         * @param query the query
         * @param callback the callback
         * @throws IOException if an I/O error occurs
         * @throws BTreeException if an error occurs with the tree
         * @throws TerminatedException if the callback is terminated
         */
        private void query(final IndexQuery query, final BTreeCallback callback)
                throws IOException, BTreeException, TerminatedException {
            if (query != null
                    && query.getOperator() != IndexQuery.ANY
                    && query.getOperator() != IndexQuery.TRUNC_LEFT) {
                final Value[] qvals = query.getValues();
                int leftIdx = searchKey(qvals[0]);
                int rightIdx = qvals.length > 1 ?
                    searchKey(qvals[qvals.length - 1]) : leftIdx;
                    final boolean pos = query.getOperator() >= 0;
                    switch (pageHeader.getType()) {
                        case BRANCH :
                            leftIdx = leftIdx < 0 ? - (leftIdx + 1) : leftIdx + 1;
                            rightIdx = rightIdx < 0 ? - (rightIdx + 1) : rightIdx + 1;
                            switch (query.getOperator()) {
                                case IndexQuery.BWX :
                                case IndexQuery.NBWX :
                                case IndexQuery.BW :
                                case IndexQuery.NBW :
                                case IndexQuery.IN :
                                case IndexQuery.NIN :
                                case IndexQuery.TRUNC_RIGHT :
                                case IndexQuery.RANGE :
                                    for (int i = 0; i < nPtrs; i++)
                                        if ((i >= leftIdx && i <= rightIdx) == pos) {
                                            getChildNode(i).query(query, callback);
                                            if (query.getOperator() == IndexQuery.TRUNC_RIGHT ||
                                                query.getOperator() == IndexQuery.RANGE) {

                                                break;
                                            }
                                        }
                                    break;
                                case IndexQuery.NEQ :
                                    getChildNode(0).query(query, callback);
                                    break;
                                case IndexQuery.EQ :
                                    getChildNode(leftIdx).query(query, callback);
                                    break;
                                case IndexQuery.LT :
                                    for (int i = 0; i < nPtrs; i++) {
                                        if ((pos && (i <= leftIdx)) || (!pos && (i >= leftIdx))) {
                                            getChildNode(i).query(query, callback);
                                        }
                                    }
                                    break;
                                case IndexQuery.GEQ :
                                case IndexQuery.GT :
                                    getChildNode(leftIdx).query(query, callback);
                                    break;
                                case IndexQuery.LEQ :
                                    for (int i = 0; i < nPtrs; i++) {
                                        if ((pos && (i >= leftIdx)) || (!pos && (i <= leftIdx))) {
                                            getChildNode(i).query(query, callback);
                                        }
                                    }
                                    break;
                                default :
                                    // If it's not implemented, we walk the tree
                                    for (int i = 0; i < nPtrs; i++) {
                                        getChildNode(i).query(query, callback);
                                    }
                                    break;
                            }
                            break;
                        case LEAF :
                            switch (query.getOperator()) {
                                case IndexQuery.EQ :
                                    if (leftIdx >= 0) {
                                        callback.indexInfo(keys[leftIdx], ptrs[leftIdx]);
                                    }
                                    break;
                                case IndexQuery.NEQ :
                                    for (int i = 0; i < nPtrs; i++) {
                                        if (i != leftIdx) {
                                            callback.indexInfo(keys[i], ptrs[i]);
                                        }
                                    }
                                    scanNextPage(query, null, callback);
                                    break;
                                case IndexQuery.BWX :
                                case IndexQuery.NBWX :
                                case IndexQuery.BW :
                                case IndexQuery.NBW :
                                    if (leftIdx < 0) {
                                        leftIdx = - (leftIdx + 1);
                                    }
                                    if (rightIdx < 0) {
                                        rightIdx = - (rightIdx + 1);
                                    }
                                    for (int i = 0; i < nPtrs; i++) {
                                        if ((pos && (i >= leftIdx && i <= rightIdx))
                                            || (!pos && (i <= leftIdx || i >= rightIdx))) {
                                            if (query.testValue(keys[i])) {
                                                callback.indexInfo(keys[i], ptrs[i]);
                                            }
                                        }
                                    }
                                    break;
                                case IndexQuery.RANGE :
                                case IndexQuery.TRUNC_RIGHT :
                                    if (leftIdx < 0) {
                                        leftIdx = - (leftIdx + 1);
                                    }
                                    if (rightIdx < 0) {
                                        rightIdx = - (rightIdx + 1);
                                    }
                                    for (int i = leftIdx; i < rightIdx && i < nPtrs; i++) {
                                        if (query.testValue(keys[i])) {
                                            callback.indexInfo(keys[i], ptrs[i]);
                                        }
                                    }
                                    if (rightIdx >= nPtrs) {
                                        scanNextPage(query, null, callback);
                                    }
                                    break;
                                case IndexQuery.IN :
                                case IndexQuery.NIN :
                                    if (leftIdx < 0) {
                                        leftIdx = - (leftIdx + 1);
                                    }
                                    if (rightIdx < 0) {
                                        rightIdx = - (rightIdx + 1);
                                    }
                                    for (int i = 0; i < nPtrs; i++) {
                                        if (!pos || (i >= leftIdx && i <= rightIdx)) {
                                            if (query.testValue(keys[i])) {
                                                callback.indexInfo(keys[i], ptrs[i]);
                                            }
                                        }
                                    }
                                    break;
                                case IndexQuery.LT :
                                    if (leftIdx < 0) {
                                        leftIdx = - (leftIdx + 1);
                                    }
                                    for (int i = 0; i < nPtrs; i++) {
                                        if ((pos && (i <= leftIdx)) || (!pos && (i >= leftIdx))) {
                                            if (query.testValue(keys[i])) {
                                                callback.indexInfo(keys[i], ptrs[i]);
                                            }
                                        }
                                    }
                                    break;
                                case IndexQuery.GEQ :
                                case IndexQuery.GT :
                                    if (leftIdx < 0) {
                                        leftIdx = - (leftIdx + 1);
                                    }
                                    for (int i = leftIdx; i < nPtrs; i++) {
                                        if (query.testValue(keys[i])) {
                                            callback.indexInfo(keys[i], ptrs[i]);
                                        }
                                    }
                                    scanNextPage(query, null, callback);
                                    break;
                                case IndexQuery.LEQ :
                                    if (leftIdx < 0) {
                                        leftIdx = - (leftIdx + 1);
                                    }
                                    for (int i = 0; i < nPtrs; i++) {
                                        if ((pos && (i >= leftIdx)) || (!pos && (i <= leftIdx))) {
                                            if (query.testValue(keys[i])) {
                                                callback.indexInfo(keys[i], ptrs[i]);
                                            } else if (query.getOperator() == IndexQuery.TRUNC_RIGHT) {
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                default :
                                    // If it's not implemented, it falls right through
                                    for (int i = 0; i < nPtrs; i++) {
                                        if (query.testValue(keys[i])) {
                                            callback.indexInfo(keys[i], ptrs[i]);
                                        }
                                    }
                                    break;
                            }
                            break;
                        default :
                            throw new BTreeException("Invalid Page Type In query");
                    }
            } else {
                // No Query - Just Walk The Tree
                switch (pageHeader.getType()) {
                case BRANCH :
                    for (int i = 0; i < nPtrs; i++) {
                        getChildNode(i).query(query, callback);
                    }
                    break;
                case LEAF :
                    for (int i = 0; i < nKeys; i++) {
                        if (query == null || query.getOperator() != IndexQuery.TRUNC_LEFT
                                || query.testValue(keys[i])) {
                            callback.indexInfo(keys[i], ptrs[i]);
                        }
                    }
                    break;
                default :
                    throw new BTreeException("Invalid Page Type In query");
                }
            }
        }

        /**
         * Search for keys matching the given {@link IndexQuery} and
         * report the to the specified {@link BTreeCallback}. This specialized
         * method only considers keys whose value starts with the specified keyPrefix.
         * 
         * @param query the query
         * @param keyPrefix the key prefix
         * @param callback the callback
         * @throws IOException if an I/O error occurs
         * @throws BTreeException if an error occurs with the tree
         * @throws TerminatedException if the callback is terminated
         */
        private void query(final IndexQuery query, final Value keyPrefix, final BTreeCallback callback)
                throws IOException, BTreeException, TerminatedException {
            if (query != null
                    && query.getOperator() != IndexQuery.ANY
                    && query.getOperator() != IndexQuery.TRUNC_LEFT) {
                final Value[] qvals = query.getValues();
                int leftIdx = searchKey(qvals[0]);
                int pfxIdx = searchKey(keyPrefix);
                switch (pageHeader.getType()) {
                    case BRANCH :
                        leftIdx = leftIdx < 0 ? - (leftIdx + 1) : leftIdx + 1;
                        pfxIdx = pfxIdx < 0 ? - (pfxIdx + 1) : pfxIdx + 1;
                        switch (query.getOperator()) {
                            case IndexQuery.EQ :
                                getChildNode(leftIdx).query(query, keyPrefix, callback);
                                break;
                            case IndexQuery.NEQ :
                                getChildNode(pfxIdx).query(query, keyPrefix, callback);
                                break;
                            case IndexQuery.LT :
                                for (int i = pfxIdx; i <= leftIdx && i < nPtrs; i++) {
                                    getChildNode(i).query(query, keyPrefix, callback);
                                }
                                break;
                            case IndexQuery.LEQ :
                                for (int i = pfxIdx; i <= leftIdx && i < nPtrs; i++) {
                                    getChildNode(i).query(query, keyPrefix, callback);
                                }
                                break;
                            case IndexQuery.GEQ :
                            case IndexQuery.GT :
                                getChildNode(leftIdx).query(query, keyPrefix, callback);
                                break;
                        }
                        break;
                    case LEAF :
                        pfxIdx = pfxIdx < 0 ? - (pfxIdx + 1) : pfxIdx + 1;
                        switch (query.getOperator()) {
                            case IndexQuery.EQ :
                                if (leftIdx >= 0) {
                                    callback.indexInfo(keys[leftIdx], ptrs[leftIdx]);
                                }
                                break;
                            case IndexQuery.NEQ :
                                for (int i = pfxIdx; i < nPtrs; i++) {
                                    if (keys[i].comparePrefix(keyPrefix) > 0) {
                                        break;
                                    }
                                    if (i != leftIdx) {
                                        callback.indexInfo(keys[i], ptrs[i]);
                                    }
                                }
                                scanNextPage(query, keyPrefix, callback);
                                break;
                            case IndexQuery.LT :
                                if (leftIdx < 0) {
                                    leftIdx = - (leftIdx + 1);
                                }
                                for (int i = pfxIdx; i < leftIdx; i++) {
                                    if (query.testValue(keys[i])) {
                                        callback.indexInfo(keys[i], ptrs[i]);
                                    }
                                }
                                break;
                            case IndexQuery.LEQ :
                                if (leftIdx < 0) {
                                    leftIdx = - (leftIdx + 1);
                                }
                                for (int i = pfxIdx; i <= leftIdx && i < nPtrs; i++) {
                                    if (query.testValue(keys[i])) {
                                        callback.indexInfo(keys[i], ptrs[i]);
                                    }
                                }
                                break;
                            case IndexQuery.GT :
                            case IndexQuery.GEQ :
                                if (leftIdx < 0) {
                                    leftIdx = - (leftIdx + 1);
                                }
                                for (int i = leftIdx; i < nPtrs; i++) {
                                    if (keys[i].comparePrefix(keyPrefix) > 0) {
                                        return;
                                    }
                                    if (query.testValue(keys[i])) {
                                        callback.indexInfo(keys[i], ptrs[i]);
                                    }
                                }
                                scanNextPage(query, keyPrefix, callback);
                                break;
                        }
                        break;
                    default :
                        throw new BTreeException("Invalid Page Type In query");
                }
            } else {
                // No Query - Just Walk The Tree
                switch (pageHeader.getType()) {
                    case BRANCH :
                        for (int i = 0; i < nPtrs; i++) {
                            getChildNode(i).query(query, callback);
                        }
                        break;
                    case LEAF :
                        for (int i = 0; i < nKeys; i++) {
                            if (query.getOperator() != IndexQuery.TRUNC_LEFT || query.testValue(keys[i])) {
                                callback.indexInfo(keys[i], ptrs[i]);
                            }
                        }
                        break;
                    default :
                        throw new BTreeException("Invalid Page Type In query");
                }
            }
        }

        protected void scanRaw(final IndexQuery query, final BTreeCallback callback) throws TerminatedException {
            for (int i = 0; i < nKeys; i++) {
                if (query == null || query.testValue(keys[i])) {
                    callback.indexInfo(keys[i], ptrs[i]);
                }
            }
        }

        protected void scanNextPage(final IndexQuery query, final Value keyPrefix, final BTreeCallback callback) throws TerminatedException {
            final long next = pageHeader.getNextPage();
            if (next != Page.NO_PAGE) {
                final BTreeNode nextPage = getBTreeNode(next);
                scanSequential(nextPage, query, keyPrefix, callback);
            }
        }

        /**
         * Search for keys matching the given {@link IndexQuery} and
         * remove them from the node. Every match is reported 
         * to the specified {@link BTreeCallback}.
         *
         * @param transaction the database transaction
         * @param query the query
         * @param callback the callback
         *
         * @throws IOException if an I/O error occurs
         * @throws BTreeException if an error occurs with the tree
         * @throws TerminatedException if the callback is terminated
         */
        private void remove(final Txn transaction, final IndexQuery query, final BTreeCallback callback)
                throws IOException, BTreeException, TerminatedException {
            if (query != null && query.getOperator() != IndexQuery.ANY
                    && query.getOperator() != IndexQuery.TRUNC_LEFT) {
                final Value[] qvals = query.getValues();
                int leftIdx = searchKey(qvals[0]);
                int rightIdx = qvals.length > 1 ? 
                    searchKey(qvals[qvals.length - 1]) : leftIdx;
                final boolean pos = query.getOperator() >= 0;
                switch (pageHeader.getType()) {
                    case BRANCH :
                        leftIdx = leftIdx < 0 ? - (leftIdx + 1) : leftIdx + 1;
                        rightIdx = rightIdx < 0 ? - (rightIdx + 1) : rightIdx + 1;
                        switch (query.getOperator()) {
                            case IndexQuery.BWX :
                            case IndexQuery.NBWX :
                            case IndexQuery.BW :
                            case IndexQuery.NBW :
                            case IndexQuery.IN :
                            case IndexQuery.NIN :
                            case IndexQuery.TRUNC_RIGHT :
                            case IndexQuery.RANGE :
                                for (int i = 0; i < nPtrs; i++) {
                                    if ((i >= leftIdx && i <= rightIdx) == pos) {
                                        getChildNode(i).remove(transaction, query, callback);
                                        if (query.getOperator() == IndexQuery.TRUNC_RIGHT) {
                                            break;
                                        }
                                    }
                                }
                                break;
                            case IndexQuery.EQ :
                            case IndexQuery.NEQ :
                                for (int i = 0; i < nPtrs; i++) {
                                    if (!pos || i == leftIdx) {
                                        getChildNode(i).remove(transaction, query, callback);
                                    }
                                }
                            case IndexQuery.LT :
                            case IndexQuery.GEQ :
                                for (int i = 0; i < nPtrs; i++){
                                    if ((pos && (i <= leftIdx)) || (!pos && (i >= leftIdx))) {
                                        getChildNode(i).remove(transaction, query, callback);
                                    }
                                }
                                break;
                            case IndexQuery.GT :
                            case IndexQuery.LEQ :
                                for (int i = 0; i < nPtrs; i++) {
                                    if ((pos && (i >= leftIdx)) || (!pos && (i <= leftIdx))) {
                                        getChildNode(i).remove(transaction, query, callback);
                                    }
                                }
                                break;
                            default :
                                // If it's not implemented, we walk the tree
                                for (int i = 0; i < nPtrs; i++) {
                                    getChildNode(i).remove(transaction, query, callback);
                                }
                                break;
                        }
                        break;
                    case LEAF :
                        try {
                            allowUnload = false;
                            switch (query.getOperator()) {
                            case IndexQuery.EQ :
                                if (leftIdx >= 0) {
                                    if (transaction != null && isRecoveryEnabled()) {
                                        final RemoveValueLoggable log =  new RemoveValueLoggable(transaction,
                                            fileId, page.getPageNum(), leftIdx, keys[leftIdx], ptrs[leftIdx]);
                                        writeToLog(log, this);
                                    }
                                    if (callback != null) {
                                        callback.indexInfo(keys[leftIdx], ptrs[leftIdx]);
                                    }
                                    removeKey(leftIdx);
                                    removePointer(leftIdx);
                                    recalculateDataLen();
                                }
                                break;
                            case IndexQuery.NEQ :
                                for (int i = 0; i < nPtrs; i++) {
                                    if (i != leftIdx) {
                                        if (transaction != null && isRecoveryEnabled()) {
                                            final RemoveValueLoggable log = new RemoveValueLoggable(transaction,
                                                fileId, page.getPageNum(), i, keys[i], ptrs[i]);
                                            writeToLog(log, this);
                                        }
                                        if (callback != null) {
                                            callback.indexInfo(keys[i], ptrs[i]);
                                        }
                                        removeKey(i);
                                        removePointer(i);
                                        recalculateDataLen();
                                    }
                                }
                                break;
                            case IndexQuery.BWX :
                            case IndexQuery.NBWX :
                            case IndexQuery.BW :
                            case IndexQuery.NBW :
                            case IndexQuery.RANGE :
                                if (leftIdx < 0) {
                                    leftIdx = - (leftIdx + 1);
                                }
                                if (rightIdx < 0) {
                                    rightIdx = - (rightIdx + 1);
                                }
                                for (int i = 0; i < nPtrs; i++) {
                                    if ((pos && (i >= leftIdx && i <= rightIdx))
                                            || (!pos && (i <= leftIdx || i >= rightIdx))) {
                                        if (query.testValue(keys[i])) {
                                            if (transaction != null && isRecoveryEnabled()) {
                                                final RemoveValueLoggable log = new RemoveValueLoggable(transaction,
                                                    fileId, page.getPageNum(), i, keys[i], ptrs[i]);
                                                writeToLog(log, this);
                                            }
                                            if (callback != null) {
                                                callback.indexInfo(keys[i], ptrs[i]);
                                            }
                                            removeKey(i);
                                            removePointer(i);
                                            recalculateDataLen();
                                            --i;
                                        }
                                    }
                                }
                                break;
                            case IndexQuery.TRUNC_RIGHT :
                                if (leftIdx < 0) {
                                    leftIdx = - (leftIdx + 1);
                                }
                                if (rightIdx < 0) {
                                    rightIdx = - (rightIdx + 1);
                                }
                                for (int i = leftIdx; i < rightIdx && i < nPtrs; i++) {
                                    if (query.testValue(keys[i])) {
                                        if (transaction != null && isRecoveryEnabled()) {
                                            final RemoveValueLoggable log = new RemoveValueLoggable(transaction,
                                                fileId, page.getPageNum(), i, keys[i], ptrs[i]);
                                            writeToLog(log, this);
                                        }
                                        if (callback != null) {
                                            callback.indexInfo(keys[i], ptrs[i]);
                                        }
                                        removeKey(i);
                                        removePointer(i);
                                        recalculateDataLen();
                                        --i;
                                    }
                                }
                                if (rightIdx >= nPtrs) {
                                    removeSequential(transaction, this, query, callback);
                                }
                                break;
                            case IndexQuery.IN :
                            case IndexQuery.NIN :
                                if (leftIdx < 0) {
                                    leftIdx = - (leftIdx + 1);
                                }
                                if (rightIdx < 0) {
                                    rightIdx = - (rightIdx + 1);
                                }
                                for (int i = 0; i < nPtrs; i++) {
                                    if (!pos || (i >= leftIdx && i <= rightIdx)) {
                                        if (query.testValue(keys[i])) {
                                            if (transaction != null && isRecoveryEnabled()) {
                                                final RemoveValueLoggable log = new RemoveValueLoggable(transaction,
                                                    fileId, page.getPageNum(), i, keys[i], ptrs[i]);
                                                    writeToLog(log, this);
                                            }
                                            if (callback != null) {
                                                callback.indexInfo(keys[i], ptrs[i]);
                                            }
                                            removeKey(i);
                                            removePointer(i);
                                            recalculateDataLen();
                                            --i;
                                        }
                                    }
                                }
                                break;
                            case IndexQuery.LT :
                            case IndexQuery.GEQ :
                                if (leftIdx < 0) {
                                    leftIdx = - (leftIdx + 1);
                                }
                                for (int i = 0; i < nPtrs; i++) {
                                    if ((pos && (i <= leftIdx)) || (!pos && (i >= leftIdx))) {
                                        if (query.testValue(keys[i])) {
                                            if (transaction != null && isRecoveryEnabled()) {
                                                final RemoveValueLoggable log = new RemoveValueLoggable(transaction,
                                                    fileId, page.getPageNum(), i, keys[i], ptrs[i]);
                                                    writeToLog(log, this);
                                            }
                                            if (callback != null) {
                                                callback.indexInfo(keys[i], ptrs[i]);
                                            }
                                            removeKey(i);
                                            removePointer(i);
                                            recalculateDataLen();
                                            --i;
                                        }
                                    }
                                }
                                break;
                            case IndexQuery.GT :
                            case IndexQuery.LEQ :
                                if (leftIdx < 0) {
                                    leftIdx = - (leftIdx + 1);
                                }
                                for (int i = 0; i < nPtrs; i++) {
                                    if ((pos && (i >= leftIdx)) || (!pos && (i <= leftIdx))) {
                                        if (query.testValue(keys[i])) {
                                            if (transaction != null && isRecoveryEnabled()) {
                                                final RemoveValueLoggable log = new RemoveValueLoggable(transaction,
                                                    fileId, page.getPageNum(), i, keys[i], ptrs[i]);
                                                writeToLog(log, this);
                                            }
                                            if (callback != null) {
                                                callback.indexInfo(keys[i], ptrs[i]);
                                            }
                                            removeKey(i);
                                            removePointer(i);
                                            recalculateDataLen();
                                            --i;
                                        } else if (query.getOperator() == IndexQuery.TRUNC_RIGHT) {
                                            break;
                                        }
                                    }
                                }
                                break;
                            default :
                                // If it's not implemented, it falls right through
                                for (int i = 0; i < nPtrs; i++) {
                                    if (query.testValue(keys[i])) {
                                        if (transaction != null && isRecoveryEnabled()) {
                                            final RemoveValueLoggable log = new RemoveValueLoggable(transaction,
                                                fileId, page.getPageNum(), i, keys[i], ptrs[i]);
                                                writeToLog(log, this);
                                        }
                                        if (callback != null) {
                                            callback.indexInfo(keys[i], ptrs[i]);
                                        }
                                        removeKey(i);
                                        removePointer(i);
                                        recalculateDataLen();
                                        --i;
                                    }
                                }
                                break;
                            }
                        } finally {
                            allowUnload = true;
                        }
                        break;
                    default :
                        throw new BTreeException("Invalid Page Type In query");
                }
            } else {
                // No Query - Just Walk The Tree
                switch (pageHeader.getType()) {
                    case BRANCH :
                        for (int i = 0; i < nPtrs; i++) {
                            if (transaction != null && isRecoveryEnabled()) {
                                final RemoveValueLoggable log =
                                    new RemoveValueLoggable(transaction,
                                        fileId, page.getPageNum(), i, keys[i], ptrs[i]);
                                writeToLog(log, this);
                            }
                            if (callback != null) {
                                callback.indexInfo(keys[i], ptrs[i]);
                            }
                            removeKey(i);
                            removePointer(i);
                            recalculateDataLen();
                            --i;
                        }
                        break;
                    case LEAF :
                        for (int i = 0; i < nKeys; i++) {
                            if (query.getOperator() != IndexQuery.TRUNC_LEFT
                                    || query.testValue(keys[i])) {
                                if (transaction != null && isRecoveryEnabled()) {
                                    final RemoveValueLoggable log = new RemoveValueLoggable(transaction,
                                        fileId, page.getPageNum(), i, keys[i], ptrs[i]);
                                    writeToLog(log, this);
                                }
                                if (callback != null) {
                                    callback.indexInfo(keys[i], ptrs[i]);
                                }
                                removeKey(i);
                                removePointer(i);
                                recalculateDataLen();
                                --i;
                            }
                        }
                        break;
                    default :
                        throw new BTreeException("Invalid Page Type In query");
                }
            }
        }

        private void growPrefix() {
            if (nKeys == 0) {
                return;
            }

            if (nKeys == 1) {
                if (keys[0].getLength() > 0) {
                    final byte[] newPrefix = new byte[prefix.getLength() + keys[0].getLength()];
                    System.arraycopy(prefix.data(), prefix.start(), newPrefix, 0 , prefix.getLength());
                    System.arraycopy(keys[0].data(), keys[0].start(), newPrefix, prefix.getLength(),
                        keys[0].getLength());
                    prefix = new Value(newPrefix);
                    keys[0] = Value.EMPTY_VALUE;
                }
                return;
            }
            //int idx;
            int max = Integer.MAX_VALUE;
            final Value first = keys[0];
            for (int i = 1; i < nKeys; i++) {
                final Value value = keys[i];
                final int idx = Math.abs(value.compareTo(first));
                if (idx < max) {
                    max = idx;
                }
            }
            final int addChars = max - 1;
            if (addChars > 0) {
                // create new prefix with the additional characters
                final byte[] pdata = new byte[prefix.getLength() + addChars];
                System.arraycopy(prefix.data(), prefix.start(), pdata, 0, prefix.getLength());
                System.arraycopy(keys[0].data(), keys[0].start(), pdata, prefix.getLength(), addChars);
                prefix = new Value(pdata);
                // shrink the keys by addChars characters
                for (int i = 0; i < nKeys; i++) {
                    final Value key = keys[i];
                    keys[i] = new Value(key.data(), key.start() + addChars, key.getLength() - addChars);
                }
                recalculateDataLen();
            }
        }

        private void shrinkPrefix(final int newLen) {
            final int diff = prefix.getLength() - newLen;
            Value[] nv = new Value[nKeys];
            for (int i = 0; i < nKeys; i++) {
                final Value value = keys[i];
                final byte[] ndata = new byte[value.getLength() + diff];
                System.arraycopy(prefix.data(), prefix.start() + newLen, ndata, 0, diff);
                System.arraycopy(value.data(), value.start(), ndata, diff, value.getLength());
                nv[i] = new Value(ndata);
            }
            keys = nv;
            prefix = new Value(prefix.data(), prefix.start(), newLen);
        }

        /**
         * Insert a key into the array of keys.
         *
         * @param val the value
         * @param idx the index
         */
        private void insertKey(Value val, final int idx) {
            if (pageHeader.getType() == PageType.BRANCH) {
                // in a leaf page we might have to adjust the prefix
                if (nKeys == 0) {
                    prefix = val;
                    val = Value.EMPTY_VALUE;
                } else {
                    final int pfxLen = val.checkPrefix(prefix);
                    if (pfxLen < prefix.getLength()) {
                        shrinkPrefix(pfxLen);
                    }
                    val = new Value(val.data(), val.start() + pfxLen, val.getLength() - pfxLen);
                }
            }
            resizeKeys(nKeys + 1);
            System.arraycopy(keys, idx, keys, idx + 1, nKeys - idx);
            keys[idx] = val;
            pageHeader.updateValueCount((short) ++nKeys);
            setDirty(true);
        }

        /**
         * Remove a key from the array of keys.
         *
         * @param idx the index
         */
        private void removeKey(final int idx) {
            try {
                System.arraycopy(keys, idx + 1, keys, idx, nKeys - idx - 1);
            } catch (ArrayIndexOutOfBoundsException e) {
                LOG.error("keys: {} idx: {}", nKeys, idx);
            }
            pageHeader.updateValueCount((short) --nKeys);
            setDirty(true);
        }

        /**
         * Insert a pointer into the array of pointers.
         * 
         * @param ptr the pointer
         * @param idx the index
         */
        private void insertPointer(final long ptr, final int idx) {
            resizePtrs(nPtrs + 1);
            System.arraycopy(ptrs, idx, ptrs, idx + 1, nPtrs - idx);
            ptrs[idx] = ptr;
            nPtrs++;
            setDirty(true);
        }

        /**
         * Remove a pointer from the array of pointers.
         *
         * @param idx the index
         */
        private void removePointer(final int idx) {
            System.arraycopy(ptrs, idx + 1, ptrs, idx, nPtrs - idx - 1);
            nPtrs--;
            setDirty(true);
        }

        /**
         * Search for the given key in the keys of this node.
         *
         * @param key the key
         *
         * @return the position
         */
        private int searchKey(Value key) {
            if (pageHeader.getType() == PageType.BRANCH && prefix != null && prefix.getLength() > 0) {
                // if this is a leaf page, check the common prefix first
                if (key.getLength() < prefix.getLength()) {
                    return key.compareTo(prefix) <= 0 ? -1 : -(nKeys + 1);
                }
                final int pfxCmp = key.comparePrefix(prefix);
                if (pfxCmp < 0) {
                    return -1;
                }
                if (pfxCmp > 0) {
                    return -(nKeys + 1);
                }
                key = new Value(key.data(), key.start() + prefix.getLength(), 
                    key.getLength() - prefix.getLength());
            }
            int low = 0;
            int high = nKeys - 1;
            while (low <= high) {
                final int mid = (low + high) >> 1;
                final Value  midVal = keys[mid];
                final int cmp = midVal.compareTo(key);
                if (cmp < 0) {
                    low = mid + 1;
                } else if (cmp > 0) {
                    high = mid - 1;
                } else {
                    return mid;  // key found
                }
            }
            return -(low + 1); // key not found.
        }

        private void resizeKeys(final int minCapacity) {
            final int oldCapacity = keys.length;
            if (minCapacity > oldCapacity) {
                final Value oldData[] = keys;
                int newCapacity = (oldCapacity * 3)/2 + 1;
                if (newCapacity < minCapacity) {
                    newCapacity = minCapacity;
                }
                keys = new Value[newCapacity];
                System.arraycopy(oldData, 0, keys, 0, nKeys);
            }
        }
 
        private void resizePtrs(final int minCapacity) {
            final int oldCapacity = ptrs.length;
            if (minCapacity > oldCapacity) {
                final long[] oldData = ptrs;
                int newCapacity = (oldCapacity * 3)/2 + 1;
                if (newCapacity < minCapacity) {
                    newCapacity = minCapacity;
                }
                ptrs = new long[newCapacity];
                System.arraycopy(oldData, 0, ptrs, 0, nPtrs);
            }
        }
    }

    public BufferStats getIndexBufferStats() {
        return new BufferStats(
            cache.getBuffers(),
            cache.getUsedBuffers(),
            cache.getHits(),
            cache.getFails());
    }

    public void printStatistics() {
        final NumberFormat nf = NumberFormat.getPercentInstance();
        final StringBuilder buf = new StringBuilder();
        buf.append(FileUtils.fileName(getFile())).append(" INDEX ");
        buf.append("Buffers occupation : ");
        if (cache.getBuffers() == 0 && cache.getUsedBuffers() == 0) {
            buf.append("N/A");
        } else {
            buf.append(nf.format(cache.getUsedBuffers()/(float)cache.getBuffers()));
        }
        buf.append(" (").append(cache.getUsedBuffers()).append(" out of ").append(cache.getBuffers()).append(")");
        buf.append(" Cache efficiency : ");
        if (cache.getHits() == 0 && cache.getFails() == 0) {
            buf.append("N/A");
        } else {
            buf.append(nf.format(cache.getHits() / (float)(cache.getFails() + cache.getHits())));
        }
        LOGSTATS.info(buf.toString());
    }
}
