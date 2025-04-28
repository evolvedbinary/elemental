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
package org.exist.xquery;

import com.evolvedbinary.j8fu.function.Function2E;
import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.dom.persistent.BinaryDocument;
import org.exist.security.PermissionDeniedException;
import org.exist.source.DBSource;
import org.exist.source.Source;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.XQueryPool;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.value.Sequence;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
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
            final DBSource mainQuerySource = storeQuery(broker, transaction, mainQueryUri, mainQuery);

            final XQueryContext escapedMainQueryContext = withCompiledQuery(broker, mainQuerySource, mainCompiledQuery -> {
                final XQueryContext mainQueryContext = mainCompiledQuery.getContext();

                mainQueryContext.setAttribute("attr1", "value1");
                mainQueryContext.setAttribute("attr2", "value2");

                // execute the query
                final Sequence result = executeQuery(broker, mainCompiledQuery);
                assertEquals(1, result.getItemCount());

                // intentionally escape the context from the lambda
                return mainQueryContext;
            });

            assertNull(escapedMainQueryContext.getAttribute("attr1"));
            assertNull(escapedMainQueryContext.getAttribute("attr2"));
            assertTrue(escapedMainQueryContext.attributes.isEmpty());

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
            final DBSource mainQuerySource = storeQuery(broker, transaction, mainQueryUri, mainQuery);

            final Tuple2<XQueryContext, ModuleContext> escapedContexts = withCompiledQuery(broker, mainQuerySource, mainCompiledQuery -> {
                final XQueryContext mainQueryContext = mainCompiledQuery.getContext();

                // get the context of the library module
                final Module[] libraryModules = mainQueryContext.getModules("http://mod1");
                assertEquals(1, libraryModules.length);
                assertTrue(libraryModules[0] instanceof ExternalModule);
                final ExternalModule libraryModule = (ExternalModule) libraryModules[0];
                final XQueryContext libraryQueryContext = libraryModule.getContext();
                assertTrue(libraryQueryContext instanceof ModuleContext);

                libraryQueryContext.setAttribute("attr1", "value1");
                libraryQueryContext.setAttribute("attr2", "value2");

                // execute the query
                final Sequence result = executeQuery(broker, mainCompiledQuery);
                assertEquals(1, result.getItemCount());

                // intentionally escape the contexts from the lambda
                return Tuple(mainQueryContext, (ModuleContext) libraryQueryContext);
            });

            final XQueryContext escapedMainQueryContext = escapedContexts._1;
            final ModuleContext escapedLibraryQueryContext = escapedContexts._2;
            assertTrue(escapedMainQueryContext != escapedLibraryQueryContext);

            assertNull(escapedMainQueryContext.getAttribute("attr1"));
            assertNull(escapedMainQueryContext.getAttribute("attr2"));
            assertTrue(escapedMainQueryContext.attributes.isEmpty());

            assertNull(escapedLibraryQueryContext.getAttribute("attr1"));
            assertNull(escapedLibraryQueryContext.getAttribute("attr2"));
            assertTrue(escapedLibraryQueryContext.attributes.isEmpty());

            transaction.commit();
        }
    }

    private static DBSource storeQuery(final DBBroker broker, final Txn transaction, final XmldbURI uri, final InputSource source) throws IOException, PermissionDeniedException, SAXException, LockException, EXistException {
        try (final Collection collection = broker.openCollection(uri.removeLastSegment(), Lock.LockMode.WRITE_LOCK)) {
            broker.storeDocument(transaction, uri.lastSegment(), source, MimeType.XQUERY_TYPE, collection);
            final BinaryDocument doc = (BinaryDocument) collection.getDocument(broker, uri.lastSegment());

            return new DBSource(broker.getBrokerPool(), doc, false);
        }
    }

    private static <T> T withCompiledQuery(final DBBroker broker, final Source source, final Function2E<CompiledXQuery, T, XPathException, PermissionDeniedException> op) throws XPathException, PermissionDeniedException, IOException {
        final BrokerPool pool = broker.getBrokerPool();
        final XQuery xqueryService = pool.getXQueryService();
        final XQueryPool xqueryPool = pool.getXQueryPool();
        final CompiledXQuery compiledQuery = compileQuery(broker, xqueryService, xqueryPool, source);
        try {
            return op.apply(compiledQuery);
        } finally {
            if (compiledQuery != null) {
                xqueryPool.returnCompiledXQuery(source, compiledQuery);
            }
        }
    }

    private static CompiledXQuery compileQuery(final DBBroker broker, final XQuery xqueryService, final XQueryPool xqueryPool, final Source query) throws PermissionDeniedException, XPathException, IOException {
        CompiledXQuery compiled = xqueryPool.borrowCompiledXQuery(broker, query);
        XQueryContext context;
        if (compiled == null) {
            context = new XQueryContext(broker.getBrokerPool());
        } else {
            context = compiled.getContext();
            context.prepareForReuse();
        }

        if (compiled == null) {
            compiled = xqueryService.compile(context, query);
        } else {
            compiled.getContext().updateContext(context);
            context.getWatchDog().reset();
        }

        return compiled;
    }

    static Sequence executeQuery(final DBBroker broker, final CompiledXQuery compiledXQuery) throws PermissionDeniedException, XPathException {
        final BrokerPool pool = broker.getBrokerPool();
        final XQuery xqueryService = pool.getXQueryService();
        return xqueryService.execute(broker, compiledXQuery, null, new Properties());
    }
}
