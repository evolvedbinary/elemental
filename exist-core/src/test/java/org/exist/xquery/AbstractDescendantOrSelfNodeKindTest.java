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
package org.exist.xquery;

import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResourceSet;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
public abstract class AbstractDescendantOrSelfNodeKindTest {

    @ClassRule
    public final static ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    protected final static String TEST_DOCUMENT = "<doc xml:id=\"x\">\n"+
        "<?xml-stylesheet type=\"text/xsl\" href=\"test\"?>\n"+
        "    <a>\n"+
        "        <b x=\"1\">text<e>text</e>text</b>\n"+
        "        </a>\n"+
        "    <a>\n"+
        "        <c><!--comment-->\n"+
        "            <d xmlns=\"x\" y=\"2\" z=\"3\">text</d>\n"+
        "            </c>\n"+
        "    </a>\n"+
        "    <d><![CDATA[ & ]]></d>\n"+
        "</doc>";


    protected abstract EXistResourceSet executeQueryOnDoc(final String docQuery) throws XMLDBException;

    @Test
    public void documentNodeCount() throws XMLDBException {
        try (final EXistResourceSet result = executeQueryOnDoc("count($doc//document-node())")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals(0, Integer.parseInt((String) resource.getContent()));
            }
        }
    }

    @Test
    public void nodeCount() throws XMLDBException {
        try (final EXistResourceSet result = executeQueryOnDoc("count($doc//node())")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals(26, Integer.parseInt((String) resource.getContent()));
            }
        }
    }

    @Test
    public void elementCount() throws XMLDBException {
        try (final EXistResourceSet result = executeQueryOnDoc("count($doc//element())")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals(8, Integer.parseInt((String) resource.getContent()));
            }
        }
    }

    @Test
    public void textCount() throws XMLDBException {
        try (final EXistResourceSet result = executeQueryOnDoc("count($doc//text())")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals(16, Integer.parseInt((String) resource.getContent()));
            }
        }
    }

    @Test
    public void attributeCount() throws XMLDBException {
        try (final EXistResourceSet result = executeQueryOnDoc("count($doc//attribute())")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals(4, Integer.parseInt((String) resource.getContent()));
            }
        }
    }

    @Test
    public void commentCount() throws XMLDBException {
        try (final EXistResourceSet result = executeQueryOnDoc("count($doc//comment())")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals(1, Integer.parseInt((String) resource.getContent()));
            }
        }
    }

    @Test
    public void processingInstructionCount() throws XMLDBException {
        try (final EXistResourceSet result = executeQueryOnDoc("count($doc//processing-instruction())")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals(1, Integer.parseInt((String) resource.getContent()));
            }
        }
    }

    @Test
    public void qnameTest() throws XMLDBException {
        try (final EXistResourceSet result = executeQueryOnDoc("name($doc//d/node())")) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("", resource.getContent());
            }
        }
    }
}
