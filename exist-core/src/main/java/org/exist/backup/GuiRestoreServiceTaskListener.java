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
package org.exist.backup;

import org.exist.xmldb.AbstractRestoreServiceTaskListener;

import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class GuiRestoreServiceTaskListener extends AbstractRestoreServiceTaskListener {
    private final RestoreDialog dialog;
    private StringBuilder allProblems = null;

    public GuiRestoreServiceTaskListener() {
        this(null);
    }

    public GuiRestoreServiceTaskListener(@Nullable final JFrame parent) {
        this.dialog = new RestoreDialog(parent, "Restoring data ...", false);
        this.dialog.setVisible(true);
    }

    @Override
    public void startedZipForTransfer(final long totalUncompressedSize) {
        dialog.setTotalRestoreUncompressedSize(totalUncompressedSize);
        super.startedZipForTransfer(totalUncompressedSize);
    }

    @Override
    public void startedTransfer(final long transferSize) {
        dialog.setTotalTransferSize(transferSize);
        super.startedTransfer(transferSize);
    }

    @Override
    public void started(final long numberOfFiles) {
        dialog.setTotalNumberOfFiles(numberOfFiles);
        super.started(numberOfFiles);
    }

    @Override
    public void info(final String message) {
        SwingUtilities.invokeLater(() -> dialog.displayMessage(message));
    }

    @Override
    public void warn(final String message) {
        SwingUtilities.invokeLater(() -> dialog.displayMessage(message));
        addProblem(true, message);
    }

    @Override
    public void error(final String message) {
        SwingUtilities.invokeLater(() -> dialog.displayMessage(message));
        addProblem(false, message);
    }

    @Override
    public void createdCollection(final String collection) {
        SwingUtilities.invokeLater(() -> dialog.setCollection(collection));
        super.createdCollection(collection);
    }

    @Override
    public void addedFileToZipForTransfer(final long uncompressedSize) {
        SwingUtilities.invokeLater(() -> dialog.addedFileToZip(uncompressedSize));
        super.addedFileToZipForTransfer(uncompressedSize);
    }

    @Override
    public void transferred(final long chunkSize) {
        SwingUtilities.invokeLater(() -> dialog.transferred(chunkSize));
        super.transferred(chunkSize);
    }

    @Override
    public void restoredResource(final String resource) {
        SwingUtilities.invokeLater(() -> dialog.setResource(resource));
        SwingUtilities.invokeLater(dialog::incrementFileCounter);
        super.restoredResource(resource);
    }

    @Override
    public void skipResources(final String message, final long count) {
        SwingUtilities.invokeLater(() -> dialog.incrementFileCounter(count));
        super.skipResources(message, count);
    }

    @Override
    public void processingDescriptor(final String backupDescriptor) {
        SwingUtilities.invokeLater(() -> dialog.setBackup(backupDescriptor));
        super.processingDescriptor(backupDescriptor);
    }

    public void enableDismissDialogButton() {
        dialog.dismissButton.setEnabled(true);
        dialog.dismissButton.grabFocus();
    }

    public void hideDialog() {
        dialog.setVisible(false);
    }

    public boolean hasProblems() {
        return allProblems != null && allProblems.length() > 0;
    }

    public String getAllProblems() {
        return allProblems.toString();
    }

    private void addProblem(final boolean warning, final String message) {
        final String sep = System.getProperty("line.separator");
        if (allProblems == null) {
            allProblems = new StringBuilder();
            allProblems.append("------------------------------------").append(sep);
            allProblems.append("Problems occurred found during restore:").append(sep);
        }

        if (warning) {
            allProblems.append("WARN: ");
        } else {
            allProblems.append("ERROR: ");
        }
        allProblems.append(message);
        allProblems.append(sep);
    }
}
