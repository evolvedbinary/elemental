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
package org.exist.xmldb;

import org.exist.test.DatabaseWebServerExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Assertions;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** An abstract wrapper for remote DB tests
 * @author <a href="mailto:pierrick.brihaye@free.fr">Sebastian Bossung, Technische Universitaet Hamburg-Harburg
 * @author Pierrick Brihaye</a>
 */
//TODO : manage content from here, not from the derived classes
public abstract class RemoteDBTest {

    @RegisterExtension
    public static final DatabaseWebServerExtension DATABASE_WEB_SERVER = new DatabaseWebServerExtension(true, false, true, true);

    private final static String CHILD_COLLECTION = "unit-testing-collection-Citt\u00E0";
    public final static String DB_DRIVER = "org.exist.xmldb.DatabaseImpl";

    private RemoteCollection collection = null;

    public static String getUri() {
        return "xmldb:exist://localhost:" + DATABASE_WEB_SERVER.getPort() + "/xmlrpc";
    }

    protected void setUpRemoteDatabase() throws ClassNotFoundException, IllegalAccessException, InstantiationException, XMLDBException {
        //Connect to the DB
        final Class<?> cl = Class.forName(DB_DRIVER);
        final Database database = (Database) cl.newInstance();
        assertNotNull(database);
        DatabaseManager.registerDatabase(database);
        //Get the root collection...
        try (final Collection rootCollection = DatabaseManager.getCollection(getUri() + XmldbURI.ROOT_COLLECTION, "admin", "")) {
            assertNotNull(rootCollection);
            final CollectionManagementService cms = rootCollection.getService(CollectionManagementService.class);
            //Creates the child collection
            //... and work from it
            this.collection = (RemoteCollection) cms.createCollection(CHILD_COLLECTION);
            assertNotNull(this.collection);
        }
    }

    protected void removeCollection() {
        Assertions.assertDoesNotThrow(() -> {
   		collection.close();
	    	try (final Collection rootCollection = DatabaseManager.getCollection(getUri() + XmldbURI.ROOT_COLLECTION, "admin", "")) {
		    assertNotNull(rootCollection);
		    final CollectionManagementService cms = rootCollection.getService(CollectionManagementService.class);
		    cms.removeCollection(CHILD_COLLECTION);
		}
	});
    }

    protected RemoteCollection getCollection() {
        return collection;
    }

    protected String getTestCollectionName() {
        return CHILD_COLLECTION;
    }
}
