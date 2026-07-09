/*
 * Copyright (C) 2014, Evolved Binary Ltd
 *
 * This file was originally ported from FusionDB to Elemental by
 * Evolved Binary, for the benefit of the Elemental Open Source community.
 * Only the ported code as it appears in this file, at the time that
 * it was contributed to Elemental, was re-licensed under The GNU
 * Lesser General Public License v2.1 only for use in Elemental.
 *
 * This license grant applies only to a snapshot of the code as it
 * appeared when ported, it does not offer or infer any rights to either
 * updates of this source code or access to the original source code.
 *
 * The GNU Lesser General Public License v2.1 only license follows.
 *
 * =====================================================================
 *
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
package org.exist.security.internal.aider;

import org.exist.security.Account;
import org.exist.security.Group;
import org.exist.security.Permission;
import org.exist.storage.io.VariableByteInput;

import java.io.IOException;

/**
 * An Immutable version of {@link UnixStylePermissionAider}.
 */
public class ImmutableUnixStylePermissionAider extends UnixStylePermissionAider {

    /**
     * Construct a Permission with given mode
     *
     */
    public ImmutableUnixStylePermissionAider() {
       super();
    }

    /**
     * Construct a Permission with given mode
     *
     * @param  mode  The mode
     */
    public ImmutableUnixStylePermissionAider(final int mode) {
       super(mode);
    }


    /**
     * Construct a permission with given user, group and mode
     *
     * @param user name of the owner
     * @param group name of the group
     * @param mode mode for the resource.
     */
    public ImmutableUnixStylePermissionAider(final String user, final String group, final int mode) {
        super(user, group, mode);
    }

    @Override
    public void read(final VariableByteInput istream) throws IOException {
        // no-op
    }

    @Override
    public void setGroup(final Group group) {
        // no-op
    }

    @Override
    public void setGroup(final String group) {
        // no-op
    }

    @Override
    public void setGroup(final int id) {
        // no-op
    }

    @Override
    public void setGroupFrom(final Permission other) {
        // no-op
    }

    @Override
    public void setGroupMode(final int groupMode) {
        // no-op
    }

    @Override
    public void setMode(final int mode) {
        // no-op
    }

    @Override
    public void setOtherMode(final int otherMode) {
        // no-op
    }

    @Override
    public void setOwner(final int id) {
        // no-op
    }

    @Override
    public void setOwner(final Account user) {
        // no-op
    }

    @Override
    public void setOwner(final String user) {
        super.setOwner(user);
    }

    @Override
    public void setOwnerMode(final int ownerMode) {
        // no-op
    }

    @Override
    public void setSetGid(final boolean setGid) {
        // no-op
    }

    @Override
    public void setSetUid(final boolean setUid) {
        // no-op
    }

    @Override
    public void setSticky(final boolean sticky) {
        // no-op
    }
}
