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
package org.exist.source;


import com.googlecode.junittoolbox.ParallelRunner;
import org.exist.EXistException;
import org.exist.dom.persistent.BinaryDocument;
import org.exist.dom.persistent.LockedDocument;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
class SourceFactoryTest {

    @Test
    void getSourceFromFile_contextAbsoluteFileUrl_locationAbsoluteUrl() throws IOException, PermissionDeniedException, URISyntaxException {
        final URL mainUrl = getClass().getResource("main.xq");
        final String contextPath = mainUrl.toString();
        final URL libraryUrl = getClass().getResource("library.xqm");
        final String location = libraryUrl.toString();

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(FileSource.class, source);
        assertEquals(new java.io.File(libraryUrl.toURI()).getAbsolutePath(), source.path());
    }

    @Test
    void getSourceFromFile_contextAbsoluteFile_locationAbsoluteFile() throws IOException, PermissionDeniedException, URISyntaxException {
        final URL mainUrl = getClass().getResource("main.xq");
        final String contextPath = Paths.get(mainUrl.toURI()).toAbsolutePath().toString();
        final URL libraryUrl = getClass().getResource("library.xqm");
        final String location = Paths.get(libraryUrl.toURI()).toAbsolutePath().toString();

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(FileSource.class, source);
        assertEquals(new java.io.File(libraryUrl.toURI()).getAbsolutePath(), source.path());
    }

    @Test
    void getSourceFromFile_contextAbsoluteFileUrl_locationRelative() throws IOException, PermissionDeniedException, URISyntaxException {
        final URL mainUrl = getClass().getResource("main.xq");
        final String contextPath = mainUrl.toString();
        final String location = "library.xqm";

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(FileSource.class, source);
        assertEquals(new java.io.File(getClass().getResource("library.xqm").toURI()).getAbsolutePath(), source.path());
    }

    @Test
    void getSourceFromFile_contextAbsoluteFile_locationRelative() throws IOException, PermissionDeniedException, URISyntaxException {
        final URL mainUrl = getClass().getResource("main.xq");
        final String contextPath = Paths.get(mainUrl.toURI()).toAbsolutePath().toString();
        final String location = "library.xqm";

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(FileSource.class, source);
        assertEquals(new java.io.File(getClass().getResource("library.xqm").toURI()).getAbsolutePath(), source.path());
    }

    @Test
    void getSourceFromFile_contextAbsoluteDir_locationRelative() throws IOException, PermissionDeniedException, URISyntaxException {
        final URL mainUrl = getClass().getResource("main.xq");
        final String contextPath = Paths.get(mainUrl.toURI()).getParent().toString();
        //final String contextPath = mainParent.substring(0, mainParent.lastIndexOf('/'));
        final String location = "library.xqm";

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(FileSource.class, source);
        assertEquals(Paths.get(getClass().getResource("library.xqm").toURI()).toString(), source.path());
    }

    @Test
    void getSourceFromResource_contextAbsoluteFileUrl_locationRelative() throws IOException, PermissionDeniedException {
        final String contextPath = "resource:org/exist/source/main.xq";
        final String location = "library.xqm";

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(ClassLoaderSource.class, source);
        assertEquals(getClass().getResource("library.xqm").getFile(), source.path());
    }

    @Test
    void getSourceFromResource_contextAbsoluteFileUrl_locationAbsoluteUrl() throws IOException, PermissionDeniedException {
        final String contextPath = "resource:org/exist/source/main.xq";
        final String location = "resource:org/exist/source/library.xqm";

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(ClassLoaderSource.class, source);
        assertEquals(getClass().getResource("library.xqm").getFile(), source.path());
    }

    @Test
    void getSourceFromResource_contextAbsoluteFileUrl_locationRelativeUrl() throws IOException, PermissionDeniedException {
        final String contextPath = "resource:org/exist/source/main.xq";
        final String location = "library.xqm";

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(ClassLoaderSource.class, source);
        assertEquals(getClass().getResource("library.xqm").getFile(), source.path());
    }

    @Test
    void getSourceFromResource_contextAbsoluteFileUrl_locationRelativeUrl_basedOnSource() throws IOException, PermissionDeniedException {
        final String contextPath = "resource:org/exist/source/main.xq";
        final String location = "library.xqm";

        final Source mainSource = SourceFactory.getSource(null, "", contextPath, false);
        assertInstanceOf(ClassLoaderSource.class, mainSource);

        final Source relativeSource = SourceFactory.getSource(null, ((ClassLoaderSource)mainSource).getSource(), location, false);

        assertInstanceOf(ClassLoaderSource.class, relativeSource);
        assertEquals(getClass().getResource(location).getFile(), relativeSource.path());
    }

    @Test
    void getSourceFromResource_contextFolderUrl_locationRelative() throws IOException, PermissionDeniedException {
        final String contextPath = "resource:org/exist/source";
        final String location = "library.xqm";

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(ClassLoaderSource.class, source);
        assertEquals(getClass().getResource("library.xqm").getFile(), source.path());
    }

    @Test
    void getSourceFromResource_contextFolderUrl_locationAbsoluteUrl() throws IOException, PermissionDeniedException {
        final String contextPath = "resource:org/exist/source";
        final String location = "resource:org/exist/source/library.xqm";

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(ClassLoaderSource.class, source);
        assertEquals(getClass().getResource("library.xqm").getFile(), source.path());
    }

    @Test
    void getSourceFromResource_contextFolderUrl_locationRelativeUrl() throws IOException, PermissionDeniedException {
        final String contextPath = "resource:org/exist/source";
        final String location = "library.xqm";

        final Source source = SourceFactory.getSource(null, contextPath, location, false);

        assertInstanceOf(ClassLoaderSource.class, source);
        assertEquals(getClass().getResource("library.xqm").getFile(), source.path());
    }

    @Test
    void getSourceFromResource_contextFolderUrl_locationRelativeUrl_basedOnSource() throws IOException, PermissionDeniedException {
        final String contextPath = "resource:org/exist/source";
        final String location = "library.xqm";

        final Source mainSource = SourceFactory.getSource(null, "", contextPath, false);
        assertInstanceOf(ClassLoaderSource.class, mainSource);

        final Source relativeSource = SourceFactory.getSource(null, ((ClassLoaderSource)mainSource).getSource(), location, false);

        assertInstanceOf(ClassLoaderSource.class, relativeSource);
        assertEquals(getClass().getResource(location).getFile(), relativeSource.path());
    }

    @Test
    void getSourceFromXmldb_noContext() throws IOException, PermissionDeniedException {
        final String contextPath = null;
        final String location = "xmldb:exist:///db/library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        final LockedDocument mockLockedDoc = createMock(LockedDocument.class);
        final BinaryDocument mockBinDoc = createMock(BinaryDocument.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(mockLockedDoc);
        expect(mockLockedDoc.getDocument()).andReturn(mockBinDoc);
        expect(mockBinDoc.getLastModified()).andReturn(123456789l);
        /*expect*/ mockTxn.commit();
        /*expect*/ mockLockedDoc.close();
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertInstanceOf(DbUriSource.class, libSource);
        assertEquals(XmldbURI.create(location), ((DBUriSource)libSource).getDocumentPath());

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);
    }

    @Test
    void getSourceFromXmldb() throws IOException, PermissionDeniedException {
        final String contextPath = "xmldb:exist:///db";
        final String location = "library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        final LockedDocument mockLockedDoc = createMock(LockedDocument.class);
        final BinaryDocument mockBinDoc = createMock(BinaryDocument.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(mockLockedDoc);
        expect(mockLockedDoc.getDocument()).andReturn(mockBinDoc);
        expect(mockBinDoc.getLastModified()).andReturn(123456789l);
        /*expect*/ mockTxn.commit();
        /*expect*/ mockLockedDoc.close();
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker,mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertInstanceOf(DbUriSource.class, libSource);
        assertEquals(XmldbURI.create(contextPath).append(location), ((DbUriSource)libSource).getDocumentPath());

        verify(mockBrokerPool, mockBroker,mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);
    }

    @Test
    void getNonExistentSourceFromXmldb_noContext() throws IOException, PermissionDeniedException {
        final String contextPath = null;
        final String location = "xmldb:exist:///db/library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(null);
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertNull(libSource);

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);
    }

    @Test
    void getNonExistentSourceFromXmldb() throws IOException, PermissionDeniedException {
        final String contextPath = "xmldb:exist:///db";
        final String location = "library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(null);
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertNull(libSource);

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);
    }

    @Test
    void getSourceFromXmldbEmbedded_noContext() throws IOException, PermissionDeniedException {
        final String contextPath = null;
        final String location = "xmldb:exist://embedded-eXist-server/db/library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        final LockedDocument mockLockedDoc = createMock(LockedDocument.class);
        final BinaryDocument mockBinDoc = createMock(BinaryDocument.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(mockLockedDoc);
        expect(mockLockedDoc.getDocument()).andReturn(mockBinDoc);
        expect(mockBinDoc.getLastModified()).andReturn(123456789l);
        /*expect*/ mockTxn.commit();
        /*expect*/ mockLockedDoc.close();
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertInstanceOf(DbUriSource.class, libSource);
        assertEquals(XmldbURI.create(location), ((DbUriSource)libSource).getDocumentPath());

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);
    }

    @Test
    void getSourceFromXmldbEmbedded() throws IOException, PermissionDeniedException {
        final String contextPath = "xmldb:exist://embedded-eXist-server/db";
        final String location = "library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        final LockedDocument mockLockedDoc = createMock(LockedDocument.class);
        final BinaryDocument mockBinDoc = createMock(BinaryDocument.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(mockLockedDoc);
        expect(mockLockedDoc.getDocument()).andReturn(mockBinDoc);
        expect(mockBinDoc.getLastModified()).andReturn(123456789l);
        /*expect*/ mockTxn.commit();
        /*expect*/ mockLockedDoc.close();
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertInstanceOf(DbUriSource.class, libSource);
        assertEquals(XmldbURI.create(contextPath).append(location), ((DbUriSource)libSource).getDocumentPath());

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);
    }

    @Test
    void getNonExistentSourceFromXmldbEmbedded_noContext() throws IOException, PermissionDeniedException {
        final String contextPath = null;
        final String location = "xmldb:exist://embedded-eXist-server/db/library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(null);
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertNull(libSource);

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);
    }

    @Test
    void getNonExistentSourceFromXmldbEmbedded() throws IOException, PermissionDeniedException {
        final String contextPath = "xmldb:exist://embedded-eXist-server/db";
        final String location = "library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(null);
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertNull(libSource);

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);
    }

    @Test
    void getSourceFromDb() throws IOException, PermissionDeniedException {
        final String contextPath = "/db";
        final String location = "library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        final LockedDocument mockLockedDoc = createMock(LockedDocument.class);
        final BinaryDocument mockBinDoc = createMock(BinaryDocument.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(mockLockedDoc);
        expect(mockLockedDoc.getDocument()).andReturn(mockBinDoc);
        expect(mockBinDoc.getLastModified()).andReturn(123456789l);
        /*expect*/ mockTxn.commit();
        /*expect*/ mockLockedDoc.close();
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertInstanceOf(DbUriSource.class, libSource);
        assertEquals(XmldbURI.create(contextPath).append(location), ((DbUriSource)libSource).getDocumentPath());

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);
    }

    @Test
    void getSourceFromDb_noContext() throws IOException, PermissionDeniedException {
        final String contextPath = null;
        final String location = "/db/library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        final LockedDocument mockLockedDoc = createMock(LockedDocument.class);
        final BinaryDocument mockBinDoc = createMock(BinaryDocument.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(mockLockedDoc);
        expect(mockLockedDoc.getDocument()).andReturn(mockBinDoc);
        expect(mockBinDoc.getLastModified()).andReturn(123456789l);
        /*expect*/ mockTxn.commit();
        /*expect*/ mockLockedDoc.close();
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertInstanceOf(DbUriSource.class, libSource);
        assertEquals(XmldbURI.create(location), ((DbUriSource)libSource).getDocumentPath());

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn, mockLockedDoc, mockBinDoc);
    }

    @Test
    void getNonExistentSourceFromDb() throws IOException, PermissionDeniedException {
        final String contextPath = "/db";
        final String location = "library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(null);
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertNull(libSource);

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);
    }

    @Test
    void getNonExistentSourceFromDb_noContext() throws IOException, PermissionDeniedException {
        final String contextPath = null;
        final String location = "/db/library.xqm";

        final BrokerPool mockBrokerPool = createMock(BrokerPool.class);
        final DBBroker mockBroker = createMock(DBBroker.class);
        final TransactionManager mockTransactionManager = createMock(TransactionManager.class);
        final Txn mockTxn = createMock(Txn.class);
        expect(mockBroker.getBrokerPool()).andReturn(mockBrokerPool);
        expect(mockBrokerPool.get(Optional.empty())).andReturn(mockBroker);
        expect(mockBrokerPool.getTransactionManager()).andReturn(mockTransactionManager);
        expect(mockTransactionManager.beginTransaction()).andReturn(mockTxn);
        expect(mockBroker.getXMLResource(anyObject(), anyObject())).andReturn(null);
        /*expect*/ mockTxn.close();
        /*expect*/ mockBroker.close();

        replay(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);

        final Source libSource = SourceFactory.getSource(mockBroker, contextPath, location, false);
        assertNull(libSource);

        verify(mockBrokerPool, mockBroker, mockTransactionManager, mockTxn);
    }

    @Test
    void getSource_justFilename() throws IOException, PermissionDeniedException {
        final String contextPath = null;
        final String location = "library.xqm";

        final Source mainSource = SourceFactory.getSource(null, contextPath, location, false);
        assertNull(mainSource);
    }
}
