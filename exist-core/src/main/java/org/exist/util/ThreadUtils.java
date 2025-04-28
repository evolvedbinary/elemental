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
package org.exist.util;

import org.exist.Database;

/**
 * Simple utility functions for creating named threads
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ThreadUtils {

    public static String nameInstanceThreadGroup(final String instanceId) {
        return "exist.db." + instanceId;
    }

    public static ThreadGroup newInstanceSubThreadGroup(final Database database, final String subThreadGroupName) {
        return new ThreadGroup(database.getThreadGroup(), subThreadGroupName);
    }

    public static String nameInstanceThread(final Database database, final String threadName) {
        return "db." + database.getId() + "." + threadName;
    }

    public static String nameInstanceThread(final String instanceId, final String threadName) {
        return "db." + instanceId + "." + threadName;
    }

    public static String nameInstanceSchedulerThread(final Database database, final String threadName) {
        return "db." + database.getId() + ".scheduler." + threadName;
    }

    public static Thread newInstanceThread(final Database database, final String threadName, final Runnable runnable) {
        return new Thread(database.getThreadGroup(), runnable, nameInstanceThread(database, threadName));
    }

    public static Thread newInstanceThread(final ThreadGroup threadGroup, final String instanceId, final String threadName, final Runnable runnable) {
        return new Thread(threadGroup, runnable, nameInstanceThread(instanceId, threadName));
    }

    public static String nameGlobalThread(final String threadName) {
        return "global." + threadName;
    }

    public static Thread newGlobalThread(final String threadName, final Runnable runnable) {
        return new Thread(runnable, nameGlobalThread(threadName));
    }
}
