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
package org.exist.storage.lock;

import net.jcip.annotations.NotThreadSafe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A lock event listener which formats events as XML and writes them to a file
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@NotThreadSafe
public class LockEventXmlListener implements LockTable.LockEventListener {

    private final static Logger LOG = LogManager.getLogger(LockEventXmlListener.class);

    private volatile boolean registered = false;

    private final Path xmlFile;
    private final boolean prettyPrint;

    private OutputStream os = null;
    private XMLStreamWriter xmlStreamWriter = null;


    public LockEventXmlListener(final Path xmlFile) {
        this(xmlFile, false);
    }

    public LockEventXmlListener(final Path xmlFile, final boolean prettyPrint) {
        this.xmlFile = xmlFile;
        this.prettyPrint = prettyPrint;
    }

    @Override
    public void registered() {
        this.registered = true;
        try {
            this.os = Files.newOutputStream(xmlFile,
                    StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            this.xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(os, UTF_8.name());

            this.xmlStreamWriter.writeStartDocument(UTF_8.name(), "1.0");
            this.xmlStreamWriter.writeStartElement("lockEvents");
        } catch (final IOException | XMLStreamException e) {
            LOG.error(e);
        }
    }

    @Override
    public void unregistered() {
        try {
            if(xmlStreamWriter != null) {
                this.xmlStreamWriter.writeEndElement();
                this.xmlStreamWriter.writeEndDocument();
                this.xmlStreamWriter.close();
                this.xmlStreamWriter = null;
            }
        } catch (final XMLStreamException e) {
            LOG.error(e);
        }

        try {
            if(os != null) {
                this.os.close();
                this.os = null;
            }
        } catch (final IOException e) {
            LOG.error(e);
        }

        this.registered = false;
    }

    public boolean isRegistered() {
        return registered;
    }

    @Override
    public void accept(final LockTable.LockEventType lockEventType, final long timestamp, final long groupId,
            final LockTable.Entry entry) {
        if(!registered) {
            return;
        }

        if(xmlStreamWriter != null) {
            try {
                xmlStreamWriter.writeStartElement("lockEvent");

                    // read count first to ensure memory visibility from volatile!
                    final int localCount = entry.count;

                    writeLongElement("timestamp", timestamp);
                    writeStringElement("lockEventType", lockEventType.name());
                    writeLongElement("groupId", groupId);
                    writeStringElement("id", entry.id);
                    writeStringElement("thread", entry.owner);
                    if (entry.stackTraces != null) {
                        for (final StackTraceElement[] stackTrace : entry.stackTraces) {
                            stackTraceToXml(stackTrace);
                        }
                    }

                    xmlStreamWriter.writeStartElement("lock");
                        writeStringElement("type", entry.lockType.name());
                        writeStringElement("mode", entry.lockMode.name());
                        writeIntElement("holdCount", localCount);
                    xmlStreamWriter.writeEndElement();

                xmlStreamWriter.writeEndElement();
            } catch(final XMLStreamException e) {
                LOG.error(e);
            }
        }
    }

    private void writeStringElement(final String name, final String value) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(name);
        xmlStreamWriter.writeCharacters(value);
        xmlStreamWriter.writeEndElement();
    }

    private void writeLongElement(final String name, final long value) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(name);
        xmlStreamWriter.writeCharacters(Long.toString(value));
        xmlStreamWriter.writeEndElement();
    }

    private void writeIntElement(final String name, final int value) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(name);
        xmlStreamWriter.writeCharacters(Integer.toString(value));
        xmlStreamWriter.writeEndElement();
    }

    private void stackTraceToXml(@Nullable final StackTraceElement[] stackTrace) throws XMLStreamException {
        xmlStreamWriter.writeStartElement("trace");

            if(stackTrace != null) {
                for(final StackTraceElement stackTraceElement : stackTrace) {
                    xmlStreamWriter.writeStartElement("frame");
                        xmlStreamWriter.writeAttribute("methodName", stackTraceElement.getMethodName());
                        xmlStreamWriter.writeAttribute("className", stackTraceElement.getClassName());
                        xmlStreamWriter.writeAttribute("lineNumber",  Integer.toString(stackTraceElement.getLineNumber()));
                    xmlStreamWriter.writeEndElement();
                }
            }

        xmlStreamWriter.writeEndElement();
    }
}
