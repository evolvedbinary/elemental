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
package org.exist.storage.journal;

import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.storage.DBBroker;
import org.exist.util.ByteConversion;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.READ;
import static org.exist.storage.journal.Journal.*;

/**
 * Read log entries from the journal file. This class is used during recovery to scan the
 * last journal file. It uses a memory-mapped byte buffer on the file.
 * Journal entries can be read forward (during redo) or backward (during undo).
 *
 * @author wolf
 */
public class JournalReader implements AutoCloseable {

    private static final Logger LOG = LogManager.getLogger(JournalReader.class);

    private final DBBroker broker;
    private final short fileNumber;
    private final ByteBuffer header = ByteBuffer.allocateDirect(LOG_ENTRY_HEADER_LEN);
    private ByteBuffer payload = ByteBuffer.allocateDirect(8192);  // 8 KB
    @Nullable
    private SeekableByteChannel fc;

    private final StreamingXXHash64 xxHash64 = XXHashFactory.fastestInstance().newStreamingHash64(Journal.XXHASH64_SEED);

    /**
     * Opens the specified file for reading.
     *
     * @param broker     the database broker
     * @param file       the journal file
     * @param fileNumber the number of the journal file
     * @throws LogException if the journal cannot be opened
     */
    public JournalReader(final DBBroker broker, final Path file, final short fileNumber) throws LogException {
        this.broker = broker;
        this.fileNumber = fileNumber;
        try {
            this.fc = Files.newByteChannel(file, READ);
            validateJournalHeader(file, fc);
        } catch (final IOException e) {
            close();
            throw new LogException("Failed to read journal file " + file.toAbsolutePath().toString(), e);
        }
    }

    private void validateJournalHeader(final Path file, final SeekableByteChannel fc) throws IOException, LogException {
        // read the magic number
        final ByteBuffer buf = ByteBuffer.allocateDirect(JOURNAL_HEADER_LEN);
        fc.read(buf);
        buf.flip();

        // check the magic number
        final boolean validMagic =
                buf.get() == JOURNAL_MAGIC_NUMBER[0]
                && buf.get() == JOURNAL_MAGIC_NUMBER[1]
                && buf.get() == JOURNAL_MAGIC_NUMBER[2]
                && buf.get() == JOURNAL_MAGIC_NUMBER[3];

        if (!validMagic) {
            throw new LogException("File was not recognised as a valid Elemental journal file: " + file.toAbsolutePath().toString());
        }

        // check the version of the journal format
        final short storedVersion = ByteConversion.byteToShortH(new byte[] {buf.get(), buf.get()}, 0);
        final boolean validVersion =
                storedVersion == JOURNAL_VERSION;

        if (!validVersion) {
            throw new LogException("Journal file was version " + storedVersion + ", but required version " + JOURNAL_VERSION + ": " + file.toAbsolutePath().toString());
        }
    }

    /**
     * Returns the next entry found from the current position.
     *
     * @return the next entry, or null if there are no more entries.
     * @throws LogException if an entry could not be read due to an inconsistency on disk.
     */
    public @Nullable
    Loggable nextEntry() throws LogException {
        try {
            checkOpen();

            // are we at the end of the journal?
            if (fc.position() + LOG_ENTRY_BASE_LEN > fc.size()) {
                return null;
            }
        } catch (final IOException e) {
            throw new LogException("Unable to check journal position and size: " + e.getMessage(), e);
        }

        return readEntry();
    }

    /**
     * Returns the previous entry found by scanning backwards from the current position.
     *
     * @return the previous entry, or null of there was no previous entry.
     * @throws LogException if an entry could not be read due to an inconsistency on disk.
     */
    public @Nullable
    Loggable previousEntry() throws LogException {
        try {
            checkOpen();

            // is there a previous entry to read?
            if (fc.position() < JOURNAL_HEADER_LEN + LOG_ENTRY_BASE_LEN) {
                return null;
            }

            // go back 8 bytes (checksum length) + 2 bytes (backLink length) and read the backLink (2 bytes) of the last entry
            fc.position(fc.position() - LOG_ENTRY_CHECKSUM_LEN - LOG_ENTRY_BACK_LINK_LEN);
            header.clear().limit(LOG_ENTRY_BACK_LINK_LEN);
            final int read = fc.read(header);
            if (read != LOG_ENTRY_BACK_LINK_LEN) {
                throw new LogException("Unable to read journal entry back-link!");
            }
            header.flip();
            final short backLink = header.getShort();

            // position the channel to the start of the previous entry and mark it
            final long prevStart = fc.position() - LOG_ENTRY_BACK_LINK_LEN - backLink;
            fc.position(prevStart);
            final Loggable loggable = readEntry();

            // reset to the mark
            fc.position(prevStart);
            return loggable;
        } catch (final IOException e) {
            throw new LogException("Fatal error while reading previous journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the last entry in the journal.
     *
     * @return the last entry in the journal, or null if there are no entries in the journal.
     * @throws LogException if an entry could not be read due to an inconsistency on disk.
     */
    public @Nullable
    Loggable lastEntry() throws LogException {
        try {
            checkOpen();
            positionLast();
            return previousEntry();
        } catch (final IOException e) {
            throw new LogException("Fatal error while reading last journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Read the current entry from the journal.
     *
     * @return The entry, or null if there is no entry.
     * @throws LogException if an entry could not be read due to an inconsistency on disk.
     */
    private @Nullable
    Loggable readEntry() throws LogException {
        try {
            final Lsn lsn = new Lsn(fileNumber, fc.position() + 1);

            // read the entry header
            header.clear();
            int read = fc.read(header);
            if (read <= 0) {
                return null;
            }
            if (read != LOG_ENTRY_HEADER_LEN) {
                throw new LogException("Incomplete journal entry header found, expected  "
                        + LOG_ENTRY_HEADER_LEN + " bytes, but found " + read + " bytes");
            }
            header.flip();

            // prepare the checksum for the header
            xxHash64.reset();
            if (header.hasArray()) {
                xxHash64.update(header.array(), 0, LOG_ENTRY_HEADER_LEN);
            } else {
                final int mark = header.position();
                header.position(0);
                final byte buf[] = new byte[LOG_ENTRY_HEADER_LEN];
                header.get(buf);
                xxHash64.update(buf, 0, LOG_ENTRY_HEADER_LEN);
                header.position(mark);
            }

            final byte entryType = header.get();
            final long transactId = header.getLong();
            final short size = header.getShort();
            if (fc.position() + size > fc.size()) {
                throw new LogException("Invalid length");
            }

            final Loggable loggable = LogEntryTypes.create(entryType, broker, transactId);
            if (loggable == null) {
                throw new LogException("Invalid log entry: " + entryType + "; size: " + size + "; id: "
                        + transactId + "; at: " + lsn);
            }
            loggable.setLsn(lsn);

            final int remainingEntryBytes = size + LOG_ENTRY_BACK_LINK_LEN + LOG_ENTRY_CHECKSUM_LEN;

            if (remainingEntryBytes > payload.capacity()) {
                // resize the payload buffer
                payload = ByteBuffer.allocateDirect(remainingEntryBytes);
            }
            payload.clear().limit(remainingEntryBytes);
            read = fc.read(payload);
            if (read < remainingEntryBytes) {
                throw new LogException("Incomplete log entry found!");
            }
            payload.flip();

            // read entry data
            loggable.read(payload);

            // read entry backLink
            final short backLink = payload.getShort();
            if (backLink != size + LOG_ENTRY_HEADER_LEN) {
                LOG.error("Bad pointer to previous: backLink = {}; size = {}; transactId = {}", backLink, size, transactId);
                throw new LogException("Bad pointer to previous in entry: " + loggable.dump());
            }

            // update the checksum for the entry data and backLink
            if (payload.hasArray()) {
                xxHash64.update(payload.array(), 0, size + LOG_ENTRY_BACK_LINK_LEN);
            } else {
                final int mark = payload.position();
                payload.position(0);
                final byte buf[] = new byte[size + LOG_ENTRY_BACK_LINK_LEN];
                payload.get(buf);
                xxHash64.update(buf, 0, size + LOG_ENTRY_BACK_LINK_LEN);
                payload.position(mark);
            }

            // read the entry checksum
            final long checksum = payload.getLong();

            // verify the checksum
            final long calculatedChecksum = xxHash64.getValue();
            if (checksum != calculatedChecksum) {
                throw new LogException("Checksum mismatch whilst reading log entry. read=" + checksum + " calculated=" + calculatedChecksum);
            }

            return loggable;
        } catch (final IOException e) {
            throw new LogException(e.getMessage(), e);
        }
    }

    /**
     * Re-position the file position so it points to the start of the entry
     * with the given LSN.
     *
     * @param lsn the log sequence number
     * @throws LogException if the journal file cannot be re-positioned
     */
    public void position(final Lsn lsn) throws LogException {
        try {
            checkOpen();
            fc.position(lsn.getOffset() - 1);
        } catch (final IOException e) {
            throw new LogException("Fatal error while seeking journal: " + e.getMessage(), e);
        }
    }

    /**
     * Re-position the file position so it points to the first entry.
     *
     * @throws LogException if the journal file cannot be re-positioned
     */
    public void positionFirst() throws LogException {
        try {
            checkOpen();
            fc.position(JOURNAL_HEADER_LEN);
        } catch (final IOException e) {
            throw new LogException("Fatal error while seeking first journal entry: " + e.getMessage(), e);
        }
    }

    /**
     * Re-position the file position so it points to the last entry.
     *
     * @throws LogException if the journal file cannot be re-positioned
     */
    public void positionLast() throws LogException {
        try {
            checkOpen();
            fc.position(fc.size());
        } catch (final IOException e) {
            throw new LogException("Fatal error while seeking last journal entry: " + e.getMessage(), e);
        }
    }

    private void checkOpen() throws IOException {
        if (fc == null) {
            throw new IOException("Journal file is closed");
        }
    }

    @Override
    public void close() {
        try {
            if (fc != null) {
                fc.close();
            }
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
        fc = null;
    }
}
