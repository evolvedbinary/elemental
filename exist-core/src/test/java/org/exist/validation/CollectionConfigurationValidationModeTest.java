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
package org.exist.validation;

import com.evolvedbinary.j8fu.function.RunnableE;
import org.exist.test.ExistXmldbEmbeddedServer;

import org.junit.*;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;

import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.modules.CollectionManagementService;

import static org.exist.collections.CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE;
import static org.junit.Assert.*;

/**
 * Switch validation mode yes/no/auto per collection and validate.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author wessels
 */
public class CollectionConfigurationValidationModeTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    private static final String valid = "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://jmvanel.free.fr/xsd/addressBook\" elementFormDefault=\"qualified\">" + "<xsd:attribute name=\"uselessAttribute\" type=\"xsd:string\"/>" + "<xsd:complexType name=\"record\">" + "<xsd:sequence>" + "<xsd:element name=\"cname\" type=\"xsd:string\"/>" + "<xsd:element name=\"email\" type=\"xsd:string\"/>" + "</xsd:sequence>" + "</xsd:complexType>" + "<xsd:element name=\"addressBook\">" + "<xsd:complexType>" + "<xsd:sequence>" + "<xsd:element name=\"owner\" type=\"record\"/>" + "<xsd:element name=\"person\" type=\"record\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>" + "</xsd:sequence>" + "</xsd:complexType>" + "</xsd:element>" + "</xsd:schema>";
    private static final String invalid = "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://jmvanel.free.fr/xsd/addressBook\" elementFormDefault=\"qualified\">" + "<xsd:attribute name=\"uselessAttribute\" type=\"xsd:string\"/>" + "<xsd:complexType name=\"record\">" + "<xsd:sequence>" + "<xsd:elementa name=\"cname\" type=\"xsd:string\"/>" + "<xsd:elementb name=\"email\" type=\"xsd:string\"/>" + "</xsd:sequence>" + "</xsd:complexType>" + "<xsd:element name=\"addressBook\">" + "<xsd:complexType>" + "<xsd:sequence>" + "<xsd:element name=\"owner\" type=\"record\"/>" + "<xsd:element name=\"person\" type=\"record\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>" + "</xsd:sequence>" + "</xsd:complexType>" + "</xsd:element>" + "</xsd:schema>";
    private static final String anonymous = "<schema elementFormDefault=\"qualified\">" + "<attribute name=\"uselessAttribute\" type=\"string\"/>" + "<complexType name=\"record\">" + "<sequence>" + "<elementa name=\"cname\" type=\"string\"/>" + "<elementb name=\"email\" type=\"string\"/>" + "</sequence>" + "</complexType>" + "<element name=\"addressBook\">" + "<complexType>" + "<sequence>" + "<element name=\"owner\" type=\"record\"/>" + "<element name=\"person\" type=\"record\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>" + "</sequence>" + "</complexType>" + "</element>" + "</schema>";
    private static final String different = "<asd:schema xmlns:asd=\"https://www.w3.org/2001/XMLSchemaschema\" targetNamespace=\"http://jmvanel.free.fr/xsd/addressBookbook\" elementFormDefault=\"qualified\">" + "<asd:attribute name=\"uselessAttribute\" type=\"asd:string\"/>" + "<asd:complexType name=\"record\">" + "<asd:sequence>" + "<asd:element name=\"cname\" type=\"asd:string\"/>" + "<asd:element name=\"email\" type=\"asd:string\"/>" + "</asd:sequence>" + "</asd:complexType>" + "<asd:element name=\"addressBook\">" + "<asd:complexType>" + "<asd:sequence>" + "<asd:element name=\"owner\" type=\"record\"/>" + "<asd:element name=\"person\" type=\"record\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>" + "</asd:sequence>" + "</asd:complexType>" + "</asd:element>" + "</asd:schema>";

    private static final String xconf_yes = "<collection xmlns=\"http://exist-db.org/collection-config/1.0\"><validation mode=\"yes\"/></collection>";
    private static final String xconf_no = "<collection xmlns=\"http://exist-db.org/collection-config/1.0\"><validation mode=\"no\"/></collection>";
    private static final String xconf_auto = "<collection xmlns=\"http://exist-db.org/collection-config/1.0\"><validation mode=\"auto\"/></collection>";

    @AfterClass
    public static void tearDownClass() throws Exception {
        existEmbeddedServer.executeQuery("validation:clear-grammar-cache()");
    }

    @Before
    public void setUp() throws Exception {
        existEmbeddedServer.executeQuery("validation:clear-grammar-cache()");
    }

    private void createCollection(final String collection) throws XMLDBException {
        final CollectionManagementService cmservice = existEmbeddedServer.getRoot().getService(CollectionManagementService.class);
        Collection testCollection = cmservice.createCollection(collection);
        assertNotNull(testCollection);

        testCollection = cmservice.createCollection("/db/system/config" + collection);
        assertNotNull(testCollection);
    }

    private void storeCollectionXconf(final String collection, final String document) throws XMLDBException {
        final ResourceSet result = existEmbeddedServer.executeQuery("xmldb:store(\"" + collection + "\", \"" + DEFAULT_COLLECTION_CONFIG_FILE + "\", " + document + ")");
        final String r = (String) result.getResource(0).getContent();
        assertEquals("Store xconf", collection + "/" + DEFAULT_COLLECTION_CONFIG_FILE, r);
    }

    private void storeDocument(final String collection, final String name, final String document) throws XMLDBException {
        final ResourceSet result = existEmbeddedServer.executeQuery("xmldb:store(\"" + collection + "\", \"" + name + "\", " + document + ")");
        final String r = (String) result.getResource(0).getContent();
        assertEquals("Store doc", collection + "/" + name, r);
    }

    @Test
    public void insertModeFalse() throws XMLDBException {
        createCollection("/db/false");
        storeCollectionXconf("/db/system/config/db/false", xconf_no);

        // namespace provided, valid document; should pass
        storeDocument("/db/false", "valid.xml", valid);

        // namespace provided, invalid document; should pass
        storeDocument("/db/false", "invalid.xml", invalid);

        // no namespace provided, should pass
        storeDocument("/db/false", "anonymous.xml", anonymous);

        // different namespace provided, should pass
        storeDocument("/db/false", "different.xml", different);
    }

    @Test
    public void insertModeTrue() throws XMLDBException {
        createCollection("/db/true");
        storeCollectionXconf("/db/system/config/db/true", xconf_yes);

        // namespace provided, valid document; should pass
        storeDocument("/db/true", "valid.xml", valid);

        // namespace provided, invalid document; should fail
        assertThrowsMessage("cvc-complex-type.2.4.a: Invalid content was found", () -> storeDocument("/db/true", "invalid.xml", invalid));

        // no namespace provided; should fail
        assertThrowsMessage("Cannot find the declaration of element 'schema'.", () -> storeDocument("/db/true", "anonymous.xml", anonymous));

        // different namespace provided, should fail
        assertThrowsMessage("Cannot find the declaration of element 'asd:schema'.", () -> storeDocument("/db/true", "different.xml", different));
    }

    @Test
    public void insertModeAuto() throws XMLDBException {
        createCollection("/db/auto");
        storeCollectionXconf("/db/system/config/db/auto", xconf_auto);

        // namespace provided, valid document; should pass
        storeDocument("/db/auto", "valid.xml", valid);

        // namespace provided, invalid document, should fail
        assertThrowsMessage("cvc-complex-type.2.4.a: Invalid content was found", () -> storeDocument("/db/auto", "invalid.xml", invalid));

        // no namespace reference, should pass
        storeDocument("/db/auto", "anonymous.xml", anonymous);

        // different namespace provided, should pass
        storeDocument("/db/auto", "different.xml", different);
    }

    private void assertThrowsMessage(final String expectedExceptionMessage, final RunnableE<XMLDBException> runnable) {
        try {
            runnable.run();
            fail("Should have raised an exception containing the error message: " + expectedExceptionMessage);
        } catch (final XMLDBException ex) {
            final String msg = ex.getMessage();
            assertTrue(expectedExceptionMessage, msg.contains(expectedExceptionMessage));
        }
    }
}