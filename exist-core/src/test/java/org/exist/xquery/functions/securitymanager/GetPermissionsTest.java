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
package org.exist.xquery.functions.securitymanager;

import org.exist.dom.memtree.ElementImpl;
import org.exist.security.internal.SecurityManagerImpl;
import org.exist.source.StringSource;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.EmbeddedDatabaseExtension;
import org.exist.xquery.XQueryUtil;
import org.exist.xquery.value.Sequence;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class GetPermissionsTest {

    @RegisterExtension
    public static final EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, true);

    /**
     * See https://github.com/eXist-db/exist/issues/3231
     */
    @Test
    void getPermissionsNestedXml() throws EXistException, PermissionDeniedException, XPathException, IOException {
        final String query = "<outer><inner perm=\"{sm:get-permissions(xs:anyURI(\"/db\"))/sm:permission/@owner}\"/></outer>";

        final BrokerPool pool = EMBEDDED_DATABASE.getBrokerPool();
        try (final DBBroker broker = pool.getBroker();
                final XQueryUtil.QueryResult queryResult = XQueryUtil.query(broker, new StringSource(query), false, null, null, null, null, null)) {

            final Sequence result = queryResult.result;

            assertEquals(1, result.getItemCount());

            final Source expected = Input.fromString("<outer><inner perm=\"" + SecurityManagerImpl.SYSTEM + "\"/></outer>").build();
            final Source actual = Input.fromDocument(((ElementImpl) result.itemAt(0)).getOwnerDocument()).build();

            final Diff diff = DiffBuilder
                    .compare(expected)
                    .withTest(actual)
                    .checkForSimilar()
                    .build();

            assertFalse(diff.hasDifferences(), diff.toString());
        }
    }
}
