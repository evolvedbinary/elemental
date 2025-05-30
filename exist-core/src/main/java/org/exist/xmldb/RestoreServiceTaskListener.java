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
package org.exist.xmldb;

public interface RestoreServiceTaskListener {
    void startedZipForTransfer(final long totalUncompressedSize);
    void addedFileToZipForTransfer(final long uncompressedSize);
    void finishedZipForTransfer();

    void startedTransfer(final long transferSize);
    void transferred(final long chunkSize);
    void finishedTransfer();

    void started(long numberOfFiles);
    void processingDescriptor(String backupDescriptor);
    void createdCollection(String collectionUri);
    void restoredResource(String resourceUri);
    void skipResources(String message, long count);

    void info(String message);
    void warn(String message);
    void error(String message);

    void finished();
}