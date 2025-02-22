/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.storage.btree;

/**
 * Interface for a Paged File.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface PagedFile<HEADER extends PagedFileHeader, PAGE_HEADER extends PageHeader> {

    /**
     * Create a new header for a page.
     *
     * @return A new page header
     */
    PAGE_HEADER createPageHeader();
}
