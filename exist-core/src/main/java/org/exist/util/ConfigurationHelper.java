/*
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
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
package org.exist.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.storage.BrokerPool;
import org.exist.xmldb.DatabaseImpl;

import javax.annotation.Nullable;

public class ConfigurationHelper {
    private final static Logger LOG = LogManager.getLogger(ConfigurationHelper.class); //Logger

    public static final String PROP_ELEMENTAL_CONFIGURATION_FILE = "elemental.configurationFile";
    /**
     * @deprecated use {@link #PROP_ELEMENTAL_CONFIGURATION_FILE}.
     */
    public static final String LEGACY_PROP_EXIST_CONFIGURATION_FILE = "exist.configurationFile";

    /**
     * Returns a file handle for Elemental's home directory.
     * We search in the following order.
     * <ol>
     *   <li>BrokerPool      : if Elemental was already configured.
     *   <li>elemental.home  : if exists
     *   <li>exist.home      : (legacy) if exists
     *   <li>user.home       : if exists, with a conf.xml file
     *   <li>user.dir        : if exists, with a conf.xml file
     *   <li>classpath entry : if exists, with a conf.xml file
     * </ol>
     *
     * @return the path to Elemental's home if known
     */
    public static Optional<Path> getElementalHome() {
        return getElementalHome(DatabaseImpl.CONF_XML);
    }

    /**
     * @deprecated use {@link #getElementalHome()}.
     */
    @Deprecated
    public static Optional<Path> getExistHome() {
        return getElementalHome();
    }

    /**
     * Returns a file handle for Elemental's home directory.
     * We search in the following order.
     * <ol>
     *   <li>BrokerPool      : if Elemental was already configured.
     *   <li>elemental.home  : if exists
     *   <li>exist.home      : (legacy) if exists
     *   <li>user.home       : if exists, with a conf.xml file
     *   <li>user.dir        : if exists, with a conf.xml file
     *   <li>classpath entry : if exists, with a conf.xml file
     * </ol>
     *
     * @param config the path to the config file.
     *
     * @return the path to Elemental's home if known
     */
    public static Optional<Path> getElementalHome(final String config) {
        // If Elemental was already configured, then return
        // the E of this instance.
    	try {
    		final BrokerPool broker = BrokerPool.getInstance();
    		if(broker != null) {
                final Optional<Path> elementalHome = broker.getConfiguration().getElementalHome().map(Path::normalize);
                if(elementalHome.isPresent()) {
                    LOG.debug("Got Elemental home from broker: {}", elementalHome);
                    return elementalHome;
                }
    		}
    	} catch(final Throwable e) {
            // Catch all potential problems
            LOG.debug("Could not retrieve instance of BrokerPool: {}", e.getMessage());
    	}

        // try elemental.home
        if (System.getProperty("elemental.home") != null) {
            final Path elementalHome = ConfigurationHelper.decodeUserHome(System.getProperty("elemental.home")).normalize();
            if (Files.isDirectory(elementalHome)) {
                LOG.debug("Got Elemental home from system property 'elemental.home': {}", elementalHome.toAbsolutePath().toString());
                return Optional.of(elementalHome);
            }
        }
    	
        // try exist.home
        if (System.getProperty("exist.home") != null) {
            final Path existHome = ConfigurationHelper.decodeUserHome(System.getProperty("exist.home")).normalize();
            if (Files.isDirectory(existHome)) {
                LOG.debug("Got Elemental home from system property 'exist.home': {}", existHome.toAbsolutePath().toString());
                return Optional.of(existHome);
            }
        }
        
        // try user.home
        final Path userHome = Paths.get(System.getProperty("user.home"));
        final Path userHomeRelativeConfig = userHome.resolve(config);
        if (Files.isDirectory(userHome) && Files.isRegularFile(userHomeRelativeConfig)) {
            final Path elementalHome = userHomeRelativeConfig.getParent().normalize();
            LOG.debug("Got Elemental home: {} from system property 'user.home': {}", elementalHome.toAbsolutePath(), userHome.toAbsolutePath());
            return Optional.of(elementalHome);
        }
        
        
        // try user.dir
        final Path userDir = Paths.get(System.getProperty("user.dir"));
        final Path userDirRelativeConfig = userDir.resolve(config);
        if (Files.isDirectory(userDir) && Files.isRegularFile(userDirRelativeConfig)) {
            final Path elementalHome = userDirRelativeConfig.getParent().normalize();
            LOG.debug("Got Elemental home: {} from system property 'user.dir': {}", elementalHome.toAbsolutePath(), userDir.toAbsolutePath());
            return Optional.of(elementalHome);
        }
        
        // try classpath
        final URL configUrl = ConfigurationHelper.class.getClassLoader().getResource(config);
        if (configUrl != null) {
            try {
                Path elementalHome;
                if ("jar".equals(configUrl.getProtocol())) {
                    elementalHome = Paths.get(new URI(configUrl.getPath())).getParent().getParent().normalize();
                    LOG.warn("{} file was found on the classpath, but inside a Jar file! Derived Elemental home from Jar's parent folder: {}", config, elementalHome);
                } else {
                    elementalHome = Paths.get(configUrl.toURI()).getParent().normalize();
                    if (FileUtils.fileName(elementalHome).equals("etc")) {
                        elementalHome = elementalHome.getParent().normalize();
                    }
                    LOG.debug("Got Elemental Home from classpath: {}", elementalHome.toAbsolutePath().toString());
                }
                return Optional.of(elementalHome);
            } catch (final URISyntaxException e) {
                // Catch all potential problems
                LOG.error("Could not derive Elemental home from classpath: {}", e.getMessage(), e);
            }
        }
        
        return Optional.empty();
    }

    /**
     * @deprecated use {@link #getElementalHome(String)}
     */
    @Deprecated
    public static Optional<Path> getExistHome(final String config) {
        return getElementalHome(config);
    }

    public static Optional<Path> getFromSystemProperty() {
        return Optional.ofNullable(System.getProperty(PROP_ELEMENTAL_CONFIGURATION_FILE)).map(Paths::get);
    }

	/**
     * Returns a file handle for the given path, where <code>path</code> specifies
     * the path to an Elemental configuration file or directory.
     * <br>
     * Note that relative paths are being interpreted relative to <code>elemental.home</code>
     * or the current working directory (in the case that <code>elemental.home</code> was not set).
     *
     * @param path the file path.
     *
     * @return the file handle.
     */
    public static Path lookup(final String path) {
        return lookup(path, Optional.empty());
    }
    
    /**
     * Returns a file handle for the given path, where <code>path</code> specifies
     * the path to an Elemental configuration file or directory.
     * <br>
     * If <code>parent</code> is null, then relative paths are being interpreted
     * relative to <code>elemental.home</code> or the current working directory (in
     * case <code>elemental.home</code> was not set).
     *
     * @param path path to the file or directory
     * @param parent parent directory used to lookup <code>path</code>
     * @return the file handle
     */
    public static Path lookup(final String path, final Optional<Path> parent) {
        // attempt to first resolve the path that is used for things like ~user/folder
        Path p = decodeUserHome(path);
        if (!p.isAbsolute()) {
            p = parent
                    .orElse(getElementalHome().orElse(Paths.get(System.getProperty("user.dir"))))
                    .resolve(path);
        }
        return p.normalize().toAbsolutePath();
    }
    
    
    /**
     * Resolves the given path by means of eventually replacing <code>~</code> with the users
     * home directory, taken from the system property <code>user.home</code>.
     *
     * @param path the path to resolve
     * @return the resolved path
     */
    public static Path decodeUserHome(final String path) {
        if (path != null && path.startsWith("~") && path.length() > 1) {
            return Paths.get(System.getProperty("user.home")).resolve(path.substring(1));
        } else {
            return Paths.get(path);
        }
    }

    public static @Nullable Properties loadProperties(final String propertiesFileName,
            @Nullable final Class<?> classPathRef) throws IOException {
        // 1) try and load from config path
        Path propFile = ConfigurationHelper.lookup(propertiesFileName);
        if (Files.isReadable(propFile)) {
            try (final InputStream pin = Files.newInputStream(propFile)) {
                final Properties properties = new Properties();
                properties.load(pin);
                return properties;
            }
        }

        // 2) try and load from config path set by system property
        propFile = ConfigurationHelper.getFromSystemProperty().map(p -> p.resolveSibling(propertiesFileName)).orElse(null);
        if (propFile != null && Files.isReadable(propFile)) {
            try (final InputStream pin = Files.newInputStream(propFile)) {
                final Properties properties = new Properties();
                properties.load(pin);
                return properties;
            }
        }

        if (classPathRef != null) {
            // 3) try and load from classpath classpathRef.getClassName()/client.properties
            try (final InputStream pin = classPathRef.getResourceAsStream(propertiesFileName)) {
                if (pin != null) {
                    final Properties properties = new Properties();
                    properties.load(pin);
                    return properties;
                }
            }

            // 4) try and load from classpath client.properties
            try (final InputStream pin = classPathRef.getResourceAsStream("/" + propertiesFileName)) {
                if (pin != null) {
                    final Properties properties = new Properties();
                    properties.load(pin);
                    return properties;
                }
            }
        }

        return null;
    }
}
