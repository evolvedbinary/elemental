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
package org.exist.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.exist.xmldb.UserManagementService;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;

import javax.annotation.Nullable;


/**
 * abstract base class for all user-related tasks.
 *
 * @author <a href="mailto:peter.klotz@blue-elephant-systems.com">Peter Klotz</a>
 * @author  andrzej@chaeron.com
 */
public abstract class UserTask extends AbstractXMLDBTask {
    private @Nullable UserManagementService service = null;
    private @Nullable Collection base = null;

    protected UserManagementService getService() {
        return service;
    }

    protected Collection getBase() {
        return base;
    }

    @Override
    public final void execute() throws BuildException {
        try {
            prepareToExecuteUserTask();
            executeUserTask();
        } finally {
            close();
        }
    }

    protected void prepareToExecuteUserTask() throws BuildException {
        if (uri == null) {
            throw new BuildException( "you have to specify an XMLDB collection URI");
        }

        registerDatabase();

        try {
            log("Get base collection: " + uri, Project.MSG_DEBUG);
            this.base = DatabaseManager.getCollection(uri, user, password);

            if (base == null) {
                final String msg = "Collection " + uri + " could not be found.";

                if (failonerror) {
                    throw new BuildException(msg);
                } else {
                    log(msg, Project.MSG_ERR);
                }
            } else {
                this.service = base.getService(UserManagementService.class);
            }

        } catch (final XMLDBException e) {
            final String msg = "XMLDB exception caught: " + e.getMessage();

            if(failonerror) {
                throw new BuildException(msg, e);
            } else {
                log(msg, e, Project.MSG_ERR);
            }
        }
    }

    /**
     * Execute the user task.
     *
     * @throws BuildException if an error occurs.
     */
    public abstract void executeUserTask() throws BuildException;

    protected void close() {
        if (service != null) {
            service = null;
        }

        if (base != null) {
            try {
                base.close();
            } catch (final XMLDBException e) {
                // no-op
            }
            base = null;
        }
    }
}
