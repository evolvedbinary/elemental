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
