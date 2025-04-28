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
package org.exist.xquery;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;

import static org.junit.Assert.assertEquals;

/** Tests for various XQuery (XML Schema) simple types conversions.
 * @author jmvanel
 */
public class ConversionsTest {

	@ClassRule
	public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

	/** test conversion from QName to string */
	@Test
	public void qname2string() throws XMLDBException {
        final String query = "declare namespace foo = 'http://foo'; \n" +
                "let $a := ( xs:QName('foo:bar'), xs:QName('foo:john'), xs:QName('foo:doe') )\n" +
                    "for $b in $a \n" +
                        "return \n" +
                            "<blah>{string($b)}</blah>" ;
        final ResourceSet result = existEmbeddedServer.executeQuery( query );
        /* which returns :
            <blah>foo:bar</blah>
            <blah>foo:john</blah>
            <blah>foo:doe</blah>"
        */
        final String r = (String) result.getResource(0).getContent();
        assertEquals( "<blah>foo:bar</blah>", r );
        assertEquals( "XQuery: " + query, 3, result.getSize() );
	}
}
