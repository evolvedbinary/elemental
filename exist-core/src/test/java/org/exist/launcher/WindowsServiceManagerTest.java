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
package org.exist.launcher;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WindowsServiceManagerTest {

    @Test
    public void asJavaCmdlineMemoryString() {
        assertEquals(Optional.of("1024k"), WindowsServiceManager.asJavaCmdlineMemoryString("1024k"));
        assertEquals(Optional.of("1024K"), WindowsServiceManager.asJavaCmdlineMemoryString("1024K"));
        assertEquals(Optional.of("1024m"), WindowsServiceManager.asJavaCmdlineMemoryString("1024m"));
        assertEquals(Optional.of("1024M"), WindowsServiceManager.asJavaCmdlineMemoryString("1024M"));
        assertEquals(Optional.of("1024g"), WindowsServiceManager.asJavaCmdlineMemoryString("1024g"));
        assertEquals(Optional.of("1024G"), WindowsServiceManager.asJavaCmdlineMemoryString("1024G"));

        // default to MB
        assertEquals(Optional.of("128m"), WindowsServiceManager.asJavaCmdlineMemoryString("128"));

        // ignore junk
        assertEquals(Optional.empty(), WindowsServiceManager.asJavaCmdlineMemoryString("One"));

        // if the unit is unknown, fallback to MB
        assertEquals(Optional.of("1024m"), WindowsServiceManager.asJavaCmdlineMemoryString("1024t"));
        assertEquals(Optional.of("1024m"), WindowsServiceManager.asJavaCmdlineMemoryString("1024T"));
        assertEquals(Optional.of("1024m"), WindowsServiceManager.asJavaCmdlineMemoryString("1024z"));
        assertEquals(Optional.of("1024m"), WindowsServiceManager.asJavaCmdlineMemoryString("1024Z"));
    }

    @Test
    public void asPrunSrvMemoryString() {
        assertEquals(Optional.of("1"), WindowsServiceManager.asPrunSrvMemoryString("1024k"));
        assertEquals(Optional.of("1"), WindowsServiceManager.asPrunSrvMemoryString("1024K"));
        assertEquals(Optional.of("1024"), WindowsServiceManager.asPrunSrvMemoryString("1024m"));
        assertEquals(Optional.of("1024"), WindowsServiceManager.asPrunSrvMemoryString("1024M"));
        assertEquals(Optional.of("1048576"), WindowsServiceManager.asPrunSrvMemoryString("1024g"));
        assertEquals(Optional.of("1048576"), WindowsServiceManager.asPrunSrvMemoryString("1024G"));

        // default to MB
        assertEquals(Optional.of("128"), WindowsServiceManager.asPrunSrvMemoryString("128"));

        // ignore junk
        assertEquals(Optional.empty(), WindowsServiceManager.asPrunSrvMemoryString("One"));

        // if the unit is unknown, fallback to MB
        assertEquals(Optional.of("1024"), WindowsServiceManager.asPrunSrvMemoryString("1024t"));
        assertEquals(Optional.of("1024"), WindowsServiceManager.asPrunSrvMemoryString("1024T"));
        assertEquals(Optional.of("1024"), WindowsServiceManager.asPrunSrvMemoryString("1024z"));
        assertEquals(Optional.of("1024"), WindowsServiceManager.asPrunSrvMemoryString("1024Z"));
    }
}
