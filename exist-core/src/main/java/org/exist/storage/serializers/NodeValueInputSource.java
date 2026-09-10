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
package org.exist.storage.serializers;

import org.exist.xquery.value.NodeValue;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.io.Reader;

/**
 * Provides a NodeValue to {@link Serializer#parse(InputSource)}.
 * This is not a general purpose {@link InputSource} implementation,
 * it should only be used with {@link Serializer}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class NodeValueInputSource extends InputSource {

    private final NodeValue nodeValue;

    /**
     * Construct a new NodeValueInputSource where the systemId
     * will be taken from the Base URI of the node.
     *
     * @param nodeValue the NodeValue.
     */
    public NodeValueInputSource(final NodeValue nodeValue) {
        this(nodeValue, nodeValue.getNode().getBaseURI());
    }

    /**
     * Construct a new NodeValueInputSource where the systemId
     * is specified.
     *
     * @param nodeValue the NodeValue.
     * @param systemId the systemId.
     */
    public NodeValueInputSource(final NodeValue nodeValue, final String systemId) {
        super(systemId);
        this.nodeValue = nodeValue;
    }

    @Override
    public void setByteStream(final InputStream byteStream) {
        throw new UnsupportedOperationException("This implementation does not support byte streams");
    }

    @Override
    public void setEncoding(final String encoding) {
        throw new UnsupportedOperationException("This implementation does not support character encodings");
    }

    @Override
    public void setCharacterStream(final Reader characterStream) {
        throw new UnsupportedOperationException("This implementation does not support character streams");
    }

    /**
     * Get the NodeValue.
     *
     * @return the NodeValue.
     */
    NodeValue getNodeValue() {
        return nodeValue;
    }
}
