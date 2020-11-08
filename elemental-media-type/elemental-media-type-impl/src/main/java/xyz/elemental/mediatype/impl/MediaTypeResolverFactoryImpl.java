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
package xyz.elemental.mediatype.impl;

import xyz.elemental.mediatype.MediaTypeResolver;
import xyz.elemental.mediatype.MediaTypeResolverFactory;

import javax.annotation.Nullable;
import java.nio.file.Path;

/**
 * Implementation of a Media Type Resolver Factory for
 * constructing {@link MediaTypeResolverImpl} instances.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class MediaTypeResolverFactoryImpl implements MediaTypeResolverFactory {

    /**
     * For public use an instance should be obtained via a
     * {@link java.util.ServiceLoader}.
     *
     * Intentionally set to package private to
     * enable access for Unit Tests.
     */
    MediaTypeResolverFactoryImpl() {
    }

    @Override
    public MediaTypeResolver newMediaTypeResolver() {
        return newMediaTypeResolver((Path[]) null);
    }

    @Override
    public MediaTypeResolver newMediaTypeResolver(@Nullable final Path... configDirs) {
        final ApplicationMimetypesFileTypeMap mimetypesFileTypeMap = new ApplicationMimetypesFileTypeMap(
                configDirs);
        final MediaTypeMapper mediaTypeMapper = new MediaTypeMapper(configDirs);
        return new MediaTypeResolverImpl(mimetypesFileTypeMap, mediaTypeMapper);
    }
}
