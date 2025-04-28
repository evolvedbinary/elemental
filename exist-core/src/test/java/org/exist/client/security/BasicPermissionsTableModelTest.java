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
package org.exist.client.security;

import org.exist.security.PermissionDeniedException;
import org.exist.security.internal.aider.UnixStylePermissionAider;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BasicPermissionsTableModelTest {
    
    @Test
    public void getMode() throws PermissionDeniedException {
        
        final int modes[] = {
            0,
            01,
            04,
            05,
            07,
            011,
            044,
            055,
            077,
            0111,
            0444,
            0555,
            0777,
            0711,
            0744,
            0755,
            04000,
            04100,
            02000,
            02010,
            06777
        };
        
        for(final int mode : modes) {
            final UnixStylePermissionAider permission = new UnixStylePermissionAider(mode);
            final ModeDisplay modeDisplay = ModeDisplay.fromPermission(permission);
            final BasicPermissionsTableModel model = new BasicPermissionsTableModel(modeDisplay);
            final UnixStylePermissionAider updatedPermission = new UnixStylePermissionAider();
            model.getMode().writeToPermission(updatedPermission);
            assertEquals(mode, updatedPermission.getMode());
        }
    }
}
