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
import org.exist.storage.journal.LogException;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class UpdateBlobRefCountLoggable extends AbstractBlobLoggable {

    private Integer currentCount;
    private Integer newCount;

    public UpdateBlobRefCountLoggable(final long transactionId, final BlobId blobId, final int currentCount, final int newCount) {
        super(LOG_UPDATE_BLOB_REF_COUNT, transactionId, blobId);
        this.currentCount = currentCount;
        this.newCount = newCount;
    }

    public UpdateBlobRefCountLoggable(final DBBroker broker, final long transactionId) {
        super(LOG_UPDATE_BLOB_REF_COUNT, broker, transactionId);
    }

    @Override
    public void write(final ByteBuffer out) {
        super.write(out);
        out.putInt(currentCount);
        out.putInt(newCount);
    }

    @Override
    public void read(final ByteBuffer in) {
        super.read(in);
        currentCount = in.getInt();
        newCount = in.getInt();
    }

    @Override
    public int getLogSize() {
        return super.getLogSize() + 4 + 4;
    }

    @Override
    public void redo() throws LogException {
        final BlobStore blobStore = broker.getBrokerPool().getBlobStore();
        blobStore.redo(this);
    }

    @Override
    public void undo() throws LogException {
        final BlobStore blobStore = broker.getBrokerPool().getBlobStore();
        blobStore.undo(this);
    }

    /**
     * Get the current count
     *
     * @return the current count
     */
    @Nullable public Integer getCurrentCount() {
        return currentCount;
    }

    /**
     * Get the new count
     *
     * @return the new count
     */
    @Nullable public Integer getNewCount() {
        return newCount;
    }
}
