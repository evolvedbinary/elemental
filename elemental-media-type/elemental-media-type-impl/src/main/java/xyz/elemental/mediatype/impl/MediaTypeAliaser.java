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

import com.evolvedbinary.j8fu.tuple.Tuple2;
import io.lacuna.bifurcan.IList;
import io.lacuna.bifurcan.IMap;
import io.lacuna.bifurcan.LinearMap;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.elemental.mediatype.impl.MediaTypeConfigUtil.MediaTypeConfigSource;
import xyz.elemental.mediatype.impl.configuration.MediaTypeAlias;
import xyz.elemental.mediatype.impl.configuration.MediaTypeAliases;

import javax.annotation.Nullable;
import java.nio.file.Path;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static xyz.elemental.mediatype.impl.MediaTypeConfigUtil.findConfigSources;
import static xyz.elemental.mediatype.impl.MediaTypeConfigUtil.parseConfigSources;

/**
 * Maps Media Type Aliases to Media Types.
 * <h2>Aliases file search order.</h2><p>
 * The MediaTypeAliaser looks in various places in the user's
 * system for media type aliases files. When requests are made
 * to resolve media type aliases to media types, it searches
 * aliases files in the following order:
 * <ol>
 *     <li>The file <code>media-type-aliases.xml</code> from within the user's home directory:
 *         <ul>
 *             <li>Linux/Unix: $XDG_CONFIG_HOME/elemental/media-type-aliases.xml. If $XDG_CONFIG_HOME is not set then, ~/.config/elemental/media-type-aliases.xml</li>
 *             <li>macOS: $XDG_CONFIG_HOME/elemental/media-type-aliases.xml. If $XDG_CONFIG_HOME is not set then, ~/Library/Preferences/xyz.elemental/media-type-aliases.xml</li>
 *             <li>Windows: %APPDATA%/Elemental/media-type-aliases.xml. If %APPDATA% is not set then, %USERPROFILE%/AppData/Local/Elemental/media-type-aliases.xml</li>
 *         </ul>
 *     </li>
 *     <li>One or more files named <code>media-type-aliases.xml</code> in the Application's config directory(s).</li>
 *     <li>The file <code>media-type-aliases.xml</code> on the classpath in the package xyz.elemental.mediatype.</li>
 * </ol>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NotThreadSafe
public class MediaTypeAliaser {

    private static final Logger LOG = LoggerFactory.getLogger(MediaTypeAliaser.class);

    private static final String MEDIA_TYPE_ALIASES_FILENAME = "media-type-aliases.xml";

    final IMap<String, String> aliases;

    public MediaTypeAliaser(@Nullable final Path... configDirs) {
        final IList<MediaTypeConfigSource> aliasesFileSources = findConfigSources(MediaTypeAliaser.class, MEDIA_TYPE_ALIASES_FILENAME, configDirs);
        final IList<Tuple2<String, String>> aliases = parseConfigSources(MediaTypeAliases.class, aliasesFileSources, MediaTypeAliaser::parseAliases);
        this.aliases = toMap(aliases);
    }

    private static void parseAliases(final String configLocation, final MediaTypeAliases mediaTypeAliases, final IList<Tuple2<String, String>> aliases) {
        if (mediaTypeAliases.getMediaTypeAlias() == null) {
            LOG.error("No aliases found in {} skipping...", configLocation);
            return;
        }

        for (final MediaTypeAlias alias : mediaTypeAliases.getMediaTypeAlias()) {
            aliases.addLast(Tuple(alias.getAlias(), alias.getTarget()));
        }
    }

    private static IMap<String, String> toMap(final IList<Tuple2<String, String>> list) {
        final IMap<String, String> map = new LinearMap<>((int) list.size());
        for (final Tuple2<String, String> entry : list) {
            map.put(entry._1, entry._2);
        }
        return map.forked();
    }

    /**
     * Given a Media Type alias identifier try and resolve its Media Type identifier.
     *
     * @param mediaTypeAliasIdentifier the identifier of the Media Type alias.
     *
     * @return aliased Media Type Identifier, or null if there is no such alias.
     */
    public @Nullable String resolveAlias(final String mediaTypeAliasIdentifier) {
        return resolveAlias(mediaTypeAliasIdentifier, null);
    }

    /**
     * Given a Media Type alias identifier try and resolve its Media Type identifier.
     *
     * @param mediaTypeAliasIdentifier the identifier of the Media Type alias.
     * @param defaultMediaTypeIdentifier the default Media Type identifier to return
     *     if a Media Type identifier cannot be resolved for the Media Type alias identifier.
     *
     * @return aliased Media Type Identifier, or {@code defaultMediaTypeIdentifier} if there is no such alias.
     */
    public @Nullable String resolveAlias(final String mediaTypeAliasIdentifier, @Nullable final String defaultMediaTypeIdentifier) {
        return aliases.get(mediaTypeAliasIdentifier, defaultMediaTypeIdentifier);
    }
}
