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
package org.exist.xquery;

import org.exist.security.Subject;

/**
 * Base class for XQuery functions which switch the current user
 *
 * Provides the function {@link #switchUser(Subject)} to allow us
 * to switch the current broker to a user and then have it switched
 * back when the XQuery expression is reset
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public abstract class UserSwitchingBasicFunction extends BasicFunction {

    /**
     * Flag which indicates how many subjects we have pushed, and so we must later
     * pop the same number of subjects when the expression is reset, see {@link UserSwitchingBasicFunction#resetState(boolean)}
     */
    private int pushedSubjects = 0;

    public UserSwitchingBasicFunction(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    /**
     * Switches the current broker to the provided user.
     *
     * @param user the subject authority that the broker will be switched to
     */
    protected void switchUser(final Subject user) {
        context.getBroker().pushSubject(user);
        pushedSubjects++;
    }

    /**
     * Takes care to switch the broker back from the switched
     * user before calling @{link super#resetState(boolean)}
     */
    @Override
    public void resetState(final boolean postOptimization) {
        //if we pushed a subject, we must pop it
        while (pushedSubjects > 0) {
            context.getBroker().popSubject();
            pushedSubjects--;
        }

        super.resetState(postOptimization);
    }
}
