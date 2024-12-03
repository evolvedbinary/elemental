/*
 * Copyright (C) 2014 Evolved Binary Ltd
 *
 * Changes made by Evolved Binary are proprietary and are not Open Source.
 */
package org.exist.util.serializer;

import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.Test;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Properties;

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

    @Test
    public void writeChars() throws IOException {
        final String inputText = "Enter Priest, &c. in procession; the Corpse of OPHELIA, LAERTES and Mourners following; KING CLAUDIUS, QUEEN GERTRUDE, their trains, &c";
        final String expectedText = inputText.replace("&", "&amp;");

        try (final StringBuilderWriter writer = new StringBuilderWriter()) {
            final XMLWriter xmlWriter = new XMLWriter(writer);

            xmlWriter.writeChars(inputText, false);

            final String actualText = writer.toString();
            assertEquals(expectedText, actualText);
        }
    }
}
