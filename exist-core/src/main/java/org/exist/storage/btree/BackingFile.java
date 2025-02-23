/*
 * Copyright (C) 2014 Evolved Binary Ltd
 */
package org.exist.storage.btree;

import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Path;

/**
 * Simple immutable data class for a file that backs an {@link AbstractPagedFile}.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class BackingFile {
    public final Path path;
    public final boolean createdNewFile;
    public final RandomAccessFile randomAccessFile;
    public final FileLock fileLock;

    BackingFile(final Path path, final boolean createdNewFile, final RandomAccessFile randomAccessFile, final FileLock fileLock) {
        this.path = path;
        this.createdNewFile = createdNewFile;
        this.randomAccessFile = randomAccessFile;
        this.fileLock = fileLock;
    }
}
