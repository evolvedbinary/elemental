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

import org.exist.EXistException;
import org.exist.TestUtils;
import org.exist.collections.triggers.TriggerException;
import org.exist.security.PermissionDeniedException;
import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.exist.util.LockException;
import org.exist.xmldb.EXistResourceSet;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XPathQueryService;

import java.io.IOException;

public class AnnotationsTest {

    @RegisterExtension
    public final static XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    @BeforeAll
    static void setUp() throws XMLDBException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        CollectionManagementService service = XMLDB_EMBEDDED_DATABASE.getRoot().getService(CollectionManagementService.class);
        try (final Collection testCollection = service.createCollection("test")) {
            assertNotNull(testCollection);
        }
    }

    @AfterAll
    static void tearDown() throws LockException, TriggerException, PermissionDeniedException, EXistException, IOException {
        // testCollection.removeResource( testCollection .getResource(file_name));
        TestUtils.cleanupDB();
    }

    @Test
    void annotation() throws XMLDBException {
        final String TEST_VALUE_CONSTANT = "hello world";
        
        final String query = 
                "declare namespace hello = 'http://world.com';\n"
                + "declare\n"
                + "%hello:world\n"
                + "function local:hello() {\n"
                +   "'" + TEST_VALUE_CONSTANT + "'\n"
                + "};\n"
                + "local:hello()";
            
        final XPathQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());

            try (final Resource res = result.getIterator().nextResource()) {
                assertEquals(TEST_VALUE_CONSTANT, res.getContent());
            }
        }
    }

    @Test
    void annotationWithLiterals() throws XMLDBException {
        final String TEST_VALUE_CONSTANT = "hello world";
        
        final String query = 
                "declare namespace hello = 'http://world.com';\n"
                + "declare\n"
                + "%hello:world('a=b', 'b=c')\n"
                + "function local:hello() {\n"
                +   "'" + TEST_VALUE_CONSTANT + "'\n"
                + "};\n"
                + "local:hello()";
            
        final XPathQueryService service = getQueryService();
        try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
            assertEquals(1, result.getSize());

            try (final Resource res = result.getIterator().nextResource()) {
                assertEquals(TEST_VALUE_CONSTANT, res.getContent());
            }
        }
    }
    
    @Test
    public void annotationInXMLNamespaceFails() throws XMLDBException {
        final String TEST_VALUE_CONSTANT = "hello world";
        
        final String query = 
                "declare namespace hello = 'http://www.w3.org/XML/1998/namespace';\n"
                + "declare\n"
                + "%hello:world\n"
                + "function local:hello() {\n"
                +   "'" + TEST_VALUE_CONSTANT + "'\n"
                + "};\n"
                + "local:hello()";
            
        final XPathQueryService service = getQueryService();
        assertThrows(XMLDBException.class, () ->
		try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
		    // needed to ensure that result is closed
		}
	});
    }
    
    @Test
    void annotationInXMLSchemaNamespaceFails() throws XMLDBException {
        final String TEST_VALUE_CONSTANT = "hello world";
        
        final String query = 
                "declare namespace hello = 'http://www.w3.org/2001/XMLSchema';\n"
                + "declare\n"
                + "%hello:world\n"
                + "function local:hello() {\n"
                +   "'" + TEST_VALUE_CONSTANT + "'\n"
                + "};\n"
                + "local:hello()";
            
        final XPathQueryService service = getQueryService();
        assertThrows(XMLDBException.class, () ->
		try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
		    // needed to ensure that result is closed
		}
	};
    }
    
    @Test
    void annotationInXMLSchemaInstanceNamespaceFails() throws XMLDBException {
        final String TEST_VALUE_CONSTANT = "hello world";
        
        final String query = 
                "declare namespace hello = 'http://www.w3.org/2001/XMLSchema-instance';\n"
                + "declare\n"
                + "%hello:world\n"
                + "function local:hello() {\n"
                +   "'" + TEST_VALUE_CONSTANT + "'\n"
                + "};\n"
                + "local:hello()";
            
        final XPathQueryService service = getQueryService();
        assertThrows(XMLDBException.class, () ->
		try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
		    // needed to ensure that result is closed
		}
	})
    }
    
    @Test
    void annotationInXPathFunctionsNamespaceFails() throws XMLDBException {
        final String TEST_VALUE_CONSTANT = "hello world";
        
        final String query = 
                "declare namespace hello = 'http://www.w3.org/2005/xpath-functions';\n"
                + "declare\n"
                + "%hello:world\n"
                + "function local:hello() {\n"
                +   "'" + TEST_VALUE_CONSTANT + "'\n"
                + "};\n"
                + "local:hello()";
            
        final XPathQueryService service = getQueryService();
        assertThrows(XMLDBException.class, () ->
		try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
		    // needed to ensure that result is closed
		}
	});
    }
    
    @Test
    void annotationInXPathFunctionsMathNamespaceFails() throws XMLDBException {
        final String TEST_VALUE_CONSTANT = "hello world";
        
        final String query = 
                "declare namespace hello = 'http://www.w3.org/2005/xpath-functions/math';\n"
                + "declare\n"
                + "%hello:world\n"
                + "function local:hello() {\n"
                +   "'" + TEST_VALUE_CONSTANT + "'\n"
                + "};\n"
                + "local:hello()";
            
        final XPathQueryService service = getQueryService();
        assertThrows(XMLDBException.class, () ->
		try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
		    // needed to ensure that result is closed
		}
	});
    }
    
    @Test
    void annotationInXQueryOptionsNamespaceFails() throws XMLDBException {
        final String TEST_VALUE_CONSTANT = "hello world";
        
        final String query = 
                "declare namespace hello = 'http://www.w3.org/2011/xquery-options';\n"
                + "declare\n"
                + "%hello:world\n"
                + "function local:hello() {\n"
                +   "'" + TEST_VALUE_CONSTANT + "'\n"
                + "};\n"
                + "local:hello()";
            
        final XPathQueryService service = getQueryService();
        assertThrows(XMLDBException.class, () ->
		try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
		    // needed to ensure that result is closed
		}
	});
    }
   
    private XPathQueryService getQueryService() throws XMLDBException {
        try (final Collection testCollection = DatabaseManager.getCollection("xmldb:exist:///db/test", "admin", "")) {
            final XPathQueryService service = testCollection.getService(XPathQueryService.class);
            return service;
        }
    }
}
