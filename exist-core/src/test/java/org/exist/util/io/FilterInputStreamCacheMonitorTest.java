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
package org.exist.util.io;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.exist.test.ExistXmldbEmbeddedServer;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.ExtendedResource;
import org.exist.xmldb.LocalBinaryResource;
import org.exist.xmldb.LocalXMLResource;
import org.exist.xquery.XPathException;
import org.exist.xquery.functions.map.MapType;
import org.exist.xquery.value.BinaryValue;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.Assert.*;

public class FilterInputStreamCacheMonitorTest {

    @ClassRule
    public static final ExistXmldbEmbeddedServer existXmldbEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    private static final String EOL = System.getProperty("line.separator");
    private static final String TEST_COLLECTION_NAME = "testFilterInputStreamCacheMonitor";
    private static final XXHash64 XXHASH64 = XXHashFactory.fastestInstance().hash64();
    private static final long XXHASH64_SEED = 0x6429e31a;
    private static long EXPECTED_ICON_HASH = -1;

    @BeforeClass
    public static void setup() throws XMLDBException, URISyntaxException, IOException {
        final FilterInputStreamCacheMonitor monitor = FilterInputStreamCacheMonitor.getInstance();
        final int activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should have no active binaries, but found: " + activeCount + ". It is likely that a previous test or process within the same JVM is leaking file handles! This should be investigated. Dump: " + monitor.dump(), 0, activeCount);

        final Path icon = Paths.get(FilterInputStreamCacheMonitorTest.class.getResource("icon.png").toURI());
        final byte[] iconBytes = Files.readAllBytes(icon);
        EXPECTED_ICON_HASH = XXHASH64.hash(iconBytes, 0, iconBytes.length, XXHASH64_SEED);

        try (final Collection testCollection = existXmldbEmbeddedServer.createCollection(existXmldbEmbeddedServer.getRoot(), TEST_COLLECTION_NAME);
                final EXistResource resource = (EXistResource) testCollection.createResource("icon.png", "BinaryResource")) {
            resource.setContent(icon);
            testCollection.storeResource(resource);
        }
    }

    @AfterClass
    public static void cleanup() throws XMLDBException {
        final CollectionManagementService cms = (CollectionManagementService) existXmldbEmbeddedServer.getRoot().getService("CollectionManagementService", "1.0");
        cms.removeCollection(TEST_COLLECTION_NAME);
    }

    @Test
    public void binaryResult() throws XMLDBException, XPathException {
        final FilterInputStreamCacheMonitor monitor = FilterInputStreamCacheMonitor.getInstance();

        // assert no binaries in use yet
        int activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should have no active binaries, but found: " + activeCount + "." +  EOL + monitor.dump(), 0, activeCount);

        try (final EXistResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery("util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/icon.png')")) {

            assertEquals(1, resourceSet.getSize());

            try (final EXistResource resource = (EXistResource) resourceSet.getResource(0)) {
                assertTrue(resource instanceof LocalBinaryResource);

                // assert that there is one active binary (as it is in the result set)
                assertEquals(1, monitor.getActive().size());

                // check the value of the retrieved binary
                final Object extendedContent = ((ExtendedResource) resource).getExtendedContent();
                assertTrue(extendedContent instanceof BinaryValue);

                final BinaryValue binaryValue = (BinaryValue) extendedContent;
                final byte[] retrievedIconBytes = binaryValue.toJavaObject(byte[].class);
                final long retrievedIconHash = XXHASH64.hash(retrievedIconBytes, 0, retrievedIconBytes.length, XXHASH64_SEED);
                assertEquals(EXPECTED_ICON_HASH, retrievedIconHash);
            }

            // assert no active binaries as we just closed the binary resource in the try-with-resources
            activeCount = monitor.getActive().size();
            assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);

        }

        // final assert no active binaries as we just cleared the resource-set
        activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);
    }

    @Test
    public void singleElementEnclosedExprBinaryValueStringResult() throws XMLDBException {
        final FilterInputStreamCacheMonitor monitor = FilterInputStreamCacheMonitor.getInstance();

        // assert no binaries in use yet
        int activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);

        try (final EXistResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(
                    "let $embedded := <logo><image>{util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/icon.png')}</image></logo>\n" +
                            "return\n" +
                            "xmldb:store('/db/" + TEST_COLLECTION_NAME + "', 'icon.xml', $embedded)")) {

            assertEquals(1, resourceSet.getSize());
            try (final EXistResource resource = (EXistResource) resourceSet.getResource(0)) {
                assertFalse(resource instanceof LocalBinaryResource);

                // assert still no active binaries (because they have been cleaned up - see XQueryContext#exitEnclosedExpr())
                activeCount = monitor.getActive().size();
                assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);
            }

        }

        // final assert no active binaries as we just cleared the resource-set in the try-with-resources
        activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);
    }

    @Test
    public void multipleElementsEnclosedExprBinaryValueStringResult() throws XMLDBException {
        final FilterInputStreamCacheMonitor monitor = FilterInputStreamCacheMonitor.getInstance();

        // assert no binaries in use yet
        int activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should have no active binaries, but found: " + activeCount + "." + EOL + monitor.dump(), 0, activeCount);

        try (final EXistResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(
                    "let $bin := util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/icon.png')\n" +
                    "let $embedded := <logo><image>{$bin}</image></logo>\n" +
                    "let $embedded-2 := <other>{$bin}</other>\n" +
                    "return\n" +
                    "xmldb:store('/db/" + TEST_COLLECTION_NAME + "', 'icon.xml', $embedded)")) {

            assertEquals(1, resourceSet.getSize());
            try (final EXistResource resource = (EXistResource) resourceSet.getResource(0)) {
                assertFalse(resource instanceof LocalBinaryResource);

                // assert still no active binaries (because they have been cleaned up - see XQueryContext#exitEnclosedExpr())
                activeCount = monitor.getActive().size();
                assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);
            }

        }

        // final assert no active binaries as we just cleared the resource-set in the try-with-resources
        activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);
    }

    @Test
    public void singleElementEnclosedExprBinaryValueElementResult() throws XMLDBException {
        final FilterInputStreamCacheMonitor monitor = FilterInputStreamCacheMonitor.getInstance();

        // assert no binaries in use yet
        int activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);

        try (final EXistResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery("<logo><image>{util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/icon.png')}</image></logo>")) {

            assertEquals(1, resourceSet.getSize());
            try (final EXistResource resource = (EXistResource) resourceSet.getResource(0)) {
                assertFalse(resource instanceof LocalBinaryResource);

                // assert still no active binaries (because they have been cleaned up - see XQueryContext#exitEnclosedExpr())
                activeCount = monitor.getActive().size();
                assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);

                // check the value of the retrieved binary enclosed in the element
                final Node logoElement = ((XMLResource) resource).getContentAsDOM();
                assertTrue(logoElement instanceof Element);
                final Node imageElement = logoElement.getFirstChild();
                assertTrue(imageElement instanceof Element);
                final String textContent = imageElement.getTextContent();
                assertNotNull(textContent);
                final byte[] retrievedIconBytes = Base64.getDecoder().decode(textContent);
                final long retrievedIconHash = XXHASH64.hash(retrievedIconBytes, 0, retrievedIconBytes.length, XXHASH64_SEED);
                assertEquals(EXPECTED_ICON_HASH, retrievedIconHash);
            }

        }

        // final assert no active binaries as we just cleared the resource-set in the try-with-resources
        activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);
    }

    @Test
    public void multipleElementsEnclosedExprBinaryValueElementResults() throws XMLDBException {
        final FilterInputStreamCacheMonitor monitor = FilterInputStreamCacheMonitor.getInstance();

        // assert no binaries in use yet
        int activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should have no active binaries, but found: " + activeCount + "." + EOL + monitor.dump(), 0, activeCount);

        try (final EXistResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(
                    "let $bin := util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/icon.png')\n" +
                    "return\n" +
                    "(<logo><image>{$bin}</image></logo>, <other>{$bin}</other>)")) {


            assertEquals(2, resourceSet.getSize());

            try (final EXistResource resource = (EXistResource) resourceSet.getResource(0)) {
                assertFalse(resource instanceof LocalBinaryResource);

                // assert still no active binaries (because they have been cleaned up - see XQueryContext#exitEnclosedExpr())
                activeCount = monitor.getActive().size();
                assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);

                // check the value of the retrieved binary enclosed in the element
                final Node logoElement = ((XMLResource) resource).getContentAsDOM();
                assertTrue(logoElement instanceof Element);
                final Node imageElement = logoElement.getFirstChild();
                assertTrue(imageElement instanceof Element);
                final String textContent = imageElement.getTextContent();
                assertNotNull(textContent);
                final byte[] retrievedIconBytes = Base64.getDecoder().decode(textContent);
                final long retrievedIconHash = XXHASH64.hash(retrievedIconBytes, 0, retrievedIconBytes.length, XXHASH64_SEED);
                assertEquals(EXPECTED_ICON_HASH, retrievedIconHash);
            }

            try (final EXistResource resource = (EXistResource) resourceSet.getResource(1)) {
                assertFalse(resource instanceof LocalBinaryResource);

                // assert still no active binaries (because they have been cleaned up - see XQueryContext#exitEnclosedExpr())
                activeCount = monitor.getActive().size();
                assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);

                // check the value of the retrieved binary enclosed in the element
                final Node otherElement = ((XMLResource) resource).getContentAsDOM();
                assertTrue(otherElement instanceof Element);
                final String textContent = otherElement.getTextContent();
                assertNotNull(textContent);
                final byte[] retrievedIconBytes = Base64.getDecoder().decode(textContent);
                final long retrievedIconHash = XXHASH64.hash(retrievedIconBytes, 0, retrievedIconBytes.length, XXHASH64_SEED);
                assertEquals(EXPECTED_ICON_HASH, retrievedIconHash);
            }

        }

        // final assert no active binaries as we just cleared the resource-set in the try-with-resources
        activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);
    }

    @Test
    public void mapEnclosedBinaryValueMapResult() throws XMLDBException, XPathException {
        final FilterInputStreamCacheMonitor monitor = FilterInputStreamCacheMonitor.getInstance();

        // assert no binaries in use yet
        int activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);

        try (final EXistResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(
                    "map { 'key1': util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/icon.png') }")) {

            assertEquals(1, resourceSet.getSize());
            try (final EXistResource resource = (EXistResource) resourceSet.getResource(0)) {
                assertFalse(resource instanceof LocalBinaryResource);

                // assert that there is one active binary (as it is within the Map in the result set)
                assertEquals(1, monitor.getActive().size());

                // check the value of the retrieved binary enclosed in the map
                assertTrue(resource instanceof LocalXMLResource);
                final Item content = ((LocalXMLResource) resource).getContentAsXdm();
                assertTrue(content instanceof MapType);
                final MapType map = (MapType) content;
                final Sequence value = map.get(new StringValue("key1"));
                assertTrue(value instanceof BinaryValue);
                final BinaryValue binaryValue = (BinaryValue) value;
                final byte[] retrievedIconBytes = binaryValue.toJavaObject(byte[].class);
                final long retrievedIconHash = XXHASH64.hash(retrievedIconBytes, 0, retrievedIconBytes.length, XXHASH64_SEED);
                assertEquals(EXPECTED_ICON_HASH, retrievedIconHash);
            }

            // assert that there is still one active binary, because closing the Resource doesn't close the binary as it is within the Map of the Resource Set
            activeCount = monitor.getActive().size();
            assertEquals(1, activeCount);

        }

        // final assert no active binaries as we just cleared the resource-set in the try-with-resources
        activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);
    }

    @Test
    public void mapEnclosedBinaryValuesMapResult() throws XMLDBException, XPathException {
        final FilterInputStreamCacheMonitor monitor = FilterInputStreamCacheMonitor.getInstance();

        // assert no binaries in use yet
        int activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);

        try (final EXistResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(
                    "map { 'key1': util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/icon.png'), 'key2': util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/icon.png') }")) {

            assertEquals(1, resourceSet.getSize());
            try (final EXistResource resource = (EXistResource)resourceSet.getResource(0)) {
                assertFalse(resource instanceof LocalBinaryResource);

                // assert that there are two active binaries (as they are within the Map in the result set)
                assertEquals(2, monitor.getActive().size());

                // check the value of the retrieved binary enclosed in the map
                assertTrue(resource instanceof LocalXMLResource);
                final Item content = ((LocalXMLResource) resource).getContentAsXdm();
                assertTrue(content instanceof MapType);
                final MapType map = (MapType) content;
                final Sequence value = map.get(new StringValue("key1"));
                assertTrue(value instanceof BinaryValue);
                final BinaryValue binaryValue = (BinaryValue) value;
                final byte[] retrievedIconBytes = binaryValue.toJavaObject(byte[].class);
                final long retrievedIconHash = XXHASH64.hash(retrievedIconBytes, 0, retrievedIconBytes.length, XXHASH64_SEED);
                assertEquals(EXPECTED_ICON_HASH, retrievedIconHash);
            }

            // assert that there are still two active binaries, because closing the Resource doesn't close the binaries as they are within the Map of the Resource Set
            activeCount = monitor.getActive().size();
            assertEquals(2, activeCount);

        }

        // final assert no active binaries as we just cleared the resource-set in the try-with-resources
        activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);
    }

    @Test
    public void mapEnclosedMapBinaryValueMapResult() throws XMLDBException, XPathException {
        final FilterInputStreamCacheMonitor monitor = FilterInputStreamCacheMonitor.getInstance();

        // assert no binaries in use yet
        int activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);

        try (final EXistResourceSet resourceSet = existXmldbEmbeddedServer.executeQuery(
                    "let $bin := util:binary-doc('/db/" + TEST_COLLECTION_NAME + "/icon.png')\n" +
                    "return\n" +
                    "map { 'key1': $bin, 'key2': map { 'key3': $bin } }")) {

            assertEquals(1, resourceSet.getSize());
            try (final EXistResource resource = (EXistResource)resourceSet.getResource(0)) {
                assertFalse(resource instanceof LocalBinaryResource);

                // assert that there is one active binary (as it is within the Map in the result set)
                assertEquals(1, monitor.getActive().size());

                // check the value of the retrieved binary enclosed in the map
                assertTrue(resource instanceof LocalXMLResource);
                final Item content = ((LocalXMLResource) resource).getContentAsXdm();
                assertTrue(content instanceof MapType);
                MapType map = (MapType) content;
                Sequence value = map.get(new StringValue("key1"));
                assertTrue(value instanceof BinaryValue);
                BinaryValue binaryValue = (BinaryValue) value;
                byte[] retrievedIconBytes = binaryValue.toJavaObject(byte[].class);
                long retrievedIconHash = XXHASH64.hash(retrievedIconBytes, 0, retrievedIconBytes.length, XXHASH64_SEED);
                assertEquals(EXPECTED_ICON_HASH, retrievedIconHash);

                value = map.get(new StringValue("key2"));
                assertTrue(value instanceof MapType);
                map = (MapType) value;

                value = map.get(new StringValue("key3"));
                assertTrue(value instanceof BinaryValue);
                binaryValue = (BinaryValue) value;
                retrievedIconBytes = binaryValue.toJavaObject(byte[].class);
                retrievedIconHash = XXHASH64.hash(retrievedIconBytes, 0, retrievedIconBytes.length, XXHASH64_SEED);
                assertEquals(EXPECTED_ICON_HASH, retrievedIconHash);
            }

            // assert that there is still one active binary, because closing the Resource doesn't close the binary as it is within the Map of the Resource Set
            activeCount = monitor.getActive().size();
            assertEquals(1, activeCount);

        }

        // final assert no active binaries as we just cleared the resource-set in the try-with-resources
        activeCount = monitor.getActive().size();
        assertEquals("FilterInputStreamCacheMonitor should again have no active binaries, but found: " + activeCount + "."  + EOL + monitor.dump(), 0, activeCount);
    }

}
