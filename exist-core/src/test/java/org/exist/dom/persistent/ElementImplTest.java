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

import com.googlecode.junittoolbox.ParallelRunner;
import org.easymock.EasyMock;
import org.exist.numbering.DLN;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by aretter on 25/04/2017.
 */
@RunWith(ParallelRunner.class)
public class ElementImplTest {

    @Test
    public void isSameNode_sameElement() {
        final DocumentImpl doc = EasyMock.createMock(DocumentImpl.class);
        expect(doc.getDocId()).andReturn(21).times(2);

        replay(doc);

        final ElementImpl elem = new ElementImpl();
        elem.setOwnerDocument(doc);
        elem.setNodeId(new DLN("1.2"));

        assertTrue(elem.isSameNode(elem));

        verify(doc);
    }

    @Test
    public void isSameNode_differentText() {
        final DocumentImpl doc = EasyMock.createMock(DocumentImpl.class);

        replay(doc);

        final ElementImpl elem = new ElementImpl();
        elem.setOwnerDocument(doc);
        elem.setNodeId(new DLN("1.2"));

        final ElementImpl elem2 = new ElementImpl();
        elem2.setOwnerDocument(doc);
        elem2.setNodeId(new DLN("1.7"));

        assertFalse(elem.isSameNode(elem2));

        verify(doc);
    }

    @Test
    public void isSameNode_differentTextDifferentDoc() {
        final DocumentImpl doc = EasyMock.createMock(DocumentImpl.class);
        expect(doc.getDocId()).andReturn(21);

        final DocumentImpl doc2 = EasyMock.createMock(DocumentImpl.class);
        expect(doc2.getDocId()).andReturn(67);

        replay(doc, doc2);

        final ElementImpl elem = new ElementImpl();
        elem.setOwnerDocument(doc);
        elem.setNodeId(new DLN("1.2"));

        final ElementImpl elem2 = new ElementImpl();
        elem2.setOwnerDocument(doc2);
        elem2.setNodeId(new DLN("1.2"));

        assertFalse(elem.isSameNode(elem2));

        verify(doc, doc2);
    }

    @Test
    public void isSameNode_nonText() {
        final DocumentImpl doc = EasyMock.createMock(DocumentImpl.class);

        replay(doc);

        final ElementImpl elem = new ElementImpl();
        elem.setOwnerDocument(doc);
        elem.setNodeId(new DLN("1.2"));


        final TextImpl text = new TextImpl("hello");
        text.setOwnerDocument(doc);
        text.setNodeId(new DLN("1.2.1"));

        assertFalse(elem.isSameNode(text));

        verify(doc);
    }
}
