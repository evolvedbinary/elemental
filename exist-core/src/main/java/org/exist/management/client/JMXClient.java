/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
package org.exist.management.client;

import org.exist.start.CompatibleJavaVersionCheck;
import org.exist.start.StartException;
import org.exist.util.FileUtils;
import org.exist.util.OSUtil;
import org.exist.util.SystemExitCodes;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;
import se.softhouse.jargo.ParsedArguments;

import javax.annotation.Nullable;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.*;

import static org.exist.util.ArgumentUtil.getBool;
import static se.softhouse.jargo.Arguments.*;

/**
 */
public class JMXClient {

    private MBeanServerConnection connection;
    private String instance;

    public JMXClient(String instanceName) {
        this.instance = instanceName;
    }

    public void connect(String address,int port) throws IOException {
        final JMXServiceURL url =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+address+":" + port + "/jmxrmi");
        final Map<String, String[]> env = new HashMap<>();
        final String[] creds = {"guest", "guest"};
        env.put(JMXConnector.CREDENTIALS, creds);

        final JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
        connection = jmxc.getMBeanServerConnection();
        echo("Connected to MBean server.");
    }

    public void memoryStats() {
        try {
            final ObjectName name = new ObjectName("java.lang:type=Memory");
            final CompositeData composite = (CompositeData) connection.getAttribute(name, "HeapMemoryUsage");
            if (composite != null) {
                echo("\nMEMORY:");
                echo(String.format("Current heap: %12s        Committed memory:  %12s",
                        FileUtils.humanSize((Long)composite.get("used")), FileUtils.humanSize((Long)composite.get("committed"))));
                echo(String.format("Max memory:   %12s", FileUtils.humanSize((Long)composite.get("max"))));
            }
        } catch (final Exception e) {
            error(e);
        }
    }

    public void instanceStats() {
        try {
            echo("\nINSTANCE:");
            final ObjectName name = new ObjectName("org.exist.management." + instance + ":type=Database");
            echo(String.format("%25s: %10s", "Name", instance));
            final Long memReserved = (Long) connection.getAttribute(name, "ReservedMem");
            echo(String.format("%25s: %10s", "Reserved memory", FileUtils.humanSize(memReserved)));
            final Long memCache = (Long) connection.getAttribute(name, "CacheMem");
            echo(String.format("%25s: %10s", "Cache memory", FileUtils.humanSize(memCache)));
            final Long memCollCache = (Long) connection.getAttribute(name, "CollectionCacheMem");
            echo(String.format("%25s: %10s", "Collection cache memory", FileUtils.humanSize(memCollCache)));

            final String[] cols = { "MaxBrokers", "AvailableBrokers", "ActiveBrokers" };
            echo(String.format("\n%17s %17s %17s", cols[0], cols[1], cols[2]));
            final AttributeList attrs = connection.getAttributes(name, cols);
            final Object[] values = getValues(attrs);
            echo(String.format("%17d %17d %17d", (Integer)values[0], (Integer)values[1], (Integer)values[2]));

            final TabularData table = (TabularData) connection.getAttribute(name, "ActiveBrokersMap");
//            if (table.size() > 0) {
                echo("\nCurrently active threads:");
//            }

            for (Object o : table.values()) {
                final CompositeData data = (CompositeData) o;
                echo(String.format("\t%20s: %3d", data.get("owner"), data.get("referenceCount")));
            }
        } catch (final Exception e) {
            error(e);
        }
    }

    public void cacheStats() {
        try {
            ObjectName name = new ObjectName("org.exist.management." + instance + ":type=CacheManager");
            String[] cols = { "MaxTotal", "CurrentSize" };
            AttributeList attrs = connection.getAttributes(name, cols);
            Object[] values = getValues(attrs);
            echo(String.format("\nCACHE [%8d pages max. / %8d pages allocated]", values[0], values[1]));

            final Set<ObjectName> beans = connection.queryNames(new ObjectName("org.exist.management." + instance + ":type=CacheManager.Cache,*"), null);
            cols = new String[] {"Type", "CacheName", "Size", "Used", "Hits", "Fails"};
            echo(String.format("%10s %20s %10s %10s %10s %10s", cols[0], cols[1], cols[2], cols[3], cols[4], cols[5]));
            for (final ObjectName bean : beans) {
                name = bean;
                attrs = connection.getAttributes(name, cols);
                values = getValues(attrs);
                echo(String.format("%10s %20s %,10d %,10d %,10d %,10d", values[0], values[1], values[2], values[3], values[4], values[5]));
            }

            echo("");
            name = new ObjectName("org.exist.management." + instance + ":type=CollectionCache");
            cols = new String[] { "MaxCacheSize", "Statistics" };
            attrs = connection.getAttributes(name, cols);
            values = getValues(attrs);


            echo(String.format("COLLECTION CACHE: [%10s max]", FileUtils.humanSize((Integer)values[0])));

            cols = new String[] {"Hit Count", "Miss Count", "Load Success Count", "Load Failure Count", "Total Load Time", "Eviction Count", "Eviction Weight"};
            echo(String.format("%10s %20s %20s %20s %20s %20s %20s", cols[0], cols[1], cols[2], cols[3], cols[4], cols[5], cols[6]));
            final CompositeDataSupport statistics = (CompositeDataSupport) values[1];
            echo(String.format("%10d %20d %20d %20d %20d %20d %20d", (Long)statistics.get("hitCount"), (Long)statistics.get("missCount"), (Long)statistics.get("loadSuccessCount"), (Long)statistics.get("loadFailureCount"), (Long)statistics.get("totalLoadTime"), (Long)statistics.get("evictionCount"), (Long)statistics.get("evictionWeight")));

        } catch (final Exception e) {
            error(e);
        }
    }

    public void lockTable() {
        echo("\nList of threads attempting to acquire a lock:");
        echo("-----------------------------------------------");
        try {
            final TabularData table = (TabularData) connection.getAttribute(new ObjectName("org.exist.management." + instance + ":type=LockTable"), "Attempting");
            printLockTable(table);
        } catch (final MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | IOException | MalformedObjectNameException e) {
            error(e);
        }

        echo("");

        echo("\nList of threads holding a lock:");
        echo("-----------------------------------------------");
        try {
            final TabularData table = (TabularData) connection.getAttribute(new ObjectName("org.exist.management." + instance + ":type=LockTable"), "Acquired");
            printLockTable(table);
        } catch (final MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | IOException | MalformedObjectNameException e) {
            error(e);
        }
    }

    private void printLockTable(final TabularData table) {
        for (final Object tv : table.values()) {
            final CompositeData data = (CompositeData) tv;

            final String resourceUri = (String) data.get("key");
            echo("URI: " + resourceUri);

            final TabularData valueData = (TabularData) data.get("value");
            for (final Object vdv : valueData.values()) {
                final CompositeData cvdv = (CompositeData) vdv;
                final String resourceType = (String) cvdv.get("key");
                echo(String.format("%20s: %s", "Lock type", resourceType));

                final TabularData resourceData = (TabularData) cvdv.get("value");

                for (final Object rvdv : resourceData.values()) {
                    final CompositeData crvdv = (CompositeData) rvdv;
                    final String lockMode = (String) crvdv.get("key");
                    echo(String.format("%20s: %s", "Lock type", lockMode));

                    final TabularData lockData = (TabularData) crvdv.get("value");

                    for (final Object lrvdv : lockData.values()) {
                        final CompositeData clrvdv = (CompositeData) lrvdv;
                        final String owner = (String) clrvdv.get("key");
                        echo(String.format("%20s: %s", "Held by", owner));

                        final CompositeData ownerData = (CompositeData) clrvdv.get("value");

                        final Integer holdCount = (Integer) ownerData.get("count");
                        echo(String.format("%20s: %d", "Hold count", holdCount));
                    }
                }

            }
        }
    }

    public void sanityReport() {
        echo("\nSanity report");
        echo("-----------------------------------------------");
        try {
            final ObjectName name = new ObjectName("org.exist.management." + instance + ".tasks:type=SanityReport");
            final String status = (String) connection.getAttribute(name, "Status");
            final Date lastCheckStart = (Date) connection.getAttribute(name, "LastCheckStart");
            final Date lastCheckEnd = (Date) connection.getAttribute(name, "LastCheckEnd");
            echo(String.format("%22s: %s", "Status", status));
            echo(String.format("%22s: %s", "Last check start", lastCheckStart));
            echo(String.format("%22s: %s", "Last check end", lastCheckEnd));
            if (lastCheckStart != null && lastCheckEnd != null)
                {echo(String.format("%22s: %dms", "Check took", (lastCheckEnd.getTime() - lastCheckStart.getTime())));}

            @Nullable final Object result = connection.getAttribute(name, "Errors");
            if (result != null) {
                @Nullable final CompositeData table;
                if (result.getClass().isArray()) {
                    final CompositeData[] tables = ((CompositeData[]) result);
                    if (tables.length > 0) {
                        table = tables[0];
                    } else {
                        table = null;
                    }
                } else {
                    table = (CompositeData) result;
                }

                if (table != null) {
                    for (final Object o : table.values()) {
                        final CompositeData data = (CompositeData) o;
                        echo(String.format("%22s: %s", "Error code", data.get("errcode")));
                        echo(String.format("%22s: %s", "Description", data.get("description")));
                    }
                }
            }
        } catch (final MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | IOException | MalformedObjectNameException e) {
            error(e);
        }
    }

    public void jobReport() {
        echo("\nRunning jobs report");
        echo("-----------------------------------------------");
        try {
            final ObjectName name = new ObjectName("org.exist.management." + instance + ":type=ProcessReport");

            TabularData table = (TabularData)
                    connection.getAttribute(name, "RunningJobs");
            String[] cols = new String[] { "ID", "Action", "Info" };
            echo(String.format("%15s %30s %30s", cols[0], cols[1], cols[2]));
            for (Object value : table.values()) {
                final CompositeData data = (CompositeData) value;
                echo(String.format("%15s %30s %30s", data.get("id"), data.get("action"), data.get("info")));
            }

            echo("\nRunning queries");
            echo("-----------------------------------------------");
            table = (TabularData)
                    connection.getAttribute(name, "RunningQueries");
            cols = new String[] { "ID", "Type", "Key", "Terminating" };
            echo(String.format("%10s %10s %30s %s", cols[0], cols[1], cols[2], cols[3]));
            for (Object o : table.values()) {
                final CompositeData data = (CompositeData) o;
                echo(String.format("%15s %15s %30s %6s", data.get("id"), data.get("sourceType"), data.get("sourceKey"), data.get("terminating")));
            }
        } catch (final MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | IOException | MalformedObjectNameException e) {
            error(e);
        }
    }

    private Object[] getValues(final AttributeList attribs) {
        final Object[] v = new Object[attribs.size()];
        for (int i = 0; i < attribs.size(); i++) {
            v[i] = ((Attribute) attribs.get(i)).getValue();
        }
        return v;
    }

    private void echo(String msg) {
        System.out.println(msg);
    }
    
    private void error(Exception e) {
        System.err.println("ERROR: " + e.getMessage());
        e.printStackTrace();
    }

    private static final int DEFAULT_PORT = 1099;
    private static final int DEFAULT_WAIT_TIME = 0;

    /* general arguments */
    private static final Argument<?> helpArg = helpArgument("-h", "--help");

    /* connection arguments */
    private static final Argument<String> addressArg = stringArgument("-a", "--address")
            .description("RMI address of the server")
            .defaultValue("localhost")
            .build();
    private static final Argument<Integer> portArg = integerArgument("-p", "--port")
            .description("RMI port of the server")
            .defaultValue(DEFAULT_PORT)
            .build();
    private static final Argument<String> instanceArg = stringArgument("-i", "--instance")
            .description("The ID of the database instance to connect to")
            .defaultValue("exist")
            .build();
    private static final Argument<Integer> waitArg = integerArgument("-w", "--wait")
            .description("while displaying server statistics: keep retrieving statistics, but wait the specified number of seconds between calls.")
            .defaultValue(DEFAULT_WAIT_TIME)
            .build();

    /* display mode options */
    private static final Argument<Boolean> cacheDisplayArg = optionArgument("-c", "--cache")
            .description("displays server statistics on cache and memory usage.")
            .defaultValue(false)
            .build();
    private static final Argument<Boolean> locksDisplayArg = optionArgument("-l", "--locks")
            .description("lock manager: display locking information on all threads currently waiting for a lock on a resource or collection. Useful to debug deadlocks. During normal operation, the list will usually be empty (means: no blocked threads).")
            .defaultValue(false)
            .build();

    /* display info options */
    private static final Argument<Boolean> dbInfoArg = optionArgument("-d", "--db")
            .description("display general info about the db instance.")
            .defaultValue(false)
            .build();
    private static final Argument<Boolean> memoryInfoArg = optionArgument("-m", "--memory")
            .description("display info on free and total memory. Can be combined with other parameters.")
            .defaultValue(false)
            .build();
    private static final Argument<Boolean> sanityCheckInfoArg = optionArgument("-s", "--report")
            .description("retrieve sanity check report from the db")
            .defaultValue(false)
            .build();
    private static final Argument<Boolean> jobsInfoArg = optionArgument("-j", "--jobs")
            .description("retrieve sanity check report from the db")
            .defaultValue(false)
            .build();

    private enum Mode {
        STATS,
        LOCKS
    }

    @SuppressWarnings("unchecked")
	public static void main(final String[] args) {
        try {
            CompatibleJavaVersionCheck.checkForCompatibleJavaVersion();

            final ParsedArguments arguments = CommandLineParser
                    .withArguments(addressArg, portArg, instanceArg, waitArg)
                    .andArguments(cacheDisplayArg, locksDisplayArg)
                    .andArguments(dbInfoArg, memoryInfoArg, sanityCheckInfoArg, jobsInfoArg)
                    .andArguments(helpArg)
                    .programName("jmxclient" + (OSUtil.IS_WINDOWS ? ".bat" : ".sh"))
                    .parse(args);

            process(arguments);
        } catch (final StartException e) {
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                System.err.println(e.getMessage());
            }
            System.exit(e.getErrorCode());
        } catch (final ArgumentException e) {
            System.out.println(e.getMessageAndUsage());
            System.exit(SystemExitCodes.INVALID_ARGUMENT_EXIT_CODE);
        }

    }

    private static void process(final ParsedArguments arguments) {
        final String address = arguments.get(addressArg);
        final int port = Optional.ofNullable(arguments.get(portArg)).orElse(DEFAULT_PORT);
        final String dbInstance = arguments.get(instanceArg);
        final long waitTime = Optional.ofNullable(arguments.get(waitArg)).orElse(DEFAULT_WAIT_TIME);

        Mode mode = Mode.STATS;
        if(getBool(arguments, cacheDisplayArg)) {
            mode = Mode.STATS;
        }
        if(getBool(arguments, locksDisplayArg)) {
            mode = Mode.LOCKS;
        }

        final boolean displayInstance = getBool(arguments, dbInfoArg);
        final boolean displayMem = getBool(arguments, memoryInfoArg);
        final boolean displayReport = getBool(arguments, sanityCheckInfoArg);
        final boolean jobReport = getBool(arguments, jobsInfoArg);

        try {
            final JMXClient stats = new JMXClient(dbInstance);
            stats.connect(address,port);
            stats.memoryStats();
            while (true) {
                switch (mode) {
                    case STATS :
                        stats.cacheStats();
                        break;
                    case LOCKS :
                        stats.lockTable();
                        break;
                }
                if (displayInstance) {stats.instanceStats();}
                if (displayMem) {stats.memoryStats();}
                if (displayReport) {stats.sanityReport();}
                if (jobReport) {stats.jobReport();}
                if (waitTime > 0) {
                    synchronized (stats) {
                        try {
                            stats.wait(waitTime);
                        } catch (final InterruptedException e) {
                            System.err.println("INTERRUPTED: " + e.getMessage());
                        }
                    }
                } else
                    {return;}
            }
        } catch (final IOException e) {
            e.printStackTrace(); 
        } 
    }
}
