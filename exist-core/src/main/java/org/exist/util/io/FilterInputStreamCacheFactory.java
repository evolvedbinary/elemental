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
package org.exist.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.lang.invoke.MethodType.methodType;

/**
 * Factory to instantiate a cache object
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class FilterInputStreamCacheFactory {

    private static final Logger LOG = LogManager.getLogger(FilterInputStreamCacheFactory.class);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public interface FilterInputStreamCacheConfiguration {
        String getCacheClass();
    }

    private FilterInputStreamCacheFactory() {
    }

    /**
     * Get a suitable Cache instance.
     *
     * @param cacheConfiguration the configuration for the cache
     * @param is the input stream to cache
     *
     * @return the cache instance
     *
     * @throws IOException if an error occurs setting up the cache
     */
    public static FilterInputStreamCache getCacheInstance(final FilterInputStreamCacheConfiguration cacheConfiguration, final InputStream is) throws IOException {
        final FilterInputStreamCache cache = new FilterInputStreamCacheFactory().instantiate(cacheConfiguration, is);
        if (cache == null) {
            throw new IOException("Could not load cache for class: " + cacheConfiguration.getCacheClass());
        }
        FilterInputStreamCacheMonitor.getInstance().register(cache);
        return cache;
    }

    private FilterInputStreamCache instantiate(final FilterInputStreamCacheConfiguration cacheConfiguration, final InputStream is) {
        try {
            final Class clazz = Class.forName(cacheConfiguration.getCacheClass());

            final MethodHandle methodHandle = LOOKUP.findConstructor(clazz, methodType(void.class, InputStream.class));

            final Function<InputStream, FilterInputStreamCache> constructor = (Function<InputStream, FilterInputStreamCache>)
                    LambdaMetafactory.metafactory(
                            LOOKUP, "apply", methodType(Function.class),
                            methodHandle.type().erase(), methodHandle, methodHandle.type()).getTarget().invokeExact();
            return constructor.apply(is);
        } catch (final Throwable e) {
            if (e instanceof InterruptedException) {
                // NOTE: must set interrupted flag
                Thread.currentThread().interrupt();
            }

            LOG.error(e.getMessage(), e);
            return null;
        }
    }
}
