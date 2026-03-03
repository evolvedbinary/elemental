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
import org.exist.xmldb.concurrent.DBUtils;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

import static org.junit.Assert.assertEquals;

/**
 * Replace an existing resource.
 * 
 * @author wolf
 */
public class ReplaceResourceAction extends Action {

	public static final String XML =
		"<config>" +
		"<user id=\"george\">" +
		"<phone>+49 69 888478</phone>" +
		"<email>george@email.com</email>" +
		"<customer-id>64534233</customer-id>" +
		"<bank-account>7466356</bank-account>" +
		"</user>" +
		"<user id=\"sam\">" +
		"<phone>+49 69 774345</phone>" +
		"<email>sam@email.com</email>" +
		"<customer-id>993834</customer-id>" +
		"<bank-account>364553</bank-account>" +
		"</user>" +
		"</config>";
	
	private final static String TEST_QUERY1 = "//user[@id = 'george']/phone[contains(., '69')]/text()";
	private final static String TEST_QUERY2 = "//user[@id = 'sam']/customer-id[. = '993834']";
	private final static String TEST_QUERY3 = "//user[email = 'sam@email.com']";
	
	private int count = 0;

	public ReplaceResourceAction(final String collectionPath, final String resourceName) {
		super(collectionPath, resourceName);
	}

	@Override
	public boolean execute() throws XMLDBException {
		try (final Collection col = DatabaseManager.getCollection(collectionPath, "admin", "")) {
			final String xml =
				"<data now=\"" + System.currentTimeMillis() + "\" count=\"" +
					++count + "\">" + XML + "</data>";

			DBUtils.addXMLResource(col, resourceName, xml);

			try (final EXistResourceSet result = DBUtils.queryResource(col, resourceName, TEST_QUERY1)) {
				assertEquals(1, result.getSize());
				try (final Resource resource = result.getResource(0)) {
					assertEquals("+49 69 888478", resource.getContent());
				}
			}

			try (final EXistResourceSet result = DBUtils.queryResource(col, resourceName, TEST_QUERY2)) {
				assertEquals(1, result.getSize());
			}

			try (final EXistResourceSet result = DBUtils.queryResource(col, resourceName, TEST_QUERY3)) {
				assertEquals(1, result.getSize());
			}
		}

		return true;
	}
}
