package org.exist.storage.structural;

import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.DocumentSet;
import org.exist.dom.persistent.NodeProxy;
import org.exist.dom.persistent.NodeSet;
import org.exist.numbering.NodeId;
import org.exist.xquery.Expression;
import org.exist.xquery.NodeSelector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.exist.xquery.Constants.FOLLOWING_SIBLING_AXIS;
import static org.exist.xquery.Constants.PRECEDING_SIBLING_AXIS;

/**
 * Internal helper class used by
 * {@link NativeStructuralIndexWorker#findElementsByTagName(byte, org.exist.dom.persistent.DocumentSet, org.exist.dom.QName, org.exist.xquery.NodeSelector)}.
 */
public class DocumentNodeRange {
    int startDocId = -1;
    int endDocId = -1;
    NodeId fromNodeId = null;
    NodeId toNodeId = null;

    NodeSelector nodeSelector = null;

    DocumentNodeRange(int start) {
        this.startDocId = start;
        this.endDocId = start;
    }

    DocumentNodeRange(final int docId, final NodeId fromNodeId, final NodeId toNodeId, @Nullable final NodeSelector nodeSelector) {
        this.startDocId = docId;
        this.endDocId = docId;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.nodeSelector = nodeSelector;
    }

    /**
     * Scan the document set to find document id ranges to query
     *
     * @param docs the document set
     * @return List of contiguous document id ranges
     */
    static List<DocumentNodeRange> getDocIdRanges(final DocumentSet docs) {
        final List<DocumentNodeRange> ranges = new ArrayList<>();
        DocumentNodeRange next = null;
        for (final Iterator<DocumentImpl> i = docs.getDocumentIterator(); i.hasNext(); ) {
            final DocumentImpl doc = i.next();
            if (next == null) {
                next = new DocumentNodeRange(doc.getDocId());
            } else if (next.endDocId + 1 == doc.getDocId()) {
                next.endDocId++;
            } else {
                ranges.add(next);
                next = new DocumentNodeRange(doc.getDocId());
            }
        }
        if (next != null) {
            ranges.add(next);
        }

        return ranges;
    }

    public static List<DocumentNodeRange> fromSiblingContextSet(final NodeSet contextSet, final int contextId, final int axis) {
        final List<DocumentNodeRange> ranges = new ArrayList<>();
        for (NodeProxy nodeProxy : contextSet) {
            final NodeId contextNodeId = nodeProxy.getNodeId();
            final NodeId leftmost = contextNodeId.getParentId();
            final NodeId rightmost = leftmost.nextSibling();
            final NodeId fromNodeId;
            final NodeId toNodeId;
            final NodeSelector nodeSelector;
            switch (axis) {
                case PRECEDING_SIBLING_AXIS:
                    fromNodeId = leftmost;
                    toNodeId = contextNodeId;
                    nodeSelector = new PrecedingSiblingNodeSelector(nodeProxy, contextId);
                    break;
                case FOLLOWING_SIBLING_AXIS:
                    fromNodeId = contextNodeId.nextSibling();
                    toNodeId = rightmost;
                    nodeSelector = new FollowingSiblingNodeSelector(nodeProxy, contextId);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported axis specified: " + axis + ". Must be PRECEDING_SIBLING_AXIS (" +
                      PRECEDING_SIBLING_AXIS + ") or FOLLOWING_SIBLING_AXIS (" + FOLLOWING_SIBLING_AXIS + ")");
            }
            ranges.add(new DocumentNodeRange(nodeProxy.getDoc().getDocId(), fromNodeId, toNodeId, nodeSelector));
        }
        return ranges;
    }

    private static class PrecedingSiblingNodeSelector extends SiblingReferenceNodeSelector {

        private PrecedingSiblingNodeSelector(NodeProxy reference, int contextId) {
            super(reference, contextId);
        }

        @Override
        boolean isCorrectAxis(NodeId nodeId) {
            return nodeId.isSiblingOf(referenceNodeId)  && nodeId.before(referenceNodeId, true);
        }
    }

    private static class FollowingSiblingNodeSelector extends SiblingReferenceNodeSelector {

        private FollowingSiblingNodeSelector(NodeProxy reference, int contextId) {
            super(reference, contextId);
        }

        @Override
        boolean isCorrectAxis(NodeId nodeId) {
            return nodeId.isSiblingOf(referenceNodeId) && nodeId.after(referenceNodeId, true);
        }
    }

    private abstract static class SiblingReferenceNodeSelector implements NodeSelector {

        private final NodeProxy reference;
        private final int docId;
        protected final NodeId referenceNodeId;
        private final int contextId;
        private SiblingReferenceNodeSelector(final NodeProxy reference, final int contextId) {
            this.reference = reference;
            this.docId = reference.getDoc().getDocId();
            this.referenceNodeId = reference.getNodeId();
            this.contextId = contextId;
        }
        @Override
        public NodeProxy match(DocumentImpl doc, NodeId nodeId) {

            if (doc.getDocId() == docId && isCorrectAxis(nodeId)) {
                final NodeProxy nodeProxy = new NodeProxy(null, doc, nodeId);

                if (Expression.IGNORE_CONTEXT != contextId) {
                    if (Expression.NO_CONTEXT_ID == contextId) {
                        nodeProxy.copyContext(reference);
                    } else {
                        nodeProxy.addContextNode(contextId, reference);
                    }
                }
                return nodeProxy;
            }
            return null;
        }

        abstract boolean isCorrectAxis(final NodeId nodeId);

    }
}