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
package org.exist.xquery.modules.file;

import com.evolvedbinary.j8fu.function.Consumer2E;
import org.exist.xmldb.XmldbURI;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.test.TestConstants.TEST_COLLECTION_URI;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for accessing binaries using XQuery via various APIs.
 *
 * @see <a href="https://github.com/eXist-db/exist/issues/790">Binary streaming is broken</a>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractBinariesTest<T, U, E extends Exception> {

    protected static final XmldbURI TEST_COLLECTION = TEST_COLLECTION_URI.append("BinariesTest");
    protected static final String BIN1_FILENAME = "1.bin";
    protected static final byte[] BIN1_CONTENT = "1234567890".getBytes(UTF_8);

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        storeBinaryFile(TEST_COLLECTION.append(BIN1_FILENAME), BIN1_CONTENT);
    }

    @After
    public void cleanup() throws Exception {
        removeCollection(TEST_COLLECTION);
    }

    /**
     * {@see https://github.com/eXist-db/exist/issues/790#error-case-3}
     */
    @Test
    public void readBinary() throws Exception {
        final byte[] data = randomData(1024 * 1024 * 10);  // 10KB
        final Path tmpFile = createTemporaryFile(data);

        final String query = "import module namespace file = \"http://exist-db.org/xquery/file\";\n" +
                "file:read-binary('" + tmpFile.toAbsolutePath().toString() + "')";

        final QueryResultAccessor<T, E> resultsAccessor = executeXQuery(query);

        resultsAccessor.accept(results -> {
            assertEquals(1, size(results));

            final U item = item(results, 0);
            assertTrue(isBinaryType(item));
            assertArrayEquals(data, getBytes(item));
        });
    }

    /**
     * {@see https://github.com/eXist-db/exist/issues/790#error-case-4}
     */
    @Test
    public void readAndWriteBinary() throws Exception {
        final byte[] data = randomData(1024 * 1024);  // 1MB
        final Path tmpInFile = createTemporaryFile(data);

        final Path tmpOutFile = temporaryFolder.newFile().toPath();

        final String query = "import module namespace file = \"http://exist-db.org/xquery/file\";\n" +
                "let $bin := file:read-binary('" +  tmpInFile.toAbsolutePath().toString() + "')\n" +
                "return\n" +
                "    file:serialize-binary($bin, '" + tmpOutFile.toAbsolutePath().toString() + "')";

        final QueryResultAccessor<T, E> resultsAccessor = executeXQuery(query);

        resultsAccessor.accept(results -> {
            assertEquals(1, size(results));

            final U item = item(results, 0);
            assertTrue(isBooleanType(item));
            assertEquals(true, getBoolean(item));
        });

        assertArrayEquals(Files.readAllBytes(tmpInFile), Files.readAllBytes(tmpOutFile));
    }

    protected byte[] randomData(final int size) {
        final byte data[] = new byte[size];
        new Random().nextBytes(data);
        return data;
    }

    protected Path createTemporaryFile(final byte[] data) throws IOException {
        final Path f = temporaryFolder.newFile().toPath();
        Files.write(f, data);
        return f;
    }

    @FunctionalInterface interface QueryResultAccessor<T, E extends Exception> extends Consumer2E<Consumer2E<T, AssertionError, E>, AssertionError, E> {
    }

    protected abstract void storeBinaryFile(final XmldbURI filePath, final byte[] content) throws Exception;
    protected abstract void removeCollection(final XmldbURI collectionUri) throws Exception;
    protected abstract QueryResultAccessor<T, E> executeXQuery(final String query) throws Exception;
    protected abstract long size(T results) throws E;
    protected abstract U item(T results, int index) throws E;
    protected abstract boolean isBinaryType(U item) throws E;
    protected abstract boolean isBooleanType(U item) throws E;
    protected abstract byte[] getBytes(U item) throws E;
    protected abstract boolean getBoolean(U item) throws E;
}
