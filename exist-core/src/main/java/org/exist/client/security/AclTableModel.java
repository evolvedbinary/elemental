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
package org.exist.client.security;

import javax.swing.table.DefaultTableModel;
import org.exist.security.ACLPermission;
import org.exist.security.Permission;

/**
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class AclTableModel extends DefaultTableModel {

    private final static String[] COLUMN_NAMES = new String [] {"Target", "Subject", "Access", "Read", "Write", "Execute"};

    public AclTableModel(final Permission permission) {
        super();
        
        final Object[][] aces;    
        if (permission instanceof ACLPermission) {
            final ACLPermission aclPermission = (ACLPermission) permission;
            aces = new Object[aclPermission.getACECount()][6];
            for(int i = 0; i < aclPermission.getACECount(); i++) {
                aces[i] = new Object[]{
                    aclPermission.getACETarget(i).toString(),
                    aclPermission.getACEWho(i),
                    aclPermission.getACEAccessType(i).toString(),
                    (aclPermission.getACEMode(i) & Permission.READ) == Permission.READ,
                    (aclPermission.getACEMode(i) & Permission.WRITE) == Permission.WRITE,
                    (aclPermission.getACEMode(i) & Permission.EXECUTE) == Permission.EXECUTE,
                };
            }
        } else {
            aces = new Object[0][6];
        }
        setDataVector(aces, COLUMN_NAMES);
    }
    
    final Class[] types = new Class [] {
        java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.Boolean.class
    };

    boolean[] canEdit = new boolean [] {
        false, false, false, true, true, true
    };

    @Override
    public Class getColumnClass(int columnIndex) {
        return types [columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit [columnIndex];
    }
}
