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
package org.exist.management.impl;

import org.exist.storage.BrokerPool;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * JMX MXBean for examining the CollectionCache
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class CollectionCache implements CollectionCacheMXBean {

    private final BrokerPool instance;

    public CollectionCache(final BrokerPool instance) {
        this.instance = instance;
    }

    public static String getAllInstancesQuery() {
        return getName("*");
    }

    private static String getName(final String instanceId) {
        return "org.exist.management." + instanceId + ":type=CollectionCache";
    }

    @Override
    public ObjectName getName() throws MalformedObjectNameException {
        return new ObjectName(getName(instance.getId()));
    }

    @Override
    public String getInstanceId() {
        return instance.getId();
    }

    @Override
    public int getMaxCacheSize() {
        return instance.getCollectionsCache().getMaxCacheSize();
    }

    @Override
    public org.exist.collections.CollectionCache.Statistics getStatistics() {
        return instance.getCollectionsCache().getStatistics();
    }
}
