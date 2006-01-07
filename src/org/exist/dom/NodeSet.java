/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-06 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.dom;

import java.util.Iterator;

import org.exist.xquery.XPathException;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.Type;
import org.w3c.dom.NodeList;

/**
 * @author wolf
 */
public interface NodeSet extends Sequence, NodeList {
	
	public final static int ANCESTOR = 0;

	public final static int DESCENDANT = 1;

	public final static int PRECEDING = 2;

	public final static int FOLLOWING = 3;

	/**
	 * Constant representing an empty node set.
	 */
	public final static NodeSet EMPTY_SET = new EmptyNodeSet();
		
	/**
	 * Return an iterator on the nodes in this list. The iterator returns nodes
	 * according to the internal ordering of nodes (i.e. level first), not in document-
	 * order.
	 * 
	 * @return
	 */
	public Iterator iterator();
	
	/**
	 * Check if this node set contains a node matching the document and
	 * node-id of the given NodeProxy object.
	 * 
	 * @param proxy
	 * @return
	 */
	public boolean contains(NodeProxy proxy);
	
	/**
	 * Check if this node set contains nodes belonging to the given document.
	 * 
	 * @param doc
	 * @return
	 */
	public boolean containsDoc(DocumentImpl doc);
	
	public DocumentSet getDocumentSet();
	
	/**
	 * Add a new proxy object to the node set. Please note: node set
	 * implementations may allow duplicates.
	 * 
	 * @param proxy
	 */
	public void add(NodeProxy proxy);
	
	/**
	 * Add a proxy object to the node set. The sizeHint parameter
	 * gives a hint about the number of items to be expected for the
	 * current document.
	 * 
	 * @param proxy
	 * @param sizeHint
	 */
	public void add(NodeProxy proxy, int sizeHint);
	
	/**
	 * Add all nodes from the given node set.
	 * 
	 * @param other
	 */
	public void addAll(NodeSet other);
	
	/**
	 * Return the number of nodes contained in this node set.
	 */
	public int getLength();
	
	/**
	 * Get the node at position pos within this node set.
	 * @param pos
	 * @return
	 */
	public NodeProxy get(int pos);
	
	/**
	 * Get a node from this node set matching the document and node id of
	 * the given NodeProxy.
	 *  
	 * @param p
	 * @return
	 */
	public NodeProxy get(NodeProxy p);
	
	/**
	 * Get a node from this node set matching the document and node id.
	 * 
	 * @param doc
	 * @param nodeId
	 * @return
	 */
	public NodeProxy get(DocumentImpl doc, long nodeId);
		
	/**
	 * Check if any child nodes are found within this node set for a given
	 * set of potential parent nodes.
	 * 
	 * If mode is {@link #DESCENDANT}, the returned node set will contain
	 * all child nodes found in this node set for each parent node. If mode is
	 * {@link #ANCESTOR}, the returned set will contain those parent nodes,
	 * for which children have been found.
	 *  
	 * @param al a node set containing potential parent nodes
	 * @param mode selection mode
	 * @return
	 */
	public NodeSet selectParentChild(NodeSet al, int mode);
	
	/**
	 * Check if any child nodes are found within this node set for a given
	 * set of potential parent nodes.
	 * 
	 * If mode is {@link #DESCENDANT}, the returned node set will contain
	 * all child nodes found in this node set for each parent node. If mode is
	 * {@link #ANCESTOR}, the returned set will contain those parent nodes,
	 * for which children have been found.
	 *  
	 * @param al a node set containing potential parent nodes
	 * @param mode selection mode
	 * @param rememberContext if true, add the matching nodes to the context node
	 * list of each returned node (this is used to track matches for predicate evaluation)
	 * @return
	 */
	
	public NodeSet selectParentChild(
		NodeSet al,
		int mode,
		boolean rememberContext);
		
	/**
	 * Check if any descendant nodes are found within this node set for a given
	 * set of potential ancestor nodes.
	 * 
	 * If mode is {@link #DESCENDANT}, the returned node set will contain
	 * all descendant nodes found in this node set for each ancestor. If mode is
	 * {@link #ANCESTOR}, the returned set will contain those ancestor nodes,
	 * for which descendants have been found.
	 *  
	 * @param al a node set containing potential parent nodes
	 * @param mode selection mode
	 * @param includeSelf if true, check if the ancestor node itself is contained in
	 * the set of descendant nodes (descendant-or-self axis)
	 * @param rememberContext if true, add the matching nodes to the context node
	 * list of each returned node (this is used to track matches for predicate evaluation)
	 * @return
	 */
	public NodeSet selectAncestorDescendant(
		NodeSet al,
		int mode,
		boolean includeSelf,
		boolean rememberContext);
		
	/**
	 * For a given set of potential ancestor nodes, return all ancestors
	 * having descendants in this node set.
	 *
	 *@param  al    node set containing potential ancestors
	 * @param includeSelf if true, check if the ancestor node itself is contained
	 * in this node set (ancestor-or-self axis)
	 * @param rememberContext if true, add the matching nodes to the context node
	 * list of each returned node (this is used to track matches for predicate evaluation)
	 *@return
	 */
	public NodeSet selectAncestors(
		NodeSet al,
		boolean includeSelf,
		boolean rememberContext);
		
	/**
	 * Select all nodes from the passed node set, which
	 * are preceding or following siblings of the nodes in
	 * this set. If mode is {@link #FOLLOWING}, only nodes following
	 * the context node are selected. {@link #PRECEDING} selects
	 * preceding nodes.
	 * 
	 * @param siblings a node set containing potential siblings
	 * @param mode either FOLLOWING or PRECEDING
	 * @return
	 */
	public NodeSet selectSiblings(NodeSet siblings, int mode);
	
	public NodeSet selectFollowing(NodeSet following) throws XPathException;
		
    public NodeSet selectPreceding(NodeSet preceding) throws XPathException;
    
	/**
	 * Check if the node identified by its node id has an ancestor contained in this node set
	 * and return the ancestor found.
	 *
	 * If directParent is true, only immediate ancestors (parents) are considered.
	 * Otherwise the method will call itself recursively for all the node's
	 * parents.
	 *
	 * If includeSelf is true, the method returns also true if
	 * the node itself is contained in the node set.
	 */
	public NodeProxy parentWithChild(
		DocumentImpl doc,
		long gid,
		boolean directParent,
		boolean includeSelf);
		
	/**
	 * Check if the node identified by its node id has an ancestor contained in this node set
	 * and return the ancestor found.
	 *
	 * If directParent is true, only immediate ancestors (parents) are considered.
	 * Otherwise the method will call itself recursively for all the node's
	 * parents.
	 *
	 * If includeSelf is true, the method returns also true if
	 * the node itself is contained in the node set.
	 */
	public NodeProxy parentWithChild(
		DocumentImpl doc,
		long gid,
		boolean directParent,
		boolean includeSelf,
		int level);
		
	/**
	 * Check if the given node has an ancestor contained in this node set
	 * and return the ancestor found.
	 *
	 * If directParent is true, only immediate ancestors (parents) are considered.
	 * Otherwise the method will call itself recursively for all the node's
	 * parents.
	 *
	 * If includeSelf is true, the method returns also true if
	 * the node itself is contained in the node set.
	 */
	public NodeProxy parentWithChild(
		NodeProxy proxy,
		boolean directParent,
		boolean includeSelf,
		int level);
	
	/**
	 * Return a new node set containing the parent nodes of all nodes in the 
	 * current set.
	 * @return
	 */
	public NodeSet getParents(boolean rememberContext);
	
    public NodeSet getAncestors(boolean rememberContext, boolean includeSelf);
    
    public NodeSet directSelectAttribute(QName qname, boolean rememberContext);
    
	/**
	 * If all nodes in this set have an index, returns the common
	 * supertype used to build the index, e.g. xs:integer or xs:string.
	 * If the nodes have different index types or no node has been indexed,
	 * returns {@link Type#ITEM}.
	 * 
	 * @see org.exist.xquery.GeneralComparison
	 * @see org.exist.xquery.ValueComparison
	 * @return
	 */
	public int getIndexType();
	
	public boolean hasTextIndex();
	
	public boolean hasMixedContent();
	
	/**
	 * Create a sub-range of this node set containing the range of nodes greater than or including
	 * the lower node and smaller than or including the upper node. Matching nodes are added to the
	 * given result node set.
	 * 
	 * @param doc
	 * @param lower
	 * @param upper
	 * @return
	 */
	public void getRange(NodeSet result, DocumentImpl doc, long lower, long upper);
	
	/**
	 * Get a hint about how many nodes in this node set belong to the 
	 * specified document. This is just used for allocating new node sets.
	 * The information does not need to be exact. -1 is returned if the
	 * size cannot be determined (the default).
	 * 
	 * @param doc
	 * @return
	 */
	public int getSizeHint(DocumentImpl doc);
	
	/**
	 * Return a new node set, which represents the intersection of the current
	 * node set with the given node set.
	 * 
	 * @param other
	 * @return
	 */
	public NodeSet intersection(NodeSet other);
	
	/**
	 * Return a new node set, containing all nodes in this node set that
	 * are contained or have descendants in the other node set.
	 * 
	 * @param other
	 * @return
	 */
	public NodeSet deepIntersection(NodeSet other);
	
	/**
	 * Return a new node set which represents the union of the
	 * current node set and the given node set.
	 * 
	 * @param other
	 * @return
	 */
	public NodeSet union(NodeSet other);
	
	/**
	 * Return a new node set containing all nodes from this node set
	 * except those nodes which are also contained in the argument node set.
	 * 
	 * @param other
	 * @return
	 */
	public NodeSet except(NodeSet other);
	
	public NodeSet getContextNodes(boolean rememberContext);
	
	public boolean hasChanged(int previousState);
	
	public int getState();
}