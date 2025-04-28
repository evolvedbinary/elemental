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
package org.exist.xmldb.txn.bridge;

import org.exist.collections.triggers.TriggerException;
import org.exist.security.Subject;
import org.exist.storage.BrokerPool;
import org.exist.xmldb.LocalCollection;
import org.exist.xmldb.LocalCollectionManagementService;
import org.exist.xmldb.XmldbURI;
import org.exist.xmldb.function.LocalXmldbFunction;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ErrorCodes;
import org.xmldb.api.base.XMLDBException;

import java.util.Date;

/**
 * @author Adam Retter
 */
public class InTxnLocalCollectionManagementService extends LocalCollectionManagementService {
    public InTxnLocalCollectionManagementService(final Subject user, final BrokerPool pool, final LocalCollection parent) {
        super(user, pool, parent);
    }

    @Override
    public Collection createCollection(final XmldbURI name, final Date created) throws XMLDBException {
        final XmldbURI collName = resolve(name);

        withDb((broker, transaction) -> {
            try {
                final org.exist.collections.Collection coll = broker.getOrCreateCollection(transaction, collName);
                if (created != null) {
                    coll.setCreated(created.getTime());
                }
                broker.saveCollection(transaction, coll);
                return null;
            } catch (final TriggerException e) {
                throw new XMLDBException(ErrorCodes.VENDOR_ERROR, e.getMessage(), e);
            }
        });

        return new InTxnLocalCollection(user, brokerPool, collection, collName);
    }

    @Override
    protected <R> R withDb(final LocalXmldbFunction<R> dbOperation) throws XMLDBException {
        return InTxnLocalCollection.withDb(brokerPool, user, dbOperation);
    }
}
