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

/**
 * Just static Constants used by {@link BrokerPool}
 *
 * We keep these here to reduce the visual
 * complexity of the BrokerPool class
 */
public interface BrokerPoolConstants {

    //on-start, ready, go
    /*** initializing sub-components */
    String SIGNAL_STARTUP = "startup";
    /*** ready for recovery &amp; read-only operations */
    String SIGNAL_READINESS = "ready";
    /*** ready for writable operations */
    String SIGNAL_WRITABLE = "writable";
    /*** ready for writable operations */
    String SIGNAL_STARTED = "started";
    /*** running shutdown sequence */
    String SIGNAL_SHUTDOWN = "shutdown";
    /*** recovery aborted, db stopped */
    String SIGNAL_ABORTED = "aborted";

    String CONFIGURATION_CONNECTION_ELEMENT_NAME = "db-connection";
    String CONFIGURATION_STARTUP_ELEMENT_NAME = "startup";
    String CONFIGURATION_POOL_ELEMENT_NAME = "pool";
    String CONFIGURATION_RECOVERY_ELEMENT_NAME = "recovery";
    String DISK_SPACE_MIN_ATTRIBUTE = "minDiskSpace";

    String DATA_DIR_ATTRIBUTE = "files";

    //TODO : move elsewhere ?
    String RECOVERY_ENABLED_ATTRIBUTE = "enabled";
    String RECOVERY_POST_RECOVERY_CHECK = "consistency-check";

    //TODO : move elsewhere ?
    String COLLECTION_CACHE_SIZE_ATTRIBUTE = "collectionCacheSize";
    String MIN_CONNECTIONS_ATTRIBUTE = "min";
    String MAX_CONNECTIONS_ATTRIBUTE = "max";
    String SYNC_PERIOD_ATTRIBUTE = "sync-period";
    String SHUTDOWN_DELAY_ATTRIBUTE = "wait-before-shutdown";
    String NODES_BUFFER_ATTRIBUTE = "nodesBuffer";

    //Various configuration property keys (set by the configuration manager)
    String PROPERTY_STARTUP_TRIGGERS = "startup.triggers";
    String PROPERTY_DATA_DIR = "db-connection.data-dir";
    String PROPERTY_MIN_CONNECTIONS = "db-connection.pool.min";
    String PROPERTY_MAX_CONNECTIONS = "db-connection.pool.max";
    String PROPERTY_SYNC_PERIOD = "db-connection.pool.sync-period";
    String PROPERTY_SHUTDOWN_DELAY = "wait-before-shutdown";
    String DISK_SPACE_MIN_PROPERTY = "db-connection.diskSpaceMin";

    //TODO : move elsewhere ?
    String PROPERTY_COLLECTION_CACHE_SIZE = "db-connection.collection-cache-size";

    //TODO : move elsewhere ? Get fully qualified class name ?
    String PROPERTY_RECOVERY_ENABLED = "db-connection.recovery.enabled";
    String PROPERTY_RECOVERY_CHECK = "db-connection.recovery.consistency-check";
    String PROPERTY_SYSTEM_TASK_CONFIG = "db-connection.system-task-config";
    String PROPERTY_NODES_BUFFER = "db-connection.nodes-buffer";
    String PROPERTY_EXPORT_ONLY = "db-connection.emergency";

    String PROPERTY_RECOVERY_GROUP_COMMIT = "db-connection.recovery.group-commit";
    String RECOVERY_GROUP_COMMIT_ATTRIBUTE = "group-commit";
    String PROPERTY_RECOVERY_FORCE_RESTART = "db-connection.recovery.force-restart";
    String RECOVERY_FORCE_RESTART_ATTRIBUTE = "force-restart";

    String PROPERTY_PAGE_SIZE = "db-connection.page-size";

    /**
     * Default values
     */
    long DEFAULT_SYNCH_PERIOD = 120000;
    long DEFAULT_MAX_SHUTDOWN_WAIT = 45000;
    //TODO : move this default setting to org.exist.collections.CollectionCache ?
    int DEFAULT_COLLECTION_BUFFER_SIZE = 64;
    int DEFAULT_PAGE_SIZE = 4096;
    short DEFAULT_DISK_SPACE_MIN = 64; // 64 MB
}
