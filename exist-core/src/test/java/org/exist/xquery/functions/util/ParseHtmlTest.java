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
package org.exist.xquery.functions.util;

import org.exist.EXistException;
import org.exist.dom.memtree.DocumentImpl;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.Sequence;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;

import static org.junit.Assert.*;

public class ParseHtmlTest {

    @ClassRule
    public static final ExistEmbeddedServer server = new ExistEmbeddedServer(true, true);

    @Test
    public void parseHtml() throws EXistException, PermissionDeniedException, XPathException {
        final String query = "util:parse-html(\"<p>hello <img src='1.jpg'></p>\")";

        final XQuery xquery = server.getBrokerPool().getXQueryService();
        try (final DBBroker broker = server.getBrokerPool().getBroker()) {
            final Sequence result = xquery.execute(broker, query, null);
            assertEquals(1, result.getItemCount());
            assertTrue(result.itemAt(0) instanceof DocumentImpl);

            final Source expected = Input.fromString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><HTML><head xmlns=\"http://www.w3.org/1999/xhtml\"/><BODY><p>hello <img src=\"1.jpg\"/></p></BODY></HTML>").build();
            final Source actual = Input.fromDocument((DocumentImpl) result.itemAt(0)).build();

            final Diff diff = DiffBuilder
                    .compare(expected)
                    .withTest(actual)
                    .checkForIdentical()
                    .build();

            assertFalse(diff.toString(), diff.hasDifferences());
        }
    }
}
