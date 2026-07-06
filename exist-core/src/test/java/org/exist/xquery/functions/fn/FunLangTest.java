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

import com.googlecode.junittoolbox.ParallelRunner;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResourceSet;
import org.junit.ClassRule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.junit.runner.RunWith;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

/**
 *
 * @author ljo
 */
@RunWith(ParallelRunner.class)
public class FunLangTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existEmbeddedServer = new ExistXmldbEmbeddedServer(true, true, true);

    @Test
    public void testFnLangWithContext() throws XMLDBException {
		final String query =
			"let $doc-frag := " +
			"<desclist xml:lang=\"en\">" +
			"<desc xml:lang=\"en-US\" n=\"1\">" +
			"<line>The first line of the description.</line>" +
			"</desc>" +
			"<desc xml:lang=\"fr\" n=\"2\">"+
			"<line>La premi&#232;re ligne de la déscription.</line>" +
			"</desc>" +
			"</desclist>" +
			"return " +
			"$doc-frag//desc[lang(\"en-US\")]";
		try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
			assertEquals(1, result.getSize());
			try (final Resource resource = result.getResource(0)) {
				assertEquals("<desc xml:lang=\"en-US\" n=\"1\">\n    <line>The first line of the description.</line>\n</desc>", resource.getContent());
			}
		}
    }

	@Test
    public void testFnLangWithArgument() throws XMLDBException {
		final String query =
			"let $doc-frag := " +
			"<desclist xml:lang=\"en\">" +
			"<desc xml:lang=\"en-US\" n=\"1\">" +
			"<line>The first line of the description.</line>" +
			"</desc>" +
			"<desc xml:lang=\"fr\" n=\"2\">"+
			"<line>La premi&#232;re ligne de la déscription.</line>" +
			"</desc>" +
			"</desclist>" +
			"return " +
			"lang(\"en-US\", $doc-frag//desc[@n eq \"2\"])";
		try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
			assertEquals(1, result.getSize());
			try (final Resource resource = result.getResource(0)) {
				assertEquals("false", resource.getContent());
			}
		}
    }
    
    @Test
    public void testFnLangWithAttributeArgument() throws XMLDBException {
		final String query =
			"let $doc-frag := " +
			"<desclist xml:lang=\"en\">" +
			"<desc xml:lang=\"en-US\" n=\"1\">" +
			"<line>The first line of the description.</line>" +
			"</desc>" +
			"<desc xml:lang=\"fr\" n=\"2\">"+
			"<line>La premi&#232;re ligne de la déscription.</line>" +
			"</desc>" +
			"</desclist>" +
			"return " +
			"lang(\"en-US\", $doc-frag//desc/@n[. eq \"1\"])";
		try (final EXistResourceSet result = existEmbeddedServer.executeQuery(query)) {
			assertEquals(1, result.getSize());
			try (final Resource resource = result.getResource(0)) {
				assertEquals("true", resource.getContent());
			}
		}
    }
}
