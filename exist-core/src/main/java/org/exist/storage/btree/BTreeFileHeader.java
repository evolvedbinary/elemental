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

import org.exist.util.ByteConversion;

import java.io.IOException;

/**
 * BTree File Header.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:meier@ifs.tu-darmstadt.de">Wolfgang Meier</a>
 */
public class BTreeFileHeader extends AbstractPagedFileHeader implements PagedFileHeader {

    private final static int MIN_SPACE_PER_KEY = 32;

    private long rootPage = 0;
    private short fixedLen = -1;

    public BTreeFileHeader(final short fileVersion, final long pageCount, final int pageSize) {
        super(fileVersion, pageCount, pageSize);
    }

    public BTreeFileHeader(final short fileVersion, final int pageSize) {
        super(fileVersion, 1024, pageSize);
    }

    @Override
    protected int read(final byte[] buf) throws IOException {
        int offset = super.read(buf);
        rootPage = ByteConversion.byteToLong(buf, offset);
        offset += 8;
        fixedLen = ByteConversion.byteToShort(buf, offset);
        offset += 2;
        return offset;
    }

    @Override
    protected int write(final byte[] buf) throws IOException {
        int offset = super.write(buf);
        ByteConversion.longToByte(rootPage, buf, offset);
        offset += 8;
        ByteConversion.shortToByte(fixedLen, buf, offset);
        offset += 2;
        return offset;
    }

    /**
     *  Set the root page of the storage tree
     *
     * @param rootPage The new rootPage value
     */
    public final void setRootPage(final long rootPage) {
        this.rootPage = rootPage;
        setDirty(true);
    }

    /**
     *  Get the root page of the storage tree
     *
     * @return The rootPage value
     */
    public final long getRootPage() {
        return rootPage;
    }

    public short getFixedKeyLen() {
        return fixedLen;
    }

    public void setFixedKeyLen(short keyLen) {
        this.fixedLen = keyLen;
    }

    @Override
    public int getMaxKeySize() {
        return (getWorkSize() / 2) - MIN_SPACE_PER_KEY;
    }
}
