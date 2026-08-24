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

import org.exist.test.XmldbEmbeddedDatabaseExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ErrorCodes;
import org.xmldb.api.base.XMLDBException;

import java.io.IOException;

/**
 * Security Manager round trip tests against the XML:DB Local API
 *
 * @author <a href="mailto:adam@exist-db.org">Adam Retter</a>
 */
public class LocalSecurityManagerRoundtripTest extends AbstractSecurityManagerRoundtripTest {

    @RegisterExtension
    public static final XmldbEmbeddedDatabaseExtension XMLDB_EMBEDDED_DATABASE = new XmldbEmbeddedDatabaseExtension(false, true, true);

    @Override
    protected Collection getRoot() {
        return XMLDB_EMBEDDED_DATABASE.getRoot();
    }

    @Override
    protected void restartServer() throws XMLDBException, IOException {
        try {
            XMLDB_EMBEDDED_DATABASE.restart();
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new XMLDBException(ErrorCodes.UNKNOWN_ERROR, e);
        }
    }
}
