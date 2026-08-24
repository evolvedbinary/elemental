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

import org.apache.commons.io.input.UnsynchronizedByteArrayInputStream;
import org.exist.xquery.util.URIUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.Service;
import org.xmldb.api.base.ServiceProviderCache;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XPathQueryService;
import org.xmldb.api.modules.XQueryService;
import org.xmldb.api.modules.XUpdateQueryService;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import xyz.elemental.mediatype.MediaType;

import javax.xml.transform.Source;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xmldb.api.base.ResourceType.BINARY_RESOURCE;
import static org.xmldb.api.base.ResourceType.XML_RESOURCE;

/** A test case for accessing collections remotely
 * @author <a href="mailto:pierrick.brihaye@free.fr">jmv
 * @author Pierrick Brihaye</a>
 */
class RemoteCollectionTest extends RemoteDBTest {

    private final static String XML_CONTENT = "<xml/>";
    private final static String BINARY_CONTENT = "TEXT";

    @BeforeEach
    void setUp() throws ClassNotFoundException, InstantiationException, XMLDBException, IllegalAccessException {
        setUpRemoteDatabase();
    }

    @AfterEach
    void tearDown() {
        removeCollection();
    }

    @Test
    void getServices() throws XMLDBException {
        final List<Class<? extends Service>> expectedServiceTypes = Arrays.asList(CollectionManagementService.class,
                DatabaseInstanceManager.class, EXistCollectionManagementService.class, EXistRestoreService.class,
                EXistUserManagementService.class, IndexQueryService.class, UserManagementService.class,
                XPathQueryService.class, XQueryService.class, XUpdateQueryService.class,
                RemoteXPathQueryService.class, RemoteCollectionManagementService.class, RemoteUserManagementService.class,
                RemoteDatabaseInstanceManager.class, RemoteIndexQueryService.class, RemoteXUpdateQueryService.class);
        final RemoteCollection colTest = getCollection();
        for (final Class<? extends Service> expectedServiceType : expectedServiceTypes) {
            assertTrue(colTest.hasService(expectedServiceType));
            assertNotNull(colTest.getService(expectedServiceType));
        }
    }

    @Test
    void isRemoteCollection() throws XMLDBException {
        final RemoteCollection collection = getCollection();
        assertTrue(collection.isRemoteCollection());
    }

    @Test
    void getPath() throws XMLDBException {
        assertEquals(XmldbURI.ROOT_COLLECTION + "/" + getTestCollectionName(), URIUtils.urlDecodeUtf8(getCollection().getPath()));
    }

    @Test
    void createXmlResourceFromString() throws XMLDBException {
        final Collection collection = getCollection();

        final String resourceName = "testresource.xml";
        final String xml = "<?xml version='1.0'?><xml>" + System.currentTimeMillis() + "</xml>";

        try (final Resource resource = collection.createResource(resourceName, XMLResource.class)) {
            assertNotNull(resource);
            assertEquals(collection, resource.getParentCollection());
            resource.setContent(xml);
            collection.storeResource(resource);
        }

        try (final Resource retrievedResource = collection.getResource(resourceName)) {
            assertNotNull(retrievedResource);
            assertEquals(XML_RESOURCE, retrievedResource.getResourceType());
            assertInstanceOf(XMLResource.class, retrievedResource);
            final String result = (String) retrievedResource.getContent();
            assertNotNull(result);

            final Source expected = Input.fromString(xml).build();
            final Source actual = Input.fromString(result).build();

            final Diff diff = DiffBuilder.compare(expected)
                .withTest(actual)
                .checkForSimilar()
                .build();

            assertFalse(diff.hasDifferences, diff.toString());
        }
    }

    @Test
    void createBinaryResourceFromString() throws XMLDBException {
        final Collection collection = getCollection();

        final String resourceName = "testresource.bin";
        final String bin = "binary data: " + System.currentTimeMillis();

        try (final Resource resource = collection.createResource(resourceName, BinaryResource.class)) {
            assertNotNull(resource);
            assertEquals(collection, resource.getParentCollection());
            resource.setContent(bin);
            collection.storeResource(resource);
        }

        try (final Resource retrievedResource = collection.getResource(resourceName)) {
            assertNotNull(retrievedResource);
            assertEquals(BINARY_RESOURCE, retrievedResource.getResourceType());
            assertInstanceOf(BinaryResource.class, retrievedResource);
            final byte[] result = (byte[]) retrievedResource.getContent();
            assertNotNull(result);
            assertEquals(bin, new String(result, UTF_8));
        }
	}

    @Test
    void createEmptyBinaryResource() throws XMLDBException, IOException {
        final Collection collection = getCollection();

        final String resourceName = "empty.dtd";
        final byte[] bin = new byte[0];

        try (final Resource resource = collection.createResource(resourceName, BinaryResource.class)) {
            ((EXistResource) resource).setMediaType(MediaType.APPLICATION_XML_DTD);

            try (final InputStream is = new UnsynchronizedByteArrayInputStream(bin)) {
                final InputSource inputSource = new InputSource();
                inputSource.setByteStream(is);
                inputSource.setSystemId("empty.dtd");

                resource.setContent(inputSource);
                collection.storeResource(resource);
            }
        }

        try (final Resource retrievedResource = collection.getResource(resourceName)) {
            assertNotNull(retrievedResource);
            assertEquals(BINARY_RESOURCE, retrievedResource.getResourceType());
            assertInstanceOf(BinaryResource.class, retrievedResource);
            final byte[] result = (byte[]) retrievedResource.getContent();
            assertNotNull(result);
            assertArrayEquals(bin, result);
        }
    }


    /* issue 1874 */
    @Test
    void createXMLFileResource() throws XMLDBException, IOException {
        final Collection collection = getCollection();
        try (final Resource resource = collection.createResource("testresource", XMLResource.class)) {
            assertNotNull(resource);
            assertEquals(collection, resource.getParentCollection());

            final String sometxt = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
            final Path path = Files.createTempFile("test-createXMLFileResource", ".xml");
            final StringBuilder sb = new StringBuilder();
            sb.append("<?xml version='1.0'?><xml>");
            for (int i = 0; i < 5000; i++) {
                sb.append("<element>").append(sometxt).append("</element>");
            }
            sb.append("</xml>");
            Files.copy(new UnsynchronizedByteArrayInputStream(sb.toString().getBytes()), path, StandardCopyOption.REPLACE_EXISTING);
            resource.setContent(path);
            collection.storeResource(resource);
        }
    }

    @Test
    void getNonExistentResource() throws XMLDBException {
        final Collection collection = getCollection();
        try (final Resource resource = collection.getResource("unknown.xml")) {
            assertNull(resource);
        }
	}

    @Test
    void listResources() throws XMLDBException {
        final List<String> xmlNames = new ArrayList<>();
        xmlNames.add("xml1");
        xmlNames.add("xml2");
        xmlNames.add("xml3");
        createResources(xmlNames, XMLResource.class);

        final List<String> binaryNames = new ArrayList<>();
        binaryNames.add("b1");
        binaryNames.add("b2");
        createResources(binaryNames, BinaryResource.class);

        for (final String resource : getCollection().listResources()) {
            xmlNames.remove(resource);
            binaryNames.remove(resource);
        }
        assertEquals(0, xmlNames.size());
        assertEquals(0, binaryNames.size());
	}

    /**
     * Trying to access a collection where the parent collection does
     * not exist caused NullPointerException on DatabaseManager.getCollection() method.
     */
    @Test
    void parent() throws XMLDBException {
        final String parentName;
        try (final Collection c = DatabaseManager.getCollection(getUri() + XmldbURI.ROOT_COLLECTION, "admin", "")) {
            assertNull(c.getChildCollection("b"));
            parentName = c.getName() + "/" + System.currentTimeMillis();
        }


        final String colName = parentName + "/a";
        try (final Collection c = DatabaseManager.getCollection(getUri() + parentName, "admin", "")) {
            assertNull(c);
        }

        // following fails for XmlDb 20051203
        try (final Collection c = DatabaseManager.getCollection(getUri() + colName, "admin", "")) {
            assertNull(c);
        }
	}

	@Test  /* issue 2743 */
    void getLoadRemoteResourceContentBiggerThan16MB() throws XMLDBException, SAXException, IOException {
        Collection collection = getCollection();
        try (final RemoteXMLResource resource = (RemoteXMLResource)collection.createResource("testresource", XMLResource.class)) {
            prepareContent(resource);
            collection.storeResource(resource);
            // load stored content
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                resource.getContentIntoAStream(outputStream);
                // compare size
                assertEquals(16777229, outputStream.size());
            }
        }
    }

    @Test
    void isOpen() throws XMLDBException {
        Collection collection = getCollection();
        assertTrue(collection.isOpen());
        collection.close();
        assertFalse(collection.isOpen());
    }

    @Test
    void getChildCollectionCount() throws XMLDBException {
        assertEquals(0, getCollection().getChildCollectionCount());
    }

    @Test
    void getPropertyWithDefault() throws XMLDBException {
        assertEquals("theDefault", getCollection().getProperty("myProperty", "theDefault"));
    }

    @Test
    void hasService(){
        assertTrue(getCollection().hasService(XPathQueryService.class));
    }

    @Test
    void findService(){
        assertNotNull(getCollection().findService(XPathQueryService.class).get());
    }

    @Test
    void getService() throws XMLDBException {
        assertNotNull(getCollection().getService(XPathQueryService.class));
    }

    @Test
    void registerProvders() {
        RemoteCollection remoteCollection = getCollection();
        final ServiceProviderCache.ProviderRegistry registry = createMock(ServiceProviderCache.ProviderRegistry.class);

        registry.add(eq(XPathQueryService.class), notNull());
        registry.add(eq(XQueryService.class), notNull());
        registry.add(eq(CollectionManagementService.class), notNull());
        registry.add(eq(EXistCollectionManagementService.class), notNull());
        registry.add(eq(UserManagementService.class), notNull());
        registry.add(eq(EXistUserManagementService.class), notNull());
        registry.add(eq(DatabaseInstanceManager.class), notNull());
        registry.add(eq(XUpdateQueryService.class), notNull());
        registry.add(eq(IndexQueryService.class), notNull());
        registry.add(eq(EXistRestoreService.class), notNull());

        replay(registry);
        remoteCollection.registerProvders(registry);
        verify(registry);
    }

    @Test
    void listChildCollections() throws XMLDBException {
        assertTrue(getCollection().listChildCollections().isEmpty());
    }

    @Test
    void getChildCollections() throws XMLDBException {
        final RemoteCollection remoteCollection = getCollection();
        assertArrayEquals(new Collection[0], remoteCollection.getChildCollections());
    }

    @Test
    void getResources() throws XMLDBException {
        final RemoteCollection remoteCollection = getCollection();
        assertArrayEquals(new org.exist.Resource[0], remoteCollection.getResources());
    }

    @Test
    void getCreationTime() throws XMLDBException {
        assertNotNull(getCollection().getCreationTime());
    }

    private void prepareContent(final RemoteXMLResource resource) throws XMLDBException, SAXException {
        final char[] buffer = new char[16 * 1024 * 1024];
        Arrays.fill(buffer, (char) 'x');
        final ContentHandler content = resource.setContentAsSAX();
        content.startDocument();
        content.startElement("", "root", "root", new AttributesImpl());
        // writing 16 mb to resource
        content.characters(buffer, 0, buffer.length);
        content.endElement("", "root", "root");
        content.endDocument();
    }

    private void createResources(final List<String> names, final Class<? extends Resource> type) throws XMLDBException {
        final Collection collection = getCollection();
        for (final String name : names) {
            try (final Resource res = collection.createResource(name, type)) {
                if (res instanceof XMLResource) {
                    res.setContent(XML_CONTENT);
                } else if (res instanceof BinaryResource) {
                    res.setContent(BINARY_CONTENT);
                }
                collection.storeResource(res);
            }
        }
    }
}
