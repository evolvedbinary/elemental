package org.exist.storage.test;

import junit.textui.TestRunner;

import org.exist.security.SecurityManager;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.TransactionManager;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.Sequence;

public class XQueryUpdateTest extends AbstractUpdateTest {

	public static void main(String[] args) {
        TestRunner.run(XQueryUpdateTest.class);
    }
	
	public void testAppend() {
		BrokerPool pool = null;
        DBBroker broker = null;
        try {
        	System.out.println("testRead() ...\n");  
        	
        	pool = startDB();
        	TransactionManager mgr = pool.getTransactionManager();
            broker = pool.get(SecurityManager.SYSTEM_USER);
            
            init(broker, mgr);
            
            XQuery xquery = broker.getXQueryService();
            String query = 
            	"for $i in 1 to 2000 return (\n" +
            	"	util:log('DEBUG', ('Update No.: ', $i)),\n" +
            	"	update insert\n" +
            	"		<product id='id{$i}'>\n" +
            	"			<description>Description {$i}</description>\n" +
            	"			<price>{$i * 2.5}</price>\n" +
            	"			<stock>{$i * 10}</stock>\n" +
            	"		</product>\n" +
            	"	into /products\n" +
            	")";
            xquery.execute(query, null);
            
            Sequence seq = xquery.execute("//product", null);
            assertEquals(seq.getLength(), 2000);
            
            query =
            	"for $i in 1 to 1000 return\n" +
            	"	update delete /products/product[last()]";
            xquery.execute(query, null);
            
            seq = xquery.execute("//product", null);
            assertEquals(seq.getLength(), 1000);
        } catch (Exception e) {            
            fail(e.getMessage());
        } finally {
            if (pool != null) pool.release(broker);
        }
	}
}
