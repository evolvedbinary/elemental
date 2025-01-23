/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.indexing.range;

import org.exist.numbering.NodeId;

import javax.annotation.Nullable;

/**
 * Context information that is statically known at construction time.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class StaticContext implements Context {
    private final String name;
    private final NodeId[] entries;

    public StaticContext(final String name, final NodeId[] entries) {
        this.name = name;
        this.entries = entries;
    }

    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public NodeId[] getEntries() {
        return entries;
    }
}
