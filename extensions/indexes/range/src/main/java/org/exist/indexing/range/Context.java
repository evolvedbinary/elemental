/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.indexing.range;

import org.exist.numbering.NodeId;

import javax.annotation.Nullable;

/**
 * Simple data class for holding Context information.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface Context {

    /**
     * Get the name of this context.
     */
    String getName();

    /**
     * Get the entries that make up this context.
     *
     * @return the entries, or null if there are no entries.
     */
    @Nullable NodeId[] getEntries();
}
