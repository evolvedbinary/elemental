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
package org.exist.dom.persistent;

import org.exist.storage.io.VariableByteInput;
import org.exist.storage.io.VariableByteOutputStream;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * XML Declaration of an XML document
 * available with SAX in Java 14+.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class XMLDeclarationImpl {

    @Nullable private final String version;
    @Nullable private final String encoding;
    @Nullable private final String standalone;

    public XMLDeclarationImpl(@Nullable final String version, @Nullable final String encoding, @Nullable final String standalone) {
        this.version = version;
        this.encoding = encoding;
        this.standalone = standalone;
    }

    /**
     * Get the version from the XML Declaration.
     *
     * @return the version (if present), or null.
     */
    @Nullable
    public String getVersion() {
        return version;
    }

    /**
     * Get the encoding from the XML Declaration.
     *
     * @return the encoding (if present), or null.
     */
    @Nullable
    public String getEncoding() {
        return encoding;
    }

    /**
     * Get the standalone from the XML Declaration.
     *
     * @return the standalone (if present), or null.
     */
    @Nullable
    public String getStandalone() {
        return standalone;
    }

    /**
     * Write the XML Declaration to the output stream.
     *
     * @param ostream the output stream.
     *
     * @throws IOException if an error occurs whilst writing to the output stream.
     */
    public void write(final VariableByteOutputStream ostream) throws IOException {
        ostream.writeUTF(version != null ? version : "");
        ostream.writeUTF(encoding != null ? encoding : "");
        ostream.writeUTF(standalone != null ? standalone : "");
    }

    /**
     * Read an XML Declaration from the input stream.
     *
     * @param istream the input stream.
     *
     * @throws IOException if an error occurs whilst reading from the input stream.
     */
    public static XMLDeclarationImpl read(final VariableByteInput istream) throws IOException {
        String version = istream.readUTF();
        if(version.length() == 0) {
            version = null;
        }
        String encoding = istream.readUTF();
        if(encoding.length() == 0) {
            encoding = null;
        }
        String standalone = istream.readUTF();
        if(standalone.length() == 0) {
            standalone = null;
        }

        return new XMLDeclarationImpl(version, encoding, standalone);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append("<?xml");

        if (version != null) {
            builder.append(" version=\"").append(version).append("\"");
        }

        if (encoding != null) {
            builder.append(" encoding=\"").append(encoding).append("\"");
        }

        if (standalone != null) {
            builder.append(" standalone=\"").append(standalone).append("\"");
        }

        builder.append("?>");

        return builder.toString();
    }
}
