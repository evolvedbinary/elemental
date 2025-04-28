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
package org.exist.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.io.input.CountingInputStream;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.persistent.BinaryDocument;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.txn.Txn;
import org.exist.util.FileInputSource;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.xmldb.XmldbURI;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class RecoverBinaryTest extends AbstractRecoverTest {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private static Path testFile1 = null;
    private static Path testFile2 = null;

    @BeforeClass
    public static void storeTempBinaryDocs() throws IOException {
        testFile1 = temporaryFolder.getRoot().toPath().resolve("blob1.bin");
        Files.write(testFile1, Arrays.asList("blob1"), CREATE_NEW);

        testFile2 = temporaryFolder.getRoot().toPath().resolve("blob2.bin");
        Files.write(testFile2, Arrays.asList("blob2"), CREATE_NEW);
    }

    @Override
    protected Path getTestFile1() throws IOException {
        return testFile1;
    }

    @Override
    protected Path getTestFile2() throws IOException {
        return testFile2;
    }

    @Override
    protected void storeAndVerify(final DBBroker broker, final Txn transaction, final Collection collection,
            final InputSource data, final String dbFilename) throws EXistException, PermissionDeniedException, IOException,
            SAXException, LockException {

        final Path file = ((FileInputSource)data).getFile();

        broker.storeDocument(transaction, XmldbURI.create(dbFilename), new FileInputSource(file), MimeType.BINARY_TYPE, collection);
        final BinaryDocument doc = (BinaryDocument) collection.getDocument(broker, XmldbURI.create(dbFilename));

        assertNotNull(doc);
        assertEquals(Files.size(file), doc.getContentLength());
    }

    @Override
    protected void readAndVerify(final DBBroker broker, final DocumentImpl doc, final InputSource data,
            final String dbFilename) throws IOException {

        final Path file = ((FileInputSource)data).getFile();

        final BinaryDocument binDoc = (BinaryDocument)doc;

        // verify the size, to ensure it is the correct content
        final long expectedSize = Files.size(file);
        assertEquals(expectedSize, binDoc.getContentLength());

        // check the actual content too!
        final byte[] bdata = new byte[(int) binDoc.getContentLength()];
        try (final CountingInputStream cis = new CountingInputStream(broker.getBinaryResource(binDoc))) {
            final int read = cis.read(bdata);
            assertEquals(bdata.length, read);

            final String content = new String(bdata);
            assertNotNull(content);

            assertEquals(expectedSize, cis.getByteCount());
        }
    }
}
