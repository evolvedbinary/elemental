/*
 * Copyright (C) 2014, Evolved Binary Ltd
 *
 * This file was originally ported from FusionDB to Elemental by
 * Evolved Binary, for the benefit of the Elemental Open Source community.
 * Only the ported code as it appears in this file, at the time that
 * it was contributed to Elemental, was re-licensed under The GNU
 * Lesser General Public License v2.1 only for use in Elemental.
 *
 * This license grant applies only to a snapshot of the code as it
 * appeared when ported, it does not offer or infer any rights to either
 * updates of this source code or access to the original source code.
 *
 * The GNU Lesser General Public License v2.1 only license follows.
 *
 * =====================================================================
 *
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package xyz.elemental.mediatype;

import javax.annotation.Nullable;

/**
 * Types of database storage to use for resources of various Media Type.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public enum StorageType {
    /**
     * Native XML document storage within the database.
     */
    XML,

    /**
     * Binary document storage within the database.
     */
    BINARY;

    /**
     * Get a StorageType by its name.
     * Similar to {@link #valueOf(String)} but ignores
     * cases sensitivity.
     *
     * @param name the name of the StorageType
     * @return the StorageType
     *
     * @throws IllegalArgumentException if the name is unknown
     */
    public static StorageType fromName(final String name) {
        final StorageType storageType = lookup(name);
        if (storageType != null) {
            return storageType;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Get a StorageType by its name.
     * Similar to {@link #fromName(String)} but
     * if no Storage Type matches the {@code name}
     * then the Storage Type for Unknown resources
     * is returned.
     *
     * @param name the name of the StorageType
     * @return the StorageType for the name, or the Storage
     *  Type for Unknown resources as returned by {@link #forUnknown()}.
     */
    public static StorageType fromNameOrDefault(final String name) {
        final StorageType storageType = lookup(name);
        if (storageType != null) {
            return storageType;
        }
        return forUnknown();
    }

    /**
     * Get a StorageType by its name.
     * Similar to {@link #valueOf(String)} but ignores
     * cases sensitivity.
     *
     * @param name the name of the StorageType
     * @return the StorageType or null if there is
     *     no StorageType for the provided name.
     */
    private static @Nullable StorageType lookup(String name) {
        name = name.toUpperCase();
        for (final StorageType storageType : StorageType.values()) {
            if (storageType.name().equals(name)) {
                return storageType;
            }
        }
        return null;
    }

    /**
     * Returns the StorageType to be used for
     * unknown types of resources.
     *
     * This is typically used as a catch-all.
     *
     * @return the StorageType to use for unknown
     *     types of resources.
     */
    public static StorageType forUnknown() {
        return BINARY;
    }
}
