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
package org.exist.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.exist.xmldb.EXistResource;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;

public class CollectionScanner {

	public final static EXistResource[] scan(Collection current, String vpath, String pattern)
	throws XMLDBException {
		final List<EXistResource> list = new ArrayList<>();
		scan(list, current, vpath, pattern);
		return list.toArray(new EXistResource[0]);
	}

	public final static void scan(List<EXistResource> list, Collection current, String vpath, String pattern)
	throws XMLDBException {
		final String[] resources = current.listResources();
		String name;
		for (String resource : resources) {
			name = vpath + resource;
			System.out.println("checking " + name);
			if (DirectoryScanner.match(pattern, name)) {
				list.add((EXistResource) current.getResource(resource));
			}
		}
		final String[] childCollections = current.listChildCollections();
		for (String sub : childCollections) {
			name = vpath + sub;
			System.out.println("checking " + name + " = " + pattern);
			if (SelectorUtils.matchPatternStart(pattern, name))
			///TODO : use dedicated function in XmldbURI
			{
				scan(list, current.getChildCollection(sub), name + "/", pattern);
			}
		}
	}
}
