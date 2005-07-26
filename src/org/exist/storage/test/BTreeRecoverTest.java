/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-04 The eXist Project
 *  http://exist-db.org
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.storage.test;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.exist.security.SecurityManager;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.NativeBroker;
import org.exist.storage.btree.BTreeCallback;
import org.exist.storage.btree.IndexQuery;
import org.exist.storage.btree.Value;
import org.exist.storage.dom.DOMFile;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.util.ByteConversion;
import org.exist.util.Configuration;
import org.exist.xquery.TerminatedException;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Tests transaction management and basic recovery for the BTree base class.
 * 
 * @author wolf
 *
 */
public class BTreeRecoverTest extends TestCase {

    public static void main(String[] args) {
        TestRunner.run(BTreeRecoverTest.class);
    }

    private BrokerPool pool;
    private int count = 0;
    
    public void testAdd() throws Exception {
        System.out.println("Add some random data and force db corruption ...\n");
        
        TransactionManager mgr = pool.getTransactionManager();
        DBBroker broker = null;
        try {
            broker = pool.get(SecurityManager.SYSTEM_USER);
            broker.flush();
            Txn txn = mgr.beginTransaction();
            System.out.println("Transaction started ...");
            
            DOMFile domDb = ((NativeBroker) broker).getDOMFile();
            domDb.setOwnerObject(this);
            
            BrokerPool.FORCE_CORRUPTION = true;
            
            // put 1000 values into the btree
            long l;
            for (int i = 1; i < 1001; i++) {
                domDb.addValue(txn, new NativeBroker.NodeRef(500, i), i);
            }
            
            IndexQuery idx = new IndexQuery(IndexQuery.GT, new NativeBroker.NodeRef(500, 800));
            domDb.remove(txn, idx, null);
            
            mgr.commit(txn);
            
            // start a dirty, uncommitted transaction. This will be rolled back by the recovery.
            txn = mgr.beginTransaction();
            
            for (int i = 801; i < 2001; i++) {
                domDb.addValue(txn, new NativeBroker.NodeRef(500, i), i);
            }
            
            for (int i = 101; i < 301; i++) {
                domDb.addValue(txn, new NativeBroker.NodeRef(500, i), i * 3);
            }
            
            idx = new IndexQuery(IndexQuery.GT, new NativeBroker.NodeRef(500, 600));
            domDb.remove(txn, idx, null);
            
            mgr.getLogManager().flushToLog(true);
            
            Writer writer = new StringWriter();
            domDb.dump(writer);
            System.out.println(writer.toString());
        } finally {
            pool.release(broker);
        }
    }
    
    public void testGet() throws Exception {
        System.out.println("Recover and read the data ...\n");
        TransactionManager mgr = pool.getTransactionManager();
        DBBroker broker = null;
        try {
            broker = pool.get(SecurityManager.SYSTEM_USER);
            
            DOMFile domDb = ((NativeBroker) broker).getDOMFile();
            domDb.setOwnerObject(this);
            
            IndexQuery query = new IndexQuery(IndexQuery.GEQ, new NativeBroker.NodeRef(500, 1));
            domDb.query(query, new IndexCallback());
            System.out.println("Found: " + count);
            assertEquals(count, 800);
            
            Writer writer = new StringWriter();
            domDb.dump(writer);
            System.out.println(writer.toString());
        } finally {
            pool.release(broker);
        }
    }
    
    protected void setUp() throws Exception {
        String home, file = "conf.xml";
        home = System.getProperty("exist.home");
        if (home == null)
            home = System.getProperty("user.dir");
        try {
            Configuration config = new Configuration(file, home);
            BrokerPool.configure(1, 5, config);
            pool = BrokerPool.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    protected void tearDown() throws Exception {
        BrokerPool.stopAll(false);
    }
    
    private final class IndexCallback implements BTreeCallback {
        public boolean indexInfo(Value value, long pointer)
                throws TerminatedException {
            System.out.println(ByteConversion.byteToLong(value.data(), value.start() + 4) + " -> " + pointer);
            count++;
            return false;
        }
    }
}
