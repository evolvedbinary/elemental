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
import xyz.elemental.mediatype.MediaTypeResolver.InstantiationException;

/**
 * Factory for instantiating Media Type Resolvers.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface MediaTypeResolverFactory {

    /**
     * Instantiate a new Media Type Resolver.
     *
     * @return a new Media Type Resolver.
     *
     * @throws InstantiationException if an error occurs
     *     whilst instantiating the MediaTypeResolver.
     */
    MediaTypeResolver newMediaTypeResolver()
            throws InstantiationException;

    /**
     * Instantiate a new Media Type Resolver.
     *
     * @param configDirs paths to directories which contain configuration
     *     files for the media type resolver.
     *
     * @return a new Media Type Resolver.
     *
     * @throws InstantiationException if an error occurs
     *     whilst instantiating the MediaTypeResolver.
     */
    MediaTypeResolver newMediaTypeResolver(@Nullable final Path... configDirs)
            throws InstantiationException;

}
