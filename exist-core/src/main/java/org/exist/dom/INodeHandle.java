/*
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
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
