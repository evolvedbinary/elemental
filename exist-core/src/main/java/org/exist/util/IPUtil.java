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
package org.exist.util;

import net.jcip.annotations.GuardedBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;

/**
 * IP Utilities
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class IPUtil {

    @GuardedBy("class")
    private static final Random random = new Random();

    /**
     * Attempts to get the next random free IP port in the range {@code from} and {@code to}.
     *
     * @param from start of the port range
     * @param to end of the port range
     * @param maxAttempts the number of attempts to make to find a free port
     *
     * @return a potentially free IP port. This is done on a best effort basis! It is possible that the port returned
     *     is free, but by time you come to use it, it is then in-use; if so, just try calling this again.
     *
     * @throws IllegalStateException if maxAttempts is exceeded
     */
    public static int nextFreePort(final int from, final int to, final int maxAttempts) {
        for (int attempts = 0; attempts < maxAttempts; attempts++) {
            final int port = random(from, to);
            if (isLocalPortFree(port)) {
                return port;
            }
        }

        throw new IllegalStateException("Exceeded MAX_RANDOM_PORT_ATTEMPTS");
    }

    private synchronized static int random(final int min, final int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    private static boolean isLocalPortFree(final int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (final IOException e) {
            return false;
        }
    }
}
