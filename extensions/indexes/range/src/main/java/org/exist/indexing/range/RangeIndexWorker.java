/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 *
 * NOTE: Parts of this file contain code from The eXist-db Authors.
 *       The original license header is included below.
 *
 * ----------------------------------------------------------------------------
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
package org.exist.indexing.range;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.lucene.util.BytesRefBuilder;
import org.exist.dom.persistent.*;
import org.exist.dom.QName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.exist.collections.Collection;
import org.exist.indexing.*;
import org.exist.indexing.StreamListener.ReindexMode;
import org.exist.indexing.lucene.BinaryTokenStream;
import org.exist.indexing.lucene.LuceneIndexWorker;
import org.exist.indexing.lucene.LuceneUtil;
import org.exist.numbering.NodeId;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;
import org.exist.storage.ElementValue;
import org.exist.storage.IndexSpec;
import org.exist.storage.NodePath;
import org.exist.storage.NodePath2;
import org.exist.storage.btree.DBException;
import org.exist.storage.dom.INodeIterator;
import org.exist.storage.txn.Txn;
import org.exist.util.ByteConversion;
import org.exist.util.DatabaseConfigurationException;
import org.exist.util.LockException;
import org.exist.util.Occurrences;
import org.exist.xquery.*;
import org.exist.xquery.modules.range.RangeQueryRewriter;
import org.exist.xquery.value.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.xquery.modules.range.ContextLookup.INDEX_DEF_CONTEXT_SIZE;

/**
 * The main worker class for the range index.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author Wolfgang Meier
 */
public class RangeIndexWorker implements OrderedValuesIndex, QNamedKeysIndex {

    private static final Logger LOG = LogManager.getLogger(RangeIndexWorker.class);

    public static final String FIELD_NODE_ID = "nodeId";
    public static final String FIELD_DOC_ID = "docId";
    public static final String FIELD_ADDRESS = "address";
    public static final String FIELD_ID = "id";

    private static final String PRE_CONTEXT_FIELD_PREFIX = "pre_";
    private static final String POST_CONTEXT_FIELD_PREFIX = "post_";

    private static final Set<String> LOAD_FIELDS = new TreeSet<>();
    static {
        LOAD_FIELDS.add(FIELD_DOC_ID);
        LOAD_FIELDS.add(FIELD_NODE_ID);
    }

    private final RangeIndex index;
    private final DBBroker broker;
    private IndexController controller;
    private DocumentImpl currentDoc;
    private ReindexMode mode = ReindexMode.STORE;
    private @Nullable List<Tuple2<RangeIndexDoc, Integer>> nodesAwaitingPostContextCompletion;
    private List<RangeIndexDoc> nodesToWrite;
    private @Nullable Set<NodeId> nodesToRemove = null;
    private @Nullable RangeIndexConfig config = null;
    private final RangeIndexListener listener = new RangeIndexListener();
    private @Nullable Deque<TextCollector> contentStack = null;
    private @Nullable Map<String, ContextTextCollector> contextCollectors = null;
    private int cachedNodesToWriteSize = 0;

    private static final int MAX_CACHED_NODES_SIZE = 4096 * 1024;
    private static final int MAX_CACHED_NODES_AWAITING_COMPLETION_SIZE = 20;

    public RangeIndexWorker(RangeIndex index, DBBroker broker) {
        this.index = index;
        this.broker = broker;
    }

    public Query toQuery(String field, QName qname, AtomicValue content, RangeIndex.Operator operator, DocumentSet docs) throws XPathException {
        final int type = content.getType();
        BytesRefBuilder bytes;
        BytesRef key = null;
        if (Type.subTypeOf(type, Type.STRING)) {
            if (operator != RangeIndex.Operator.MATCH) {
                key = analyzeContent(field, qname, content.getStringValue(), docs);
            }
            WildcardQuery query;
            switch (operator) {
                case EQ:
                    return new TermQuery(new Term(field, key));
                case NE:
                    final BooleanQuery qnot = new BooleanQuery();
                    query = new WildcardQuery(new Term(field, new BytesRef("*")));
                    query.setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE);
                    qnot.add(query, BooleanClause.Occur.MUST);
                    qnot.add(new TermQuery(new Term(field, key)), BooleanClause.Occur.MUST_NOT);
                    return qnot;
                case STARTS_WITH:
                    return new PrefixQuery(new Term(field, key));
                case ENDS_WITH:
                    bytes = new BytesRefBuilder();
                    bytes.append((byte)'*');
                    bytes.append(key);
                    query = new WildcardQuery(new Term(field, bytes.toBytesRef()));
                    query.setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE);
                    return query;
                case CONTAINS:
                    bytes = new BytesRefBuilder();
                    bytes.append((byte)'*');
                    bytes.append(key);
                    bytes.append((byte)'*');
                    query = new WildcardQuery(new Term(field, bytes.toBytesRef()));
                    query.setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE);
                    return query;
                case MATCH:
                    RegexpQuery regexQuery = new RegexpQuery(new Term(field, content.getStringValue()));
                    regexQuery.setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_FILTER_REWRITE);
                    return regexQuery;
            }
        }
        if (operator == RangeIndex.Operator.EQ) {
            return new TermQuery(new Term(field, BasicRangeIndexConfigElement.convertToBytes(content)));
        }
        if (operator == RangeIndex.Operator.NE) {
            final BooleanQuery nq = new BooleanQuery();
            nq.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
            nq.add(new TermQuery(new Term(field, BasicRangeIndexConfigElement.convertToBytes(content))), BooleanClause.Occur.MUST_NOT);
            return nq;
        }
        final boolean includeUpper = operator == RangeIndex.Operator.LE;
        final boolean includeLower = operator == RangeIndex.Operator.GE;
        switch (type) {
            case Type.INTEGER:
            case Type.LONG:
            case Type.UNSIGNED_LONG:
                if (operator == RangeIndex.Operator.LT || operator == RangeIndex.Operator.LE) {
                    return NumericRangeQuery.newLongRange(field, null, ((NumericValue)content).getLong(), includeLower, includeUpper);
                } else {
                    return NumericRangeQuery.newLongRange(field, ((NumericValue)content).getLong(), null, includeLower, includeUpper);
                }
            case Type.INT:
            case Type.UNSIGNED_INT:
            case Type.SHORT:
            case Type.UNSIGNED_SHORT:
                if (operator == RangeIndex.Operator.LT || operator == RangeIndex.Operator.LE) {
                    return NumericRangeQuery.newIntRange(field, null, ((NumericValue) content).getInt(), includeLower, includeUpper);
                } else {
                    return NumericRangeQuery.newIntRange(field, ((NumericValue) content).getInt(), null, includeLower, includeUpper);
                }
            case Type.DECIMAL:
            case Type.DOUBLE:
                if (operator == RangeIndex.Operator.LT || operator == RangeIndex.Operator.LE) {
                    return NumericRangeQuery.newDoubleRange(field, null, ((NumericValue) content).getDouble(), includeLower, includeUpper);
                } else {
                    return NumericRangeQuery.newDoubleRange(field, ((NumericValue) content).getDouble(), null, includeLower, includeUpper);
                }
            case Type.FLOAT:
                if (operator == RangeIndex.Operator.LT || operator == RangeIndex.Operator.LE) {
                    return NumericRangeQuery.newFloatRange(field, null, (float) ((NumericValue) content).getDouble(), includeLower, includeUpper);
                } else {
                    return NumericRangeQuery.newFloatRange(field, (float) ((NumericValue) content).getDouble(), null, includeLower, includeUpper);
                }
            case Type.DATE:
                long dl = BasicRangeIndexConfigElement.dateToLong((DateValue) content);
                if (operator == RangeIndex.Operator.LT || operator == RangeIndex.Operator.LE) {
                    return NumericRangeQuery.newLongRange(field, null, dl, includeLower, includeUpper);
                } else {
                    return NumericRangeQuery.newLongRange(field, dl, null, includeLower, includeUpper);
                }
            case Type.TIME:
                long tl = BasicRangeIndexConfigElement.timeToLong((TimeValue) content);
                if (operator == RangeIndex.Operator.LT || operator == RangeIndex.Operator.LE) {
                    return NumericRangeQuery.newLongRange(field, null, tl, includeLower, includeUpper);
                } else {
                    return NumericRangeQuery.newLongRange(field, tl, null, includeLower, includeUpper);
                }
            case Type.DATE_TIME:
            default:
                if (type == Type.DATE_TIME) {
                    key = BasicRangeIndexConfigElement.convertToBytes(content);
                }
                if (operator == RangeIndex.Operator.LT || operator == RangeIndex.Operator.LE) {
                    return new TermRangeQuery(field, null, key, includeLower, includeUpper);
                } else {
                    return new TermRangeQuery(field, key, null, includeLower, includeUpper);
                }
        }
    }

    @Override
    public String getIndexId() {
        return index.getIndexId();
    }

    @Override
    public String getIndexName() {
        return index.getIndexName();
    }

    @Override
    public Object configure(IndexController controller, NodeList configNodes, Map<String, String> namespaces) throws DatabaseConfigurationException {
        this.controller = controller;
        LOG.debug("Configuring lucene index...");
        return new RangeIndexConfig(configNodes, namespaces);
    }

    @Override
    public void setDocument(DocumentImpl document) {
        setDocument(document, ReindexMode.UNKNOWN);
    }

    @Override
    public void setDocument(DocumentImpl document, ReindexMode mode) {
        this.currentDoc = document;
        IndexSpec indexConf = document.getCollection().getIndexConfiguration(broker);
        if (indexConf != null) {
            config = (RangeIndexConfig) indexConf.getCustomIndexSpec(RangeIndex.ID);
            if (config != null)
                // Create a copy of the original RangeIndexConfig (there's only one per db instance),
                // so we can safely work with it.
                config = new RangeIndexConfig(config);
        } else {
            config = RangeIndexConfig.DEFAULT_CONFIG;
        }
        this.mode = mode;

        if (contextCollectors != null) {
            for (final ContextTextCollector contextCollector : contextCollectors.values()) {
                contextCollector.reset();
            }
        }
    }

    @Override
    public void setMode(final ReindexMode mode) {
        this.mode = mode;
        switch (mode) {

            case STORE:
                if (nodesAwaitingPostContextCompletion != null) {
                    nodesAwaitingPostContextCompletion.clear();
                }
                if (nodesToWrite != null) {
                    nodesToWrite.clear();
                }
                cachedNodesToWriteSize = 0;
                break;

            case REMOVE_SOME_NODES:
                nodesToRemove = new TreeSet<>();
                break;
        }
    }

    @Override
    public DocumentImpl getDocument() {
        return currentDoc;
    }

    @Override
    public ReindexMode getMode() {
        return mode;
    }

    @Override
    public <T extends IStoredNode> IStoredNode getReindexRoot(IStoredNode<T> node, NodePath path, boolean insert, boolean includeSelf) {
//        if (node.getNodeType() == Node.ATTRIBUTE_NODE)
//            return null;
        if (config == null)
            return null;
        NodePath2 p = new NodePath2((NodePath2)path);
        boolean reindexRequired = false;
        if (node.getNodeType() == Node.ELEMENT_NODE && !includeSelf) {
            p.removeLastNode();
        }
        while (p.length() > 0) {
            if (config.matches(p)) {
                reindexRequired = true;
                break;
            }
            p.removeLastNode();
        }
        if (reindexRequired) {
            p = new NodePath2((NodePath2)path);
            IStoredNode topMost = null;
            IStoredNode currentNode = node;
            if (currentNode.getNodeType() != Node.ELEMENT_NODE)
                currentNode = currentNode.getParentStoredNode();
            while (currentNode != null) {
                if (config.matches(p))
                    topMost = currentNode;
                currentNode = currentNode.getParentStoredNode();
                p.removeLastNode();
            }
            return topMost;
        }
        return null;
    }

    @Override
    public QueryRewriter getQueryRewriter(XQueryContext context) {
        return new RangeQueryRewriter(context);
    }

    @Override
    public StreamListener getListener() {
        return listener;
    }

    @Override
    public MatchListener getMatchListener(DBBroker broker, NodeProxy proxy) {
        // range index does not support matches
        return null;
    }

    @Override
    public void flush() {
        switch (mode) {
            case STORE:
                checkAndUpdateNodesAwaitingPostContextCompletion();
                write();
                break;
            case REMOVE_SOME_NODES:
                removeNodes();
                break;
            case REMOVE_ALL_NODES:
                removeDocument(currentDoc.getDocId());
                break;
        }
    }

    @Override
    public void removeCollection(Collection collection, DBBroker broker, boolean reindex) throws PermissionDeniedException {
        if (LOG.isDebugEnabled())
            LOG.debug("Removing collection {}", collection.getURI());
        IndexWriter writer = null;
        try {
            writer = index.getWriter();
            for (Iterator<DocumentImpl> i = collection.iterator(broker); i.hasNext(); ) {
                DocumentImpl doc = i.next();
                final BytesRefBuilder bytes = new BytesRefBuilder();
                NumericUtils.intToPrefixCoded(doc.getDocId(), 0, bytes);
                Term dt = new Term(FIELD_DOC_ID, bytes.toBytesRef());
                writer.deleteDocuments(dt);
            }
        } catch (IOException | PermissionDeniedException | LockException e) {
            LOG.error("Error while removing lucene index: {}", e.getMessage(), e);
        } finally {
            index.releaseWriter(writer);
            if (reindex) {
                try {
                    index.sync();
                } catch (DBException e) {
                    LOG.warn("Exception during reindex: {}", e.getMessage(), e);
                }
            }
            mode = ReindexMode.STORE;
        }
        if (LOG.isDebugEnabled())
            LOG.debug("Collection removed.");
    }

    protected void removeDocument(int docId) {
        IndexWriter writer = null;
        try {
            writer = index.getWriter();
            final BytesRefBuilder bytes = new BytesRefBuilder();
            NumericUtils.intToPrefixCoded(docId, 0, bytes);
            Term dt = new Term(FIELD_DOC_ID, bytes.toBytesRef());
            writer.deleteDocuments(dt);
        } catch (IOException e) {
            LOG.warn("Error while removing lucene index: {}", e.getMessage(), e);
        } finally {
            index.releaseWriter(writer);
            mode = ReindexMode.STORE;
        }
    }

    /**
     * Remove specific nodes from the index. This method is used for node updates
     * and called from flush() if the worker is in {@link ReindexMode#REMOVE_SOME_NODES}
     * mode.
     */
    protected void removeNodes() {
        if (nodesToRemove == null)
            return;
        IndexWriter writer = null;
        try {
            writer = index.getWriter();

            for (NodeId nodeId : nodesToRemove) {
                // build id from nodeId and docId
                int nodeIdLen = nodeId.size();
                byte[] data = new byte[nodeIdLen + 4];
                ByteConversion.intToByteH(currentDoc.getDocId(), data, 0);
                nodeId.serialize(data, 4);

                Term it = new Term(FIELD_ID, new BytesRef(data));
                TermQuery iq = new TermQuery(it);
                writer.deleteDocuments(iq);
            }
        } catch (IOException e) {
            LOG.warn("Error while deleting lucene index entries: {}", e.getMessage(), e);
        } finally {
            nodesToRemove = null;
            index.releaseWriter(writer);
        }
    }

    @Override
    public boolean checkIndex(DBBroker broker) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void indexText(final NodeHandle nodeHandle, final QName qname, final NodePath path, final RangeIndexConfigElement config, final TextCollector collector) {
        @Nullable List<StaticContext> preContexts = null;
        @Nullable List<CompletableContext> postContexts = null;
        if (this.contextCollectors != null && config instanceof BasicRangeIndexConfigElement) {
            @Nullable final Map<String, RangeIndexConfigContextRefElement> contextRefs = ((BasicRangeIndexConfigElement) config).getContextRefs();
            if (contextRefs != null) {
                for (final RangeIndexConfigContextRefElement contextRefConfig : contextRefs.values()) {
                    final ContextTextCollector contextTextCollector = this.contextCollectors.get(contextRefConfig.getRef());

                    if (contextRefConfig.getRangeIndexConfigContextElement().getPreContextSize() > 0) {
                        @Nullable final NodeId[] preContextEntries = contextTextCollector.getContextEntries();
                        if (preContextEntries != null) {
                            final StaticContext preContext = new StaticContext(contextRefConfig.getName(), preContextEntries);
                            if (preContexts == null) {
                                preContexts = new ArrayList<>(contextRefs.size());
                            }
                            preContexts.add(preContext);
                        }
                    }

                    if (contextRefConfig.getRangeIndexConfigContextElement().getPostContextSize() > 0) {
                        final CompletableContext postContext = new CompletableContext(contextRefConfig.getName());
                        // listen for the post context
                        contextTextCollector.addContextReceiver(new CompletableContext.ContextReceiverAdapter(postContext), contextRefConfig.getRangeIndexConfigContextElement().getPostContextSize());
                        if (postContexts == null) {
                            postContexts = new ArrayList<>(contextRefs.size());
                        }
                        postContexts.add(postContext);
                    }
                }
            }
        }

        final RangeIndexDoc pending = new RangeIndexDoc(nodeHandle.getNodeId(), qname, path.copy(), collector, preContexts, postContexts, config);
        pending.setAddress(nodeHandle.getInternalAddress());

        if (postContexts != null) {
            // if the node has not had its post contexts completed, we need to wait for that before adding it to `nodesToWrite`
            if (nodesAwaitingPostContextCompletion == null) {
                nodesAwaitingPostContextCompletion = new ArrayList<>();
            }
            nodesAwaitingPostContextCompletion.add(Tuple(pending, collector.length()));

            // check and update any nodes that were previously awaiting their post context(s) and are now complete
            lazyCheckAndUpdateNodesAwaitingPostContextCompletion();
        } else {
            // if the node has no post contexts we can add it to `nodesToWrite`
            if (nodesToWrite == null) {
                nodesToWrite = new ArrayList<>();
            }
            nodesToWrite.add(pending);
            cachedNodesToWriteSize += collector.length();
        }

        // write any nodes that may be ready to be written
        lazyWrite();
    }

    private void lazyCheckAndUpdateNodesAwaitingPostContextCompletion() {
        if (nodesAwaitingPostContextCompletion != null && nodesAwaitingPostContextCompletion.size() > MAX_CACHED_NODES_AWAITING_COMPLETION_SIZE) {
            checkAndUpdateNodesAwaitingPostContextCompletion();
        }
    }

    /**
     * Checks if the post context(s) of each RangeIndexDoc in `nodesAwaitingPostContextCompletion`
     * have been completed, if they have, then they are moved from `nodesAwaitingPostContextCompletion` to `nodesToWrite`
     */
    private void checkAndUpdateNodesAwaitingPostContextCompletion() {
        if (nodesAwaitingPostContextCompletion == null || nodesAwaitingPostContextCompletion.isEmpty()) {
            return;
        }

        // find all nodes whose post contexts have been completed
        final Iterator<Tuple2<RangeIndexDoc, Integer>> itNodeAwaitingPostContextCompletion = nodesAwaitingPostContextCompletion.iterator();
        while (itNodeAwaitingPostContextCompletion.hasNext()) {
            final Tuple2<RangeIndexDoc, Integer> nodeAwaitingPostContextCompletion = itNodeAwaitingPostContextCompletion.next();
            @Nullable final List<CompletableContext> completablePostContexts = nodeAwaitingPostContextCompletion._1.getPostContexts();
            if (completablePostContexts != null) {
                boolean done = false;
                for (final CompletableContext completablePostContext : completablePostContexts) {
                    done = completablePostContext.isDone();
                    if (!done) {
                        break;
                    }
                }
                if (done) {
                    // node has had its post contexts completed

                    // remove node with completed post contexts from `nodesAwaitingPostContextCompletion`
                    itNodeAwaitingPostContextCompletion.remove();

                    // add node with completed post context to `nodesToWrite` and update cached size
                    if (nodesToWrite == null) {
                        nodesToWrite = new ArrayList<>();
                    }
                    nodesToWrite.add(nodeAwaitingPostContextCompletion._1);
                    cachedNodesToWriteSize += nodeAwaitingPostContextCompletion._2;
                }
            }
        }
    }

    private void lazyWrite() {
        if (cachedNodesToWriteSize > MAX_CACHED_NODES_SIZE) {
            write();
        }
    }

    private void write() {
        if (nodesToWrite == null || nodesToWrite.isEmpty()) {
            return;
        }

        IndexWriter writer = null;
        try {
            writer = index.getWriter();

            // docId and nodeId are stored as doc value
            final IntDocValuesField fDocId = new IntDocValuesField(FIELD_DOC_ID, 0);
            final BinaryDocValuesField fNodeId = new BinaryDocValuesField(FIELD_NODE_ID, new BytesRef(8));
            final BinaryDocValuesField fAddress = new BinaryDocValuesField(FIELD_ADDRESS, new BytesRef(8));

            // docId also needs to be indexed
            final IntField fDocIdIdx = new IntField(FIELD_DOC_ID, 0, IntField.TYPE_NOT_STORED);

            for (final RangeIndexDoc pending : nodesToWrite) {
                final Document doc = new Document();

                fDocId.setIntValue(currentDoc.getDocId());
                doc.add(fDocId);

                // store the node id
                final int nodeIdLen = pending.getNodeId().size();
                final byte[] data = new byte[nodeIdLen + 2];
                ByteConversion.shortToByte((short) pending.getNodeId().units(), data, 0);
                pending.getNodeId().serialize(data, 2);
                fNodeId.setBytesValue(data);
                doc.add(fNodeId);

                if (pending.getCollector().hasFields() && pending.getAddress() != -1) {
                    fAddress.setBytesValue(ByteConversion.longToByte(pending.getAddress()));
                    doc.add(fAddress);
                }

                // add separate index for node id
                final byte[] idData = new byte[nodeIdLen + 4];
                ByteConversion.intToByteH(currentDoc.getDocId(), idData, 0);
                pending.getNodeId().serialize(idData, 4);
                final BinaryTokenStream bts = new BinaryTokenStream(new BytesRef(idData));
                final Field fNodeIdIdx = new Field(FIELD_ID, bts, LuceneIndexWorker.TYPE_NODE_ID);
                doc.add(fNodeIdIdx);

                // store any fields
                @Nullable final List<TextCollector.Field> fields = pending.getCollector().getFields();
                if (fields != null) {
                    for (final TextCollector.Field field : fields) {
                        final String contentField;
                        if (field.isNamed()) {
                            contentField = field.getName();
                        } else {
                            contentField = LuceneUtil.encodeQName(pending.getQName(), index.getBrokerPool().getSymbols());
                        }
                        Field fld = null;
                        if (pending.getConfig() instanceof BasicRangeIndexConfigElement) {
                            fld = ((BasicRangeIndexConfigElement) pending.getConfig()).convertToField(contentField, field.getContent());
                        }
                        if (fld != null) {
                            doc.add(fld);
                        }
                    }
                }

                // store any context

                // store the pre-context if present
                @Nullable final List<StaticContext> preContexts = pending.getPreContexts();
                if (preContexts != null) {
                    for (final StaticContext preContext : preContexts) {
                        @Nullable final NodeId[] preContextEntries = preContext.getEntries();
                        if (preContextEntries != null) {
                            final byte[] nodeIdsBytes = LuceneUtil.encodeNodeIds(preContextEntries);
                            final StoredField fPreContext = new StoredField(PRE_CONTEXT_FIELD_PREFIX + preContext.getName(), new BytesRef(nodeIdsBytes));
                            doc.add(fPreContext);
                        }
                    }
                }
                // store the post-context if present
                @Nullable final List<CompletableContext> postContexts = pending.getPostContexts();
                // store the pre-context if present... all should have been completed by this point!
                if (postContexts != null) {
                    for (final CompletableContext postContext : postContexts) {
                        @Nullable final NodeId[] postContextEntries = postContext.getEntries();
                        if (postContextEntries != null) {
                            final byte[] nodeIdsBytes = LuceneUtil.encodeNodeIds(postContextEntries);
                            final StoredField fPostContext = new StoredField(POST_CONTEXT_FIELD_PREFIX + postContext.getName(), new BytesRef(nodeIdsBytes));
                            doc.add(fPostContext);
                        }
                    }
                }

                // store the document id
                fDocIdIdx.setIntValue(currentDoc.getDocId());
                doc.add(fDocIdIdx);

                Analyzer analyzer = null;
                if (pending.getConfig() instanceof BasicRangeIndexConfigElement) {
                    analyzer = ((BasicRangeIndexConfigElement) pending.getConfig()).getAnalyzer();
                }
                if (analyzer == null) {
                    analyzer = config.getDefaultAnalyzer();
                }
                writer.addDocument(doc, analyzer);
            }
        } catch (final IOException e) {
            LOG.warn("An exception was caught while indexing document: {}", e.getMessage(), e);
        } finally {
            index.releaseWriter(writer);
            nodesToWrite = new ArrayList<>();
            cachedNodesToWriteSize = 0;
        }
    }

    public NodeSet query(int contextId, DocumentSet docs, NodeSet contextSet, List<QName> qnames, AtomicValue[] keys, RangeIndex.Operator operator, int axis) throws IOException, XPathException {
        return index.withSearcher(searcher -> {
            List<QName> definedIndexes = getDefinedIndexes(qnames);
            NodeSet resultSet = null;
            for (QName qname : definedIndexes) {
                Query query;
                String field = LuceneUtil.encodeQName(qname, index.getBrokerPool().getSymbols());
                if (keys.length > 1) {
                    BooleanQuery bool = new BooleanQuery();
                    for (AtomicValue key : keys) {
                        bool.add(toQuery(field, qname, key, operator, docs), BooleanClause.Occur.SHOULD);
                    }
                    query = bool;
                } else {
                    query = toQuery(field, qname, keys[0], operator, docs);
                }
                final short nodeType = qname.getNameType() == ElementValue.ATTRIBUTE ? Node.ATTRIBUTE_NODE : Node
                        .ELEMENT_NODE;

                final NodeSet indexedResultSet = doQuery(contextId, docs, contextSet, axis, searcher.searcher, nodeType, query, null);
                if (resultSet == null) {
                    resultSet = indexedResultSet;
                } else {
                    resultSet.addAll(indexedResultSet);
                }
            }
            return resultSet;
        });
    }

    public NodeSet queryField(int contextId, DocumentSet docs, NodeSet contextSet, Sequence fields, Sequence[] keys, RangeIndex.Operator[] operators, int axis) throws IOException, XPathException {
        return index.withSearcher(searcher -> {
            BooleanQuery query = new BooleanQuery();
            int j = 0;
            for (SequenceIterator i = fields.iterate(); i.hasNext(); j++) {
                String field = i.nextItem().getStringValue();
                if (keys[j].getItemCount() > 1) {
                    BooleanQuery bool = new BooleanQuery();
                    bool.setMinimumNumberShouldMatch(1);
                    for (SequenceIterator ki = keys[j].iterate(); ki.hasNext(); ) {
                        Item key = ki.nextItem();
                        Query q = toQuery(field, null, key.atomize(), operators[j], docs);
                        bool.add(q, BooleanClause.Occur.SHOULD);
                    }
                    query.add(bool, BooleanClause.Occur.MUST);
                } else {
                    Query q = toQuery(field, null, keys[j].itemAt(0).atomize(), operators[j], docs);
                    query.add(q, BooleanClause.Occur.MUST);
                }
            }
            Query qu = query;
            BooleanClause[] clauses = query.getClauses();
            if (clauses.length == 1) {
                qu = clauses[0].getQuery();
            }
            final NodeSet resultSet = new NewArrayNodeSet();
            resultSet.addAll(doQuery(contextId, docs, contextSet, axis, searcher.searcher, Node.ELEMENT_NODE, qu, null));
            return resultSet;
        });
    }

//    private OpenBitSet getDocs(DocumentSet docs, IndexSearcher searcher) throws IOException {
//        OpenBitSet bits = new OpenBitSet(searcher.getIndexReader().maxDoc());
//        for (Iterator i = docs.getDocumentIterator(); i.hasNext(); ) {
//            DocumentImpl nextDoc = (DocumentImpl) i.next();
//            Term dt = new Term(FIELD_DOC_ID, NumericUtils.intToPrefixCoded(nextDoc.getDocId()));
//            TermDocs td = searcher.getIndexReader().termDocs(dt);
//            while (td.next()) {
//                bits.set(td.doc());
//            }
//            td.close();
//        }
//        return bits;
//    }

    private NodeSet doQuery(final int contextId, final DocumentSet docs, final NodeSet contextSet, final int axis,
                            IndexSearcher searcher, final short nodeType, Query query, Filter filter) throws
            IOException {
        SearchCollector collector = new SearchCollector(docs, contextSet, nodeType, axis, contextId);
        searcher.search(query, filter, collector);
        return collector.getResultSet();
    }

    /**
     * Lookup context information for a node indexed in the range index.
     *
     * @param expression the calling expression.
     * @param nodeProxy the stored node as retrieved from the range index.
     * @param contextName the name of the context to lookup.
     * @param maxPreContextSize the size of the pre-context to retrieve from zero and up to that configured in the index definition. If set to {@link org.exist.xquery.modules.range.ContextLookup#INDEX_DEF_CONTEXT_SIZE} then the size from the index definition is used.
     * @param maxPostContextSize the size of the post-context to retrieve from zero and up to that configured in the index definition. If set to {@link org.exist.xquery.modules.range.ContextLookup#INDEX_DEF_CONTEXT_SIZE} then the size from the index definition is used.
     *
     * @return or null if the {@code nodeProxy} is not indexed.
     */
    public @Nullable Tuple2<Sequence, Sequence> contextLookup(final Expression expression, final NodeProxy nodeProxy, final String contextName, final int maxPreContextSize, final int maxPostContextSize) throws XPathException, IOException {
        // 1. find the context configuration
        @Nullable RangeIndexConfigContextElement rangeIndexConfigContextElement = null;
        final Node node = nodeProxy.getNode();
        final StoredNode<?> storedNode = (StoredNode<?>) node;
        final NodePath nodePath = storedNode.getPath();
        @Nullable final Iterator<RangeIndexConfigElement> configIter = config.getConfig(nodePath);
        if (configIter != null) {
            while (configIter.hasNext()) {
                final RangeIndexConfigElement configuration = configIter.next();
                if (configuration.match(nodePath) && configuration instanceof BasicRangeIndexConfigElement) {
                    @Nullable final Map<String, RangeIndexConfigContextRefElement> contextRefs = ((BasicRangeIndexConfigElement) configuration).getContextRefs();
                    if (contextRefs != null) {
                        @Nullable final RangeIndexConfigContextRefElement contextRef = contextRefs.get(contextName);
                        if (contextRef != null) {
                            rangeIndexConfigContextElement = contextRef.getRangeIndexConfigContextElement();
                            break;
                        }
                    }
                }
            }
        }
        if (rangeIndexConfigContextElement == null) {
            return null;
        }
        final short contextNodeType = rangeIndexConfigContextElement.path.getLastComponent().getNameType() == ElementValue.ELEMENT ? Node.ELEMENT_NODE : Node.ATTRIBUTE_NODE;

        // 2. Calculate max pre and post context sizes
        final int computedMaxPreContextSize;
        final int computedMaxPostContextSize;
        if (maxPreContextSize == INDEX_DEF_CONTEXT_SIZE) {
            computedMaxPreContextSize = rangeIndexConfigContextElement.getPreContextSize();
        } else {
            computedMaxPreContextSize = Math.min(rangeIndexConfigContextElement.getPreContextSize(), maxPreContextSize);
        }
        if (maxPostContextSize == INDEX_DEF_CONTEXT_SIZE) {
            computedMaxPostContextSize = rangeIndexConfigContextElement.getPostContextSize();
        } else {
            computedMaxPostContextSize = Math.min(rangeIndexConfigContextElement.getPostContextSize(), maxPostContextSize);
        }

        // if we are asked to search for nothing... then return nothing
        if ((computedMaxPreContextSize == 0 || rangeIndexConfigContextElement.getPreContextSize() == 0) && (computedMaxPostContextSize == 0 && rangeIndexConfigContextElement.getPostContextSize() == 0)) {
            return Tuple(Sequence.EMPTY_SEQUENCE, Sequence.EMPTY_SEQUENCE);
        }

        // 3. access the lucene document directly, and read the pre and post context fields
        @Nullable final Tuple2<Sequence, Sequence> contextSearchResults = index.withSearcher(searcher -> {

            // search on the document id and node id field
            final int nodeIdLen = nodeProxy.getNodeId().size();
            final byte[] idData = new byte[nodeIdLen + 4];
            ByteConversion.intToByteH(nodeProxy.getOwnerDocument().getDocId(), idData, 0);
            nodeProxy.getNodeId().serialize(idData, 4);

            final TermQuery docIdNodeIdQuery = new TermQuery(new Term(FIELD_ID, new BytesRef(idData)));
            final TopDocs queryResults = searcher.searcher.search(docIdNodeIdQuery, 1);

            if (queryResults.totalHits == 1) {
                final int luceneDocId = queryResults.scoreDocs[0].doc;
                final Document luceneDocument = searcher.searcher.doc(luceneDocId);

                // get the pre-context
                final Sequence preContextNodes;
                if (computedMaxPreContextSize <= 0) {
                    preContextNodes = Sequence.EMPTY_SEQUENCE;
                } else {
                    preContextNodes = getContextNodesFromField(nodeProxy.getOwnerDocument(), luceneDocument, PRE_CONTEXT_FIELD_PREFIX + contextName, contextNodeType);
                }

                // get the post-context
                final Sequence postContextNodes;
                if (computedMaxPostContextSize <= 0) {
                    postContextNodes = Sequence.EMPTY_SEQUENCE;
                } else {
                    postContextNodes = getContextNodesFromField(nodeProxy.getOwnerDocument(), luceneDocument, POST_CONTEXT_FIELD_PREFIX + contextName, contextNodeType);
                }

                return Tuple(preContextNodes, postContextNodes);
            } else if (queryResults.totalHits > 1) {
                throw new IOException("Found more than one document in the Range Index that has the same XML document id and node id");
            } else {
                // no such document in the index, perhaps the nodeProxy we were asked to search on does not have an entry in the Range Index - it should be though... so this is an error
                return null;
            }
        });

        return contextSearchResults;
    }

    private @Nullable Sequence getContextNodesFromField(final DocumentImpl dbDocument, final Document luceneDocument, final String contextFieldName, final short contextNodeType) throws IOException {
        @Nullable final IndexableField contextField = luceneDocument.getField(contextFieldName);
        if (contextField == null) {
            // could not find the context field in the index - it should be there though... so this is an error
            return null;
        }

        final BytesRef contextFieldValue = contextField.binaryValue();
        final NodeId[] contextNodeIds = LuceneUtil.decodeNodeIds(broker.getBrokerPool().getNodeFactory(), contextFieldValue.bytes, contextFieldValue.offset, contextFieldValue.length);
        int idx = 0;

        final Sequence contextNodes = new NewArrayNodeSet(contextNodeIds.length);
        final NodeHandle contextFirstDocumentNodeId = new DocumentNodeIdNodeHandle(dbDocument, contextNodeIds[idx], contextNodeType);

        // TODO(AR) could be more efficient to create a feature whereby we can set a filter on the iterator so we don't return IStoredNode for nodes that don't match the nodeIds we are looking for
        try (final INodeIterator nodeIterator = broker.getNodeIterator(contextFirstDocumentNodeId)) {
            while (nodeIterator.hasNext() && idx < contextNodeIds.length) {
                final IStoredNode<?> contextStoredNode = nodeIterator.next();
                if (contextStoredNode.getNodeId().equals(contextNodeIds[idx])) {
                    ((NewArrayNodeSet) contextNodes).add(new NodeProxy(contextStoredNode));
                    idx++;
                }
            }
        }

        return contextNodes;
    }

    private class SearchCollector extends Collector {
        private final NodeSet resultSet;
        private final NodeSet contextSet;
        private final short nodeType;
        private final int axis;
        private final int contextId;
        private final DocumentSet docs;
        private AtomicReader reader;
        private NumericDocValues docIdValues;
        private BinaryDocValues nodeIdValues;
        private BinaryDocValues addressValues;
        private final byte[] buf = new byte[1024];

        public SearchCollector(DocumentSet docs, NodeSet contextSet, short nodeType, int axis, int contextId) {
            this.resultSet = new NewArrayNodeSet();
            this.docs = docs;
            this.contextSet = contextSet;
            this.nodeType = nodeType;
            this.axis = axis;
            this.contextId = contextId;
        }

        public NodeSet getResultSet() {
            return resultSet;
        }

        @Override
        public void setScorer(Scorer scorer) throws IOException {
            // ignore
        }

        @Override
        public void collect(int doc) throws IOException {
            int docId = (int) this.docIdValues.get(doc);
            DocumentImpl storedDocument = docs.getDoc(docId);
            if (storedDocument == null) {
                return;
            }
            final BytesRef ref = this.nodeIdValues.get(doc);

            int units = ByteConversion.byteToShort(ref.bytes, ref.offset);
            NodeId nodeId = index.getBrokerPool().getNodeFactory().createFromData(units, ref.bytes, ref.offset + 2);

            // if a context set is specified, we can directly check if the
            // matching node is a descendant of one of the nodes
            // in the context set.
            if (contextSet != null) {
                int sizeHint = contextSet.getSizeHint(storedDocument);
                NodeProxy parentNode = contextSet.parentWithChild(storedDocument, nodeId, false, true);
                if (parentNode != null) {
                    NodeProxy storedNode = new NodeProxy(parentNode.getExpression(), storedDocument, nodeId);
                    storedNode.setNodeType(nodeType);
                    getAddress(doc, storedNode);
                    if (axis == NodeSet.ANCESTOR) {
                        resultSet.add(parentNode, sizeHint);
                        if (Expression.NO_CONTEXT_ID != contextId) {
                            parentNode.deepCopyContext(storedNode, contextId);
                        } else
                            parentNode.copyContext(storedNode);
                    } else {
                        resultSet.add(storedNode, sizeHint);
                    }
                }
            } else {
                NodeProxy storedNode = new NodeProxy(null, storedDocument, nodeId);
                storedNode.setNodeType(nodeType);
                getAddress(doc, storedNode);
                resultSet.add(storedNode);
            }
        }

        private void getAddress(int doc, NodeHandle storedNode) {
            if (addressValues != null) {
                final BytesRef ref = addressValues.get(doc);
                if (ref.offset < ref.bytes.length) {
                    final long address = ByteConversion.byteToLong(ref.bytes, ref.offset);
                    storedNode.setInternalAddress(address);
                }
            }
        }

        @Override
        public void setNextReader(AtomicReaderContext atomicReaderContext) throws IOException {
            this.reader = atomicReaderContext.reader();
            this.docIdValues = this.reader.getNumericDocValues(FIELD_DOC_ID);
            this.nodeIdValues = this.reader.getBinaryDocValues(FIELD_NODE_ID);
            this.addressValues = this.reader.getBinaryDocValues(FIELD_ADDRESS);
        }

        @Override
        public boolean acceptsDocsOutOfOrder() {
            return true;
        }
    }

    /**
     * Check index configurations for all collection in the given DocumentSet and return
     * a list of QNames, which have indexes defined on them.
     *
     * @return List of QName objects on which indexes are defined
     */
    private List<QName> getDefinedIndexes(List<QName> qnames) throws IOException {
        List<QName> indexes = new ArrayList<>(20);
        if (qnames != null && !qnames.isEmpty()) {
            for (QName qname : qnames) {
                if (qname.getLocalPart() == null || qname.getNamespaceURI() == null)
                    getDefinedIndexesFor(qname, indexes);
                else
                    indexes.add(qname);
            }
            return indexes;
        }
        return getDefinedIndexesFor(null, indexes);
    }

    private List<QName> getDefinedIndexesFor(QName qname, final List<QName> indexes) throws IOException {
        return index.withReader(reader -> {
            for (FieldInfo info: MultiFields.getMergedFieldInfos(reader)) {
                if (!FIELD_DOC_ID.equals(info.name)) {
                    QName name = LuceneUtil.decodeQName(info.name, index.getBrokerPool().getSymbols());
                    if (name != null && (qname == null || matchQName(qname, name)))
                        indexes.add(name);
                }
            }
            return indexes;
        });
    }

    protected BytesRef analyzeContent(String field, QName qname, String data, DocumentSet docs) throws XPathException {
        final Analyzer analyzer = getAnalyzer(qname, field, docs);
        if (!isCaseSensitive(qname, field, docs)) {
            data = data.toLowerCase();
        }
        if (analyzer == null) {
            return new BytesRef(data);
        }
        try {
            TokenStream stream = analyzer.tokenStream(field, new StringReader(data));
            TermToBytesRefAttribute termAttr = stream.addAttribute(TermToBytesRefAttribute.class);
            BytesRef token = null;
            try {
                stream.reset();
                if (stream.incrementToken()) {
                    termAttr.fillBytesRef();
                    token = BytesRef.deepCopyOf(termAttr.getBytesRef());
                }
                stream.end();
            } finally {
                stream.close();
            }
            return token;
        } catch (IOException e) {
            throw new XPathException((Expression) null, "Error analyzing the query string: " + e.getMessage(), e);
        }
    }

    /**
     * Return the analyzer to be used for the given field or qname. Either field
     * or qname should be specified.
     */
    private Analyzer getAnalyzer(QName qname, String fieldName, DocumentSet docs) {
        for (Iterator<Collection> i = docs.getCollectionIterator(); i.hasNext(); ) {
            Collection collection = i.next();
            IndexSpec idxConf = collection.getIndexConfiguration(broker);
            if (idxConf != null) {
                RangeIndexConfig config = (RangeIndexConfig) idxConf.getCustomIndexSpec(RangeIndex.ID);
                if (config != null) {
                    Analyzer analyzer = config.getAnalyzer(qname, fieldName);
                    if (analyzer != null)
                        return analyzer;
                }
            }
        }
        return null;
    }

    /**
     * Return the analyzer to be used for the given field or qname. Either field
     * or qname should be specified.
     */
    private boolean isCaseSensitive(QName qname, String fieldName, DocumentSet docs) {
        for (Iterator<Collection> i = docs.getCollectionIterator(); i.hasNext(); ) {
            Collection collection = i.next();
            IndexSpec idxConf = collection.getIndexConfiguration(broker);
            if (idxConf != null) {
                RangeIndexConfig config = (RangeIndexConfig) idxConf.getCustomIndexSpec(RangeIndex.ID);
                if (config != null && !config.isCaseSensitive(qname, fieldName)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean matchQName(QName qname, QName candidate) {
        boolean match = true;
        if (qname.getLocalPart() != null)
            match = qname.getLocalPart().equals(candidate.getLocalPart());
        if (match && qname.getNamespaceURI() != null && !qname.getNamespaceURI().isEmpty())
            match = qname.getNamespaceURI().equals(candidate.getNamespaceURI());
        return match;
    }

    private class RangeIndexListener extends AbstractStreamListener {

        @Override
        public void startElement(final Txn transaction, final ElementImpl element, final NodePath path) {
            if (mode == ReindexMode.STORE && config != null) {
                if (contentStack != null) {
                    for (final TextCollector extractor : contentStack) {
                        extractor.startElement(element, path);
                    }
                }
                final Iterator<RangeIndexConfigElement> configIter = config.getConfig(path);
                if (configIter != null) {
                    if (contentStack == null) {
                        contentStack = new ArrayDeque<>();
                    }
                    while (configIter.hasNext()) {
                        final RangeIndexConfigElement configuration = configIter.next();
                        if (configuration.match(path)) {
                            final TextCollector collector;
                            if (configuration instanceof RangeIndexConfigContextElement) {
                                final String contextId = ((RangeIndexConfigContextElement) configuration).getId();
                                if (contextCollectors == null) {
                                    contextCollectors = new HashMap<>();
                                    collector = configuration.newCollector(path);
                                    contextCollectors.put(contextId, (ContextTextCollector) collector);
                                } else {
                                    collector = contextCollectors.computeIfAbsent(contextId, _id -> (ContextTextCollector) configuration.newCollector(path));
                                }
                            } else {
                                collector = configuration.newCollector(path);
                                contentStack.push(collector);
                            }
                            collector.startElement(element, path);
                        }
                    }
                }
            }
            super.startElement(transaction, element, path);
        }

        @Override
        public void attribute(final Txn transaction, final AttrImpl attrib, final NodePath path) {
            path.addComponent(attrib.getQName());
            if (contentStack != null) {
                for (final TextCollector collector : contentStack) {
                    collector.attribute(attrib, path);
                }
            }
            Iterator<RangeIndexConfigElement> configIter = null;
            if (config != null) {
                configIter = config.getConfig(path);
            }
            if (mode != ReindexMode.REMOVE_ALL_NODES && configIter != null) {
                if (mode == ReindexMode.REMOVE_SOME_NODES) {
                    nodesToRemove.add(attrib.getNodeId());
                } else {
                    while (configIter.hasNext()) {
                        final RangeIndexConfigElement configuration = configIter.next();
                        if (configuration.match(path)) {
                            final SimpleTextCollector collector = new SimpleTextCollector(attrib.getValue());
                            indexText(attrib, attrib.getQName(), path, configuration, collector);
                        }
                    }
                }
            }
            path.removeLastComponent();
            super.attribute(transaction, attrib, path);
        }

        @Override
        public void endElement(Txn transaction, ElementImpl element, NodePath path) {
            if (config != null) {
                if (mode == ReindexMode.STORE && contentStack != null) {
                    for (final TextCollector extractor : contentStack) {
                        extractor.endElement(element, path);
                    }
                }
                final Iterator<RangeIndexConfigElement> configIter = config.getConfig(path);
                if (mode != ReindexMode.REMOVE_ALL_NODES && configIter != null) {
                    if (mode == ReindexMode.REMOVE_SOME_NODES) {
                        nodesToRemove.add(element.getNodeId());
                    } else {
                        while (configIter.hasNext()) {
                            final RangeIndexConfigElement configuration = configIter.next();
                            if (!(configuration instanceof RangeIndexConfigContextElement)) {
                                boolean match = configuration.match(path);
                                if (match) {
                                    final TextCollector collector = contentStack.pop();
                                    match = collector instanceof ComplexTextCollector
                                            ? match && ((ComplexTextCollector) collector).getConfig().matchConditions(element)
                                            : match;
                                    if (match) {
                                        indexText(element, element.getQName(), path, configuration, collector);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            super.endElement(transaction, element, path);
        }

        @Override
        public void characters(Txn transaction, AbstractCharacterData text, NodePath path) {
            if (contentStack != null) {
                for (final TextCollector collector : contentStack) {
                    collector.characters(text, path);
                }
            }
            super.characters(transaction, text, path);
        }

        @Override
        public void endIndexDocument(final Txn transaction) {
            if (mode == ReindexMode.STORE) {
                // document is finished, make sure to complete any nodes from the document that are awaiting post context
                if (contextCollectors != null) {
                    for (final ContextTextCollector contextCollector : contextCollectors.values()) {
                        contextCollector.reset();
                    }
                }
                checkAndUpdateNodesAwaitingPostContextCompletion();
                assert(nodesAwaitingPostContextCompletion == null || nodesAwaitingPostContextCompletion.isEmpty());
            }
            super.endIndexDocument(transaction);
        }

        @Override
        public IndexWorker getWorker() {
            return RangeIndexWorker.this;
        }
    }

    /**
     * Optimize the Lucene index by merging all segments into a single one. This
     * may take a while and write operations will be blocked during the optimize.
     *
     * @see org.apache.lucene.index.IndexWriter#forceMerge(int)
     */
    public void optimize() {
        IndexWriter writer = null;
        try {
            writer = index.getWriter(true);
            writer.forceMerge(1, true);
            writer.commit();
        } catch (IOException e) {
            LOG.warn("An exception was caught while optimizing the lucene index: {}", e.getMessage(), e);
        } finally {
            index.releaseWriter(writer);
        }
    }

//    public static class DocIdSelector implements FieldSelector {
//
//        private static final long serialVersionUID = -4899170629980829109L;
//
//        public FieldSelectorResult accept(String fieldName) {
//            if (FIELD_DOC_ID.equals(fieldName)) {
//                return FieldSelectorResult.LOAD;
//            } else if (FIELD_NODE_ID.equals(fieldName)) {
//                return FieldSelectorResult.LATENT;
//            }
//            return FieldSelectorResult.NO_LOAD;
//        }
//    }

    @Override
    public Occurrences[] scanIndex(XQueryContext context, DocumentSet docs, NodeSet nodes, Map hints) {
        try {
            List<QName> qnames = hints == null ? null : (List<QName>)hints.get(QNAMES_KEY);
            qnames = getDefinedIndexes(qnames);
            //Expects a StringValue
            String start = null, end = null;
            long max = Long.MAX_VALUE;
            if (hints != null) {
                Object vstart = hints.get(START_VALUE);
                Object vend = hints.get(END_VALUE);
                start = vstart == null ? null : vstart.toString();
                end = vend == null ? null : vend.toString();
                IntegerValue vmax = (IntegerValue) hints.get(VALUE_COUNT);
                max = vmax == null ? Long.MAX_VALUE : vmax.getValue();
            }
            return scanIndexByQName(qnames, docs, nodes, start, end, max);
        } catch (IOException e) {
            LOG.warn("Failed to scan index: {}", e.getMessage(), e);
            return new Occurrences[0];
        }
    }

    public Occurrences[] scanIndexByField(String field, DocumentSet docs, String start, long max) {
        try {
            return index.withReader(reader -> {
                TreeMap<String, Occurrences> map = new TreeMap<>();
                scan(docs, null, start, null, max, map, reader, field);

                Occurrences[] occur = new Occurrences[map.size()];
                return map.values().toArray(occur);
            });
        } catch (IOException e) {
            LOG.warn("Failed to scan index: {}", e.getMessage(), e);
            return new Occurrences[0];
        }
    }

    private Occurrences[] scanIndexByQName(List<QName> qnames, DocumentSet docs, NodeSet nodes, String start, String end, long max) throws IOException {
        return index.withReader(reader -> {
            TreeMap<String, Occurrences> map = new TreeMap<>();
            for (QName qname : qnames) {
                String field = LuceneUtil.encodeQName(qname, index.getBrokerPool().getSymbols());
                scan(docs, nodes, start, end, max, map, reader, field);
            }
            Occurrences[] occur = new Occurrences[map.size()];
            return map.values().toArray(occur);
        });
    }

    private void scan(DocumentSet docs, NodeSet nodes, String start, String end, long max, TreeMap<String, Occurrences> map, IndexReader reader, String field) throws IOException {
        List<AtomicReaderContext> leaves = reader.leaves();
        for (AtomicReaderContext context : leaves) {
            NumericDocValues docIdValues = context.reader().getNumericDocValues(FIELD_DOC_ID);
            BinaryDocValues nodeIdValues = context.reader().getBinaryDocValues(FIELD_NODE_ID);
            Bits liveDocs = context.reader().getLiveDocs();
            Terms terms = context.reader().terms(field);
            if (terms == null)
                continue;
            TermsEnum termsIter = terms.iterator(null);
            if (termsIter.next() == null) {
                continue;
            }
            do {
                if (map.size() >= max) {
                    break;
                }
                BytesRef ref = termsIter.term();
                String term = ref.utf8ToString();
                boolean include = true;
                if (end != null) {
                    if (term.compareTo(end) > 0)
                        include = false;
                } else if (start != null && !term.startsWith(start))
                    include = false;
                if (include) {
                    DocsEnum docsEnum = termsIter.docs(null, null);
                    while (docsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS) {
                        if (liveDocs != null && !liveDocs.get(docsEnum.docID())) {
                            continue;
                        }
                        int docId = (int) docIdValues.get(docsEnum.docID());
                        DocumentImpl storedDocument = docs.getDoc(docId);
                        if (storedDocument == null)
                            continue;
                        NodeId nodeId = null;
                        if (nodes != null) {
                            final BytesRef nodeIdRef = nodeIdValues.get(docsEnum.docID());
                            int units = ByteConversion.byteToShort(nodeIdRef.bytes, nodeIdRef.offset);
                            nodeId = index.getBrokerPool().getNodeFactory().createFromData(units, nodeIdRef.bytes, nodeIdRef.offset + 2);
                        }
                        if (nodeId == null || nodes.get(storedDocument, nodeId) != null) {
                            Occurrences oc = map.get(term);
                            if (oc == null) {
                                oc = new Occurrences(term);
                                map.put(term, oc);
                            }
                            oc.addDocument(storedDocument);
                            oc.addOccurrences(docsEnum.freq());
                        }
                    }
                }
            } while(termsIter.next() != null);
        }
    }

    private static class DocumentNodeIdNodeHandle implements NodeHandle {
        private final DocumentImpl document;
        private final NodeId nodeId;
        private final short nodeType;

        public DocumentNodeIdNodeHandle(final DocumentImpl document, final NodeId nodeId, final short nodeType) {
            this.document = document;
            this.nodeId = nodeId;
            this.nodeType = nodeType;
        }

        @Override
        public void setNodeId(final NodeId dln) {
            throw new UnsupportedOperationException("This classes NodeId is immutable");
        }

        @Override
        public long getInternalAddress() {
            return StoredNode.UNKNOWN_NODE_IMPL_ADDRESS;
        }

        @Override
        public void setInternalAddress(final long internalAddress) {
            throw new UnsupportedOperationException("This classes NodeId is immutable");
        }

        @Override
        public NodeId getNodeId() {
            return nodeId;
        }

        @Override
        public short getNodeType() {
            return nodeType;
        }

        @Override
        public DocumentImpl getOwnerDocument() {
            return document;
        }
    }
}
