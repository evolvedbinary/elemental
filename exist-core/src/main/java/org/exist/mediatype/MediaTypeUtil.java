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
package org.exist.mediatype;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.elemental.mediatype.MediaTypeResolver;
import xyz.elemental.mediatype.MediaTypeResolverFactory;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * Service for accessing the Media Type Resolver.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class MediaTypeUtil {

    private static final Logger LOG = LogManager.getLogger(MediaTypeUtil.class);

    public static @Nullable MediaTypeResolver newMediaTypeResolver(@Nullable final Path applicationConfigDir) {
        final ServiceLoader<MediaTypeResolverFactory> serviceloader =
                ServiceLoader.load(MediaTypeResolverFactory.class);

        @Nullable final Path[] configDirs;

        // config from application's `etc/` folder
        if (applicationConfigDir != null) {
            configDirs = new Path[] { applicationConfigDir };
        } else {
            configDirs = null;
        }

        for (final MediaTypeResolverFactory mediaTypeResolverFactory : serviceloader) {
            MediaTypeResolver mediaTypeResolver = null;
            try {
                mediaTypeResolver = mediaTypeResolverFactory.newMediaTypeResolver(configDirs);
            } catch (final MediaTypeResolver.InstantiationException e) {
                LOG.error(e.getMessage(), e);
            }

            if (mediaTypeResolver != null) {
                return mediaTypeResolver;  // NOTE: at present there is only one implementation provided by Elemental.
            }
        }

        LOG.error("Unable to find a suitable implementation of MediaTypeResolverFactory");
        return null;
    }
}
