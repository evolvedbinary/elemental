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
package org.exist.stax;

import org.exist.dom.persistent.IStoredNode;
import org.exist.dom.persistent.NodeHandle;
import org.exist.storage.DBBroker;
import org.exist.util.XMLString;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;

public interface IEmbeddedXMLStreamReader extends ExtendedXMLStreamReader {

    /**
     * Reposition the stream reader to another start node.
     *
     * NOTE: This maybe in a different document!
     *
     * @param broker the database broker.
     * @param node the new start node.
     * @param reportAttributes if set to true, attributes will be reported as top-level events.
     *
     * @throws java.io.IOException if an error occurs whilst repositioning the stream
     */
    void reposition(final DBBroker broker, final NodeHandle node, final boolean reportAttributes) throws IOException;

    /**
     * Deserialize the node at the current position of the cursor and return
     * it as a {@link org.exist.dom.persistent.IStoredNode}.
     *
     * @return the node at the current position.
     */
    IStoredNode getNode();

    /**
     * Returns the last node in document sequence that occurs before the
     * current node. Usually used to find the last child before an END_ELEMENT
     * event.
     *
     * @return the last node in document sequence before the current node
     */
    IStoredNode getPreviousNode();

    /**
     * Iterates over each node until
     * the filter returns false
     *
     * @param filter the filter
     *
     * @throws XMLStreamException if an error occurs whilst iterating.
     */
    void filter(StreamFilter filter) throws XMLStreamException;

    /**
     * Get the Node Type
     * as used in the persistent
     * DOM.
     *
     * Types are defined in {@link org.exist.storage.Signatures}
     *
     * @return the node type
     */
    short getNodeType();

    /**
     * Returns the current value of the parse event as an XMLString,
     * this returns the string value of a CHARACTERS event,
     * returns the value of a COMMENT, the replacement value
     * the string value of a CDATA section or
     * the string value for a SPACE event.
     *
     * @return the current text or the empty text
     */
    XMLString getXMLText();
}
