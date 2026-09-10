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
package org.exist.xslt;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.util.Configuration;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.jcip.annotations.ThreadSafe;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.trans.XPathException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author <a href="mailto:alan@evolvedbinary.com">Alan Paxton</a>
 */
@ThreadSafe
public final class SaxonConfiguration {

  private static final Logger LOG = LogManager.getLogger(SaxonConfiguration.class);

  public static final String SAXON_CONFIGURATION_ELEMENT_NAME = "saxon";
  public static final String SAXON_CONFIGURATION_FILE_ATTRIBUTE = "configuration-file";
  public static final String SAXON_CONFIGURATION_FILE_PROPERTY = "saxon.configuration";
  private static final String SAXON_DEFAULT_SAXON_CONFIG_FILE = "saxon-config.xml";

  private static volatile SaxonConfiguration DEFAULT_SAXON_CONFIGURATION;

  private static final Cache<Long, SaxonConfiguration> SAXON_CONFIGURATION_CACHE = Caffeine.newBuilder()
          .maximumSize(8)
          .weakValues()
          .build();

  private final @Nullable Path saxonConfigFilePath;
  private final @Nullable List<Tuple2<String, Object>> configurationProperties;
  private final net.sf.saxon.Configuration configuration;
  private final Processor processor;

  private SaxonConfiguration(@Nullable final Path saxonConfigFile, @Nullable final List<Tuple2<String, Object>> configurationProperties, final net.sf.saxon.Configuration configuration) {
    this.saxonConfigFilePath = saxonConfigFile;
    this.configurationProperties = configurationProperties != null ? Collections.unmodifiableList(configurationProperties) : null;
    this.configuration = configuration;
    this.processor = new Processor(configuration);
    //TODO (AP) This is a better place to configure URI/Resource resolution for Saxon within Elemental, at present the configuration for Saxon to resolve xmldb:exist: URIs is restricted to fn:transform
  }

  /**
   * Get the path to the Saxon config file, if a config file was used.
   *
   * @return the path to the Saxon config file, or null if not config file was used.
   */
  public @Nullable Path getSaxonConfigFilePath() {
    return saxonConfigFilePath;
  }

  /**
   * Get any additional Saxon configuration properties
   *
   * @return the additional Saxon configuration properties, or null if there are none.
   */
  public @Nullable List<Tuple2<String, Object>> getConfigurationProperties() {
    return configurationProperties;
  }

  /**
   * Get the Saxon API's {@link net.sf.saxon.Configuration} object.
   *
   * @return Saxon internal configuration object
   */
  public net.sf.saxon.Configuration getConfiguration() {
    return configuration;
  }

  /**
   * Get the Saxon API's {@link Processor} through which Saxon operations
   * such as transformation can be effected.
   *
   * @return the Saxon {@link Processor} associated with the configuration.
   */
  public Processor getProcessor() {
    return processor;
  }

  /**
   * Gets a Saxon configuration that is unique for the configuration and configuration properties.
   *
   * May either load a new configuration or return a cached Saxon configuration that can be re-used.
   *
   * @param elementalConfiguration the database configuration.
   * @param configurationProperties any additional configuration properties for Saxon, or null.
   *
   * @return the Saxon configuration.
   */
  public static SaxonConfiguration getConfiguration(final Configuration elementalConfiguration, @Nullable final List<Tuple2<String, Object>> configurationProperties) {
    final @Nullable Path saxonConfigFile = getSaxonConfigFile(elementalConfiguration);

    if (saxonConfigFile != null && configurationProperties == null) {
      // NOTE(AR) the default Saxon configuration for Elemental has been requested
      if (DEFAULT_SAXON_CONFIGURATION == null) {
        synchronized (SaxonConfiguration.class) {
          if (DEFAULT_SAXON_CONFIGURATION == null) {
            DEFAULT_SAXON_CONFIGURATION = newSaxonConfiguration(saxonConfigFile, configurationProperties);
          }
        }
      }
      return DEFAULT_SAXON_CONFIGURATION;
    }

    final long cacheKey = cacheKey(saxonConfigFile, configurationProperties);
    return SAXON_CONFIGURATION_CACHE.get(cacheKey, key -> newSaxonConfiguration(saxonConfigFile, configurationProperties));
  }

  /**
   * Calculate a key for {@link #SAXON_CONFIGURATION_CACHE}.
   *
   * @param saxonConfigFile The path to a Saxon configuration file, or null.
   * @param configurationProperties any additional configuration properties for Saxon, or null.
   *
   * @return the cache key.
   */
  private static long cacheKey(final @Nullable Path saxonConfigFile, @Nullable final List<Tuple2<String, Object>> configurationProperties) {
    return Objects.hash(saxonConfigFile, configurationProperties);
  }

  /**
   * Load the Saxon {@link net.sf.saxon.Configuration} from a configuration file when it is first needed;
   * if we cannot find a configuration file (and license) to give to Saxon, it (Saxon) may still be able to find
   * something by searching in more "well-known to Saxon" locations.
   *
   * @param saxonConfigFile The path to a Saxon configuration file, or null.
   * @param configurationProperties any additional configuration properties for Saxon, or null.
   *
   * @return a freshly loaded Saxon configuration
   */
  private static SaxonConfiguration newSaxonConfiguration(final @Nullable Path saxonConfigFile, @Nullable final List<Tuple2<String, Object>> configurationProperties) {
    @Nullable net.sf.saxon.Configuration saxonConfiguration = null;
    if (saxonConfigFile != null) {
      saxonConfiguration = readSaxonConfigurationFile(saxonConfigFile);
    }

    if (saxonConfiguration == null) {
      LOG.warn("Elemental could not find any Saxon configuration:\n" +
              "No Saxon configuration file in configuration item " + SAXON_CONFIGURATION_FILE_PROPERTY + "\n" +
              "No default Elemental Saxon configuration file " + SAXON_DEFAULT_SAXON_CONFIG_FILE);

      saxonConfiguration = net.sf.saxon.Configuration.newConfiguration();
    }

    if (configurationProperties != null) {
      for (final Tuple2<String, Object> configurationProperty : configurationProperties) {
        saxonConfiguration.setConfigurationProperty(configurationProperty._1, configurationProperty._2);
      }
    }

    reportLicensedFeatures(saxonConfiguration);

    return new SaxonConfiguration(saxonConfigFile, configurationProperties, saxonConfiguration);
  }

  private static @Nullable net.sf.saxon.Configuration readSaxonConfigurationFile(final Path saxonConfigFile) {
    try (final InputStream is = Files.newInputStream(saxonConfigFile)) {
      return net.sf.saxon.Configuration.readConfiguration(new StreamSource(is));
    } catch (final XPathException | IOException e) {
      LOG.warn("Saxon could not read the configuration file: " + saxonConfigFile + ", with error: " + e.getMessage(), e);
    } catch (RuntimeException runtimeException) {
      if (runtimeException.getCause() instanceof ClassNotFoundException e) {
        LOG.warn("Saxon could not honour the configuration file: " + saxonConfigFile + ", with class not found error: " + e.getMessage() + ". You may need to install the SaxonPE or SaxonEE JAR in Elemental.");
      } else {
        throw runtimeException;
      }
    }
    return null;
  }

  private static void reportLicensedFeatures(final net.sf.saxon.Configuration configuration) {
    configuration.displayLicenseMessage();

    final StringBuilder sb = new StringBuilder();
    if (configuration.isLicensedFeature(net.sf.saxon.Configuration.LicenseFeature.SCHEMA_VALIDATION)) {
      sb.append(" SCHEMA_VALIDATION");
    }
    if (configuration.isLicensedFeature(net.sf.saxon.Configuration.LicenseFeature.ENTERPRISE_XSLT)) {
      sb.append(" ENTERPRISE_XSLT");
    }
    if (configuration.isLicensedFeature(net.sf.saxon.Configuration.LicenseFeature.ENTERPRISE_XQUERY)) {
      sb.append(" ENTERPRISE_XQUERY");
    }
    if (configuration.isLicensedFeature(net.sf.saxon.Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
      sb.append(" PROFESSIONAL_EDITION");
    }
    if (sb.isEmpty()) {
      LOG.info("Saxon - no licensed features reported.");
    } else {
      LOG.info("Saxon - licensed features are" + sb + ".");
    }
  }

  /**
   * Resolve a possibly relative configuration file;
   * if it is relative, it is relative to the current elemental configuration (conf.xml)
   *
   * @param elementalConfiguration configuration to which this file may be relative
   * @param filename the file we are trying to resolve
   * @return the input file, if it is absolute. a file relative to conf.xml, if the input file is relative
   */
  private static Path resolveConfigurationFile(final Configuration elementalConfiguration, final String filename) {
    final Path configurationFile = Paths.get(filename);
    if (configurationFile.isAbsolute()) {
      return configurationFile;
    }

    final Optional<Path> configPath = elementalConfiguration.getConfigFilePath();
    return configPath.map(p -> p.getParent().resolve(configurationFile)).orElse(configurationFile);
  }

  public static @Nullable Path getSaxonConfigFile(final Configuration elementalConfiguration) {
    if (elementalConfiguration.getProperty(SAXON_CONFIGURATION_FILE_PROPERTY) instanceof String saxonConfigurationFile) {
      final Path configurationFile = resolveConfigurationFile(elementalConfiguration, saxonConfigurationFile);
      if (Files.isReadable(configurationFile)) {
        return configurationFile;
      } else {
        LOG.warn("Configuration item " + SAXON_CONFIGURATION_FILE_PROPERTY + " : " + configurationFile +
            " does not refer to a readable file. Continuing search for Saxon configuration.");
      }
    }

    final Path configurationFile = resolveConfigurationFile(elementalConfiguration, SAXON_DEFAULT_SAXON_CONFIG_FILE);
    if (Files.isReadable(configurationFile)) {
      return configurationFile;
    }

    return null;
  }
}
