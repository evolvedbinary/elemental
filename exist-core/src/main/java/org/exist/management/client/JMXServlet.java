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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.management.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.storage.BrokerPool;
import org.exist.util.UUIDGenerator;
import org.exist.util.serializer.DOMSerializer;
import org.w3c.dom.Element;

import static org.exist.util.StringUtil.notNullOrEmpty;
import static org.exist.util.StringUtil.notNullOrEmptyOrWs;

/**
 * A servlet to monitor the database. It returns status information for the database based on the JMX interface. For
 * simplicity, the JMX beans provided by eXist are organized into categories. One calls the servlet with one or more
 * categories in parameter "c", e.g.:
 *
 * /exist/jmx?c=instances&amp;c=memory
 *
 * If no parameter is specified, all categories will be returned. Valid categories are "memory", "instances", "disk",
 * "system", "caches", "locking", "processes", "sanity", "all".
 *
 * The servlet can also be used to test if the database is responsive by using parameter "operation=ping" and a timeout
 * (t=timeout-in-milliseconds). For example, the following call
 *
 * /exist/jmx?operation=ping&amp;t=1000
 *
 * will wait for a response within 1000ms. If the ping returns within the specified timeout, the servlet returns the
 * attributes of the SanityReport JMX bean, which will include an element &lt;jmx:Status&gt;PING_OK&lt;/jmx:Status&gt;.
 * If the ping takes longer than the timeout, you'll instead find an element &lt;jmx:error&gt; in the returned XML. In
 * this case, additional information on running queries, memory consumption and database locks will be provided.
 *
 * @author wolf
 */
public class JMXServlet extends HttpServlet {

    protected final static Logger LOG = LogManager.getLogger(JMXServlet.class);

    private static final String TOKEN_KEY = "token";
    private static final String TOKEN_FILE = "jmxservlet.token";
    private static final String WEBINF_DATA_DIR = "WEB-INF/data";

    private final static Properties defaultProperties = new Properties();

    static {
        defaultProperties.setProperty(OutputKeys.INDENT, "yes");
        defaultProperties.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    }

    private JMXtoXML client;
    private final Set<String> serverAddresses = new HashSet<>();

    private Path dataDir;
    private Path tokenFile;


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Verify if request is from localhost or if user has specific servlet/container managed role.
        if (isFromLocalHost(request)) {
            // Localhost is always authorized to access
            if (LOG.isDebugEnabled()) {
                LOG.debug("Local access granted");
            }

        } else if (hasSecretToken(request, getToken())) {
            // Correct token is provided
            if (LOG.isDebugEnabled()) {
                LOG.debug("Correct token provided by {}", request.getRemoteHost());
            }

        } else {
            // Check if user is already authorized, e.g. via MONEX allow user too
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access allowed for localhost, or when correct token has been provided.");
            return;
        }

        // Perform actual writing of data
        writeXmlData(request, response);
    }

    private void writeXmlData(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Element root = null;

        final String operation = request.getParameter("operation");
        if ("ping".equals(operation)) {
            long timeout = 5000;
            final String timeoutParam = request.getParameter("t");
            if (notNullOrEmptyOrWs(timeoutParam)) {
                try {
                    timeout = Long.parseLong(timeoutParam);
                } catch (final NumberFormatException e) {
                    throw new ServletException("timeout parameter needs to be a number. Got: " + timeoutParam);
                }
            }

            final long responseTime = client.ping(BrokerPool.DEFAULT_INSTANCE_NAME, timeout);
            if (responseTime == JMXtoXML.PING_TIMEOUT) {
                root = client.generateXMLReport(String.format("no response on ping after %sms", timeout),
                        new String[]{"sanity", "locking", "processes", "instances", "memory"});
            } else {
                root = client.generateXMLReport(null, new String[]{"sanity"});
            }
        } else if (notNullOrEmpty(operation)) {
            final String mbean = request.getParameter("mbean");
            if (mbean == null) {
                throw new ServletException("to call an operation, you also need to specify parameter 'mbean'");
            }
            String[] args = request.getParameterValues("args");
            try {
                root = client.invoke(mbean, operation, args);
                if (root == null) {
                    throw new ServletException("operation " + operation + " not found on " + mbean);
                }
            } catch (InstanceNotFoundException e) {
                throw new ServletException("mbean " + mbean + " not found: " + e.getMessage(), e);
            } catch (MalformedObjectNameException | IntrospectionException | ReflectionException | MBeanException e) {
                throw new ServletException(e.getMessage(), e);
            }
        } else {
            String[] categories = request.getParameterValues("c");
            if (categories == null) {
                categories = new String[]{"all"};
            }
            root = client.generateXMLReport(null, categories);
        }

        response.setContentType("application/xml");

        final Object useAttribute = request.getAttribute("jmx.attribute");
        if (useAttribute != null) {
            request.setAttribute(useAttribute.toString(), root);

        } else {
            final Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
            final DOMSerializer streamer = new DOMSerializer(writer, defaultProperties);
            try {
                streamer.serialize(root);
            } catch (final TransformerException e) {
                LOG.error(e.getMessageAndLocation());
                throw new ServletException("Error while serializing result: " + e.getMessage(), e);
            }
            writer.flush();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        // Setup JMS client
        client = new JMXtoXML();
        client.connect();

        // Register all known localhost addresses
        registerServerAddresses();

        // Get directory for token file
        final String jmxDataDir = client.getDataDir();
        if (jmxDataDir == null) {
            dataDir = Paths.get(config.getServletContext().getRealPath(WEBINF_DATA_DIR)).normalize();
        } else {
            dataDir = Paths.get(jmxDataDir).normalize();
        }
        if (!Files.isDirectory(dataDir) || !Files.isWritable(dataDir)) {
            LOG.error("Cannot access directory {}", WEBINF_DATA_DIR);
        }

        // Setup token and tokenfile
        obtainTokenFileReference();

        LOG.info("JMXservlet token: {}", getToken());

    }

    /**
     * Register all known IP-addresses for server.
     */
    void registerServerAddresses() {
        // The IPv4 address of the loopback interface of the server - 127.0.0.1 on Windows/Linux/macOS, or 127.0.1.1 on Debian/Ubuntu
        try {
            serverAddresses.add(InetAddress.getLocalHost().getHostAddress());
        } catch (final UnknownHostException ex) {
            LOG.warn("Unable to get loopback IP address for localhost: {}", ex.getMessage());
        }

        // Any additional IPv4 and IPv6 addresses associated with the loopback interface of the server
        try {
            for (final InetAddress loopBackAddress : InetAddress.getAllByName("localhost")) {
                serverAddresses.add(loopBackAddress.getHostAddress());
            }
        } catch (final UnknownHostException ex) {
            LOG.warn("Unable to retrieve additional loopback IP addresses for localhost: {}", ex.getMessage());
        }

        // Any IPv4 and IPv6 addresses associated with other interfaces in the server
        try {
            for (final InetAddress hostAddress : InetAddress.getAllByName(InetAddress.getLocalHost().getHostName())) {
                serverAddresses.add(hostAddress.getHostAddress());
            }
        } catch (final UnknownHostException ex) {
            LOG.warn("Unable to retrieve additional interface IP addresses for localhost: {}", ex.getMessage());
        }

        if (serverAddresses.isEmpty()) {
            LOG.error("Unable to determine IP addresses for localhost, JMXServlet might be dysfunctional.");
        }
    }

    /**
     * Determine if HTTP request is originated from localhost.
     *
     * @param request The HTTP request
     * @return TRUE if request is from LOCALHOST otherwise FALSE
     */
    boolean isFromLocalHost(final HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr.charAt(0) == '[') {
            // Handle IPv6 addresses that are wrapped in []
            remoteAddr = remoteAddr.substring(1, remoteAddr.length() - 1);
        }
        return serverAddresses.contains(remoteAddr);
    }

    /**
     * Check if URL contains magic Token
     *
     * @param request The HTTP request
     * @return TRUE if request contains correct value for token, else FALSE
     */
    boolean hasSecretToken(HttpServletRequest request, String token) {
        final String[] tokenValues = request.getParameterValues(TOKEN_KEY);
        if (tokenValues != null) {
            for (final String tokenValue : tokenValues) {
                if (tokenValue.equals(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Obtain reference to token file
     */
    private void obtainTokenFileReference() {

        if (tokenFile == null) {
            tokenFile = dataDir.resolve(TOKEN_FILE);
            LOG.info("Token file:  {}", tokenFile.toAbsolutePath().toAbsolutePath());
        }
    }

    /**
     * Get token from file, create if not existent. Data is read for each call so the file can be updated run-time.
     *
     * @return Toke for servlet
     */
    private String getToken() {

        Properties props = new Properties();
        String token = null;

        // Read if possible
        if (Files.exists(tokenFile)) {

            try (final InputStream is = Files.newInputStream(tokenFile)) {
                props.load(is);
                token = props.getProperty(TOKEN_KEY);
            } catch (IOException ex) {
                LOG.error(ex.getMessage());
            }

        }

        // Create and write when needed
        if (!Files.exists(tokenFile) || token == null) {

            // Create random token
            token = UUIDGenerator.getUUIDversion4();

            // Set value to properties
            props.setProperty(TOKEN_KEY, token);

            // Write data to file
            try (final OutputStream os = Files.newOutputStream(tokenFile)) {
                props.store(os, "JMXservlet token: http://localhost:8080/exist/status?token=......");
            } catch (IOException ex) {
                LOG.error(ex.getMessage());
            }

            LOG.debug("Token written to file {}", tokenFile.toAbsolutePath().toString());

        }

        return token;
    }

}
