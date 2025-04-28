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

import org.exist.collections.Collection;
import org.exist.collections.ManagedLocks;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.ManagedDocumentLock;
import org.exist.util.LockException;
import org.exist.xmldb.XmldbURI;

import java.util.Collections;
import java.util.Iterator;

/**
 * An Empty DocumentSet
 *
 * @author aretter
 */
public class EmptyDocumentSet implements DocumentSet {

    /**
     * Use {@link DocumentSet#EMPTY_DOCUMENT_SET}
     */
    EmptyDocumentSet() {
    }

    @Override
    public Iterator<DocumentImpl> getDocumentIterator() {
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<Collection> getCollectionIterator() {
        return Collections.emptyIterator();
    }

    @Override
    public int getDocumentCount() {
        return 0;
    }

    @Override
    public DocumentImpl getDoc(final int docId) {
        return null;
    }

    private final XmldbURI[] NO_NAMES = new XmldbURI[0];

    @Override
    public XmldbURI[] getNames() {
        return NO_NAMES;
    }

    @Override
    public DocumentSet intersection(final DocumentSet other) {
        return DocumentSet.EMPTY_DOCUMENT_SET;
    }

    @Override
    public boolean contains(final DocumentSet other) {
        return false;
    }

    @Override
    public boolean contains(final int id) {
        return false;
    }

    @Override
    public NodeSet docsToNodeSet() {
        return NodeSet.EMPTY_SET;
    }

    @Override
    public ManagedLocks<ManagedDocumentLock> lock(final DBBroker broker, final boolean exclusive) throws LockException {
        return new ManagedLocks<>(Collections.emptyList());
    }

    @Override
    public boolean equalDocs(final DocumentSet other) {
        return false;
    }
}
