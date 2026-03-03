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

import org.exist.TestUtils;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.Test;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XQueryService;

/**
 * RemoveAndReloadTest.java
 *
 * O2 IT Engineering
 * Zurich,  Switzerland (CH)
 *
 * This test provokes a parameter type error (how?).
 * 
 * @author Tobias Wunden
 * @version 1.0
 */
public class NodeTypeTest {

	@ClassRule
	public static final ExistXmldbEmbeddedServer server = new ExistXmldbEmbeddedServer(false, true, true);

	public static final String DOC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<page partition=\"home\" path=\"/\" version=\"live\">" +
			"    <header>" +
			"        <renderer>home_dreispaltig</renderer>" +
			"        <layout>default</layout>" +
			"        <type>default</type>" +
			"        <publish>" +
			"            <from>2005/06/06 10:53:40 GMT</from>" +
			"            <to>292278994/08/17 07:12:55 GMT</to>" +
			"        </publish>" +
			"        <security>" +
			"            <owner>www</owner>" +
			"            <permission id=\"system:manage\" type=\"role\">system:editor</permission>" +
			"            <permission id=\"system:read\" type=\"role\">system:guest</permission>" +
			"            <permission id=\"system:translate\" type=\"role\">system:translator</permission>" +
			"            <permission id=\"system:publish\" type=\"role\">system:publisher</permission>" +
			"            <permission id=\"system:write\" type=\"role\">system:editor</permission>" +
			"        </security>" +
			"        <keywords/>" +
			"        <title language=\"de\">Home</title>" +
			"        <title language=\"fr\">Home</title>" +
			"        <title language=\"it\">Home</title>" +
			"        <modified>" +
			"            <date>2005/06/06 10:53:40 GMT</date>" +
			"            <user>markus.jauss</user>" +
			"        </modified>" +
			"    </header>" +
			"    <body/>" +
			"</page>";

	/**
	 * This test passes nodes containing xml entities to eXist and tries
	 * to read it back in:
	 * <ul>
	 * <li>Register a database instance</li>
	 * <li>Write a "live" document to the database using the XQueryService</li>
	 * <li>Create a "work" version of it</li>
	 * </ul>
	 */
	@Test
	public final void removeAndReload() throws XMLDBException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		// write "live" document to the database
		store(DOC, "live.xml");
		
		// copy content from work.xml to live.xml using XUpdate
		prepareWorkVersion();
	}

	/**
	 * Stores the given xml fragment into the database.
	 * 
	 * @param xml the xml document
	 * @param document the document name	 
	 */
	private void store(final String xml, final String document) throws XMLDBException {
		final StringBuilder query = new StringBuilder();
		query.append("declare namespace xmldb='http://exist-db.org/xquery/xmldb';");
        query.append("declare variable $document as xs:string external;");
        query.append("declare variable $data as xs:string external;");
		query.append("let $isLoggedIn := xmldb:login('" + XmldbURI.ROOT_COLLECTION_URI + "', '" + TestUtils.ADMIN_DB_USER + "', '" + TestUtils.ADMIN_DB_USER + "'),");
		query.append("$doc := xmldb:store('" + XmldbURI.ROOT_COLLECTION + "', $document, $data)");
		query.append("return <result/>");

		final XQueryService service = server.getRoot().getService(XQueryService.class);
        final CompiledExpression cQuery = service.compile(query.toString());
        service.declareVariable("document", document);
        service.declareVariable("data", xml);
        try (final EXistResourceSet result = (EXistResourceSet) service.execute(cQuery)) {
			// needed to ensure that result is closed
		}
	}

	/**
	 * Updates the given xml fragment in the database using XUpdate.
	 */
	private void prepareWorkVersion() throws XMLDBException {
		final StringBuilder query = new StringBuilder();
		query.append("declare namespace xmldb='http://exist-db.org/xquery/xmldb';\n");
		query.append("declare namespace f='urn:weblounge';\n");
        query.append("declare variable $collection as xs:string external;");

		// Returns a new with a given body and a new header
		query.append("declare function f:create($live as node(), $target as xs:string) as node() { \n");
		query.append("    <page partition='{$live/@partition}' path='{$live/@path}' version='{$target}'> \n");
		query.append("        {$live/*} \n");
		query.append("    </page> \n");
		query.append("}; \n");

		// Function "prepare". Checks if the work version already exists. If this is not the
		// case, it calls the "create" function to have a new page created with the live body
		// but with a "work" or "$target" header.
		query.append("declare function f:prepare($data as node(), $target as xs:string) as xs:string? { \n");
		query.append("    if (empty(xmldb:xcollection($collection)/page[@version eq $target])) then \n");
		query.append("        let $isLoggedIn := xmldb:login($collection, '" + TestUtils.ADMIN_DB_USER + "', '" + TestUtils.ADMIN_DB_PWD + "') \n");
		query.append("        return xmldb:store($collection, concat($target, '.xml'), f:create($data, $target)) \n");
		query.append("    else \n");
		query.append("    () \n");
		query.append("}; \n");
		
		// Main clause, tries to create a work from an existing live version
		query.append("let $live := xmldb:xcollection($collection)/page[@version eq 'live'],\n");
        query.append("     $log := util:log('DEBUG', $live),\n");
		query.append("     $w := f:prepare($live, 'work')\n");
		query.append("    return\n");
		query.append("		              ()\n");

		final XQueryService service = server.getRoot().getService(XQueryService.class);
        final CompiledExpression cQuery = service.compile(query.toString());
        service.declareVariable("collection", XmldbURI.ROOT_COLLECTION);
		try (final EXistResourceSet result = (EXistResourceSet) service.execute(cQuery)) {
			// needed to ensure that result is closed
		}
	}

	/**
	 * Updates the given xml fragment in the database using XUpdate.
	 */
	@SuppressWarnings("unused")
	private void xupdateRemove(final String doc) throws XMLDBException {
		final StringBuilder query = new StringBuilder();
		query.append("declare namespace xmldb='http://exist-db.org/xquery/xmldb';");
		query.append("let $isLoggedIn := xmldb:login('" + XmldbURI.ROOT_COLLECTION_URI + "', '" + TestUtils.ADMIN_DB_USER + "', '" + TestUtils.ADMIN_DB_USER + "'),");
		query.append("$mods := xmldb:remove('" + XmldbURI.ROOT_COLLECTION + "', '" + doc + "')");
		query.append("return <modifications>{$mods}</modifications>");

		final XQueryService service = server.getRoot().getService(XQueryService.class);
        final CompiledExpression cQuery = service.compile(query.toString());
		try (final EXistResourceSet result = (EXistResourceSet) service.execute(cQuery)) {
			// needed to ensure that result is closed
		}
	}
}