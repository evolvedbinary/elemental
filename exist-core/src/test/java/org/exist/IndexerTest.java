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
import org.junit.jupiter.api.Disabled;

import static org.exist.util.PropertiesBuilder.propertiesBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

/**
 * Tests the indexer.
 * 
 * @author ljo
 */
public class IndexerTest {

	@RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(
			propertiesBuilder()
            	.set(Indexer.PROPERTY_SUPPRESS_WHITESPACE, "none")
            	.build(),
			true,
			true);

    private final static String XML =
        "<?xml version=\"1.0\"?>\n" +
	"<x>\n" +
	"    <y>a <b>b</b> c</y>\n" +
	"    <z>a<b>b</b>c</z>\n" +
	"</x>\n";
    
    private final static String XML_XSLT =
        "<?xml version=\"1.0\"?>\n" +
	"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
        "    <xsl:template match=\"processing-instruction()\" mode=\"xml2string\">\n" +
        "        <xsl:text>&lt;?</xsl:text>\n" +
        "        <xsl:value-of select=\"name()\"/>\n" +
        "        <xsl:text> \n" +
        "\n" +
        "</xsl:text>\n" +
        "        <xsl:value-of select=\".\"/>\n" +
        "        <xsl:text>?&gt;</xsl:text>\n" +
        "    </xsl:template>\n" +
        "</xsl:stylesheet>\n";

    private final static String RESULT_NO_PRESERVE_MIXED_WS_XML =
	"<result>\n" +
	"    <node n=\"1\">\n" +
	"        <y>a\n" +
	"        <b>b</b>\n" +
	"            c</y>\n" +
	"    </node>\n" +
	"    <node n=\"2\">a </node>\n" +
	"    <node n=\"3\">\n" +
	"        <b>b</b>\n" +
	"    </node>\n" +
	"    <node n=\"4\">b</node>\n"+
	"    <node n=\"5\"> c</node>\n" +
	"    <node n=\"6\">\n" +
	"        <z>a\n" +
	"            <b>b</b>\n" +
	"            c</z>\n" +
	"    </node>\n" +
	"    <node n=\"7\">a</node>\n" +
	"    <node n=\"8\">\n" +
	"        <b>b</b>\n" +
	"    </node>\n" +
	"    <node n=\"9\">b</node>\n" +
	"    <node n=\"10\">c</node>\n" +
	"</result>";

    private final static String RESULT_PRESERVE_MIXED_WS_XML =
	"<result>\n" +
	"    <node n=\"1\">\n" +
	"        <y>a <b>b</b> c</y>\n" +
	"    </node>\n" +
	"    <node n=\"2\">a </node>\n" +
	"    <node n=\"3\">\n" +
	"        <b>b</b>\n" +
	"    </node>\n" +
	"    <node n=\"4\">b</node>\n"+
	"    <node n=\"5\"> c</node>\n" +
	"    <node n=\"6\">\n" +
	"        <z>a<b>b</b> c</z>\n" +
	"    </node>\n" +
	"    <node n=\"7\">a</node>\n" +
	"    <node n=\"8\">\n" +
	"        <b>b</b>\n" +
	"    </node>\n" +
	"    <node n=\"9\">b</node>\n" +
	"    <node n=\"10\">c</node>\n" +
	"</result>";

    private final static String RESULT_XML_XSLT =
        "<result>\n" +
	"    <xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
        "    <xsl:template match=\"processing-instruction()\" mode=\"xml2string\">\n" +
        "        <xsl:text>&lt;?</xsl:text>\n" +
        "        <xsl:value-of select=\"name()\"/>\n" +
        "        <xsl:text> \n" +
        "\n" +
        "</xsl:text>\n" +
        "        <xsl:value-of select=\".\"/>\n" +
        "        <xsl:text>?&gt;</xsl:text>\n" +
        "    </xsl:template>\n" +
        "</xsl:stylesheet>\n" +
        "</result>";
    
    private final static String XQUERY =
	"let $test := doc('" + TestConstants.TEST_COLLECTION_URI.toString() + "/"+ TestConstants.TEST_XML_URI.toString() + "')/* " +
	"return " +
	"    <result>" +
	"    {" +
	"        for $node at $i in $test//node()\n" +
	"        return <node n=\"{$i}\">{$node}</node>\n" +
	"    }" +
        "    </result>";
    
    private final static String XQUERY_XSLT =
	"let $test := doc('" + TestConstants.TEST_COLLECTION_URI.toString() + "/"+ TestConstants.TEST_XML_URI.toString() + "')/* " +
	"return " +
	"    <result>{$test}</result>";

    private void store_preserve_ws_mixed_content_value(final BrokerPool pool, final boolean propValue, final String xml) throws PermissionDeniedException, IOException, EXistException, SAXException, LockException, AuthenticationException {
		pool.getConfiguration().setProperty(Indexer.PROPERTY_PRESERVE_WS_MIXED_CONTENT, propValue);

        final TransactionManager txnMgr = pool.getTransactionManager();

    	try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().authenticate("admin", "")));
	    		final Txn txn = txnMgr.beginTransaction()) {

            try (final Collection collection = broker.getOrCreateCollection(txn, TestConstants.TEST_COLLECTION_URI)) {
                final MediaType xmlMediaType = broker.getBrokerPool().getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
				broker.storeDocument(txn, TestConstants.TEST_XML_URI, new StringInputSource(xml), xmlMediaType, collection);

				broker.flush();
				broker.saveCollection(txn, collection);
			}
            txnMgr.commit(txn);

        }
	
    }
    
    private String store_and_retrieve_ws_mixed_content_value(final BrokerPool pool, final boolean preserve, final String typeXml, final String typeXquery) throws EXistException, IOException, LockException, AuthenticationException, PermissionDeniedException, SAXException, XPathException {
		store_preserve_ws_mixed_content_value(pool, preserve, typeXml);
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
             final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(typeXquery), false, null, null, null, null, null)) {

            final Sequence result = queryResult.result;
            try (final StringBuilderWriter out = new StringBuilderWriter()) {
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
    }

    @Disabled
    @Test
    void retrieve_preserve_mixed_ws(final BrokerPool pool) throws EXistException, IOException, LockException, AuthenticationException, PermissionDeniedException, SAXException, XPathException {
		//Nodes 1, 7 and 13 are not in mixed-contents and should not be preserved. They are the spaces between elements x and y, y and z, and z and x.
        assertEquals(RESULT_PRESERVE_MIXED_WS_XML, store_and_retrieve_ws_mixed_content_value(pool, true, XML, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_no_preserve_mixed_ws(final BrokerPool pool) throws EXistException, IOException, LockException, AuthenticationException, PermissionDeniedException, SAXException, XPathException {
        assertEquals(RESULT_NO_PRESERVE_MIXED_WS_XML, store_and_retrieve_ws_mixed_content_value(pool, false, XML, XQUERY));
    }

    @Test
    void retrieve_xslt_preserve_mixed_ws(final BrokerPool pool) throws EXistException, IOException, LockException, AuthenticationException, PermissionDeniedException, SAXException, XPathException {
        assertEquals(RESULT_XML_XSLT, store_and_retrieve_ws_mixed_content_value(pool, true, XML_XSLT, XQUERY_XSLT));
    }
}
