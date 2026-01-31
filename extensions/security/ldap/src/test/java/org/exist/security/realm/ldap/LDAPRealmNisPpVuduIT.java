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
package org.exist.security.realm.ldap;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration Tests against an LDAP Server with NIS Schema
 * where the Elemental Security Config has:
 * <ul>
 *     <li>principal-pattern</li>
 *     <li>default-username that is unqualified and valid</li>
 * </ul>
 */
@ExtendWith(ApacheDSTestExtension.class)
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAP") }, allowAnonymousAccess = true)
@CreateDS(name = "myDS",
    loadedSchemas = {
        @LoadSchema(name = "nis"),
    },
    partitions = {
        @CreatePartition(name = "test", suffix = "dc=gb,dc=myorg,dc=com")
    }
)
@ApplyLdifFiles({"ldap-example-1.ldif"})
@AbstractLDAPRealmNisIT.ElementalDbSecurityConfig(fileName = "ldap-example-1-pp-vudu.config.xml")
public class LDAPRealmNisPpVuduIT extends AbstractLDAPRealmNisIT {

    public LDAPRealmNisPpVuduIT() {
        super(PrincipalPattern.PRESENT_VALID, DefaultUser.PRESENT_VALID_UNQUALIFIED);
    }
}
