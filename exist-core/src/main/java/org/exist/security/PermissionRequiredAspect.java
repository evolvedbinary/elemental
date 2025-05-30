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
package org.exist.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import static org.exist.security.PermissionRequired.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@Aspect
public class PermissionRequiredAspect {

    
    
    @Pointcut("execution(* *(@org.exist.security.PermissionRequired (*),..)) && args(o,..) && this(permission)")
    public void methodParameterWithPermissionRequired(Permission permission, Object o) {
        
    }
    
    @Before("methodParameterWithPermissionRequired(permission, o)")
    public void enforcePermissionsOnParameter(JoinPoint joinPoint, Permission permission, Object o) throws PermissionDeniedException {
        
        //TODO(AR) the next two lines can be replaced when this aspectj bug is closed - https://bugs.eclipse.org/bugs/show_bug.cgi?id=259416
        final MethodSignature ms = (MethodSignature)joinPoint.getSignature(); 
        final PermissionRequired parameterPermissionRequired = (PermissionRequired)ms.getMethod().getParameterAnnotations()[0][0];
        
        // 1) check if we should allow DBA access
        if(((parameterPermissionRequired.user() & IS_DBA) == IS_DBA) && permission.isCurrentSubjectDBA()) {
            return;
        }
        
        // 2) check if the user is in the target group
        if((parameterPermissionRequired.user() & IS_MEMBER) == IS_MEMBER) {
            final Integer groupId = (Integer)o;
            if(permission.isCurrentSubjectInGroup(groupId)) {
               return; 
            }
        }

        //  3) check if we should allow access when POSIX_CHOWN_RESTRICTED is not set
        if((parameterPermissionRequired.user() & NOT_POSIX_CHOWN_RESTRICTED) == NOT_POSIX_CHOWN_RESTRICTED
                && !permission.isPosixChownRestricted()) {
            final PermissionRequired methodPermissionRequired = ms.getMethod().getAnnotation(PermissionRequired.class);
            if ((methodPermissionRequired.user() & IS_OWNER) == IS_OWNER && permission.isCurrentSubjectOwner()) {
                return;
            }
        }

        // 4) check if we are looking for setGID
        if((parameterPermissionRequired.mode() & IS_SET_GID) == IS_SET_GID) {
            final Permission other = (Permission)o;
            if(other.isSetGid()) {
                return;
            }
        }
            
        throw new PermissionDeniedException("You must be a member of the group you are changing the item to");        
    }
    
    @Pointcut("execution(@org.exist.security.PermissionRequired * *(..)) && this(permission) && @annotation(permissionRequired)")
    public void methodWithPermissionRequired(Permission permission, PermissionRequired permissionRequired) {
    }

    @Before("methodWithPermissionRequired(permission, permissionRequired)")
    public void enforcePermissions(JoinPoint joinPoint, Permission permission, PermissionRequired permissionRequired) throws PermissionDeniedException {

        //1) check if we should allow DBA access
        if(((permissionRequired.user() & IS_DBA) == IS_DBA) && permission.isCurrentSubjectDBA()) {
            return;
        }

        //2) check for owner access
        if((permissionRequired.user() & IS_OWNER) == IS_OWNER && permission.isCurrentSubjectOwner()) {
            if(permissionRequired.group() == UNDEFINED) {
                return;
            } else {
                //check for group memebership
                if(permissionRequired.group() == IS_MEMBER && permission.isCurrentSubjectInGroup()) {
                    return;
                }
            }
        }

        //3) check for group access
        if(permissionRequired.user() == UNDEFINED && permissionRequired.group() != UNDEFINED) {
            if(permissionRequired.group() == IS_MEMBER && permission.isCurrentSubjectInGroup()) {
                return;
            }
        }
        
        //4) check for acl mode access
        if(permission instanceof ACLPermission && permissionRequired.mode() != UNDEFINED) {
            if((permissionRequired.mode() & ACL_WRITE) == ACL_WRITE && ((ACLPermission)permission).isCurrentSubjectCanWriteACL()) {
                return;
            }
        }

        throw new PermissionDeniedException("You do not have appropriate access rights to modify permissions on this object");
    }

    //TODO(AR) change Pointcut so that @annotation values are directly bound. see - https://bugs.eclipse.org/bugs/show_bug.cgi?id=347684
    /*
    @Pointcut("execution(@org.exist.security.PermissionRequired * *(..)) && this(permission) && @annotation(org.exist.security.PermissionRequired(mode,user,group))")
    public void methodWithPermissionRequired(Permission permission, int mode, int user, int group) {
    }

    @Before("methodWithPermissionRequired(permission, mode, user, group)")
    public void enforcePermissions(JoinPoint joinPoint, Permission permission, int mode, int user, int group) {
        System.out.println("POINTCUT");
    }*/
}