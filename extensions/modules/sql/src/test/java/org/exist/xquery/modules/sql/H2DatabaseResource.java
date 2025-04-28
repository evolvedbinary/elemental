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
package org.exist.xquery.modules.sql;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.tools.Server;
import org.junit.rules.ExternalResource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Embedded H2 Database JUnit Test Resource.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class H2DatabaseResource extends ExternalResource {

    private static final Logger LOG =  LogManager.getLogger(H2DatabaseResource.class);

    private static final String DEFAULT_URL = "jdbc:h2:mem:test-1";
    private static final String DEFAULT_USER = "sa";
    private static final String DEFAULT_PASSWORD = "sa";

    private final String url;
    private final String user;
    private final String password;
    private final Optional<Integer> tcpPort;
    private Connection rootConnection = null;
    private Server server = null;

    public H2DatabaseResource() {
        this(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
    }

    public H2DatabaseResource(final String url, final String user, final String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.tcpPort = Optional.empty();
    }

    public H2DatabaseResource(final String url, final String user, final String password, final int tcpPort) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.tcpPort = Optional.of(tcpPort);
    }

    @Override
    protected void before() throws SQLException {
        if (rootConnection == null) {
            org.h2.Driver.load();

            // Start the server if configured to do so
            if (this.tcpPort.isPresent()) {
                this.server = Server.createTcpServer(new String[] { "-tcpPort", Integer.toString(tcpPort.get()) });
                this.server.start();
            }

            this.rootConnection = DriverManager.getConnection(url, user, password);

            LOG.info("Started H2Database...");

        } else {
            throw new IllegalStateException("H2Database is already running");
        }

    }

    @Override
    protected void after() {
        if (rootConnection != null) {
            try {
                final Statement stat = rootConnection.createStatement();
                stat.execute("SHUTDOWN");
                stat.close();
            } catch (final Exception e) {
                LOG.error(e);
            }
            try {
                rootConnection.close();
                rootConnection = null;
            } catch (final Exception e) {
                LOG.error(e);
            }
            if (server != null) {
                server.stop();
                server = null;
            }

            LOG.info("Stopped H2Database.");

        } else {
            throw new IllegalStateException("H2Database already stopped");
        }
    }

    public Class getDriverClass() {
        return org.h2.Driver.class;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public Optional<Integer> getTcpPort() {
        return tcpPort;
    }

    public Connection getEmbeddedConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
