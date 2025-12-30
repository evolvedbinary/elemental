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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;

import static xyz.elemental.mediatype.impl.StringUtil.emptyStringAsNull;

/**
 * Utilities for working with Paths.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
class PathUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PathUtil.class);

    /**
     * Gets the config directory from the user's home folder.
     *
     * @return One of the following:
     *     <ul>
     *     <li>Linux/Unix: $XDG_CONFIG_HOME/elemental. If $XDG_CONFIG_HOME is not set then, ~/.config/elemental</li>
     *     <li>macOS: $XDG_CONFIG_HOME/elemental. If $XDG_CONFIG_HOME is not set then, ~/Library/Preferences/xyz.elemental</li>
     *     <li>Windows: %APPDATA%/Elemental. If %APPDATA% is not set then, %USERPROFILE%/AppData/Local/Elemental</li>
     *     </ul>
     *     or null if the environment variables or home folder cannot be resolved.
     */
    static @Nullable Path getUserConfigFolder() {
        final String osName = System.getProperty("os.name").toLowerCase();

        if (osName.startsWith("windows")) {
            // Windows
            @Nullable String appDataPath = emptyStringAsNull(System.getenv("APPDATA"));

            if (appDataPath == null) {
                @Nullable String userProfilePath = emptyStringAsNull(System.getProperty("user.home"));
                if (userProfilePath == null) {
                    LOG.warn("Unable to find environment variables %APPDATA% or %USERPROFILE%");
                } else {
                    appDataPath = userProfilePath + "\\AppData\\Local";
                }
            }

            if (appDataPath != null) {
                return Paths.get(appDataPath, "Elemental");
            }

        } else {
            @Nullable final String xdgConfigPath = emptyStringAsNull(System.getenv("XDG_CONFIG_HOME"));
            if (xdgConfigPath != null) {
                return Paths.get(xdgConfigPath, "elemental");
            }

            @Nullable final String homePath = System.getProperty("user.home");
            if (homePath == null) {
                LOG.warn("Unable to find environment variable %HOME%");
            } else {
                if (osName.startsWith("mac os x")) {
                    // macOS
                    return Paths.get(homePath, "Library", "Preferences", "xyz.elemental");

                } else {
                    // Linux/Unix (or anything else)
                    return Paths.get(homePath, ".config", "elemental");
                }
            }
        }

        return null;
    }

    /**
     * Gets the data directory from the user's home folder.
     *
     * @return One of the following:
     *     <ul>
     *         <li>Linux/Unix: $XDG_DATA_HOME If $XDG_DATA_HOME is not set then, ~/.local/share</li>
     *         <li>macOS: $XDG_DATA_HOME. If $XDG_DATA_HOME is not set then, ~/Library/Application Support</li>
     *         <li>Windows: %APPDATA%. If %APPDATA% is not set then, %USERPROFILE%/AppData/Local</li>
     *     </ul>
     *     or null if the environment variables or home folder cannot be resolved.
     */
    static @Nullable Path getUserDataFolder() {
        final String osName = System.getProperty("os.name").toLowerCase();

        if (osName.startsWith("windows")) {
            // Windows
            @Nullable String appDataPath = emptyStringAsNull(System.getenv("APPDATA"));

            if (appDataPath == null) {
                @Nullable String userProfilePath = emptyStringAsNull(System.getProperty("user.home"));
                if (userProfilePath == null) {
                    LOG.warn("Unable to find environment variables %APPDATA% or %USERPROFILE%");
                } else {
                    appDataPath = userProfilePath + "\\AppData\\Local";
                }
            }

            if (appDataPath != null) {
                return Paths.get(appDataPath);
            }

        } else {
            @Nullable final String xdgConfigPath = emptyStringAsNull(System.getenv("XDG_DATA_HOME"));
            if (xdgConfigPath != null) {
                return Paths.get(xdgConfigPath);
            }

            @Nullable final String homePath = System.getProperty("user.home");
            if (homePath == null) {
                LOG.warn("Unable to find environment variable %HOME%");
            } else {
                if (osName.startsWith("mac os x")) {
                    // macOS
                    return Paths.get(homePath, "Library", "Application Support");

                } else {
                    // Linux/Unix (or anything else)
                    return Paths.get(homePath, ".local", "share");
                }
            }
        }

        return null;
    }
}
