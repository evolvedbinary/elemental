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

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import static org.exist.util.ThreadUtils.nameGlobalThread;
import static org.exist.util.ThreadUtils.nameInstanceThread;

/**
 * A simple thread factory that provides a standard naming convention
 * for threads.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class NamedThreadFactory implements ThreadFactory {

    private final ThreadGroup threadGroup;
    @Nullable private final String instanceId;
    private final String nameBase;
    private final AtomicLong threadId = new AtomicLong();

    /**
     * A factory who will produce threads named like either:
     *     "instance.${instanceId}.${nameBase}-${id}".
     *
     * @param instanceId the id of the database instance
     * @param nameBase The name base for the thread name
     *
     * @deprecated use {@link #NamedThreadFactory(Database, String)}.
     */
    public NamedThreadFactory(final String instanceId, final String nameBase) {
        this(null, instanceId, nameBase);
    }

    /**
     * A factory who will produce threads named like either:
     *     "instance.${instanceId}.${nameBase}-${id}".
     *
     * @param database the database instance which the threads are created for
     * @param nameBase The name base for the thread name
     *
     * @deprecated use {@link #NamedThreadFactory(Database, String)}.
     */
    @Deprecated
    public NamedThreadFactory(final Database database, final String nameBase) {
        this(database.getThreadGroup(), database.getId(), nameBase);
    }

    /**
     * A factory who will produce threads named like either:
     *
     *    1. "instance.${instanceId}.${nameBase}-${id}".
     *    2. "global.${nameBase}-${id}".
     *
     * @param threadGroup The thread group for the created threads, or null
     *     to use the same group as the calling thread.
     * @param instanceId the id of the database instance, or null if the
     *     thread is a global thread i.e. shared between instances.
     * @param nameBase The name base for the thread name.
     */
    public NamedThreadFactory(@Nullable final ThreadGroup threadGroup, @Nullable final String instanceId, final String nameBase) {
        Objects.requireNonNull(nameBase);
        this.threadGroup = threadGroup;
        this.instanceId = instanceId;
        this.nameBase = nameBase;
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        final String localName = nameBase + "-" + threadId.getAndIncrement();
        if (instanceId == null) {
            return new Thread(threadGroup, runnable, nameGlobalThread(localName));
        } else {
            return new Thread(threadGroup, runnable, nameInstanceThread(instanceId, localName));
        }
    }
}
