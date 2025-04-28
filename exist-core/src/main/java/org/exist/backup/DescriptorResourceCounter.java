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
package org.exist.backup;

import net.jcip.annotations.NotThreadSafe;
import org.exist.Namespaces;
import org.exist.util.ExistSAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NotThreadSafe
public class DescriptorResourceCounter {

    private final static SAXParserFactory saxFactory = ExistSAXParserFactory.getSAXParserFactory();
    static {
        saxFactory.setNamespaceAware(true);
    }

    private final XMLReader xmlReader;
    private final CounterHandler counterHandler;

    public DescriptorResourceCounter() throws ParserConfigurationException, SAXException {
        final SAXParser saxParser = saxFactory.newSAXParser();
        this.xmlReader = saxParser.getXMLReader();
        this.counterHandler = new CounterHandler();

        xmlReader.setContentHandler(counterHandler);
    }

    public long count(final InputStream descriptorInputStream) throws IOException, SAXException {
        xmlReader.parse(new InputSource(descriptorInputStream));
        final long numberOfFiles = counterHandler.numberOfFiles;

        // reset
        counterHandler.numberOfFiles = 0;
        return numberOfFiles;
    }

    private static class CounterHandler extends DefaultHandler {
        long numberOfFiles = 0;

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
            if (Namespaces.EXIST_NS.equals(uri) && "resource".equals(localName)) {
                numberOfFiles++;
            }
        }
    }
}
