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

import java.io.Writer;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import org.exist.storage.lock.Lock.LockMode;
import org.exist.storage.lock.Lock.LockType;
import org.exist.storage.lock.LockTable.LockCountTraces;
import org.exist.storage.lock.LockTable.LockModeOwner;
import org.exist.xquery.value.TimeUtils;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Utilities for working with the Lock Table
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class LockTableUtils {

    private static final String EOL = System.getProperty("line.separator");

    public static String stateToString(final LockTable lockTable, final boolean includeStack) {
        final Map<String, Map<LockType, List<LockModeOwner>>> attempting = lockTable.getAttempting();
        final Map<String, Map<LockType, Map<LockMode, Map<String, LockCountTraces>>>> acquired = lockTable.getAcquired();

        final StringBuilder builder = new StringBuilder();

        builder
                .append(EOL)
                .append("Acquired Locks").append(EOL)
                .append("------------------------------------").append(EOL);

        for(final Map.Entry<String, Map<LockType, Map<LockMode, Map<String, LockCountTraces>>>> acquire : acquired.entrySet()) {
            builder.append(acquire.getKey()).append(EOL);
            for(final Map.Entry<LockType, Map<LockMode, Map<String, LockCountTraces>>> type : acquire.getValue().entrySet()) {
                builder.append('\t').append(type.getKey()).append(EOL);
                for(final Map.Entry<LockMode, Map<String, LockCountTraces>> lockModeOwners : type.getValue().entrySet()) {
                    builder
                            .append("\t\t").append(lockModeOwners.getKey())
                            .append('\t');

                    boolean firstOwner = true;
                    for(final Map.Entry<String, LockCountTraces> ownerHoldCount : lockModeOwners.getValue().entrySet()) {
                        if(!firstOwner) {
                            builder.append(", ");
                        } else {
                            firstOwner = false;
                        }
                        final LockCountTraces holdCount = ownerHoldCount.getValue();
                        builder.append(ownerHoldCount.getKey())
                                .append(" (count=").append(holdCount.count).append(")");
                        if (holdCount.traces != null && includeStack) {
                            for (int i = 0; i < holdCount.traces.size(); i++) {
                                 final StackTraceElement[] trace = holdCount.traces.get(i);
                                 builder
                                         .append(EOL)
                                         .append("\t\t\tTrace ").append(i).append(": ").append(EOL);
                                for (StackTraceElement stackTraceElement : trace) {
                                    builder.append("\t\t\t\t").append(stackTraceElement).append(EOL);
                                }
                            }
                        }
                    }
                    builder.append(EOL);
                }
            }
        }

        builder.append(EOL).append(EOL);

        builder
                .append("Attempting Locks").append(EOL)
                .append("------------------------------------").append(EOL);

        for(final Map.Entry<String, Map<Lock.LockType, List<LockTable.LockModeOwner>>> attempt : attempting.entrySet()) {
            builder.append(attempt.getKey()).append(EOL);
            for(final Map.Entry<Lock.LockType, List<LockTable.LockModeOwner>> type : attempt.getValue().entrySet()) {
                builder.append('\t').append(type.getKey()).append(EOL);
                for(final LockTable.LockModeOwner lockModeOwner : type.getValue()) {
                    builder
                            .append("\t\t").append(lockModeOwner.getLockMode())
                            .append('\t').append(lockModeOwner.getOwnerThread());
                            if (lockModeOwner.trace != null && includeStack) {
                                builder.append(EOL).append("\t\t\tTrace ").append(": ").append(EOL);
                                for (int i = 0; i < lockModeOwner.trace.length; i++) {
                                    builder.append("\t\t\t\t").append(lockModeOwner.trace[i]).append(EOL);
                                }
                            }
                            builder.append(EOL);
                }
            }
        }

        return builder.toString();
    }

    public static void stateToXml(final LockTable lockTable, final boolean includeStack, final Writer writer) throws XMLStreamException {
        final GregorianCalendar cal = new GregorianCalendar();

        final Map<String, Map<LockType, List<LockModeOwner>>> attempting = lockTable.getAttempting();
        final Map<String, Map<LockType, Map<LockMode, Map<String, LockCountTraces>>>> acquired = lockTable.getAcquired();

        final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        final XMLStreamWriter xmlWriter = outputFactory.createXMLStreamWriter(writer);

        xmlWriter.writeStartDocument();
        xmlWriter.writeStartElement("lock-table");
        final XMLGregorianCalendar xmlCal = TimeUtils.getInstance().newXMLGregorianCalendar(cal);
        xmlWriter.writeAttribute("timestamp", xmlCal.toXMLFormat());

        // acquired locks
        xmlWriter.writeStartElement("acquired");
        for(final Map.Entry<String, Map<LockType, Map<LockMode, Map<String, LockCountTraces>>>> acquire : acquired.entrySet()) {
            xmlWriter.writeStartElement("lock");
            xmlWriter.writeAttribute("id", acquire.getKey());

            for(final Map.Entry<LockType, Map<LockMode, Map<String, LockCountTraces>>> type : acquire.getValue().entrySet()) {
                xmlWriter.writeStartElement("type");
                xmlWriter.writeAttribute("id", type.getKey().name());

                for(final Map.Entry<LockMode, Map<String, LockCountTraces>> lockModeOwners : type.getValue().entrySet()) {
                    xmlWriter.writeStartElement("mode");
                    xmlWriter.writeAttribute("id", lockModeOwners.getKey().name());

                    for(final Map.Entry<String, LockCountTraces> ownerHoldCount : lockModeOwners.getValue().entrySet()) {
                        xmlWriter.writeStartElement("thread");
                        xmlWriter.writeAttribute("id", ownerHoldCount.getKey());
                        final LockCountTraces holdCount = ownerHoldCount.getValue();
                        xmlWriter.writeAttribute("hold-count", Integer.toString(holdCount.count));

                        if (holdCount.traces != null && includeStack) {
                            for (int i = 0; i < holdCount.traces.size(); i++) {
                                xmlWriter.writeStartElement("stack-trace");
                                xmlWriter.writeAttribute("index", Integer.toString(i));

                                final StackTraceElement[] trace = holdCount.traces.get(i);
                                for (int j = 0; j < trace.length; j++) {
                                    xmlWriter.writeStartElement("call");
                                    final StackTraceElement call = trace[j];
                                    xmlWriter.writeAttribute("index", Integer.toString(j));
                                    xmlWriter.writeAttribute("class", call.getClassName());
                                    xmlWriter.writeAttribute("method", call.getMethodName());
                                    xmlWriter.writeAttribute("file", call.getFileName());
                                    xmlWriter.writeAttribute("line", Integer.toString(call.getLineNumber()));
                                    xmlWriter.writeCharacters(call.toString());
                                    xmlWriter.writeEndElement();
                                }

                                xmlWriter.writeEndElement();
                            }
                        }
                        xmlWriter.writeEndElement();
                    }
                    xmlWriter.writeEndElement();
                }
                xmlWriter.writeEndElement();
            }
            xmlWriter.writeEndElement();
        }
        xmlWriter.writeEndElement();


        // attempting locks
        xmlWriter.writeStartElement("attempting");
        for(final Map.Entry<String, Map<Lock.LockType, List<LockTable.LockModeOwner>>> attempt : attempting.entrySet()) {
            xmlWriter.writeStartElement("lock");
            xmlWriter.writeAttribute("id", attempt.getKey());

            for(final Map.Entry<Lock.LockType, List<LockTable.LockModeOwner>> type : attempt.getValue().entrySet()) {
                xmlWriter.writeStartElement("type");
                xmlWriter.writeAttribute("id", type.getKey().name());

                for(final LockTable.LockModeOwner lockModeOwner : type.getValue()) {
                    xmlWriter.writeStartElement("mode");
                    xmlWriter.writeAttribute("id", lockModeOwner.getLockMode().name());

                    xmlWriter.writeStartElement("thread");
                    xmlWriter.writeAttribute("id", lockModeOwner.getOwnerThread());

                    if (lockModeOwner.trace != null && includeStack) {
                        xmlWriter.writeStartElement("stack-trace");

                        for (int i = 0; i < lockModeOwner.trace.length; i++) {
                            xmlWriter.writeStartElement("call");
                            final StackTraceElement call = lockModeOwner.trace[i];
                            xmlWriter.writeAttribute("index", Integer.toString(i));
                            xmlWriter.writeAttribute("class", call.getClassName());
                            xmlWriter.writeAttribute("method", call.getMethodName());
                            xmlWriter.writeAttribute("file", call.getFileName());
                            xmlWriter.writeAttribute("line", Integer.toString(call.getLineNumber()));
                            xmlWriter.writeCharacters(call.toString());
                            xmlWriter.writeEndElement();
                        }

                        xmlWriter.writeEndElement();
                    }

                    xmlWriter.writeEndElement();

                    xmlWriter.writeEndElement();
                }

                xmlWriter.writeEndElement();
            }

            xmlWriter.writeEndElement();
        }

        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement();
        xmlWriter.writeEndDocument();
    }
}
