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
package org.exist.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.exist.storage.DBBroker;
import org.exist.storage.StartupTrigger;
import org.exist.storage.txn.Txn;

import java.util.List;
import java.util.Map;

/**
 * Startup Trigger to register the Bouncy Castle JCE Provider
 *
 * @author <a href="mailto:adam@evolvedbinary.com">Adam Retter</a>
 */
public class BouncyCastleJceProviderStartupTrigger implements StartupTrigger {

    private final static Logger LOG = LogManager.getLogger(
        BouncyCastleJceProviderStartupTrigger.class);

    @Override
    public void execute(final DBBroker sysBroker, final Txn transaction,
            final Map<String, List<? extends Object>> params) {

      java.security.Security.addProvider(new BouncyCastleProvider());

      LOG.info("Registered JCE Security Provider: org.bouncycastle.jce.provider.BouncyCastleProvider");
    }
}
