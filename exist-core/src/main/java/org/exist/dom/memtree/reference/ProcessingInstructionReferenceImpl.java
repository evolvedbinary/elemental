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
package org.exist.dom.memtree.reference;

import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.persistent.NodeProxy;
import org.exist.xquery.Expression;
import org.w3c.dom.DOMException;
import org.w3c.dom.ProcessingInstruction;

import javax.annotation.Nullable;

/**
 * Processing Instruction wrapper around a NodeProxy for use in the in-memory DOM.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ProcessingInstructionReferenceImpl extends AbstractReferenceNodeImpl<ProcessingInstructionReferenceImpl, org.exist.dom.persistent.ProcessingInstructionImpl> implements ProcessingInstruction {

    public ProcessingInstructionReferenceImpl(@Nullable final Expression expression, final DocumentImpl doc, final int nodeNumber, final NodeProxy nodeProxy) {
        super(expression, doc, nodeNumber, nodeProxy);
    }

    @Override
    public int compareTo(final ProcessingInstructionReferenceImpl other) {
        return 0;
    }

    @Override
    public String getTarget() {
        return getProxiedNode().getTarget();
    }

    @Override
    public String getData() {
        return getProxiedNode().getData();
    }

    @Override
    public void setData(final String data) throws DOMException {
        getProxiedNode().setData(data);
    }
}
