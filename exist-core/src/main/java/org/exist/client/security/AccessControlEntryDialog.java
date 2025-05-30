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
package org.exist.client.security;

import java.util.*;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;
import org.exist.client.DialogCompleteWithResponse;
import org.exist.client.DialogWithResponse;
import org.exist.client.InteractiveClient;
import org.exist.security.ACLPermission.ACE_ACCESS_TYPE;
import org.exist.security.ACLPermission.ACE_TARGET;
import org.exist.security.Account;
import org.exist.security.Permission;
import org.exist.security.internal.aider.ACEAider;
import org.exist.xmldb.UserManagementService;
import org.xmldb.api.base.XMLDBException;

/**
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class AccessControlEntryDialog extends javax.swing.JFrame implements DialogWithResponse<ACEAider> {
    
    private final UserManagementService userManagementService;
    
    private DefaultTableModel permissionTableModel = null;
    private DefaultComboBoxModel<String> usernameModel;
    private final Set<String> allUsernames;
    private DefaultComboBoxModel<String> groupNameModel = null;
    private final Set<String> allGroupNames;
    private final List<DialogCompleteWithResponse<ACEAider>> dialogCompleteWithResponseCallbacks = new ArrayList<>();

    public AccessControlEntryDialog(final UserManagementService userManagementService, final String title) throws XMLDBException {
        this.userManagementService = userManagementService;
        this.setIconImage(InteractiveClient.getElementalIcon(getClass()).getImage());
        allUsernames = new HashSet<>();
        for(final Account account : userManagementService.getAccounts()) {
            allUsernames.add(account.getName());
        }
        
        allGroupNames = new HashSet<>();
        allGroupNames.addAll(Arrays.asList(userManagementService.getGroups()));
        
        initComponents();
        setTitle(title);
    }
    
    private DefaultTableModel getPermissionTableModel() {
        if(permissionTableModel == null) {
            permissionTableModel = new DefaultTableModel(
                new Object[][]{
                    new Object[]{false, false, false}
                },
                new String[] { "Read", "Write", "Execute" }
            ){
                @Override
                public Class getColumnClass(int columnIndex) {
                    return Boolean.class;
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return true;
                }
            };
        }
        
        return permissionTableModel;
    }
    
    private ComboBoxModel<String> getUsernameModel() {
        if(usernameModel == null) {
            usernameModel = new DefaultComboBoxModel<>();
            usernameModel.addElement("");
            for(final String username : allUsernames) {
               usernameModel.addElement(username);
            }
        }
        
        return usernameModel;
    }
    
    private ComboBoxModel<String> getGroupNameModel() {
        if(groupNameModel == null) {
            groupNameModel = new DefaultComboBoxModel<>();
            groupNameModel.addElement("");
            for(final String groupName : allGroupNames) {
               groupNameModel.addElement(groupName);
            }
        }
        
        return groupNameModel;
    }
    
    private boolean isValidUsername(final String username) {
        return allUsernames.contains(username);
    }
    
    private boolean isValidGroupName(final String groupName) {
        return allGroupNames.contains(groupName);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTarget = new javax.swing.JLabel();
        cmbTarget = new javax.swing.JComboBox();
        lblUsername = new javax.swing.JLabel();
        cmbUsername = new javax.swing.JComboBox<>();
        AutoCompletion.enable(cmbUsername);
        lblGroupName = new javax.swing.JLabel();
        cmbGroupName = new javax.swing.JComboBox<>();
        AutoCompletion.enable(cmbGroupName);
        lblAccess = new javax.swing.JLabel();
        cmbAccess = new javax.swing.JComboBox();
        lblPermission = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblPermission = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        btnCreate = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTarget.setText("Target:");

        cmbTarget.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "USER", "GROUP" }));
        cmbTarget.addActionListener(this::cmbTargetActionPerformed);

        lblUsername.setText("Username:");

        cmbUsername.setEditable(true);
        cmbUsername.setModel(getUsernameModel());
        cmbUsername.addActionListener(this::cmbUsernameActionPerformed);

        lblGroupName.setText("Group:");

        cmbGroupName.setEditable(true);
        cmbGroupName.setModel(getGroupNameModel());
        cmbGroupName.setEnabled(false);
        cmbGroupName.addActionListener(this::cmbGroupNameActionPerformed);

        lblAccess.setText("Access:");

        cmbAccess.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ALLOWED", "DENIED" }));

        lblPermission.setText("Permission");

        tblPermission.setModel(getPermissionTableModel());
        tblPermission.setRowSelectionAllowed(false);
        jScrollPane1.setViewportView(tblPermission);

        btnCreate.setText("Create");
        btnCreate.addActionListener(this::btnCreateActionPerformed);

        btnClose.setText("Close");
        btnClose.addActionListener(this::btnCloseActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblPermission)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblUsername)
                            .addComponent(lblTarget)
                            .addComponent(lblGroupName)
                            .addComponent(lblAccess))
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cmbAccess, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(cmbTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(cmbUsername, 0, 257, Short.MAX_VALUE)
                                .addComponent(cmbGroupName, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                .addContainerGap(24, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnClose)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCreate)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTarget)
                    .addComponent(cmbTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblUsername)
                    .addComponent(cmbUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblGroupName)
                    .addComponent(cmbGroupName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbAccess, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblAccess))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblPermission)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCreate)
                    .addComponent(btnClose))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateActionPerformed

        final ACE_TARGET target = ACE_TARGET.valueOf((String)cmbTarget.getSelectedItem());
        final String who;
        if(target == ACE_TARGET.USER) {
            who =(String)cmbUsername.getSelectedItem();
            if(!isValidUsername(who)) {
                return;
            }
        } else {
            who = (String)cmbGroupName.getSelectedItem();
            if(!isValidGroupName(who)) {
                return;
            }
        }
        
        final ACE_ACCESS_TYPE accessType = ACE_ACCESS_TYPE.valueOf((String)cmbAccess.getSelectedItem());
        int mode = 0;
        if((Boolean)tblPermission.getValueAt(0, 0)) {
            mode |= Permission.READ;
        }
        if((Boolean)tblPermission.getValueAt(0, 1)) {
            mode |= Permission.WRITE;
        }
        if((Boolean)tblPermission.getValueAt(0, 2)) {
            mode |= Permission.EXECUTE;
        }
        
        final ACEAider ace = new ACEAider(accessType, target, who, mode);
        for(final DialogCompleteWithResponse<ACEAider> callback : getDialogCompleteWithResponseCallbacks()) {
            callback.complete(ace);
        }
        
        setVisible(false);
        dispose();
    }//GEN-LAST:event_btnCreateActionPerformed

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCloseActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_btnCloseActionPerformed

    private void cmbTargetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbTargetActionPerformed
        final ACE_TARGET aceTarget = ACE_TARGET.valueOf((String)cmbTarget.getSelectedItem());
        switch(aceTarget) {
            case USER:
                cmbGroupName.setEnabled(false);
                cmbUsername.setEnabled(true);
                break;
                
            case GROUP:
                cmbUsername.setEnabled(false);
                cmbGroupName.setEnabled(true);
                break;
        }
    }//GEN-LAST:event_cmbTargetActionPerformed

    private void cmbUsernameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbUsernameActionPerformed
        final String currentUsername = (String)cmbUsername.getSelectedItem();
        final boolean isValid = isValidUsername(currentUsername);
        btnCreate.setEnabled(isValid);
    }//GEN-LAST:event_cmbUsernameActionPerformed

    private void cmbGroupNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbGroupNameActionPerformed
        final String currentGroupName = (String)cmbGroupName.getSelectedItem();
        final boolean isValid = isValidGroupName(currentGroupName);
        btnCreate.setEnabled(isValid);
    }//GEN-LAST:event_cmbGroupNameActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnCreate;
    private javax.swing.JComboBox cmbAccess;
    private javax.swing.JComboBox<String> cmbGroupName;
    private javax.swing.JComboBox cmbTarget;
    private javax.swing.JComboBox<String> cmbUsername;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblAccess;
    private javax.swing.JLabel lblGroupName;
    private javax.swing.JLabel lblPermission;
    private javax.swing.JLabel lblTarget;
    private javax.swing.JLabel lblUsername;
    private javax.swing.JTable tblPermission;
    // End of variables declaration//GEN-END:variables

    private List<DialogCompleteWithResponse<ACEAider>> getDialogCompleteWithResponseCallbacks() {
        return dialogCompleteWithResponseCallbacks;
    }
    
    @Override
    public void addDialogCompleteWithResponseCallback(final DialogCompleteWithResponse<ACEAider> dialogCompleteWithResponseCallback) {
        getDialogCompleteWithResponseCallbacks().add(dialogCompleteWithResponseCallback);
    }
}
