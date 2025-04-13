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

import java.io.IOException;
import java.io.StringWriter;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static java.lang.management.ManagementFactory.CLASS_LOADING_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.MEMORY_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.RUNTIME_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.THREAD_MXBEAN_NAME;

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import javax.annotation.Nullable;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.dom.QName;
import org.exist.management.Cache;
import org.exist.management.CacheManager;
import org.exist.management.impl.*;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.start.CompatibleJavaVersionCheck;
import org.exist.start.StartException;
import org.exist.util.NamedThreadFactory;
import org.exist.util.serializer.DOMSerializer;
import org.exist.xquery.Expression;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Utility class to output database status information from eXist's JMX interface as XML.
 *
 * @author wolf
 */
public class JMXtoXML {

    private final static Logger LOG = LogManager.getLogger(JMXtoXML.class);

    private final static Map<String, ObjectName[]> CATEGORIES = new TreeMap<>();

    private static void putCategory(final String categoryName, final String... objectNames) {
        final ObjectName[] aryObjectNames = new ObjectName[objectNames.length];
        try {
            for (int i = 0; i < aryObjectNames.length; i++) {
                aryObjectNames[i] = new ObjectName(objectNames[i]);
            }
        } catch (final MalformedObjectNameException | NullPointerException e) {
            LOG.warn("Error in initialization: {}", e.getMessage(), e);
        }

        CATEGORIES.put(categoryName, aryObjectNames);
    }
    static {
        // Java
        putCategory("memory", MEMORY_MXBEAN_NAME);
        putCategory("runtime", RUNTIME_MXBEAN_NAME);
        putCategory("operatingsystem", OPERATING_SYSTEM_MXBEAN_NAME);
        putCategory("thread", THREAD_MXBEAN_NAME);
        putCategory("classloading", CLASS_LOADING_MXBEAN_NAME);

        // eXist cross-instance
        putCategory("system", SystemInfo.OBJECT_NAME);

        // eXist per-instance
        putCategory("instances", Database.getAllInstancesQuery());
        putCategory("locking", LockTable.getAllInstancesQuery());
        putCategory("disk", DiskUsage.getAllInstancesQuery());
        putCategory("collectioncaches", CollectionCache.getAllInstancesQuery());
        putCategory("caches",
                CacheManager.getAllInstancesQuery(),
                Cache.getAllInstancesQuery()
        );
        putCategory("binarystreamcaches", BinaryValues.getAllInstancesQuery());
        putCategory("processes", ProcessReport.getAllInstancesQuery());
        putCategory("sanity", SanityReport.getAllInstancesQuery());

        // Jetty
        putCategory("jetty.threads", "org.eclipse.jetty.util.thread:type=queuedthreadpool,*");
        putCategory("jetty.nio", "org.eclipse.jetty.server.nio:type=selectchannelconnector,id=0");

        // Special case: all data
        putCategory("all", "org.exist.*:*", "java.lang:*");
    }

    private static final Properties defaultProperties = new Properties();

    static {
        defaultProperties.setProperty(OutputKeys.INDENT, "yes");
        defaultProperties.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    }

    public static final String JMX_NAMESPACE = "http://exist-db.org/jmx";
    public static final String JMX_PREFIX = "jmx";

    private static final QName ROW_ELEMENT = new QName("row", JMX_NAMESPACE, JMX_PREFIX);
    private static final QName JMX_ELEMENT = new QName("jmx", JMX_NAMESPACE, JMX_PREFIX);
    private static final QName JMX_RESULT = new QName("result", JMX_NAMESPACE, JMX_PREFIX);
    private static final QName JMX_RESULT_TYPE_ATTR = new QName("class", JMX_NAMESPACE, JMX_PREFIX);
    private static final QName JMX_CONNECTION_ATTR = new QName("connection", XMLConstants.NULL_NS_URI);
    private static final QName JMX_ERROR = new QName("error", JMX_NAMESPACE, JMX_PREFIX);
    private static final QName VERSION_ATTR = new QName("version", XMLConstants.NULL_NS_URI);

    public static final long PING_TIMEOUT = -99;

    public static final int VERSION = 1;

    private @Nullable List<MBeanServerConnection> connections;
    private JMXServiceURL url;

    /**
     * Connect to the local JMX instance.
     */
    public void connect() {
        final List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
        if (!servers.isEmpty()) {
            this.connections = new ArrayList<>(servers);
        }
    }

    /**
     * Connect to a remote JMX instance using address and port.
     *
     * @param address The remote address
     * @param port    The report port
     * @throws MalformedURLException The RMI url could not be constructed
     * @throws IOException           An IO error occurred
     */
    public void connect(final String address, final int port) throws MalformedURLException, IOException {
        this.url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + address + ":" + port + "/jmxrmi");
        final Map<String, String[]> env = new HashMap<>();
        final String[] creds = {"guest", "guest"};
        env.put(JMXConnector.CREDENTIALS, creds);

        final JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
        this.connections = Collections.singletonList(jmxc.getMBeanServerConnection());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Connected to JMX server at {}", url.toString());
        }
    }

    /**
     * Retrieve JMX output for the given categories and return a string of XML. Valid categories are "memory",
     * "instances", "disk", "system", "caches", "locking", "processes", "sanity", "all".
     *
     * @param categories array of categories to include in the report
     * @throws TransformerException in case of serialization errors
     * @return string containing an XML report
     */
    public String generateReport(final String categories[]) throws TransformerException {
        final Element root = generateXMLReport(null, categories);
        final StringWriter writer = new StringWriter();
        final DOMSerializer streamer = new DOMSerializer(writer, defaultProperties);
        streamer.serialize(root);
        return writer.toString();
    }

    /**
     * Ping the database to see if it is still responsive. This will first try to get a database broker object and if it
     * succeeds, run a simple query. If the server does not respond within the given timeout, the method will return an
     * error code -99 ({@link JMXtoXML#PING_TIMEOUT}). If there's an error on the server, the return value will be less
     * than 0. Otherwise the return value is the response time in milliseconds.
     *
     * @param instance the name of the database instance (default instance is "exist")
     * @param timeout  a timeout in milliseconds
     * @return Response time in msec, less than 0 in case of an error on server or PING_TIMEOUT when server does not
     * respond in time
     */
    public long ping(final String instance, final long timeout) {
        final long start = System.currentTimeMillis();
        final ThreadFactory jmxPingFactory = new NamedThreadFactory(instance, "jmx.ping");
        final ExecutorService executorService = Executors.newSingleThreadExecutor(jmxPingFactory);

        // find the connection and MBean name for the ping
        final ObjectName name;
        final MBeanServerConnection connection;
        try {
            name = SanityReport.getName(instance);
            final List<Tuple2<MBeanServerConnection, Set<ObjectName>>> matches = queryNames(name);
            if (matches.isEmpty()) {
                LOG.warn("Unable to locate MBean connection for ping destination");
                return SanityReport.PING_ERROR;
            }
            connection = matches.get(0)._1;
        } catch (final MalformedObjectNameException | IOException e) {
            LOG.warn("Unable to locate MBean connection for ping destination: " + e.getMessage(), e);
            return SanityReport.PING_ERROR;
        }

        final Future<Long> futurePing = executorService.submit(new Ping(connection, name));

        while (true) {
            try {
                return futurePing.get(timeout, TimeUnit.MILLISECONDS);
            } catch (final ExecutionException e) {
                LOG.error(e);
                return PING_TIMEOUT;
            } catch (final TimeoutException e) {
                return PING_TIMEOUT;
            } catch (final InterruptedException e) {
                if ((System.currentTimeMillis() - start) >= timeout) {
                    return PING_TIMEOUT;
                }
                // else will retry in loop
            }
        }
    }

    private static class Ping implements Callable<Long> {
        private final MBeanServerConnection connection;
        private final ObjectName name;

        public Ping(final MBeanServerConnection connection, final ObjectName name) {
            this.connection = connection;
            this.name = name;
        }

        @Override
        public Long call() {
            try {
                return (Long) connection.invoke(name, "ping", new Object[]{Boolean.TRUE}, new String[]{boolean.class.getName()});
            } catch (final Exception e) {
                LOG.warn(e.getMessage(), e);
                return (long) SanityReport.PING_ERROR;
            }
        }
    }

    /**
     * Retrieve JMX output for the given categories and return it as an XML DOM. Valid categories are "memory",
     * "instances", "disk", "system", "caches", "locking", "processes", "sanity", "all".
     *
     * @param errcode    an optional error description
     * @param categories the categories to generate the report for
     * @return xml report
     */
    public Element generateXMLReport(final String errcode, final String categories[]) {
        final MemTreeBuilder builder = new MemTreeBuilder((Expression) null);

        try {
            builder.startDocument();

            builder.startElement(JMX_ELEMENT, null);
            builder.addAttribute(VERSION_ATTR, Integer.toString(VERSION));
            if (url != null) {
                builder.addAttribute(JMX_CONNECTION_ATTR, url.toString());
            }

            if (errcode != null) {
                builder.startElement(JMX_ERROR, null);
                builder.characters(errcode);
                builder.endElement();
            }

            for (final String category : categories) {
                final ObjectName[] names = CATEGORIES.get(category);
                for (final ObjectName name : names) {
                    queryMBeans(builder, name);
                }
            }

            builder.endElement();

            builder.endDocument();

        } catch (final Exception e) {
            e.printStackTrace();
            LOG.warn("Could not generate XML report from JMX: {}", e.getMessage());
        }
        return (Element) builder.getDocument().getNode(1);
    }

    public String getDataDir() {
        try {
            final List<Tuple2<MBeanServerConnection, Object>> attributeValues = getAttribute(new ObjectName("org.exist.management.exist:type=DiskUsage"), "DataDirectory");
            if (attributeValues.isEmpty()) {
                return null;
            }
            final Object dir = attributeValues.get(0)._2;
            return dir == null ? null : dir.toString();
        } catch (final MalformedObjectNameException | IOException e) {
            return null;
        }
    }

    public Element invoke(final String name, final String operation, final String[] args) throws InstanceNotFoundException, MalformedObjectNameException, MBeanException, IOException, ReflectionException, IntrospectionException {
        final ObjectName objectName = new ObjectName(name);
        final List<Tuple2<MBeanServerConnection, MBeanInfo>> mbeanInfos = getMBeanInfo(objectName);
        if (mbeanInfos.isEmpty()) {
            return null;
        }

        final MBeanServerConnection conn = mbeanInfos.get(0)._1;
        final MBeanInfo info = mbeanInfos.get(0)._2;

        final MBeanOperationInfo[] operations = info.getOperations();
        for (final MBeanOperationInfo op : operations) {
            if (operation.equals(op.getName())) {
                final MBeanParameterInfo[] sig = op.getSignature();
                final Object[] params = new Object[sig.length];
                final String[] types = new String[sig.length];
                for (int i = 0; i < sig.length; i++) {
                    String type = sig[i].getType();
                    types[i] = type;
                    params[i] = mapParameter(type, args[i]);
                }
                final Object result = conn.invoke(objectName, operation, params, types);

                final MemTreeBuilder builder = new MemTreeBuilder((Expression) null);

                try {
                    builder.startDocument();

                    builder.startElement(JMX_ELEMENT, null);
                    builder.addAttribute(VERSION_ATTR, Integer.toString(VERSION));
                    if (url != null) {
                        builder.addAttribute(JMX_CONNECTION_ATTR, url.toString());
                    }

                    builder.startElement(JMX_RESULT, null);
                    builder.addAttribute(JMX_RESULT_TYPE_ATTR, op.getReturnType());
                    serializeObject(builder, result);
                    builder.endElement();

                    builder.endElement();

                    builder.endDocument();

                } catch (final Exception e) {
                    e.printStackTrace();
                    LOG.warn("Could not generate XML report from JMX: {}", e.getMessage());
                }
                return (Element) builder.getDocument().getNode(1);
            }
        }
        return null;
    }

    private List<Tuple2<MBeanServerConnection, Set<ObjectName>>> queryNames(final ObjectName nameQuery) throws IOException {
        final List<Tuple2<MBeanServerConnection, Set<ObjectName>>> matchedNames = new ArrayList<>();
        if (connections != null ) {
            for (final MBeanServerConnection connection : connections) {
                final Set<ObjectName> matches = connection.queryNames(nameQuery, null);
                if (!matches.isEmpty()) {
                    matchedNames.add(Tuple(connection, matches));
                }
            }
        }
        return matchedNames;
    }

    private List<Tuple2<MBeanServerConnection, Object>> getAttribute(final ObjectName objectName, final String attributeName) throws IOException {
        final List<Tuple2<MBeanServerConnection, Object>> attributeValues = new ArrayList<>();
        if (connections != null ) {
            for (final MBeanServerConnection connection : connections) {
                try {
                    attributeValues.add(Tuple(connection, connection.getAttribute(objectName, attributeName)));
                } catch (final AttributeNotFoundException | MBeanException | InstanceNotFoundException | ReflectionException e) {
                    // no-op
                }
            }
        }
        return attributeValues;
    }

    private List<Tuple2<MBeanServerConnection, MBeanInfo>> getMBeanInfo(final ObjectName objectName) throws IOException {
        final List<Tuple2<MBeanServerConnection, MBeanInfo>> mbeanInfos = new ArrayList<>();
        if (connections != null ) {
            for (final MBeanServerConnection connection : connections) {
                try {
                    mbeanInfos.add(Tuple(connection, connection.getMBeanInfo(objectName)));
                } catch (final IntrospectionException | InstanceNotFoundException | ReflectionException e) {
                    // no-op
                }
            }
        }
        return mbeanInfos;
    }

    private void queryMBeans(final MemTreeBuilder builder, final ObjectName nameQuery)
            throws IOException, InstanceNotFoundException, IntrospectionException, ReflectionException, NullPointerException {

        final List<Tuple2<MBeanServerConnection, Set<ObjectName>>> matchedNames = queryNames(nameQuery);

        for (final Tuple2<MBeanServerConnection, Set<ObjectName>> matchedName : matchedNames) {
            final MBeanServerConnection connection = matchedName._1;
            for (final ObjectName name : matchedName._2) {
                final MBeanInfo info = connection.getMBeanInfo(name);
                String className = info.getClassName().replace('$', '.');
                final int p = className.lastIndexOf('.');
                if (p > -1 && p + 1 < className.length()) {
                    className = className.substring(p + 1);
                }

                final QName qname = new QName(className, JMX_NAMESPACE, JMX_PREFIX);
                builder.startElement(qname, null);
                builder.addAttribute(new QName("name", XMLConstants.NULL_NS_URI), name.toString());

                final MBeanAttributeInfo[] beanAttribs = info.getAttributes();
                for (MBeanAttributeInfo beanAttrib : beanAttribs) {
                    if (beanAttrib.isReadable()) {
                        try {
                            final QName attrQName = new QName(beanAttrib.getName(), JMX_NAMESPACE, JMX_PREFIX);
                            final Object attrib = connection.getAttribute(name, beanAttrib.getName());

                            builder.startElement(attrQName, null);
                            serializeObject(builder, attrib);
                            builder.endElement();
                        } catch (final Exception e) {
                            LOG.debug("exception caught: {}", e.getMessage(), e);
                        }
                    }
                }
                builder.endElement();
            }
        }
    }

    private void serializeObject(final MemTreeBuilder builder, final Object object) throws SAXException {
        if (object == null) {
            return;
        }

        if (object instanceof TabularData) {
            serialize(builder, (TabularData) object);

        } else if (object instanceof CompositeData[]) {
            serialize(builder, (CompositeData[]) object);
        } else if (object instanceof CompositeData) {
            serialize(builder, (CompositeData) object);

        } else if (object instanceof Object[]) {
            serialize(builder, (Object[]) object);

        } else {
            builder.characters(object.toString());
        }
    }

    private void serialize(final MemTreeBuilder builder, final Object[] data) throws SAXException {
        for (final Object o : data) {
            serializeObject(builder, o);
        }
    }

    private void serialize(final MemTreeBuilder builder, final CompositeData data) throws SAXException {
        final CompositeType type = data.getCompositeType();
        for (final String key : type.keySet()) {
            final QName qname = new QName(key, JMX_NAMESPACE, JMX_PREFIX);
            builder.startElement(qname, null);
            serializeObject(builder, data.get(key));
            builder.endElement();
        }
    }

    private void serialize(final MemTreeBuilder builder, final TabularData data) throws SAXException {
        final CompositeType rowType = data.getTabularType().getRowType();
        for (final Object rowObj : data.values()) {
            final CompositeData row = (CompositeData) rowObj;
            builder.startElement(ROW_ELEMENT, null);
            for (final String key : rowType.keySet()) {
                final Object columnData = row.get(key);
                final QName columnQName = new QName(key, JMX_NAMESPACE, JMX_PREFIX);
                builder.startElement(columnQName, null);
                serializeObject(builder, columnData);
                builder.endElement();
            }
            builder.endElement();
        }
    }

    private void serialize(final MemTreeBuilder builder, final CompositeData[] array) throws SAXException {
        for (final CompositeData data : array) {
            builder.startElement(ROW_ELEMENT, null);
            serialize(builder, data);
            builder.endElement();
        }
    }

    private Object mapParameter(final String type, final String value) {
        if (type.equals("int") || type.equals(Integer.class.getName())) {
            return Integer.parseInt(value);
        } else if (type.equals("long") || type.equals(Long.class.getName())) {
            return Long.parseLong(value);
        } else if (type.equals("float") || type.equals(Float.class.getName())) {
            return Float.parseFloat(value);
        } else if (type.equals("double") || type.equals(Double.class.getName())) {
            return Double.parseDouble(value);
        } else if (type.equals("boolean") || type.equals(Boolean.class.getName())) {
            return Boolean.parseBoolean(value);
        } else {
            return value;
        }
    }

    /**
     * @param args program arguments
     */
    public static void main(final String[] args) {
        try {
            CompatibleJavaVersionCheck.checkForCompatibleJavaVersion();
        } catch (final StartException e) {
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                System.err.println(e.getMessage());
            }
            System.exit(e.getErrorCode());
        }

        final JMXtoXML client = new JMXtoXML();
        try {
            client.connect("localhost", 1099);
            System.out.println(client.generateReport(args));
        } catch (final IOException | TransformerException e) {
            e.printStackTrace();
        }
    }
}
