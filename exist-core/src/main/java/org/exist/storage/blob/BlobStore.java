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

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.backup.RawDataBackup;
import org.exist.storage.journal.LogException;
import org.exist.storage.txn.Txn;
import org.exist.util.crypto.digest.DigestType;
import org.exist.util.crypto.digest.MessageDigest;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Store for BLOBs (Binary Large Objects).
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface BlobStore extends Closeable {

    /**
     * Open's the BLOB Store.
     *
     * Should be called before any other actions.
     *
     * @throws IOException if the store cannot be opened.
     */
    void open() throws IOException;

    /**
     * Open's the BLOB Store for Recovery after a system crash.
     *
     * Should ONLY be called from {@link org.exist.storage.recovery.RecoveryManager#recover()}
     * when stating up the database after a system crash.
     *
     * @throws FileNotFoundException if there is no existing blob.dbx to recover!
     * @throws IOException if the store cannot be opened.
     */
    void openForRecovery() throws FileNotFoundException, IOException;

    /**
     * Add a BLOB to the BLOB Store.
     *
     * @param transaction the current database transaction.
     * @param is the input stream containing the blob data.
     *
     * @return an identifier representing the stored blob and the size of the blob in bytes.
     *
     * @throws IOException if the BLOB cannot be added.
     */
    Tuple2<BlobId, Long> add(final Txn transaction, final InputStream is) throws IOException;

    /**
     * Make a copy of a BLOB in the the BLOB Store.
     *
     * There is no requirements that an implementation actually make
     * a physical copy of a BLOB, so long as it can provide COPY like
     * semantics.
     *
     * This function exists as an optimisation opportunity for
     * implementations to avoid having to call {@link #add(Txn, InputStream)}
     * with the BLOB data to make a copy.
     *
     * @param transaction the current database transaction.
     * @param blobId the id of the BLOB to copy.
     *
     * @return an identifier representing the copied blob,
     *     or null if there is no such BLOB to copy.
     *
     * @throws IOException if the BLOB cannot be copied.
     */
    BlobId copy(final Txn transaction, final BlobId blobId) throws IOException;

    /**
     * Get a BLOB from the BLOB Store.
     *
     * @param transaction the current database transaction.
     * @param blobId the identifier representing the blob to be retrieved.
     *
     * @return an InputStream for accessing the BLOB data, or null if there is no such BLOB.
     *     NOTE the stream MUST be closed when the caller has finished
     *     with it to release any associated resources.
     *
     * @throws IOException if an error occurs whilst retrieving the BLOB.
     */
    @Nullable InputStream get(final Txn transaction, final BlobId blobId) throws IOException;

    /**
     * Get the digest of a BLOB in the BLOB store.
     *
     * @param transaction the current database transaction.
     * @param blobId the identifier representing the blob to be digested.
     * @param digestType the type of digest to compute
     *
     * @return the digest of the BLOB, or null if there is no such BLOB.
     *
     * @throws IOException if an error occurs whilst computing the digest of the BLOB.
     */
    @Nullable MessageDigest getDigest(final Txn transaction, final BlobId blobId, final DigestType digestType)
            throws IOException;

    /**
     * Perform an operation with a {@link Path} reference to a BLOB.
     *
     * NOTE: Use of this method should be avoided where possible. It only
     * exists for integration with tools external to Java which can only
     * work with File Paths and where making a copy of the file is not
     * necessary.
     *
     * WARNING: The provided {@link Path} MUST ONLY be used for
     * READ operations, any WRITE/DELETE operation will corrupt the
     * integrity of the blob store.
     *
     * Consider if you really need to use this method. It is likely you could
     * instead use {@link #get(Txn, BlobId)} and make a copy of the data to
     * a temporary file.
     *
     * Note that any resources associated with the BLOB file
     * may not be released until the {@code fnFile} has finished executing.
     *
     * USE WITH CAUTION!
     *
     * @param transaction the current database transaction.
     * @param blobId the identifier representing the blob to be retrieved.
     * @param <T> the type of the return value
     * @param fnFile a function which performs a read-only operation on the BLOB file.
     *     The Path will be null if the Blob does not exist in the Blob Store.
     *     If you wish to handle exceptions in your function you should consider
     *     {@link com.evolvedbinary.j8fu.Try} or similar.
     *
     * @return the result of the {@code fnFile} function.
     *
     * @throws IOException if an error occurs whilst retrieving the BLOB file.
     */
    <T> T with(final Txn transaction, final BlobId blobId, final Function<Path, T> fnFile) throws IOException;

    /**
     * Remove a BLOB from the BLOB Store.
     *
     * @param transaction the current database transaction.
     * @param blobId the identifier representing the blob to be removed.
     *
     * @throws IOException if an error occurs whilst removing the BLOB.
     */
    void remove(final Txn transaction, final BlobId blobId) throws IOException;

    /**
     * Backup the Blob Store to the backup.
     *
     * @param backup the backup to write the Blob Store to.
     * @throws IOException if an error occurs whilst creating a backup of the BLOB.
     */
    void backupToArchive(final RawDataBackup backup) throws IOException;

    void redo(final BlobLoggable blobLoggable) throws LogException;

    void undo(final BlobLoggable blobLoggable) throws LogException;
}
