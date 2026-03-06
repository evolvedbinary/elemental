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

import net.jcip.annotations.NotThreadSafe;
import xyz.elemental.mediatype.MediaType;
import xyz.elemental.mediatype.MediaTypeAlias;
import xyz.elemental.mediatype.StorageType;

import javax.annotation.Nullable;

/**
 * Immutable implementation of a Media Type Alias.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class MediaTypeAliasImpl implements MediaTypeAlias {

    private final String aliasIdentifier;
    private final MediaType target;

    private MediaTypeAliasImpl(final String aliasIdentifier, final MediaType target) {
        this.aliasIdentifier = aliasIdentifier;
        this.target = target;
    }

    @Override
    public String getAliasIdentifier() {
        return aliasIdentifier;
    }

    @Override
    public String getIdentifier() {
        return target.getIdentifier();
    }

    @Nullable
    @Override
    public String[] getKnownFileExtensions() {
        return target.getKnownFileExtensions();
    }

    @Override
    public StorageType getStorageType() {
        return target.getStorageType();
    }

    /**
     * Construct a new Media Type Alias.
     *
     * @param aliasIdentifier the Media Type alias identifier
     *
     * @return a media type alias builder.
     */
    public static MediaTypeAliasImpl.Builder builder(final String aliasIdentifier) {
        return MediaTypeAliasImpl.Builder.alias(aliasIdentifier);
    }

    /**
     * Builder pattern which allows us to
     * ultimately construct an Immutable MediaTypeImpl.
     */
    @NotThreadSafe
    public static class Builder {
        private final String aliasIdentifier;
        private @Nullable MediaType target;

        private Builder(final String aliasIdentifier) {
            this.aliasIdentifier = aliasIdentifier;
        }

        /**
         * Initiate the build of a MediaTypeImpl.
         *
         * @param aliasIdentifier the Media Type alias identifier
         * @return a Media Type Alias builder
         */
        static MediaTypeAliasImpl.Builder alias(final String aliasIdentifier) {
            return new MediaTypeAliasImpl.Builder(aliasIdentifier);
        }

        /**
         * Set the target for the Media Type alias.
         *
         * @param target a media type.
         * @return this
         */
        public MediaTypeAliasImpl.Builder of(final MediaType target) {
            this.target = target;
            return this;
        }

        /**
         * Build the Immutable MediaTypeAlias.
         *
         * @return an immutable MediaTypeAlias.
         */
        public MediaTypeAlias build() {
            if (target == null) {
                throw new IllegalStateException("A target must be set for the alias");
            }

            return new MediaTypeAliasImpl(aliasIdentifier, target);
        }
    }
}
