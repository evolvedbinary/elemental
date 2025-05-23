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
package org.exist.launcher;

import org.exist.jetty.JettyStart;
import org.exist.storage.BrokerPool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;

import org.exist.SystemProperties;

/**
 * Display a splash screen showing the logo and a status line.
 *
 * @author Wolfgang Meier
 */
public class SplashScreen extends JFrame implements Observer, Comparable {

    private static final long serialVersionUID = -8449133653386075548L;

    private JLabel statusLabel;
    private JLabel versionLabel;
    private Launcher launcher;

    public SplashScreen(Launcher launcher) {
        this.launcher = launcher;
        setUndecorated(true);
        setBackground(new Color(255, 255, 255, 255));

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        getContentPane().setBackground(new Color(255, 255, 255, 255));

        final URL imageURL = SplashScreen.class.getResource("logo.png");
        final ImageIcon icon = new ImageIcon(imageURL, "Elemental Logo");
        getContentPane().setLayout(new BorderLayout());

        // add the image label
        final JLabel imageLabel = new JLabel();
        imageLabel.setIcon(icon);
        final EmptyBorder border = new EmptyBorder(20, 20, 10, 20);
        imageLabel.setBorder(border);
        getContentPane().add(imageLabel, BorderLayout.NORTH);

        // version label
        final SystemProperties sysProps = SystemProperties.getInstance();
        final StringBuilder builder = new StringBuilder();
	    builder.append("Version ");
        builder.append(sysProps.getSystemProperty("product-version", "unknown"));
        final String gitCommit = sysProps.getSystemProperty("git-commit");
        if (gitCommit != null && !gitCommit.isEmpty()) {
            builder.append(" (");
            builder.append(gitCommit, 0, Math.min(7, gitCommit.length()));
            builder.append(")");
        }
        versionLabel = new JLabel(builder.toString(), SwingConstants.CENTER);
        versionLabel.setFont(new Font(versionLabel.getFont().getName(), Font.BOLD, 10));
        versionLabel.setForeground(Color.black);
        versionLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        versionLabel.setSize(new Dimension(icon.getIconWidth(), 60));

        getContentPane().add(versionLabel, BorderLayout.CENTER);

        // message label
        statusLabel = new JLabel("Launching ...", SwingConstants.CENTER);
        statusLabel.setFont(new Font(statusLabel.getFont().getName(), Font.PLAIN, 16));
        statusLabel.setForeground(Color.black);
        statusLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        statusLabel.setSize(new Dimension(icon.getIconWidth(), 60));

        getContentPane().add(statusLabel, BorderLayout.SOUTH);
        // show it
        setSize(new Dimension(icon.getIconWidth() + 40, icon.getIconHeight() + 50));
        pack();
        this.setLocationRelativeTo(null);
        setVisible(true);

        // bring to front
        SwingUtilities.invokeLater(() -> {
            toFront();
            repaint();
        });
    }

    public void setStatus(final String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    public void update(Observable o, Object arg) {
        if (JettyStart.SIGNAL_STARTED.equals(arg)) {
            setStatus("Server started!");
            setVisible(false);
            launcher.signalStarted();
        } else if (BrokerPool.SIGNAL_STARTUP.equals(arg)) {
            setStatus("Starting Elemental ...");
        } else if (BrokerPool.SIGNAL_ABORTED.equals(arg)) {
            setVisible(false);
            launcher.showMessageAndExit("Startup aborted",
                "Elemental detected an error during recovery. This may not be fatal, " +
                "but to avoid possible damage, the db will now stop. Please consider " +
                "running a consistency check via the export tool and create " +
                "a backup if problems are reported. The db should come up again if you restart " +
                "it.", true);
        } else if (BrokerPool.SIGNAL_WRITABLE.equals(arg)) {
            setStatus("Elemental is up. Waiting for web server ...");
        } else if (JettyStart.SIGNAL_ERROR.equals(arg)) {
            setVisible(false);
            launcher.showMessageAndExit("Error Occurred",
                    "An error occurred during startup. Please check the logs.", true);
        } else if (BrokerPool.SIGNAL_SHUTDOWN.equals(arg)) {
            launcher.signalShutdown();
        } else {
            setStatus(arg.toString());
        }
    }

    @Override
    public int compareTo(Object other) {
        return other == this ? 0 : -1;
    }
}
