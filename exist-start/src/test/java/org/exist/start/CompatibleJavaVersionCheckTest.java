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
package org.exist.start;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class CompatibleJavaVersionCheckTest {

    @Test
    public void extractNoVersionComponents() {
        final Optional<int[]> maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.empty());
        assertFalse(maybeComponents.isPresent());
    }

    @Test
    public void extractJava8VersionComponents() {
        final Optional<int[]> maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("1.8.0_292"));
        assertTrue(maybeComponents.isPresent());
        final int[] components = maybeComponents.get();
        assertEquals(4, components.length);
        assertEquals(1, components[0]);
        assertEquals(8, components[1]);
        assertEquals(0, components[2]);
        assertEquals(292, components[3]);
    }

    @Test
    public void extractJava9VersionComponents() {
        Optional<int[]> maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("9.0.4"));
        assertTrue(maybeComponents.isPresent());
        int[] components = maybeComponents.get();
        assertEquals(3, components.length);
        assertEquals(9, components[0]);
        assertEquals(0, components[1]);
        assertEquals(4, components[2]);

        maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("9.0.7.1"));
        assertTrue(maybeComponents.isPresent());
        components = maybeComponents.get();
        assertEquals(4, components.length);
        assertEquals(9, components[0]);
        assertEquals(0, components[1]);
        assertEquals(7, components[2]);
        assertEquals(1, components[3]);
    }

    @Test
    public void extractJava10VersionComponents() {
        final Optional<int[]> maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("10"));
        assertTrue(maybeComponents.isPresent());
        final int[] components = maybeComponents.get();
        assertEquals(1, components.length);
        assertEquals(10, components[0]);
    }

    @Test
    public void extractJava11VersionComponents() {
        Optional<int[]> maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("11"));
        assertTrue(maybeComponents.isPresent());
        int[] components = maybeComponents.get();
        assertEquals(1, components.length);
        assertEquals(11, components[0]);

        maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("11.0.11"));
        assertTrue(maybeComponents.isPresent());
        components = maybeComponents.get();
        assertEquals(3, components.length);
        assertEquals(11, components[0]);
        assertEquals(0, components[1]);
        assertEquals(11, components[2]);
    }

    @Test
    public void extractJava12VersionComponents() {
        Optional<int[]> maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("12.0.1"));
        assertTrue(maybeComponents.isPresent());
        int[] components = maybeComponents.get();
        assertEquals(3, components.length);
        assertEquals(12, components[0]);
        assertEquals(0, components[1]);
        assertEquals(1, components[2]);

        maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("12.0.2-BellSoft"));
        assertTrue(maybeComponents.isPresent());
        components = maybeComponents.get();
        assertEquals(3, components.length);
        assertEquals(12, components[0]);
        assertEquals(0, components[1]);
        assertEquals(2, components[2]);
    }

    @Test
    public void extractJava13VersionComponents() {
        final Optional<int[]> maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("13.0.2"));
        assertTrue(maybeComponents.isPresent());
        final int[] components = maybeComponents.get();
        assertEquals(3, components.length);
        assertEquals(13, components[0]);
        assertEquals(0, components[1]);
        assertEquals(2, components[2]);
    }

    @Test
    public void extractJava14VersionComponents() {
        final Optional<int[]> maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("14.0.2"));
        assertTrue(maybeComponents.isPresent());
        final int[] components = maybeComponents.get();
        assertEquals(3, components.length);
        assertEquals(14, components[0]);
        assertEquals(0, components[1]);
        assertEquals(2, components[2]);
    }

    @Test
    public void extractJava15VersionComponents() {
        final Optional<int[]> maybeComponents = CompatibleJavaVersionCheck.extractJavaVersionComponents(Optional.of("15.0.3"));
        assertTrue(maybeComponents.isPresent());
        final int[] components = maybeComponents.get();
        assertEquals(3, components.length);
        assertEquals(15, components[0]);
        assertEquals(0, components[1]);
        assertEquals(3, components[2]);
    }

    @Test
    public void checkNoVersion() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.empty());
    }

    @Test
    public void checkJava8() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("1.8.0_292"));
    }

    @Test
    public void checkJava9() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("9.0.4"));
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("9.0.7.1"));
    }

    @Test
    public void checkJava10() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("10"));
    }

    @Test
    public void checkJava11() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("11"));
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("11.0.11"));
    }

    @Test(expected = StartException.class)
    public void checkJava12() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("12.0.1"));
    }

    @Test(expected = StartException.class)
    public void checkJava12_BellSoft() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("12.0.2-BellSoft"));
    }

    @Test(expected = StartException.class)
    public void checkJava13() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("13.0.2"));
    }

    @Test(expected = StartException.class)
    public void checkJava14() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("14.0.2"));
    }

    @Test(expected = StartException.class)
    public void checkJava15_0_0() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("15.0.0"));
    }

    @Test(expected = StartException.class)
    public void checkJava15_0_1() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("15.0.1"));
    }

    @Test
    public void checkJava15_0_2() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("15.0.2"));
    }

    @Test
    public void checkJava15_0_3() throws StartException {
        CompatibleJavaVersionCheck.checkForCompatibleJavaVersion(Optional.of("15.0.3"));
    }
}
