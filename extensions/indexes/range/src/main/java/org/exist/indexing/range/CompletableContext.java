/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.indexing.range;

import org.exist.numbering.NodeId;

import javax.annotation.Nullable;

/**
 * Context information that is completed at a later point in time.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class CompletableContext implements Context {

    private final String name;
    private boolean done;
    private @Nullable NodeId[] entries;

    public CompletableContext(final String name) {
        this.name = name;
    }

    /**
     * Complete this context with the provided entries.
     *
     * @param entries the entries for this context.
     *
     * @throws IllegalStateException if the context has already been completed.
     */
    public void complete(final NodeId[] entries) {
        if (isDone()) {
            throw new IllegalStateException("Context has already been completed");
        }
        this.entries = entries;
        this.done = true;
    }

    /**
     * Returns true if the context has been completed.
     *
     * @return true if the context has been completed, false otherwise.
     */
    public boolean isDone() {
        return done;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nullable NodeId[] getEntries() {
        if (!isDone()) {
            throw new IllegalStateException("Context has not yet been completed");
        }
        return entries;
    }

    /**
     * An adapter so that a {@link CompletableContext} may be used as a {@link ContextTextCollector.ContextReceiver}.
     */
    public static class ContextReceiverAdapter implements ContextTextCollector.ContextReceiver {
        private final CompletableContext completableContext;

        public ContextReceiverAdapter(final CompletableContext completableContext) {
            this.completableContext = completableContext;
        }

        @Override
        public void receive(final NodeId[] entries) {
            completableContext.complete(entries);
        }
    }
}
