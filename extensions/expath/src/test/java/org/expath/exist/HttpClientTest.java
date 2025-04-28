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
package org.expath.exist;

import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.Sequence;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class HttpClientTest {

    @ClassRule
    public static ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @Test
    public void readResponse() throws XPathException, PermissionDeniedException, EXistException {
        assumeTrue("No Internet access: skipping 'readResponse' test", hasInternetAccess());

        final String query =
                "xquery version \"3.1\";\n" +
                "import module namespace http=\"http://expath.org/ns/http-client\";\n" +
                "let $url := \"http://www.exist-db.org/exist/apps/homepage/resources/img/existdb.gif\"\n" +
                "let $request :=\n" +
                "    <http:request method=\"GET\" href=\"{$url}\"/>\n" +
                "let $response := http:send-request($request)\n" +
                "let $str := util:binary-to-string($response[2])\n" +
                "return\n" +
                "    $str";

        final Sequence result = executeQuery(query);
        assertEquals(1, result.getItemCount());
    }

    private Sequence executeQuery(final String query) throws EXistException, PermissionDeniedException, XPathException {
        final BrokerPool brokerPool = existEmbeddedServer.getBrokerPool();
        final XQuery xquery = brokerPool.getXQueryService();
        try (final DBBroker broker = brokerPool.getBroker()) {
            return xquery.execute(broker, query, null);
        }
    }

    private boolean hasInternetAccess() {
        boolean hasInternetAccess = false;

        //Checking that we have an Internet Access
        try {
            final URL url = new URL("http://www.exist-db.org");
            final URLConnection con = url.openConnection();
            if (con instanceof HttpURLConnection) {
                final HttpURLConnection httpConnection = (HttpURLConnection) con;
                hasInternetAccess = (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK);
            }
        } catch(final MalformedURLException e) {
            fail(e.getMessage());
        } catch (final IOException e) {
            //Ignore
        }

        return hasInternetAccess;
    }
}
