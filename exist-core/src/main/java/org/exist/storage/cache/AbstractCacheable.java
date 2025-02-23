/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.storage.cache;

/**
 * Simple base class for handling Cacheable objects.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class AbstractCacheable implements Cacheable {

    private int referenceCount = 0;
    private int timestamp = 0;
    private boolean dirty = false;

    @Override
    public int getReferenceCount() {
        return referenceCount;
    }

    @Override
    public int incReferenceCount() {
        if (referenceCount < Cacheable.MAX_REF) {
            referenceCount++;
        }
        return referenceCount;
    }

    @Override
    public int decReferenceCount() {
        if (referenceCount > Cacheable.MIN_REF) {
            referenceCount--;
        }
        return referenceCount;
    }

    @Override
    public void setReferenceCount(final int count) {
        referenceCount = count;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(final int timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean allowUnload() {
        return true;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Set whether this cacheable is dirty of not.
     *
     * @param dirty true if the cacheable is dirty, or false otherwise.
     */
    public void setDirty(final boolean dirty) {
        this.dirty = dirty;
    }
}
