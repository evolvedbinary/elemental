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
import org.exist.xmldb.XmldbURI;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;

import javax.annotation.Nullable;
import java.net.URISyntaxException;


/**
 * An ant task to create a empty collection.
 *
 * @author <a href="mailto:peter.klotz@blue-elephant-systems.com">Peter Klotz</a>
 */
public class XMLDBCreateTask extends AbstractXMLDBTask
{
    private String collection = null;

    @Override
    public void execute() throws BuildException
    {
        if( uri == null ) {
            throw( new BuildException( "you have to specify an XMLDB collection URI" ) );
        }

        registerDatabase();

        log( "Get base collection: " + uri, Project.MSG_DEBUG );
        try (final Collection base = DatabaseManager.getCollection(uri, user, password)) {

            if( base == null ) {
                final String msg = "Collection " + uri + " could not be found.";

                if( failonerror ) {
                    throw( new BuildException( msg ) );
                } else {
                    log( msg, Project.MSG_ERR );
                }

            } else {
                @Nullable Collection root = null;
                try {
                    if (collection != null) {
                        log("Creating collection " + collection + " in base collection " + uri, Project.MSG_DEBUG);
                        root = mkcol(base, uri, collection);
                    } else {
                        root = base;
                    }

                    if (permissions != null) {
                        setPermissions(root);
                    }

                    log("Created collection " + root.getName(), Project.MSG_INFO);

                } finally {
                    if (root != null) {
                        try {
                            root.close();
                        } catch (final XMLDBException e) {
                            // no-op
                        }
                    }
                }
            }

        }
        catch( final XMLDBException e ) {
            final String msg = "XMLDB exception caught: " + e.getMessage();

            if( failonerror ) {
                throw( new BuildException( msg, e ) );
            } else {
                log( msg, e, Project.MSG_ERR );
            }

        }
        catch( final URISyntaxException e ) {
            final String msg = "URISyntaxException: " + e.getMessage();

            if( failonerror ) {
                throw( new BuildException( msg, e ) );
            } else {
                log( msg, e, Project.MSG_ERR );
            }
        }
    }


    /**
     * Set the Collection.
     *
     * @param collection the collection.
     */
    public void setCollection(final String collection )
    {
        this.collection = collection;
    }


    private Collection mkcol(Collection collection, final String base, final String relPath) throws XMLDBException, URISyntaxException {
        XmldbURI baseUri  = XmldbURI.xmldbUriFor(base);
        final XmldbURI collPath = XmldbURI.xmldbUriFor(relPath);
        final XmldbURI[] segments = collPath.getPathSegments();

        for (final XmldbURI segment : segments) {
            baseUri = baseUri.append( segment);

            Collection child = DatabaseManager.getCollection(baseUri.toString(), user, password);
            if (child == null) {
                final CollectionManagementService mgtService = collection.getService(CollectionManagementService.class);
                log("Create child collection " + segment);
                child = mgtService.createCollection(segment.toString());
                log("Created collection " + child.getName() + '.');
            }

            try {
                // close the parent collection
                collection.close();
            } catch (final XMLDBException e) {
                // no-op
            }

            collection = child;

        }

        return collection;
    }
}
