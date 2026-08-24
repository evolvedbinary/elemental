/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.dom.persistent;

import org.exist.EXistException;
import org.exist.collections.triggers.TriggerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import javax.xml.transform.OutputKeys;

import org.exist.collections.Collection;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock.LockMode;
import org.exist.storage.serializers.EXistOutputKeys;
import org.exist.storage.serializers.Serializer;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.util.*;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.w3c.dom.DocumentType;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests basic DOM methods like getChildNodes(), getAttribute() ...
 * 
 * @author wolf
 *
 */
@Execution(ExecutionMode.CONCURRENT)
public class DocTypeTest {

	public final static Properties OUTPUT_PROPERTIES = new Properties();
    static {
    	OUTPUT_PROPERTIES.setProperty(OutputKeys.INDENT, "no");
    	OUTPUT_PROPERTIES.setProperty(OutputKeys.ENCODING, "UTF-8");
    	OUTPUT_PROPERTIES.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    	OUTPUT_PROPERTIES.setProperty(EXistOutputKeys.OUTPUT_DOCTYPE, "yes");
    	OUTPUT_PROPERTIES.setProperty(EXistOutputKeys.EXPAND_XINCLUDES, "no");
    	OUTPUT_PROPERTIES.setProperty(EXistOutputKeys.PROCESS_XSL_PI, "no");
    }
	
	private static final String XML =
		"<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"http://docs.oasis-open.org/dita/v1.2/os/dtd1.2/technicalContent/dtd/topic.dtd\">" +
		"<!-- doc starts here -->" +
        "<topic >" +
		"	<title>abc</title>" +
		"	<shortdesc>def</shortdesc>" +
        "   <body>ghi</body>" +
		"</topic>";

	private static Collection root = null;

    @Test
    void docType_usingInputSource(final BrokerPool pool) throws EXistException, URISyntaxException, LockException, SAXException, PermissionDeniedException, IOException {
		final TransactionManager transact = pool.getTransactionManager();

		try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            final URL testFileUrl = getClass().getResource("test_content.xml");
			final Path testFile = Paths.get(testFileUrl.toURI());
			assertTrue(Files.isReadable(testFile));
			
			final InputSource is = new FileInputSource(testFile);

			try(final Txn transaction = transact.beginTransaction()) {
                final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
                broker.storeDocument(transaction, XmldbURI.create("test2.xml"), is, xmlMediaType, root);

                transact.commit(transaction);
            }

			try(final LockedDocument lockedDoc = broker.getXMLResource(root.getURI().append(XmldbURI.create("test2.xml")),LockMode.READ_LOCK)) {
			    final DocumentImpl doc = lockedDoc.getDocument();
                final DocumentType docType = doc.getDoctype();
                assertNotNull(docType);
                assertEquals("-//OASIS//DTD DITA Reference//EN", docType.getPublicId());

                final Serializer serializer = broker.borrowSerializer();
                try {
                    serializer.setProperties(OUTPUT_PROPERTIES);
                    final String serialized = serializer.serialize(doc);
                    assertTrue(serialized.contains("-//OASIS//DTD DITA Reference//EN"), "Checking for Public Id in output");
                } finally {
                    broker.returnSerializer(serializer);
                }
            }
		}
	}

    @Test
    void docType_usingString(final BrokerPool pool) throws EXistException, PermissionDeniedException, SAXException {
		try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final LockedDocument lockedDoc = broker.getXMLResource(root.getURI().append(XmldbURI.create("test.xml")),LockMode.READ_LOCK)) {
            final DocumentImpl doc = lockedDoc.getDocument();

            DocumentType docType = doc.getDoctype();

            assertNotNull(docType);

            assertEquals("-//OASIS//DTD DITA Topic//EN", docType.getPublicId());

            final Serializer serializer = broker.borrowSerializer();
            try {
                serializer.setProperties(OUTPUT_PROPERTIES);
                String serialized = serializer.serialize(doc);
                assertTrue(serialized.contains("-//OASIS//DTD DITA Topic//EN"), "Checking for Public Id in output");
            } finally {
                broker.returnSerializer(serializer);
            }

        }
	}

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, true);

    @BeforeAll
    static void setUp(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, SAXException, LockException, DatabaseConfigurationException {
	    final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = transact.beginTransaction()) {
            
            root = broker.getOrCreateCollection(transaction, XmldbURI.create(XmldbURI.ROOT_COLLECTION + "/test"));
            assertNotNull(root);
            broker.saveCollection(transaction, root);

            final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
            broker.storeDocument(transaction, XmldbURI.create("test.xml"), new StringInputSource(XML), xmlMediaType, root);
            //TODO : unlock the collection here ?
            
            transact.commit(transaction);
        }
	}

    @AfterAll
    static void tearDown(final BrokerPool pool) throws PermissionDeniedException, IOException, TriggerException, EXistException {
	    final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = transact.beginTransaction()) {
            
            root = broker.getOrCreateCollection(transaction, XmldbURI.create(XmldbURI.ROOT_COLLECTION + "/test"));
            assertNotNull(root);
            broker.removeCollection(transaction, root);
            
            transact.commit(transaction);
        }
    }
}
