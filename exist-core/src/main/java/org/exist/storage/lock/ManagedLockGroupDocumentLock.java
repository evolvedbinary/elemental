/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.storage.lock;

import org.exist.xmldb.XmldbURI;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ManagedLockGroupDocumentLock extends ManagedDocumentLock<LockGroup> {

    private final LockTable lockTable;

    public ManagedLockGroupDocumentLock(final XmldbURI documentUri, final LockGroup lock, final LockTable lockTable) {
        super(documentUri, lock, null);    // NOTE(AR) we can set the closer as null here, because we override {@link #close()} below!
        this.lockTable = lockTable;
    }

    @Override
    public void close() {
        if (!closed) {
            LockManager.unlockAll(lock.locks, l -> lockTable.released(lock.groupId, l.path, Lock.LockType.DOCUMENT, l.mode));
        }
        this.closed = true;
    }
}
