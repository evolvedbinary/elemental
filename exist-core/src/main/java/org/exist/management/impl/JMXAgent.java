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
package org.exist.management.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.management.Agent;
import org.exist.management.TaskStatus;
import org.exist.storage.BrokerPool;
import org.exist.util.DatabaseConfigurationException;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Real implementation of interface {@link org.exist.management.Agent}
 * which registers MBeans with the MBeanServer.
 *
 * Note that the agent will be constructed via reflection by the
 * {@link org.exist.management.AgentFactory}
 */
public final class JMXAgent implements Agent {

    private static final Logger LOG = LogManager.getLogger(JMXAgent.class);

    private final MBeanServer server;
    private final Map<String, Deque<ObjectName>> registeredMBeans = new HashMap<>();
    private final Map<ObjectName, Object> beanInstances = new HashMap<>();

    public JMXAgent() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating the JMX MBeanServer.");
        }

        this.server = ManagementFactory.getPlatformMBeanServer();

        registerSystemMBeans();
    }

    private void registerSystemMBeans() {
        try {
            final SystemInfoMXBean systemInfoMXBean = new org.exist.management.impl.SystemInfo();
            addMBean(systemInfoMXBean.getName(), systemInfoMXBean);
        } catch (final MalformedObjectNameException | DatabaseConfigurationException e) {
            LOG.warn("Exception while registering cache mbean.", e);
        }
    }

    @Override
    public synchronized void initDBInstance(final BrokerPool instance) {
        final List<PerInstanceMBean> perInstanceMBeans = Arrays.asList(
                new Database(instance),
                new LockTable(instance),
                new SanityReport(instance),
                new DiskUsage(instance),
                new ProcessReport(instance),
                new BinaryValues(instance),
                new CollectionCache(instance)
        );

        for (final PerInstanceMBean perInstanceMBean : perInstanceMBeans) {
            try {
                addMBean(perInstanceMBean);
            } catch (final DatabaseConfigurationException e) {
                LOG.warn("Exception while registering JMX MBean: {}, for database: {}.", perInstanceMBean.getClass().getName(), instance.getId(), e);
            }
        }
    }

    @Override
    public synchronized void closeDBInstance(final BrokerPool instance) {
        final Deque<ObjectName> stack = registeredMBeans.get(instance.getId());
        while (!stack.isEmpty()) {
            final ObjectName on = stack.pop();
            if (LOG.isDebugEnabled()) {
                LOG.debug("deregistering JMX MBean: {}", on);
            }
            beanInstances.remove(on);
            removeMBean(on);
        }
    }

    @Override
    public synchronized void addMBean(final PerInstanceMBean mbean) throws DatabaseConfigurationException {
        try {
            addMBean(mbean.getName(), mbean);
            if (mbean.getInstanceId() != null) {
                Deque<ObjectName> stack = registeredMBeans.get(mbean.getInstanceId());
                if (stack == null) {
                    stack = new ArrayDeque<>();
                    registeredMBeans.put(mbean.getInstanceId(), stack);
                }
                stack.push(mbean.getName());
            }
            beanInstances.put(mbean.getName(), mbean);
        } catch (final MalformedObjectNameException e) {
            LOG.warn("Problem registering JMX MBean: {}", e.getMessage(), e);
            throw new DatabaseConfigurationException("Exception while registering JMX MBean: " + e.getMessage());
        }
    }

    private void addMBean(final ObjectName name, final Object mbean) throws DatabaseConfigurationException {
        try {
            if (!server.isRegistered(name)) {
                server.registerMBean(mbean, name);
            }

        } catch (final InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            LOG.warn("Problem registering JMX MBean: {}", e.getMessage(), e);
            throw new DatabaseConfigurationException("Exception while registering JMX MBean: " + e.getMessage());
        }
    }

    private void removeMBean(final ObjectName name) {
        try {
            if (server.isRegistered(name)) {
                server.unregisterMBean(name);
            }
        } catch (final InstanceNotFoundException | MBeanRegistrationException  e) {
            LOG.warn("Problem unregistering mbean: {}", e.getMessage(), e);
        }
    }

    @Override
    public synchronized void changeStatus(final BrokerPool instance, final TaskStatus actualStatus) {
        try {
            final ObjectName name = new ObjectName("org.exist.management." + instance.getId() + ".tasks:type=SanityReport");
            final SanityReport report = (SanityReport) beanInstances.get(name);
            if (report != null) {
                report.changeStatus(actualStatus);
            }
        } catch (final MalformedObjectNameException e) {
            LOG.warn("Problem calling JMX MBean: {}", e.getMessage(), e);
        }
    }

    @Override
    public synchronized void updateStatus(final BrokerPool instance, final int percentage) {
        try {
            final ObjectName name = new ObjectName("org.exist.management." + instance.getId() + ".tasks:type=SanityReport");
            final SanityReport report = (SanityReport) beanInstances.get(name);
            if (report != null) {
                report.updateStatus(percentage);
            }
        } catch (final MalformedObjectNameException e) {
            LOG.warn("Problem calling JMX MBean: {}", e.getMessage(), e);
        }
    }
}
