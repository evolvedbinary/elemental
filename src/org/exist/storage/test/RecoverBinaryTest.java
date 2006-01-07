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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

import org.exist.collections.Collection;
import org.exist.dom.BinaryDocument;
import org.exist.security.SecurityManager;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.lock.Lock;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.util.Configuration;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * @author wolf
 *
 */
public class RecoverBinaryTest extends TestCase {

    public static void main(String[] args) {
        TestRunner.run(RecoverBinaryTest.class);
    }
    
    private BrokerPool pool;
    
    public void testStore() {
    	BrokerPool.FORCE_CORRUPTION = true;
        DBBroker broker = null;
        try {
        	assertNotNull(pool);
            broker = pool.get(SecurityManager.SYSTEM_USER);
            assertNotNull(broker);
            TransactionManager transact = pool.getTransactionManager();
            assertNotNull(transact);
            Txn transaction = transact.beginTransaction();
            assertNotNull(transaction);
            System.out.println("Transaction started ...");
            
            Collection root = broker.getOrCreateCollection(transaction, DBBroker.ROOT_COLLECTION + "/test");
            assertNotNull(root);
            broker.saveCollection(transaction, root);
    
            FileInputStream is = new FileInputStream("LICENSE");
            assertNotNull(is);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buf = new byte[512];
            int count = 0;
            while ((count = is.read(buf)) > -1) {
                os.write(buf, 0, count);
            }
            BinaryDocument doc = 
				root.addBinaryResource(transaction, broker, "binary.txt", os.toByteArray(),	"text/text");
            assertNotNull(doc);
            
            transact.commit(transaction);
            System.out.println("Transaction commited ...");
            
            // the following transaction will not be committed. It will thus be rolled back by recovery
//            transaction = transact.beginTransaction();
//            root.removeBinaryResource(transaction, broker, doc);
            
            //TODO : remove ?
            transact.getJournal().flushToLog(true);
		} catch (Exception e) {            
	        fail(e.getMessage());             
        } finally {
            if (pool != null) pool.release(broker);
        }
    }
    
    public void testLoad() {
        BrokerPool.FORCE_CORRUPTION = false;
        DBBroker broker = null;
        try {
        	System.out.println("testRead() ...\n");
        	assertNotNull(pool);
        	broker = pool.get(SecurityManager.SYSTEM_USER);
        	assertNotNull(broker);
            BinaryDocument binDoc = (BinaryDocument) broker.openDocument(DBBroker.ROOT_COLLECTION + "/test/binary.txt", Lock.READ_LOCK);
            assertNotNull("Binary document is null", binDoc);
            String data = new String(broker.getBinaryResource(binDoc));
            assertNotNull(data);
            System.out.println(data);
		} catch (Exception e) {            
	        fail(e.getMessage());
	    } finally {
            if (pool != null) pool.release(broker);
        }
    }
    
    protected void setUp() {
        String home, file = "conf.xml";
        home = System.getProperty("exist.home");
        if (home == null)
            home = System.getProperty("user.dir");
        try {
            Configuration config = new Configuration(file, home);
            BrokerPool.configure(1, 5, config);
            pool = BrokerPool.getInstance();
        } catch (Exception e) {            
            fail(e.getMessage());
        }
    }

    protected void tearDown() {
        BrokerPool.stopAll(false);
    }
}
