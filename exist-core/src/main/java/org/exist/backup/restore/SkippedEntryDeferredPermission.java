/*
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
package org.exist.backup.restore;

import org.exist.security.ACLPermission.ACE_ACCESS_TYPE;
import org.exist.security.ACLPermission.ACE_TARGET;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.xmldb.XmldbURI;

/**
 * Represents the permissions for a skipped entry in the restore process, e.g. apply() does nothing
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class SkippedEntryDeferredPermission implements DeferredPermission {

    @Override
    public void apply(final DBBroker broker, final Txn transaction) {
    }

    @Override
    public XmldbURI getTarget() {
        return XmldbURI.create("skipped");
    }

    @Override
    public void addACE(final int index, final ACE_TARGET target, final String who, final ACE_ACCESS_TYPE access_type, final int mode) {
    }
}