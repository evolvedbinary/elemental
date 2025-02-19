/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.storage.btree;

import org.exist.storage.journal.Lsn;

/**
 * Interface for a Paged Header.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public interface PageHeader {

    /**
     * Get the length of the data.
     *
     * @return The length of the data.
     */
    int getDataLen();

    /**
     * Update the length of the data.
     *
     * @param newDataLen The new length of the data.
     */
    void updateDataLen(final int newDataLen);

    /**
     * Get the next page for this record (if the page overflowed).
     *
     * @return The next page.
     */
    long getNextPage();

    /**
     * Update the next page for this record (when the page overflows)
     *
     * @param newNextPage The new next page.
     */
    void updateNextPage(final long newNextPage);

    /**
     * Returns true if the current page is dirty.
     *
     * @return true if the current page is dirty, false otherwise.
     */
    boolean isDirty();

    /**
     * Set the dirty flag for the current page.
     *
     * @param dirty true if the current page is dirty, false otherwise.
     */
    void setDirty(final boolean dirty);

    /**
     * Get the status of the current page.
     *
     * @return the status of the current page.
     */
    PageStatus getStatus();

    /**
     * Update the status of the current page.
     *
     * @param newStatus The new status for the current page.
     */
    void updateStatus(final PageStatus newStatus);

    /**
     * Returns the LSN, i.e. the Log Sequence Number, of the last
     * operation that modified this page. This information is used
     * during recovery: if the log sequence number of a log record
     * is smaller or equal to the LSN stored in this page header, then
     * the page has already been written to disk before the database
     * failure. Otherwise, the modification is not yet reflected in the page
     * and the operation needs to be redone.
     *
     * @return log sequence number of the last operation that modified this page.
     */
    Lsn getLsn();

    /**
     * Set the Log Sequence Number of the last operation that modified this page.
     *
     * @param lsn the Log Sequence Number of the last operation that modified this page.
     */
    void setLsn(final Lsn lsn);
}
