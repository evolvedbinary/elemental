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

import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.security.Permission;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.journal.Journal;
import org.exist.storage.txn.Txn;
import org.exist.util.*;
import org.exist.xmldb.XmldbURI;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class RecoverXmlTest extends AbstractRecoverTest {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private static Path testFile1 = null;
    private static Path testFile2 = null;

    @BeforeClass
    public static void storeTempXmlDocs() throws IOException {
        testFile1 = temporaryFolder.getRoot().toPath().resolve("RecoverXmlTest.doc1.xml");
        Files.write(testFile1, Arrays.asList("<?xml version=\"1.0\" encoding=\"UTF-8\"?><element1>text1</element1>"), CREATE_NEW);

        testFile2 = temporaryFolder.getRoot().toPath().resolve("RecoverXmlTest.doc2.xml");
        Files.write(testFile2, Arrays.asList("<?xml version=\"1.0\" encoding=\"UTF-8\"?><element2>text2</element2>"), CREATE_NEW);
    }

    @Test
    public void storeLargeAndLoad() throws LockException, SAXException, PermissionDeniedException, EXistException,
            IOException, DatabaseConfigurationException, InterruptedException {
        // generate a string filled with random a-z characters which is larger than the journal buffer
        final byte[] buf = new byte[Journal.BUFFER_SIZE * 3]; // 3 * the journal buffer size
        final Random random = new Random();
        for (int i = 0; i < buf.length; i++) {
            final byte singleByteChar = (byte)('a' + random.nextInt('z' - 'a' - 1));
            buf[i] = singleByteChar;
        }
        final String largeText = new String(buf, UTF_8);
        final String xml = "<large-text>" + largeText + "</large-text>";
        final InputSource source = new StringInputSource(xml);
        source.setEncoding("UTF-8");

        BrokerPool.FORCE_CORRUPTION = true;
        store(COMMIT, source, "large.xml");
        flushJournal();

        existEmbeddedServer.restart();

        BrokerPool.FORCE_CORRUPTION = false;
        read(MUST_EXIST, source, "large.xml");
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
            final InputSource data, final String dbFilename) throws EXistException, PermissionDeniedException,
            IOException, LockException {
        final XmldbURI docUri = XmldbURI.create(dbFilename);
        try {
            broker.storeDocument(transaction, docUri, data, MimeType.XML_TYPE, collection);

        } catch (final SAXException e) {
            throw new IOException(e);
        }


        final DocumentImpl doc = broker.getResource(collection.getURI().append(docUri), Permission.READ);
        assertNotNull(doc);

        final Source expected;
        if (data instanceof FileInputSource) {
            expected = Input.fromFile(((FileInputSource)data).getFile().toFile()).build();
        } else if(data instanceof StringInputSource) {
            try (final Reader reader = data.getCharacterStream()) {
                expected = Input.fromString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + readAll(reader)).build();
            }
        } else {
            throw new IllegalStateException();
        }
        final Source actual = Input.fromDocument(doc).build();

        final Diff diff = DiffBuilder.compare(expected).withTest(actual)
                .checkForIdentical()
                .build();

        assertFalse("XML identical: " + diff.toString(), diff.hasDifferences());
    }

    private final String readAll(final Reader reader) throws IOException {
        final StringBuilder builder = new StringBuilder();
        final char buf[] = new char[4096];
        int read = -1;
        while ((read = reader.read(buf)) > -1) {
            builder.append(buf, 0, read);
        }
        return builder.toString();
    }

    @Override
    protected void readAndVerify(final DBBroker broker, final DocumentImpl doc, final InputSource data,
            final String dbFilename) throws IOException {

        final Source expected;
        if (data instanceof FileInputSource) {
            expected = Input.fromFile(((FileInputSource)data).getFile().toFile()).build();
        } else if(data instanceof StringInputSource) {
            try (final Reader reader = data.getCharacterStream()) {
                expected = Input.fromString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + readAll(reader)).build();
            }
        } else {
            throw new IllegalStateException();
        }

        final Source actual = Input.fromDocument(doc).build();

        final Diff diff = DiffBuilder.compare(expected).withTest(actual)
                .checkForIdentical()
                .build();

        assertFalse("XML identical: " + diff.toString(), diff.hasDifferences());
    }
}
