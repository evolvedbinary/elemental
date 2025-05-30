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
package org.exist.dom.persistent;

import org.exist.dom.INode;
import org.exist.storage.NodePath;


//TODO do we really need to extend Visitable any more?
public interface IStoredNode<T extends IStoredNode> extends INode<DocumentImpl, T>, NodeHandle, Visitable {

    //<editor-fold desc="serialization">

    /**
     * Serialize the state of this node
     * into a byte array.
     *
     * @return A byte array containing the
     * serialization of the node
     */
    public byte[] serialize();

    //public static StoredNode deserialize(byte[] data, int start, int len);
    //IStoredNode deserialize(); //TODO perhaps use package protected method?

    //</editor-fold>

    /**
     * Set the Document that this node belongs to
     *
     * Counterpart to @see org.exist.dom.INode#getOwnerDocument()
     *
     * @param doc The document that this node belongs to
     */
    public void setOwnerDocument(DocumentImpl doc);


    //<editor-fold desc="temp">

    //TODO see StoredNode.getParentStoredNode and StoredNode.getParentNode, should be able to remove in favour of getParentNode() in future.
    public IStoredNode getParentStoredNode();
    //</editor-fold>


    /**
     * Returns a count of the number of children.
     *
     * @return the number of children
     */
    public int getChildCount(); //TODO also available in memtree.ElementImpl - consider moving to org.exist.dom.INode (also this is only really used for ElementImpl and DocumentImpl)

    /**
     * Set the node to dirty to indicate
     * that nodes were inserted at the start
     * or in the middle of its children.
     *
     * @param dirty the dirty status of the node
     */
    public void setDirty(boolean dirty);


    public NodePath getPath();

    public NodePath getPath(NodePath parentPath); //TODO seems to be ElementImpl specific see StoredNode

    /**
     * Release the node.
     *
     * See {@link StoredNode#release()}.
     *
     * This function currently does two things, (1) it clears the state,
     * and (2) it then returns the object to NodePool.
     *
     * NOTE(AR) we should try and clean this up!
     *
     * org.exist.Indexer seems to borrow and return to the pool
     * org.exist.memtree.DOMIndexer only seems to borrow nodes
     * org.exist.serializers.NativeSerializer only seems to return nodes
     * org.exist.dom.persistent.*Impl#deserialize(...) seem to have support for pooling
     * yet this is set to false in the invoking code!
     */
    public void release();
}
