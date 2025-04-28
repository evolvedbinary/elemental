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
package org.exist.management;

import com.evolvedbinary.j8fu.function.FunctionE;
import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.exist.test.ExistWebServer;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.management.client.JMXtoXML.JMX_NAMESPACE;
import static org.exist.management.client.JMXtoXML.JMX_PREFIX;
import static org.exist.util.JREUtil.IS_JAVA_1_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

public class JmxRemoteTest {

    @ClassRule
    public static final ExistWebServer existWebServer = new ExistWebServer(true, false, true, true, false);

    private static String getServerUri() {
        return "http://localhost:" + existWebServer.getPort() + "/exist/status";
    }

    @Test
    public void checkContent() throws IOException {
        // Get content
        final Request request = Request.Get(getServerUri());
        final String jmxXml = withHttpExecutor(executor -> executor.execute(request).returnContent().asString());

        // Prepare XPath validation
        final Map<String, String> prefix2Uri = new HashMap<>();
        prefix2Uri.put(JMX_PREFIX, JMX_NAMESPACE);

        // Java GC
        if (IS_JAVA_1_8) {
            assertThat(jmxXml, hasXPath("//jmx:GarbageCollectorImpl").withNamespaceContext(prefix2Uri));
        } else {
            assertThat(jmxXml, hasXPath("//jmx:GarbageCollectorExtImpl").withNamespaceContext(prefix2Uri));
        }

        // Jetty
        assertThat(jmxXml, hasXPath("//jmx:WebAppContext").withNamespaceContext(prefix2Uri));

        assertThat(jmxXml, hasXPath("//jmx:ProcessReport").withNamespaceContext(prefix2Uri));
        assertThat(jmxXml, hasXPath("//jmx:Cache").withNamespaceContext(prefix2Uri));
        assertThat(jmxXml, hasXPath("//jmx:SystemInfo").withNamespaceContext(prefix2Uri));
        assertThat(jmxXml, hasXPath("//jmx:CacheManager").withNamespaceContext(prefix2Uri));
        assertThat(jmxXml, hasXPath("//jmx:CollectionCache").withNamespaceContext(prefix2Uri));
        assertThat(jmxXml, hasXPath("//jmx:LockTable").withNamespaceContext(prefix2Uri));
        assertThat(jmxXml, hasXPath("//jmx:SanityReport").withNamespaceContext(prefix2Uri));
        assertThat(jmxXml, hasXPath("//jmx:Database").withNamespaceContext(prefix2Uri));
    }

    @Test
    public void checkBasicRequest() throws IOException {
        final Request request = Request.Get(getServerUri())
                .addHeader(new BasicHeader("Accept", ContentType.APPLICATION_XML.toString()));

         final Tuple2<Integer, String> codeAndMediaType = withHttpExecutor(executor -> {
            final HttpResponse response = executor.execute(request).returnResponse();
            return Tuple(response.getStatusLine().getStatusCode(), response.getEntity().getContentType().getValue());
        });

        assertEquals(Tuple(HttpStatus.SC_OK, "application/xml"), codeAndMediaType);
    }

    private static <T> T withHttpClient(final FunctionE<HttpClient, T, IOException> fn) throws IOException {
        try (final CloseableHttpClient client = HttpClientBuilder
                .create()
                .disableAutomaticRetries()
                .build()) {
            return fn.apply(client);
        }
    }

    private static <T> T withHttpExecutor(final FunctionE<Executor, T, IOException> fn) throws IOException {
        return withHttpClient(client -> {
            final Executor executor = Executor.newInstance(client);
            return fn.apply(executor);
        });
    }
}
