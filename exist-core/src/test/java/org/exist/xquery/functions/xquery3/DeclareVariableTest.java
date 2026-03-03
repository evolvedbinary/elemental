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
package org.exist.xquery.functions.xquery3;

import static org.junit.Assert.assertEquals;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResourceSet;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

public class DeclareVariableTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    @Test
    public void defaultNamespaceTest() throws XMLDBException {
        final String query = "xquery version \"3.1\";\n"
            + "\n"
            + "    declare default element namespace \"http://www.w3.org/1999/xhtml\";\n"
            + "\n"
            + "    declare variable $docName := 'test.xml';\n"
            + "    declare variable $docPath := concat('/db/', $docName);\n"
            + "\n"
            + "    $docPath";

        try (final EXistResourceSet results = existEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = results.getResource(0)) {
                final String r = (String) resource.getContent();

                assertEquals("/db/test.xml", r);
            }
        }
    }
}
