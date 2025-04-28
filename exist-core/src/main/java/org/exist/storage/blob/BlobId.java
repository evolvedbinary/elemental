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

import java.util.Arrays;

import static org.exist.util.HexEncoder.bytesToHex;

/**
 * Identifier for a BLOB.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public final class BlobId {
    private final byte[] id;

    /**
     * @param id the raw identifier
     */
    public BlobId(final byte[] id) {
        this.id = id;
    }

    /**
     * Gets the raw identifier.
     *
     * @return the raw identifier.
     */
    public byte[] getId() {
        return id;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BlobId blobId = (BlobId) o;
        return Arrays.equals(id, blobId.id);
    }

    @Override
    public final int hashCode() {
        return Arrays.hashCode(id);
    }

    @Override
    public String toString() {
        return bytesToHex(id);
    }
}
