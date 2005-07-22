/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-03 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
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
package org.exist.xquery.test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Wolfgang Meier (wolfgang@exist-db.org)
 */
public class AllTests {

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(AllTests.class);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.exist.xquery.test");
		//$JUnit-BEGIN$
		suite.addTestSuite(XPathQueryTest.class);
		suite.addTestSuite(ValueIndexTest.class);
		suite.addTestSuite(LexerTest.class); // jmv: Note: LexerTest needs /db/test created by XPathQueryTest
		suite.addTestSuite(DeepEqualTest.class);
		suite.addTestSuite(SeqOpTest.class);
      suite.addTestSuite(XMLNodeAsXQueryParameterTest.class);
      suite.addTestSuite(OpNumericTest.class);
//		suite.addTestSuite(XQueryUseCasesTest.class);
		//$JUnit-END$
		return suite;
	}
}
