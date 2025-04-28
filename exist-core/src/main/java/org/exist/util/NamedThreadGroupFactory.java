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

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple factory for thread groups, where you
 * may want multiple groups with similar names.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class NamedThreadGroupFactory {

    private final String threadGroupNameBase;
    private final AtomicLong threadGroupId = new AtomicLong();

    /**
     * @param threadGroupNameBase the base name for the thread group.
     */
    public NamedThreadGroupFactory(final String threadGroupNameBase) {
        this.threadGroupNameBase = threadGroupNameBase;
    }

    /**
     * Produces a thread group named like:
     *     "${threadGroupNameBase}-${id}"
     *
     * Where id is a global monontonically increasing identifier.
     *
     * @param parent the parent thread group, or null to use the current threads thread group.
     *
     * @return the new thread group
     */
    public ThreadGroup newThreadGroup(@Nullable final ThreadGroup parent) {
        final String threadGroupName = threadGroupNameBase + "-" + threadGroupId.getAndIncrement();
        if (parent != null) {
            return new ThreadGroup(parent, threadGroupName);
        } else {
            return new ThreadGroup(threadGroupName);
        }
    }
}
