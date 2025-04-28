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
package org.exist.storage.serializers;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.exist.storage.DBBroker;
import org.exist.util.Configuration;

import javax.annotation.Nullable;

public class XmlSerializerPool extends GenericObjectPool<Serializer> {
    public XmlSerializerPool(final DBBroker broker, final Configuration config, final int maxIdle) {
        super(new XmlSerializerPoolObjectFactory(broker, config), toConfig(broker.getId(), maxIdle));
    }

    private static GenericObjectPoolConfig<Serializer> toConfig(@Nullable final String brokerId, final int maxIdle) {
        final GenericObjectPoolConfig<Serializer> config = new GenericObjectPoolConfig<>();
        config.setBlockWhenExhausted(false);
        config.setLifo(true);
        config.setMaxIdle(maxIdle);
        config.setMaxTotal(-1);            // TODO(AR) is this the best way to allow us to temporarily exceed the size of the pool?
        final String poolName = brokerId == null ? "" : "pool." + brokerId;
        config.setJmxNameBase("org.exist.management.exist:type=XmlSerializerPool,name=" + poolName);
        return config;
    }

    @Override
    public Serializer borrowObject() {
        try {
            return super.borrowObject();
        } catch (final Exception e) {
            throw new IllegalStateException("Error while borrowing serializer: " + e.getMessage());
        }
    }

    @Override
    public void returnObject(final Serializer obj) {
        if (obj == null) {
            return;
        }

        try {
            super.returnObject(obj);
        } catch (final Exception e) {
            throw new IllegalStateException("Error while returning serializer: " + e.getMessage());
        }
    }
}
