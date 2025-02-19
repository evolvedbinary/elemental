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
package org.exist.storage.btree;

import java.nio.ByteBuffer;

import org.exist.storage.DBBroker;
import org.exist.storage.journal.LogException;
import org.exist.storage.txn.Txn;

public class CreateBTNodeLoggable extends BTAbstractLoggable {

	// TODO(AR) should this be immutable - or can we reuse these objects if they are mutable

	private PageStatus pageStatus;
	private long pageNum;
	private long parentNum;
	
	public CreateBTNodeLoggable(final Txn transaction, final byte fileId, final PageStatus pageStatus, final long pageNum, final long parentNum) {
		super(BTree.LOG_CREATE_BNODE, fileId, transaction);
		this.pageNum = pageNum;
		this.parentNum = parentNum;
		this.pageStatus = pageStatus;
	}
	
	public CreateBTNodeLoggable(final DBBroker broker, final long transactionId) {
		super(BTree.LOG_CREATE_BNODE, broker, transactionId);
	}

	public long getPageNum() {
		return this.pageNum;
	}

	public long getParentNum() {
		return this.parentNum;
	}

	public PageStatus getPageStatus() {
		return this.pageStatus;
	}

	@Override
	public void redo() throws LogException {
		getStorage().redoCreateBTNode(this);
	}

	@Override
	public void write(final ByteBuffer out) {
        super.write(out);
		out.put(this.pageStatus.getValue());
		out.putLong(this.pageNum);
		out.putLong(this.parentNum);
	}

	@Override
	public void read(final ByteBuffer in) {
        super.read(in);
		this.pageStatus = PageStatus.fromValue(in.get());
		this.pageNum = in.getLong();
		this.parentNum = in.getLong();
	}

	@Override
	public int getLogSize() {
		return super.getLogSize() + 17;
	}

	@Override
	public String dump() {
		return super.dump() + " - create btree node: " + this.pageNum;
	}
}
