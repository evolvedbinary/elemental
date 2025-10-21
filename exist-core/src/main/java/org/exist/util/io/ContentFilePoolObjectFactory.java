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
package org.exist.util.io;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Object factory responsible for creating {@link ContentFile} instances and wrap those into pool objects.
 *
 * @author <a href="mailto:patrick@reini.net">Patrick Reinhart</a>
 */
public final class ContentFilePoolObjectFactory extends BasePooledObjectFactory<ContentFile> {
    private final TemporaryFileManager tempFileManager;
    private final int inMemorySize;

    /**
     * Creates a object factory instance using the given temporary file manager and maximum in memotx size.
     *
     * @param tempFileManager the temporary file manager used when need to swap out data bigger than max in memory size
     * @param inMemorySize the maximum in memory size before swappiing the data to a temporay file
     */
    public ContentFilePoolObjectFactory(final TemporaryFileManager tempFileManager, int inMemorySize) {
        this.tempFileManager = tempFileManager;
        this.inMemorySize = inMemorySize;
    }

    @Override
    public ContentFile create() throws Exception {
        return new VirtualTempPath(ContentFile.ContentFileType.UNKNOWN, inMemorySize, tempFileManager);
    }

    @Override
    public PooledObject<ContentFile> wrap(ContentFile obj) {
        return new DefaultPooledObject<>(obj);
    }
}
