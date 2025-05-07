/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.util.serializer;

import org.apache.commons.io.output.StringBuilderWriter;
import org.exist.dom.QName;
import org.junit.Test;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class XMLWriterTest {

    @Test
    public void characters() throws IOException, TransformerException {
        final String inputText = "Enter Priest, &c. in procession; the Corpse of OPHELIA, LAERTES and Mourners following; KING CLAUDIUS, QUEEN GERTRUDE, their trains, &c";
        final String expectedText = inputText.replace("&", "&amp;");

        final Properties outputProperties = new Properties();
        outputProperties.getProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        try (final StringBuilderWriter writer = new StringBuilderWriter()) {
            final XMLWriter xmlWriter = new XMLWriter(writer);
            xmlWriter.setOutputProperties(outputProperties);

            xmlWriter.characters(inputText);

            final String actualText = writer.toString();
            assertEquals(expectedText, actualText);
        }
    }

    private String writeNSURI(final String localPart, final String namespaceURI) throws TransformerException {

        final QName qName = new QName(localPart, namespaceURI);

        StringBuilderWriter writer = new StringBuilderWriter();
        final XMLWriter xmlWriter = new XMLWriter(writer);
        xmlWriter.startDocument();
        xmlWriter.startElement(qName);

        xmlWriter.namespace("ns", qName.getNamespaceURI());

        xmlWriter.endElement(qName);
        xmlWriter.endDocument();

        return writer.getBuilder().toString();
    }

    @Test
    public void writeNSURI() throws TransformerException {

        assertThat(writeNSURI("qname", "http://exist.sourceforge.net/NS/exist"))
          .isEqualTo("<qname xmlns:ns=\"http://exist.sourceforge.net/NS/exist\"/>");
        assertThat(writeNSURI("qname", "http://exist.source>forge.net/NS/exist"))
          .isEqualTo("<qname xmlns:ns=\"http://exist.source&gt;forge.net/NS/exist\"/>");
        assertThat(writeNSURI("qname", "http://exist.source#forge.net/NS/exist"))
          .isEqualTo("<qname xmlns:ns=\"http://exist.source#forge.net/NS/exist\"/>");
        assertThat(writeNSURI("qname", "http://exist.source&forge.net/NS/exist"))
          .isEqualTo("<qname xmlns:ns=\"http://exist.source&amp;forge.net/NS/exist\"/>");
    }

    @Test
    public void writeChars1() throws IOException {
        final String inputText = "Enter Priest, &c. in procession; the Corpse of OPHELIA, LAERTES and Mourners following; KING CLAUDIUS, QUEEN GERTRUDE, their trains, &c";
        final String expectedText = inputText.replace("&", "&amp;");
        writeChars(inputText, expectedText);
    }

    @Test
    public void writeChars2() throws IOException {
        final String inputText = "<a><b></a>";
        final String expectedText = inputText.replace("<", "&lt;").replace(">", "&gt;");
        writeChars(inputText, expectedText);
    }

    private void writeChars(final String inputText, final String expectedText) throws IOException {
        try (final StringBuilderWriter writer = new StringBuilderWriter()) {
            final XMLWriter xmlWriter = new XMLWriter(writer);

            xmlWriter.writeChars(inputText, false);

            final String actualText = writer.toString();
            assertEquals(expectedText, actualText);
        }
    }
}
