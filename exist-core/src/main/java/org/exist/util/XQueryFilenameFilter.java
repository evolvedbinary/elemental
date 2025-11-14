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

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

public class XQueryFilenameFilter implements FilenameFilter {

    public static final String MEDIA_TYPE_APPLICATION_XQUERY = "application/xquery";

    private final MimeTable mimeTable;

    public XQueryFilenameFilter(final MimeTable mimeTable) {
        this.mimeTable = mimeTable;
    }

    @Override
    public boolean accept(final File dir, final String name) {
        final MimeType mime = mimeTable.getContentTypeFor(name);
        return mime != null && !mime.isXMLType() && mime.getName().equals(MEDIA_TYPE_APPLICATION_XQUERY);
    }

    public static Predicate<Path> asPredicate(final MimeTable mimeTable) {
        return path -> {
            if(!Files.isDirectory(path)) {
                final MimeType mime = mimeTable.getContentTypeFor(FileUtils.fileName(path));
                return mime != null && !mime.isXMLType() && mime.getName().equals(MEDIA_TYPE_APPLICATION_XQUERY);
            }
            return false;
        };
    }
}
