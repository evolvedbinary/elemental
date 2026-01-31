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
package org.exist.security.realm.ldap;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.exist.config.Configurable;
import org.exist.config.Configuration;
import org.exist.config.Configurator;
import org.exist.config.annotation.ConfigurationClass;
import org.exist.config.annotation.ConfigurationFieldAsElement;
import org.exist.security.AXSchemaType;
import org.exist.security.EXistSchemaType;
import org.exist.security.SchemaType;

import javax.annotation.Nullable;

/**
 * @author aretter
 */
@ConfigurationClass("")
public abstract class AbstractLDAPSearchPrincipal implements Configurable {

    @ConfigurationFieldAsElement("search-filter-prefix")
    protected String searchFilterPrefix = null;

    @ConfigurationFieldAsElement("search-attribute")
    protected Map<String, String> searchAttributes = new HashMap<>();

    @ConfigurationFieldAsElement("metadata-search-attribute")
    protected Map<String, String> metadataSearchAttributes = new HashMap<>();

    @ConfigurationFieldAsElement("whitelist")
    protected LDAPPrincipalWhiteList whiteList = null;

    @ConfigurationFieldAsElement("blacklist")
    protected LDAPPrincipalBlackList blackList = null;

    protected Configuration configuration;

    public AbstractLDAPSearchPrincipal(final Configuration config) {
        this.configuration = Configurator.configure(this, config);
    }

    public String getSearchFilterPrefix() {
        return searchFilterPrefix;
    }

    public String getSearchAttribute(final LDAPSearchAttributeKey ldapSearchAttributeKey) {
        return searchAttributes.get(ldapSearchAttributeKey.getKey());
    }

    public String getMetadataSearchAttribute(final SchemaType schemaType) {
        return metadataSearchAttributes.get(schemaType.getNamespace());
    }

    public Set<SchemaType> getMetadataSearchAttributeKeys() {
        @Nullable Set<SchemaType> metadataSearchAttributeKeys = null;
        for (final String key : metadataSearchAttributes.keySet()) {
            @Nullable SchemaType value = EXistSchemaType.valueOfNamespace(key);
            if (value == null) {
                value = AXSchemaType.valueOfNamespace(key);
            }

            if (value != null) {
                if (metadataSearchAttributeKeys == null) {
                    metadataSearchAttributeKeys = new HashSet<>(metadataSearchAttributes.size());
                }
                metadataSearchAttributeKeys.add(value);
            }
        }

        if (metadataSearchAttributeKeys == null) {
            return Collections.emptySet();
        }

        return metadataSearchAttributeKeys;
    }

    @Override
    public boolean isConfigured() {
        return (configuration != null);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    public LDAPPrincipalBlackList getBlackList() {
        return blackList;
    }

    public LDAPPrincipalWhiteList getWhiteList() {
        return whiteList;
    }
}
