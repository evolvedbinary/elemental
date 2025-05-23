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
package org.exist.xquery.functions.fn;

import it.unimi.dsi.fastutil.shorts.ShortSet;
import org.exist.xquery.*;
import org.exist.xquery.value.*;
import org.w3c.dom.Node;

import java.net.URI;
import java.net.URISyntaxException;

import static org.exist.util.StringUtil.notNullOrEmptyOrWs;
import static org.exist.xquery.FunctionDSL.optParam;
import static org.exist.xquery.FunctionDSL.returnsOpt;
import static org.exist.xquery.functions.fn.FnModule.functionSignature;

/**
 * @author wolf
 */
public class FunBaseURI extends BasicFunction {

    private static final ShortSet QUICK_STOPS = ShortSet.of(
        Node.ELEMENT_NODE,
        Node.ATTRIBUTE_NODE,
        Node.PROCESSING_INSTRUCTION_NODE,
        Node.COMMENT_NODE,
        Node.TEXT_NODE,
        Node.DOCUMENT_NODE
    );

    public static final String FS_BASE_URI = "base-uri";
    public static final String FS_STATIC_BASE_URI = "static-base-uri";

    static final FunctionSignature FS_BASE_URI_0 = functionSignature(
            FS_BASE_URI,
            "Returns the base URI of the context node. " +
                    "It is equivalent to calling fn:base-uri(.).",
            returnsOpt(Type.ANY_URI, "The base URI from the context node.")
    );

    static final FunctionSignature FS_STATIC_BASE_URI_0 = functionSignature(
            FS_STATIC_BASE_URI,
            "Returns the value of the static base URI property from the static context. " +
                    "If the base-uri property is undefined, the empty sequence is returned.",
            returnsOpt(Type.ANY_URI, "The base URI from the static context.")
    );

    private static final FunctionParameterSequenceType FS_PARAM_NODE
            = optParam("arg", Type.NODE, "The node.");

    static final FunctionSignature FS_BASE_URI_1 = functionSignature(
            FS_BASE_URI,
            "Returns the base URI of a node." +
                    "If $arg is the empty sequence, the empty sequence is returned.",
            returnsOpt(Type.ANY_URI, "The base URI from $arg."),
            FS_PARAM_NODE
    );

    public FunBaseURI(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    /* (non-Javadoc)
     * @see org.exist.xquery.BasicFunction#eval(org.exist.xquery.value.Sequence[],
     * org.exist.xquery.value.Sequence)
     */
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {

        if (isCalledAs(FS_STATIC_BASE_URI)) {
            if (context.isBaseURIDeclared()) {
                // use whatever value is defined, does not need to be absolute
                return context.getBaseURI();

            } else {
                // Quick escape
                return Sequence.EMPTY_SEQUENCE;
            }
        }


        NodeValue nodeValue;

        // Called as base-uri
        if (args.length == 0) {
            if (contextSequence == null) {
                throw new XPathException(this, ErrorCodes.XPDY0002, "The context item is absent");
            }
            if (contextSequence.isEmpty()) {
                return Sequence.EMPTY_SEQUENCE;
            }
            final Item item = contextSequence.itemAt(0);
            if (!Type.subTypeOf(item.getType(), Type.NODE)) {
                throw new XPathException(this, ErrorCodes.XPTY0004, "Context item is not a node");
            }
            nodeValue = (NodeValue) item;


        } else {
            if (args[0].isEmpty()) {
                return Sequence.EMPTY_SEQUENCE;
            } else {
                nodeValue = (NodeValue) args[0].itemAt(0);
            }
        }

        Sequence result = Sequence.EMPTY_SEQUENCE;

        final Node node = nodeValue.getNode();
        final short type = node.getNodeType();

        // Namespace node does not exist in xquery, hence left out of array.

        // Quick escape
        if (!QUICK_STOPS.contains(type)) {
            return Sequence.EMPTY_SEQUENCE;
        }

        // Constructed Comment nodes/PIs/Attributes do not have a baseURI according tests
        if ((node.getNodeType() == Node.COMMENT_NODE
                || node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE
                || node.getNodeType() == Node.ATTRIBUTE_NODE)
                && nodeValue.getOwnerDocument().getDocumentElement() == null) {
            return Sequence.EMPTY_SEQUENCE;
        }

        // "" when not set // check with isBaseURIDeclared()
        final AnyURIValue contextBaseURI = context.getBaseURI();
        final boolean hasContextBaseURI = context.isBaseURIDeclared();

        // "" when not set, can be null
        final String nodeBaseURI = node.getBaseURI();
        final boolean hasNodeBaseURI = notNullOrEmptyOrWs(nodeBaseURI);

        try {
            if (hasNodeBaseURI) {
                // xml:base is defined
                URI nbURI = new URI(nodeBaseURI);
                final boolean nbURIAbsolute = nbURI.isAbsolute();

                if (!nbURIAbsolute && hasContextBaseURI) {
                    // when xml:base is not an absolute URL and there is a contextURI
                    // join them
                    final URI newURI = contextBaseURI.toURI().resolve(nodeBaseURI);
                    result = new AnyURIValue(this, newURI);

                } else {
                    // just take xml:base value
                    result = new AnyURIValue(this, nbURI);
                }

            } else if (hasContextBaseURI) {
                // if there is no xml:base, take the root document, if present.
                result = contextBaseURI;
            }

        } catch (URISyntaxException e) {
            LOG.debug(e.getMessage());
        }

        return result;
    }
}
