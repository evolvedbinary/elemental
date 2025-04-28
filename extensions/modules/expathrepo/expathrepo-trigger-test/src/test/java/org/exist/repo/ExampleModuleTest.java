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
package org.exist.repo;


import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.Sequence;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExampleModuleTest {

    @ClassRule
    public static ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(false, true);

    @Test
    public void helloWorld() throws XPathException, PermissionDeniedException, EXistException {
        final String query =
                "declare namespace myjmod = \"https://my-organisation.com/exist-db/ns/app/my-java-module\";\n" +
                        "myjmod:hello-world()";
        final Sequence result = executeQuery(query);

        assertTrue(result.hasOne());

        final Source inExpected = Input.fromString("<hello>World</hello>").build();
        final Source inActual = Input.fromDocument((Document) result.itemAt(0)).build();

        final Diff diff = DiffBuilder.compare(inExpected)
                .withTest(inActual)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void sayHello() throws XPathException, PermissionDeniedException, EXistException {
        final String query =
                "declare namespace myjmod = \"https://my-organisation.com/exist-db/ns/app/my-java-module\";\n" +
                        "myjmod:say-hello('Adam')";
        final Sequence result = executeQuery(query);

        assertTrue(result.hasOne());

        final Source inExpected = Input.fromString("<hello>Adam</hello>").build();
        final Source inActual = Input.fromDocument((Document) result.itemAt(0)).build();

        final Diff diff = DiffBuilder.compare(inExpected)
                .withTest(inActual)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void sayHello_noName() throws XPathException, PermissionDeniedException, EXistException {
        final String query =
                "declare namespace myjmod = \"https://my-organisation.com/exist-db/ns/app/my-java-module\";\n" +
                        "myjmod:say-hello(())";
        final Sequence result = executeQuery(query);

        assertTrue(result.hasOne());

        final Source inExpected = Input.fromString("<hello>stranger</hello>").build();
        final Source inActual = Input.fromDocument((Document) result.itemAt(0)).build();

        final Diff diff = DiffBuilder.compare(inExpected)
                .withTest(inActual)
                .checkForSimilar()
                .build();

        assertFalse(diff.toString(), diff.hasDifferences());
    }

    @Test
    public void add() throws XPathException, PermissionDeniedException, EXistException {
        final String query =
                "declare namespace myjmod = \"https://my-organisation.com/exist-db/ns/app/my-java-module\";\n" +
                        "myjmod:add(xs:int(123), xs:int(456))";
        final Sequence result = executeQuery(query);

        assertTrue(result.hasOne());

        assertEquals(579, ((IntegerValue)result.itemAt(0)).getInt());
    }


    private Sequence executeQuery(final String xquery) throws EXistException, PermissionDeniedException, XPathException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final XQuery xqueryService = pool.getXQueryService();

        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            return xqueryService.execute(broker, xquery, null);
        }
    }
}
