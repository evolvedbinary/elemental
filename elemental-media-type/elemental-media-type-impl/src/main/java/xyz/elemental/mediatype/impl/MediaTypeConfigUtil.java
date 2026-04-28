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

import com.evolvedbinary.j8fu.function.TriConsumer;
import io.lacuna.bifurcan.IList;
import io.lacuna.bifurcan.LinearList;
import io.lacuna.bifurcan.List;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utilities for working with Media Type config files.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class MediaTypeConfigUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MediaTypeConfigUtil.class);

    /**
     * Searches in various places in the user's
     * system for media type config files in the following order:
     * <ol>
     *     <li>The file <code>configFilename</code> from within the user's home directory:
     *         <ul>
     *             <li>Linux/Unix: $XDG_CONFIG_HOME/elemental/configFilename. If $XDG_CONFIG_HOME is not set then, ~/.config/elemental/configFilename</li>
     *             <li>macOS: $XDG_CONFIG_HOME/elemental/configFilename. If $XDG_CONFIG_HOME is not set then, ~/Library/Preferences/xyz.elemental/configFilename</li>
     *             <li>Windows: %APPDATA%/Elemental/configFilename. If %APPDATA% is not set then, %USERPROFILE%/AppData/Local/Elemental/configFilename</li>
     *         </ul>
     *     </li>
     *     <li>One or more files named <code>configFilename</code> in the Application's config directory(s).</li>
     *     <li>The file <code>configFilename</code> on the classpath in the package xyz.elemental.mediatype.</li>
     * </ol>
     *
     * @param configManager The class that is calling this function - used for improved log messages, and finding the classpath.
     * @param configFilename The filename of the config files to search for.
     * @param configDirs Any application specific config directories to search within.
     *
     * @return a list of config sources.
     */
    static IList<MediaTypeConfigSource> findConfigSources(final Class<?> configManager, final String configFilename, final Path... configDirs) {
        final LinearList<MediaTypeConfigSource> configSources = new LinearList<>();

        LOG.trace("{}: load HOME", configManager.getSimpleName());
        @Nullable final Path userConfigFolder = PathUtil.getUserConfigFolder();
        if (userConfigFolder != null) {
            final Path mappingsFile = userConfigFolder.resolve(configFilename);
            if (!Files.exists(mappingsFile)) {
                LOG.trace("No {} found at: {}, skipping...", configFilename, mappingsFile.toAbsolutePath());
            } else {
                configSources.addLast(new MediaTypeConfigSource(mappingsFile));
            }
        }

        LOG.trace("{}: load application", configManager.getSimpleName());
        if (configDirs != null) {
            for (final Path configDir : configDirs) {
                final Path mappingsFile = configDir.resolve(configFilename);
                if (!Files.exists(mappingsFile)) {
                    LOG.warn("No custom {} found at: {}, skipping...", configFilename, mappingsFile.toAbsolutePath());
                } else {
                    configSources.addLast(new MediaTypeConfigSource(mappingsFile));
                }
            }
        }

        LOG.trace("{}: load classpath from xyz.elemental.mediatype", configManager.getSimpleName());
        final String classPathLocationStr = "xyz/elemental/mediatype/" + configFilename;
        @Nullable final URL url = MediaTypeMapper.class.getClassLoader().getResource(classPathLocationStr);
        if (url == null) {
            LOG.trace("No {} found on classpath from xyz.elemental.mediatype, skipping...", configFilename);
        } else {
            final InputStream is = configManager.getClassLoader().getResourceAsStream(classPathLocationStr);
            configSources.addLast(new MediaTypeConfigSource(url.toString(), is));
        }

        return configSources.forked();
    }

    /**
     * Parses config sources and generates a list of objects.
     *
     * @param configClass the JAXB object class that we will unmarshall each config sources.
     * @param configSources the config sources.
     * @param parseFn a Function that converts an object of type C into one or more result objects of type U.
     *
     * @param <C> The type of the JAB object to unmarshall.
     * @param <U> The type of the result object.
     *
     * @return the list of configured objects.
     */
    @SuppressWarnings("unchecked")
    static <C, U> IList<U> parseConfigSources(final Class<C> configClass, final IList<MediaTypeConfigSource> configSources, final TriConsumer<String, C, IList<U>> parseFn) {
        try {
            assert (!configSources.isLinear());

            final JAXBContext context;
            final Unmarshaller unmarshaller;
            try {
                context = JAXBContext.newInstance(configClass);
                unmarshaller = context.createUnmarshaller();
            } catch (final JAXBException e) {
                LOG.error("Unable to instantiate JAXB Unmarshaller: {}", e.getMessage(), e);
                return List.EMPTY;
            }

            final LinearList<U> results = new LinearList<>();
            for (final MediaTypeConfigSource configSource : configSources) {
                if (configSource.path != null && !Files.exists(configSource.path)) {
                    LOG.warn("Config path {} does not exist, skipping...", configSource.path);
                    continue;
                }

                try {
                    final C config;
                    if (configSource.path != null) {
                        config = (C) unmarshaller.unmarshal(configSource.path.toUri().toURL());
                    } else {
                        config = (C) unmarshaller.unmarshal(configSource.is);
                    }
                    if (config == null) {
                        LOG.error("No  config found in {} skipping...", configSource.location);
                        continue;
                    }

                    parseFn.accept(configSource.location, config, results);

                } catch (final MalformedURLException | JAXBException e) {
                    @Nullable String message = e.getMessage();
                    if (message == null) {
                        @Nullable final Throwable cause = e.getCause();
                        if (cause != null) {
                            message = cause.getMessage();
                        }
                    }
                    LOG.error("Skipping {} due to error: {}", configSource.location, message, e);
                }
            }

            return results.forked();

        } finally {
            for (final MediaTypeConfigSource configSource : configSources) {
                if (configSource.is != null) {
                    try {
                        configSource.is.close();
                    } catch (final IOException e) {
                        LOG.warn("Unable to close config file source: {} ", configSource.location, e);
                    }
                }
            }
        }
    }

    /**
     * Some source of configuration information.
     *
     * Either {@link #path} or {@link #is} will be set, but never both.
     */
    static class MediaTypeConfigSource {
        private final String location;

        @Nullable private final Path path;
        @Nullable private final InputStream is;

        private MediaTypeConfigSource(final Path path) {
            this.location = path.normalize().toAbsolutePath().toString();
            this.path = path;
            this.is = null;
        }

        private MediaTypeConfigSource(final String location, final InputStream is) {
            this.location = location;
            this.is = is;
            this.path = null;
        }
    }
}
