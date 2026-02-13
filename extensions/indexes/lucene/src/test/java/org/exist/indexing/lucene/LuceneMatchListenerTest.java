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
package org.exist.indexing.lucene;

import org.exist.EXistException;
import org.exist.TestUtils;
import org.exist.collections.Collection;
import org.exist.collections.CollectionConfigurationException;
import org.exist.collections.CollectionConfigurationManager;
import org.exist.collections.triggers.TriggerException;
import org.exist.security.PermissionDeniedException;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.serializers.EXistOutputKeys;
import org.exist.storage.serializers.Serializer;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.test.TestConstants;
import org.exist.util.*;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.NodeValue;
import org.exist.xquery.value.Sequence;
import org.junit.AfterClass;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xmlunit.matchers.CompareMatcher;
import xyz.elemental.mediatype.MediaType;

import javax.xml.transform.OutputKeys;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class LuceneMatchListenerTest {

    private static String XML =
            "<root>" +
            "   <para>some paragraph with <hi>mixed</hi> content.</para>" +
            "   <para>another paragraph with <note><hi>nested</hi> inner</note> elements.</para>" +
            "   <para>a third paragraph with <term>term</term>.</para>" +
            "   <para>double match double match</para>" +
            "</root>";

    private static String XML1 =
            "<article>" +
            "   <head>The <b>title</b>of it</head>" +
            "   <p>A simple<note>sic</note> paragraph with <hi>highlighted</hi> text <note>and a note</note> to be ignored.</p>" +
            "   <p>Paragraphs with <s>mix</s><s>ed</s> content are <s>danger</s>ous.</p>" +
            "</article>";

    private static String XML2 =
            "<p xmlns=\"http://www.tei-c.org/ns/1.0\">\n" +
            "    <s type=\"combo\"><w lemma=\"из\">из</w>\n" +
            "        <w>новина</w>\n" +
            "        <w lemma=\"и\">и</w>\n" +
            "        <w lemma=\"од\">од</w>\n" +
            "        <lb/>\n" +
            "        <pb n=\"32\"/>\n" +
            "        <w>других</w>\n" +
            "        <w lemma=\"човек\">људи</w>\n" +
            "        <w>дознајем</w>, <w xml:id=\"VSK.P13.t1.p4.w205\" lemma=\"ма\">ма</w>\n" +
            "        <w>се</w>\n" +
            "        <w lemma=\"не\">не</w>\n" +
            "        <w>прорезује</w>\n" +
            "        <w>право</w>\n" +
            "        <w lemma=\"по\">по</w>\n" +
            "        <w>имућству</w>, <w xml:id=\"VSK.P13.t1.p4.w219\" lemma=\"те\">те</w>\n" +
            "        <w>се</w>\n" +
            "        <w>на</w>\n" +
            "        <w lemma=\"то\">то</w>\n" +
            "        <w>видим</w>\n" +
            "        <w>многи</w>\n" +
            "        <w>љуте</w>.</s>\n" +
            "</p>";

    private static String CONF1 =
        "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">" +
        "   <index>" +
        "       <text qname=\"para\"/>" +
        "   </index>" +
        "</collection>";

    private static String CONF2 =
        "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">" +
        "   <index>" +
        "       <text qname=\"para\"/>" +
        "       <text qname=\"term\"/>" +
        "   </index>" +
        "</collection>";

    private static String CONF3 =
        "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">" +
        "   <index>" +
        "       <text qname=\"hi\"/>" +
        "   </index>" +
        "</collection>";

    private static String CONF4 =
        "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">" +
        "   <index xmlns:tei=\"http://www.tei-c.org/ns/1.0\">" +
        "       <lucene>" +
        "           <text qname=\"p\">" +
        "               <ignore qname=\"note\"/>" +
        "           </text>" +
        "           <text qname=\"head\"/>" +
        "           <inline qname=\"s\"/>" +
        "       </lucene>" +
        "   </index>" +
        "</collection>";


    private static String CONF5 =
            "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">\n" +
            "    <index xmlns:tei=\"http://www.tei-c.org/ns/1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" +
            "        <lucene>" +
            "            <text qname=\"tei:p\"/>" +
            "            <text qname=\"tei:w\"/>" +
            "            <text qname=\"@lemma\"/>" +
            "        </lucene>" +
            "    </index>" +
            "</collection>";

    private static String MATCH_START = "<exist:match xmlns:exist=\"http://exist.sourceforge.net/NS/exist\">";
    private static String MATCH_END = "</exist:match>";

    private static final Map<String, String> NS_CONTEXT = MapUtil.hashMap(
        Tuple("tei", "http://www.tei-c.org/ns/1.0"),
        Tuple("exist", "http://exist.sourceforge.net/NS/exist")
    );

    /**
     * Test match highlighting for index configured by QName, e.g.
     * &lt;create qname="a"/&gt;.
     */
    @Test
    public void indexByQName() throws EXistException, PermissionDeniedException, XPathException, SAXException, CollectionConfigurationException, LockException, IOException {

        configureAndStore(CONF2, XML);

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {

            String query = "//para[ft:query(., 'mixed')]";
            Sequence seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            String result = queryResult2String(broker, seq);
            assertThat(result, CompareMatcher.isIdenticalTo("<para>some paragraph with <hi>" + MATCH_START + "mixed" + MATCH_END + "</hi> content.</para>"));

            query = "//para[ft:query(., '+nested +inner +elements')]";
            seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            result = queryResult2String(broker, seq);
            assertThat(result, CompareMatcher.isIdenticalTo("<para>another paragraph with <note><hi>" + MATCH_START + "nested" + MATCH_END + "</hi> " + MATCH_START + "inner" + MATCH_END + "</note> " + MATCH_START + "elements" + MATCH_END + ".</para>"));

            query = "//para[ft:query(term, 'term')]";
            seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            result = queryResult2String(broker, seq);
            assertThat(result, CompareMatcher.isIdenticalTo("<para>a third paragraph with <term>" + MATCH_START + "term" + MATCH_END + "</term>.</para>"));

            query = "//para[ft:query(., '+double +match')]";
            seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            result = queryResult2String(broker, seq);
            assertThat(result, CompareMatcher.isIdenticalTo("<para>" + MATCH_START + "double" + MATCH_END + " " + MATCH_START + "match" + MATCH_END + " " + MATCH_START + "double" + MATCH_END + " " + MATCH_START + "match" + MATCH_END + "</para>"));

            query = "for $para in //para[ft:query(., '+double +match')] return <hit>{$para}</hit>";
            seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            result = queryResult2String(broker, seq);
            assertThat(result, CompareMatcher.isIdenticalTo("<hit><para>" + MATCH_START + "double" + MATCH_END + " " + MATCH_START + "match" + MATCH_END + " " + MATCH_START + "double" + MATCH_END + " " + MATCH_START + "match" + MATCH_END + "</para></hit>"));
        }
    }

    @Test
    public void matchInAncestor() throws EXistException, PermissionDeniedException, XPathException, SAXException, IOException, LockException, CollectionConfigurationException {
        configureAndStore(CONF1, XML);
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            String query = "//para[ft:query(., 'mixed')]/hi";
            Sequence seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            String result = queryResult2String(broker, seq);
            assertThat(result, hasXPath("count(//exist:match)", equalTo("1")).withNamespaceContext(NS_CONTEXT));

            query = "//para[ft:query(., 'nested')]/note";
            seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            result = queryResult2String(broker, seq);
            assertThat(result, hasXPath("count(//hi/exist:match)", equalTo("1")).withNamespaceContext(NS_CONTEXT));
        }
    }

    @Test
    public void matchInDescendant() throws EXistException, PermissionDeniedException, XPathException, SAXException, IOException, LockException, CollectionConfigurationException {
        configureAndStore(CONF3, XML);
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            String query = "//hi[ft:query(., 'mixed')]/ancestor::para";
            Sequence seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            String result = queryResult2String(broker, seq);
            assertThat(result, hasXPath("count(//exist:match)", equalTo("1")).withNamespaceContext(NS_CONTEXT));

            query = "//hi[ft:query(., 'nested')]/parent::note";
            seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            result = queryResult2String(broker, seq);
            assertThat(result, hasXPath("count(//hi/exist:match)", equalTo("1")).withNamespaceContext(NS_CONTEXT));
        }
    }

    @Test
    public void inlineNodes_whenNotIndenting() throws EXistException, PermissionDeniedException, XPathException, SAXException, CollectionConfigurationException, LockException, IOException {
        configureAndStore(CONF4, XML1);

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            String query = "//p[ft:query(., 'mixed')]";
            Sequence seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            String result = queryResult2String(broker, seq);
            assertThat(result, CompareMatcher.isIdenticalTo("<p>Paragraphs with <s>" + MATCH_START + "mix" + MATCH_END + "</s><s>ed</s> content are <s>danger</s>ous.</p>"));

            query = "//p[ft:query(., 'ignored')]";
            seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            result = queryResult2String(broker, seq);
            assertThat(result, CompareMatcher.isIdenticalTo("<p>A simple<note>sic</note> paragraph with <hi>highlighted</hi> text <note>and a note</note> to be " + MATCH_START + "ignored" + MATCH_END + ".</p>"));

            query = "//p[ft:query(., 'highlighted')]";
            seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            result = queryResult2String(broker, seq);
            assertThat(result, CompareMatcher.isIdenticalTo("<p>A simple<note>sic</note> paragraph with <hi>" + MATCH_START + "highlighted" + MATCH_END + "</hi> text <note>and a note</note> to be " + "ignored.</p>"));

            query = "//p[ft:query(., 'highlighted')]/hi";
            seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            result = queryResult2String(broker, seq);
            assertThat(result, CompareMatcher.isIdenticalTo("<hi>" + MATCH_START + "highlighted" + MATCH_END + "</hi>"));
            
            query = "//head[ft:query(., 'title')]";
            seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            result = queryResult2String(broker, seq);
            assertThat(result, CompareMatcher.isIdenticalTo("<head>The <b>" + MATCH_START + "title" + MATCH_END + "</b>of it</head>"));
        }
    }

    @Test
    public void inlineMatchNodes_whenIndenting() throws EXistException, PermissionDeniedException, XPathException, SAXException, CollectionConfigurationException, LockException, IOException {
        configureAndStore(CONF5, XML2);

        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        try (final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            final String query = "declare namespace tei=\"http://www.tei-c.org/ns/1.0\";" +
                    "//tei:p[.//tei:w[ft:query(., <query><bool><term>дознајем</term></bool></query>)]] ! util:expand(.)";
            final Sequence seq = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null).result;
            assertNotNull(seq);
            assertEquals(1, seq.getItemCount());
            final String result = queryResult2String(broker, seq, true);

            final String expected =
            "<p xmlns=\"http://www.tei-c.org/ns/1.0\">\n" +
            "    <s type=\"combo\">\n" +
            "        <w lemma=\"из\">из</w>\n" +
            "        <w>новина</w>\n" +
            "        <w lemma=\"и\">и</w>\n" +
            "        <w lemma=\"од\">од</w>\n" +
            "        <lb/>\n" +
            "        <pb n=\"32\"/>\n" +
            "        <w>других</w>\n" +
            "        <w lemma=\"човек\">људи</w>\n" +
            "        <w>" + MATCH_START + "дознајем" + MATCH_END + "</w>, <w xml:id=\"VSK.P13.t1.p4.w205\" lemma=\"ма\">ма</w>\n" +
            "        <w>се</w>\n" +
            "        <w lemma=\"не\">не</w>\n" +
            "        <w>прорезује</w>\n" +
            "        <w>право</w>\n" +
            "        <w lemma=\"по\">по</w>\n" +
            "        <w>имућству</w>, <w xml:id=\"VSK.P13.t1.p4.w219\" lemma=\"те\">те</w>\n" +
            "        <w>се</w>\n" +
            "        <w>на</w>\n" +
            "        <w lemma=\"то\">то</w>\n" +
            "        <w>видим</w>\n" +
            "        <w>многи</w>\n" +
            "        <w>љуте</w>.</s>\n" +
            "</p>";

            assertThat(result, CompareMatcher.isIdenticalTo(expected));
        }
    }

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(true, true);

    @BeforeClass
    public static void startDB() throws EXistException, PermissionDeniedException, IOException, TriggerException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = transact.beginTransaction()) {

            final Collection root = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI);
            assertNotNull(root);
            broker.saveCollection(transaction, root);

            transact.commit(transaction);
        }
    }

    @AfterClass
    public static void closeDB() throws LockException, TriggerException, PermissionDeniedException, EXistException, IOException {
        TestUtils.cleanupDB();
    }

    private void configureAndStore(final String config, final String data) throws EXistException, PermissionDeniedException, IOException, SAXException, CollectionConfigurationException, LockException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();
        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()));
            final Txn transaction = transact.beginTransaction()) {

            final Collection root = broker.getOrCreateCollection(transaction, TestConstants.TEST_COLLECTION_URI);
            assertNotNull(root);
            final CollectionConfigurationManager mgr = pool.getConfigurationManager();
            mgr.addConfiguration(transaction, broker, root, config);

            final MediaType xmlMediaType = pool.getMediaTypeService().getMediaTypeResolver().fromString(MediaType.APPLICATION_XML);
            broker.storeDocument(transaction, XmldbURI.create("test_matches.xml"), new StringInputSource(data), xmlMediaType, root);

            transact.commit(transaction);
        }
    }

    private String queryResult2String(final DBBroker broker, final Sequence seq) throws SAXException, XPathException {
        return queryResult2String(broker, seq, false);
    }

    private String queryResult2String(final DBBroker broker, final Sequence seq, final boolean indent) throws SAXException, XPathException {
        final Properties props = new Properties();
        props.setProperty(OutputKeys.INDENT, indent ? "yes" : "no");
        props.setProperty(EXistOutputKeys.HIGHLIGHT_MATCHES, "elements");
        final Serializer serializer = broker.borrowSerializer();
        try {
            serializer.setProperties(props);
            return serializer.serialize((NodeValue) seq.itemAt(0));
        } finally {
            broker.returnSerializer(serializer);
        }
    }
}
