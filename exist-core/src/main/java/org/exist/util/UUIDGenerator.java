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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.fasterxml.uuid.impl.RandomBasedGenerator;

/**
 * UUID generator.
 *
 * See <a href="http://en.wikipedia.org/wiki/UUID">UUID</a>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author Dannes Wessels
 */
public class UUIDGenerator {

    private final static UUID EXISTDB_UUID_NAMESPACE = UUID.fromString("a99c2b03-67c4-49fb-b812-aa4c12099e65");

    private final static RandomBasedGenerator UUIDv4_GENERATOR = Generators.randomBasedGenerator();
    private final static NameBasedGenerator UUIDv3_GENERATOR;
    static {
        try {
            UUIDv3_GENERATOR = Generators.nameBasedGenerator(EXISTDB_UUID_NAMESPACE, MessageDigest.getInstance("MD5"));
        } catch (final NoSuchAlgorithmException e) {
            // NOTE: very very unlikely, MD5 is widely supported in various JDKs
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    
    /**
     * Generate random UUID code.
     *
     * @return UUID code, formatted as f271ec43-bf1f-4030-a269-b11576538f71
     */
    public static String getUUID() {
        return getUUIDversion4();
    }

    /**
     * Generate a version 4 UUID.
     *
     * See <a href="http://en.wikipedia.org/wiki/Universally_Unique_Identifier#Version_4_.28random.29">http://en.wikipedia.org/wiki/Universally_Unique_Identifier#Version_4_.28random.29</a>.
     *
     * @return a Version 4 UUID
     */
    public static String getUUIDversion4() {
        return UUIDv4_GENERATOR.generate().toString();
    }

    /**
     * Generate a version 3 UUID code.
     *
     * See <a href="http://en.wikipedia.org/wiki/Universally_Unique_Identifier#Version_3_.28MD5_hash.29">http://en.wikipedia.org/wiki/Universally_Unique_Identifier#Version_3_.28MD5_hash.29</a>
     *
     * @param name the name to generate a UUID for.
     *
     * @return a Version 3 UUID
     */
    public static String getUUIDversion3(final String name) {
        return UUIDv3_GENERATOR.generate(name).toString();
    }
}
