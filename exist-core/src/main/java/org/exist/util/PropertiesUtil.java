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
 */
package org.exist.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Properties;

/**
 * Utilities for working with {@link java.util.Properties}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NullMarked
public class PropertiesUtil {

    /**
     * Parse a boolean property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     */
    public static boolean getBooleanProperty(final Properties properties, final String name, final boolean defaultValue) {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(str);
    }

    /**
     * Parse a boolean property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     */
    public static boolean getBooleanProperty(final Map<String, String> properties, final String name, final boolean defaultValue) {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(str);
    }

    /**
     * Parse a boolean property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     */
    public static @Nullable Boolean getBooleanProperty(final Properties properties, final String name) {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return null;
        }
        return "true".equalsIgnoreCase(str);
    }

    /**
     * Parse a boolean property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     */
    public static @Nullable Boolean getBooleanProperty(final Map<String, String> properties, final String name) {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return null;
        }
        return "true".equalsIgnoreCase(str);
    }

    /**
     * Parse a yes/no string property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     */
    public static boolean getYesNoProperty(final Properties properties, final String name, final boolean defaultValue) {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return defaultValue;
        }
        return "yes".equalsIgnoreCase(str);
    }

    /**
     * Parse a yes/no string property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     */
    public static boolean getYesNoProperty(final Map<String, String> properties, final String name, final boolean defaultValue) {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return defaultValue;
        }
        return "yes".equalsIgnoreCase(str);
    }

    /**
     * Parse a yes/no string property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     */
    public static @Nullable Boolean getYesNoProperty(final Properties properties, final String name) {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return null;
        }
        return "yes".equalsIgnoreCase(str);
    }

    /**
     * Parse a yes/no string property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     */
    public static @Nullable Boolean getYesNoProperty(final Map<String, String> properties, final String name) {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return null;
        }
        return "yes".equalsIgnoreCase(str);
    }

    /**
     * Parse a boolean or yes/no string property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     */
    public static boolean getBooleanOrYesNoProperty(final Properties properties, final String name, final boolean defaultValue) {
        @Nullable String str = properties.getProperty(name);
        if (str == null) {
            return defaultValue;
        }

        str = str.toLowerCase();
        return "true".equals(str) || "yes".equals(str);
    }

    /**
     * Parse a boolean or yes/no string property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     */
    public static boolean getBooleanOrYesNoProperty(final Map<String, String> properties, final String name, final boolean defaultValue) {
        @Nullable String str = properties.get(name);
        if (str == null) {
            return defaultValue;
        }

        str = str.toLowerCase();
        return "true".equals(str) || "yes".equals(str);
    }

    /**
     * Parse a boolean or yes/no string property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     */
    public static @Nullable Boolean getBooleanOrYesNoProperty(final Properties properties, final String name) {
        @Nullable String str = properties.getProperty(name);
        if (str == null) {
            return null;
        }
        str = str.toLowerCase();
        return "true".equals(str) || "yes".equals(str);
    }

    /**
     * Parse a boolean or yes/no string property as a boolean value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     */
    public static @Nullable Boolean getBooleanOrYesNoProperty(final Map<String, String> properties, final String name) {
        @Nullable String str = properties.get(name);
        if (str == null) {
            return null;
        }
        str = str.toLowerCase();
        return "true".equals(str) || "yes".equals(str);
    }

    /**
     * Parse an integer property as an integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     *
     * @throws NumberFormatException if the property is not an integer.
     */
    public static int getIntegerProperty(final Properties properties, final String name, final int defaultValue) throws NumberFormatException {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return defaultValue;
        }
        return Integer.parseInt(str);
    }

    /**
     * Parse an integer property as an integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     *
     * @throws NumberFormatException if the property is not an integer.
     */
    public static int getIntegerProperty(final Map<String, String> properties, final String name, final int defaultValue) throws NumberFormatException {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return defaultValue;
        }
        return Integer.parseInt(str);
    }

    /**
     * Parse an integer property as an integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     *
     * @throws NumberFormatException if the property is not an integer.
     */
    public static @Nullable Integer getIntegerProperty(final Properties properties, final String name) throws NumberFormatException {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return null;
        }
        return Integer.valueOf(str);
    }

    /**
     * Parse an integer property as an integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     *
     * @throws NumberFormatException if the property is not an integer.
     */
    public static @Nullable Integer getIntegerProperty(final Map<String, String> properties, final String name) throws NumberFormatException {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return null;
        }
        return Integer.valueOf(str);
    }

    /**
     * Parse a positive integer property as a positive integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     *
     * @throws NumberFormatException if the property is not a positive integer.
     */
    public static int getPositiveIntegerProperty(final Properties properties, final String name, final int defaultValue) throws NumberFormatException {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return defaultValue;
        }

        final int result = Integer.parseInt(str);
        if (result < 0) {
            throw new NumberFormatException("Expected a positive integer, but found: " + str);
        }

        return result;
    }

    /**
     * Parse a positive integer property as a positive integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     *
     * @throws NumberFormatException if the property is not a positive integer.
     */
    public static int getPositiveIntegerProperty(final Map<String, String> properties, final String name, final int defaultValue) throws NumberFormatException {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return defaultValue;
        }

        final int result = Integer.parseInt(str);
        if (result < 0) {
            throw new NumberFormatException("Expected a positive integer, but found: " + str);
        }

        return result;
    }

    /**
     * Parse a positive integer property as a positive integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     *
     * @throws NumberFormatException if the property is not a positive integer.
     */
    public static @Nullable Integer getPositiveIntegerProperty(final Properties properties, final String name) throws NumberFormatException {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return null;
        }

        final int result = Integer.parseInt(str);
        if (result < 0) {
            throw new NumberFormatException("Expected a positive integer, but found: " + str);
        }

        return result;
    }

    /**
     * Parse a positive integer property as a positive integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     *
     * @throws NumberFormatException if the property is not a positive integer.
     */
    public static @Nullable Integer getPositiveIntegerProperty(final Map<String, String> properties, final String name) throws NumberFormatException {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return null;
        }

        final int result = Integer.parseInt(str);
        if (result < 0) {
            throw new NumberFormatException("Expected a positive integer, but found: " + str);
        }

        return result;
    }

    /**
     * Parse a long integer property as a long integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     */
    public static long getLongProperty(final Properties properties, final String name, final long defaultValue) throws NumberFormatException {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return defaultValue;
        }
        return Long.parseLong(str);
    }

    /**
     * Parse a long integer property as a long integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     */
    public static long getLongProperty(final Map<String, String> properties, final String name, final long defaultValue) throws NumberFormatException {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return defaultValue;
        }
        return Long.parseLong(str);
    }

    /**
     * Parse a long integer property as a long integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     */
    public static @Nullable Long getLongProperty(final Properties properties, final String name) throws NumberFormatException {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return null;
        }
        return Long.valueOf(str);
    }

    /**
     * Parse a long integer property as a long integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     */
    public static @Nullable Long getLongProperty(final Map<String, String> properties, final String name) throws NumberFormatException {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return null;
        }
        return Long.valueOf(str);
    }

    /**
     * Parse a positive long integer property as a positive long integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     *
     * @throws NumberFormatException if the property is not a positive long integer.
     */
    public static long getPositiveLongProperty(final Properties properties, final String name, final long defaultValue) throws NumberFormatException {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return defaultValue;
        }

        final long result = Long.parseLong(str);
        if (result < 0) {
            throw new NumberFormatException("Expected a positive long integer, but found: " + str);
        }

        return result;
    }

    /**
     * Parse a positive long integer property as a positive long integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     * @param defaultValue the default value to return if there is no such named property.
     *
     * @return the parsed value, or default value if there is no such named property.
     *
     * @throws NumberFormatException if the property is not a positive long integer.
     */
    public static long getPositiveLongProperty(final Map<String, String> properties, final String name, final long defaultValue) throws NumberFormatException {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return defaultValue;
        }

        final long result = Long.parseLong(str);
        if (result < 0) {
            throw new NumberFormatException("Expected a positive long integer, but found: " + str);
        }

        return result;
    }

    /**
     * Parse a positive long integer property as a positive long integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     *
     * @throws NumberFormatException if the property is not a positive long integer.
     */
    public static @Nullable Long getPositiveLongProperty(final Properties properties, final String name) throws NumberFormatException {
        @Nullable final String str = properties.getProperty(name);
        if (str == null) {
            return null;
        }

        final long result = Long.parseLong(str);
        if (result < 0) {
            throw new NumberFormatException("Expected a positive long integer, but found: " + str);
        }

        return result;
    }

    /**
     * Parse a positive long integer property as a positive long integer value.
     *
     * @param properties the properties collection.
     * @param name the name of the property to retrieve.
     *
     * @return the parsed value, or null if there is no such named property.
     *
     * @throws NumberFormatException if the property is not a positive long integer.
     */
    public static @Nullable Long getPositiveLongProperty(final Map<String, String> properties, final String name) throws NumberFormatException {
        @Nullable final String str = properties.get(name);
        if (str == null) {
            return null;
        }

        final long result = Long.parseLong(str);
        if (result < 0) {
            throw new NumberFormatException("Expected a positive long integer, but found: " + str);
        }

        return result;
    }
}
