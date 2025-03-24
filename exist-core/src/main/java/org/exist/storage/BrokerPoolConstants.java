/*
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
package org.exist.storage;

import org.exist.util.Str;

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

    Str DATA_DIR_ATTRIBUTE = Str.of("files");

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
    Str PROPERTY_STARTUP_TRIGGERS = Str.of("startup.triggers");
    Str PROPERTY_DATA_DIR = Str.of("db-connection.data-dir");
    Str PROPERTY_MIN_CONNECTIONS = Str.of("db-connection.pool.min");
    Str PROPERTY_MAX_CONNECTIONS = Str.of("db-connection.pool.max");
    Str PROPERTY_SYNC_PERIOD = Str.of("db-connection.pool.sync-period");
    Str PROPERTY_SHUTDOWN_DELAY = Str.of("wait-before-shutdown");
    Str DISK_SPACE_MIN_PROPERTY = Str.of("db-connection.diskSpaceMin");

    //TODO : move elsewhere ?
    Str PROPERTY_COLLECTION_CACHE_SIZE = Str.of("db-connection.collection-cache-size");

    //TODO : move elsewhere ? Get fully qualified class name ?
    Str PROPERTY_RECOVERY_ENABLED = Str.of("db-connection.recovery.enabled");
    Str PROPERTY_RECOVERY_CHECK = Str.of("db-connection.recovery.consistency-check");
    Str PROPERTY_SYSTEM_TASK_CONFIG = Str.of("db-connection.system-task-config");
    Str PROPERTY_NODES_BUFFER = Str.of("db-connection.nodes-buffer");
    Str PROPERTY_EXPORT_ONLY = Str.of("db-connection.emergency");

    Str PROPERTY_RECOVERY_GROUP_COMMIT = Str.of("db-connection.recovery.group-commit");
    String RECOVERY_GROUP_COMMIT_ATTRIBUTE = "group-commit";
    Str PROPERTY_RECOVERY_FORCE_RESTART = Str.of("db-connection.recovery.force-restart");
    String RECOVERY_FORCE_RESTART_ATTRIBUTE = "force-restart";

    Str PROPERTY_PAGE_SIZE = Str.of("db-connection.page-size");

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
