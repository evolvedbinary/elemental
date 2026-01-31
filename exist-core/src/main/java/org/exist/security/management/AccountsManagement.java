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
import org.exist.security.PermissionDeniedException;
import org.exist.security.Account;

import javax.annotation.Nullable;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 */
public interface AccountsManagement {
	
	Account addAccount(Account account) throws PermissionDeniedException, EXistException, ConfigurationException;
	
	@Nullable Account getAccount(String name);

	/**
	 * Returns true if an account of this name
	 * is known to the Security Manager.
	 *
	 * @param account the account
	 *
	 * @return true if an account with the provided name is known.
	 */
	boolean hasAccount(Account account);

	/**
	 * Returns true if an account of this name
	 * is known to the Security Manager.
	 *
	 * @param name the account name
	 *
	 * @return true if an account with the provided name is known.
	 */
	boolean hasAccount(String name);

	boolean hasAccountLocal(Account account);
	boolean hasAccountLocal(String name);

	boolean updateAccount(Account account) throws PermissionDeniedException, EXistException, ConfigurationException;
	
	boolean deleteAccount(Account account) throws PermissionDeniedException, EXistException, ConfigurationException;
}