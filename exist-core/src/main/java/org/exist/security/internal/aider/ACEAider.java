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
package org.exist.security.internal.aider;

import org.exist.security.ACLPermission.ACE_ACCESS_TYPE;
import org.exist.security.ACLPermission.ACE_TARGET;

/**
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class ACEAider {
    private ACE_ACCESS_TYPE accessType;
    private ACE_TARGET target;
    private String who;
    private int mode;

    public ACEAider() {
    }

    public ACEAider(final ACE_ACCESS_TYPE accessType, final ACE_TARGET target, final String who, final int mode) {
        this.accessType = accessType;
        this.target = target;
        this.who = who;
        this.mode = mode;
    }

    public ACE_ACCESS_TYPE getAccessType() {
        return accessType;
    }

    public int getMode() {
        return mode;
    }

    public ACE_TARGET getTarget() {
        return target;
    }

    public String getWho() {
        return who;
    }

    public void setAccessType(ACE_ACCESS_TYPE accessType) {
        this.accessType = accessType;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setTarget(ACE_TARGET target) {
        this.target = target;
    }

    public void setWho(String who) {
        this.who = who;
    }

    public ACEAider copy() {
        return new ACEAider(accessType, target, who, mode);
    }
}