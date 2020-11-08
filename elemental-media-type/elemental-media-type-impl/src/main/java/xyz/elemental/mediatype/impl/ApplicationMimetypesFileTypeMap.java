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
import io.lacuna.bifurcan.LinearList;
import jakarta.activation.MimeTypeEntry;
import jakarta.activation.MimeTypeRegistry;
import jakarta.activation.MimetypesFileTypeMap;
import org.eclipse.angus.activation.MimeTypeFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * An implementation of {@link jakarta.activation.FileTypeMap} which
 * extends {@link jakarta.activation.MimetypesFileTypeMap} to also
 * read from an application specific file.
 *
 * <h2>MIME types file search order</h2>
 * The ApplicationMimetypesFileTypeMap looks in various places in the user's
 * system for MIME types file entries. When requests are made
 * to search for MIME types in the ApplicationMimetypesFileTypeMap, it searches
 * MIME types files in the following order:
 * <ol>
 *     <li>Programmatically added entries to the ApplicationMimetypesFileTypeMap instance.</li>
 *     <li>The file <code>.jakarta.mime.types</code> in the user's home directory.</li>
 *     <li>The file <code>.mime.types</code> in the user's home directory.</li>
 *     <li>The file <code>jakarta.mime.types</code> from within the user's home directory:
 *         <ul>
 *             <li>Linux/Unix: $XDG_DATA_HOME/jakarta.mime.types If $XDG_DATA_HOME is not set then, ~/.local/share/jakarta.mime.types</li>
 *             <li>macOS: $XDG_DATA_HOME/jakarta.mime.types. If $XDG_DATA_HOME is not set then, ~/Library/Application Support/jakarta.mime.types</li>
 *             <li>Windows: %APPDATA%/jakarta.mime.types. If %APPDATA% is not set then, %USERPROFILE%/AppData/Local/jakarta.mime.types</li>
 *         </ul>
 *      </li>
 *      <li>One or more files named <code>jakarta.mime.types</code> in the application's config directory(s).</li>
 *      <li>One or more files named <code>mime.types</code> in the application's config directory(s).</li>
 *      <li>The file <code>jakarta.mime.types</code> on the classpath in the package xyz.elemental.mediatype.</li>
 *      <li>The file <code>mime.types</code> on the classpath in the package xyz.elemental.mediatype.</li>
 *      <li>The file <code>jakarta.mime.types</code> in the Java runtime.</li>
 *      <li>The file <code>mime.types</code> in the Java runtime.</li>
 *      <li>The file or resources named <code>META-INF/jakarta.mime.types</code>.</li>
 *      <li>The file or resources named <code>META-INF/mime.types</code>.</li>
 *      <li>The file or resource named <code>META-INF/jakarta.mimetypes.default</code> (usually found only in the <code>activation.jar</code> file).</li>
 *      <li>The file or resource named <code>META-INF/mimetypes.default</code> (usually found only in the <code>activation.jar</code> file).</li>
 * </ol>
 *
 * <p>(The current implementation looks for the <code>jakarta.mime.types</code> and <code>mime.types</code> files
 * in the Java runtime in the directory <code><i>java.home</i>/conf</code>
 * if it exists, and otherwise in the directory
 * <code><i>java.home</i>/lib</code>, where <i>java.home</i> is the value
 * of the "java.home" System property.  Note that the "conf" directory was
 * introduced in JDK 9.)<p>
 *
 * See {@link MimetypesFileTypeMap} for further information.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ApplicationMimetypesFileTypeMap extends MimetypesFileTypeMap {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationMimetypesFileTypeMap.class);

    private static final String JAKARTA_MIME_TYPES_FILENAME = "jakarta.mime.types";
    private static final String LEGACY_MIME_TYPES_FILENAME = "mime.types";

    public ApplicationMimetypesFileTypeMap(@Nullable final Path... configDirs) {
        final Vector<MimeTypeRegistry> dbv = new Vector<>(6);	// usually 6 or fewer databases
        @Nullable MimeTypeRegistry mf;
        dbv.add(null);		// place holder for PROG entry

        @Nullable final Method loadFileMethod = getLoadFileMethod();
        if (loadFileMethod != null) {

            LOG.trace("MimetypesFileTypeMap: load HOME");
            try {
                final String user_home = System.getProperty("user.home");

                if (user_home != null) {
                    String path = user_home + File.separator + "." + JAKARTA_MIME_TYPES_FILENAME;
                    mf = loadFileRefl(loadFileMethod, path);
                    if (mf != null) {
                        dbv.add(mf);
                    }

                    path = user_home + File.separator + "." + LEGACY_MIME_TYPES_FILENAME;
                    mf = loadFileRefl(loadFileMethod, path);
                    if (mf != null) {
                        dbv.add(mf);
                    }
                }
            } catch (final SecurityException ex) {
                // no-op
            }

            LOG.trace("MimetypesFileTypeMap: load HOME DATA");
            try {
                @Nullable final Path userDataFolder = PathUtil.getUserDataFolder();

                if (userDataFolder != null) {
                    String path = userDataFolder.resolve("." + JAKARTA_MIME_TYPES_FILENAME).toAbsolutePath().toString();
                    mf = loadFileRefl(loadFileMethod, path);
                    if (mf != null) {
                        dbv.add(mf);
                    }

                    path = userDataFolder.resolve("." + LEGACY_MIME_TYPES_FILENAME).toAbsolutePath().toString();
                    mf = loadFileRefl(loadFileMethod, path);
                    if (mf != null) {
                        dbv.add(mf);
                    }
                }
            } catch (final SecurityException ex) {
                // no-op
            }

            LOG.trace("ApplicationMimetypesFileTypeMap: load application");
            if (configDirs != null) {
                for (final Path configDir : configDirs) {
                    boolean found = false;
                    final String[] filenames = new String[]{JAKARTA_MIME_TYPES_FILENAME, LEGACY_MIME_TYPES_FILENAME};
                    for (final String filename : filenames) {

                        final Path mimeTypesPath = configDir.resolve(filename);
                        if (!Files.exists(mimeTypesPath)) {
                            LOG.trace("No custom {} found at: {}, skipping...", filename, mimeTypesPath.toAbsolutePath());
                        }

                        mf = loadFileRefl(loadFileMethod, mimeTypesPath.toAbsolutePath().toString());
                        if (mf != null) {
                            dbv.add(mf);
                            found = true;
                        }
                    }
                    if (!found) {
                        LOG.warn("Could not find Media Types file named {} or {} in: {}, skipping...", JAKARTA_MIME_TYPES_FILENAME, LEGACY_MIME_TYPES_FILENAME, configDir.toAbsolutePath());
                    }
                }
            }
        }

        LOG.trace("ApplicationMimetypesFileTypeMap: load classpath from xyz.elemental.mediatype");
        @Nullable final Method loadAllResourcesMethod = getLoadAllResourcesMethod();
        if (loadAllResourcesMethod != null) {
            loadAllResourcesRefl(loadAllResourcesMethod, dbv, new String[] {
                "xyz/elemental/mediatype/" + JAKARTA_MIME_TYPES_FILENAME,
                "xyz/elemental/mediatype/" + LEGACY_MIME_TYPES_FILENAME
            });
        }

        if (loadFileMethod != null) {
            LOG.trace("MimetypesFileTypeMap: load SYS");
            try {
                // check system's home
                final String confDir = confDirRefl();
                if (confDir != null) {
                    mf = loadFileRefl(loadFileMethod, confDir + JAKARTA_MIME_TYPES_FILENAME);
                    if (mf != null) {
                        dbv.add(mf);
                    }

                    mf = loadFileRefl(loadFileMethod, confDir + LEGACY_MIME_TYPES_FILENAME);
                    if (mf != null) {
                        dbv.add(mf);
                    }
                }
            } catch (final SecurityException ex) {
                // no-op
            }
        }

        if (loadAllResourcesMethod != null) {
            LOG.trace("MimetypesFileTypeMap: load JAR");
            // load from the app's jar file
            loadAllResourcesRefl(loadAllResourcesMethod, dbv, new String[] {
                "META-INF/" + JAKARTA_MIME_TYPES_FILENAME,
                "META-INF/" + LEGACY_MIME_TYPES_FILENAME
            });
        }

        @Nullable final Method loadResourceMethod = getLoadResourceMethod();
        if (loadResourceMethod != null) {
            LOG.trace("MimetypesFileTypeMap: load DEF");
            mf = loadResourceRefl(loadResourceMethod, new String[] {
                "/META-INF/jakarta.mimetypes.default",
                "/META-INF/mimetypes.default"
            });
            if (mf != null) {
                dbv.add(mf);
            }
        }

        final MimeTypeFile[] DB = new MimeTypeFile[dbv.size()];
        dbv.copyInto(DB);
        setDBRefl(DB);
    }

    private static @Nullable Method getLoadFileMethod() {
        try {
            final Method loadFileMethod = MimetypesFileTypeMap.class.getDeclaredMethod("loadFile", String.class);
            loadFileMethod.setAccessible(true);
            return loadFileMethod;
        } catch (final NoSuchMethodException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private @Nullable MimeTypeRegistry loadFileRefl(final Method loadFileMethod, final String name) {
        try {
            return (MimeTypeRegistry) loadFileMethod.invoke(this, name);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private static @Nullable String confDirRefl() {
        try {
            final Field confDirField = MimetypesFileTypeMap.class.getDeclaredField("confDir");
            confDirField.setAccessible(true);
            return (String) confDirField.get(null);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private static @Nullable Method getLoadAllResourcesMethod() {
        try {
            final Method loadAllResourcesMethod = MimetypesFileTypeMap.class.getDeclaredMethod("loadAllResources", Vector.class, String[].class);
            loadAllResourcesMethod.setAccessible(true);
            return loadAllResourcesMethod;
        } catch (final NoSuchMethodException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("RawUseOfParameterizedType")
    private void loadAllResourcesRefl(final Method loadAllResourcesMethod, final Vector v, final String[] names) {
        try {
            loadAllResourcesMethod.invoke(this, v, (Object) names);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static @Nullable Method getLoadResourceMethod() {
        try {
            final Method loadResourceMethod = MimetypesFileTypeMap.class.getDeclaredMethod("loadResource", String[].class);
            loadResourceMethod.setAccessible(true);
            return loadResourceMethod;
        } catch (final NoSuchMethodException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private @Nullable MimeTypeRegistry loadResourceRefl(final Method loadResourceMethod, final String[] names) {
        try {
            return (MimeTypeFile) loadResourceMethod.invoke(this, (Object) names);
        } catch (final IllegalAccessException | InvocationTargetException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private void setDBRefl(final MimeTypeRegistry[] DB) {
        try {
            final Field dbField = MimetypesFileTypeMap.class.getDeclaredField("DB");
            dbField.setAccessible(true);
            dbField.set(this, DB);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Get all the entries from the MimetypesFileTypeMap.
     *
     * @return the entries from the MimetypesFileTypeMap.
     */
    @SuppressWarnings("unchecked")
    IList<Set<Map.Entry<String, MimeTypeEntry>>> getAllEntries() {
        final MimeTypeRegistry[] mimeTypeRegistries;
        try {
            final Field dbField = MimetypesFileTypeMap.class.getDeclaredField("DB");
            dbField.setAccessible(true);
            mimeTypeRegistries = (MimeTypeRegistry[]) dbField.get(this);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }

        if (mimeTypeRegistries == null) {
            return null;
        }

        final LinearList<Set<Map.Entry<String, MimeTypeEntry>>> entries = new LinearList<>();
        final Field typeHashField;
        try {
            typeHashField = MimeTypeFile.class.getDeclaredField("type_hash");
        } catch (final NoSuchFieldException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
        typeHashField.setAccessible(true);

        try {
            for (final MimeTypeRegistry mimeTypeRegistry : mimeTypeRegistries) {
                if (mimeTypeRegistry != null) {
                    final Hashtable<String, MimeTypeEntry> typeHash = (Hashtable<String, MimeTypeEntry>) typeHashField.get(mimeTypeRegistry);
                    entries.addLast(typeHash.entrySet());
                }
            }
        } catch (final IllegalAccessException e) {
            LOG.error("Unable to access type_hash from MimeTypeFile", e);
            return null;
        }

        return entries.forked();
    }
}
