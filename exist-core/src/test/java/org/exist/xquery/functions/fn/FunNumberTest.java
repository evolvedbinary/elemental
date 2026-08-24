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
package org.exist.xquery.functions.fn;

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.xmldb.EXistResourceSet;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.xmldb.api.base.ResourceSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author aretter
 */
public class FunNumberTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(true, true, true);

    @Test
    void fnNumberWithContext() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(
            "let $errors := " +
                "<report>" +
                    "<message level=\"Error\" line=\"1191\" column=\"49\" repeat=\"96\"></message>" +
                    "<message level=\"Error\" line=\"161740\" column=\"25\"></message>" +
                    "<message level=\"Error\" line=\"162327\" column=\"92\" repeat=\"87\"></message>" +
                    "<message level=\"Error\" line=\"255090\" column=\"25\">c</message>" +
                    "<message level=\"Error\" line=\"255702\" column=\"414\" repeat=\"9\"></message>" +
                "</report>" +
            "return sum($errors//message/(@repeat/number(),1)[1])"
        )) {

            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("194", resource.getContent());
            }
        }
    }

    @Test
    void fnNumberWithArgument() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery(
            "let $errors := " +
                "<report>" +
                    "<message level=\"Error\" line=\"1191\" column=\"49\" repeat=\"96\"></message>" +
                    "<message level=\"Error\" line=\"161740\" column=\"25\"></message>" +
                    "<message level=\"Error\" line=\"162327\" column=\"92\" repeat=\"87\"></message>" +
                    "<message level=\"Error\" line=\"255090\" column=\"25\">c</message>" +
                    "<message level=\"Error\" line=\"255702\" column=\"414\" repeat=\"9\"></message>" +
                "</report>" +
            "return sum($errors//message/(number(@repeat),1)[1])"
        )) {

            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("NaN", resource.getContent());
            }
        }
    }
}
