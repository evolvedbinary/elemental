/*
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

package org.exist.dom.memtree;

import org.exist.Namespaces;
import org.exist.util.ExistSAXParserFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.parsers.ParserConfigurationException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

@Execution(ExecutionMode.CONCURRENT)
public class MemtreeBuilderTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "namespaceAware,true",
        "namespaceIgnorant,false"
    })
    public void parseSimple(final String parameterizedTestsName, final boolean namespaceAware) throws IOException, SAXException, ParserConfigurationException {
        final String doc = "<timestamp>" + System.currentTimeMillis() + "</timestamp>";
        final DocumentImpl parsedDoc = parse(doc, namespaceAware);

        final Source expectedSource = Input.fromString(doc).build();
        final Source actualSource = Input.fromNode(parsedDoc).build();
        final Diff diff = DiffBuilder.compare(expectedSource)
                .withTest(actualSource)
                .checkForIdentical()
                .checkForSimilar()
                .build();

        assertFalse(diff.hasDifferences(), diff.toString());
    }

    private DocumentImpl parse(final String xml, final boolean namespaceAware) throws ParserConfigurationException, SAXException, IOException {
        final SAXParserFactory saxParserFactory = ExistSAXParserFactory.getSAXParserFactory();
        saxParserFactory.setNamespaceAware(namespaceAware);

        final SAXAdapter saxAdapter = new SAXAdapter();
        final SAXParser saxParser = saxParserFactory.newSAXParser();
        final XMLReader xmlReader = saxParser.getXMLReader();

        xmlReader.setContentHandler(saxAdapter);
        xmlReader.setProperty(Namespaces.SAX_LEXICAL_HANDLER, saxAdapter);

        try (final Reader reader = new StringReader(xml)) {
            xmlReader.parse(new InputSource(reader));
        }

        return saxAdapter.getDocument();
    }
}
