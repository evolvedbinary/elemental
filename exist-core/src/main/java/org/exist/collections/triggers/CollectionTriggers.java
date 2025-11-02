/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
package org.exist.collections.triggers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.exist.collections.Collection;
import org.exist.collections.CollectionConfiguration;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.xmldb.XmldbURI;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public class CollectionTriggers implements CollectionTrigger {
    
    private final List<CollectionTrigger> triggers;

    public CollectionTriggers(final DBBroker broker, final Txn transaction) throws TriggerException {
        this(broker, transaction, null, null);
    }

    public CollectionTriggers(final DBBroker broker, final Txn transaction, final Collection collection) throws TriggerException {
        this(broker, transaction, collection, collection.getConfiguration(broker));
    }

    public CollectionTriggers(final DBBroker broker, final Txn transaction, final Collection collection, final CollectionConfiguration config) throws TriggerException {
        final List<TriggerProxy<? extends CollectionTrigger>> colTriggers = config != null ? config.collectionTriggers() : null;
        final java.util.Collection<TriggerProxy<? extends CollectionTrigger>> masterTriggers = broker.getDatabase().getCollectionTriggers();
        
        triggers = new ArrayList<>(masterTriggers.size() + (colTriggers == null ? 0 : colTriggers.size()));
        
        for (final TriggerProxy<? extends CollectionTrigger> colTrigger : masterTriggers) {
            final CollectionTrigger instance = colTrigger.newInstance(broker, transaction, collection);
            register(instance);
        }
        
        if (colTriggers != null) {
            for (final TriggerProxy<? extends CollectionTrigger> colTrigger : colTriggers) {
                final CollectionTrigger instance = colTrigger.newInstance(broker, transaction, collection);
                register(instance);
            }
        }
    }
    
    private void register(final CollectionTrigger trigger) {
        triggers.add(trigger);
    }
    
    @Override
    public void configure(final DBBroker broker, final Txn transaction, final Collection col, final Map<String, List<? extends Object>> parameters) throws TriggerException {
    }

    @Override
    public void beforeCreateCollection(final DBBroker broker, final Txn txn, final XmldbURI uri) throws TriggerException {
        for (final CollectionTrigger trigger : triggers) {
            try {
                trigger.beforeCreateCollection(broker, txn, uri);
            } catch (final Exception e) {
                logAndThrowError("beforeCreateCollection", trigger, uri, e);
            }
        }
    }

    @Override
    public void afterCreateCollection(final DBBroker broker, final Txn txn, final Collection collection) {
        for (final CollectionTrigger trigger : triggers) {
            try {
                trigger.afterCreateCollection(broker, txn, collection);
            } catch (final Exception e) {
                logError("afterCreateCollection", trigger, collection.getURI(), e);
            }
        }
    }

    @Override
    public void beforeCopyCollection(final DBBroker broker, final Txn txn, final Collection collection, final XmldbURI newUri) throws TriggerException {
        for (final CollectionTrigger trigger : triggers) {
            try {
                trigger.beforeCopyCollection(broker, txn, collection, newUri);
            } catch (final Exception e) {
                logAndThrowError("beforeCopyCollection", trigger, collection.getURI(), e);
            }
        }
    }

    @Override
    public void afterCopyCollection(final DBBroker broker, final Txn txn, final Collection collection, final XmldbURI oldUri) {
        for (final CollectionTrigger trigger : triggers) {
            try {
                trigger.afterCopyCollection(broker, txn, collection, oldUri);
            } catch (final Exception e) {
                logError("afterCopyCollection", trigger, oldUri, e);
            }
        }
    }

    @Override
    public void beforeMoveCollection(final DBBroker broker, final Txn txn, final Collection collection, final XmldbURI newUri) throws TriggerException {
        for (final CollectionTrigger trigger : triggers) {
            try {
                trigger.beforeMoveCollection(broker, txn, collection, newUri);
            } catch (final Exception e) {
                logAndThrowError("beforeMoveCollection", trigger, collection.getURI(), e);
            }
        }
    }

    @Override
    public void afterMoveCollection(final DBBroker broker, final Txn txn, final Collection collection, final XmldbURI oldUri) {
        for (final CollectionTrigger trigger : triggers) {
            try {
                trigger.afterMoveCollection(broker, txn, collection, oldUri);
            } catch (final Exception e) {
                logError("afterMoveCollection", trigger, oldUri, e);
            }
        }
    }

    @Override
    public void beforeDeleteCollection(final DBBroker broker, final Txn txn, final Collection collection) throws TriggerException {
        for (final CollectionTrigger trigger : triggers) {
            try {
                trigger.beforeDeleteCollection(broker, txn, collection);
            } catch (final Exception e) {
                logAndThrowError("beforeDeleteCollection", trigger, collection.getURI(), e);
            }
        }
    }

    @Override
    public void afterDeleteCollection(final DBBroker broker, final Txn txn, final XmldbURI uri) {
        for (final CollectionTrigger trigger : triggers) {
            try {
                trigger.afterDeleteCollection(broker, txn, uri);
            } catch (final Exception e) {
                logError("afterDeleteCollection", trigger, uri, e);
            }
        }
    }

    private void logAndThrowError(final String eventName, final CollectionTrigger collectionTrigger, final XmldbURI source, final Exception e) throws TriggerException {
        logError(eventName, collectionTrigger, source, e);
        throwError(e);
    }

    private void logError(final String eventName, final CollectionTrigger collectionTrigger, final XmldbURI source, final Exception e) {
        final String message = String.format("Error in %s#%s triggered by: %s, %s", collectionTrigger.getClass().getSimpleName(), eventName, source, e.getMessage());
        Trigger.LOG.error(message, e);
    }

    private void throwError(final Exception e) throws TriggerException {
        if (e instanceof TriggerException) {
            throw (TriggerException) e;
        } else if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new TriggerException(e);
        }
    }
}
