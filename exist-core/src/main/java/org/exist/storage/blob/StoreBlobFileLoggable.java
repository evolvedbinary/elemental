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

import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class StoreBlobFileLoggable extends AbstractBlobLoggable {
    private String stagedUuid;

    public StoreBlobFileLoggable(final long transactionId, final BlobId blobId, final String stagedUuid) {
        super(LOG_STORE_BLOB_FILE, transactionId, blobId);
        this.stagedUuid = stagedUuid;
    }

    public StoreBlobFileLoggable(final DBBroker broker, final long transactionId) {
        super(LOG_STORE_BLOB_FILE, broker, transactionId);
    }

    @Override
    public void write(final ByteBuffer out) {
        super.write(out);
        final byte[] strUuidBytes = stagedUuid.getBytes(UTF_8);
        out.putInt(strUuidBytes.length);
        out.put(strUuidBytes);
    }

    @Override
    public void read(final ByteBuffer in) {
        super.read(in);
        final int strUuidBytesLen = in.getInt();
        final byte[] strUuidBytes = new byte[strUuidBytesLen];
        in.get(strUuidBytes);
        this.stagedUuid = new String(strUuidBytes, UTF_8);
    }

    @Override
    public int getLogSize() {
        return super.getLogSize() + 4 + stagedUuid.getBytes(UTF_8).length;
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
     * Get the UUID of the staged file
     *
     * @return the UUID of the staged file
     */
    public String getStagedUuid() {
        return stagedUuid;
    }
}
