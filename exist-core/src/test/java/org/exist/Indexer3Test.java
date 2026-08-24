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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xml.sax.SAXException;
import xyz.elemental.mediatype.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the indexer.
 *
 * @author ljo
 */
public class Indexer3Test {

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, true);

    private final static String XML1 =
            "<?xml version=\"1.0\"?>\n" +
                    "<k>\n" +
                    "<l>a <b>b</b> c <d> d </d>  <e>  </e> f</l>\n" +
                    "<m> a <b>b</b> c <d> d </d>  <e>  </e> f </m>\n" +
                    "<n> a<b>b</b> c <d> d </d>  <e>  </e>f </n>\n" +
                    "<o>  <b>b</b> c <d> d </d>  <e>  </e>  </o>\n" +
                    "</k>\n";

    private final static String XML2 =
            "<?xml version=\"1.0\"?>\n" +
                    "<k>\n" +
                    "<l>a <b>b</b> c <d> d </d>  <e>  </e> f</l>\n" +
                    "</k>\n";

    private final static String XML3 =
            "<?xml version=\"1.0\"?>\n" +
                    "<k>\n" +
                    "<m> a <b>b</b> c <d> d </d>  <e>  </e> f </m>\n" +
                    "</k>\n";

    private final static String XML4 =
            "<?xml version=\"1.0\"?>\n" +
                    "<k>\n" +
                    "<n> a<b>b</b> c <d> d </d>  <e>  </e>f </n>\n" +
                    "</k>\n";

    private final static String XML5 =
            "<?xml version=\"1.0\"?>\n" +
                    "<k>\n" +
                    "<o>  <b>b</b> c <d> d </d>  <e>  </e>  </o>\n" +
                    "</k>\n";

    private final static String XML6 =
            "<?xml version=\"1.0\"?>\n" +
                    "<k>\n" +
                    "<!--    a comment with whitespace    leading, intermediate\n" +
                    " and trailing   -->\n" +
                    "</k>\n";

    private final static String XML7 =
            "<?xml version=\"1.0\"?>\n" +
                    "<k>\n" +
                    "    <o>    leading and trailing    </o>\n" +
                    "</k>\n";

    private final static String RESULT_SUPPRESS_WS_NONE_XML1 =
            "<result>" +
                    "<k>\n" +
                    "<l>a <b>b</b> c <d> d </d>  <e>  </e> f</l>\n" +
                    "<m> a <b>b</b> c <d> d </d>  <e>  </e> f </m>\n" +
                    "<n> a<b>b</b> c <d> d </d>  <e>  </e>f </n>\n" +
                    "<o>  <b>b</b> c <d> d </d>  <e>  </e>  </o>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_NONE_XML2 =
            "<result>" +
                    "<k>\n" +
                    "<l>a <b>b</b> c <d> d </d>  <e>  </e> f</l>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_NONE_XML3 =
            "<result>" +
                    "<k>\n" +
                    "<m> a <b>b</b> c <d> d </d>  <e>  </e> f </m>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_NONE_XML4 =
            "<result>" +
                    "<k>\n" +
                    "<n> a<b>b</b> c <d> d </d>  <e>  </e>f </n>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_NONE_XML5 =
            "<result>" +
                    "<k>\n" +
                    "<o>  <b>b</b> c <d> d </d>  <e>  </e>  </o>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_NONE_XML6 =
            "<result>" +
                    "<k>\n" +
                    "<!--    a comment with whitespace    leading, intermediate\n" +
                    " and trailing   -->\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_NONE_XML7 =
            "<result>" +
                    "<k>\n" +
                    "    <o>    leading and trailing    </o>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_LEADING_XML1 =
            "<result>" +
                    "<k>" +
                    "<l>a <b>b</b> c <d>d </d> <e>  </e> f</l>" +
                    "<m>a <b>b</b> c <d>d </d> <e>  </e> f </m>" +
                    "<n>a<b>b</b> c <d>d </d>  <e>  </e>f </n>" +
                    "<o> <b>b</b> c <d>d </d> <e>  </e>  </o>" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_LEADING_XML2 =
            "<result>" +
                    "<k>" +
                    "<l>a <b>b</b> c <d>d </d> <e>  </e> f</l>" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_LEADING_XML3 =
            "<result>" +
                    "<k>" +
                    "<m>a <b>b</b> c <d>d </d> <e>  </e> f </m>" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_LEADING_XML4 =
            "<result>" +
                    "<k>" +
                    "<n>a<b>b</b> c <d>d </d>  <e>  </e>f </n>" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_LEADING_XML5 =
            "<result>" +
                    "<k>" +
                    "<o> <b>b</b> c <d>d </d> <e>  </e>  </o>" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_LEADING_XML6 =
            "<result>" +
                    "<k>\n" +
                    "<!--    a comment with whitespace    leading, intermediate\n" +
                    " and trailing   -->\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_LEADING_XML7 =
            "<result>" +
                    "<k>\n" +
                    "    <o>leading and trailing    </o>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_TRAILING_XML1 =
            "<result>" +
                    "<k>" +
                    "<l>a <b>b</b> c <d> d</d>  <e>  </e> f</l>" +
                    "<m> a <b>b</b> c <d> d</d>  <e>  </e> f</m>" +
                    "<n> a<b>b</b> c <d> d</d>  <e>  </e>f</n>" + // kolla " a" och "f "
                    "<o>  <b>b</b> c <d> d</d>  <e>  </e> </o>" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_TRAILING_XML2 =
            "<result>" +
                    "<k>" +
                    "<l>a <b>b</b> c <d> d</d>  <e>  </e> f</l>" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_TRAILING_XML3 =
            "<result>" +
                    "<k>" +
                    "<m> a <b>b</b> c <d> d</d>  <e>  </e> f</m>" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_TRAILING_XML4 =
            "<result>" +
                    "<k>" +
                    "<n> a<b>b</b> c <d> d</d>  <e>  </e>f</n>" + // kolla " a" och "f "
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_TRAILING_XML5 =
            "<result>" +
                    "<k>" +
                    "<o>  <b>b</b> c <d> d</d>  <e>  </e> </o>" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_TRAILING_XML6 =
            "<result>" +
                    "<k>\n" +
                    "<!--    a comment with whitespace    leading, intermediate\n" +
                    " and trailing   -->\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_TRAILING_XML7 =
            "<result>" +
                    "<k>\n" +
                    "    <o>    leading and trailing</o>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_BOTH_XML1 =
            "<result>" +
                    "<k>\n" +
                    "<l>a <b>b</b> c <d>d</d>  <e>  </e> f</l>\n" +  // kolla "a"
                    "<m>a <b>b</b> c <d>d</d>  <e>  </e> f</m>\n" +  // kolla "f "
                    "<n>a<b>b</b> c <d>d</d>  <e>  </e>f</n>\n" +
                    "<o> <b>b</b> c <d>d</d>  <e>  </e> </o>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_BOTH_XML2 =
            "<result>" +
                    "<k>\n" +
                    "<l>a <b>b</b> c <d>d</d>  <e>  </e> f</l>\n" +  // kolla "a"
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_BOTH_XML3 =
            "<result>" +
                    "<k>\n" +
                    "<m>a <b>b</b> c <d>d</d>  <e>  </e> f</m>\n" +  // kolla "f "
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_BOTH_XML4 =
            "<result>" +
                    "<k>\n" +
                    "<n>a<b>b</b> c <d>d</d>  <e> </e>f</n>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_BOTH_XML5 =
            "<result>" +
                    "<k>\n" +
                    "<o>  <b>b</b>c <d>d</d> <e> </e> </o>\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_BOTH_XML6 =
            "<result>" +
                    "<k>\n" +
                    "<!--    a comment with whitespace    leading, intermediate\n" +
                    " and trailing   -->\n" +
                    "</k>" +
                    "</result>";

    private final static String RESULT_SUPPRESS_WS_BOTH_XML7 =
            "<result>" +
                    "<k>\n" +
                    "    <o>leading and trailing</o>\n" +
                    "</k>" +
                    "</result>";

    private final static String XQUERY =
            "let $test := doc('" + TestConstants.TEST_COLLECTION_URI.toString() + "/" + TestConstants.TEST_XML_URI.toString() + "') " +
                    "return " +
                    "    <result>{$test/k}</result>";

    private void store_suppress_type(final BrokerPool pool, final String propValue, final String xml) throws PermissionDeniedException, IOException, EXistException, SAXException, LockException, AuthenticationException {
        pool.getConfiguration().setProperty(Indexer.PROPERTY_SUPPRESS_WHITESPACE, propValue);
        // Make sure to keep preserve whitespace mixed content stable even if default changes. fixme! - should test both. /ljo
        boolean propWSMValue = false;
        pool.getConfiguration().setProperty(Indexer.PROPERTY_PRESERVE_WS_MIXED_CONTENT, propWSMValue);

        final TransactionManager txnMgr = pool.getTransactionManager();

        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().authenticate("admin", "")));
                final Txn txn = txnMgr.beginTransaction()) {

            try (final Collection collection = broker.getOrCreateCollection(txn, TestConstants.TEST_COLLECTION_URI)) {
                final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
                broker.storeDocument(txn, TestConstants.TEST_XML_URI, new StringInputSource(xml), xmlMediaType, collection);

                broker.flush();
                broker.saveCollection(txn, collection);
            }
            txnMgr.commit(txn);
        }
    }

    private String store_and_retrieve_suppress_type(final BrokerPool pool, final String type, final String typeXml, final String typeXquery) throws EXistException, IOException, LockException, AuthenticationException, PermissionDeniedException, SAXException, XPathException {
        store_suppress_type(pool, type, typeXml);
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
                final StringBuilderWriter out = new StringBuilderWriter();
                final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(typeXquery), false, null, null, null, null, null)) {

            final Sequence result = queryResult.result;
            final Properties props = new Properties();
            props.setProperty(OutputKeys.INDENT, "no");
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

    @Test
    void retrieve_suppress_ws_none1(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_NONE_XML1, store_and_retrieve_suppress_type(pool, "none", XML1, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_none2(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_NONE_XML2, store_and_retrieve_suppress_type(pool, "none", XML2, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_none3(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_NONE_XML3, store_and_retrieve_suppress_type(pool, "none", XML3, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_none4(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_NONE_XML4, store_and_retrieve_suppress_type(pool, "none", XML4, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_none5(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_NONE_XML5, store_and_retrieve_suppress_type(pool, "none", XML5, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_none6(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_NONE_XML6, store_and_retrieve_suppress_type(pool, "none", XML6, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_none7(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_NONE_XML7, store_and_retrieve_suppress_type(pool, "none", XML7, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_leading1(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_LEADING_XML1, store_and_retrieve_suppress_type(pool, "leading", XML1, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_leading2(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_LEADING_XML2, store_and_retrieve_suppress_type(pool, "leading", XML2, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_leading3(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_LEADING_XML3, store_and_retrieve_suppress_type(pool, "leading", XML3, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_leading4(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_LEADING_XML4, store_and_retrieve_suppress_type(pool, "leading", XML4, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_leading5(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_LEADING_XML5, store_and_retrieve_suppress_type(pool, "leading", XML5, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_leading6(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_LEADING_XML6, store_and_retrieve_suppress_type(pool, "leading", XML6, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_leading7(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_LEADING_XML7, store_and_retrieve_suppress_type(pool, "leading", XML7, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_trailing1(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_TRAILING_XML1, store_and_retrieve_suppress_type(pool, "trailing", XML1, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_trailing2(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_TRAILING_XML2, store_and_retrieve_suppress_type(pool, "trailing", XML2, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_trailing3(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_TRAILING_XML3, store_and_retrieve_suppress_type(pool, "trailing", XML3, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_trailing4(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_TRAILING_XML4, store_and_retrieve_suppress_type(pool, "trailing", XML4, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_trailing5(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_TRAILING_XML5, store_and_retrieve_suppress_type(pool, "trailing", XML5, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_trailing6(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_TRAILING_XML6, store_and_retrieve_suppress_type(pool, "trailing", XML6, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_trailing7(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_TRAILING_XML7, store_and_retrieve_suppress_type(pool, "trailing", XML7, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_both1(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_BOTH_XML1, store_and_retrieve_suppress_type(pool, "both", XML1, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_both2(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_BOTH_XML2, store_and_retrieve_suppress_type(pool, "both", XML2, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_both3(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_BOTH_XML3, store_and_retrieve_suppress_type(pool, "both", XML3, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_both4(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_BOTH_XML4, store_and_retrieve_suppress_type(pool, "both", XML4, XQUERY));
    }

    @Disabled
    @Test
    void retrieve_suppress_ws_both5(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_BOTH_XML5, store_and_retrieve_suppress_type(pool, "both", XML5, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_both6(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_BOTH_XML6, store_and_retrieve_suppress_type(pool, "both", XML6, XQUERY));
    }

    @Test
    void retrieve_suppress_ws_both7(final BrokerPool pool) throws LockException, AuthenticationException, XPathException, PermissionDeniedException, EXistException, IOException, SAXException {
        assertEquals(RESULT_SUPPRESS_WS_BOTH_XML7, store_and_retrieve_suppress_type(pool, "both", XML7, XQUERY));
    }
}
