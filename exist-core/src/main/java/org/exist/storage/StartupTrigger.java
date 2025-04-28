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

import org.exist.storage.txn.Txn;

import java.util.List;
import java.util.Map;

/**
 * Database Startup Trigger
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface StartupTrigger {
    
    /**
     * Synchronously execute a task at database Startup before the database is made available to connections
     * Remember, your code within the execute function will block the database startup until it completes!
     *
     * Any RuntimeExceptions thrown will be ignored and database startup will continue
     * Database Startup cannot be aborted by this Trigger!
     * 
     * Note: If you want an Asynchronous Trigger, you could use a Future in your implementation
     * to start a new thread, however you cannot access the sysBroker from that thread
     * as it may have been returned to the broker pool. Instead if you need a broker, you may be able to
     * do something clever by checking the database status and then acquiring a new broker
     * from the broker pool. If you wish to work with the broker pool you must obtain this before
     * starting your asynchronous execution by calling sysBroker.getBrokerPool().
     * 
     * @param sysBroker the single system broker available during database startup
     * @param transaction the database transaction to participate in
     * @param params Key, Values
     */
    public void execute(final DBBroker sysBroker, final Txn transaction, final Map<String, List<? extends Object>> params);
}
