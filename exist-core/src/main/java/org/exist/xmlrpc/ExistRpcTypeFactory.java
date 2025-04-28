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
package org.exist.xmlrpc;

import com.evolvedbinary.j8fu.tuple.*;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.exist.security.internal.aider.ACEAider;
import org.xml.sax.SAXException;

/**
 * Custom XML-RPC type factory to enable the use
 * of extended types in XML-RPC.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ExistRpcTypeFactory extends TypeFactoryImpl {
    public ExistRpcTypeFactory(final XmlRpcController controller) {
        super(controller);
    }

    @Override
    public TypeParser getParser(final XmlRpcStreamConfig config, final NamespaceContextImpl context, final String uri, final String localName) {
        if (TupleSerializer.TUPLE_TAG.equals(localName)) {
            return new TupleParser(config, context, this);

        } else if (ACEAiderSerializer.ACEAIDER_TAG.equals(localName)) {
            return new ACEAiderParser(config, context, this);

        } else {
            return super.getParser(config, context, uri, localName);
        }
    }

    @Override
    public TypeSerializer getSerializer(final XmlRpcStreamConfig config, final Object object) throws SAXException {
        if (object instanceof Tuple) {
            return new TupleSerializer(this, config);

        } else if (object instanceof ACEAider) {
            return new ACEAiderSerializer(this, config);

        } else {
            return super.getSerializer(config, object);
        }
    }
}
