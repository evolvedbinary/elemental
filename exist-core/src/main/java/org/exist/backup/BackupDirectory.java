/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.backup;

import com.evolvedbinary.j8fu.tuple.Tuple2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.util.FileUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.evolvedbinary.j8fu.tuple.Tuple.Tuple;
import static org.exist.backup.BackupDescriptor.PREVIOUS_PROP_NAME;


public class BackupDirectory {
    public final static Logger LOG = LogManager.getLogger(BackupDirectory.class);


    public final static String PREFIX_FULL_BACKUP_FILE = "full";
    public final static String PREFIX_INC_BACKUP_FILE = "inc";

    public final static String FULL_FILE_REGEX = PREFIX_FULL_BACKUP_FILE + "(\\d{8}-\\d{6}).*";
    public final static String INC_FILE_REGEX = PREFIX_INC_BACKUP_FILE + "\\d{8}-\\d{6}.*";
    public final static String FILE_REGEX = "(?:" + PREFIX_FULL_BACKUP_FILE + "|" + PREFIX_INC_BACKUP_FILE + ")(\\d{8}-\\d{6}).*";

    public final static String DATE_FORMAT_PICTURE = "yyyyMMdd-HHmmss";
    private final static DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PICTURE);


    private final Path dir;
    private @Nullable Matcher fileMatcher;
    private @Nullable Matcher fullFileMatcher;
    private @Nullable Matcher incFileMatcher;


    public BackupDirectory(final String dirPath) {
        this(Paths.get(dirPath));
    }

    public BackupDirectory(final Path directory) {
        this.dir = directory;
    }

    public Path createBackup(final boolean incremental, final boolean zip) {
        int counter = 0;
        while (true) {
            final StringBuilder buf = new StringBuilder();
            buf.append(incremental ? PREFIX_INC_BACKUP_FILE : PREFIX_FULL_BACKUP_FILE);
            buf.append(DATE_FORMAT.format(new Date()));

            if (counter++ > 0) {
                buf.append('_').append(counter);
            }

            // make sure a file/dir of the same basic name (i.e. without extension) does not exist
            Path file = dir.resolve(buf.toString());
            if (!Files.exists(file)) {

                // is this a zip backup file?
                if (!zip) {
                    // no
                    return file;

                } else {
                    // yes, so check that a file/dir of the same name as the desired zip file does not exist
                    buf.append(".zip");
                    file = dir.resolve(buf.toString());
                    if (!Files.exists(file)) {
                        return file;
                    }
                }
            }
        }
    }

    /**
     * Gets the details of the last backup (if any).
     *
     * @return the details of the last backup, or null if there was no previous backup.
     */
    public @Nullable BackupDescriptor lastBackupFile() throws IOException {
        final List<Path> files = FileUtils.list(dir);

        @Nullable Path newest = null;
        @Nullable Date newestDate = null;

        for (final Path file : files) {
            @Nullable final Date date = isBackupFile(file);
            if (date != null) {
                if (newestDate == null || date.after(newestDate)) {
                    newestDate = date;
                    newest = file;
                }
            }
        }

        if (newest != null) {
            return getBackupDescriptor(newest);
        }

        return null;
    }

    /**
     * If the provided file is a full or incremental backup
     * then the Date of the backup is returned.
     *
     * @param file the file to test if it is a backup.
     *
     * @return the date of the backup, or null if the file is not a backup.
     */
    private @Nullable Date isBackupFile(final Path file) {
        final String fileName = FileUtils.fileName(file);
        if (fileMatcher == null) {
            final Pattern filePattern = Pattern.compile(FILE_REGEX);
            this.fileMatcher = filePattern.matcher(fileName);
        } else {
            this.fileMatcher.reset(fileName);
        }

        if (this.fileMatcher.matches()) {
            final String dateTime = fileMatcher.group(1);
            try {
                return DATE_FORMAT.parse(dateTime);
            } catch (final ParseException e) {
                // no-op
            }
        }

        return null;
    }

    /**
     * Returns the number of full backups present in the directory.
     *
     * @return the number of full backups present in the directory.
     */
    public int countFullBackups() throws IOException {
        final List<Path> files = FileUtils.list(dir);

        int count = 0;

        for (final Path file : files) {
            @Nullable final Date date = isFullBackupFile(file);
            if (date != null) {
                count++;
            }
        }

        return count;
    }

    /**
     * If the provided file is a full backup
     * then the Date of the backup is returned.
     *
     * @param file the file to test if it is a full backup.
     *
     * @return the date of the full backup, or null if the file is not a full backup.
     */
    private @Nullable Date isFullBackupFile(final Path file) {
        final String fileName = FileUtils.fileName(file);
        if (fullFileMatcher == null) {
            final Pattern fullFilePattern = Pattern.compile(FULL_FILE_REGEX);
            this.fullFileMatcher = fullFilePattern.matcher(fileName);
        } else {
            this.fullFileMatcher.reset(fileName);
        }

        if (this.fullFileMatcher.matches()) {
            final String dateTime = fullFileMatcher.group(1);
            try {
                return DATE_FORMAT.parse(dateTime);
            } catch (final ParseException e) {
                // no-op
            }
        }

        return null;
    }

    /**
     * Gets the oldest full-backup and any associated incremental backups (if any).
     *
     * @return the path of the oldest backup, and any associated incremental backups, or null if there was no previous full backup.
     */
    public @Nullable List<Path> getOldestFullBackup() throws IOException {
        final List<Path> files = FileUtils.list(dir);

        @Nullable Path oldestFullBackup = null;
        @Nullable Date oldestFullBackupDate = null;

        // find the oldest full backup, and create a map of incremental backups
        @Nullable Map<String, Tuple2<String, Path>> previousToIncremental = null;
        for (final Path file : files) {
            @Nullable final Date date = isFullBackupFile(file);
            if (date != null) {
                if (oldestFullBackupDate == null || date.before(oldestFullBackupDate)) {
                    oldestFullBackupDate = date;
                    oldestFullBackup = file;
                }
            } else {
                @Nullable BackupDescriptor incBackupDescriptor = isIncBackupFile(file);
                if (incBackupDescriptor != null) {
                    @Nullable String previousBackup = incBackupDescriptor.getProperties().getProperty(PREVIOUS_PROP_NAME);
                    if (previousBackup != null) {
                        if (previousToIncremental == null) {
                            previousToIncremental = new HashMap<>();
                        }
                        previousToIncremental.put(previousBackup, Tuple(incBackupDescriptor.getName(), file));
                    }
                }
            }
        }

        @Nullable List<Path> oldestBackups = null;
        if (oldestFullBackup != null) {
            oldestBackups = new ArrayList<>();
            oldestBackups.add(oldestFullBackup);

            // Find any associated incremental backups
            if (previousToIncremental != null) {
                final BackupDescriptor oldestFullBackupDescriptor = getBackupDescriptor(oldestFullBackup);
                @Nullable Tuple2<String, Path> incrementalBackupInfo = previousToIncremental.get(oldestFullBackupDescriptor.getName());
                while (incrementalBackupInfo != null) {
                    oldestBackups.add(incrementalBackupInfo._2);
                    incrementalBackupInfo = previousToIncremental.get(incrementalBackupInfo._1);
                }
            }
        }

        return oldestBackups;
    }

    /**
     * If the provided file is an incremental backup
     * then the Date of the backup is returned.
     *
     * @param file the file to test if it is an incremental backup.
     *
     * @return the date of the incremental backup, or null if the file is not an incremental backup.
     */
    private @Nullable BackupDescriptor isIncBackupFile(final Path file) {
        final String fileName = FileUtils.fileName(file);
        if (incFileMatcher == null) {
            final Pattern incFilePattern = Pattern.compile(INC_FILE_REGEX);
            this.incFileMatcher = incFilePattern.matcher(fileName);
        } else {
            this.incFileMatcher.reset(fileName);
        }

        if (this.incFileMatcher.matches()) {
            return getBackupDescriptor(file);
        }

        return null;
    }

    private @Nullable BackupDescriptor getBackupDescriptor(final Path path) {
        try {
            if (FileUtils.fileName(path).toLowerCase().endsWith(".zip")) {
                return new ZipArchiveBackupDescriptor(path);
            } else {
                return new FileSystemBackupDescriptor(path, path.resolve("db").resolve(BackupDescriptor.COLLECTION_DESCRIPTOR));
            }
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return null;
    }

}
