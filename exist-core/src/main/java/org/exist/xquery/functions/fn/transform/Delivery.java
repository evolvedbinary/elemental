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
package org.exist.xquery.functions.fn.transform;

import net.sf.saxon.s9api.*;
import net.sf.saxon.serialize.SerializationProperties;
import org.exist.dom.memtree.DocumentBuilderReceiver;
import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;

import java.io.StringWriter;
import java.util.Objects;

class Delivery {

    enum Format {
        DOCUMENT,
        SERIALIZED,
        RAW
    }

    final XQueryContext context;
    final Format format;
    final SerializationProperties serializationProperties;
    MemTreeBuilder builder;
    StringWriter stringWriter;

    RawDestination rawDestination;

    Delivery(final XQueryContext context, final Format format, final SerializationProperties serializationProperties) {
        this.context = context;
        this.format = format;
        this.serializationProperties = serializationProperties;
    }

    final Destination createDestination(final Xslt30Transformer xslt30Transformer, final boolean forceCreation) {
        switch (format) {
            case DOCUMENT:
                if (!forceCreation) {
                    this.builder = context.getDocumentBuilder();
                } else {
                    this.builder = new MemTreeBuilder(context);
                    this.builder.startDocument();
                }
                return new SAXDestination(new DocumentBuilderReceiver(builder));
            case SERIALIZED:
                final Serializer serializer = xslt30Transformer.newSerializer();
                final SerializationProperties stylesheetProperties = serializer.getSerializationProperties();

                final SerializationProperties combinedProperties =
                        SerializationParameters.combinePropertiesAndCharacterMaps(
                                stylesheetProperties,
                                serializationProperties);

                serializer.setOutputProperties(combinedProperties);
                stringWriter = new StringWriter();
                serializer.setOutputWriter(stringWriter);
                return serializer;
            case RAW:
                this.rawDestination = new RawDestination();
                return rawDestination;
            default:
                return null;
        }
    }

    private String getSerializedString() {

        if (stringWriter == null) {
            return null;
        }
        return stringWriter.getBuffer().toString();
    }

    private DocumentImpl getDocument() {
        if (builder == null) {
            return null;
        }
        return builder.getDocument();
    }

    private XdmValue getXdmValue() {
        if (rawDestination == null) {
            return null;
        }
        return rawDestination.getXdmValue();
    }

    Sequence convert() throws XPathException {

        switch (format) {
            case SERIALIZED:
                return new StringValue(getSerializedString());
            case RAW:
                return Convert.ToExist.of(Objects.requireNonNull(getXdmValue()));
            case DOCUMENT:
            default:
                return getDocument();
        }
    }
}
