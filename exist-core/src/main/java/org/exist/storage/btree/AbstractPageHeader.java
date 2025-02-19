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

import org.exist.storage.journal.Lsn;
import org.exist.util.ByteConversion;

import java.io.IOException;

/**
 * Base class for page headers.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
public abstract class AbstractPageHeader implements PageHeader {

    public static final int LENGTH_PAGE_STATUS = 1; //sizeof byte
    public static final int LENGTH_PAGE_DATA_LENGTH = 4; //sizeof int
    public static final int LENGTH_PAGE_NEXT_PAGE = 8; //sizeof long
    public static final int LENGTH_PAGE_LSN = Lsn.RAW_LENGTH;

    private int dataLen = 0;
    private long nextPage = Page.NO_PAGE;
    private boolean dirty = false;

    /**
     * The status of the current page.
     */
    private PageStatus status = PageStatus.UNUSED;

    private Lsn lsn = Lsn.LSN_INVALID;

    public AbstractPageHeader() {
    }

    public AbstractPageHeader(final byte[] data, final int offset) throws IOException {
        read(data, offset);
    }

    @Override
    public int getDataLen() {
        return this.dataLen;
    }

    @Override
    public void updateDataLen(final int newDataLength) {
        this.dataLen = newDataLength;
        setDirty(true);
    }

    @Override
    public long getNextPage() {
        return this.nextPage;
    }

    @Override
    public void updateNextPage(final long newNextPage) {
        this.nextPage = newNextPage;
        setDirty(true);
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setDirty(final boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public PageStatus getStatus() {
        return this.status;
    }

    @Override
    public void updateStatus(final PageStatus newStatus) {
        this.status = newStatus;
        setDirty(true);
    }

    @Override
    public Lsn getLsn() {
        return this.lsn;
    }

    @Override
    public void setLsn(final Lsn lsn) {
        this.lsn = lsn;
    }

    public int read(final byte[] data, int offset) throws IOException {
        status = PageStatus.fromValue(data[offset]);
        offset += LENGTH_PAGE_STATUS;
        dataLen = ByteConversion.byteToInt(data, offset);
        offset += LENGTH_PAGE_DATA_LENGTH;
        nextPage = ByteConversion.byteToLong(data, offset);
        offset += LENGTH_PAGE_NEXT_PAGE;
        lsn = Lsn.read(data, offset);
        offset += LENGTH_PAGE_LSN;

        // TODO(AR) should we mark this an non-dirty?
        //  setDirty(false);

        return offset;
    }

    public int write(final byte[] data, int offset) throws IOException {
        data[offset] = status.getValue();
        offset += LENGTH_PAGE_STATUS;
        ByteConversion.intToByte(dataLen, data, offset);
        offset += LENGTH_PAGE_DATA_LENGTH;
        ByteConversion.longToByte(nextPage, data, offset);
        offset += LENGTH_PAGE_NEXT_PAGE;
        lsn.write(data, offset);
        offset += LENGTH_PAGE_LSN;
        setDirty(false);
        return offset;
    }
}
