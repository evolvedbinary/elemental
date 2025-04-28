/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 */
package org.exist.util;

import java.util.Properties;

/**
 * Simple fluent builder pattern for {@link Properties}
 */
public class PropertiesBuilder {

    private final Properties properties;

    private PropertiesBuilder(final Properties properties) {
        this.properties = properties;
    }

    public static PropertiesBuilder propertiesBuilder() {
        return new PropertiesBuilder(new Properties());
    }

    public PropertiesBuilder set(final String key, final String value) {
        properties.setProperty(key, value);
        return this;
    }

    public PropertiesBuilder put(final String key, final Object value) {
        properties.put(key, value);
        return this;
    }

    public Properties build() {
        return properties;
    }
}
