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

import java.nio.ByteBuffer;

import org.exist.storage.DBBroker;
import org.exist.storage.btree.PageStatus;
import org.exist.storage.journal.LogException;
import org.exist.storage.txn.Txn;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
public class OverflowRemoveLoggable extends AbstractBFileLoggable {

    // TODO(AR) should this be immutable - or can we reuse these objects if they are mutable

    private PageStatus pageStatus;
    private long pageNum;
    private byte[] data;
    private int length;
    private long nextInChain;
    
    /**
     *
     * @param fileId the file id
     * @param transaction the database transaction
     * @param pageStatus the page status
     * @param pageNum the page number
     * @param data the data
     * @param length the length of the data
     * @param nextInChain the next in chain
     */
    public OverflowRemoveLoggable(final byte fileId, final Txn transaction, final PageStatus pageStatus, final long pageNum, final byte[] data,
            final int length, final long nextInChain) {
        super(BFile.LOG_OVERFLOW_REMOVE, fileId, transaction);
        this.pageStatus = pageStatus;
        this.pageNum = pageNum;
        this.data = data;
        this.length = length;
        this.nextInChain = nextInChain;
    }

    /**
     * @param broker the database broker
     * @param transactionId thr transaction id
     */
    public OverflowRemoveLoggable(final DBBroker broker, final long transactionId) {
        super(broker, transactionId);
    }

    public PageStatus getPageStatus() {
        return this.pageStatus;
    }

    public long getPageNum() {
        return this.pageNum;
    }

    public int getLength() {
        return this.length;
    }

    public long getNextInChain() {
        return this.nextInChain;
    }

    public byte[] getData() {
        return this.data;
    }

    @Override
    public void write(final ByteBuffer out) {
        super.write(out);
        out.put(pageStatus.getValue());
        out.putInt((int) this.pageNum);
        out.putInt((int) this.nextInChain);
        out.putInt(this.length);
        out.put(this.data, 0, this.length);
    }

    @Override
    public void read(final ByteBuffer in) {
        super.read(in);
        this.pageStatus = PageStatus.fromValue(in.get());
        this.pageNum = in.getInt();
        this.nextInChain = in.getInt();
        this.length = in.getInt();
        this.data = new byte[this.length];
        in.get(this.data);
    }

    @Override
    public int getLogSize() {
        return super.getLogSize() + 13 + this.length;
    }

    @Override
    public void redo() throws LogException {
        getIndexFile().redoRemoveOverflow(this);
    }

    @Override
    public void undo() throws LogException {
        getIndexFile().undoRemoveOverflow(this);
    }

    @Override
    public String dump() {
        return super.dump() + " - remove overflow page " + this.pageNum;
    }
}
