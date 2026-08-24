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

import com.googlecode.junittoolbox.ParallelRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.xmldb.EXistResourceSet;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.base.Resource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;
import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
public class DuplicateAttributesTest {

    private static final Logger LOG = LogManager.getLogger(DuplicateAttributesTest.class);

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    private static Collection testCollection;

    private static String STORED_DOC1 = "<node attr='ab'/>";

    private static String STORED_DOC2 = "<node attr2='ab'/>";

    private static String DOC_WITH_DTD =
        "<!DOCTYPE IDS [\n" +
        "<!ELEMENT IDS (elementwithid-1+, elementwithid-2+,\n" +
        "               elementwithidrefattr-1+,elementwithidrefattr-2+)>\n" +
        "<!ELEMENT elementwithid-1 (#PCDATA)>\n" +
        "<!ELEMENT elementwithid-2 (#PCDATA)>\n" +
        "<!ELEMENT elementwithidrefattr-1 (#PCDATA)>\n" +
        "<!ELEMENT elementwithidrefattr-2 (#PCDATA)>\n" +
        "<!ATTLIST elementwithid-1 anId  ID #REQUIRED>\n" +
        "<!ATTLIST elementwithid-2 anId  ID #REQUIRED>\n" +
        "<!ATTLIST elementwithidrefattr-1 anIdRef IDREF #REQUIRED>  \n" +
        "<!ATTLIST elementwithidrefattr-2 anIdRef IDREF #REQUIRED>\n" +
        "]>\n" +
        " <IDS>\n" +
        "  <elementwithid-1 anId = \"id1\"/>\n" +
        "  <elementwithid-2 anId = \"id2\"/>\n" +
        "  <elementwithidrefattr-1 anIdRef = \"id1\"/>\n" +
        "  <elementwithidrefattr-2 anIdRef = \"id2\"/> \n" +
        " </IDS>";

    /**
     * Add attribute to element which already has an attribute of that name.
     */
    @Test
    void appendStoredAttrFail() {
        final XQueryService xqs = testCollection.getService(XQueryService.class);
        String query =
            "let $a := \n" +
            "<node attr=\"a\" b=\"c\">{doc(\"/db/test/stored1.xml\")//@attr}</node>" +
            "return $a";
        assertThrows(XMLDBException.class, () ->
		try (final EXistResourceSet result = (EXistResourceSet) xqs.query(query)) {
		    // needed to ensure that result is closed
		}
	});
    }

    /**
     * Add attribute to element which has no conflicting attributes.
     */
    @Test
    public void appendStoredAttrOK() throws XMLDBException {
        final XQueryService xqs = testCollection.getService(XQueryService.class);
        final String query =
            "let $a := \n" +
            "<node attr=\"a\" b=\"c\">{doc(\"/db/test/stored2.xml\")//@attr2}</node>" +
            "return $a";
        try (final EXistResourceSet result = (EXistResourceSet) xqs.query(query)) {
            assertEquals(1, result.getSize());
            try (final Resource resource = result.getResource(0)) {
                assertEquals("<node attr=\"a\" b=\"c\" attr2=\"ab\"/>", resource.getContent());
            }
        }
    }

    /**
     * Add constructed in-memory attribute to element which already has an
     * attribute of that name.
     */
    @Test
    void appendConstrAttr() {
        final XQueryService xqs = testCollection.getService(XQueryService.class);
        final String query =
            "let $a := <root attr=\"ab\"/>" +
            "let $b := \n" +
            "   <node attr=\"a\" b=\"c\">{$a//@attr}</node>" +
            "return $a";
        assertThrows(XMLDBException.class, () ->
		try (final EXistResourceSet result = (EXistResourceSet) xqs.query(query)) {
		    // needed to ensure that result is cloded
		}
	});
    }

    /**
     * Add attribute to element which already has an
     * attribute of that name (using idref).
     */
    @Test
    void appendIdref() {
        final XQueryService xqs = testCollection.getService(XQueryService.class);
        final String query =
            "<results>{fn:idref(('id1', 'id2'), doc('/db/test/docdtd.xml')/IDS)}</results>";
        try (final EXistResourceSet result = (EXistResourceSet) xqs.query(query)) {
		     assertThrows(XMLDBException.class, () ->
			    try (final Resource resource = result.getResource(0)) {
				resource.getContent();
			    }
			});
        }
    }

    @BeforeAll
    static void setup() throws XMLDBException {
        final CollectionManagementService service = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
        testCollection = service.createCollection("test");
        assertNotNull(testCollection);

        try (final Resource resource = testCollection.createResource("stored1.xml", XMLResource.class)) {
            resource.setContent(STORED_DOC1);
            testCollection.storeResource(resource);
        }

        try (final Resource resource = testCollection.createResource("stored2.xml", XMLResource.class)) {
            resource.setContent(STORED_DOC2);
            testCollection.storeResource(resource);
        }

        try (final Resource resource = testCollection.createResource("docdtd.xml", XMLResource.class)) {
            resource.setContent(DOC_WITH_DTD);
            testCollection.storeResource(resource);
        }
    }

    @AfterAll
    static void cleanup() throws XMLDBException {
        testCollection.close();
        final CollectionManagementService service = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
        service.removeCollection("test");
    }
}
