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
package org.exist.xquery.functions.util;

import org.exist.test.ExistXmldbEmbeddedServer;

import static org.junit.Assert.*;

import org.exist.xmldb.EXistResourceSet;
import org.junit.ClassRule;
import org.junit.Test;

import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;


/**
 * @author Andrzej Taramina (andrzej@chaeron.com)
 */
public class Base64FunctionsTest {

    @ClassRule
    public static ExistXmldbEmbeddedServer existXmldbEmbeddedServer = new ExistXmldbEmbeddedServer(false, true, true);

    @Test
    public void testBase64Encode() throws XMLDBException {
        final String query = "util:base64-encode( 'This is a test!' )";
        try (final EXistResourceSet result = existXmldbEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("VGhpcyBpcyBhIHRlc3Qh", r);
            }
        }
    }

    @Test
    public void testBase64EncodeWithTrim() throws XMLDBException {
        final String query = "util:base64-encode( 'This is a longer test to enforce an encoded string longer than the chunking limit!', true() )";
        try (final EXistResourceSet result = existXmldbEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("VGhpcyBpcyBhIGxvbmdlciB0ZXN0IHRvIGVuZm9yY2UgYW4gZW5jb2RlZCBzdHJpbmcgbG9uZ2VyIHRoYW4gdGhlIGNodW5raW5nIGxpbWl0IQ==", r);
            }
        }
    }

    @Test
    public void testBase64EncodeWithTrimFalse() throws XMLDBException {
        final String query = "util:base64-encode( 'This is a longer test to enforce an encoded string longer than the chunking limit!', false() )";
        try (final EXistResourceSet result = existXmldbEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("VGhpcyBpcyBhIGxvbmdlciB0ZXN0IHRvIGVuZm9yY2UgYW4gZW5jb2RlZCBzdHJpbmcgbG9uZ2VyIHRoYW4gdGhlIGNodW5raW5nIGxpbWl0IQ==", r);
            }
        }
    }

    @Test
    public void testBase64Decode() throws XMLDBException {
        final String query = "util:base64-decode( 'VGhpcyBpcyBhIHRlc3Qh' )";
        try (final EXistResourceSet result = existXmldbEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("This is a test!", r);
            }
        }
    }

    @Test
    public void testBase64EncodeDecode() throws XMLDBException {
        final String query = "util:base64-decode( util:base64-encode( 'This is a test!' ) )";
        try (final EXistResourceSet result = existXmldbEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("This is a test!", r);
            }
        }
    }

    @Test
    public void testBase64EncodeUrlSafeNoSpecial() throws XMLDBException {
        final String query = "util:base64-encode-url-safe( 'This is a test!' )";
        try (final EXistResourceSet result = existXmldbEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("VGhpcyBpcyBhIHRlc3Qh", r);
            }
        }
    }

    @Test
    public void testBase64EncodeUrlSafeSpecial() throws XMLDBException {
        final String query = "util:base64-encode-url-safe( '.ÿd' )";
        try (final EXistResourceSet result = existXmldbEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals("LsO_ZA", r);
            }
        }
    }

    @Test
    public void testBase64DecodeUrlSafe() throws XMLDBException {
        final String query = "util:base64-decode( 'LsO_ZA' )";
        try (final EXistResourceSet result = existXmldbEmbeddedServer.executeQuery(query)) {
            try (final Resource resource = result.getResource(0)) {
                final String r = (String) resource.getContent();
                assertEquals(".ÿd", r);
            }
        }
    }
}
