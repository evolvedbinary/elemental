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
package org.exist.storage.statistics;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.exist.storage.BrokerPool;
import org.exist.test.ExistEmbeddedServer;
import org.junit.*;

import static org.exist.storage.NativeBroker.DEFAULT_DATA_DIR;
import static org.junit.Assert.assertTrue;

public class StatisticsIndexTest {

    private static Path configFile;

    @BeforeClass
    public static void prepare() throws URISyntaxException {
        final ClassLoader loader = StatisticsIndexTest.class.getClassLoader();
        final char separator = System.getProperty("file.separator").charAt(0);
        final String packagePath = StatisticsIndexTest.class.getPackage().getName().replace('.', separator);

        configFile = Paths.get(loader.getResource(packagePath + separator + "conf.xml").toURI());
    }

    @Rule
    public final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer("db1", configFile, null, true);

    @Test
    public void statsFileExists() {
        final Path dataDir = existEmbeddedServer.getBrokerPool().getConfiguration().getProperty(BrokerPool.PROPERTY_DATA_DIR, Paths.get(DEFAULT_DATA_DIR));
        assertTrue(Files.exists(dataDir.resolve("stats.dbx")));
    }
}
