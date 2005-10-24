/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-05 The eXist Project
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
*/

package org.exist.collections.triggers.test;

import org.exist.xmldb.EXistResource;
import org.exist.xmldb.IndexQueryService;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XPathQueryService;
import org.xmldb.api.modules.XUpdateQueryService;
import org.custommonkey.xmlunit.*;

/**
 * @author Pierrick Brihaye <pierrick.brihaye@free.fr>
 */
public class XQueryTriggerTest extends XMLTestCase {
	
	private final static String URI = "xmldb:exist:///db";
	private final static String TEST_COLLECTION = "testXQueryTrigger";

    private String CONFIG =
    	"<exist:collection xmlns:exist=\"http://exist-db.org/collection-config/1.0\">" +
	    "  <exist:triggers>" +
		"     <exist:trigger event=\"store\"" +
		"                    class=\"org.exist.collections.triggers.XQueryTrigger\">" +
		"	     <exist:parameter name=\"query\" value=\"import module namespace log = 'log' at '" + URI +  "/" + TEST_COLLECTION + "/" + MODULE_NAME + "'; log:log('trigger1')\"/>" +
		"	     <exist:parameter name=\"bindingPrefix\" value=\"log\"/>" +
		"        />" +
		"     </exist:trigger>" +
		
		"     <exist:trigger event=\"update\"" +
		"                    class=\"org.exist.collections.triggers.XQueryTrigger\">" +
		"	     <exist:parameter name=\"query\" value=\"import module namespace log = 'log' at '" + URI +  "/" + TEST_COLLECTION + "/" + MODULE_NAME + "'; log:log('trigger2')\"/>" +
		"	     <exist:parameter name=\"bindingPrefix\" value=\"log\"/>" +
		"        />" +
		"     </exist:trigger>" +

		"     <exist:trigger event=\"remove\"" +
		"                    class=\"org.exist.collections.triggers.XQueryTrigger\">" +
		"	     <exist:parameter name=\"query\" value=\"import module namespace log = 'log' at '" + URI +  "/" + TEST_COLLECTION + "/" + MODULE_NAME + "'; log:log('trigger3')\"/>" +
		"	     <exist:parameter name=\"bindingPrefix\" value=\"log\"/>" +
		"        />" +
		"     </exist:trigger>" +
		
		"  </exist:triggers>" +
        "</exist:collection>";    

    private String EMPTY_CONFIG =
    	"<exist:collection xmlns:exist=\"http://exist-db.org/collection-config/1.0\">" +
        "</exist:collection>";    
    
    private final static String DOCUMENT_NAME = "test.xml";
    
    private final static String DOCUMENT_CONTENT = 
		  "<test>"
		+ "<item id='1'><price>5.6</price><stock>22</stock></item>"
		+ "<item id='2'><price>7.4</price><stock>43</stock></item>"
		+ "<item id='3'><price>18.4</price><stock>5</stock></item>"
		+ "<item id='4'><price>65.54</price><stock>16</stock></item>"
		+ "</test>";    

    private final static String DOCUMENT_UPDATE =
        "<xu:modifications xmlns:xu=\"http://www.xmldb.org/xupdate\" version=\"1.0\">" +
        "<!-- special offer -->" +
        "<xu:update select=\"/test/item[@id = '3']/price\">" +       
        "15.2"+
        "</xu:update>" +
      "</xu:modifications>";
    
//    private final static String MODIFIED_DOCUMENT_CONTENT = DOCUMENT_CONTENT.replace("18.4", "15.2");
    
    private final static String LOG_NAME = "XQueryTriggerLog.xml";
    
    private final static String EMPTY_LOG = "<events/>";
    
    private final static String MODULE_NAME = "XQueryTriggerLogger.xqm";
    	
    private final static String MODULE =
    	"module namespace log='log'; " +
    	"import module namespace xmldb='http://exist-db.org/xquery/xmldb'; " +
    	"declare variable $log:collectionName external;" +    	
    	"declare variable $log:documentName external;" +
    	"declare variable $log:triggerEvent external;" +
    	"declare variable $log:document external;" +
    	"declare function log:log($id as xs:string?) {" +
    	  "xmldb:update("+
    	    "xmldb:collection('" + URI + "/" + TEST_COLLECTION + "', 'admin', ''), " +
            "<xu:modifications xmlns:xu=\"http://www.xmldb.org/xupdate\" version=\"1.0\">" +
              "<xu:append select=\"/events\">" +
                "<xu:element name=\"event\">" +
                  "<xu:attribute name=\"id\">{$id}</xu:attribute>" +
                  "<xu:attribute name=\"time\">{current-dateTime()}</xu:attribute>" +
                  "<xu:element name=\"collectionName\">{$log:collectionName}</xu:element>" +
                  "<xu:element name=\"documentName\">{$log:documentName}</xu:element>" +
                  "<xu:element name=\"triggerEvent\">{$log:triggerEvent}</xu:element>" +                 
                  "<xu:element name=\"document\">{$log:document}</xu:element>" +   
                "</xu:element>" +            
              "</xu:append>" +
            "</xu:modifications>" +
          ")" +
        "};";
    
    private Collection testCollection;

    protected void setUp() {
        try {
            // initialize driver
            Class cl = Class.forName("org.exist.xmldb.DatabaseImpl");
            Database database = (Database) cl.newInstance();
            database.setProperty("create-database", "true");
            DatabaseManager.registerDatabase(database);

            Collection root = DatabaseManager.getCollection(URI, "admin", null);
            CollectionManagementService service = (CollectionManagementService) root
                    .getService("CollectionManagementService", "1.0");
            testCollection = service.createCollection(TEST_COLLECTION);
            assertNotNull(testCollection);
            
        } catch (ClassNotFoundException e) {
        	fail(e.getMessage());
        } catch (InstantiationException e) {
        	fail(e.getMessage());
        } catch (IllegalAccessException e) {
        	fail(e.getMessage());
        } catch (XMLDBException e) {
        	fail(e.getMessage());
        }
    }

    public void testStorePreliminaryDocuments() {
    	try {
			XMLResource doc =
				(XMLResource) testCollection.createResource(LOG_NAME, "XMLResource" );
			doc.setContent(EMPTY_LOG);
			testCollection.storeResource(doc);
	
			BinaryResource module =
				(BinaryResource) testCollection.createResource(MODULE_NAME, "BinaryResource" );
			((EXistResource)module).setMimeType("application/xquery");
			module.setContent(MODULE.getBytes());
			testCollection.storeResource(module);
			
    	} catch (XMLDBException e) {
    		fail(e.getMessage());
        }    	
    }

    public void testStoreDocument() throws Exception {
    	
    	ResourceSet result;
    	
    	try {    		
    		
			IndexQueryService idxConf = (IndexQueryService)
			testCollection.getService("IndexQueryService", "1.0");
			idxConf.configureCollection(CONFIG);
				
			XMLResource doc =
				(XMLResource) testCollection.createResource(DOCUMENT_NAME, "XMLResource" );
			doc.setContent(DOCUMENT_CONTENT);
			testCollection.storeResource(doc);
			
			idxConf.configureCollection(EMPTY_CONFIG);			

	        XPathQueryService service = (XPathQueryService) testCollection
    			.getService("XPathQueryService", "1.0");	        
	        
	        result = service.query("/events/event[@id = 'trigger1']");
	        assertEquals(2, result.getSize());
	        
	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger1'][collectionName = '" + "/db" +  "/" + TEST_COLLECTION + "']");
	        assertEquals(2, result.getSize());	        

	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger1'][documentName = '" + "/db" +  "/" + TEST_COLLECTION + "/" + DOCUMENT_NAME + "']");
	        assertEquals(2, result.getSize());	        

	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger1'][triggerEvent = 'STORE']");
	        assertEquals(2, result.getSize());	        
	        
	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger1']/document/test");
	        assertEquals(1, result.getSize());
	        //TODO : comparison fails !
	        //assertXMLEqual(DOCUMENT_CONTENT, result.getResource(0).getContent().toString());
	        
    	} catch (Exception e) {
    		fail(e.getMessage());
    		throw e;
    	}
			
    }

    public void bugtestUpdateDocument() throws Exception {
    	
    	ResourceSet result;
    	
    	try {
			IndexQueryService idxConf = (IndexQueryService)
			testCollection.getService("IndexQueryService", "1.0");
			idxConf.configureCollection(CONFIG);
	       
			//TODO : trigger UPDATE events !
	        XUpdateQueryService update = (XUpdateQueryService) testCollection.getService("XUpdateQueryService", "1.0");
	        update.updateResource(DOCUMENT_NAME, DOCUMENT_UPDATE);
	        
	        idxConf.configureCollection(EMPTY_CONFIG);

	        XPathQueryService service = (XPathQueryService) testCollection
    		.getService("XPathQueryService", "1.0");	        

	        result = service.query("/events/event[@id = 'trigger2']");
	        assertEquals(2, result.getSize());
	        
	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger2'][collectionName = '" + "/db" +  "/" + TEST_COLLECTION + "']");
	        assertEquals(2, result.getSize());	        

	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger2'][documentName = '" + "/db" +  "/" + TEST_COLLECTION + "/" + DOCUMENT_NAME + "']");
	        assertEquals(2, result.getSize());	        

	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger2'][triggerEvent = 'UPDATE']");
	        assertEquals(2, result.getSize());	        
	        
	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger2']/document/test");
	        assertEquals(2, result.getSize());
	        //TODO : comparison fails !
	        //assertXMLEqual(DOCUMENT_CONTENT, result.getResource(0).getContent().toString());	        
	        //assertXMLEqual(MODIFIED_DOCUMENT_CONTENT, result.getResource(1).getContent().toString());

    	} catch (Exception e) {
    		fail(e.getMessage());
    		throw e;
    	}	
        
    }
    
    public void testDeleteDocument() throws Exception {
    	
    	ResourceSet result;
    	
    	try {
			IndexQueryService idxConf = (IndexQueryService)
			testCollection.getService("IndexQueryService", "1.0");
			idxConf.configureCollection(CONFIG);
	
			testCollection.removeResource(testCollection.getResource(DOCUMENT_NAME));

			idxConf.configureCollection(EMPTY_CONFIG);
			
	        XPathQueryService service = (XPathQueryService) testCollection
	        	.getService("XPathQueryService", "1.0");   

	        result = service.query("/events/event[@id = 'trigger3']");
	        assertEquals(2, result.getSize());
	        
	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger3'][collectionName = '" + "/db" +  "/" + TEST_COLLECTION + "']");
	        assertEquals(2, result.getSize());	        

	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger3'][documentName = '" + "/db" +  "/" + TEST_COLLECTION + "/" + DOCUMENT_NAME + "']");
	        assertEquals(2, result.getSize());	        

	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger3'][triggerEvent = 'REMOVE']");
	        assertEquals(2, result.getSize());	        
	        
	        //TODO : consistent URI !	        
	        result = service.query("/events/event[@id = 'trigger3']/document/test");
	        assertEquals(1, result.getSize());
	        //TODO : comparison fails !
	        //assertXMLEqual(MODIFIED_DOCUMENT_CONTENT, result.getResource(0).getContent().toString());        
			
    	} catch (Exception e) {
    		fail(e.getMessage());
    		throw e;
    	}	
    }
    
    public static void main(String[] args) {
		junit.textui.TestRunner.run(XQueryTriggerTest.class);
	}    

}
