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
package org.exist.xquery.modules.expathrepo;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.dom.QName;
import org.exist.repo.ExistRepository;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.*;
import org.expath.pkg.repo.PackageException;
import org.expath.pkg.repo.Repository;
import org.expath.pkg.repo.UserInteractionStrategy;
import org.expath.pkg.repo.tui.BatchUserInteraction;


/**
 * Remove Function: Remove package from repository
 *
 * @author <a href="mailto:jim.fuller@exist-db.org">James Fuller</a>
 * @author cutlass
 * @author ljo
 */
public class RemoveFunction extends BasicFunction {
    @SuppressWarnings("unused")
	private final static Logger logger = LogManager.getLogger(RemoveFunction.class);

    public final static FunctionSignature signature =
		new FunctionSignature(
			new QName("remove", ExpathPackageModule.NAMESPACE_URI, ExpathPackageModule.PREFIX),
			"Remove package, pkgName, from repository.",
			new SequenceType[] { new FunctionParameterSequenceType("pkgName", Type.STRING, Cardinality.EXACTLY_ONE, "package name")},
			new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE, "true if successful, false otherwise"));

	public RemoveFunction(XQueryContext context) {
		super(context, signature);
 	}

	public Sequence eval(Sequence[] args, Sequence contextSequence)
		throws XPathException {
	    Sequence removed = BooleanValue.TRUE;
	    boolean force = false;
	    UserInteractionStrategy interact = new BatchUserInteraction();
	    String pkg = args[0].getStringValue();

	    try {
		Optional<ExistRepository> repo = getContext().getRepository();
		if (repo.isPresent()) {
		    Repository parent_repo = repo.get().getParentRepo();
		    parent_repo.removePackage(pkg, force, interact);
		    repo.get().reportAction(ExistRepository.Action.UNINSTALL, pkg);
		    context.getBroker().getBrokerPool().getXQueryPool().clear();
		} else {
		    throw new XPathException(this, "expath repository not available");
		}
	    } catch (PackageException | XPathException pe) {
		return BooleanValue.FALSE;
		// /TODO: _repo.removePackage seems to throw PackageException
		// throw new XPathException("Problem removing package " + pkg + " in expath repository, check the access permissions to the expath repository file directory  ", pe);
	    }
        return removed;
	}
}
