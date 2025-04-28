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
package org.exist.service;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.exist.start.CompatibleJavaVersionCheck;
import org.exist.start.Main;
import org.exist.start.StartException;

public class ExistDbDaemon implements Daemon {

    private static final String MODE_JETTY = "jetty";

    private Main main = null;
    private String[] args = null;

    private void init(final String args[]) {
        this.main = new Main("jetty");
        this.args = args;
    }

    //<editor-fold desc="Jsvc Implementation">

    @Override
    public void init(final DaemonContext daemonContext) throws DaemonInitException {
        try {
            CompatibleJavaVersionCheck.checkForCompatibleJavaVersion();
        } catch (final StartException e) {
            if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                System.err.println(e.getMessage());
            }
            System.exit(e.getErrorCode());
        }

        if (this.main != null) {
            throw new DaemonInitException("Daemon already initialised");
        }
        init(daemonContext.getArguments());
    }

    @Override
    public void start() throws Exception {
        final String[] runArgs = new String[1 + args.length];
        runArgs[0] = MODE_JETTY;
        System.arraycopy(args, 0, runArgs, 1, args.length);

        this.main.runEx(runArgs);
    }

    @Override
    public void stop() throws Exception {
        this.main.shutdownEx();
    }

    @Override
    public void destroy() {
        this.args = null;
        this.main = null;
    }

    //</editor-fold>


    //<editor-fold desc="Procrun Implementation">

    private static ExistDbDaemon instance;

    static void start(final String[] args) throws Exception {
        if (instance != null) {
            throw new IllegalStateException("Instance already started");
        }

        instance = new ExistDbDaemon();
        instance.init(args);
        instance.start();
    }

    static void stop(final String[] args) throws Exception {
        if (instance == null) {
            throw new IllegalStateException("Instance already stopped");
        }

        instance.stop();
        instance.destroy();
        instance = null;
    }

    //</editor-fold>
}
