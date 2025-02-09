package org.exist.storage.structural;

/**
 * Internal helper class used by
 * {@link NativeStructuralIndexWorker#findElementsByTagName(byte, org.exist.dom.persistent.DocumentSet, org.exist.dom.QName, org.exist.xquery.NodeSelector)}.
 */
public class Range {
  int start = -1;
  int end = -1;

  Range(int start) {
    this.start = start;
    this.end = start;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Range range = (Range) o;

    if (start != range.start) return false;
    return end == range.end;
  }

  @Override
  public int hashCode() {
    int result = start;
    result = 31 * result + end;
    return result;
  }
}
