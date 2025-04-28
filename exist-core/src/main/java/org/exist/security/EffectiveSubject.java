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
package org.exist.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.exist.config.Configuration;
import org.exist.config.ConfigurationException;
import org.exist.security.internal.RealmImpl;
import org.exist.security.realm.Realm;
import org.exist.storage.DBBroker;

/**
 * Represents an Effective User
 * 
 * This is used during setUid and setGid operations
 * to replace the Subject used by DBBroker
 * with a subject which is potentially a composite
 * of a user and/or group
 * 
 * @author <a href="mailto:adam@existsolutions.com">Adam Retter</a>
 */
public class EffectiveSubject implements Subject {
    private final Account account;
    private final Group group;
    
    public EffectiveSubject(final Account account) {
        this(account, null);
    }
    
    public EffectiveSubject(final Account account, final Group group) {
        this.account = account;
        this.group = group;
    }
    
    @Override
    public String getRealmId() {
        return account.getRealmId();
    }
    
    @Override
    public Realm getRealm() {
        return account.getRealm();
    }
    
    @Override
    public int getId() {
        return account.getId(); //TODO is this correct or need own reserved id?
    }
    
    @Override
    public String getUsername() {
        return account.getUsername();
    }
    
    @Override
    public String getName() {
        return account.getName();
    }
    
    @Override
    public boolean authenticate(final Object credentials) {
        return false;
    }

    //<editor-fold desc="account status">
    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public boolean isExternallyAuthenticated() {
        return false;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return account.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return account.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return account.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return account.isEnabled();
    }
    
    @Override
    public void setEnabled(final boolean enabled) {
        throw new UnsupportedOperationException("You cannot change the Enabled status of the Effective User.");
    }
    //</editor-fold>

    @Override
    public String getSessionId() {
        throw new UnsupportedOperationException("The Effective User has no session!");
    }

    @Override
    public Session getSession() {
        throw new UnsupportedOperationException("The Effective User has no session!");
    }

    //<editor-fold desc="group functions">
    @Override
    public String[] getGroups() {
        if(group != null) {
            final Set<String> groups = new HashSet<>(Arrays.asList(account.getGroups()));
            groups.add(group.getName());
            return groups.toArray(new String[0]);
        } else {
            return account.getGroups();
        }
    }

    @Override
    public int[] getGroupIds() {
        if(group != null) {
            final IntOpenHashSet groupIds = new IntOpenHashSet(account.getGroupIds());
            groupIds.add(group.getId());
            return groupIds.toIntArray();
        } else {
            return account.getGroupIds();
        }
    }

    @Override
    public boolean hasDbaRole() {
        if(group != null) {
            return account.hasDbaRole() || group.getId() == RealmImpl.DBA_GROUP_ID;
        } else {
            return account.hasDbaRole();
        }
    }

    @Override
    public String getPrimaryGroup() {
        return account.getPrimaryGroup();
    }

    @Override
    public Group getDefaultGroup() {
        return account.getDefaultGroup();
    }

    @Override
    public boolean hasGroup(final String group) {
        if(this.group != null) {
            return this.group.getName().equals(group);
        } else {
            return account.hasGroup(group);
        }
    }
    
    @Override
    public Group addGroup(final String name) throws PermissionDeniedException {
        throw new UnsupportedOperationException("You cannot add a group to the Effective User");
    }

    @Override
    public Group addGroup(final Group group) throws PermissionDeniedException {
        throw new UnsupportedOperationException("You cannot add a group to the Effective User");
    }

    @Override
    public void setPrimaryGroup(final Group group) throws PermissionDeniedException {
        throw new UnsupportedOperationException("You cannot add a group to the Effective User");
    }

    @Override
    public void setGroups(final String[] groups) {
        throw new UnsupportedOperationException("You cannot set the groups of the Effective User");
    }
    
    @Override
    public void remGroup(final String group) throws PermissionDeniedException {
        throw new UnsupportedOperationException("You cannot remove a group from the Effective User");
    }
    //</editor-fold>

    @Override
    public void setPassword(final String passwd) {
        throw new UnsupportedOperationException("The Effective User has no password!");
    }

    @Override
    public void setCredential(final Credential credential) {
        throw new UnsupportedOperationException("The Effective User has no credential!");
    }

    @Override
    public String getPassword() {
        throw new UnsupportedOperationException("The Effective User has no password!");
    }

    @Override
    public String getDigestPassword() {
        throw new UnsupportedOperationException("The Effective User has no password!");
    }

    @Override
    public void assertCanModifyAccount(final Account user) throws PermissionDeniedException {
        throw new PermissionDeniedException("The Effective User account cannot be modified");
    }

    @Override
    public int getUserMask() {
        return account.getUserMask();
    }

    @Override
    public void setUserMask(final int umask) {
        throw new UnsupportedOperationException("You cannot set the UserMask of the Effective User");
    }
    
    //<editor-fold desc="metadata">
    @Override
    public String getMetadataValue(final SchemaType schemaType) {
        return account.getMetadataValue(schemaType);
    }

    @Override
    public Set<SchemaType> getMetadataKeys() {
        return account.getMetadataKeys();
    }
    
    @Override
    public void setMetadataValue(final SchemaType schemaType, final String value) {
         throw new UnsupportedOperationException("You cannot modify the metadata of the Effective User");
    }

    @Override
    public void clearMetadata() {
        throw new UnsupportedOperationException("You cannot modify the metadata of the Effective User");
    }
    //</editor-fold>

    //<editor-fold desc="persistence">
    @Override
    public void save() throws ConfigurationException, PermissionDeniedException {
        throw new UnsupportedOperationException("You cannot perist the Effective User.");
    }

    @Override
    public void save(final DBBroker broker) throws ConfigurationException, PermissionDeniedException {
        throw new UnsupportedOperationException("You cannot perist the Effective User.");
    }
    
    @Override
    public boolean isConfigured() {
        return true; //the effective user does not need configuring
    }

    @Override
    public Configuration getConfiguration() {
        return null; //the effective user does not need configuring
    }
    //</editor-fold>
}
