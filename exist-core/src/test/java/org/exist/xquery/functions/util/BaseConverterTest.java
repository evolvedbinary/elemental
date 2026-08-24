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
package org.exist.xquery.functions.util;

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.xmldb.EXistResourceSet;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

/**
 * @author ljo
 */
public class BaseConverterTest {

    @RegisterExtension
    public static XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    @Test
    void baseConverterOctalToInt() throws XMLDBException {
        final String query = "util:base-to-integer(0755, 8)";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("493", r);
            }
        }
    }

    @Test
    void baseConverterIntToHex() throws XMLDBException {
        final String query = "util:integer-to-base(10, 16)";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("a", r);
            }
        }
    }

    @Test
    void baseConverterIntToBinary() throws XMLDBException {
        final String query = "util:integer-to-base(4, 2)";
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("100", r);
            }
        }
    }
}
