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
package org.exist.xquery.modules.file;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.exist.xquery.Expression;
import org.exist.xquery.XPathException;

/**
 *  Helper class for FileModule
 * 
 * @author <a href="mailto:dannes@exist-db.org">Dannes Wessels</a>
 */
public class FileModuleHelper {

    private FileModuleHelper() {
        // no instance
    }
    
    /**
     * Get a file path.
     *
     * @param path Path written as OS specific path or as URL.
     * @param expression the calling expression.
     *
     * @return a path to the file.
     *
     * @throws XPathException if the path is an invalid URI.
     */
    public static Path getFile(String path, final Expression expression) throws XPathException {
        if(path.startsWith("file:")){
            try {
                return Paths.get(new URI(path));
            } catch (Exception ex) { // catch all (URISyntaxException)
                throw new XPathException(expression, path + " is not a valid URI: '"+ ex.getMessage() +"'");
            }
        } else {
            return Paths.get(path);
        }
    }
    
}
