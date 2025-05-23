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
package org.exist.xquery.modules.xslfo;

import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ApacheFopTest {

    @ClassRule
    public static final ExistEmbeddedServer server = new ExistEmbeddedServer(true, true);

    @Test
    public void simplePdf() throws EXistException, PermissionDeniedException, XPathException {
        final String fopConfig =
                "<fop version=\"1.0\">\n" +
                "    <strict-configuration>true</strict-configuration>\n" +
                "    <strict-validation>false</strict-validation>\n" +
                "    <base>./</base>\n" +
                "    <renderers>\n" +
                "        <renderer mime=\"application/pdf\"></renderer>\n" +
                "    </renderers>\n" +
                "</fop>";

        final String fo =
                "<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">\n" +
                "    <fo:layout-master-set>\n" +
                "        <fo:simple-page-master master-name=\"page-left\" page-height=\"297mm\" page-width=\"210mm\" margin-bottom=\"10mm\" margin-top=\"10mm\" margin-left=\"36mm\" margin-right=\"18mm\">\n" +
                "            <fo:region-body margin-bottom=\"10mm\" margin-top=\"16mm\"/>\n" +
                "            <fo:region-before region-name=\"head-left\" extent=\"10mm\"/>\n" +
                "        </fo:simple-page-master>\n" +
                "        <fo:simple-page-master master-name=\"page-right\" page-height=\"297mm\" page-width=\"210mm\" margin-bottom=\"10mm\" margin-top=\"10mm\" margin-left=\"18mm\" margin-right=\"36mm\">\n" +
                "            <fo:region-body margin-bottom=\"10mm\" margin-top=\"16mm\"/>\n" +
                "            <fo:region-before region-name=\"head-right\" extent=\"10mm\"/>\n" +
                "        </fo:simple-page-master>\n" +
                "        <fo:page-sequence-master master-name=\"page-content\">\n" +
                "            <fo:repeatable-page-master-alternatives>\n" +
                "                <fo:conditional-page-master-reference master-reference=\"page-right\" odd-or-even=\"odd\"/>\n" +
                "                <fo:conditional-page-master-reference master-reference=\"page-left\" odd-or-even=\"even\"/>\n" +
                "            </fo:repeatable-page-master-alternatives>\n" +
                "        </fo:page-sequence-master>\n" +
                "    </fo:layout-master-set>\n" +
                "    <fo:page-sequence master-reference=\"page-content\">\n" +
                "         <fo:flow flow-name=\"xsl-region-body\" hyphenate=\"true\" language=\"en\" xml:lang=\"en\">\n" +
                "                <fo:block id=\"A97060-t\" line-height=\"16pt\" font-size=\"11pt\">\n" +
                "                    <fo:block id=\"A97060-e0\" page-break-after=\"right\">\n" +
                "                        <fo:block id=\"A97060-e100\" text-align=\"justify\" space-before=\".5em\" text-indent=\"1.5em\" space-after=\".5em\">\n" +
                "                            Hello World!\n" +
                "                        </fo:block>\n" +
                "                    </fo:block>\n" +
                "                </fo:block>\n" +
                "        </fo:flow>\n" +
                "    </fo:page-sequence>\n" +
                "</fo:root>";

        final String xquery =
                "xquery version \"3.1\";\n" +
                "\n" +
                "import module namespace xslfo=\"http://exist-db.org/xquery/xslfo\";\n" +
                "\n" +
                "let $config := " + fopConfig + "\n" +
                "let $fo := " + fo + "\n" +
                "\n" +
                "let $pdf := xslfo:render($fo, \"application/pdf\", (), $config)\n" +
                "return $pdf";

        final BrokerPool pool = server.getBrokerPool();
        final XQuery xqueryService = pool.getXQueryService();

        try (final DBBroker broker = pool.getBroker()) {
            final Sequence result = xqueryService.execute(broker, xquery, null);
            assertNotNull(result);
            assertEquals(1, result.getItemCount());
            final Item pdf = result.itemAt(0);
            assertEquals(Type.BASE64_BINARY, pdf.getType());
        }
    }
}
