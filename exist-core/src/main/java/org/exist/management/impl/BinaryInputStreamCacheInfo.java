/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.management.impl;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Date;
import java.util.Optional;

/**
 * Simple bean to hold JMX info and a Binary Input Stream's cache
 */
public class BinaryInputStreamCacheInfo {

    private final long created;
    private final CacheType cacheType;
    private final Optional<Path> file;
    private final long size;

    public BinaryInputStreamCacheInfo(final CacheType cacheType, final long created, final Optional<Path> file,
            final long size) {
        this.created = created;
        this.cacheType = cacheType;
        this.file = file;
        this.size = size;
    }

    /**
     * Get the time that the Cache was created.
     *
     * @return the time the Cache was created
     */
    public Date getCreated() {
        return new Date(created);
    }

    /**
     * Get the type of the Cache.
     *
     * @return the type of the Cache
     */
    public CacheType getCacheType() {
        return cacheType;
    }

    /**
     * Get the path of the file backing the cache.
     *
     * @return The path of the file backing the cache (if there is one)
     */
    @Nullable
    public String getFile() {
        return file
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .orElse(null);
    }

    /**
     * Get the size of the cache.
     *
     * @return the size of the cache.
     */
    public long getSize() {
        return size;
    }

    enum CacheType {
        FILE,
        MEMORY_MAPPED_FILE,
        MEMORY
    }
}
