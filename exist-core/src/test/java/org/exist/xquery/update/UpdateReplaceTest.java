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
package org.exist.xquery.update;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;

import static org.junit.Assert.*;
import static org.xmldb.api.base.ResourceType.XML_RESOURCE;

public class UpdateReplaceTest extends AbstractTestUpdate {

    @Test
    public void replaceOnlyChildWhereParentHasNoAttributes() throws XMLDBException {
        final String testDocName = "replaceOnlyChildWhereParentHasNoAttributes.xml";
        final String testDoc = "<Test><Content><A/></Content></Test>";

        final String updateQuery =
                "let $content := doc('/db/test/" + testDocName + "')/Test/Content\n" +
                        "    let $legacy := $content/A\n" +
                        "    return\n" +
                        "      update replace $legacy with <AA/>,\n" +
                        "    doc('/db/test/" + testDocName + "')/Test";

        final XQueryService xqueryService = storeXMLStringAndGetQueryService(testDocName, testDoc);
        final ResourceSet result = xqueryService.query(updateQuery);
        assertNotNull(result);
        assertEquals(1, result.getSize());

        final Resource res1 = result.getResource(0);
        assertNotNull(res1);
        assertEquals(XML_RESOURCE, res1.getResourceType());
        final Document doc = ((XMLResource) res1).getContentAsDOM().getOwnerDocument();

        final Source actual = Input.fromDocument(doc).build();
        final Source expected = Input.fromString("<Test><Content><AA/></Content></Test>").build();

        final Diff diff = DiffBuilder
                .compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void replaceFirstChildWhereParentHasNoAttributes() throws XMLDBException {
        final String testDocName = "replaceFirstChildWhereParentHasNoAttributes.xml";
        final String testDoc = "<Test><Content><A/><A/></Content></Test>";

        final String updateQuery =
                "let $content := doc('/db/test/" + testDocName + "')/Test/Content\n" +
                        "    let $legacy := $content/A[1]\n" +
                        "    return\n" +
                        "      update replace $legacy with <AA/>,\n" +
                        "    doc('/db/test/" + testDocName + "')/Test";

        final XQueryService xqueryService = storeXMLStringAndGetQueryService(testDocName, testDoc);
        final ResourceSet result = xqueryService.query(updateQuery);
        assertNotNull(result);
        assertEquals(1, result.getSize());

        final Resource res1 = result.getResource(0);
        assertNotNull(res1);
        assertEquals(XML_RESOURCE, res1.getResourceType());
        final Document doc = ((XMLResource) res1).getContentAsDOM().getOwnerDocument();

        final Source actual = Input.fromDocument(doc).build();
        final Source expected = Input.fromString("<Test><Content><AA/><A/></Content></Test>").build();

        final Diff diff = DiffBuilder
                .compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void replaceOnlyChildWhereParentHasAttribute() throws XMLDBException {
        final String testDocName = "replaceOnlyChildWhereParentHasAttribute.xml";
        final String testDoc = "<Test><Content Foo=\"bar\"><A/></Content></Test>";

        final String updateQuery =
                "let $content := doc('/db/test/" + testDocName + "')/Test/Content\n" +
                "    let $legacy := $content/A\n" +
                "    return\n" +
                "      update replace $legacy with <AA/>,\n" +
                "    doc('/db/test/" + testDocName + "')/Test";

        final XQueryService xqueryService = storeXMLStringAndGetQueryService(testDocName, testDoc);
        final ResourceSet result = xqueryService.query(updateQuery);
        assertNotNull(result);
        assertEquals(1, result.getSize());

        final Resource res1 = result.getResource(0);
        assertNotNull(res1);
        assertEquals(XML_RESOURCE, res1.getResourceType());
        final Document doc = ((XMLResource) res1).getContentAsDOM().getOwnerDocument();

        final Source actual = Input.fromDocument(doc).build();
        final Source expected = Input.fromString("<Test><Content Foo='bar'><AA/></Content></Test>").build();

        final Diff diff = DiffBuilder
                .compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void replaceFirstChildWhereParentHasAttribute() throws XMLDBException {
        final String testDocName = "replaceFirstChildWhereParentHasAttribute.xml";
        final String testDoc = "<Test><Content Foo=\"bar\"><A/><A/></Content></Test>";

        final String updateQuery =
                "let $content := doc('/db/test/" + testDocName + "')/Test/Content\n" +
                        "    let $legacy := $content/A[1]\n" +
                        "    return\n" +
                        "      update replace $legacy with <AA/>,\n" +
                        "    doc('/db/test/" + testDocName + "')/Test";

        final XQueryService xqueryService = storeXMLStringAndGetQueryService(testDocName, testDoc);
        final ResourceSet result = xqueryService.query(updateQuery);
        assertNotNull(result);
        assertEquals(1, result.getSize());

        final Resource res1 = result.getResource(0);
        assertNotNull(res1);
        assertEquals(XML_RESOURCE, res1.getResourceType());
        final Document doc = ((XMLResource) res1).getContentAsDOM().getOwnerDocument();

        final Source actual = Input.fromDocument(doc).build();
        final Source expected = Input.fromString("<Test><Content Foo='bar'><AA/><A/></Content></Test>").build();

        final Diff diff = DiffBuilder
                .compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }
}
