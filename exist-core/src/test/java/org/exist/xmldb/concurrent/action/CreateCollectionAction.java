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
package org.exist.xmldb.concurrent.action;

import java.io.IOException;
import java.io.InputStream;

import org.exist.util.io.InputStreamUtil;
import org.exist.xmldb.EXistCollectionManagementService;
import org.exist.xmldb.concurrent.DBUtils;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.exist.samples.Samples.SAMPLES;

public class CreateCollectionAction extends Action {
    
    private int collectionCnt = 0;
    
    public CreateCollectionAction(final String collectionPath, final String resourceName) {
        super(collectionPath, resourceName);
    }

    @Override
    public boolean execute() throws XMLDBException, IOException {
        try (final Collection col = DatabaseManager.getCollection(collectionPath, "admin", "");
            final Collection target = DBUtils.addCollection(col, "C" + ++collectionCnt)) {

            addFiles(target);

            final EXistCollectionManagementService mgt = (EXistCollectionManagementService) col.getService("CollectionManagementService", "1.0");
            try (final Collection copy = DBUtils.addCollection(col, "CC" + collectionCnt)) {
                for (final String resource : target.listResources()) {
                    mgt.copyResource(target.getName() + '/' + resource, copy.getName(), null);
                }
            }

            return true;
        }
    }

    private void addFiles(final Collection col) throws XMLDBException, IOException {
        for (final String sampleName : SAMPLES.getShakespeareXmlSampleNames()) {
            final String sample;
            try (final InputStream is = SAMPLES.getShakespeareSample(sampleName)) {
                sample = InputStreamUtil.readString(is, UTF_8);
            }
            DBUtils.addXMLResource(col, sampleName, sample);
        }
    }
}
