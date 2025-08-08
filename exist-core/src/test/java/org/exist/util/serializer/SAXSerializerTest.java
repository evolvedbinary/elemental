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
 */
package org.exist.util.serializer;

import org.apache.commons.io.output.StringBuilderWriter;
import org.exist.dom.QName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SAX Serialization tests to check for:
 * <a href="https://github.com/eXist-db/exist/issues/5790">eXist-db issue #5790</a>
 * <a href="https://github.com/evolvedbinary/elemental/issues/61">Elemental issue #61</a>
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class SAXSerializerTest {

    private static final Properties OUTPUT_PROPERTIES = new Properties();
    static {
        OUTPUT_PROPERTIES.put(OutputKeys.INDENT, "no");
        OUTPUT_PROPERTIES.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
    }

    @Test
    public void coalesceNamespacePrefixesFromStrings() throws SAXException {
        try (final StringBuilderWriter writer = new StringBuilderWriter()) {
            final SAXSerializer saxSerializer = new SAXSerializer(writer, OUTPUT_PROPERTIES);

            saxSerializer.startDocument();
            saxSerializer.startElement("http://exist-db.org/xquery/repo", "meta", "meta", null);
            saxSerializer.startElement("http://exist-db.org/xquery/repo", "changelog", "changelog", null);

            saxSerializer.startElement("http://exist-db.org/xquery/repo", "change", "change", null);
            saxSerializer.endElement("http://exist-db.org/xquery/repo", "change", "change");

            saxSerializer.startElement("http://exist-db.org/xquery/repo", "change", "change", null);
            saxSerializer.characters("change-text-1");
            saxSerializer.endElement("http://exist-db.org/xquery/repo", "change", "change");

            saxSerializer.startElement("http://exist-db.org/xquery/repo", "change", "repo:change", null);
            saxSerializer.endElement("http://exist-db.org/xquery/repo", "change", "repo:change");

            saxSerializer.startElement("http://exist-db.org/xquery/repo", "change", "repo:change", null);
            saxSerializer.characters("change-text-2");
            saxSerializer.endElement("http://exist-db.org/xquery/repo", "change", "repo:change");

            saxSerializer.endElement("http://exist-db.org/xquery/repo", "changelog", "changelog");
            saxSerializer.endElement("http://exist-db.org/xquery/repo", "meta", "meta");
            saxSerializer.endDocument();

            assertEquals("<meta xmlns=\"http://exist-db.org/xquery/repo\"><changelog><change/><change>change-text-1</change><change/><change>change-text-2</change></changelog></meta>", writer.toString());
        }
    }

    @Test
    public void coalesceNamespacePrefixesFromQName() throws SAXException {
        try (final StringBuilderWriter writer = new StringBuilderWriter()) {
            final SAXSerializer saxSerializer = new SAXSerializer(writer, OUTPUT_PROPERTIES);

            saxSerializer.startDocument();
            saxSerializer.startElement(new QName("meta", "http://exist-db.org/xquery/repo", null), null);
            saxSerializer.startElement(new QName("changelog", "http://exist-db.org/xquery/repo", null), null);

            saxSerializer.startElement(new QName("change", "http://exist-db.org/xquery/repo", null), null);
            saxSerializer.endElement(new QName("change", "http://exist-db.org/xquery/repo", null));

            saxSerializer.startElement(new QName("change", "http://exist-db.org/xquery/repo", null), null);
            saxSerializer.characters("change-text-1");
            saxSerializer.endElement(new QName("change", "http://exist-db.org/xquery/repo", null));

            saxSerializer.startElement(new QName("change", "http://exist-db.org/xquery/repo", "repo"), null);
            saxSerializer.endElement(new QName("change", "http://exist-db.org/xquery/repo", "repo"));

            saxSerializer.startElement(new QName("change", "http://exist-db.org/xquery/repo", "repo"), null);
            saxSerializer.characters("change-text-1");
            saxSerializer.endElement(new QName("change", "http://exist-db.org/xquery/repo", "repo"));

            saxSerializer.endElement(new QName("changelog", "http://exist-db.org/xquery/repo", null));
            saxSerializer.endElement(new QName("meta", "http://exist-db.org/xquery/repo", null));
            saxSerializer.endDocument();

            assertEquals("<meta xmlns=\"http://exist-db.org/xquery/repo\"><changelog><change/><change>change-text-1</change><change/><change>change-text-2</change></changelog></meta>", writer.toString());
        }
    }
}
