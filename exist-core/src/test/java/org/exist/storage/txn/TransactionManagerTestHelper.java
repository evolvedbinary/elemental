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
package org.exist.storage.txn;

import org.exist.EXistException;
import org.exist.security.SecurityManager;
import org.exist.security.Subject;
import org.exist.storage.BrokerPool;
import org.exist.storage.NativeBroker;
import org.exist.storage.SystemTaskManager;
import org.exist.storage.journal.JournalManager;

import java.util.Optional;

import static org.easymock.EasyMock.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class TransactionManagerTestHelper {

    BrokerPool mockBrokerPool = null;
    NativeBroker mockBroker = null;

    protected TransactionManager createTestableTransactionManager(final boolean expectTxnClose) throws NoSuchFieldException, IllegalAccessException, EXistException {
        mockBrokerPool = createMock(BrokerPool.class);
        mockBroker = createMock(NativeBroker.class);
        expect(mockBrokerPool.getBroker()).andReturn(mockBroker).atLeastOnce();
        mockBroker.addCurrentTransaction(anyObject());
        expectLastCall().atLeastOnce();
        if (expectTxnClose) {
            mockBroker.removeCurrentTransaction(anyObject());
            expectLastCall().atLeastOnce();
        }
        mockBroker.close();
        expectLastCall().atLeastOnce();
        final SecurityManager mockSecurityManager = createMock(SecurityManager.class);
        final Subject mockSystemSubject = createMock(Subject.class);
        expect(mockBrokerPool.get(Optional.of(mockSystemSubject))).andReturn(mockBroker).anyTimes();
        expect(mockBrokerPool.getSecurityManager()).andReturn(mockSecurityManager).anyTimes();
        expect(mockSecurityManager.getSystemSubject()).andReturn(mockSystemSubject).anyTimes();

        final JournalManager mockJournalManager = createMock(JournalManager.class);
        final SystemTaskManager mockTaskManager = createMock(SystemTaskManager.class);

        replay(mockBrokerPool, mockBroker, mockSecurityManager);

        return new TransactionManager(mockBrokerPool, Optional.of(mockJournalManager), mockTaskManager);
    }

    protected void verifyMocks() {
	verify(mockBrokerPool, mockBroker);
    }
}
