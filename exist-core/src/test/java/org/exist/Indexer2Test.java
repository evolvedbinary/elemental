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
package org.exist;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import javax.xml.transform.OutputKeys;

import org.apache.commons.io.output.StringBuilderWriter;
import org.exist.collections.Collection;
import org.exist.security.AuthenticationException;
import org.exist.security.PermissionDeniedException;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.test.TestConstants;
import org.exist.util.LockException;
import org.exist.util.StringInputSource;
import org.exist.util.serializer.SAXSerializer;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceIterator;

import static org.exist.util.PropertiesBuilder.propertiesBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

/**
 * Tests the indexer.
 *
 * @author ljo
 */
public class Indexer2Test {

    private final static String XML =
            "<?xml version=\"1.0\"?>\n" +
            "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\n" +
            "Government of new Territory of Nevada—Governor <name>Nye</name> <lb/>and the practical jokers—<name>Mr. Clemens</name> begins journalistic life <lb/>on <name>Virginia City</name> <name>Enterprise</name>.\n" +
            "</TEI>\n";

    private final static String XQUERY =
            "declare namespace tei=\"http://www.tei-c.org/ns/1.0\"; " +
            "declare boundary-space preserve; " +
            "declare function local:get-text($input as node()*) as item()* {" +
            "   for $node in $input/node() " +
            "       return " +
            "           typeswitch($node)" +
            "               case text() return $node " +
            "               default return local:get-text($node) " +
            "}; " +
            "let $in-memory := " +
            "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\n" +
            "Government of new Territory of Nevada—Governor <name>Nye</name> <lb/>and the practical jokers—<name>Mr. Clemens</name> begins journalistic life <lb/>on <name>Virginia City</name> <name>Enterprise</name>.\n" +
            "</TEI>" +
            "let $stored := doc('" + TestConstants.TEST_COLLECTION_URI.toString() + "/"+ TestConstants.TEST_XML_URI2.toString() + "') " +
            "return " +
            "<result name=\"" + TestConstants.TEST_COLLECTION_URI.toString() + "/"+ TestConstants.TEST_XML_URI2.toString() + "\">\n" +
            "    <inline>{string-join(local:get-text($in-memory))}</inline>\n" +
            "    <stored>{string-join(local:get-text($stored))}</stored>\n" +
            "</result>";

    @Test
    void store_preserve_mixed_ws(final BrokerPool pool) {
        assertTrue(((Boolean) pool.getConfiguration().getProperty(Indexer.PROPERTY_PRESERVE_WS_MIXED_CONTENT)).booleanValue());
        assertEquals("none", pool.getConfiguration().getProperty(Indexer.PROPERTY_SUPPRESS_WHITESPACE));
    }

    @Test
    void retrieve_boundary_space_preserve_with_preserve_mixed_ws(final BrokerPool pool) throws XPathException, PermissionDeniedException, EXistException, SAXException {
        assertEquals("<result name=\"" + TestConstants.TEST_COLLECTION_URI.toString() + "/"+ TestConstants.TEST_XML_URI2.toString() + "\">\n" +
                "    <inline>\n" +
                "Government of new Territory of Nevada—Governor Nye and the practical jokers—Mr. Clemens begins journalistic life on Virginia City Enterprise.\n" + "</inline>\n" +
                "    <stored>\n" +
                "Government of new Territory of Nevada—Governor Nye and the practical jokers—Mr. Clemens begins journalistic life on Virginia City Enterprise.\n" + "</stored>\n" +
                "</result>", executeQuery(pool));
    }

    private String executeQuery(final BrokerPool pool) throws EXistException, PermissionDeniedException, SAXException, XPathException {
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final StringBuilderWriter out = new StringBuilderWriter();
             final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(XQUERY), false, null, null, null, null, null)) {

            final Sequence result = queryResult.result;
            final Properties props = new Properties();
            props.setProperty(OutputKeys.INDENT, "yes");
            final SAXSerializer serializer = new SAXSerializer(out, props);
            serializer.startDocument();
            for (final SequenceIterator i = result.iterate(); i.hasNext(); ) {
                final Item next = i.nextItem();
                next.toSAX(broker, serializer, props);
            }
            serializer.endDocument();

            return out.toString();
        }
    }

    private static void storeDoc(final BrokerPool pool) throws PermissionDeniedException, IOException, EXistException, SAXException, LockException, AuthenticationException {
        final TransactionManager txnMgr = pool.getTransactionManager();

        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().authenticate("admin", "")));
                final Txn txn = txnMgr.beginTransaction()) {

            try (final Collection collection = broker.getOrCreateCollection(txn, TestConstants.TEST_COLLECTION_URI)) {
                final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
                broker.storeDocument(txn, TestConstants.TEST_XML_URI2, new StringInputSource(XML), xmlMediaType, collection);

                broker.flush();
                broker.saveCollection(txn, collection);
            }
            txnMgr.commit(txn);
        }
    }

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(
            propertiesBuilder()
                .put(Indexer.PROPERTY_PRESERVE_WS_MIXED_CONTENT, true)
                .set(Indexer.PROPERTY_SUPPRESS_WHITESPACE, "none")
                .build(),
            true,
            false);

    @BeforeAll
    static void setUp(final BrokerPool pool) throws EXistException, PermissionDeniedException, IOException, SAXException, LockException, AuthenticationException {
        storeDoc(pool);
    }
}
