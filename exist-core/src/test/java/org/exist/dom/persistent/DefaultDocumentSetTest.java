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
import org.exist.collections.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Iterator;

import static junit.framework.TestCase.assertFalse;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@RunWith(ParallelRunner.class)
public class DefaultDocumentSetTest {

    @Test
    public void contains_leftRight() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        replay(col, doc1, doc6, doc9, doc15, doc34);

        final DefaultDocumentSet set1 = new DefaultDocumentSet();
        set1.add(doc1);
        set1.add(doc6);
        set1.add(doc9);

        final DefaultDocumentSet set2 = new DefaultDocumentSet();
        set2.add(doc1);
        set2.add(doc6);
        set2.add(doc9);
        set2.add(doc15);
        set2.add(doc34);

        // functions under test
        assertTrue(set2.contains(set1));
        assertFalse(set1.contains(set2));

        verify(col, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void contains_nonOptimized_leftRight() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        final DefaultDocumentSet set1 = new DefaultDocumentSet();

        final DocumentSet set2 = testableDocumentSet(doc1, doc6, doc9, doc15, doc34);

        replay(col, set2, doc1, doc6, doc9, doc15, doc34);

        set1.add(doc1);
        set1.add(doc6);
        set1.add(doc9);

        // functions under test
        assertFalse(set1.contains(set2));

        verify(col, set2, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void contains_nonOptimized_rightLeft() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        final DefaultDocumentSet set1 = new DefaultDocumentSet();

        final DocumentSet set2 = testableDocumentSet(doc1, doc6, doc9);

        replay(col, set2, doc1, doc6, doc9, doc15, doc34);

        set1.add(doc1);
        set1.add(doc6);
        set1.add(doc9);
        set1.add(doc15);
        set1.add(doc34);

        // functions under test
        assertTrue(set1.contains(set2));

        verify(col, set2, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void contains_noMatch() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        replay(col, doc1, doc6, doc9, doc15, doc34);

        final DefaultDocumentSet set1 = new DefaultDocumentSet();
        set1.add(doc1);
        set1.add(doc6);
        set1.add(doc9);

        final DefaultDocumentSet set2 = new DefaultDocumentSet();
        set2.add(doc15);
        set2.add(doc34);

        // functions under test
        assertFalse(set2.contains(set1));
        assertFalse(set1.contains(set2));

        verify(col, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void contains_nonOptimized_noMatch_leftRight() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        final DefaultDocumentSet set1 = new DefaultDocumentSet();
        final DocumentSet set2 = testableDocumentSet(doc15, doc34);

        replay(col, set2, doc1, doc6, doc9, doc15, doc34);

        set1.add(doc1);
        set1.add(doc6);
        set1.add(doc9);

        // functions under test
        assertFalse(set1.contains(set2));

        verify(col, set2, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void contains_nonOptimized_noMatch_rightLeft() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        final DefaultDocumentSet set1 = new DefaultDocumentSet();
        final DocumentSet set2 = testableDocumentSet(doc1, doc6, doc9);

        replay(col, set2, doc1, doc6, doc9, doc15, doc34);

        set1.add(doc15);
        set1.add(doc34);

        // functions under test
        assertFalse(set1.contains(set2));

        verify(col, set2, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void contains_emptySet() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        replay(col, doc1, doc6, doc9, doc15, doc34);

        final DocumentSet set1 = DocumentSet.EMPTY_DOCUMENT_SET;

        final DefaultDocumentSet set2 = new DefaultDocumentSet();
        set2.add(doc15);
        set2.add(doc34);

        // functions under test
        assertTrue(set2.contains(set1));
        assertFalse(set1.contains(set2));

        verify(col, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void equalDocs() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        replay(col, doc1, doc6, doc9, doc15, doc34);

        final DefaultDocumentSet set1 = new DefaultDocumentSet();
        set1.add(doc1);
        set1.add(doc6);
        set1.add(doc9);
        set1.add(doc15);
        set1.add(doc34);

        final DefaultDocumentSet set2 = new DefaultDocumentSet();
        set2.add(doc1);
        set2.add(doc6);
        set2.add(doc9);
        set2.add(doc15);
        set2.add(doc34);

        // functions under test
        assertTrue(set1.equalDocs(set2));
        assertTrue(set2.equalDocs(set1));

        verify(col, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void equalDocs_noMatch() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        replay(col, doc1, doc6, doc9, doc15, doc34);

        final DefaultDocumentSet set1 = new DefaultDocumentSet();
        set1.add(doc1);
        set1.add(doc6);
        set1.add(doc9);
        set1.add(doc15);
        set1.add(doc34);

        final DefaultDocumentSet set2 = new DefaultDocumentSet();
        set2.add(doc1);
        set2.add(doc6);
        set2.add(doc9);

        // functions under test
        assertFalse(set1.equalDocs(set2));
        assertFalse(set2.equalDocs(set1));

        verify(col, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void equalDocs_nonOptimized() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        final DefaultDocumentSet set1 = new DefaultDocumentSet();

        final DocumentSet set2 = testableDocumentSet(doc1, doc6, doc9, doc15, doc34);

        replay(col, set2, doc1, doc6, doc9, doc15, doc34);

        set1.add(doc1);
        set1.add(doc6);
        set1.add(doc9);
        set1.add(doc15);
        set1.add(doc34);

        // functions under test
        assertTrue(set1.equalDocs(set2));

        verify(col, set2, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void equalDocs_nonOptimized_noMatch() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        final DefaultDocumentSet set1 = new DefaultDocumentSet();

        final DocumentSet set2 = testableDocumentSet(doc1, doc6, doc9, doc15, doc34);

        replay(col, set2, doc1, doc6, doc9, doc15, doc34);

        set1.add(doc1);
        set1.add(doc6);
        set1.add(doc9);

        // functions under test
        assertFalse(set1.equalDocs(set2));

        verify(col, set2, doc1, doc6, doc9, doc15, doc34);
    }

    @Test
    public void equalDocs_emptySet() {
        final Collection col = mockCollection(1);

        final DocumentImpl doc1 = mockDoc(col, 1);
        final DocumentImpl doc6 = mockDoc(col, 6);
        final DocumentImpl doc9 = mockDoc(col, 9);
        final DocumentImpl doc15 = mockDoc(col, 15);
        final DocumentImpl doc34 = mockDoc(col, 34);

        replay(col, doc1, doc6, doc9, doc15, doc34);

        final DocumentSet set1 = DocumentSet.EMPTY_DOCUMENT_SET;

        final DefaultDocumentSet set2 = new DefaultDocumentSet();
        set2.add(doc15);
        set2.add(doc34);

        // functions under test
        assertFalse(set2.equalDocs(set1));
        assertFalse(set1.equalDocs(set2));

        verify(col, doc1, doc6, doc9, doc15, doc34);
    }

    private final Collection mockCollection(final int colId) {
        final Collection col = createMock(Collection.class);
        expect(col.compareTo(col)).andReturn(0).anyTimes();
        expect(col.getId()).andReturn(colId).anyTimes();
        return col;
    }

    private final DocumentImpl mockDoc(final Collection collection, final int docId) {
        final DocumentImpl doc = createMock(DocumentImpl.class);
        expect(doc.getCollection()).andReturn(collection).anyTimes();
        expect(doc.getDocId()).andReturn(docId).anyTimes();
        return doc;
    }

    private final DocumentSet testableDocumentSet(final DocumentImpl... docs) {
        final DocumentSet documentSet = createMock(DocumentSet.class);

        expect(documentSet.getDocumentCount()).andReturn(docs.length);

        expect(documentSet.getDocumentIterator()).andReturn(new Iterator<DocumentImpl>(){
            private int idx = 0;

            @Override
            public boolean hasNext() {
                return idx < docs.length;
            }

            @Override
            public DocumentImpl next() {
                return docs[idx++];
            }
        }).anyTimes();

        return documentSet;
    }
}
