/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Disabled;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;

/**
 * Small test to show than entities are required to be resolved.
 * 
 * @author wessels
 */
public class DtdEntityTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    @Test
    void loadWithEntities() throws XMLDBException {
        final String input = "<a>first empty: &empty; then trade: &trade; </a>";

        try (final Collection col = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "entity")) {
            XmldbEmbeddedDatabaseExtension.storeResource(col, "docname.xml", input.getBytes());

            // should throw XMLDBException
            XmldbEmbeddedDatabaseExtension.getXMLResource(col, "docname.xml");

        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().contains("The entity \"empty\" was referenced, but not declared"));
            return;
        }

        fail("Should have thrown XMLDBException");
    }

    @Test
    @Disabled("Entity resolve bug")
    void bugloadWithEntities() throws XMLDBException {
        final String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE procedure PUBLIC \"-//AAAA//DTD Procedure 0.4//EN\" \"aaaa.dtd\" >"
                + "<a>first empty: &empty; then trade: &trade; </a>";

        try (final Collection col = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), "entity")) {
            XMLDB_EMBEDDED_DATABASE.storeResource(col, "docname.xml", input.getBytes(UTF_8));

            // should throw XMLDBException
            XmldbEmbeddedDatabaseExtension.getXMLResource(col, "docname.xml");

        } catch (final XMLDBException e) {
            assertTrue(e.getMessage().contains("The entity \"empty\" was referenced, but not declared"));
            return;
        }

        fail("Should have thrown XMLDBException");
    }
}
