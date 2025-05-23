/*
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

/**
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface ACLPermission {

    enum ACE_ACCESS_TYPE {
        DENIED(01),
        ALLOWED(02);
        private final int val;

        ACE_ACCESS_TYPE(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }

        public static ACE_ACCESS_TYPE fromVal(int val) {
            for(final ACE_ACCESS_TYPE access_type : ACE_ACCESS_TYPE.values()) {
                if(access_type.getVal() == val) {
                    return access_type;
                }
            }

            throw new RuntimeException("Unknown ACE Access Type");
        }
    }

    enum ACE_TARGET {
        USER(01),
        GROUP(02);
        private final int val;

        ACE_TARGET(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }

        public static ACE_TARGET fromVal(int val) {
            for(final ACE_TARGET target : ACE_TARGET.values()) {
                if(target.getVal() == val) {
                    return target;
                }
            }

            throw new RuntimeException("Unknown ACE Target");
        }
    }

    short getVersion();

    void addACE(ACE_ACCESS_TYPE access_type, ACE_TARGET target, String name, String modeStr) throws PermissionDeniedException;

    void addACE(ACE_ACCESS_TYPE access_type, ACE_TARGET target, String name, int mode) throws PermissionDeniedException;

    void insertACE(int index, ACE_ACCESS_TYPE access_type, ACE_TARGET target, String name, String modeStr) throws PermissionDeniedException;

    void modifyACE(int index, ACE_ACCESS_TYPE access_type, String modeStr) throws PermissionDeniedException;

    void modifyACE(int index, ACE_ACCESS_TYPE access_type, int mode) throws PermissionDeniedException;

    void removeACE(int index) throws PermissionDeniedException;

    int getACECount();

    ACE_ACCESS_TYPE getACEAccessType(int index);

    ACE_TARGET getACETarget(int index);
    
    /**
     * Convenience method for getting the name of the user or group
     * of which this ace is applied to
     * @param index the index of the ACE within the ACL
     * @return the user or group that the ACE is applied for
     */
    String getACEWho(int index);
   
    int getACEMode(int index);

    /**
     * Clears all ACE's
     * @throws PermissionDeniedException if the user does not have permission to clear the ACL
     */
    void clear() throws PermissionDeniedException;
    
    boolean isCurrentSubjectCanWriteACL();

    /**
     * Determines if this ACL is equal to another ACL.
     *
     * @param other Another ACL to compare against.
     *
     * @return true if this ACL is equal to the other ACL.
     */
    boolean aclEquals(final ACLPermission other);
}
