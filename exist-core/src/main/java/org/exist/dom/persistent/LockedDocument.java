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

import org.exist.storage.lock.ManagedDocumentLock;

/**
 * Just a wrapper around a  {@link DocumentImpl} which allows us to also hold a lock
 * lease which is released when {@link #close()} is called. This
 * allows us to use ARM (Automatic Resource Management) e.g. try-with-resources
 * with eXist Document objects
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class LockedDocument implements AutoCloseable {
    private final ManagedDocumentLock managedDocumentLock;
    private final DocumentImpl document;

    public LockedDocument(final ManagedDocumentLock managedDocumentLock, final DocumentImpl document) {
        this.managedDocumentLock = managedDocumentLock;
        this.document = document;
    }

    /**
     * Get the document
     *
     * @return the locked document
     */
    public DocumentImpl getDocument() {
        return document;
    }

    /**
     * Unlocks the Document
     */
    @Override
    public void close() {
        managedDocumentLock.close();
    }
}
