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
package org.exist.xquery.functions.xmldb;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.concurrent.DBUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XPathQueryService;

import static org.junit.Assert.*;

/**
 * Due to limitation of ExistXmldbEmbeddedServer we need to split this test to two files.
 * It's not possible to have two instances of ExistXmldbEmbeddedServer at the same time.
 */
public class DbStoreTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    private final static String TEST_COLLECTION = "testAnyUri";

    @Test
    public final void simpleTest() throws XMLDBException {
        final Collection rootCol = existEmbeddedServer.getRoot();
        Collection testCol = rootCol.getChildCollection(TEST_COLLECTION);
        if (testCol == null) {
            testCol = DBUtils.addCollection(rootCol, TEST_COLLECTION);
            assertNotNull(testCol);
        }

        final XPathQueryService xpqs = testCol.getService(XPathQueryService.class);
        assertThrows(XMLDBException.class, () -> xpqs.query(
                "xmldb:store(\n" +
                        "        '/db',\n" +
                        "        'image.jpg',\n" +
                        "        xs:anyURI('https://www.example.com/image.jpg'),\n" +
                        "        'image/png'\n" +
                        "    )"));


    }

}
