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
import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.test.TestConstants;
import org.exist.xmldb.EXistResourceSet;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
public class FunSubSequenceTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    private static Collection test = null;
    private static final String SIMPLE_XML_FILENAME = "simple.xml";
    private static final String SIMPLE_XML = "<nums><i>1</i><i>2</i><i>3</i><i>4</i></nums>";

    @Test
    void all_arity2() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 1)")) {
            assertEquals("(1,2,3,4,5)", asSequenceStr(result));
        }
    }

    @Test
    void all_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 1, 5)")) {
            assertEquals("(1,2,3,4,5)", asSequenceStr(result));
        }
    }

    @Test
    void firstItem_arity2() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1), 1)")) {
            assertEquals("(1)", asSequenceStr(result));
        }
    }

    @Test
    void firstItem_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 1, 1)")) {
            assertEquals("(1)", asSequenceStr(result));
        }
    }

    @Test
    void midItem_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 2, 1)")) {
            assertEquals("(2)", asSequenceStr(result));
        }
    }

    @Test
    void midItems_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 3, 2)")) {
            assertEquals("(3,4)", asSequenceStr(result));
        }
    }

    @Test
    void lastItem_arity2() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 5)")) {
            assertEquals("(5)", asSequenceStr(result));
        }
    }

    @Test
    void lastItem_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 5, 1)")) {
            assertEquals("(5)", asSequenceStr(result));
        }
    }

    @Test
    void allButFirst_arity2() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 2)")) {
            assertEquals("(2,3,4,5)", asSequenceStr(result));
        }
    }

    @Test
    void allButFirst_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 2, 4)")) {
            assertEquals("(2,3,4,5)", asSequenceStr(result));
        }
    }

    @Test
    void allButLast_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 1, 4)")) {
            assertEquals("(1,2,3,4)", asSequenceStr(result));
        }
    }

    @Test
    void outOfRange_arity2() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 7)")) {
            assertEquals("()", asSequenceStr(result));
        }
    }

    @Test
    void outOfRange_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 7, 4)")) {
            assertEquals("()", asSequenceStr(result));
        }
    }

    @Test
    void zeroStartingLoc_arity2() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 0)")) {
            assertEquals("(1,2,3,4,5)", asSequenceStr(result));
        }
    }

    @Test
    void zeroStartingLocToMid_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 0, 3)")) {
            assertEquals("(1,2)", asSequenceStr(result));
        }
    }

    @Test
    void zeroStartingLocToEnd_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), 0, 5)")) {
            assertEquals("(1,2,3,4)", asSequenceStr(result));
        }
    }

    @Test
    void negativeStartingLoc_arity2() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), -2)")) {
            assertEquals("(1,2,3,4,5)", asSequenceStr(result));
        }
    }

    @Test
    void negativeStartingLoc_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 5), -2, 5)")) {
            assertEquals("(1,2)", asSequenceStr(result));
        }
    }

    @Test
    void smallPartOfLargeRange_arity2() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 3000000000), 2999999995)")) {
            assertEquals("(2999999995,2999999996,2999999997,2999999998,2999999999,3000000000)", asSequenceStr(result));
        }
    }

    @Test
    void smallPartOfLargeRange_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence((1 to 3000000000), 2999999995, 5)")) {
            assertEquals("(2999999995,2999999996,2999999997,2999999998,2999999999)", asSequenceStr(result));
        }
    }

    @Test
    void largeRange_arity2() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:count(fn:subsequence((1 to 3000000000), -2147483649))")) {
            assertEquals("(3000000000)", asSequenceStr(result));
        }
    }

    @Test
    void largeRange_arity3() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:count(fn:subsequence((1 to 3000000000), 1, 3000000000))")) {
            assertEquals("(3000000000)", asSequenceStr(result));
        }
    }

    @Test
    void persistentSupsequence_toInMemory() throws XMLDBException {
        try (final EXistResourceSet result = XMLDB_EMBEDDED_DATABASE.executeQuery("fn:subsequence(doc('" + TestConstants.TEST_COLLECTION_URI.getCollectionPath() + "/" + SIMPLE_XML_FILENAME + "')/nums/i, 2, 2)//text()")) {
            assertEquals("(2,3)", asSequenceStr(result));
        }
    }

    @BeforeAll
    static void setup() throws XMLDBException {
        test = XMLDB_EMBEDDED_DATABASE.createCollection(XMLDB_EMBEDDED_DATABASE.getRoot(), TestConstants.TEST_COLLECTION_URI.lastSegment().toString());
        try (final Resource resource = test.createResource(SIMPLE_XML_FILENAME, XMLResource.class)) {
            resource.setContent(SIMPLE_XML);
            test.storeResource(resource);
        }
    }

    @AfterAll
    static void cleanup() throws XMLDBException {
        test.close();
        final CollectionManagementService collectionManagementService = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
        collectionManagementService.removeCollection(test.getName());
    }

    private static String asSequenceStr(final EXistResourceSet result) throws XMLDBException {
        final StringBuilder builder = new StringBuilder();
        builder.append('(');
        for (int i = 0; i < result.getSize(); i++) {
            try (final Resource resource = result.getResource(i)) {
                builder.append(resource.getContent().toString());
            }
            if (i + 1 < result.getSize()) {
                builder.append(',');
            }
        }
        builder.append(')');
        return builder.toString();
    }
}
