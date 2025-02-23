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

import org.exist.storage.StorageAddress;
import org.exist.storage.btree.BTreeCallback;
import org.exist.storage.btree.Value;
import org.exist.util.ByteConversion;
import org.exist.xquery.TerminatedException;

import java.io.IOException;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
class FilterCallback implements BTreeCallback {
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
