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
package org.exist.dom.persistent;

import org.exist.numbering.NodeId;
import org.exist.storage.Signatures;
import org.exist.util.ByteConversion;
import org.exist.util.pool.NodePool;
import org.exist.xquery.Expression;
import org.w3c.dom.Comment;
import org.w3c.dom.Node;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CommentImpl extends AbstractCharacterData<CommentImpl> implements Comment {

    public CommentImpl() {
        this((Expression) null);
    }

    public CommentImpl(final Expression expression) {
        super(expression, Node.COMMENT_NODE);
    }

    public CommentImpl(final String data) {
        this(null, data);
    }

    public CommentImpl(final Expression expression, final String data) {
        super(expression, Node.COMMENT_NODE, data);
    }

    public CommentImpl(final char[] data, final int start, final int howmany) {
        this(null, data, start, howmany);
    }

    public CommentImpl(final Expression expression, final char[] data, final int start, final int howmany) {
        super(expression, Node.COMMENT_NODE, data, start, howmany);
    }

    @Override
    public String toString() {
        return "<!-- " + cdata.toString() + " -->";
    }

    /**
     * Serializes a (persistent DOM) Comment to a byte array
     *
     * data = signature nodeIdUnitsLength nodeId cdata
     *
     * signature = [byte] 0x60
     *
     * nodeIdUnitsLength = [short] (2 bytes) The number of units of the comment's NodeId
     * nodeId = {@link org.exist.numbering.DLNBase#serialize(byte[], int)}
     *
     * cdata = jUtf8
     *
     * jUtf8 = {@link java.io.DataOutputStream#writeUTF(java.lang.String)}
     */
    @Override
    public byte[] serialize() {
        final String s = cdata.toString();
        final byte[] cd = s.getBytes(UTF_8);

        final int nodeIdLen = nodeId.size();
        final byte[] data = new byte[StoredNode.LENGTH_SIGNATURE_LENGTH + NodeId.LENGTH_NODE_ID_UNITS +
            +nodeIdLen + cd.length];
        int pos = 0;
        data[pos] = (byte) (Signatures.Comm << 0x5);
        pos += StoredNode.LENGTH_SIGNATURE_LENGTH;
        ByteConversion.shortToByte((short) nodeId.units(), data, pos);
        pos += NodeId.LENGTH_NODE_ID_UNITS;
        nodeId.serialize(data, pos);
        pos += nodeIdLen;
        System.arraycopy(cd, 0, data, pos, cd.length);
        return data;
    }

    public static StoredNode deserialize(final byte[] data, final int start, final int len,
                                         final DocumentImpl doc, final boolean pooled) {
        int pos = start;
        pos += LENGTH_SIGNATURE_LENGTH;
        final int dlnLen = ByteConversion.byteToShort(data, pos);
        pos += NodeId.LENGTH_NODE_ID_UNITS;
        final NodeId dln = doc.getBrokerPool().getNodeFactory().createFromData(dlnLen, data, pos);
        int nodeIdLen = dln.size();
        pos += nodeIdLen;
        final String cdata = new String(data, pos, len - (pos - start), UTF_8);
        //OK : we have the necessary material to build the comment
        final CommentImpl comment;
        if(pooled) {
            comment = (CommentImpl) NodePool.getInstance().borrowNode(Node.COMMENT_NODE);
        } else {
            comment = new CommentImpl((Expression) null);
        }
        comment.setNodeId(dln);
        comment.appendData(cdata);
        return comment;
    }
}

