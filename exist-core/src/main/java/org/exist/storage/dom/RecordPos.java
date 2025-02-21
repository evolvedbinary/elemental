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
package org.exist.storage.dom;

class RecordPos {

    DOMPage page;
    int offset;
    short tupleId;
    boolean link;

    public RecordPos(final int offset, final DOMPage page, final short tupleId) {
        this(offset, page, tupleId, false);
    }

    public RecordPos(final RecordPos other) {
        this(other.offset, other.page, other.tupleId, other.link);
    }

    public RecordPos(final int offset, final DOMPage page, final short tupleId, final boolean link) {
        this.offset = offset;
        this.page = page;
        this.tupleId = tupleId;
        this.link = link;
    }

    public void set(final RecordPos other) {
        this.page = other.page;
        this.offset = other.offset;
        this.tupleId = other.tupleId;
        this.link = other.link;
    }

    @Override
    public String toString() {
        return "RecordPos { offset: " + offset + ", tupleId: " + tupleId + ", link: " + link + "}";
    }
}