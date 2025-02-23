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
import org.exist.storage.btree.PageStatus;
import org.exist.storage.btree.Value;
import org.exist.util.ByteConversion;
import org.exist.util.IndexCallback;
import org.exist.xquery.TerminatedException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
class FindCallback implements BTreeCallback {

    enum Mode {
        BOTH,
        KEYS,
        VALUES
    }

    private final Mode mode;
    private @Nullable final IndexCallback callback;
    private @Nullable final ArrayList<Value> values;

    public FindCallback(final Mode mode) {
        this.mode = mode;
        this.callback = null;
        this.values = new ArrayList<>();
    }

    public FindCallback(final IndexCallback callback) {
        this.mode = Mode.BOTH;
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
                    if (page.getPageHeader().getStatus() == PageStatus.MULTI_PAGE) {
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
