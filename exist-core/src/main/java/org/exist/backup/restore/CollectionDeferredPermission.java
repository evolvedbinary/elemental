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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.backup.restore.listener.RestoreListener;
import org.exist.collections.Collection;
import org.exist.security.ACLPermission;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.security.PermissionFactory;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.xmldb.XmldbURI;

import java.io.IOException;
import java.util.Optional;

/**
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
class CollectionDeferredPermission extends AbstractDeferredPermission {
    
    private final static Logger LOG = LogManager.getLogger(CollectionDeferredPermission.class);
    
    public CollectionDeferredPermission(final RestoreListener listener, final XmldbURI collectionUri, final String owner, final String group, final Integer mode) {
        super(listener, collectionUri, owner, group, mode);
    }

    @Override
    public void apply(final DBBroker broker, final Txn transaction) {
        try (final Collection collection = broker.openCollection(getTarget(), Lock.LockMode.WRITE_LOCK)) {
            final Permission permission = collection.getPermissions();
            PermissionFactory.chown(broker, permission, Optional.ofNullable(getOwner()), Optional.ofNullable(getGroup()));
            PermissionFactory.chmod(broker, permission, Optional.of(getMode()), Optional.ofNullable(permission instanceof ACLPermission ? getAces() : null));
            broker.saveCollection(transaction, collection);
        } catch (final PermissionDeniedException | IOException e) {
            final String msg = "ERROR: Failed to set permissions on Collection '" + getTarget() + "'.";
            LOG.error(msg, e);
            getListener().warn(msg);
        }
    }
}
