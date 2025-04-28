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
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
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
package org.exist.util.serializer;

import org.exist.dom.persistent.AttrImpl;
import org.exist.dom.QName;
import org.exist.numbering.NodeId;

/**
 * Represents a list of attributes. Each attribute is defined by
 * a {@link org.exist.dom.QName} and a value. Instances
 * of this class can be passed to 
 * {@link org.exist.util.serializer.Receiver#startElement(QName, AttrList)}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author wolf
 */
public class AttrList {

    private NodeId[] nodeIds;
	private QName[] names;
	private String[] values;
	private int[] type;
	private int size = 0;

	public AttrList(final int length) {
		nodeIds = new NodeId[length];
		names = new QName[length];
		values = new String[length];
		type = new int[length];
	}

	public AttrList() {
		this(4);
	}

    public void addAttribute(QName name, String value) {
        addAttribute(name, value, AttrImpl.CDATA);
    }

    public void addAttribute(QName name, String value, int attrType) {
        addAttribute(name, value, attrType, null);
    }
    
    public void addAttribute(QName name, String value, int attrType, NodeId nodeId) {
		ensureCapacity();
        nodeIds[size] = nodeId;
		names[size] = name;
		values[size] = value;
        type[size] = attrType;
        size++;
	}
	
	public int getLength() {
		return size;
	}
	
	public QName getQName(int pos) {
		return names[pos];
	}

    public NodeId getNodeId(int pos) {
        return nodeIds[pos];
    }
    
	public String getValue(int pos) {
		return values[pos];
	}
	
	public String getValue(QName name) {
		for(int i = 0; i < size; i++) {
			if(names[i].equals(name))
				{return values[i];}
		}
		return null;
	}

    public int getType(int pos) {
        return type[pos];
    }
    
    private void ensureCapacity() {
		if(size == names.length) {
			// resize
			final int newSize = names.length * 3 / 2;
            NodeId[] tnodeIds = new NodeId[newSize];
            System.arraycopy(nodeIds, 0, tnodeIds, 0, nodeIds.length);

			QName[] tnames = new QName[newSize];
			System.arraycopy(names, 0, tnames, 0, names.length);
			
			String[] tvalues = new String[newSize];
			System.arraycopy(values, 0, tvalues, 0, values.length);

            int[] ttype = new int[newSize];
            System.arraycopy(type, 0, ttype, 0, type.length);

            nodeIds = tnodeIds;
            names = tnames;
			values = tvalues;
            type = ttype;
        }
	}
}
