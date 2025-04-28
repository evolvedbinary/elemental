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
package org.exist.storage.lock;

import net.jcip.annotations.NotThreadSafe;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * A lock event listener which sends events to Log4j
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NotThreadSafe
public class LockEventLogListener implements LockTable.LockEventListener {
    private final Logger log;
    private final Level level;

    /**
     * @param log The Log4j log
     * @param level The level at which to to log the lock events to Log4j
     */
    public LockEventLogListener(final Logger log, final Level level) {
        this.log = log;
        this.level = level;
    }

    @Override
    public void accept(final LockTable.LockEventType lockEventType, final long timestamp, final long groupId,
            final LockTable.Entry entry) {
        if(log.isEnabled(level)) {
            // read count first to ensure memory visibility from volatile!
            final int localCount = entry.count;

            log.log(level, LockTable.formatString(lockEventType, groupId, entry.id, entry.lockType, entry.lockMode,
                    entry.owner, localCount, timestamp, entry.stackTraces == null ? null : entry.stackTraces.get(0)));
        }
    }
}
