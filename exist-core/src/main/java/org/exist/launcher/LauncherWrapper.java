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

import org.exist.start.CompatibleJavaVersionCheck;
import org.exist.start.StartException;
import org.exist.util.ConfigurationHelper;
import org.exist.util.OSUtil;
import org.exist.util.SystemExitCodes;
import se.softhouse.jargo.Argument;
import se.softhouse.jargo.ArgumentException;
import se.softhouse.jargo.CommandLineParser;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.exist.launcher.ConfigurationUtility.*;
import static se.softhouse.jargo.Arguments.helpArgument;

/**
 * A wrapper to start a Java process using start.jar with correct VM settings.
 * Spawns a new Java VM using Ant. Mainly used when launching
 * Elemental by double-clicking on start.jar.
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 * @author Tobi Krebs
 * @author Wolfgang Meier
 */
public class LauncherWrapper {

    private static final String LAUNCHER = org.exist.launcher.Launcher.class.getName();

    /* general arguments */
    private static final Argument<?> helpArg = helpArgument("-h", "--help");

    public final static void main(final String[] args) {
        try {
            CompatibleJavaVersionCheck.checkForCompatibleJavaVersion();

            // parse command-line options
            CommandLineParser
                    .withArguments(helpArg)
                    .programName("launcher" + (OSUtil.IS_WINDOWS ? ".bat" : ".sh"))
                    .parse(args);

        } catch (final StartException e) {
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                System.err.println(e.getMessage());
            }
            System.exit(e.getErrorCode());
        } catch (final ArgumentException e) {
            consoleOut(e.getMessageAndUsage().toString());
            System.exit(SystemExitCodes.INVALID_ARGUMENT_EXIT_CODE);
        }

        final LauncherWrapper wrapper = new LauncherWrapper(LAUNCHER);
        if (ConfigurationUtility.isFirstStart()) {
            System.out.println("First launch: opening configuration dialog");
            ConfigurationDialog configDialog = new ConfigurationDialog(restart -> {
                wrapper.launch();
                // make sure the process dies when the dialog is closed
                System.exit(0);
            });
            configDialog.open(true);
            configDialog.requestFocus();
        } else {
            wrapper.launch();
        }
    }

    protected String command;

    public LauncherWrapper(String command) {
        this.command = command;
    }

    public void launch() {
        final String debugLauncher = System.getProperty("exist.debug.launcher", "false");
        final Properties launcherProperties = ConfigurationUtility.loadProperties();

        final List<String> args = new ArrayList<>();
        args.add(getJavaCmd());
        getJavaOpts(args, launcherProperties);

        if (Boolean.parseBoolean(debugLauncher) && !"client".equals(command)) {
            args.add("-Xdebug");
            args.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006");

            System.out.println("Debug mode for Launcher on JDWP port 5006. Will await connection...");
        }

        // recreate the classpath
        args.add("-cp");
        args.add(getClassPath());

        // call exist main with our new command
        args.add("org.exist.start.Main");
        args.add(command);

        try {
            run(args);
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error Running Process", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getClassPath() {
        // if we are booted using appassembler-booter, then we should use `app.class.path`
        return System.getProperty("app.class.path", System.getProperty("java.class.path"));
    }

    private void run(final List<String> args) throws IOException {
        final StringBuilder buf = new StringBuilder("Executing: [");
        for (int i = 0; i < args.size(); i++) {
            buf.append('"');
            buf.append(args.get(i));
            buf.append('"');
            if (i != args.size() - 1) {
                buf.append(", ");
            }
        }
        buf.append(']');
        System.out.println(buf.toString());

        final ProcessBuilder pb = new ProcessBuilder(args);
        final Optional<Path> home = ConfigurationHelper.getExistHome();
        pb.directory(home.orElse(Paths.get(".")).toFile());
        pb.redirectErrorStream(true);
        pb.inheritIO();

        pb.start();
    }


    protected String getJavaCmd() {
        final File javaHome = new File(System.getProperty("java.home"));
        if (OSUtil.IS_WINDOWS) {
            Path javaBin = Paths.get(javaHome.getAbsolutePath(), "bin", "javaw.exe");
            if (Files.isExecutable(javaBin)) {
                return '"' + javaBin.toString() + '"';
            }
            javaBin = Paths.get(javaHome.getAbsolutePath(), "bin", "java.exe");
            if (Files.isExecutable(javaBin)) {
                return '"' + javaBin.toString() + '"';
            }
        } else {
            Path javaBin = Paths.get(javaHome.getAbsolutePath(), "bin", "java");
            if (Files.isExecutable(javaBin)) {
                return javaBin.toString();
            }
        }
        return "java";
    }

    protected void getJavaOpts(final List<String> args, final Properties launcherProperties) {
        args.add("-XX:+UseNUMA");
        args.add("-XX:+UseZGC");

        args.add("-XX:+ExitOnOutOfMemoryError");

        getLauncherOpts(args, launcherProperties);

        boolean foundExistHomeSysProp = false;
        final Properties sysProps = System.getProperties();
        for (final Map.Entry<Object, Object> entry : sysProps.entrySet()) {
            final String key = entry.getKey().toString();
            if (key.startsWith("exist.") || key.startsWith("log4j.") || key.startsWith("jetty.") || key.startsWith("app.")) {
                args.add("-D" + key + "=" + entry.getValue().toString());
                if (key.equals("exist.home")) {
                    foundExistHomeSysProp = true;
                }
            }
        }

        if (!foundExistHomeSysProp) {
            args.add("-Dexist.home=\".\"");
        }

        if (command.equals(LAUNCHER) && OSUtil.IS_MAC_OSX) {
            args.add("-Dapple.awt.UIElement=true");
        }
    }

    protected void getLauncherOpts(final List<String> args, final Properties launcherProperties) {
        for (final String key : launcherProperties.stringPropertyNames()) {
            if (key.startsWith("memory.")) {
                if (LAUNCHER_PROPERTY_MAX_MEM.equals(key)) {
                    args.add("-Xmx" + launcherProperties.getProperty(key) + 'm');
                } else if (LAUNCHER_PROPERTY_MIN_MEM.equals(key)) {
                    args.add("-Xms" + launcherProperties.getProperty(key) + 'm');
                }
            } else if (LAUNCHER_PROPERTY_VMOPTIONS.equals(key)) {
                args.add(launcherProperties.getProperty(key));
            } else if (key.startsWith(LAUNCHER_PROPERTY_VMOPTIONS + '.')) {
                final String os = key.substring((LAUNCHER_PROPERTY_VMOPTIONS + '.').length()).toLowerCase();
                if (OSUtil.OS_NAME.toLowerCase().contains(os)) {
                    final String value = launcherProperties.getProperty(key);
                    Arrays.stream(value.split("\\s+")).forEach(args::add);
                }
            }
        }
    }

    private static void consoleOut(final String msg) {
        System.out.println(msg); //NOSONAR this has to go to the console
    }
}
