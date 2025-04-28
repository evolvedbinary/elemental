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

/**
 * Definitions of codes to use with {@link System#exit(int)}
 */
public class SystemExitCodes {

    public final static int OK_EXIT_CODE = 0;

    public final static int CATCH_ALL_GENERAL_ERROR_EXIT_CODE = 1;

    public final static int INVALID_ARGUMENT_EXIT_CODE = 3;
    public final static int NO_BROKER_EXIT_CODE = 4;
    public final static int TERMINATED_EARLY_EXIT_CODE = 5;
    public final static int PERMISSION_DENIED_EXIT_CODE = 6;
    public final static int IO_ERROR_EXIT_CODE = 7;
}
