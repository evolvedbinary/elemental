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
package org.exist.xquery;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.exist.dom.QName;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.storage.BrokerPoolService;
import org.exist.storage.BrokerPoolServiceException;
import org.exist.storage.lock.Lock;
import org.exist.storage.lock.ManagedLock;
import org.exist.util.Configuration;

import javax.annotation.Nullable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.exist.storage.lock.ManagedLock.acquire;

/**
 * Implementation of a PerformanceStats that is designed
 * to be used my multiple-threads as a Service from the BrokerPool.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@ThreadSafe
public class PerformanceStatsService implements BrokerPoolService, PerformanceStats {

    private @Nullable ReadWriteLock performanceStatsLock = null;  // access is guarded by volatile `performanceStats` field below

    @GuardedBy("performanceStatsLock")
    private volatile @Nullable PerformanceStats performanceStats = null;  // volatile access as it is lazy-initialised (or not) in {@link BrokerPoolService#configure()} by the system thread

    @Override
    public void configure(final Configuration configuration) throws BrokerPoolServiceException {
        final String xqueryProfilingTraceEnabled = (String) configuration.getProperty(PerformanceStatsImpl.CONFIG_PROPERTY_TRACE);
        if ("yes".equals(xqueryProfilingTraceEnabled) || "functions".equals(xqueryProfilingTraceEnabled)) {
            init();
        }
    }

    private void init() {
        this.performanceStatsLock = new ReentrantReadWriteLock();
        this.performanceStats = new PerformanceStatsImpl(true);
    }

    @Override
    public boolean isEnabled() {
        if (performanceStats == null) {
            // not initialized or disabled
            return false;
        }

        try (final ManagedLock<ReadWriteLock> readLock = acquire(performanceStatsLock, Lock.LockMode.READ_LOCK)) {
            return performanceStats.isEnabled();
        }
    }

    @Override
    public void setEnabled(final boolean enabled) {
        if (performanceStats == null) {
            // not initialized or disabled
            if (enabled == true) {
                init();
            }
            return;
        }

        try (final ManagedLock<ReadWriteLock> writeLock = acquire(performanceStatsLock, Lock.LockMode.WRITE_LOCK)) {
            performanceStats.setEnabled(enabled);
        }
    }

    @Override
    public void recordQuery(final String source, final long elapsed) {
        if (performanceStats == null) {
            // not initialized or disabled
            return;
        }

        try (final ManagedLock<ReadWriteLock> writeLock = acquire(performanceStatsLock, Lock.LockMode.WRITE_LOCK)) {
            performanceStats.recordQuery(source, elapsed);
        }
    }

    @Override
    public void recordFunctionCall(final QName qname, final String source, final long elapsed) {
        if (performanceStats == null) {
            // not initialized or disabled
            return;
        }

        try (final ManagedLock<ReadWriteLock> writeLock = acquire(performanceStatsLock, Lock.LockMode.WRITE_LOCK)) {
            performanceStats.recordFunctionCall(qname, source, elapsed);
        }
    }

    @Override
    public void recordIndexUse(final Expression expression, final String indexName, final String source, final IndexOptimizationLevel indexOptimizationLevel, final long elapsed) {
        if (performanceStats == null) {
            // not initialized or disabled
            return;
        }

        try (final ManagedLock<ReadWriteLock> writeLock = acquire(performanceStatsLock, Lock.LockMode.WRITE_LOCK)) {
            performanceStats.recordIndexUse(expression, indexName, source, indexOptimizationLevel, elapsed);
        }
    }

    @Override
    public void recordOptimization(final Expression expression, final PerformanceStatsImpl.OptimizationType type, final String source) {
        if (performanceStats == null) {
            // not initialized or disabled
            return;
        }

        try (final ManagedLock<ReadWriteLock> writeLock = acquire(performanceStatsLock, Lock.LockMode.WRITE_LOCK)) {
            performanceStats.recordOptimization(expression, type, source);
        }
    }

    @Override
    public void recordAll(final PerformanceStats otherPerformanceStats) {
        if (performanceStats == null) {
            // not initialized or disabled
            return;
        }

        try (final ManagedLock<ReadWriteLock> writeLock = acquire(performanceStatsLock, Lock.LockMode.WRITE_LOCK)) {
            performanceStats.recordAll(otherPerformanceStats);
        }
    }

    @Override
    public void serialize(final MemTreeBuilder builder) {
        if (performanceStats == null) {
            // not initialized or disabled
            builder.startElement(new QName(XML_ELEMENT_CALLS, XML_NAMESPACE, XML_PREFIX), null);
            builder.endElement();
            return;
        }

        try (final ManagedLock<ReadWriteLock> readLock = acquire(performanceStatsLock, Lock.LockMode.READ_LOCK)) {
            performanceStats.serialize(builder);
        }
    }

    @Override
    public void reset() {
        if (performanceStats == null) {
            // not initialized or disabled
            return;
        }

        try (final ManagedLock<ReadWriteLock> writeLock = acquire(performanceStatsLock, Lock.LockMode.WRITE_LOCK)) {
            performanceStats.reset();
        }
    }

    @Override
    public String toString() {
        if (performanceStats == null) {
            // not initialized or disabled
            return "";
        }

        try (final ManagedLock<ReadWriteLock> readLock = acquire(performanceStatsLock, Lock.LockMode.READ_LOCK)) {
            return performanceStats.toString();
        }
    }
}
