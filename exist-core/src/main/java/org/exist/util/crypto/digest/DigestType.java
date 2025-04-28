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
package org.exist.util.crypto.digest;

import java.util.function.Supplier;

/**
 * An enumeration of message digest types.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public enum DigestType {
    MD_2((byte)0x01, 128, MD2StreamableDigest::new, "MD2"),
    MD_4((byte)0x02, 128, MD4StreamableDigest::new, "MD4"),
    MD_5((byte)0x03, 128, MD5StreamableDigest::new, "MD5"),

    SHA_1(  (byte)0x10,  160, SHA1StreamableDigest::new, "SHA-1"),
    SHA_256((byte)0x11,  256, SHA256StreamableDigest::new, "SHA-256"),
    SHA_512((byte)0x12, 512, SHA512StreamableDigest::new, "SHA-512"),

    RIPEMD_160((byte)0x20, 160, RIPEMD160StreamableDigest::new, "RIPEMD-160", "RIPEMD160"),
    RIPEMD_256((byte)0x21, 256, RIPEMD256StreamableDigest::new, "RIPEMD-256", "RIPEMD256"),

    BLAKE_160((byte)0x30, 160, Blake160StreamableDigest::new, "BLAKE2B-160", "BLAKE-160"),
    BLAKE_256((byte)0x31, 256, Blake256StreamableDigest::new, "BLAKE2B-256", "BLAKE-256"),
    BLAKE_512((byte)0x31, 512, Blake512StreamableDigest::new, "BLAKE2B-512", "BLAKE-512");


    private final byte id;
    private final int bits;
    private final Supplier<StreamableDigest> streamableFactory;
    private final String[] commonNames;

    DigestType(final byte id, final int bits, final Supplier<StreamableDigest> streamableFactory, final String... commonNames) {
        this.id = id;
        this.bits = bits;
        this.streamableFactory = streamableFactory;
        this.commonNames = commonNames;
    }

    /**
     * Get the id of the message digest.
     *
     * @return the id of the message digest
     */
    public byte getId() {
        return id;
    }

    /**
     * Get the digest type by id.
     *
     * @param id the id of the digest type
     *
     * @return the digest type
     *
     * @throws IllegalArgumentException if the id is invalid.
     */
    public static DigestType forId(final byte id) {
        for (final DigestType digestType : values()) {
            if (id == digestType.getId()) {
                return digestType;
            }
        }
        throw new IllegalArgumentException("Unknown digest type id: " + id);
    }

    /**
     * Get the common names for the digest type.
     *
     * @return the common names.
     */
    public String[] getCommonNames() {
        return commonNames;
    }

    /**
     * Get the digest type by common name.
     *
     * @param commonName the common name of the digest type
     *
     * @return the digest type
     *
     * @throws IllegalArgumentException if the common name is invalid.
     */
    public static DigestType forCommonName(final String commonName) {
        for (final DigestType digestType : values()) {
            for (final String cn : digestType.commonNames) {
                if (cn.equals(commonName)) {
                    return digestType;
                }
            }
        }

        throw new IllegalArgumentException("Unknown digest type common name: " + commonName);
    }

    /***
     * The length of the generated message digest
     *
     * @return the message digest length in bits
     */
    public int getDigestLength() {
        return bits;
    }

    /***
     * The length of the generated message digest
     *
     * @return the message digest length in bytes
     */
    public int getDigestLengthBytes() {
        return bits / 8;
    }

    public StreamableDigest newStreamableDigest() {
        return streamableFactory.get();
    }
}
