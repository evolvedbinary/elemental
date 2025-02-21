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

import org.exist.storage.btree.BTreeFileHeader;

import java.io.IOException;

/**
 * BFile Header.
 * The file header stores the list of data pages containing unused space.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:meier@ifs.tu-darmstadt.de">Wolfgang Meier</a>
 */
public class BFileHeader extends BTreeFileHeader {

    private final FreeList freeList = new FreeList();

    //public final static int MAX_FREE_LIST_LEN = 128;

    public BFileHeader(final short fileVersion, final int pageSize) {
        super(fileVersion, pageSize);
    }

    public void addFreeSpace(final FreeSpace freeSpace) {
        freeList.add(freeSpace);
        setDirty(true);
    }

    public FreeSpace findFreeSpace(final int needed) {
        return freeList.find(needed);
    }

    public FreeSpace getFreeSpace(final long page) {
        return freeList.retrieve(page);
    }

    public void removeFreeSpace(final FreeSpace space) {
        if (space == null) {
            return;
        }
        freeList.remove(space);
        setDirty(true);
    }

//    public void debugFreeList() {
//        LOG.debug("{}: {}", FileUtils.fileName(getFile()), freeList.toString());
//    }

    @Override
    protected int read(final byte[] buf) throws IOException {
        final int offset = super.read(buf);
        return freeList.read(buf, offset);
    }

    @Override
    protected int write(final byte[] buf) throws IOException {
        final int offset = super.write(buf);
        return freeList.write(buf, offset);
    }
}
