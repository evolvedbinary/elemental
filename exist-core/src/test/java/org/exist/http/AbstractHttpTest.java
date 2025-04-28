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
package org.exist.http;

import com.evolvedbinary.j8fu.function.FunctionE;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.exist.TestUtils;
import org.exist.test.ExistWebServer;

import java.io.IOException;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractHttpTest {

    /**
     * Get the Server URI.
     *
     * @param existWebServer the Web Server.
     *
     * @return the Server URI.
     */
    protected static String getServerUri(final ExistWebServer existWebServer) {
        return "http://localhost:" + existWebServer.getPort() + "/exist";
    }

    /**
     * Get the URI of the Server's REST end-point.
     *
     * @param existWebServer the Web Server.
     *
     * @return the URI of the Server's REST end-point.
     */
    protected static String getRestUri(final ExistWebServer existWebServer) {
        return getServerUri(existWebServer) + "/rest";
    }

    /**
     * Get the URI of the Server's Apps end-point.
     *
     * @param existWebServer the Web Server.
     *
     * @return the URI of the Server's Apps end-point.
     */
    protected static String getAppsUri(final ExistWebServer existWebServer) {
        return getServerUri(existWebServer) + "/apps";
    }

    /**
     * Execute a function with a HTTP Client.
     *
     * @param <T> the return type of the <code>fn</code> function.
     * @param fn the function which accepts the HTTP Client.
     *
     * @return the result of the <code>fn</code> function.
     *
     * @throws IOException if an I/O error occurs
     */
    protected static <T> T withHttpClient(final FunctionE<HttpClient, T, IOException> fn) throws IOException {
        try (final CloseableHttpClient client = HttpClientBuilder
                .create()
                .disableAutomaticRetries()
                .build()) {
            return fn.apply(client);
        }
    }

    /**
     * Execute a function with a HTTP Executor.
     *
     * @param <T> the return type of the <code>fn</code> function.
     * @param existWebServer the Web Server.
     * @param fn the function which accepts the HTTP Executor.
     *
     * @return the result of the <code>fn</code> function.
     *
     * @throws IOException if an I/O error occurs
     */
    protected static <T> T withHttpExecutor(final ExistWebServer existWebServer, final FunctionE<Executor, T, IOException> fn) throws IOException {
        return withHttpClient(client -> {
            final Executor executor = Executor
                    .newInstance(client)
                    .auth(TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)
                    .authPreemptive(new HttpHost("localhost", existWebServer.getPort()));
            return fn.apply(executor);
        });
    }
}
