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

import org.exist.util.Prop;

/**
 * Just static Constants used by {@link BrokerPool}
 *
 * We keep these here to reduce the visual
 * complexity of the BrokerPool class
 */
public interface BrokerPoolConstants {

    //on-start, ready, go
    /*** initializing sub-components */
    Prop SIGNAL_STARTUP = Prop.of("startup");
    /*** ready for recovery &amp; read-only operations */
    Prop SIGNAL_READINESS = Prop.of("ready");
    /*** ready for writable operations */
    Prop SIGNAL_WRITABLE = Prop.of("writable");
    /*** ready for writable operations */
    Prop SIGNAL_STARTED = Prop.of("started");
    /*** running shutdown sequence */
    Prop SIGNAL_SHUTDOWN = Prop.of("shutdown");
    /*** recovery aborted, db stopped */
    Prop SIGNAL_ABORTED = Prop.of("aborted");

    Prop CONFIGURATION_CONNECTION_ELEMENT_NAME = Prop.of("db-connection");
    Prop CONFIGURATION_STARTUP_ELEMENT_NAME = Prop.of("startup");
    Prop CONFIGURATION_POOL_ELEMENT_NAME = Prop.of("pool");
    Prop CONFIGURATION_RECOVERY_ELEMENT_NAME = Prop.of("recovery");
    Prop DISK_SPACE_MIN_ATTRIBUTE = Prop.of("minDiskSpace");

    Prop DATA_DIR_ATTRIBUTE = Prop.of("files");

    //TODO : move elsewhere ?
    Prop RECOVERY_ENABLED_ATTRIBUTE = Prop.of("enabled");
    Prop RECOVERY_POST_RECOVERY_CHECK = Prop.of("consistency-check");

    //TODO : move elsewhere ?
    Prop COLLECTION_CACHE_SIZE_ATTRIBUTE = Prop.of("collectionCacheSize");
    Prop MIN_CONNECTIONS_ATTRIBUTE = Prop.of("min");
    Prop MAX_CONNECTIONS_ATTRIBUTE = Prop.of("max");
    Prop SYNC_PERIOD_ATTRIBUTE = Prop.of("sync-period");
    Prop SHUTDOWN_DELAY_ATTRIBUTE = Prop.of("wait-before-shutdown");
    Prop NODES_BUFFER_ATTRIBUTE = Prop.of("nodesBuffer");

    //Various configuration property keys (set by the configuration manager)
    Prop PROPERTY_STARTUP_TRIGGERS = Prop.of("startup.triggers");
    Prop PROPERTY_DATA_DIR = Prop.of("db-connection.data-dir");
    Prop PROPERTY_MIN_CONNECTIONS = Prop.of("db-connection.pool.min");
    Prop PROPERTY_MAX_CONNECTIONS = Prop.of("db-connection.pool.max");
    Prop PROPERTY_SYNC_PERIOD = Prop.of("db-connection.pool.sync-period");
    Prop PROPERTY_SHUTDOWN_DELAY = Prop.of("wait-before-shutdown");
    Prop DISK_SPACE_MIN_PROPERTY = Prop.of("db-connection.diskSpaceMin");

    //TODO : move elsewhere ?
    Prop PROPERTY_COLLECTION_CACHE_SIZE = Prop.of("db-connection.collection-cache-size");

    //TODO : move elsewhere ? Get fully qualified class name ?
    Prop PROPERTY_RECOVERY_ENABLED = Prop.of("db-connection.recovery.enabled");
    Prop PROPERTY_RECOVERY_CHECK = Prop.of("db-connection.recovery.consistency-check");
    Prop PROPERTY_SYSTEM_TASK_CONFIG = Prop.of("db-connection.system-task-config");
    Prop PROPERTY_NODES_BUFFER = Prop.of("db-connection.nodes-buffer");
    Prop PROPERTY_EXPORT_ONLY = Prop.of("db-connection.emergency");

    Prop PROPERTY_RECOVERY_GROUP_COMMIT = Prop.of("db-connection.recovery.group-commit");
    Prop RECOVERY_GROUP_COMMIT_ATTRIBUTE = Prop.of("group-commit");
    Prop PROPERTY_RECOVERY_FORCE_RESTART = Prop.of("db-connection.recovery.force-restart");
    Prop RECOVERY_FORCE_RESTART_ATTRIBUTE = Prop.of("force-restart");

    Prop PROPERTY_PAGE_SIZE = Prop.of("db-connection.page-size");

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
