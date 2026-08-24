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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exist.TestUtils;
import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.XmldbURI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XPathQueryService;

import static org.junit.jupiter.api.Assertions.*;

public class SeqOpTest {
	private static XPathQueryService query;
	private static Collection c;

    @Test
    void reverseEmpty() throws XMLDBException {
		assertSeq(new String[0], "reverse(())");
	}

    @Test
    void reverseAtomic1() throws XMLDBException {
		assertSeq(new String[]{"a"}, "reverse(('a'))");
	}

    @Test
    void reverseAtomic2() throws XMLDBException {
		assertSeq(new String[]{"b", "a"}, "reverse(('a', 'b'))");
	}

    @Test
    void reverseNodes1() throws XMLDBException {
		createDocument("foo", "<top><a/><b/></top>");
		assertSeq(new String[]{"<a/>"}, "reverse(//a)");
	}

    @Test
    void reverseNodes2() throws XMLDBException {
		createDocument("foo", "<top><a/><b/></top>");
		assertSeq(new String[]{"<b/>", "<a/>"}, "reverse(/top/*)");
	}

    @Test
    void reverseMixed() throws XMLDBException {
		createDocument("foo", "<top><a/><b/></top>");
		assertSeq(new String[]{"c", "<b/>", "<a/>"}, "reverse((/top/*, 'c'))");
	}

    @Test
    void removeEmpty1() throws XMLDBException {
		assertSeq(new String[0], "remove((), 1)");
	}

    @Test
    void removeEmpty2() throws XMLDBException {
		assertSeq(new String[0], "remove((), 0)");
	}

    @Test
    void removeEmpty3() throws XMLDBException {
		assertSeq(new String[0], "remove((), 42)");
	}

    @Test
    void removeOutOfBounds1() throws XMLDBException {
		assertSeq(new String[]{"a", "b"}, "remove(('a', 'b'), 0)");
	}

    @Test
    void removeOutOfBounds2() throws XMLDBException {
		assertSeq(new String[]{"a", "b"}, "remove(('a', 'b'), 3)");
	}

    @Test
    void removeOutOfBounds3() throws XMLDBException {
		assertSeq(new String[]{"a", "b"}, "remove(('a', 'b'), -1)");
	}

    @Test
    void removeAtomic1() throws XMLDBException {
		assertSeq(new String[]{"b", "c"}, "remove(('a', 'b', 'c'), 1)");
	}

    @Test
    void removeAtomic2() throws XMLDBException {
		assertSeq(new String[]{"a", "c"}, "remove(('a', 'b', 'c'), 2)");
	}

    @Test
    void removeAtomic3() throws XMLDBException {
		assertSeq(new String[]{"a", "b"}, "remove(('a', 'b', 'c'), 3)");
	}

    @Test
    void removeMixed1() throws XMLDBException {
		createDocument("foo", "<top><a/><b/></top>");
		assertSeq(new String[]{"<b/>", "a", "b", "c"}, "remove((/top/*, 'a', 'b', 'c'), 1)");
	}

    @Test
    void removeMixed2() throws XMLDBException {
		createDocument("foo", "<top><a/><b/></top>");
		assertSeq(new String[]{"<a/>", "a", "b", "c"}, "remove((/top/*, 'a', 'b', 'c'), 2)");
	}

    @Test
    void removeMixed3() throws XMLDBException {
		createDocument("foo", "<top><a/><b/></top>");
		assertSeq(new String[]{"<a/>", "<b/>", "b", "c"}, "remove((/top/*, 'a', 'b', 'c'), 3)");
	}

    @Test
    void removeNodes1() throws XMLDBException {
		createDocument("foo", "<top><a/><b/><c/></top>");
		assertSeq(new String[]{"<b/>", "<c/>"}, "remove(/top/*, 1)");
	}

    @Test
    void removeNodes2() throws XMLDBException {
		createDocument("foo", "<top><a/><b/><c/></top>");
		assertSeq(new String[]{"<a/>", "<c/>"}, "remove(/top/*, 2)");
	}

    @Test
    void removeNodes3() throws XMLDBException {
		createDocument("foo", "<top><a/><b/><c/></top>");
		assertSeq(new String[]{"<a/>", "<b/>"}, "remove(/top/*, 3)");
	}

    @Test
    void insertEmpty1() throws XMLDBException {
		assertSeq(new String[0], "insert-before((), 1, ())");
	}

    @Test
    void insertEmpty2() throws XMLDBException {
		assertSeq(new String[]{"a"}, "insert-before((), 1, ('a'))");
	}

    @Test
    void insertEmpty3() throws XMLDBException {
		assertSeq(new String[]{"a"}, "insert-before(('a'), 1, ())");
	}

    @Test
    void insertOutOfBounds1() throws XMLDBException {
		assertSeq(new String[]{"c", "d", "a", "b"}, "insert-before(('a', 'b'), 0, ('c', 'd'))");
	}

    @Test
    void insertOutOfBounds2() throws XMLDBException {
		assertSeq(new String[]{"a", "b", "c", "d"}, "insert-before(('a', 'b'), 3, ('c', 'd'))");
	}

    @Test
    void insertOutOfBounds3() throws XMLDBException {
		assertSeq(new String[]{"a", "b", "c", "d"}, "insert-before(('a', 'b'), 4, ('c', 'd'))");
	}

    @Test
    void insertAtomic1() throws XMLDBException {
		assertSeq(new String[]{"a", "c", "d", "b"}, "insert-before(('a', 'b'), 2, ('c', 'd'))");
	}

    @Test
    void insertAtomic2() throws XMLDBException {
		assertSeq(new String[]{"c", "d", "a", "b"}, "insert-before(('a', 'b'), 1, ('c', 'd'))");
	}

    @Test
    void insertAtomic3() throws XMLDBException {
		assertSeq(new String[]{"a", "a", "b", "b"}, "insert-before(('a', 'b'), 2, ('a', 'b'))");
	}

    @Test
    void insertNodes1() throws XMLDBException {
		createDocument("foo", "<top><x><a/><b/></x><y><c/><d/></y></top>");
		assertSeq(new String[]{"<a/>", "<c/>", "<d/>", "<b/>"}, "insert-before(/top/x/*, 2, /top/y/*)");
	}

    @Test
    void insertNodes2() throws XMLDBException {
		createDocument("foo", "<top><x><a/><b/></x><y><c/><d/></y></top>");
		assertSeq(new String[]{"<c/>", "<d/>", "<a/>", "<b/>"}, "insert-before(/top/x/*, 1, /top/y/*)");
	}

    @Test
    void insertNodes3() throws XMLDBException {
		createDocument("foo", "<top><x><a/><b/></x><y><c/><d/></y></top>");
		assertSeq(new String[]{"<a/>", "<b/>", "<c/>", "<d/>"}, "insert-before(/top/x/*, 3, /top/y/*)");
	}

    // TODO: currently fails because duplicate nodes are removed
    @Test
    void insertNodes4() throws XMLDBException {
		createDocument("foo", "<top><x><a/><b/></x><y><c/><d/></y></top>");
		assertSeq(new String[]{"<a/>", "<a/>", "<b/>", "<b/>"}, "insert-before(/top/x/*, 2, /top/x/*)");
	}

    @Test
    void insertMixed1() throws XMLDBException {
		createDocument("foo", "<top><x><a/><b/></x><y><c/><d/></y></top>");
		assertSeq(new String[]{"<a/>", "c", "<b/>"}, "insert-before(/top/x/*, 2, ('c'))");
	}

    // TODO: currently fails because duplicate nodes are removed
    @Test
    void insertMixed2() throws XMLDBException {
		createDocument("foo", "<top><x><a/><b/></x><y><c/><d/></y></top>");
		assertSeq(new String[]{"<a/>", "<a/>", "<b/>", "<b/>", "c"}, "insert-before((/top/x/*, 'c'), 2, /top/x/*)");
	}

	private void assertSeq(final String[] expected, final String q) throws XMLDBException {
		try (final EXistResourceSet rs = (EXistResourceSet) query.query(q)){
			assertEquals(expected.length, rs.getSize());

			final List<String> a = Arrays.asList(expected);
			final List<Object> r = new ArrayList<>((int) rs.getSize());
			for (int i = 0; i < rs.getSize(); i++) {
				try (final Resource resource = rs.getResource(i)) {
					r.add(resource.getContent());
				}
			}

			if (!a.equals(r)) {
				fail("expected " + a + ", got " + r);
			}
		}
	}
	
	private void createDocument(String name, String content) throws XMLDBException {
		try (final XMLResource res = c.createResource(name, XMLResource.class)) {
			res.setContent(content);
			c.storeResource(res);
		}
	}

	@RegisterExtension
	public static XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    @BeforeAll
    static void setupTestCollection() throws XMLDBException {
		try (final Collection root = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
			final CollectionManagementService rootcms = root.getService(CollectionManagementService.class);
			c = root.getChildCollection("test");
			if (c != null) {
				rootcms.removeCollection("test");
			}
			c = rootcms.createCollection("test");
			assertNotNull(c);
			query = c.getService(XPathQueryService.class);
		}
	}

    @AfterAll
    static void tearDown() throws XMLDBException {
		if (c != null) {
			c.close();
			try (final Collection root = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
				final CollectionManagementService rootcms = root.getService(CollectionManagementService.class);
				rootcms.removeCollection("test");
			}
			query = null;
			c = null;
		}
	}
	
}
