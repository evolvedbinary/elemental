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
package org.exist.dom;

import org.exist.numbering.NodeId;

/**
 * Interface for handling Nodes in eXist
 * used for both persistent and
 * in-memory nodes.
 * 
 * @param <D> The type of the persistent
 * or in-memory document
 * 
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface INodeHandle<D extends org.w3c.dom.Document> {
    
    /**
     * Get the ID of the Node
     * 
     * @return The ID of the Node
     */
    public NodeId getNodeId();
    
    /**
     * Get the type of the node
     * @return the type of the node
     */
    public short getNodeType(); //TODO convert to enum? what about persistence of the enum id (if it is ever persisted?)?
    
    /**
     * @see org.w3c.dom.Node#getOwnerDocument()
     * 
     * @return The persistent Owner Document
     */
    public D getOwnerDocument(); //TODO consider extracting D into "org.exist.dom.IDocument extends org.w3c.com.Document" and returning an IDocument here
}
