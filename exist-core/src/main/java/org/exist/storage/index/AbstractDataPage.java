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

import org.exist.storage.cache.AbstractCacheable;
import org.exist.storage.cache.Cacheable;
import org.exist.xquery.Constants;

import java.io.IOException;

/**
 * Base class for a data page in a {@link BFile}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:wolfgang@exist-db.org">Wolfgang Meier</a>
 */
abstract class AbstractDataPage extends AbstractCacheable implements Comparable<AbstractDataPage>, Cacheable {

    public abstract void delete() throws IOException;

    public abstract byte[] getData() throws IOException;

    public abstract BFilePageHeader getPageHeader();

    public abstract String getPageInfo();

    public abstract long getPageNum();

    public abstract int findValuePosition(short tid) throws IOException;

    public abstract short getNextTID();

    public abstract void removeTID(short tid, int length) throws IOException;

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

