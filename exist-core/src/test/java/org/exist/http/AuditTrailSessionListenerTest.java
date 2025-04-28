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
package org.exist.http;

import com.googlecode.junittoolbox.ParallelRunner;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.triggers.TriggerException;
import org.exist.dom.persistent.LockedDocument;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.lock.LockManager;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;

import java.io.IOException;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;

@RunWith(ParallelRunner.class)
public class AuditTrailSessionListenerTest {

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    private static final XmldbURI TEST_COLLECTION = XmldbURI.create("/db/test");
    private static final String CREATE_SCRIPT = "session-create.xq";
    private static final String DESTROYED_SCRIPT = "session-destroyed.xq";
    private static final String CREATE_SCRIPT_PATH = "/db/test/" + CREATE_SCRIPT;
    private static final String DESTROYED_SCRIPT_PATH = "/db/test/" + DESTROYED_SCRIPT;

    /**
     * Ensures that AuditTrailSessionListener releases any locks
     * on the XQuery document when creating a session
     */
    @Test
    public void sessionCreated() throws EXistException, PermissionDeniedException {
        final HttpSessionEvent httpSessionEvent = createMock(HttpSessionEvent.class);
        final HttpSession httpSession = createMock(HttpSession.class);
        expect(httpSessionEvent.getSession()).andReturn(httpSession);
        expect(httpSession.getId()).andReturn("mock-session");

        replay(httpSessionEvent, httpSession);

        final AuditTrailSessionListener listener = new AuditTrailSessionListener();
        listener.sessionCreated(httpSessionEvent);

        verify(httpSessionEvent, httpSession);

        final XmldbURI docUri = XmldbURI.create(CREATE_SCRIPT_PATH);
        try(final DBBroker broker = existEmbeddedServer.getBrokerPool().getBroker();
                final LockedDocument lockedResource = broker.getXMLResource(docUri, Lock.LockMode.NO_LOCK)) {

            // ensure that AuditTrailSessionListener released the lock
            final LockManager lockManager = broker.getBrokerPool().getLockManager();
            assertFalse(lockManager.isDocumentLockedForRead(docUri));
            assertFalse(lockManager.isDocumentLockedForWrite(docUri));
        }
    }

    /**
     * Ensures that AuditTrailSessionListener releases any locks
     * on the XQuery document when destroying a session
     */
    @Test
    public void sessionDestroyed() throws EXistException, PermissionDeniedException {
        final HttpSessionEvent httpSessionEvent = createMock(HttpSessionEvent.class);
        final HttpSession httpSession = createMock(HttpSession.class);
        expect(httpSessionEvent.getSession()).andReturn(httpSession);
        expect(httpSession.getId()).andReturn("mock-session");

        replay(httpSessionEvent, httpSession);

        final AuditTrailSessionListener listener = new AuditTrailSessionListener();
        listener.sessionDestroyed(httpSessionEvent);

        verify(httpSessionEvent, httpSession);

        final XmldbURI docUri = XmldbURI.create(DESTROYED_SCRIPT_PATH);
        try(final DBBroker broker = existEmbeddedServer.getBrokerPool().getBroker();
                final LockedDocument lockedResource = broker.getXMLResource(docUri, Lock.LockMode.NO_LOCK)) {

            // ensure that AuditTrailSessionListener released the lock
            final LockManager lockManager = broker.getBrokerPool().getLockManager();
            assertFalse(lockManager.isDocumentLockedForRead(docUri));
            assertFalse(lockManager.isDocumentLockedForWrite(docUri));
        }
    }

    @BeforeClass
    public static void setup() throws EXistException, LockException, SAXException, PermissionDeniedException, IOException {
        storeScripts();
        System.setProperty(AuditTrailSessionListener.REGISTER_CREATE_XQUERY_SCRIPT_PROPERTY, CREATE_SCRIPT_PATH);
        System.setProperty(AuditTrailSessionListener.REGISTER_DESTROY_XQUERY_SCRIPT_PROPERTY, DESTROYED_SCRIPT_PATH);
    }

    private static void storeScripts() throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        try(final DBBroker broker = existEmbeddedServer.getBrokerPool().get(Optional.of(existEmbeddedServer.getBrokerPool().getSecurityManager().getSystemSubject()));
                final Txn transaction = existEmbeddedServer.getBrokerPool().getTransactionManager().beginTransaction()) {

            final Collection testCollection = broker.getOrCreateCollection(transaction, TEST_COLLECTION);
            broker.storeDocument(transaction, XmldbURI.create(CREATE_SCRIPT), new StringInputSource("<create/>".getBytes(UTF_8)), MimeType.XQUERY_TYPE, testCollection);
            broker.storeDocument(transaction, XmldbURI.create(DESTROYED_SCRIPT), new StringInputSource("</destroyed>".getBytes(UTF_8)), MimeType.XQUERY_TYPE, testCollection);

            transaction.commit();
        }
    }

    @AfterClass
    public static void teardown() throws TriggerException, PermissionDeniedException, EXistException, IOException {
        System.clearProperty(AuditTrailSessionListener.REGISTER_CREATE_XQUERY_SCRIPT_PROPERTY);
        System.clearProperty(AuditTrailSessionListener.REGISTER_DESTROY_XQUERY_SCRIPT_PROPERTY);
        removeScripts();
    }

    private static void removeScripts() throws EXistException, PermissionDeniedException, IOException, TriggerException {
        try(final DBBroker broker = existEmbeddedServer.getBrokerPool().get(Optional.of(existEmbeddedServer.getBrokerPool().getSecurityManager().getSystemSubject()));
                final Txn transaction = existEmbeddedServer.getBrokerPool().getTransactionManager().beginTransaction()) {
            final Collection testCollection = broker.getCollection(TEST_COLLECTION);
            if(testCollection != null) {
                broker.removeCollection(transaction, testCollection);
            }
            transaction.commit();
        }
    }
}
