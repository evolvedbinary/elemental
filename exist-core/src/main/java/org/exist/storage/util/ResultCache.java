package org.exist.storage.util;

import org.checkerframework.checker.units.qual.K;
import org.exist.dom.QName;
import org.exist.dom.persistent.NodeSet;
import org.exist.xquery.NodeSelector;
import org.exist.storage.structural.Range;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO (AP) WIP of cacheing the results of an index query for re-use by other (parts of) an XQuery
 */
public class ResultCache {

  private final Map<Key, NodeSet> map = new ConcurrentHashMap<>();

  public void put(final byte type, final QName qName, final NodeSelector nodeSelector, final List<Range> range, NodeSet nodeSet) {
    map.put(new Key(type, qName, nodeSelector, range), nodeSet);
  }

  public NodeSet get(final byte type, final QName qName, final NodeSelector nodeSelector, final List<Range> range) {
    return map.get(new Key(type, qName, nodeSelector, range));
  }

  private static class Key {
    private final byte type;
    private final QName qName;
    private final NodeSelector nodeSelector;
    private final List<Range> range;

    private Key(final byte type, final QName qName, final NodeSelector nodeSelector, final List<Range> range) {
      this.type = type;
      this.qName = qName;
      this.nodeSelector = nodeSelector;
      this.range = range;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Key key = (Key) o;

      if (type != key.type) return false;
      if (!Objects.equals(qName, key.qName)) return false;
      if (!Objects.equals(nodeSelector, key.nodeSelector)) return false;
      return Objects.equals(range, key.range);
    }

    @Override
    public int hashCode() {
      int result = type;
      result = 31 * result + (qName != null ? qName.hashCode() : 0);
      result = 31 * result + (nodeSelector != null ? nodeSelector.hashCode() : 0);
      result = 31 * result + (range != null ? range.hashCode() : 0);
      return result;
    }

  }
}
