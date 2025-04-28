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
package org.exist.util;

import com.evolvedbinary.j8fu.tuple.Tuple2;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple utility functions for working with Java Maps.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface MapUtil {

    /**
     * Create a Hash Map from a List of Tuples.
     *
     * @param <K> the type of the keys in the map.
     * @param <V> the types of the values in the map.
     *
     * @param entries the entries for the map.
     *
     * @return The HashMap
     */
    @SafeVarargs
    static <K, V> Map<K,V> hashMap(final Tuple2<K, V>... entries) {
        return hashMap(Math.max(entries.length, 16), entries);
    }

    /**
     * Create a Hash Map from a List of Tuples.
     *
     * @param <K> the type of the keys in the map.
     * @param <V> the types of the values in the map.
     *
     * @param initialCapacity allows you to oversize the map if you plan to add more entries.
     * @param entries the entries for the map.
     *
     * @return The HashMap
     */
    @SafeVarargs
    static <K, V> Map<K,V> hashMap(final int initialCapacity, final Tuple2<K, V>... entries) {
        final Map<K, V> map = new HashMap<>(initialCapacity);
        for (final Tuple2<K, V> entry : entries) {
            map.put(entry._1, entry._2);
        }
        return map;
    }
}
