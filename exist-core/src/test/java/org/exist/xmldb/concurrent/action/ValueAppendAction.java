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
package org.exist.xmldb.concurrent.action;

import org.exist.xmldb.EXistResourceSet;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XPathQueryService;
import org.xmldb.api.modules.XUpdateQueryService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author wolf
 */
public class ValueAppendAction extends Action {

    private static final String REMOVE =
        "<xu:modifications xmlns:xu=\"http://www.xmldb.org/xupdate\" version=\"1.0\">"
        + "<xu:remove select=\"//item[last()]\">"
        + "</xu:remove>"
        + "</xu:modifications>";
    
    public ValueAppendAction(final String collectionPath, final String resourceName) {
		super(collectionPath, resourceName);
	}
    
    @Override
    public boolean execute() throws XMLDBException {
        try (final Collection col = DatabaseManager.getCollection(collectionPath, "admin", "")) {
			final XUpdateQueryService service = col.getService(XUpdateQueryService.class);
			final XPathQueryService query = col.getService(XPathQueryService.class);
			append(service);
			query(query);
			remove(service);
		}
        return true;
    }

    private void remove(final XUpdateQueryService service) throws XMLDBException {
		for(int i = 0; i < 10; i++) {
			service.update(REMOVE);
		}
	}
    
    private void append(final XUpdateQueryService service) throws XMLDBException {
		final String updateOpen =
			"<xu:modifications xmlns:xu=\"http://www.xmldb.org/xupdate\" version=\"1.0\">" +
			"<xu:append select=\"/items\" child=\"1\">";
		final String updateClose =
			"</xu:append>" +
			"</xu:modifications>";
		for (int i = 0; i < 10; i++) {
			final String update = updateOpen +
				"<item id=\"" + i + "\"><name>abcdefg</name>" +
				"<value>" + (44.53 + i) + "</value></item>"
				+ updateClose;
			service.update(update);
		}
	}
    
    private void query(final XPathQueryService service) throws XMLDBException {
		try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(resourceName, "/items/item[value = 44.53]")) {
			assertEquals(1, result.getSize());
		}

		try (final EXistResourceSet result = (EXistResourceSet) service.queryResource(resourceName, "/items/item[@id=1]/name[.='abcdefg']/text()")) {
			assertEquals(1, result.getSize());
			try (final Resource resource = result.getResource(0)) {
				assertEquals("abcdefg", resource.getContent().toString());
			}
		}
    }
}
