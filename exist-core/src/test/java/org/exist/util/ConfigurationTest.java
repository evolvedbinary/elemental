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
package org.exist.util;

import org.exist.Namespaces;
import org.exist.dom.memtree.SAXAdapter;
import org.exist.storage.BrokerFactory;
import org.exist.xquery.Expression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.xml.sax.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static org.assertj.core.api.Assertions.*;

class ConfigurationTest {
    @Test
    void testConfigurationConstructors() {
        assertThatNoException().isThrownBy(Configuration::new);
        assertThatNoException().isThrownBy(() -> new Configuration(null));
    }

    @Test
    void testConfigurationConstructorWithClasspathConf(@TempDir Path existHomeDir) throws Exception {
        assertThatNoException().isThrownBy(() -> new Configuration("conf.xml"));
        assertThatNoException().isThrownBy(() -> new Configuration("conf.xml", Optional.empty()));
        assertThatNoException().isThrownBy(() -> new Configuration("conf.xml", Optional.of(existHomeDir)));
    }

    @Test
    void testConfigurationContents(@TempDir Path existHomeDir) throws Exception {
        validate(new Configuration("conf.xml"));
        validate(new Configuration("conf.xml", Optional.empty()));
        validate(new Configuration("conf.xml", Optional.of(existHomeDir)));
    }

    private void validate(final Configuration config) {
        assertThat(config.hasProperty(BrokerFactory.PROPERTY_DATABASE)).isTrue();
        assertThat(config.getProperty(BrokerFactory.PROPERTY_DATABASE)).isEqualTo("native");
    }

    private Document getDocument(final InputStream is) throws SAXException, IOException, ParserConfigurationException {

        final SAXParserFactory factory = ExistSAXParserFactory.getSAXParserFactory();
        factory.setNamespaceAware(true);

        final InputSource src = new InputSource(is);
        final SAXParser parser = factory.newSAXParser();
        final XMLReader reader = parser.getXMLReader();

        reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
        reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        reader.setFeature(FEATURE_SECURE_PROCESSING, true);

        final SAXAdapter adapter = new SAXAdapter((Expression) null);
        reader.setContentHandler(adapter);
        reader.setProperty(Namespaces.SAX_LEXICAL_HANDLER, adapter);
        reader.parse(src);

        return adapter.getDocument();
    }

    private String documentToString(final Document document) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (TransformerException te) {
            throw new RuntimeException("Transformation ", te);
        }
    }

    @Test
    void testConfigurationConstructorWithAbsoluteConf(@TempDir Path existHomeDir) throws Exception {
        final Path conf = existHomeDir.resolve("test-conf.xml");
        try (InputStream in = getClass().getResourceAsStream("/conf.xml")) {
            Files.copy(in, conf);
        }
        assertThatNoException().isThrownBy(() -> new Configuration(conf.toString(), Optional.of(existHomeDir)));
    }
}