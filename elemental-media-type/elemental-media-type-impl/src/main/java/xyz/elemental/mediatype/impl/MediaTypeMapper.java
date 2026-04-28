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

import io.lacuna.bifurcan.IList;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.elemental.mediatype.StorageType;
import xyz.elemental.mediatype.impl.MediaTypeConfigUtil.MediaTypeConfigSource;
import xyz.elemental.mediatype.impl.configuration.MediaType;
import xyz.elemental.mediatype.impl.configuration.MediaTypeMappings;
import xyz.elemental.mediatype.impl.configuration.Storage;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static xyz.elemental.mediatype.impl.MediaTypeConfigUtil.findConfigSources;
import static xyz.elemental.mediatype.impl.MediaTypeConfigUtil.parseConfigSources;

/**
 * Maps Media Types to database Storage Types.
 * <h2>Mappings file search order.</h2><p>
 * The MediaTypeMapper looks in various places in the user's
 * system for media type mappings files. When requests are made
 * to resolve media types to storage types, it searches
 * mappings types files in the following order:
 * <ol>
 *     <li>The file <code>media-type-mappings.xml</code> from within the user's home directory:
 *         <ul>
 *             <li>Linux/Unix: $XDG_CONFIG_HOME/elemental/media-type-mappings.xml. If $XDG_CONFIG_HOME is not set then, ~/.config/elemental/media-type-mappings.xml</li>
 *             <li>macOS: $XDG_CONFIG_HOME/elemental/media-type-mappings.xml. If $XDG_CONFIG_HOME is not set then, ~/Library/Preferences/xyz.elemental/media-type-mappings.xml</li>
 *             <li>Windows: %APPDATA%/Elemental/media-type-mappings.xml. If %APPDATA% is not set then, %USERPROFILE%/AppData/Local/Elemental/media-type-mappings.xml</li>
 *         </ul>
 *     </li>
 *     <li>One or more files named <code>media-type-mappings.xml</code> in the Application's config directory(s).</li>
 *     <li>The file <code>media-type-mappings.xml</code> on the classpath in the package xyz.elemental.mediatype.</li>
 * </ol>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NotThreadSafe
public class MediaTypeMapper {

    private static final Logger LOG = LoggerFactory.getLogger(MediaTypeMapper.class);

    private static final String MEDIA_TYPE_MAPPINGS_FILENAME = "media-type-mappings.xml";

    private final Function<String, StorageType>[] matchers;

    public MediaTypeMapper(@Nullable final Path... configDirs) {
        final IList<MediaTypeConfigSource> mappingsFileSources = findConfigSources(MediaTypeMapper.class, MEDIA_TYPE_MAPPINGS_FILENAME, configDirs);
        final IList<Function<String, StorageType>> matchers = parseConfigSources(MediaTypeMappings.class, mappingsFileSources, MediaTypeMapper::mappingsToMatchers);
        this.matchers = matchers.toArray(Function[]::new);
    }

    private static void mappingsToMatchers(final String configLocation, final MediaTypeMappings mediaTypeMappings, final IList<Function<String, StorageType>> matchers) {
        if (mediaTypeMappings.getStorage() == null || mediaTypeMappings.getStorage().isEmpty()) {
            LOG.error("No mappings found in {} skipping...", configLocation);
            return;
        }

        for (final Storage storage : mediaTypeMappings.getStorage()) {
            for (final MediaType mediaType : storage.getMediaType()) {

                final StorageType storageType = toStorageType(storage.getType());
                final Function<String, StorageType> matcher;
                switch (mediaType.getMatch()) {
                    case STARTS_WITH:
                        matcher = identifier -> {
                            if (identifier.startsWith(mediaType.getValue())) {
                                return storageType;
                            } else {
                                return null;
                            }
                        };
                        break;

                    case FULL:
                        matcher = identifier -> {
                            if (identifier.equals(mediaType.getValue())) {
                                return storageType;
                            } else {
                                return null;
                            }
                        };
                        break;

                    case PATTERN:
                        final Pattern pattern = Pattern.compile(mediaType.getValue());
                        final Matcher patternMatcher = pattern.matcher("");
                        matcher = identifier -> {
                            patternMatcher.reset(identifier);
                            if (patternMatcher.matches()) {
                                return storageType;
                            } else {
                                return null;
                            }
                        };
                        break;

                    default:
                        throw new IllegalArgumentException();
                }

                matchers.addLast(matcher);
            }
        }
    }

    private static StorageType toStorageType(final xyz.elemental.mediatype.impl.configuration.StorageType type) {
        switch (type) {
            case XML:
                return StorageType.XML;

            case BINARY:
                return StorageType.BINARY;

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Given a Media Type identifier try and resolve its Storage Type.
     *
     * @param mediaTypeIdentifier the identifier of the Media Type.
     *
     * @return the Storage Type that should be used for the Media Type,
     *     or {@link StorageType#forUnknown()} if the storage type could not be resolved.
     */
    public StorageType resolveStorageType(final String mediaTypeIdentifier) {
        return resolveStorageType(mediaTypeIdentifier, StorageType.forUnknown());
    }

    /**
     * Given a Media Type identifier try and resolve its Storage Type.
     *
     * @param mediaTypeIdentifier the identifier of the Media Type.
     * @param defaultStorageType the default Storage Type to return if a Storage Type cannot
     *     be resolved for the Media Type identifier.
     *
     * @return the Storage Type that should be used for the Media Type, or {@link StorageType#forUnknown()} if the storage type could not be found.
     */
    public @Nullable StorageType resolveStorageType(final String mediaTypeIdentifier, @Nullable final StorageType defaultStorageType) {
        for (final Function<String, StorageType> matcher : matchers) {
            final StorageType storageType = matcher.apply(mediaTypeIdentifier);
            if (storageType != null) {
                return storageType;
            }
        }

        return defaultStorageType;
    }
}
