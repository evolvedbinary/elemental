/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.storage.btree;

/**
 * The type of a Page.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public enum PageType {
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

    PageType(final byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static PageType fromValue(final byte value) throws IllegalArgumentException {
        for (final PageType pageType : PageType.values()) {
            if (pageType.value == value) {
                return pageType;
            }
        }
        throw new IllegalArgumentException("Unknown value for PageType: " + value);
    }

    /**
     * Determine if the page type is a type of record.
     *
     * @return true if the page type is a type of record, false otherwise.
     */
    public static boolean isRecordType(final PageType pageType) {
        return pageType.value >= RECORD.value;
    }
}
