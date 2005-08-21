/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist-db.org
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

/**
 * A node with a QName, i.e. an element or attribute.
 * 
 * @author wolf
 */
public class NamedNode extends NodeImpl implements QNameable {

    protected QName nodeName = null;
    
    public NamedNode(short nodeType) {
        super(nodeType);
    }
    
    /**
     * @param nodeType
     */
    public NamedNode(short nodeType, QName qname) {
        super(nodeType);
        this.nodeName = qname;
    }

    /**
     * @param gid
     */
    public NamedNode(long gid, QName qname) {
        super(gid);
        this.nodeName = qname;
    }

    /**
     * @param nodeType
     * @param gid
     */
    public NamedNode(short nodeType, long gid, QName qname) {
        super(nodeType, gid);
        this.nodeName = qname;
    }
    
    public NamedNode(NamedNode other) {
        super(other);
        this.nodeName = other.nodeName;
    }
    
    /* (non-Javadoc)
     * @see org.exist.dom.NodeImpl#getQName()
     */
    public QName getQName() {
        return nodeName;
    }
    
    /* (non-Javadoc)
     * @see org.exist.dom.NodeImpl#clear()
     */
    public void clear() {
        super.clear();
        nodeName = null;
    }
    
    public void setNodeName(QName name) {
		nodeName = name;
	}
}
