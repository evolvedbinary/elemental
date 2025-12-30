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
import java.nio.file.Path;

/**
 * Finds Media Types for resources.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface MediaTypeResolver {

    /**
     * Resolve the Media Type for the filename.
     *
     * @param path the Path containing the filename.
     *
     * @return The MediaType for the filename, or null if there is no known or configured media type
     */
    @Nullable MediaType fromFileName(@Nullable final Path path);

    /**
     * Resolve the MediaType for the filename.
     *
     * @param path the file path containing the filename.
     *
     * @return The MediaType for the filename, or null if there is no known or configured media type.
     */
    @Nullable MediaType fromFileName(@Nullable final String path);

    /**
     * Resolve the MediaType from the name/identifier
     * of the media type.
     *
     * @param mediaType the name/identifier of the media type.
     *
     * @return The MediaType for the name/identifier, or null if the media type is unknown.
     */
    @Nullable MediaType fromString(@Nullable final String mediaType);

    /**
     * Returns the MediaType to be used for
     * unknown types of resources.
     *
     * This is typically used as a catch-all.
     *
     * @return the MediaType to use for unknown
     *     types of resources.
     */
    MediaType forUnknown();

    /**
     * The Instantiation Exception is thrown
     * if an error occurs when the factory
     * tries to instantiate a new Media Type Resolver.
     */
    class InstantiationException extends Exception {
        public InstantiationException(final String message) {
            super(message);
        }

        public InstantiationException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public InstantiationException(final Throwable cause) {
            super(cause);
        }
    }
}
