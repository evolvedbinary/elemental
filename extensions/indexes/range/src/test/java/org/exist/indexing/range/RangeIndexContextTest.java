/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.indexing.range;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.EXistException;
import org.exist.collections.Collection;
import org.exist.collections.CollectionConfiguration;
import org.exist.dom.persistent.NodeProxy;
import org.exist.security.PermissionDeniedException;
import org.exist.source.Source;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.XQueryPool;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.LockException;
import org.exist.util.MimeType;
import org.exist.util.StringInputSource;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.CompiledXQuery;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.array.ArrayType;
import org.exist.xquery.value.Item;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.util.MapUtil.HashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for checking that the `range:context` XQuery function returns the correct results.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class RangeIndexContextTest {

    private static final XmldbURI TEST_COLLECTION = XmldbURI.create("/db/range-index-context-test");
    private static final XmldbURI TEST_CONFIG_COLLECTION = XmldbURI.CONFIG_COLLECTION_URI.append(TEST_COLLECTION);

    @ClassRule
    public static final ExistEmbeddedServer EXIST_EMBEDDED_SERVER = new ExistEmbeddedServer(true, true);

    private static final String COLLECTION_XCONF_SMALL_CONTEXT =
            "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">\n" +
            "    <index xmlns:c=\"http://corpus\">\n" +
            "        <range>\n" +
            "            <context id=\"word-context\" qname=\"c:w\" pre-context-size=\"4\" post-context-size=\"4\"/>\n" +
            "            <create qname=\"@lemma\" type=\"xs:string\">\n" +
            "                <context ref=\"word-context\" name=\"lemma-context\"/>\n" +
            "            </create>\n" +
            "        </range>\n" +
            "    </index>\n" +
            "</collection>";

    private static final String COLLECTION_XCONF_BIG_CONTEXT =
            "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">\n" +
            "    <index xmlns:c=\"http://corpus\">\n" +
            "        <range>\n" +
            "            <context id=\"word-context\" qname=\"c:w\" pre-context-size=\"9\" post-context-size=\"9\"/>\n" +
            "            <create qname=\"@lemma\" type=\"xs:string\">\n" +
            "                <context ref=\"word-context\" name=\"lemma-context\"/>\n" +
            "            </create>\n" +
            "        </range>\n" +
            "    </index>\n" +
            "</collection>";

    private static final String COLLECTION_XCONF_MIXED_PRE_POST_CONTEXT =
            "<collection xmlns=\"http://exist-db.org/collection-config/1.0\">\n" +
            "    <index xmlns:c=\"http://corpus\">\n" +
            "        <range>\n" +
            "            <context id=\"word-context\" qname=\"c:w\" pre-context-size=\"5\" post-context-size=\"2\"/>\n" +
            "            <create qname=\"@lemma\" type=\"xs:string\">\n" +
            "                <context ref=\"word-context\" name=\"lemma-context\"/>\n" +
            "            </create>\n" +
            "        </range>\n" +
            "    </index>\n" +
            "</collection>";

    private static final XmldbURI EXAMPLE_DOCUMENT_1_NAME = XmldbURI.create("test-corpus-1.xml");

    private static final String EXAMPLE_DOCUMENT_1 =
            "<text xmlns=\"http://corpus\">\n" +
            "    <p>\n" +
            "        <pc join=\"right\">’</pc>\n" +
            "        <w id=\"1\" lemma=\"S\">S</w>\n" +
            "        <w id=\"2\" lemma=\"ann\">ann</w>\n" +
            "        <w id=\"3\" lemma=\"a\">a</w>\n" +
            "        <w id=\"4\" lemma=\"b\">b</w>\n" +
            "        <pc join=\"left\">’</pc>\n" +
            "        <w id=\"5\" lemma=\"àbhaist\">àbhaist</w>\n" +
            "        <w id=\"6\" lemma=\"gu\">gu</w>\n" +
            "        <pc join=\"right\">’</pc>\n" +
            "        <w id=\"7\" lemma=\"n\">n</w>\n" +
            "        <w id=\"8\" lemma=\"àireamh\">àireamh</w>\n" +
            "        <lb/>\n" +
            "        <w id=\"9\" lemma=\"Bhi\">Bhi</w>\n" +
            "        <pc join=\"right\">’</pc>\n" +
            "        <w id=\"10\" lemma=\"m\">m</w>\n" +
            "        <w id=\"11\" lemma=\"fàbhar\">fàbhar</w>\n" +
            "        <w id=\"12\" lemma=\"Shiol\">Shiol</w>\n" +
            "        <w id=\"13\" lemma=\"Chuinn\">Chuinn</w>\n" +
            "        <pc join=\"left\">.</pc>\n" +
            "    </p>\n" +
            "    <lg>\n" +
            "        <l>\n" +
            "            <w id=\"14\" lemma=\"Na\">Na</w>\n" +
            "            <w id=\"15\" lemma=\"Leodaich\">Leodaich</w>\n" +
            "            <w id=\"16\" lemma=\"am\">am</w>\n" +
            "            <w id=\"17\" lemma=\"por\">por</w>\n" +
            "            <w id=\"18\" lemma=\"glan\">glan</w>\n" +
            "            <t resp=\"osz\">The MacLeods the pure seed</t>\n" +
            "        </l>\n" +
            "        <l>\n" +
            "            <w id=\"19\" lemma=\"Cha\">Cha</w>\n" +
            "            <w id=\"20\" lemma=\"b\">b</w>\n" +
            "            <pc join=\"left\">’</pc>\n" +
            "            <w id=\"21\" lemma=\"fholach\">fholach</w>\n" +
            "            <pc join=\"right\">’</pc>\n" +
            "            <w id=\"22\" lemma=\"ur\">ur</w>\n" +
            "            <w id=\"23\" lemma=\"siol\">siol</w>\n" +
            "            <pc join=\"left\">,</pc>\n" +
            "            <t resp=\"osz\">Not hidden your lineage</t>\n" +
            "        </l>\n" +
            "    </lg>\n" +
            "</text>";

    private static final XmldbURI EXAMPLE_DOCUMENT_2_NAME = XmldbURI.create("test-corpus-2.xml");

    private static final String EXAMPLE_DOCUMENT_2 =
        "<text xmlns=\"http://corpus\">\n" +
        "  <p>\n" +
        "    <w wid=\"_64_d2e83514\" id=\"d1e171\" pos=\"a\" lemma=\"àrd\">àrd</w>\n" +
        "    <w wid=\"_64_d2e83517\" id=\"d1e174\" pos=\"n\" lemma=\"caisteal\">Caisteal</w>\n" +
        "    <w wid=\"_64_d2e83520\" id=\"d1e177\" pos=\"a\" lemma=\"mòr\">mór</w>\n" +
        "    <w wid=\"_64_d2e83523\" id=\"d1e180\" pos=\"V\" lemma=\"dùin\">Dhùin</w>\n" +
        "    <pc join=\"both\">-</pc>\n" +
        "    <w wid=\"_64_d2e83530\" id=\"d1e187\" pos=\"n\" lemma=\"àluinn\">àluinn</w>\n" +
        "    <pc join=\"left\">.</pc>\n" +
        "  </p>\n" +
        "  <p>\n" +
        "    <pc join=\"right\">“</pc>\n" +
        "    <w wid=\"_64_d2e83543\" id=\"d1e200\" pos=\"\" lemma=\"rx\">A</w>\n" +
        "    <w wid=\"_64_d2e83546\" id=\"d1e203\" pos=\"n\" lemma=\"Chailein\">Chailein</w>\n" +
        "    <pc join=\"left\">,</pc>\n" +
        "    <w wid=\"_64_d2e83552\" id=\"d1e209\" pos=\"\" lemma=\"rx\">a</w>\n" +
        "    <w wid=\"_64_d2e83556\" id=\"d1e213\" pos=\"n\" lemma=\"Chailein\">Chailein</w>\n" +
        "    <pc join=\"left\">!</pc>\n" +
        "    <w wid=\"_64_d2e83562\" id=\"d1e219\" pos=\"V\" lemma=\"Tha\">Tha</w>\n" +
        "    <w wid=\"_64_d2e83565\" id=\"d1e222\" pos=\"D\" lemma=\"mi\">mise</w>\n" +
        "    <pc join=\"right\">’</pc>\n" +
        "  </p>\n" +
        "</text>";

    @Test
    public void smallContext() throws LockException, PermissionDeniedException, EXistException, IOException, SAXException, XPathException {
        configureIndexAndStoreExamples(COLLECTION_XCONF_SMALL_CONTEXT);

//        final String lemma = "S";
        final String lemma = "Shiol";
        final int preContextSize = 4;
        final int postContextSize = 4;

        final NodeProxy lemmaAttr = findLemma(lemma);
        final Tuple2<Sequence, Sequence> contextResult = contextLookup(lemmaAttr, preContextSize, postContextSize);

        // check pre-context results
        final Sequence preContextResult = contextResult._1;
        assertNotNull(preContextResult);
        assertEquals(preContextSize, preContextResult.getItemCount());

        Element result = assertItemIsElement(preContextResult.itemAt(0));
        assertHasId(9, result);
        assertWord("Bhi", result);

        result = assertItemIsElement(preContextResult.itemAt(1));
        assertHasId(10, result);
        assertWord("m", result);

        result = assertItemIsElement(preContextResult.itemAt(2));
        assertHasId(11, result);
        assertWord("fàbhar", result);

        // NOTE(AR) the pre-context will include the `w` (element) that has the lemma (attribute) attached to it as it occurs before the lemma
        result = assertItemIsElement(preContextResult.itemAt(3));
        assertHasId(12, result);
        assertWord("Shiol", result);

        // check post-context results
        final Sequence postContextResult = contextResult._2;
        assertNotNull(postContextResult);
        assertEquals(postContextSize, postContextResult.getItemCount());

        result = assertItemIsElement(postContextResult.itemAt(0));
        assertHasId(13, result);
        assertWord("Chuinn", result);

        result = assertItemIsElement(postContextResult.itemAt(1));
        assertHasId(14, result);
        assertWord("Na", result);

        result = assertItemIsElement(postContextResult.itemAt(2));
        assertHasId(15, result);
        assertWord("Leodaich", result);

        result = assertItemIsElement(postContextResult.itemAt(3));
        assertHasId(16, result);
        assertWord("am", result);
    }

    private static Element assertItemIsElement(final Item item) {
        assertTrue(item instanceof NodeProxy);
        final Node node = ((NodeProxy) item).getNode();
        assertTrue(node instanceof Element);
        return (Element) node;
    }

    private static void assertHasId(final int expectedId, final Element element) {
        assertEquals(expectedId, Integer.valueOf(element.getAttribute("id")).intValue());
    }

    private static void assertWord(final String expectedText, final Element element) {
        assertEquals("w", element.getLocalName());
        assertEquals(expectedText, element.getTextContent());
    }

    private static Tuple2<Sequence, Sequence> contextLookup(final NodeProxy lemmaAttr, final int preContextSize, final int postContextSize) throws XPathException, PermissionDeniedException, EXistException, IOException {
        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final String xquery = "declare variable $lemma-attr as attribute(lemma) external;\n" +
                    "range:context($lemma-attr, 'lemma-context', " + preContextSize + ", " + postContextSize + ")";
            final Sequence result = executeXQuery(broker, xquery, HashMap(Tuple("lemma-attr", lemmaAttr)));
            assertEquals(1, result.getItemCount());

            final Item firstItem = result.itemAt(0);
            assertEquals(Type.ARRAY, firstItem.getType());
            assertTrue(firstItem instanceof ArrayType);
            final Sequence[] prePostContextResult = firstItem.toJavaObject(Sequence[].class);

            transaction.commit();

            return Tuple(prePostContextResult[0], prePostContextResult[1]);
        }
    }

    private static Sequence executeXQuery(final DBBroker broker,final String query, @Nullable final Map<String, Item> variables) throws EXistException, PermissionDeniedException, XPathException, IOException {
        final Source source = new StringSource(query);
        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        final XQueryPool pool = brokerPool.getXQueryPool();
        final XQuery xquery = brokerPool.getXQueryService();

        final CompiledXQuery existingCompiled = pool.borrowCompiledXQuery(broker, source);

        final XQueryContext context;
        final CompiledXQuery compiled;
        if (existingCompiled == null) {
            context = new XQueryContext(brokerPool);
            compiled = xquery.compile(context, source);
        } else {
            context = existingCompiled.getContext();
            context.prepareForReuse();
            compiled = existingCompiled;
        }

        // declare any variables
        if (variables != null) {
            for (final Map.Entry<String, Item> variable : variables.entrySet()) {
                context.declareVariable(variable.getKey(), variable.getValue());
            }
        }

        return xquery.execute(broker, compiled, null);
    }

    private static NodeProxy findLemma(final String lemma) throws XPathException, PermissionDeniedException, EXistException {
        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
             final Txn transaction = brokerPool.getTransactionManager().beginTransaction()) {

            final XQuery xqueryService = brokerPool.getXQueryService();

            final Sequence result = xqueryService.execute(broker, "//@lemma[. eq '" + lemma + "']", null);
            assertEquals(1, result.getItemCount());

            final Item firstItem = result.itemAt(0);
            assertEquals(Type.ATTRIBUTE, firstItem.getType());
            assertTrue(firstItem instanceof NodeProxy);

            transaction.commit();

            return (NodeProxy) firstItem;
        }
    }

    private static void configureIndexAndStoreExamples(final String collectionXConf) throws EXistException, PermissionDeniedException, IOException, SAXException, LockException {
        final BrokerPool brokerPool = EXIST_EMBEDDED_SERVER.getBrokerPool();
        try (final DBBroker broker = brokerPool.get(Optional.of(brokerPool.getSecurityManager().getSystemSubject()));
                final Txn transaction = brokerPool.getTransactionManager().beginTransaction();
                final Collection configCollection = broker.getOrCreateCollection(transaction, TEST_CONFIG_COLLECTION);
                final Collection collection = broker.getOrCreateCollection(transaction, TEST_COLLECTION)) {

            // store collection config
            broker.storeDocument(transaction, CollectionConfiguration.DEFAULT_COLLECTION_CONFIG_FILE_URI, new StringInputSource(collectionXConf), MimeType.XML_TYPE, configCollection);

            // store example document 1
            broker.storeDocument(transaction, EXAMPLE_DOCUMENT_1_NAME, new StringInputSource(EXAMPLE_DOCUMENT_1), MimeType.XML_TYPE, collection);

            // store example document 2
            broker.storeDocument(transaction, EXAMPLE_DOCUMENT_2_NAME, new StringInputSource(EXAMPLE_DOCUMENT_2), MimeType.XML_TYPE, collection);

            transaction.commit();
        }
    }

}
