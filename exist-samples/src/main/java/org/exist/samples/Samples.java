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
package org.exist.samples;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.net.URL;

public class Samples {

    public static final Samples SAMPLES = new Samples();

    private Samples() {}

    /**
     * Gets the Address Book sample.
     *
     * @return The stream of the Address Book sample
     */
    public @Nullable InputStream getAddressBookSample() {
        return getSample("validation/addressbook/addressbook.xsd");
    }

    /**
     * Gets the Shakespeare Hamlet sample.
     *
     * @return The stream of the Shakespeare Hamlet sample
     */
    public @Nullable InputStream getHamletSample() {
        return getShakespeareSample("hamlet.xml");
    }

    /**
     * Gets the Shakespeare Romeo and Juliet sample.
     *
     * @return The stream of the Shakespeare Romeo and Juliet sample
     */
    public @Nullable InputStream getRomeoAndJulietSample() {
        return getShakespeareSample("r_and_j.xml");
    }

    /**
     * Gets the Shakespeare Macbeth sample.
     *
     * @return The stream of the Macbeth sample
     */
    public@Nullable InputStream getMacbethSample() {
        return getShakespeareSample("macbeth.xml");
    }

    /**
     * Get the names of just the Shakespeare XML sample files.
     *
     * @return the names of the Shakespeare XML files.
     */
    public String[] getShakespeareXmlSampleNames() {
        return new String[] { "hamlet.xml", "macbeth.xml", "r_and_j.xml"};
    }

    /**
     * Get the names of all the Shakespeare sample files.
     *
     * @return the names of all the Shakespeare sample files.
     */
    public String[] getShakespeareSampleNames() {
        return new String[] { "collection.xconf", "hamlet.xml", "macbeth.xml", "play.dtd", "r_and_j.xml", "shakes.css", "shakes.xsl"};
    }

    /**
     * Gets the shakespeare sample.
     *
     * @param sampleFileName the name of the shakespeare sample.
     *
     * @return The stream of the Shakespeare sample
     */
    public @Nullable InputStream getShakespeareSample(final String sampleFileName) {
        return getSample("shakespeare/" + sampleFileName);
    }

    /**
     * Gets the path of the Bibliographic sample.
     *
     * @return The stream of the Bibliographic sample
     */
    public @Nullable InputStream getBiblioSample() {
        return getSample("biblio.rdf");
    }

    /**
     * Get the names of just the MODS XML data sample files.
     *
     * @return the names of the MODS XML data files.
     */
    public String[] getModsXmlSampleNames() {
        return new String[] {
            "02db3b51-146d-4740-81c5-c22dcdadecfb.xml",
            "0d569a0b-2738-4865-8b47-a9f8b821a653.xml",
            "1a186ea9-d41a-4e03-8f0a-d3389cbcf769.xml",
            "1a745d88-0d42-42e7-b911-26f1976bf41f.xml",
            "321a0f72-8ecc-419c-992f-05b4000124e0.xml",
            "36fe6751-7d83-4155-81f8-c00e515f07d1.xml",
            "494ebe80-aa3c-457c-8c20-7de246bf5f72.xml",
            "49afb9d1-71d8-49c2-8566-13eff9ff0935.xml",
            "58f1eb27-0cec-4e2e-83f5-57d03123c194.xml",
            "6bbce50c-8e1c-46cb-b804-9cd020816f63.xml",
            "6fd69e78-77ca-4b2a-84c1-b22a24d676b3.xml",
            "78f221c8-5bb0-4bf1-ad43-cf0aff6d53c7.xml",
            "81fb0a7e-f268-4091-aae2-1e6ccb867930.xml",
            "9f8877f7-2064-4ee5-92f5-a1b6afe68714.xml",
            "b5c28abd-8a78-4a2d-b2eb-30d06d7dac10.xml",
            "ba45cdc0-96d4-4181-a519-ed3f1f9f89cc.xml",
            "ba5f637f-2ca5-4d07-b8b3-804dab7fbdb7.xml",
            "be884622-f29f-42ba-8903-e5eda73bcf34.xml",
            "dae9118e-573a-4230-b781-5007c0579f27.xml",
            "f3ad5614-d1b4-4860-a108-542c42dceebf.xml"
        };
    }

    /**
     * Gets the MODS sample.
     *
     * @param sampleFileName the name of the MODS sample.
     *
     * @return The stream of the MODS sample
     */
    public @Nullable InputStream getModsSample(final String sampleFileName) {
        return getSample("mods/" + sampleFileName);
    }

    /**
     * Gets the sample.
     *
     * @param sample relative path to the sample
     *
     * @return The stream of the sample
     */
    public @Nullable InputStream getSample(final String sample) {
        return getClass().getResourceAsStream(sample);
    }


    /**
     * Gets the URL of the sample.
     *
     * @param sample relative path to the sample
     *
     * @return The url of the sample
     */
    public @Nullable URL getSampleUrl(final String sample) {
        return getClass().getResource(sample);
    }
}
