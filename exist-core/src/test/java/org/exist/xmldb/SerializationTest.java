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

import org.exist.Namespaces;
import org.exist.TestUtils;
import org.exist.storage.serializers.EXistOutputKeys;
import org.exist.test.DatabaseWebServerExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XQueryService;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationTest {

	@RegisterExtension
    public static final DatabaseWebServerExtension DATABASE_WEB_SERVER = new DatabaseWebServerExtension(true, false, true, true);
	private static final String PORT_PLACEHOLDER = "${PORT}";

	private static final String EOL = System.getProperty("line.separator");

	private static final String TEST_COLLECTION_NAME = "xmlrpc-serialization-test";

	private static final String XML_DOC_NAME = "defaultns.xml";
	private static final String XML =
		"<root xmlns=\"http://foo.com\">" +
		"	<entry>1</entry>" +
		"	<entry>2</entry>" +
		"</root>";

	private static final String XML_EXPECTED1 =
		"<exist:result xmlns:exist=\"" + Namespaces.EXIST_NS + "\" hitCount=\"2\">" + EOL +
		"    <entry xmlns=\"http://foo.com\">1</entry>" + EOL +
		"    <entry xmlns=\"http://foo.com\">2</entry>" + EOL +
		"</exist:result>";

	private static final String XML_EXPECTED2 =
		"<exist:result xmlns:exist=\"" + Namespaces.EXIST_NS + "\" hitCount=\"1\">" + EOL +
		"    <Site xmlns=\"urn:content\">" + EOL +
        "        <config xmlns=\"urn:config\">123</config>" + EOL +
        "        <serverconfig xmlns=\"urn:config\">123</serverconfig>" + EOL +
		"    </Site>" + EOL +
		"</exist:result>";

	private static final String XML_UPDATED_EXPECTED =
		"<root xmlns=\"http://foo.com\">" + EOL +
		"	<entry>1</entry>" + EOL +
		"	<entry>2</entry>" + EOL +
		"	<entry xmlns=\"\" xml:id=\"aargh\"/>" + EOL +
		"</root>";

	private static final XmldbURI TEST_XML_DOC_WITH_DOCTYPE_URI = XmldbURI.create("test-with-doctype.xml");

	private static final String XML_WITH_DOCTYPE =
			"<!DOCTYPE bookmap PUBLIC \"-//OASIS//DTD DITA BookMap//EN\" \"bookmap.dtd\">\n" +
			"<bookmap id=\"bookmap-1\"/>";

	private static final XmldbURI TEST_XML_DOC_WITH_XMLDECL_URI = XmldbURI.create("test-with-xmldecl.xml");

	private static final String XML_WITH_XMLDECL =
			"<?xml version=\"1.1\" encoding=\"ISO-8859-1\" standalone=\"yes\"?>\n" +
			"<bookmap id=\"bookmap-2\"/>";

	public static java.util.Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "local", "xmldb:exist://" },
				{ "remote", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc" }
		});
	}
	public String apiName;
	public String baseUri;

	private Collection testCollection;

	private final String getBaseUri() {
		return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(DATABASE_WEB_SERVER.getPort()));
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void wrappedNsTest1(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		final XQueryService service = testCollection.getService(XQueryService.class);
		try (final EXistResourceSet result = (EXistResourceSet) service.query("declare namespace foo=\"http://foo.com\"; //foo:entry")) {
			assertEquals(2, result.getSize());

			try (final Resource resource = result.getMembersAsResource()) {
				assertXMLEquals(XML_EXPECTED1, resource);
			}
		}
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void wrappedNsTest2(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		final XQueryService service = testCollection.getService(XQueryService.class);
		try (final EXistResourceSet result = (EXistResourceSet) service.query(
				"declare variable $config := <config xmlns='urn:config'>123</config>; " +
				"declare variable $serverConfig := <serverconfig xmlns='urn:config'>123</serverconfig>; " +
				"<c:Site xmlns='urn:content' xmlns:c='urn:content'> " +
				"{($config,$serverConfig)} " +
				"</c:Site>")) {
			assertEquals(1, result.getSize());

			try (final Resource resource = result.getMembersAsResource()) {
				assertXMLEquals(XML_EXPECTED2, resource);
			}
		}
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void xqueryUpdateNsTest(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		final XQueryService service = testCollection.getService(XQueryService.class);
		try (final EXistResourceSet result = (EXistResourceSet) service.query(
				"xquery version \"1.0\";" + EOL +
				"declare namespace foo=\"http://foo.com\";" + EOL +
				"let $in-memory :=" + EOL + XML + EOL +
				"let $on-disk := doc('/db/" + TEST_COLLECTION_NAME + '/' + XML_DOC_NAME + "')" + EOL +
				"let $new-node := <entry xml:id='aargh'/>" + EOL +
				"let $update := update insert $new-node into $on-disk/foo:root" + EOL +
				"return" + EOL +
				"    (" + EOL +
				"        $in-memory," + EOL +
				"        $on-disk" + EOL +
				"    )" + EOL
		)) {

			assertEquals(2, result.getSize());

			try (final Resource inMemoryResource = result.getResource(0)) {
				assertXMLEquals(XML, inMemoryResource);
			}

			try (final Resource onDiskResource = result.getResource(1)) {
				assertXMLEquals(XML_UPDATED_EXPECTED, onDiskResource);
			}
		}
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void getDocTypeDefault(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		try (final Resource res = testCollection.getResource(TEST_XML_DOC_WITH_DOCTYPE_URI.lastSegmentString())) {
			assertEquals(XML_WITH_DOCTYPE, res.getContent());
		}
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void getDocTypeNo(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		final String prevOutputDocType = testCollection.getProperty(EXistOutputKeys.OUTPUT_DOCTYPE);
		try (final Resource res = testCollection.getResource(TEST_XML_DOC_WITH_DOCTYPE_URI.lastSegmentString())) {
			testCollection.setProperty(EXistOutputKeys.OUTPUT_DOCTYPE, "no");
			assertEquals("<bookmap id=\"bookmap-1\"/>", res.getContent());
		} finally {
			if (prevOutputDocType != null) {
				testCollection.setProperty(EXistOutputKeys.OUTPUT_DOCTYPE, prevOutputDocType);
			}
		}
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void getDocTypeYes(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		final String prevOutputDocType = testCollection.getProperty(EXistOutputKeys.OUTPUT_DOCTYPE);
		try (final Resource res = testCollection.getResource(TEST_XML_DOC_WITH_DOCTYPE_URI.lastSegmentString())) {
			testCollection.setProperty(EXistOutputKeys.OUTPUT_DOCTYPE, "yes");
			assertEquals(XML_WITH_DOCTYPE, res.getContent());
		} finally {
			if (prevOutputDocType != null) {
				testCollection.setProperty(EXistOutputKeys.OUTPUT_DOCTYPE, prevOutputDocType);
			}
		}
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void getXmlDeclDefault(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		try (final Resource res = testCollection.getResource(TEST_XML_DOC_WITH_XMLDECL_URI.lastSegmentString())) {
			assertEquals(XML_WITH_XMLDECL, res.getContent());
		}
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void getXmlDeclNo(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		final String prevOmitOriginalXmlDecl = testCollection.getProperty(EXistOutputKeys.OMIT_ORIGINAL_XML_DECLARATION);
		try (final Resource res = testCollection.getResource(TEST_XML_DOC_WITH_XMLDECL_URI.lastSegmentString())) {
			testCollection.setProperty(EXistOutputKeys.OMIT_ORIGINAL_XML_DECLARATION, "no");
			assertEquals(XML_WITH_XMLDECL, res.getContent());
		} finally {
			if (prevOmitOriginalXmlDecl != null) {
				testCollection.setProperty(EXistOutputKeys.OMIT_ORIGINAL_XML_DECLARATION, prevOmitOriginalXmlDecl);
			}
		}
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void getXmlDeclYes(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		final String prevOmitOriginalXmlDecl = testCollection.getProperty(EXistOutputKeys.OMIT_ORIGINAL_XML_DECLARATION);
		try (final Resource res = testCollection.getResource(TEST_XML_DOC_WITH_XMLDECL_URI.lastSegmentString())) {
			testCollection.setProperty(EXistOutputKeys.OMIT_ORIGINAL_XML_DECLARATION, "yes");
			assertEquals("<bookmap id=\"bookmap-2\"/>", res.getContent());
		} finally {
			if (prevOmitOriginalXmlDecl != null) {
				testCollection.setProperty(EXistOutputKeys.OMIT_ORIGINAL_XML_DECLARATION, prevOmitOriginalXmlDecl);
			}
		}
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testArray(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		final String query = "array { \"value 1\", \"value 2\" }";

		final XQueryService service = testCollection.getService(XQueryService.class);
		try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
			assertEquals(1, result.getSize());

			try (final Resource resource = result.getResource(0)) {
				assertEquals("[ \"value 1\", \"value 2\" ]", resource.getContent());
			}
		}
	}

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testMap(String apiName, String baseUri) throws XMLDBException {
        initSerializationTest(apiName, baseUri);
		final String query = "map { \"prop1\" : \"value 1\", \"prop2\" : \"value 2\" }";

		final XQueryService service = testCollection.getService(XQueryService.class);
		try (final EXistResourceSet result = (EXistResourceSet) service.query(query)) {
			assertEquals(1, result.getSize());

			try (final Resource resource = result.getResource(0)) {
				assertEquals("map {\"prop2\": \"value 2\", \"prop1\": \"value 1\"}", resource.getContent());
			}
		}
	}

	private static void assertXMLEquals(final String expected, final Resource actual) throws XMLDBException {
		final Source srcExpected = Input.fromString(expected).build();
		final Source srcActual = Input.fromString(actual.getContent().toString()).build();
		final Diff diff = DiffBuilder.compare(srcExpected)
				.withTest(srcActual)
				.checkForIdentical()
				.ignoreWhitespace()
				.build();
		assertFalse(diff.hasDifferences(), diff.toString());
	}

    @BeforeEach
    void setUp() throws XMLDBException {
		try (final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
			final CollectionManagementService service = root.getService(CollectionManagementService.class);
			testCollection = service.createCollection(TEST_COLLECTION_NAME);
			assertNotNull(testCollection);

			try (final XMLResource res = testCollection.createResource(XML_DOC_NAME, XMLResource.class)) {
				res.setContent(XML);
				testCollection.storeResource(res);
			}

			try (final XMLResource res1 = testCollection.createResource(TEST_XML_DOC_WITH_DOCTYPE_URI.lastSegmentString(), XMLResource.class)) {
				res1.setContent(XML_WITH_DOCTYPE);
				testCollection.storeResource(res1);
			}

			try (final XMLResource res2 = testCollection.createResource(TEST_XML_DOC_WITH_XMLDECL_URI.lastSegmentString(), XMLResource.class)) {
				res2.setContent(XML_WITH_XMLDECL);
				testCollection.storeResource(res2);
			}
		}
    }

    @AfterEach
    void tearDown() throws XMLDBException {
		testCollection.close();

		try (final Collection root = DatabaseManager.getCollection(getBaseUri() + "/db", TestUtils.ADMIN_DB_USER, TestUtils.ADMIN_DB_PWD)) {
			final CollectionManagementService service = root.getService(CollectionManagementService.class);
			service.removeCollection(TEST_COLLECTION_NAME);
		}

        testCollection = null;
    }

    public void initSerializationTest(String apiName, String baseUri) {
        this.apiName = apiName;
        this.baseUri = baseUri;
    }
}
