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

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.xmldb.EXistResourceSet;
import org.junit.Assert;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * This is the simplest test that demonstrates the <code>Predicate</code>/<code>OpOr</code>
 * bug. Right now, there is only one test - at the very bottom of the 
 * source code. 
 * @author Jason Smith
 */
public class XPathOpOrSpecialCaseTest extends Assert {

	@RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

	/** Database test collection (<code>/db/blah</code>). */
	private Collection testCollection;

    @BeforeEach
    void setUp() throws XMLDBException {
        final CollectionManagementService service = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
        testCollection = service.createCollection("blah");
        assertNotNull(testCollection);
    }

    @AfterEach
    void tearDown() throws XMLDBException {
		if (testCollection != null) {
			testCollection.close();
			testCollection = null;
		}
		final CollectionManagementService service =
				XMLDB_EMBEDDED_DATABASE.getRoot().getService(
						CollectionManagementService.class);
		service.removeCollection("blah");
	}

    /**
     * Given an essentially empty XML document at path <code>/db/blah/blah.xml</code>,
     * query the document with a bogus predicate containing an <code>or<code> operation;
     * expect <code>org.exist.xquery.XPathException: exerr:ERROR cannot convert xs:boolean('false') to a node set</code>.
     */
    @Test
    void verifyOpOrInPredicate() throws XMLDBException {
        storeXML(testCollection, "blah.xml", "<blah>No element content.</blah>");
       try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("/blah[a='A' or b='B']")) {
			// needed to close the result
	}
    }

	/** 
	 * Store the XML string into the specified collection and document.
	 * @param collection The target collection.
     * @param documentName The target document name.
     * @param content The XML content to be stored.
     * @throws XMLDBException See {@link XMLDBException}.
     */
    private void storeXML(final Collection collection, final String documentName, final String content) throws XMLDBException
    {
        try (final XMLResource doc = collection.createResource(documentName, XMLResource.class)) {
            doc.setContent(content);
            collection.storeResource(doc);
        }
    }
}
