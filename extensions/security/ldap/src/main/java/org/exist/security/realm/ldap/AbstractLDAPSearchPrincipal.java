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
package org.exist.security.realm.ldap;

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

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam retter</a>
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

    public String getMetadataSearchAttribute(final AXSchemaType axSchemaType) {
        return metadataSearchAttributes.get(axSchemaType.getNamespace());
    }


    public Set<AXSchemaType> getMetadataSearchAttributeKeys() {
        final Set<AXSchemaType> metadataSearchAttributeKeys = new HashSet<>();
        for (final String key : metadataSearchAttributes.keySet()) {
            metadataSearchAttributeKeys.add(AXSchemaType.valueOfNamespace(key));
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

    public enum LDAPSearchAttributeKey {
        NAME("name"),
        DN("dn"),
        MEMBER_OF("memberOf"),
        MEMBER("member"),
        PRIMARY_GROUP_TOKEN("primaryGroupToken"),
        PRIMARY_GROUP_ID("primaryGroupID"),
        OBJECT_SID("objectSid");

        private final String key;

        LDAPSearchAttributeKey(final String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public static LDAPSearchAttributeKey valueOfKey(final String key) {
            for (final LDAPSearchAttributeKey ldapSearchAttributeKey : LDAPSearchAttributeKey.values()) {
                if (ldapSearchAttributeKey.getKey().equals(key)) {
                    return ldapSearchAttributeKey;
                }
            }
            return null;
        }
    }
}
