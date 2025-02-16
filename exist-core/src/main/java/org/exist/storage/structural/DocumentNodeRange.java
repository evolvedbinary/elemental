package org.exist.storage.structural;

import org.exist.dom.QName;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.DocumentSet;
import org.exist.dom.persistent.NodeProxy;
import org.exist.dom.persistent.NodeSet;
import org.exist.numbering.NodeId;
import org.exist.xquery.NodeSelector;

import java.util.*;

import static org.exist.xquery.Constants.FOLLOWING_SIBLING_AXIS;
import static org.exist.xquery.Constants.PRECEDING_SIBLING_AXIS;

/**
 * Internal helper class used by
 * {@link NativeStructuralIndexWorker#findElementsByTagName(byte, DocumentSet, QName, NodeSelector)}.
 */
public class DocumentNodeRange implements Comparable<DocumentNodeRange> {
    int startDocId = -1;
    int endDocId = -1;
    NodeId fromNodeId = null;
    NodeId toNodeId = null;

    DocumentNodeRange(int start) {
        this.startDocId = start;
        this.endDocId = start;
    }

    DocumentNodeRange(final int docId, final NodeId fromNodeId, final NodeId toNodeId) {
        this.startDocId = docId;
        this.endDocId = docId;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
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

    private static List<DocumentNodeRange> merge(final List<DocumentNodeRange> ranges) {

        Collections.sort(ranges);
        final List<DocumentNodeRange> result = new ArrayList<>(ranges.size());

        DocumentNodeRange r1 = null;
        while (true) {
            if (r1 == null && !ranges.isEmpty()) {
                r1 = ranges.remove(0);
            }
            if (ranges.isEmpty()) {
                if (r1 != null) {
                    result.add(r1);
                }
                break;
            }

            DocumentNodeRange r2 = ranges.remove(0);

            if (r1.startDocId == r2.startDocId
              && r1.endDocId == r2.endDocId
              && r1.fromNodeId.compareTo(r2.fromNodeId) <= 0
              && r2.fromNodeId.compareTo(r1.toNodeId) <= 0) {
                final NodeId toNodeId = r1.toNodeId.compareTo(r2.toNodeId) < 0 ? r2.toNodeId : r1.toNodeId;
                r1 = new DocumentNodeRange(r1.startDocId, r1.fromNodeId, toNodeId);
            } else {
                result.add(r1);
                r1 = r2;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "DocumentNodeRange{" +
          "startDocId=" + startDocId +
          ", endDocId=" + endDocId +
          ", fromNodeId=" + fromNodeId +
          ", toNodeId=" + toNodeId +
          '}';
    }

    public static List<DocumentNodeRange> fromSiblingContextSet(final NodeSet contextSet, final int axis) {
        final List<DocumentNodeRange> ranges = new ArrayList<>();
        for (NodeProxy nodeProxy : contextSet) {
            final NodeId contextNodeId = nodeProxy.getNodeId();
            final NodeId leftmost = contextNodeId.getParentId();
            final NodeId rightmost = leftmost.nextSibling();
            final NodeId from;
            final NodeId to;
            switch (axis) {
                case PRECEDING_SIBLING_AXIS:
                    from = leftmost;
                    to = contextNodeId;
                    break;
                case FOLLOWING_SIBLING_AXIS:
                    from = contextNodeId.nextSibling();
                    to = rightmost;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported axis specified: " + axis + ". Must be PRECEDING_SIBLING_AXIS (" +
                      PRECEDING_SIBLING_AXIS + ") or FOLLOWING_SIBLING_AXIS (" + FOLLOWING_SIBLING_AXIS + ")");
            }
            ranges.add(new DocumentNodeRange(nodeProxy.getDoc().getDocId(), from, to));
        }
        return merge(ranges);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DocumentNodeRange that = (DocumentNodeRange) o;

        if (startDocId != that.startDocId) return false;
        if (endDocId != that.endDocId) return false;
        if (!Objects.equals(fromNodeId, that.fromNodeId)) return false;
        return Objects.equals(toNodeId, that.toNodeId);
    }

    @Override
    public int hashCode() {
        int result = startDocId;
        result = 31 * result + endDocId;
        result = 31 * result + (fromNodeId != null ? fromNodeId.hashCode() : 0);
        result = 31 * result + (toNodeId != null ? toNodeId.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(final DocumentNodeRange other) {
        final int startDocDiff = other.startDocId - startDocId;
        if (startDocDiff != 0) return startDocDiff;

        final int fromNodeDiff = fromNodeId.compareTo(other.fromNodeId);
        if (fromNodeDiff != 0) return fromNodeDiff;

        final int endDocDiff = other.endDocId - endDocId;
        if (endDocDiff != 0) return endDocDiff;

        return toNodeId.compareTo(other.toNodeId);
    }
}
