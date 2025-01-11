/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.indexing.range;

import org.exist.storage.NodePath;

import javax.annotation.Nullable;

public interface RangeIndexConfigElement {

    NodePath getNodePath();

    void add(final RangeIndexConfigElement config);

    @Nullable RangeIndexConfigElement getNext();

    boolean match(NodePath other);

    boolean find(NodePath other);

    TextCollector newCollector(NodePath path);
}
