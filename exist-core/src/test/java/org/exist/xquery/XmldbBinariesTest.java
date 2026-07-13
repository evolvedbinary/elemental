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

import org.exist.test.ExistWebServer;
import org.exist.xmldb.EXistResourceSet;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XQueryService;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import static org.exist.TestUtils.ADMIN_DB_PWD;
import static org.exist.TestUtils.ADMIN_DB_USER;
import static org.xmldb.api.base.ResourceType.BINARY_RESOURCE;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@RunWith(Parameterized.class)
public class XmldbBinariesTest extends AbstractBinariesTest<EXistResourceSet, Resource, XMLDBException> {

    @ClassRule
    public static final ExistWebServer existWebServer = new ExistWebServer(true, false, true, true);
    private static final String PORT_PLACEHOLDER = "${PORT}";

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "local", "xmldb:exist://" },
                { "remote", "xmldb:exist://localhost:" + PORT_PLACEHOLDER + "/xmlrpc" }
        });
    }

    @Parameterized.Parameter
    public String apiName;

    @Parameterized.Parameter(value = 1)
    public String baseUri;

    private final String getBaseUri() {
        return baseUri.replace(PORT_PLACEHOLDER, Integer.toString(existWebServer.getPort()));
    }

    @Override
    protected void storeBinaryFile(final XmldbURI filePath, byte[] content) throws Exception {
        try (final Collection colRoot = DatabaseManager.getCollection(getBaseUri() + "/db", ADMIN_DB_USER, ADMIN_DB_PWD)) {

            final XmldbURI collectionNames[] = filePath.removeLastSegment().getPathSegments();

            final Deque<Collection> cols = new ArrayDeque<>();
            try {
                Collection current = colRoot;
                for (int i = 1; i < collectionNames.length; i++) {
                    final Collection child = getOrCreateCollection(current, collectionNames[i].toString());
                    cols.addFirst(child);
                    current = child;
                }

                final String fileName = filePath.lastSegment().toString();
                try (final Resource resource = current.createResource(fileName, BinaryResource.class)) {
                    resource.setContent(content);
                    current.storeResource(resource);
                }

            } finally {
                while (!cols.isEmpty()) {
                    try {
                        cols.removeFirst().close();
                    } catch (final XMLDBException e) {

                    }
                }
            }
        }
    }

    private Collection getOrCreateCollection(final Collection parent, final String childName) throws XMLDBException {
        Collection child = parent.getChildCollection(childName);
        if(child == null) {
            final CollectionManagementService cms = parent.getService(CollectionManagementService.class);
            child = cms.createCollection(childName);
        }
        return child;
    }

    @Override
    protected void removeCollection(final XmldbURI collectionUri) throws Exception {
        try (final Collection colRoot = DatabaseManager.getCollection(getBaseUri() + "/db", ADMIN_DB_USER, ADMIN_DB_PWD)) {

            try (final Collection colTest = colRoot.getChildCollection("test")) {
                final CollectionManagementService cms = colTest.getService(CollectionManagementService.class);

                final String testCollectionName = collectionUri.lastSegment().toString();
                cms.removeCollection(testCollectionName);
            }
        }
    }

    @Override
    protected QueryResultAccessor<EXistResourceSet, XMLDBException> executeXQuery(final String query) {
        return consumer -> {
            try (Collection colRoot = DatabaseManager.getCollection(getBaseUri() + "/db", ADMIN_DB_USER, ADMIN_DB_PWD)) {
                final XQueryService xqueryService = colRoot.getService(XQueryService.class);

                final CompiledExpression compiledExpression = xqueryService.compile(query);
                try (final EXistResourceSet results = (EXistResourceSet) xqueryService.execute(compiledExpression)) {
//                    compiledExpression.reset();  // shows the ordering issue with binary values (see comment below)
                    consumer.accept(results);
                }
            }
        };
    }

    @Override
    protected long size(final EXistResourceSet results) throws XMLDBException {
        return results.getSize();
    }

    @Override
    protected Resource item(final EXistResourceSet results, final int index) throws XMLDBException {
        return results.getResource(index);
    }

    @Override
    protected boolean isBinaryType(final Resource item) throws XMLDBException {
        return BINARY_RESOURCE.equals(item.getResourceType());
    }

    @Override
    protected boolean isBooleanType(final Resource item) throws XMLDBException {
        final String value = item.getContent().toString();
        return "true".equals(value) || "false".equals(value);
    }

    @Override
    protected byte[] getBytes(final Resource item) throws XMLDBException {
        return (byte[])item.getContent();
    }

    @Override
    protected boolean getBoolean(final Resource item) throws XMLDBException {
        return Boolean.parseBoolean(item.getContent().toString());
    }
}
