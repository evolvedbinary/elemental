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

import org.exist.start.CompatibleJavaVersionCheck;
import org.exist.start.StartException;
import org.exist.util.SystemExitCodes;
import org.exist.util.crypto.digest.DigestType;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.READ;
import static org.exist.storage.blob.BlobStoreImpl.BLOB_STORE_HEADER_LEN;
import static org.exist.storage.blob.BlobStoreImpl.BLOB_STORE_MAGIC_NUMBER;
import static org.exist.storage.blob.BlobStoreImpl.REFERENCE_COUNT_LEN;
import static se.softhouse.jargo.Arguments.enumArgument;
import static se.softhouse.jargo.Arguments.fileArgument;
import static se.softhouse.jargo.Arguments.helpArgument;

/**
 * Simple tool for dumping the content
 * of the Blob Store in CSV format for the
 * purposes of debugging or visualisation.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class BlobStoreDumpTool {

    private static final Argument<?> helpArg = helpArgument("-h", "--help");
    private static final Argument<DigestType> digestArg = enumArgument(DigestType.class, "-d", "--digest")
            .description("The digest type used by the Blob Store")
            .defaultValue(DigestType.BLAKE_256)
            .build();
    private static final Argument<Path> persistentFileArg = fileArgument()
            .description("Path to the Blob Store persistent file e.g. blob.dbx")
            .required()
            .transform(java.io.File::toPath)
            .build();

    public static void main(final String args[]) throws IOException {
        try {
            CompatibleJavaVersionCheck.checkForCompatibleJavaVersion();

            final ParsedArguments arguments = CommandLineParser
                    .withArguments(digestArg, persistentFileArg)
                    .andArguments(helpArg)
                    .parse(args);

            final DigestType digestType = arguments.get(digestArg);
            final Path persistentFile = arguments.get(persistentFileArg);

            final PrintStream printStream = System.out;

            dump(digestType, persistentFile, printStream);
        } catch (final StartException e) {
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                System.err.println(e.getMessage());
            }
            System.exit(e.getErrorCode());
        } catch(final ArgumentException e) {
            System.out.println(e.getMessageAndUsage());
            System.exit(SystemExitCodes.INVALID_ARGUMENT_EXIT_CODE);
        }
    }

    /**
     * Dumps the content of the Blob Store persistent file.
     *
     * @param digestType the digest type used by the Blob Store
     * @param persistentFile the Blob Store persistent file to dump
     * @param printStream the stream to dump the Blob Store to
     *
     * @throws IOException if an I/O error occurs whilst reading the Blob Store's persistent file.
     */
    public static void dump(final DigestType digestType, final Path persistentFile, final PrintStream printStream)
            throws IOException {
        final int digestBytesLen = digestType.getDigestLengthBytes();

        final ByteBuffer buffer = ByteBuffer.allocate(digestBytesLen + REFERENCE_COUNT_LEN);

        try (final SeekableByteChannel channel = Files.newByteChannel(persistentFile, READ)) {

            // dump the file header details
            dumpFileHeader(printStream, buffer, persistentFile, channel);
            printStream.println("Digest Type: " + digestType.name());
            printStream.println();

            // write a CSV table header
            printStream.println("blobId,referenceCount");

            buffer.clear();

            final byte[] id = new byte[digestBytesLen];

            while (channel.read(buffer) > -1) {
                buffer.flip();
                buffer.get(id);
                final BlobId blobId = new BlobId(id);
                final int count = buffer.getInt();

                printStream.println(blobId + "," + count);

                buffer.clear();
            }
        }
    }

    /**
     * Dump the File Header of the Blob Store
     *
     * @param printStream the stream to dump the Blob Store to
     * @param buffer a buffer to use
     * @param persistentFile the Blob Store persistent file to dump
     * @param channel the open channel for reading the {@code persistentFile}.
     *
     * @throws IOException if an I/O error occurs whilst reading the Blob Store's persistent file.
     */
    private static void dumpFileHeader(final PrintStream printStream, final ByteBuffer buffer, final Path persistentFile,
            final SeekableByteChannel channel) throws IOException {
        buffer.clear();
        buffer.limit(BLOB_STORE_HEADER_LEN);

        channel.read(buffer);

        buffer.flip();

        final boolean validMagic =
                buffer.get() == BLOB_STORE_MAGIC_NUMBER[0]
                        && buffer.get() == BLOB_STORE_MAGIC_NUMBER[1]
                        && buffer.get() == BLOB_STORE_MAGIC_NUMBER[2]
                        && buffer.get() == BLOB_STORE_MAGIC_NUMBER[3];

        if (!validMagic) {
            throw new IOException("File was not recognised as a valid Elemental Blob Store: "
                    + persistentFile.toAbsolutePath().toString());
        }

        // check the version of the blob store format
        final short storedVersion = buffer.getShort();
        printStream.println("*** Blob Store file format version: " + storedVersion);

        // TODO(AR) validate that we can read this format version
    }
}
