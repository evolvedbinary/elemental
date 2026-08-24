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
package org.exist.xmldb.concurrent;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.exist.samples.Samples;
import org.exist.util.StringInputSource;
import org.exist.util.io.InputStreamUtil;
import org.exist.xmldb.XmldbURI;
import org.exist.xmldb.concurrent.action.MultiResourcesAction;
import org.exist.xmldb.concurrent.action.XQueryAction;
import org.junit.jupiter.api.BeforeEach;
import org.xml.sax.InputSource;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author wolf
 */
public class ConcurrentResource2Test extends AbstractConcurrentTest {
    
    private static final String QUERY0 =
        "declare default element namespace 'http://www.loc.gov/mods/v3';" +
        "collection(\"" + XmldbURI.ROOT_COLLECTION + "\")//mods[contains(titleInfo/title, 'germany')]";
    
    private static final String QUERY1 =
        "declare default element namespace 'http://www.loc.gov/mods/v3';" +
        "<result>{for $t in distinct-values(collection(\"" + XmldbURI.ROOT_COLLECTION + "\")//mods/subject/topic) order by $t return <topic>{$t}</topic>}</result>";

    @BeforeEach
    void setUp() throws XMLDBException {
        try (final Collection c1 = DBUtils.addCollection(getTestCollection(), "C1-C2")) {
            assertNotNull(c1);
        }
    }

    @Override
    public String getTestCollectionName() {
        return "C1";
    }

    @Override
    public List<Runner> getRunners() throws IOException {
        final List<Tuple2<String, InputSource>> sources = new ArrayList<>();
        for (final String modsFilename : Samples.SAMPLES.getModsXmlSampleNames()) {
            try (final InputStream is = Samples.SAMPLES.getModsSample(modsFilename)) {
                final String modsContent = InputStreamUtil.readString(is, UTF_8);
                sources.add(Tuple(modsFilename, new StringInputSource(modsContent)));
            }
        }

        return Arrays.asList(
            new Runner(new MultiResourcesAction(sources, XmldbURI.LOCAL_DB + "/C1/C1-C2"), 100, 0, 50),
            new Runner(new MultiResourcesAction(sources, XmldbURI.LOCAL_DB + "/C1/C1-C2"), 100, 0, 50),
            new Runner(new XQueryAction(XmldbURI.LOCAL_DB + "/C1/C1-C2", "R1.xml", QUERY0), 100, 200, 100),
            new Runner(new XQueryAction(XmldbURI.LOCAL_DB + "/C1/C1-C2", "R1.xml", QUERY1), 100, 300, 100)
            //new Runner(new XQueryAction(getUri + "/C1/C1-C2", "R1.xml", QUERY0), 200, 400, 500),
            //new Runner(new XQueryAction(getUri + "/C1/C1-C2", "R1.xml", QUERY1), 200, 500, 500)
        );
    }
}
