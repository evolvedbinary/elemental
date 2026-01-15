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

enum LDAPSearchAttributeKey {

    /**
     * The system name of the principal.
     * For an Account, this is the Username,
     * and for a Group this is the Groupname.
     *
     * Account examples:
     * <ul>
     *     <li>For NIS/POSIX use <code>uid</code>.</li>
     *     <li>For Active Directory use <code>sAMAccountName</code>.</li>
     * </ul>
     *
     * Group examples:
     * <ul>
     *     <li>For NIS/POSIX use <code>cn</code>.</li>
     *     <li>For Active Directory use <code>sAMAccountName</code>.</li>
     * </ul>
     */
    NAME("name"),

    /**
     * The Distinguished Name of the LDAP object.
     *
     * Account examples:
     * <ul>
     *     <li>For NIS/POSIX use <code>uid</code>.</li>
     *     <li>For Active Directory use <code>distinguishedName</code>.</li>
     * </ul>
     *
     * Group examples:
     * <ul>
     *     <li>For NIS/POSIX this is not needed.</li>
     *     <li>For Active Directory use <code>distinguishedName</code>.</li>
     * </ul>
     */
    DN("dn"),


    /**
     * Present only on the Account, indicates that the Account is a member of a Group.
     *
     * Account examples:
     * <ul>
     *      <li>For NIS/POSIX this is not needed.</li>
     *      <li>For Active Directory use <code>memberOf</code>.</li>
     * </ul>
     */
    MEMBER_OF("memberOf"),

    /**
     * Present only on the Account, indicates that the Account has a Primary Group.
     *
     * Account examples:
     * <ul>
     *      <li>For NIS/POSIX use <code>gidNumber</code>.</li>
     *      <li>For Active Directory use <code>primaryGroupID</code>.</li>
     * </ul>
     */
    PRIMARY_GROUP_ID("primaryGroupID"),

    /**
     * Currently unused.
     */
    PRIMARY_GROUP_TOKEN("primaryGroupToken"),

    /**
     * Present only on the Group, appears zero-or more times in the directory to indicate the members of a group.
     *
     * Account examples:
     * <ul>
     *      <li>For NIS/POSIX use <code>memberUid</code>.</li>
     *      <li>For Active Directory use <code>member</code>.</li>
     * </ul>
     */
    MEMBER("member"),

    /**
     * A unique id for the Principal.
     *
     * Account examples:
     * <ul>
     *     <li>For NIS/POSIX use <code>uidNumber</code>.</li>
     *     <li>For Active Directory use <code>objectSid</code>.</li>
     * </ul>
     *
     * Group examples:
     * <ul>
     *     <li>For NIS/POSIX use <code>gidNumber</code>.</li>
     *     <li>For Active Directory use <code>objectSid</code>.</li>
     * </ul>
     */
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
