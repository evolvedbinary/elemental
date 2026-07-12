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

import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.EXistResourceSet;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XPathQueryService;

import static org.junit.Assert.assertEquals;

public class FunStrLengthTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer server = new ExistXmldbEmbeddedServer(true, true, true);

    @Test
    public void contextItemWithinPredicate() throws XMLDBException {
        final XPathQueryService queryService = (XPathQueryService) server.getRoot().getService("XQueryService", "1.0");

        // upon empty sequence
        try (final EXistResourceSet result = (EXistResourceSet) queryService.query("()[fn:string-length(.) gt 0]")) {
            assertEquals(0, result.getSize());
        }

             // upon computed empty sequence
        try (final EXistResourceSet result = (EXistResourceSet) queryService.query("fn:tokenize('', '/')[fn:string-length(.) gt 0]")) {
            assertEquals(0, result.getSize());
        }

        // upon non-empty sequence
        try (final EXistResourceSet result = (EXistResourceSet) queryService.query("('a', '', 'bc', '', 'def')[fn:string-length(.) gt 0]")) {
            assertEquals(3, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                assertEquals("a", resource.getContent());
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                assertEquals("bc", resource.getContent());
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                assertEquals("def", resource.getContent());
            }
        }

        // upon computed non-empty sequence
        try (final EXistResourceSet result = (EXistResourceSet) queryService.query("fn:tokenize('ab/c//def//g//', '/')[fn:string-length(.) gt 0]")) {
            assertEquals(4, result.getSize());
            try (final EXistResource resource = (EXistResource) result.getResource(0)) {
                assertEquals("ab", resource.getContent());
            }
            try (final EXistResource resource = (EXistResource) result.getResource(1)) {
                assertEquals("c", resource.getContent());
            }
            try (final EXistResource resource = (EXistResource) result.getResource(2)) {
                assertEquals("def", resource.getContent());
            }
            try (final EXistResource resource = (EXistResource) result.getResource(3)) {
                assertEquals("g", resource.getContent());
            }
        }
    }
}
