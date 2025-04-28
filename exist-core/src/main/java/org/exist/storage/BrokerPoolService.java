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
import org.exist.util.Configuration;

/**
 * Interface for a class which provides
 * services to a BrokerPool instance
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface BrokerPoolService {

    /**
     * Configure this service
     *
     * By default there is nothing to configure.
     *
     * @param configuration BrokerPool configuration
     *
     * @throws BrokerPoolServiceException if an error occurs when configuring the service
     */
    default void configure(final Configuration configuration) throws BrokerPoolServiceException {
        //nothing to configure
    }

    /**
     * Prepare this service
     *
     * Prepare is called before the BrokerPool enters
     * system (single user) mode. As yet there are still
     * no brokers
     *
     * @param brokerPool The BrokerPool instance that is being prepared
     *
     * @throws BrokerPoolServiceException if an error occurs when preparing the service
     */
    default void prepare(final BrokerPool brokerPool) throws BrokerPoolServiceException {
        //nothing to prepare
    }

    /**
     * Start any part of this service that should happen before
     * system (single-user) mode.
     *
     * As this point the database is not generally available
     * and the only system broker is passed to this function
     *
     * @param systemBroker The system mode broker
     * @param transaction The transaction for the system service
     *
     * @throws BrokerPoolServiceException if an error occurs when starting the pre-system service
     */
    default void startPreSystem(final DBBroker systemBroker, final Txn transaction) throws BrokerPoolServiceException {
        // nothing to start
    }

    /**
     * Start any part of this service that should happen during
     * system (single-user) mode.
     *
     * As this point the database is not generally available
     * and the only system broker is passed to this function
     *
     * @param systemBroker The system mode broker
     * @param transaction The transaction for the system service
     *
     * @throws BrokerPoolServiceException if an error occurs when starting the system service
     */
    default void startSystem(final DBBroker systemBroker, final Txn transaction) throws BrokerPoolServiceException {
        // nothing to start
    }

    /**
     * Start any part of this service that should happen at the
     * end of system (single-user) mode and directly before multi-user
     * mode
     *
     * As this point the database is not generally available,
     * {@link #startSystem(DBBroker, Txn)} has already been called
     * for all services, any reindexing and recovery has completed
     * but there is still only a system broker which is passed to this
     * function
     *
     * @param systemBroker The system mode broker
     * @param transaction The transaction for the pre-multi-user system service
     *
     * @throws BrokerPoolServiceException if an error occurs when starting the pre-multi-user system service
     */
    default void startPreMultiUserSystem(final DBBroker systemBroker, final Txn transaction) throws BrokerPoolServiceException {
        //nothing to start
    }

    /**
     * Start any part of this service that should happen at the
     * start of multi-user mode.
     *
     * As this point the database is generally available,
     * {@link #startPreMultiUserSystem(DBBroker, Txn)} has already been called
     * for all services. You may be competing with other services and/or
     * users for database access
     *
     * @param brokerPool The multi-user available broker pool instance
     *
     * @throws BrokerPoolServiceException if an error occurs when starting the multi-user service
     */
    default void startMultiUser(final BrokerPool brokerPool) throws BrokerPoolServiceException {
        //nothing to start
    }

    /**
     * Stop any part of this service that should happen at the
     * end of multi-user mode.
     *
     * As this point the database is about to shutdown but has not yet
     * transitioned to single-user mode.
     * You may still be competing with other services and/or
     * users for database access.
     *
     * @param brokerPool The multi-user available broker pool instance
     *
     * @throws BrokerPoolServiceException if an error occurs when stopping the multi-user service
     */
    default void stopMultiUser(final BrokerPool brokerPool) throws BrokerPoolServiceException {
        //nothing to stop
    }

    /**
     * Stop any part of this service that should happen during
     * system (single-user) mode.
     *
     * By default there is nothing to stop
     *
     * As this point the database is not generally available
     * and the only system broker is passed to this function
     *
     * @param systemBroker The system mode broker
     *
     * @throws BrokerPoolServiceException if an error occurs when stopping the service
     *
     * @deprecated Use {@link #stopSystem(DBBroker)} instead.
     */
    @Deprecated
    default void stop(final DBBroker systemBroker) throws BrokerPoolServiceException {
        stopSystem(systemBroker);
    }

    /**
     * Stop any part of this service that should happen during
     * system (single-user) mode.
     *
     * By default there is nothing to stop
     *
     * As this point the database is not generally available
     * and the only system broker is passed to this function
     *
     * @param systemBroker The system mode broker
     *
     * @throws BrokerPoolServiceException if an error occurs when stopping the service
     */
    default void stopSystem(final DBBroker systemBroker) throws BrokerPoolServiceException {
        //nothing to actually stop
    }

    /**
     * Shutdown this service.
     *
     * By default there is nothing to shutdown
     */
    default void shutdown() {
        //nothing to actually shutdown
    }
}
