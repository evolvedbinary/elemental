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
package org.exist.storage.blob;

import org.exist.storage.DBBroker;
import org.exist.storage.journal.AbstractLoggable;

import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractBlobLoggable extends AbstractLoggable implements BlobLoggable {
    protected DBBroker broker;
    private BlobId blobId;

    public AbstractBlobLoggable(final byte type, final long transactionId, final BlobId blobId) {
        super(type, transactionId);
        this.blobId = blobId;
    }

    public AbstractBlobLoggable(final byte type, final DBBroker broker, final long transactionId) {
        super(type, transactionId);
        this.broker = broker;
    }

    @Override
    public void write(final ByteBuffer out) {
        out.putInt(blobId.getId().length);
        out.put(blobId.getId());
    }

    @Override
    public void read(final ByteBuffer in) {
        final int idLen = in.getInt();
        final byte[] id = new byte[idLen];
        in.get(id);
        this.blobId = new BlobId(id);
    }

    @Override
    public int getLogSize() {
        return 4 + blobId.getId().length;
    }

    /**
     * Get the Blob id
     *
     * @return the blob id
     */
    public BlobId getBlobId() {
        return blobId;
    }
}
