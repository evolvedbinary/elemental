/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.storage.btree;

/**
 * The status of a Page.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public enum PageStatus {
    UNUSED((byte)         0),
    LEAF((byte)           1),
    BRANCH((byte)         2),
    RECORD((byte)        20),
    LOB((byte)           21),
    FREE_LIST((byte)     22),
    MULTI_PAGE((byte)    23),
    OVERFLOW((byte)     126),
    DELETED((byte)      127);

    private final byte value;

    PageStatus(final byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static PageStatus fromValue(final byte value) throws IllegalArgumentException {
        for (final PageStatus pageStatus : PageStatus.values()) {
            if (pageStatus.value == value) {
                return pageStatus;
            }
        }
        throw new IllegalArgumentException("Unknown value for PageStatus: " + value);
    }

    /**
     * Determine if the page status is a type of record.
     *
     * @return true if the page status is a type of record, false otherwise.
     */
    public static boolean isRecordType(final PageStatus pageStatus) {
        return pageStatus.value >= RECORD.value;
    }
}
