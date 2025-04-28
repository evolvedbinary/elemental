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

import com.googlecode.junittoolbox.ParallelRunner;
import org.exist.EXistException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@RunWith(ParallelRunner.class)
public class TxnTest {

    final TransactionManagerTestHelper helper = new TransactionManagerTestHelper();

    @Test
    public void commitTransaction() throws NoSuchFieldException, IllegalAccessException, EXistException {
        final TransactionManager transact = helper.createTestableTransactionManager(false);

        final Txn transaction = transact.beginTransaction();

        final CountingTxnListener listener = new CountingTxnListener();
        transaction.registerListener(listener);

        transaction.commit();

        assertEquals(Txn.State.COMMITTED, transaction.getState());

        assertEquals(1, listener.getCommit());
        assertEquals(0, listener.getAbort());

        helper.verifyMocks();
    }

    @Test
    public void commitAndCloseTransaction() throws NoSuchFieldException, IllegalAccessException, EXistException {
        final TransactionManager transact = helper.createTestableTransactionManager(true);

        final Txn transaction = transact.beginTransaction();

        final CountingTxnListener listener = new CountingTxnListener();
        transaction.registerListener(listener);

        transaction.commit();
        transaction.close();

        assertEquals(Txn.State.CLOSED, transaction.getState());

        assertEquals(1, listener.getCommit());
        assertEquals(0, listener.getAbort());

        helper.verifyMocks();
    }

    @Test
    public void abortTransaction() throws NoSuchFieldException, IllegalAccessException, EXistException {
        final TransactionManager transact = helper.createTestableTransactionManager(false);

        final Txn transaction = transact.beginTransaction();

        final CountingTxnListener listener = new CountingTxnListener();
        transaction.registerListener(listener);

        transaction.abort();

        assertEquals(Txn.State.ABORTED, transaction.getState());

        assertEquals(0, listener.getCommit());
        assertEquals(1, listener.getAbort());

        helper.verifyMocks();
    }

    @Test
    public void abortAndCloseTransaction() throws NoSuchFieldException, IllegalAccessException, EXistException {
        final TransactionManager transact = helper.createTestableTransactionManager(true);

        final Txn transaction = transact.beginTransaction();

        final CountingTxnListener listener = new CountingTxnListener();
        transaction.registerListener(listener);

        transaction.abort();
        transaction.close();

        assertEquals(Txn.State.CLOSED, transaction.getState());

        assertEquals(0, listener.getCommit());
        assertEquals(1, listener.getAbort());

        helper.verifyMocks();
    }

    @Test
    public void repeatedAbortOnlyAbortsTransactionOnce() throws NoSuchFieldException, IllegalAccessException, EXistException {
        final TransactionManager transact = helper.createTestableTransactionManager(false);

        final Txn transaction = transact.beginTransaction();

        final CountingTxnListener listener = new CountingTxnListener();
        transaction.registerListener(listener);

        //call 3 times, abort count should be one!
        transaction.abort();
        transaction.abort();
        transaction.abort();

        assertEquals(Txn.State.ABORTED, transaction.getState());

        assertEquals(0, listener.getCommit());
        assertEquals(1, listener.getAbort());

        helper.verifyMocks();
    }

    @Test
    public void closeWithoutCommitAbortsTransaction() throws NoSuchFieldException, IllegalAccessException, EXistException {
        final TransactionManager transact = helper.createTestableTransactionManager(true);

        final Txn transaction = transact.beginTransaction();

        final CountingTxnListener listener = new CountingTxnListener();
        transaction.registerListener(listener);

        transaction.close();

        assertEquals(Txn.State.CLOSED, transaction.getState());

        assertEquals(0, listener.getCommit());
        assertEquals(1, listener.getAbort());

        helper.verifyMocks();
    }

    @Test
    public void repeatedCloseWithoutCommitOnlyAbortsTransactionOnce() throws NoSuchFieldException, IllegalAccessException, EXistException {
        final TransactionManager transact = helper.createTestableTransactionManager(true);

        final Txn transaction = transact.beginTransaction();

        final CountingTxnListener listener = new CountingTxnListener();
        transaction.registerListener(listener);

        //call 3 times, abort count should be one!
        transaction.close();
        transaction.close();
        transaction.close();

        assertEquals(Txn.State.CLOSED, transaction.getState());

        assertEquals(0, listener.getCommit());
        assertEquals(1, listener.getAbort());

        helper.verifyMocks();
    }
}
