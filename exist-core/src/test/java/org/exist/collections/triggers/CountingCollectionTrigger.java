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
package org.exist.collections.triggers;

import org.exist.storage.DBBroker;
import org.exist.storage.txn.Txn;
import org.exist.xmldb.XmldbURI;

import java.util.List;
import java.util.Map;

public class CountingCollectionTrigger implements CollectionTrigger {

    final CountingCollectionTriggerState state = CountingCollectionTriggerState.getInstance();

    @Override
    public void configure(DBBroker broker, Txn transaction, org.exist.collections.Collection parent, Map<String, List<? extends Object>> parameters) throws TriggerException {
        state.incConfigure();
    }

    @Override
    public void beforeCreateCollection(DBBroker broker, Txn txn, XmldbURI uri) throws TriggerException {
        state.incBeforeCreate();
    }

    @Override
    public void afterCreateCollection(DBBroker broker, Txn txn, org.exist.collections.Collection collection) throws TriggerException {
        state.incAfterCreate();
    }

    @Override
    public void beforeCopyCollection(DBBroker broker, Txn txn, org.exist.collections.Collection collection, XmldbURI newUri) throws TriggerException {
        state.incBeforeCopy();
    }

    @Override
    public void afterCopyCollection(DBBroker broker, Txn txn, org.exist.collections.Collection collection, XmldbURI oldUri) throws TriggerException {
        state.incAfterCopy();
    }

    @Override
    public void beforeMoveCollection(DBBroker broker, Txn txn, org.exist.collections.Collection collection, XmldbURI newUri) throws TriggerException {
        state.incBeforeMove();
    }

    @Override
    public void afterMoveCollection(DBBroker broker, Txn txn, org.exist.collections.Collection collection, XmldbURI oldUri) throws TriggerException {
        state.incAfterMove();
    }

    @Override
    public void beforeDeleteCollection(DBBroker broker, Txn txn, org.exist.collections.Collection collection) throws TriggerException {
        state.incBeforeDelete();
    }

    @Override
    public void afterDeleteCollection(DBBroker broker, Txn txn, XmldbURI uri) throws TriggerException {
        state.incAfterDelete();
    }

    //this evil thing is here so that we can share state between the trigger and the test
    //we really should re-design triggers to make them easily testable!
    //...all I am going to say is that I am not redesigning triggers again right now
    //and that `they made me do it`!
    public final static class CountingCollectionTriggerState {
        private int configure = 0;
        private int beforeCreate = 0;
        private int afterCreate = 0;
        private int beforeCopy = 0;
        private int afterCopy = 0;
        private int beforeMove = 0;
        private int afterMove = 0;
        private int beforeDelete = 0;
        private int afterDelete = 0;

        private final static CountingCollectionTriggerState instance = new CountingCollectionTriggerState();

        private CountingCollectionTriggerState() {
        }

        public final static CountingCollectionTriggerState getInstance() {
            return instance;
        }

        public int getConfigure() {
            return configure;
        }

        public void incConfigure() {
            configure++;
        }

        public int getBeforeCreate() {
            return beforeCreate;
        }

        public void incBeforeCreate() {
            beforeCreate++;
        }

        public int getAfterCreate() {
            return afterCreate;
        }

        public void incAfterCreate() {
            afterCreate++;
        }

        public int getBeforeCopy() {
            return beforeCopy;
        }

        public void incBeforeCopy() {
            beforeCopy++;
        }

        public int getAfterCopy() {
            return afterCopy;
        }

        public void incAfterCopy() {
            afterCopy++;
        }

        public int getBeforeMove() {
            return beforeMove;
        }

        public void incBeforeMove() {
            beforeMove++;
        }

        public int getAfterMove() {
            return afterMove;
        }

        public void incAfterMove() {
            afterMove++;
        }

        public int getBeforeDelete() {
            return beforeDelete;
        }

        public void incBeforeDelete() {
            beforeDelete++;
        }

        public int getAfterDelete() {
            return afterDelete;
        }

        public void incAfterDelete() {
            afterDelete++;
        }
    }
}
