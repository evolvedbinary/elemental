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
package org.exist.xquery;

import com.googlecode.junittoolbox.ParallelRunner;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.EXistResourceSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XMLResource;

/**
 * @author <a href="mailto:adam.retter@googlemail.com">Adam Retter</a>
 */
@RunWith(ParallelRunner.class)
public class PersistentDescendantOrSelfNodeKindTest extends AbstractDescendantOrSelfNodeKindTest {

    private static final String TEST_DOCUMENT_NAME = "PersistentDescendantOrSelfNodeKindTest.xml";

    private String getDbQuery(final String queryPostfix) {
        return "let $doc := doc('/db/" + TEST_DOCUMENT_NAME + "')\n" +
            "return\n" +
            queryPostfix;
    }

    @Override
    protected EXistResourceSet executeQueryOnDoc(final String docQuery) throws XMLDBException {
        return  existEmbeddedServer.executeQuery(getDbQuery(docQuery));
    }

    @BeforeClass
    public static void storeTestDoc() throws XMLDBException {
        final Collection root =  existEmbeddedServer.getRoot();
        try (final EXistResource res = (EXistResource) root.createResource(TEST_DOCUMENT_NAME, XMLResource.RESOURCE_TYPE)) {
            res.setContent(TEST_DOCUMENT);
            root.storeResource(res);
        }
    }

    @AfterClass
    public static void removeTestDoc() throws XMLDBException {
        final Collection root =  existEmbeddedServer.getRoot();
        try (final EXistResource res = (EXistResource) root.getResource(TEST_DOCUMENT_NAME)) {
            root.removeResource(res);
        }
    }
}
