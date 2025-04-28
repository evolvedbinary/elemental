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

import org.exist.config.Configuration;
import org.exist.config.ConfigurationException;
import org.exist.config.annotation.ConfigurationClass;
import org.exist.security.Account;
import org.exist.security.Group;
import org.exist.security.PermissionDeniedException;
import org.exist.security.AbstractRealm;
import org.exist.security.internal.AccountImpl;
import org.exist.storage.DBBroker;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam retter</a>
 */
@ConfigurationClass("account")
public class LDAPAccountImpl extends AccountImpl {

    public LDAPAccountImpl(final AbstractRealm realm, final Configuration configuration) throws ConfigurationException {
        super(realm, configuration);
    }

    public LDAPAccountImpl(final DBBroker broker, final AbstractRealm realm, final AccountImpl from_user) throws ConfigurationException {
        super(broker, realm, from_user);
    }

    public LDAPAccountImpl(final DBBroker broker, final AbstractRealm realm, final int id, final Account from_user) throws ConfigurationException, PermissionDeniedException {
        super(broker, realm, id, from_user);
    }

    public LDAPAccountImpl(final DBBroker broker, final AbstractRealm realm, final String name) throws ConfigurationException {
        super(broker, realm, name);
    }

    public LDAPAccountImpl(final DBBroker broker, final AbstractRealm realm, final int id, final String name, final String password) throws ConfigurationException {
        super(broker, realm, id, name, password);
    }

    LDAPAccountImpl(final AbstractRealm realm, final Configuration config, final boolean removed) throws ConfigurationException {
        super(realm, config, removed);
    }

    @Override
    public Group addGroup(final Group group) throws PermissionDeniedException {
        if (group instanceof LDAPGroupImpl) {
            //TODO
            //we dont support writes to LDAP yet!
            return null;
        } else {
            //adds an LDAP User to a group from a different Realm
            return super.addGroup(group);
        }
    }

    @Override
    public Group addGroup(final String name) throws PermissionDeniedException {
        Group group = getRealm().getGroup(name);

        //allow LDAP users to have groups from other realms
        if (group == null) {
            //if the group is not present in this realm, look externally
            group = getRealm().getExternalGroup(name);
        }
        return addGroup(group);
    }
}