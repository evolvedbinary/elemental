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
 */
package org.exist.xquery.modules.file;

import org.exist.test.ExistWebServer;
import org.exist.xmldb.XmldbURI;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.*;
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
public class XmldbBinariesTest extends AbstractBinariesTest<ResourceSet, Resource, XMLDBException> {

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
        Collection colRoot = null;
        try {
            colRoot = DatabaseManager.getCollection(getBaseUri() + "/db", ADMIN_DB_USER, ADMIN_DB_PWD);

            final XmldbURI collectionNames[] = filePath.removeLastSegment().getPathSegments();

            final Deque<Collection> cols = new ArrayDeque<>();
            try {
                Collection current = colRoot;
                for (int i = 1; i < collectionNames.length; i++) {
                    final Collection child = getOrCreateCollection(current, collectionNames[i].toString());
                    cols.push(child);
                    current = child;
                }

                final String fileName = filePath.lastSegment().toString();
                final Resource resource = current.createResource(fileName, BinaryResource.class);
                resource.setContent(content);
                current.storeResource(resource);

            } finally {
                while(!cols.isEmpty()) {
                    try {
                        cols.pop().close();
                    } catch(XMLDBException e) {

                    }
                }
            }
        } finally {
            if(colRoot != null) {
                colRoot.close();
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
        Collection colRoot = null;
        try {
            colRoot = DatabaseManager.getCollection(getBaseUri() + "/db", ADMIN_DB_USER, ADMIN_DB_PWD);

            final Collection colTest = colRoot.getChildCollection("test");
            try {
                final CollectionManagementService cms = colTest.getService(CollectionManagementService.class);

                final String testCollectionName = collectionUri.lastSegment().toString();
                cms.removeCollection(testCollectionName);
            } finally {
                if(colTest != null) {
                    colTest.close();
                }
            }
        } finally {
            if(colRoot != null) {
                colRoot.close();
            }
        }
    }

    @Override
    protected QueryResultAccessor<ResourceSet, XMLDBException> executeXQuery(final String query) throws Exception {
        return consumer -> {
            Collection colRoot = null;
            try {
                colRoot = DatabaseManager.getCollection(getBaseUri() + "/db", ADMIN_DB_USER, ADMIN_DB_PWD);
                final XQueryService xqueryService = colRoot.getService(XQueryService.class);

                final CompiledExpression compiledExpression = xqueryService.compile(query);
                final ResourceSet results = xqueryService.execute(compiledExpression);


                    try {
    //                    compiledExpression.reset();  // shows the ordering issue with binary values (see comment below)

                        consumer.accept(results);
                    } finally {
                        //the following calls cause the streams of any binary result values to be closed, so if we did so before we are finished with the results, serialization would fail.
                        results.clear();
                        compiledExpression.reset();
                    }
            } finally {
                colRoot.close();
            }
        };
    }

    @Override
    protected long size(final ResourceSet results) throws XMLDBException {
        return results.getSize();
    }

    @Override
    protected Resource item(final ResourceSet results, final int index) throws XMLDBException {
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
