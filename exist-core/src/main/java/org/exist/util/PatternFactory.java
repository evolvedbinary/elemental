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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.regex.Pattern;

/**
 * A simple Java Regular Expression Pattern Factory.
 *
 * Patterns are Cached in a LRU like Cache
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class PatternFactory {

    private static final PatternFactory instance = new PatternFactory();

    private final Cache<String, Pattern> cache;

    private PatternFactory() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .build();
    }

    public static PatternFactory getInstance() {
        return instance;
    }

    public Pattern getPattern(final String pattern) {
        return cache.get(pattern, ptn -> Pattern.compile(ptn));
    }

    public Pattern getPattern(final String pattern, final int flags) {
        return cache.get(pattern + flags, key -> Pattern.compile(pattern, flags));
    }
}
