/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.indexing.range;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.exist.dom.persistent.AbstractCharacterData;
import org.exist.dom.persistent.AttrImpl;
import org.exist.dom.persistent.ElementImpl;
import org.exist.numbering.NodeId;
import org.exist.storage.NodePath;
import com.evolvedbinary.j8cu.RingBuffer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Collects context information.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ContextTextCollector implements TextCollector {

    private final RangeIndexConfigContextElement config;
    private final NodePath path;
    @Nullable private RingBuffer<NodeId> contextEntriesBuffer = null;
    @Nullable private FollowingContextListener followingContextListener = null;

    public ContextTextCollector(final RangeIndexConfigContextElement config, final NodePath path) {
        this.config = config;
        this.path = path;
    }

    @Override
    public void startElement(final ElementImpl element, final NodePath path) {
        if (this.path.match(path)) {
            if (contextEntriesBuffer == null) {
                // initialize the context for pre-context
                contextEntriesBuffer = new RingBuffer<>(NodeId.class, config.getPreContextSize(), true);
                followingContextListener = new FollowingContextListener(this::getContextEntries);
                contextEntriesBuffer.addListener(followingContextListener);
            }

            contextEntriesBuffer.put(element.getNodeId());
        }
    }

    @Override
    public void endElement(final ElementImpl element, final NodePath path) {
        // no-op
    }

    @Override
    public void characters(final AbstractCharacterData text, final NodePath path) {
        // no-op
    }

    @Override
    public void attribute(final AttrImpl attribute, final NodePath path) {
        // no-op
    }

    @Override
    public int length() {
        if (contextEntriesBuffer == null) {
            return 0;
        }
        return config.getPreContextSize() + config.getPostContextSize();
    }

    @Override
    public List<Field> getFields() {
        return null;
    }

    @Override
    public boolean hasFields() {
        return false;
    }

    public void reset() {
        if (followingContextListener != null) {
            followingContextListener.reset();
        }
        if (contextEntriesBuffer != null) {
            contextEntriesBuffer.reset();
        }
    }

    public @Nullable NodeId[] getContextEntries() {
        if (contextEntriesBuffer == null) {
            return null;
        }
        return contextEntriesBuffer.copy();
    }

    /**
     * Add a receiver that will receive a copy of the context after `maxContextSize` context entries have been encountered.
     *
     * @param contextReceiver the receiver.
     * @param maxContextSize the maximum number of context entries to receive.
     *
     * @throws IllegalStateException if the context has not yet been initialised.
     */
    public void addContextReceiver(final ContextReceiver contextReceiver, final int maxContextSize) {
        if (contextEntriesBuffer == null) {
            throw new IllegalStateException("Cannot add a listener to the context as the context has not yet been initialised");
        }
        if (followingContextListener == null) {
            throw new IllegalStateException("Cannot add a receiver to the context as the context listener has not yet been initialised");
        }
        followingContextListener.addContextReceiver(contextReceiver, maxContextSize);
    }

    /**
     * Simple interface for a class that will receive a copy of the context entries.
     */
    public interface ContextReceiver {
        /**
         * Receive a copy of the context entries.
         *
         * @param entries the context entries.
         */
        void receive(final NodeId[] entries);
    }

    /**
     * A Ring Buffer Listener that sends a copy of the Ring Buffer's internal buffer
     * to a number of receivers after at most `n` store events, and then removes the receiver.
     * The receiver may be triggered on less than `n` store events if less events are available
     * when the document indexing process completes.
     */
    public static class FollowingContextListener implements RingBuffer.Listener<NodeId> {
        private final Supplier<NodeId[]> contextAccessor;
        private int receivedStoreEvents;
        private final Int2ObjectMap<List<ContextReceiver>> followers = new Int2ObjectOpenHashMap<>();

        public FollowingContextListener(final Supplier<NodeId[]> contextAccessor) {
            this.contextAccessor = contextAccessor;
        }

        /**
         * Add a receiver that will receive a copy of the buffer after a number of store events.
         *
         * @param contextReceiver the receiver
         * @param maxContextSize the maximum number of store events the receiver expects before receiving the buffer.
         */
        public void addContextReceiver(final ContextReceiver contextReceiver, final int maxContextSize) {
            final int trigger = receivedStoreEvents + maxContextSize;
            final List<ContextReceiver> receivers = followers.computeIfAbsent(trigger, key -> new ArrayList<>(1));
            receivers.add(contextReceiver);
        }

        @Override
        public void stored(final NodeId entry) {
            receivedStoreEvents++;
            @Nullable final List<ContextReceiver> receivers = followers.remove(receivedStoreEvents);
            if (receivers != null) {
                @Nullable final NodeId[] followedContext = contextAccessor.get();
                if (followedContext == null) {
                    throw new IllegalStateException("We were following the context, but the context now appears to be uninitialised");
                }

                for (final ContextReceiver receiver : receivers) {
                    receiver.receive(followedContext);
                }
            }
        }

        /**
         * If there are any remaining followers that have not yet waited for the full `n` events, send whatever
         * remains in the Ring Buffer to them.
         * Subsequently, removes all followers and sets the number of received store events back to zero.
         */
        public void reset() {
            if (!followers.isEmpty()) {
                @Nullable final NodeId[] followedContext = contextAccessor.get();
                if (followedContext == null) {
                    throw new IllegalStateException("We were following the context, but the context now appears to be uninitialised");
                }

                for (final List<ContextReceiver> receivers : followers.values()) {
                    for (final ContextReceiver receiver : receivers) {
                        receiver.receive(followedContext);
                    }
                }

                followers.clear();
            }
            receivedStoreEvents = 0;
        }

        @Override
        public void retrieved(@org.jspecify.annotations.Nullable final NodeId entry) {
            // no-op
        }
    }
}
