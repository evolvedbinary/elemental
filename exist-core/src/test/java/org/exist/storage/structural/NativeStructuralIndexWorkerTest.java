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
package org.exist.storage.structural;

import com.googlecode.junittoolbox.ParallelRunner;
import org.easymock.EasyMock;
import org.exist.dom.persistent.DocumentImpl;
import org.exist.dom.persistent.DocumentSet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
@RunWith(ParallelRunner.class)
public class NativeStructuralIndexWorkerTest {

    @Test
    public void getDocIdRanges_singleContiguous() {
        final NativeStructuralIndexWorker indexWorker = new NativeStructuralIndexWorker(null);

        final DocumentSet docs = documentIdSet(Arrays.asList(1,2,3,4,5,6));

        final List<NativeStructuralIndexWorker.Range> ranges = indexWorker.getDocIdRanges(docs);

        assertEquals(1, ranges.size());

        assertEquals(1, ranges.get(0).start);
        assertEquals(6, ranges.get(0).end);
    }

    @Test
    public void getDocIdRanges_multipleContiguous() {
        final NativeStructuralIndexWorker indexWorker = new NativeStructuralIndexWorker(null);

        final DocumentSet docs = documentIdSet(Arrays.asList(1,2,3,4,5,6, 88,89, 3,4,5,6, 77, 10,11,12));

        final List<NativeStructuralIndexWorker.Range> ranges = indexWorker.getDocIdRanges(docs);

        assertEquals(5, ranges.size());

        assertEquals(1, ranges.get(0).start);
        assertEquals(6, ranges.get(0).end);

        assertEquals(88, ranges.get(1).start);
        assertEquals(89, ranges.get(1).end);

        assertEquals(3, ranges.get(2).start);
        assertEquals(6, ranges.get(2).end);

        assertEquals(77, ranges.get(3).start);
        assertEquals(77, ranges.get(3).end);

        assertEquals(10, ranges.get(4).start);
        assertEquals(12, ranges.get(4).end);
    }

    @Test
    public void getDocIdRanges_singleId() {
        final NativeStructuralIndexWorker indexWorker = new NativeStructuralIndexWorker(null);

        final DocumentSet docs = documentIdSet(Arrays.asList(6574));

        final List<NativeStructuralIndexWorker.Range> ranges = indexWorker.getDocIdRanges(docs);

        assertEquals(1, ranges.size());

        assertEquals(6574, ranges.get(0).start);
        assertEquals(6574, ranges.get(0).end);
    }

    @Test
    public void getDocIdRanges_singleId_followed_by_continguousIds() {
        final NativeStructuralIndexWorker indexWorker = new NativeStructuralIndexWorker(null);

        final DocumentSet docs = documentIdSet(Arrays.asList(6574, 11,12,13,14,15));

        final List<NativeStructuralIndexWorker.Range> ranges = indexWorker.getDocIdRanges(docs);

        assertEquals(2, ranges.size());

        assertEquals(6574, ranges.get(0).start);
        assertEquals(6574, ranges.get(0).end);

        assertEquals(11, ranges.get(1).start);
        assertEquals(15, ranges.get(1).end);
    }

    @Test
    public void getDocIdRanges_contiguousIds_followed_by_single() {
        final NativeStructuralIndexWorker indexWorker = new NativeStructuralIndexWorker(null);

        final DocumentSet docs = documentIdSet(Arrays.asList(11,12,13,14,15, 6574));

        final List<NativeStructuralIndexWorker.Range> ranges = indexWorker.getDocIdRanges(docs);

        assertEquals(2, ranges.size());

        assertEquals(11, ranges.get(0).start);
        assertEquals(15, ranges.get(0).end);

        assertEquals(6574, ranges.get(1).start);
        assertEquals(6574, ranges.get(1).end);
    }

    @Test
    public void getDocIdRanges_multiple_singleIds() {
        final NativeStructuralIndexWorker indexWorker = new NativeStructuralIndexWorker(null);

        final DocumentSet docs = documentIdSet(Arrays.asList(6574, 200, 12, 24));

        final List<NativeStructuralIndexWorker.Range> ranges = indexWorker.getDocIdRanges(docs);

        assertEquals(4, ranges.size());

        assertEquals(6574, ranges.get(0).start);
        assertEquals(6574, ranges.get(0).end);

        assertEquals(200, ranges.get(1).start);
        assertEquals(200, ranges.get(1).end);

        assertEquals(12, ranges.get(2).start);
        assertEquals(12, ranges.get(2).end);

        assertEquals(24, ranges.get(3).start);
        assertEquals(24, ranges.get(3).end);
    }

    private DocumentSet documentIdSet(final List<Integer> documentIds) {
        final DocumentSet mockDocumentSet = createMock(DocumentSet.class);

        final List<DocumentImpl> docs = documentIds.stream().map(id -> {
            final DocumentImpl mockDocument = createMock(DocumentImpl.class);
            expect(mockDocument.getDocId()).andReturn(id).anyTimes();
            return mockDocument;
        }).collect(Collectors.toList());

        expect(mockDocumentSet.getDocumentIterator()).andReturn(docs.iterator());

        replay(mockDocumentSet);
        docs.forEach(EasyMock::replay);

        return mockDocumentSet;
    }
}
