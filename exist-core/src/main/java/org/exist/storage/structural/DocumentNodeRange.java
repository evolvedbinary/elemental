package org.exist.storage.structural;

import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.DocumentSet;
import org.exist.dom.persistent.NodeProxy;
import org.exist.dom.persistent.NodeSet;
import org.exist.numbering.NodeId;
import org.exist.xquery.Constants;

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
  int start = -1;
  int end = -1;
  NodeId from = null;
  NodeId to = null;

  DocumentNodeRange(int start) {
    this.start = start;
    this.end = start;
  }

  DocumentNodeRange(final int docId, final NodeId from, final NodeId to) {
    this.start = docId;
    this.end = docId;
    this.from = from;
    this.to = to;
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
      } else if (next.end + 1 == doc.getDocId()) {
        next.end++;
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
          from = contextNodeId;
          to = rightmost;
          break;
        default:
          throw new IllegalArgumentException("Unsupported axis specified: " + axis + ". Must be PRECEDING_SIBLING_AXIS (" +
            PRECEDING_SIBLING_AXIS + ") or FOLLOWING_SIBLING_AXIS (" + FOLLOWING_SIBLING_AXIS + ")");
      }
      ranges.add(new DocumentNodeRange(nodeProxy.getDoc().getDocId(), from, to));
    }
    return ranges;
  }

}
