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
package org.exist.security;

import org.exist.security.Permission;

import static org.exist.security.PermissionRequired.*;
import static org.exist.security.PermissionRequired.ACL_WRITE;
import static org.exist.security.PermissionRequired.IS_MEMBER;
import static org.exist.security.PermissionRequired.UNDEFINED;

/**
 * Checks permissions on operations annotated with {@link PermissionRequired}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class PermissionRequiredCheck {

    public static void checkMethodPermissions(final Permission permission, final byte methodRequiresUser, final byte methodRequiresGroup, final byte methodRequiresMode) throws PermissionDeniedException {
        //1) check if we should allow DBA access
        if (((methodRequiresUser & IS_DBA) == IS_DBA) && permission.isCurrentSubjectDBA()) {
            return;
        }

        //2) check for owner access
        if ((methodRequiresUser & IS_OWNER) == IS_OWNER && permission.isCurrentSubjectOwner()) {
            if (methodRequiresGroup == UNDEFINED) {
                return;
            } else {
                //check for group membership
                if (methodRequiresGroup == IS_MEMBER && permission.isCurrentSubjectInGroup()) {
                    return;
                }
            }
        }

        //3) check for group access
        if (methodRequiresUser == UNDEFINED && methodRequiresGroup != UNDEFINED) {
            if (methodRequiresGroup == IS_MEMBER && permission.isCurrentSubjectInGroup()) {
                return;
            }
        }

        //4) check for acl mode access
        if (permission instanceof ACLPermission && methodRequiresMode != UNDEFINED) {
            if ((methodRequiresMode & ACL_WRITE) == ACL_WRITE && ((ACLPermission) permission).isCurrentSubjectCanWriteACL()) {
                return;
            }
        }

        throw new PermissionDeniedException("You do not have appropriate access rights to modify permissions on this object");
    }

    public static void checkMethodParameterPermissions(final Permission permission, final byte methodRequiresUser, final byte methodRequiresGroup, final byte methodRequiresMode, final Object parameter, final byte parameterRequiresUser, final byte parameterRequiresGroup, final byte parameterRequiresMode) throws PermissionDeniedException {
        // 1) check if we should allow DBA access
        if (((parameterRequiresUser & IS_DBA) == IS_DBA) && permission.isCurrentSubjectDBA()) {
            return;
        }

        // 2) check if the user is in the target group
        if ((parameterRequiresUser & IS_MEMBER) == IS_MEMBER) {
            final Integer groupId = (Integer) parameter;
            if (permission.isCurrentSubjectInGroup(groupId)) {
                return;
            }
        }

        //  3) check if we should allow access when POSIX_CHOWN_RESTRICTED is not set
        if ((parameterRequiresUser & NOT_POSIX_CHOWN_RESTRICTED) == NOT_POSIX_CHOWN_RESTRICTED
                && !permission.isPosixChownRestricted()) {
            if ((methodRequiresUser & IS_OWNER) == IS_OWNER && permission.isCurrentSubjectOwner()) {
                return;
            }
        }

        // 4) check if we are looking for setGID
        if ((parameterRequiresMode & IS_SET_GID) == IS_SET_GID) {
            final Permission other = (Permission) parameter;
            if (other.isSetGid()) {
                return;
            }
        }

        throw new PermissionDeniedException("You must be a member of the group you are changing the item to");
    }
}
