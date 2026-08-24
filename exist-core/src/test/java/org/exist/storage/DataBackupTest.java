/*
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

package org.exist.storage;

import org.exist.EXistException;
import org.exist.storage.txn.Txn;
import org.exist.test.EmbeddedDatabaseExtension;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataBackupTest {

    @RegisterExtension
    public static EmbeddedDatabaseExtension EMBEDDED_DATABASE = new EmbeddedDatabaseExtension(true, true);

    @TempDir
    public static File folder;

    @Test
    void backup() throws InterruptedException, IOException {
        final TestableDataBackup dataBackup = new TestableDataBackup(folder.toPath());
        EMBEDDED_DATABASE.getBrokerPool().triggerSystemTask(dataBackup);

        while(!dataBackup.isCompleted()) {
            Thread.sleep(100);
        }

        final Optional<Path> lastBackup = dataBackup.getLastBackup();
        assertTrue(lastBackup.isPresent());

        final ZipFile zipFile = new ZipFile(lastBackup.get().toFile());
        assertNotNull(zipFile.getEntry("collections.dbx"));
        assertNotNull(zipFile.getEntry("dom.dbx"));
        assertNotNull(zipFile.getEntry("structure.dbx"));
        assertNotNull(zipFile.getEntry("symbols.dbx"));
        assertNotNull(zipFile.getEntry("values.dbx"));
        assertNotNull(zipFile.getEntry("blob.dbx"));
    }

    private class TestableDataBackup extends DataBackup {
        private volatile boolean completed = false;

        public TestableDataBackup(final Path destination) {
            super(destination);
        }

        @Override
        public void execute(final DBBroker broker, final Txn transaction) throws EXistException {
            super.execute(broker, transaction);
            completed = true;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
}
