package org.exist.xupdate.test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author berlinge-to
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class AllTests {

	public static Test suite() {

        XUpdateTest test = new XUpdateTest();
		TestSuite suite = new TestSuite("Test suite for org.exist.xupdate");
   
		//$JUnit-BEGIN$
        suite.addTest(new XUpdateTestCases("append", test));
        suite.addTest(new XUpdateTestCases("insertafter", test));
        suite.addTest(new XUpdateTestCases("insertbefore", test));
        suite.addTest(new XUpdateTestCases("remove", test));
        suite.addTest(new XUpdateTestCases("update", test));
        suite.addTest(new XUpdateTestCases("appendAttribute", test));
        suite.addTest(new XUpdateTestCases("appendChild", test));
        suite.addTest(new XUpdateTestCases("insertafter_big", test));
        suite.addTest(new XUpdateTestCases("conditional", test));
        
        /*
         * create new TestCase
         * -------------------
         * add the following line:
         *
         * suite.addTest(new XUpdateTests(<TestName>, exist));
         * 
         * Param: TestName is the filename of the XUpdateStatement xml file (without '.xml').
         * 
         */
        
		//$JUnit-END$
        
		return suite;
	}

}
