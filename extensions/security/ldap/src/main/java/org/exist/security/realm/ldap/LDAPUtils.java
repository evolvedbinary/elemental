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

import javax.annotation.Nullable;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
class LDAPUtils {
    private static final Logger LOG = LogManager.getLogger(LDAPUtils.class);

    static void closeContext(@Nullable final LdapContext ctx) {
        if (ctx == null) {
            return;
        }

        try {
            ctx.close();
        } catch (final NamingException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("Exception while closing LDAP context. ", e);
            }
        }
    }

    /**
     * Returns true if the string is fully qualified.
     *
     * @param string the string to test
     *
     * @return true if the string is fully qualified, false otherwise
     */
    public static boolean isFullyQualified(final String string) {
        return string.indexOf('=') > - 1;
    }

    /**
     * Formats a username.
     *
     * If the provided username is unqualified and a principal pattern has
     * been set then the username is formatted with the principal pattern.
     *
     * @param username the username to format
     * @param principalPatternFormat the formatter or null if no principal pattern is available
     *
     * @return the formatted username
     */
    public static @Nullable String formatUsername(@Nullable final String username, @Nullable final MessageFormat principalPatternFormat) {
        if (username == null) {
            return null;
        }

        if (isFullyQualified(username) || principalPatternFormat == null) {
            return username;
        }

        return principalPatternFormat.format(new String[]{ username });
    }

    /**
     * Extracts the base from a distinguished name.
     *
     * e.g. Given: `uid=user2,ou=Users,dc=gb,dc=myorg,dc=com`
     * this function will return: `ou=Users,dc=gb,dc=myorg,dc=com`.
     *
     * @param dn the distinguished name
     *
     * @return the base of the distinguished name
     */
    public static @Nullable String extractBaseFromDn(@Nullable final String dn) {
        if (dn == null) {
            return null;
        }

        StringBuilder base = null;

        final String[] attributes = dn.split(",");
        for (final String attribute : attributes) {
            if (attribute.startsWith("ou=") || attribute.startsWith("dc=")) {
                if (base != null) {
                    base.append(',');
                } else {
                    base = new StringBuilder();
                }
                base.append(attribute);
            }
        }

        if (base == null) {
            return null;
        }

        return base.toString();
    }

    /**
     * Extracts the search attributes from a distinguished name.
     *
     * e.g. Given: `uid=user2,ou=Users,dc=gb,dc=myorg,dc=com`
     * this function will return: `uid=user2`.
     *
     * @param dn the distinguished name
     *
     * @return the Search Attributes from the distinguished name
     */
    public static @Nullable SearchAttribute[] extractSearchAttributesFromDn(@Nullable final String dn) {
        if (dn == null) {
            return null;
        }

        List<SearchAttribute> searchAttributes = null;

        final String[] attributes = dn.split(",");
        for (final String attribute : attributes) {
            if (!attribute.startsWith("ou=") && !attribute.startsWith("dc=") && attribute.trim().length() > 0) {
                final String[] attributeKeyValue = attribute.split("=");
                if (attributeKeyValue.length == 2) {
                    if (searchAttributes == null) {
                        searchAttributes = new ArrayList<>();
                    }
                    searchAttributes.add(new SearchAttribute(attributeKeyValue[0], attributeKeyValue[1]));
                }
            }
        }

        if (searchAttributes == null) {
            return null;
        }

        return searchAttributes.toArray(new SearchAttribute[0]);
    }

    /**
     * Extracts the pricipal from a distinguished name.
     *
     * e.g. Given: `uid=user2,ou=Users,dc=gb,dc=myorg,dc=com`
     * this function will return: `user2`.
     *
     * @param dn the distinguished name
     *
     * @return the principle
     */
    public static @Nullable String extractPrincipalFromDn(@Nullable final String dn) {
        if (dn == null) {
            return null;
        }

        if (!isFullyQualified(dn)) {
            return dn;
        }

        final String[] attributes = dn.split(",");
        if (attributes.length == 0) {
            return dn;
        }

        final String[] attributeKeyValue = attributes[0].split("=");
        if (attributeKeyValue.length != 2) {
            return dn;
        }

        return attributeKeyValue[1];
    }
}
