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
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
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
package org.exist.storage.sync;

import java.nio.file.FileStore;
import java.nio.file.Path;
import java.util.Properties;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.EXistException;
import org.exist.scheduler.JobDescription;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.SystemTask;
import org.exist.storage.txn.Txn;
import org.exist.util.Configuration;
import org.exist.util.FileUtils;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;

public class SyncTask implements SystemTask {

    private final static Logger LOG = LogManager.getLogger(SyncTask.class);

    private final static String JOB_NAME = "Sync";

    public static String getJobName() {
        return JOB_NAME;
    }

    public static String getJobGroup() {
        return JobDescription.EXIST_INTERNAL_GROUP;
    }

    private Path dataDir;
    private long diskSpaceMin;

    @Override
    public boolean afterCheckpoint() {
        // a checkpoint is created by the MAJOR_SYNC event
        return false;
    }

    @Override
    public String getName() {
        return getJobName();
    }

    @Override
    public void configure(final Configuration config, final Properties properties) throws EXistException {
        this.diskSpaceMin = 1024L * 1024L * config.getProperty(BrokerPool.DISK_SPACE_MIN_PROPERTY, BrokerPool.DEFAULT_DISK_SPACE_MIN);

        // fixme! - Shouldn't it be data dir AND journal dir we check
        // rather than EXIST_HOME? /ljo
        dataDir = (Path) config.getProperty(BrokerPool.PROPERTY_DATA_DIR);
        LOG.info("Using DATA_DIR: {}. Minimal disk space required for database to continue operations: {}mb", dataDir.toAbsolutePath().toString(), diskSpaceMin / 1024 / 1024);
        final long space = FileUtils.measureFileStore(dataDir, FileStore::getUsableSpace);
        LOG.info("Usable space on partition containing DATA_DIR: {}: {}mb", dataDir.toAbsolutePath().toString(), space / 1024 / 1024);
    }

    @Override
    public void execute(final DBBroker broker, final Txn transaction) throws EXistException {
        final BrokerPool pool = broker.getBrokerPool();
        final Tuple2<Boolean, Long> availableSpace = checkDiskSpace();
        if (!availableSpace._1) {
            LOG.fatal("Partition containing DATA_DIR: {} is running out of disk space [minimum: {} free: {}]. Switching Elemental into read-only mode to prevent data loss!", dataDir.toAbsolutePath().toString(), diskSpaceMin, availableSpace._2);
            pool.setReadOnly();
        }

        if(System.currentTimeMillis() - pool.getLastMajorSync() >
                pool.getMajorSyncPeriod()) {
            pool.sync(broker, Sync.MAJOR);
        } else {
            pool.sync(broker, Sync.MINOR);
        }
    }

    /**
     * @return A tuple, where the first value indicates if there is enough disk space, the
     * second value is the amount of usable space in bytes
     */
    private Tuple2<Boolean,Long> checkDiskSpace() {
        final long usableSpace = FileUtils.measureFileStore(dataDir, FileStore::getUsableSpace);
        //LOG.info("Usable space on partition containing DATA_DIR: " + dataDir.getAbsolutePath() + ": " + (space / 1024 / 1024) + "mb");
        final boolean enoughSpace = usableSpace > diskSpaceMin || usableSpace == -1;
        return Tuple(enoughSpace, usableSpace);
    }
}
