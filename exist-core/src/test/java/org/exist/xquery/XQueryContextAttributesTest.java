/*
 * Copyright (C) 2014, Evolved Binary Ltd
 *
 * This file was originally ported from FusionDB to eXist-db by
 * Evolved Binary, for the benefit of the eXist-db Open Source community.
 * Only the ported code as it appears in this file, at the time that
 * it was contributed to eXist-db, was re-licensed under The GNU
 * Lesser General Public License v2.1 only for use in eXist-db.
 *
 * This license grant applies only to a snapshot of the code as it
 * appeared when ported, it does not offer or infer any rights to either
 * updates of this source code or access to the original source code.
 *
 * The GNU Lesser General Public License v2.1 only license follows.
 *
 * =====================================================================
 *
 * Copyright (C) 2014, Evolved Binary Ltd
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
 */
package org.exist.xquery;

import com.evolvedbinary.j8fu.function.BiConsumerE;
import com.evolvedbinary.j8fu.function.ConsumerE;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.persistent.BinaryDocument;
import org.exist.security.PermissionDeniedException;
import org.exist.source.DbUriSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.Holder;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.value.Sequence;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import java.io.IOException;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class XQueryContextAttributesTest {

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @Test
    public void attributesOfMainModuleContextCleared() throws EXistException, LockException, SAXException, PermissionDeniedException, IOException, XPathException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
            final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final XmldbURI mainQueryUri = XmldbURI.create("/db/query1.xq");
            final InputSource mainQuery = new StringInputSource("<not-important/>".getBytes(UTF_8));
            final DbUriSource mainQuerySource = storeQuery(broker, transaction, mainQueryUri, mainQuery);

            // set some attributes on the context before query execution
            final ConsumerE<XQueryContext, XPathException> preExecutionContext = xQueryContext -> {
                xQueryContext.setAttribute("attr1", "value1");
                xQueryContext.setAttribute("attr2", "value2");
            };

            // will hold whether the context attributes is empty once the query has finished executing
            final Holder<Boolean> attributesIsEmptyHolder = new Holder<>();
            final BiConsumerE<XQueryContext, XQueryUtil.QueryResult, XPathException> postExecutionContext = (xqueryContext, result) -> {
                attributesIsEmptyHolder.value = xqueryContext.attributes.isEmpty();
            };

            // execute the query
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, mainQuerySource, false, null, null, null, preExecutionContext, postExecutionContext)) {
                final Sequence result = queryResult.result;
                assertEquals(1, result.getItemCount());
            }

            // now the query has finished executing and been reset, check the attributes map is empty for the Main Module
            assertTrue(attributesIsEmptyHolder.value);

            transaction.commit();
        }
    }

    @Test
    public void attributesOfLibraryModuleContextCleared() throws EXistException, LockException, SAXException, PermissionDeniedException, IOException, XPathException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final XmldbURI libraryQueryUri = XmldbURI.create("/db/mod1.xqm");
            final InputSource libraryQuery = new StringInputSource(
                    ("module namespace mod1 = 'http://mod1';\n" +
                    "declare function mod1:f1() { <not-important/> };").getBytes(UTF_8)
            );
            storeQuery(broker, transaction, libraryQueryUri, libraryQuery);

            final XmldbURI mainQueryUri = XmldbURI.create("/db/query1.xq");
            final InputSource mainQuery = new StringInputSource(
                    ("import module namespace mod1 = 'http://mod1' at 'xmldb:exist://" + libraryQueryUri + "';\n" +
                    "mod1:f1()").getBytes(UTF_8)
            );
            final DbUriSource mainQuerySource = storeQuery(broker, transaction, mainQueryUri, mainQuery);


            // set some attributes on the context in the Library Module before query execution
            final ConsumerE<XQueryContext, XPathException> preExecutionContext = mainModuleXqueryContext -> {
                final Module[] libraryModules = mainModuleXqueryContext.getModules("http://mod1");
                final ExternalModule libraryModule = (ExternalModule) libraryModules[0];
                final XQueryContext libraryModuleXqueryContext = libraryModule.getContext();
                libraryModuleXqueryContext.setAttribute("attr1", "value1");
                libraryModuleXqueryContext.setAttribute("attr2", "value2");
            };

            // will hold whether the context attributes in the Main Module is empty once the query has finished executing
            final Holder<Boolean> mainModuleAttributesIsEmptyHolder = new Holder<>();
            // will hold whether the context attributes in the Library Module is empty once the query has finished executing
            final Holder<Boolean> libraryModuleAttributesIsEmptyHolder = new Holder<>();
            final BiConsumerE<XQueryContext, XQueryUtil.QueryResult, XPathException> postExecutionContext = (mainModuleXqueryContext, result) -> {
                mainModuleAttributesIsEmptyHolder.value = mainModuleXqueryContext.attributes.isEmpty();

                final Module[] libraryModules = mainModuleXqueryContext.getModules("http://mod1");
                final ExternalModule libraryModule = (ExternalModule) libraryModules[0];
                final XQueryContext libraryModuleXqueryContext = libraryModule.getContext();
                libraryModuleAttributesIsEmptyHolder.value = libraryModuleXqueryContext.attributes.isEmpty();
            };

            // execute the query
            try (final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, mainQuerySource, false, null, null, null, preExecutionContext, postExecutionContext)) {
                final Sequence result = queryResult.result;
                assertEquals(1, result.getItemCount());
            }

            // now the query has finished executing and been reset, check the attributes map is empty for the Main Module
            assertTrue(mainModuleAttributesIsEmptyHolder.value);

            // now the query has finished executing and been reset, check the attributes map is empty for the Library Module
            assertTrue(libraryModuleAttributesIsEmptyHolder.value);

            transaction.commit();
        }
    }

    private static DbUriSource storeQuery(final DBBroker broker, final Txn transaction, final XmldbURI uri, final InputSource source) throws IOException, PermissionDeniedException, SAXException, LockException, EXistException {
        try (final Collection collection = broker.openCollection(uri.removeLastSegment(), Lock.LockMode.WRITE_LOCK)) {
            final MediaType xqueryMediaType = broker.getBrokerPool().getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XQUERY);
            broker.storeDocument(transaction, uri.lastSegment(), source, xqueryMediaType, collection);
            final BinaryDocument doc = (BinaryDocument) collection.getDocument(broker, uri.lastSegment());
            return DbUriSource.from(broker.getBrokerPool(), broker.getCurrentSubject(), doc, false, false);
        }
    }
}
