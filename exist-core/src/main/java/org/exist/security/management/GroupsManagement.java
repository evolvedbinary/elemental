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
package org.exist.security.management;

import org.exist.EXistException;
import org.exist.config.ConfigurationException;
import org.exist.security.Group;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.DBBroker;

import javax.annotation.Nullable;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public interface GroupsManagement {

	Group addGroup(DBBroker broker, Group group) throws PermissionDeniedException, EXistException, ConfigurationException;
	
	@Nullable Group getGroup(String name);

	/**
	 * Returns true if a group of this name
	 * is known to the Security Manager.
	 *
	 * @param group the group
	 *
	 * @return true if a group with the provided name is known.
	 */
	boolean hasGroup(Group group);

	/**
	 * Returns true if a group of this name
	 * is known to the Security Manager.
	 *
	 * @param name the group name
	 *
	 * @return true if a group with the provided name is known.
	 */
	boolean hasGroup(String name);

	boolean hasGroupLocal(Group group);
	boolean hasGroupLocal(String name);

	boolean updateGroup(Group group) throws PermissionDeniedException, EXistException, ConfigurationException;
	
	boolean deleteGroup(Group group) throws PermissionDeniedException, EXistException, ConfigurationException;

}